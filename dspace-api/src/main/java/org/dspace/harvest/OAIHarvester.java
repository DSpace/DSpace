/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;
import static org.dspace.harvest.HarvestedCollection.STATUS_BUSY;
import static org.dspace.harvest.HarvestedCollection.STATUS_READY;
import static org.dspace.harvest.HarvestedCollection.STATUS_UNKNOWN_ERROR;
import static org.dspace.harvest.service.OAIHarvesterClient.OAI_IDENTIFIER_NS;
import static org.dspace.harvest.util.NamespaceUtils.METADATA_FORMATS_KEY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.service.ItemSearchService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CERIFIngestionCrosswalk;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.core.service.PluginService;
import org.dspace.handle.service.HandleService;
import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.dspace.harvest.util.NamespaceUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;


/**
 * This class handles OAI harvesting of externally located records into this
 * repository.
 *
 * @author Alexey Maslov
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class OAIHarvester {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(OAIHarvester.class);

    private static final Namespace OAI_NS = OAIHarvesterClient.OAI_NS;

    public static final String OAI_ADDRESS_ERROR = "invalidAddress";
    public static final String OAI_SET_ERROR = "noSuchSet";
    public static final String OAI_DMD_ERROR = "metadataNotSupported";
    public static final String OAI_ORE_ERROR = "oreNotSupported";

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private HarvestedCollectionService harvestedCollectionService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private HandleService handleService;

    @Autowired
    private HarvestedItemService harvestedItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OAIHarvesterClient oaiHarvesterClient;

    @Autowired
    private ItemSearchService itemSearchService;

    private String ORESerialURI;

    private String ORESerialKey;

    /**
     * Set the ORE options.
     */
    @PostConstruct
    private void setup() {
        Namespace ORESerializationNamespace = NamespaceUtils.getORENamespace();
        ORESerialURI = ORESerializationNamespace.getURI();
        ORESerialKey = ORESerializationNamespace.getPrefix();
    }

    /**
     * Performs a harvest cycle on this collection. This will query the remote
     * OAI-PMH provider, check for updates since last harvest, and ingest the
     * returned items.
     *
     * @throws IOException        A general class of exceptions produced by failed
     *                            or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a
     *                            database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the
     *                            context does not have permission to perform a
     *                            particular action.
     */
    public void runHarvest(Context context, HarvestedCollection harvestRow) {

        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.BATCH_EDIT);

        try {

            harvestRow = context.reloadEntity(harvestRow);
            if (harvestRow == null || !harvestedCollectionService.isHarvestable(harvestRow)) {
                throw new HarvestingException("Provided collection is not set up for harvesting");
            }

            String oaiSource = harvestRow.getOaiSource();

            Date fromDate = harvestRow.getLastHarvestDate();
            Date toDate = new Date();

            harvestRow = setCollectionBusy(context, harvestRow, toDate);

            startHarvest(context, harvestRow, fromDate, toDate);

            setCollectionReady(context, harvestRow, "Harvest from " + oaiSource + " successful", toDate);

        } catch (Exception ex) {
            String message = "An error occurred: " + getRootExceptionMessage(ex);
            log.error(message, ex);
            setCollectionInError(context, harvestRow, message);
            alertAdmin(harvestRow.getCollection(), STATUS_UNKNOWN_ERROR, ex);
        } finally {
            context.setMode(originalMode);
        }

    }

    private void startHarvest(Context context, HarvestedCollection harvestRow, Date fromDate, Date toDate) {

        String oaiSource = harvestRow.getOaiSource();
        String oaiSetId = harvestRow.getOaiSetId();
        String metadataKey = harvestRow.getHarvestMetadataConfig();

        String descriptiveMetadataFormat = getDescriptiveMetadataFormat(oaiSource, metadataKey);

        String dateFormat = getDateFormat(oaiSource);
        String fromDateAsString = fromDate != null ? formatDate(fromDate, dateFormat) : null;
        String toDateAsString = formatDate(toDate, 0, dateFormat);

        String repositoryIdentifier = getRepositoryIdentifier(harvestRow);

        OAIHarvesterResponseDTO responseDTO = oaiHarvesterClient.listRecords(oaiSource, fromDateAsString,
            toDateAsString, oaiSetId, descriptiveMetadataFormat);

        processOAIHarvesterResponse(context, harvestRow, oaiSource, responseDTO, toDate, repositoryIdentifier);

    }

    private void processOAIHarvesterResponse(Context context, HarvestedCollection harvestRow, String oaiSource,
        OAIHarvesterResponseDTO responseDTO, Date toDate, String repositoryIdentifier) {

        Document oaiResponse = responseDTO.getDocument();

        if (responseDTO.hasErrors()) {
            Set<String> errors = responseDTO.getErrors();
            if (errors.size() == 1 && errors.contains("noRecordsMatch")) {
                String message = "noRecordsMatch: OAI server did not contain any updates";
                setCollectionReady(context, harvestRow, message, toDate);
                log.info(message);
                return;
            } else {
                throw new HarvestingException(errors.toString());
            }
        }

        List<Element> records = getAllRecords(oaiResponse);

        if (CollectionUtils.isEmpty(records)) {
            log.info("No records to process found");
            return;
        }

        // Process the obtained records
        harvestRow = processRecords(context, harvestRow, records, repositoryIdentifier);

        // keep going if there are more records to process
        String token = responseDTO.getResumptionToken();
        if (isNotEmpty(token)) {
            OAIHarvesterResponseDTO nextResponseDTO = oaiHarvesterClient.listRecords(oaiSource, token);
            processOAIHarvesterResponse(context, harvestRow, oaiSource, nextResponseDTO, toDate, repositoryIdentifier);
        }

    }

    private HarvestedCollection processRecords(Context context, HarvestedCollection harvestRow, List<Element> records,
        String repositoryIdentifier) {

        UUID collectionId = harvestRow.getCollection().getID();
        Date expirationDate = getExpirationDate();

        log.info("Found " + records.size() + " records to process");
        for (Element record : records) {

            // check for STOP interrupt from the scheduler
            if (HarvestScheduler.getInterrupt() == HarvestScheduler.HARVESTER_INTERRUPT_STOP) {
                throw new HarvestingException(
                    "Harvest process for " + collectionId + " interrupted by stopping the scheduler.");
            }

            // check for timeout
            if (expirationDate.before(new Date())) {
                throw new HarvestingException("runHarvest method timed out for collection " + collectionId);
            }

            try {

                processRecord(context, harvestRow, record, repositoryIdentifier);

                context.commit();
                harvestRow = context.reloadEntity(harvestRow);

            } catch (Exception ex) {
                log.error("An error occurs while process the record " + getItemIdentifier(record), ex);
            }

        }

        return harvestRow;
    }

    private void processRecord(Context context, HarvestedCollection harvestRow, Element record,
        String repositoryIdentifier) throws Exception {

        Collection targetCollection = harvestRow.getCollection();
        String itemOaiID = getItemIdentifier(record);

        HarvestedItem harvestedItem = harvestedItemService.findByOAIId(context, itemOaiID, targetCollection);
        Item item = harvestedItem != null ? harvestedItem.getItem() : null;

        if (item == null) {
            String searchParam = "OAI" + SPLIT + repositoryIdentifier + SPLIT + getMetadataIdentifier(record);
            item = itemSearchService.search(context, searchParam);
            if (item != null) {
                harvestedItem = harvestedItemService.create(context, item, itemOaiID);
            }
        }

        if (hasDeletedStatus(record)) {
            log.info("Item " + itemOaiID + " has been marked as deleted on the OAI server.");
            if (item != null) {
                collectionService.removeItem(context, targetCollection, item);
            }
            return;
        }

        Element oreREM = null;
        if (harvestRow.getHarvestType() > 1) {
            String OREPrefix = getOREPrefix(harvestRow.getOaiSource(), harvestRow);
            oreREM = getMetadataRecord(harvestRow.getOaiSource(), itemOaiID, OREPrefix).get(0);
        }

        context.turnOffAuthorisationSystem();

        if (item != null) {
            harvestedItem = updateItem(context, item, harvestedItem, harvestRow, record, oreREM, repositoryIdentifier);
        } else {
            harvestedItem = createItem(context, harvestRow, record, itemOaiID, oreREM, repositoryIdentifier);
            item = harvestedItem.getItem();
        }

        if (harvestRow.getHarvestType() > 1) {
            createOREBundle(context, item, oreREM);
        }

        harvestedItem.setHarvestDate(new Date());

        // Add provenance that this item was harvested via OAI
        String provenanceMsg = "Item created via OAI harvest from source: "
            + harvestRow.getOaiSource() + " on " + new DCDate(harvestedItem.getHarvestDate())
            + " (GMT).  Item's OAI Record identifier: " + harvestedItem.getOaiID();
        itemService.addMetadata(context, item, "dc", "description", "provenance", "en", provenanceMsg);

        itemService.update(context, item);
        harvestedItemService.update(context, harvestedItem);

        context.uncacheEntity(harvestedItem.getItem());
        context.uncacheEntity(harvestedItem);

        context.restoreAuthSystemState();
    }

    private HarvestedItem updateItem(Context context, Item item, HarvestedItem harvestedItem,
        HarvestedCollection harvestRow, Element record, Element oreREM, String repositoryIdentifier) throws Exception {

        String itemOaiID = harvestedItem.getOaiID();
        log.debug("Item " + item.getHandle() + " was found locally. Using it to harvest " + itemOaiID + ".");

        if (isLocalItemMoreRecent(harvestedItem, getHeader(record))) {
            log.info("Item " + item.getHandle() + " was harvested more recently than the last update time "
                + "reported by the OAI server; skipping.");
            return harvestedItem;
        }

        ingestItem(context, harvestRow, record, item, oreREM, repositoryIdentifier);

        return harvestedItem;
    }

    private HarvestedItem createItem(Context context, HarvestedCollection harvestRow, Element record,
        String itemOaiID, Element oreREM, String repositoryIdentifier) throws Exception {

        Collection targetCollection = harvestRow.getCollection();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, targetCollection, false);
        Item item = workspaceItem.getItem();

        HarvestedItem harvestedItem = harvestedItemService.create(context, item, itemOaiID);

        ingestItem(context, harvestRow, record, item, oreREM, repositoryIdentifier);

        // see if a handle can be extracted for the item
        String handle = extractHandle(item);

        if (handle != null) {
            DSpaceObject dso = handleService.resolveToObject(context, handle);
            if (dso != null) {
                throw new HarvestingException("Handle collision: attempted to re-assign handle '" + handle + "' "
                    + "to an incoming harvested item '" + harvestedItem.getOaiID() + "'.");
            }
        }

        try {
            item = installItemService.installItem(context, workspaceItem, handle);
        } catch (SQLException | IOException | AuthorizeException se) {
            workspaceItemService.deleteWrapper(context, workspaceItem);
            throw se;
        }

        context.uncacheEntity(workspaceItem);

        return harvestedItem;
    }

    private void ingestItem(Context context, HarvestedCollection harvestRow, Element record, Item item, Element oreREM,
        String repositoryIdentifier) throws Exception {

        String metadataConfig = harvestRow.getHarvestMetadataConfig();

        IngestionCrosswalk metadataElementCrosswalk = getIngestionCrosswalk(metadataConfig, repositoryIdentifier);

        List<Element> metadataElements = getMetadataElements(record);
        metadataElementCrosswalk.ingest(context, item, metadataElements, true);

        if (harvestRow.getHarvestType() == 3) {

            IngestionCrosswalk oreCrosswalk = getIngestionCrosswalk(this.ORESerialKey, repositoryIdentifier);

            List<Bundle> allBundles = item.getBundles();
            for (Bundle bundle : allBundles) {
                itemService.removeBundle(context, item, bundle);
            }

            oreCrosswalk.ingest(context, item, oreREM, true);
        }

    }

    private void createOREBundle(Context context, Item item, Element oreREM) throws Exception {

        List<Bundle> OREBundles = itemService.getBundles(item, "ORE");
        Bundle OREBundle = OREBundles.size() > 0 ? OREBundles.get(0) : bundleService.create(context, item, "ORE");

        XMLOutputter outputter = new XMLOutputter();
        String OREString = outputter.outputString(oreREM);
        ByteArrayInputStream OREStream = new ByteArrayInputStream(OREString.getBytes());

        Bitstream OREBitstream = bundleService.getBitstreamByName(OREBundle, "ORE.xml");

        if (OREBitstream != null) {
            bundleService.removeBitstream(context, OREBundle, OREBitstream);
        }

        OREBitstream = bitstreamService.create(context, OREBundle, OREStream);
        OREBitstream.setName(context, "ORE.xml");

        BitstreamFormat bf = bitstreamFormatService.guessFormat(context, OREBitstream);
        bitstreamService.setFormat(context, OREBitstream, bf);
        bitstreamService.update(context, OREBitstream);

        bundleService.addBitstream(context, OREBundle, OREBitstream);
        bundleService.update(context, OREBundle);
    }

    @SuppressWarnings("unchecked")
    private List<Element> getAllRecords(Document document) {
        return document.getRootElement().getChild("ListRecords", OAI_NS).getChildren("record", OAI_NS);
    }

    private String getOREPrefix(String oaiSource, HarvestedCollection harvestRow) {
        String OREPrefix = oaiHarvesterClient.resolveNamespaceToPrefix(oaiSource, ORESerialURI);
        if (OREPrefix == null && harvestRow.getHarvestType() != HarvestedCollection.TYPE_DMD) {
            String message = "The OAI server doesn't support ORE dissemination in the format: " + ORESerialURI;
            log.error(message);
            throw new HarvestingException(message);
        }
        return OREPrefix;
    }

    private String getDescriptiveMetadataFormat(String oaiSource, String metadataKey) {

        Namespace metadataNS = NamespaceUtils.getDMDNamespace(metadataKey);

        if (metadataNS == null) {
            String message = format("No matching metadata namespace found for '%s', see oai.cfg property '%s'",
                metadataKey, METADATA_FORMATS_KEY);
            log.error(message);
            throw new HarvestingException(message);
        }

        String metadataURI = metadataNS.getURI();
        String descriptiveMetadataFormat = oaiHarvesterClient.resolveNamespaceToPrefix(oaiSource, metadataURI);
        if (descriptiveMetadataFormat == null) {
            String message = "The OAI server does not support this metadata format: " + metadataURI;
            log.error(message);
            throw new HarvestingException(message);
        }

        return descriptiveMetadataFormat;
    }

    private String getRootExceptionMessage(Exception ex) {
        String message = ExceptionUtils.getRootCauseMessage(ex);
        return StringUtils.isNotEmpty(message) ? message : "Unknown error";
    }

    private HarvestedCollection setCollectionBusy(Context context, HarvestedCollection harvestRow, Date startDate) {
        harvestRow.setHarvestMessage("Collection harvesting is initializing...");
        harvestRow.setHarvestStatus(STATUS_BUSY);
        harvestRow.setHarvestStartTime(startDate);
        return updateHarvestRow(context, harvestRow);
    }

    private HarvestedCollection setCollectionReady(Context context, HarvestedCollection harvestRow, String message,
        Date lastDate) {
        harvestRow.setHarvestMessage(message);
        harvestRow.setHarvestStatus(STATUS_READY);
        harvestRow.setLastHarvested(lastDate);
        return updateHarvestRow(context, harvestRow);
    }

    private HarvestedCollection setCollectionInError(Context context, HarvestedCollection harvestRow, String message) {
        harvestRow.setHarvestMessage(message);
        harvestRow.setHarvestStatus(STATUS_UNKNOWN_ERROR);
        return updateHarvestRow(context, harvestRow);
    }

    private HarvestedCollection updateHarvestRow(Context context, HarvestedCollection harvestRow) {
        try {
            harvestedCollectionService.update(context, harvestRow);
            context.commit();
            return context.reloadEntity(harvestRow);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private Date getExpirationDate() {
        int expirationInterval = configurationService.getIntProperty("oai.harvester.threadTimeout");
        if (expirationInterval == 0) {
            expirationInterval = 24;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, expirationInterval);
        Date expirationTime = calendar.getTime();
        return expirationTime;
    }

    private String getDateFormat(String oaiSource) {
        String dateFormat = getDateGranularityFromServer(oaiSource);
        dateFormat = StringUtils.isEmpty(dateFormat) ? "yyyy-MM-dd'T'HH:mm:ss'Z'" : dateFormat;
        return dateFormat;
    }

    private Element getHeader(Element record) {
        return record.getChild("header", OAI_NS);
    }

    private boolean hasDeletedStatus(Element record) {
        Element header = getHeader(record);
        return header.getAttribute("status") != null && header.getAttribute("status").getValue().equals("deleted");
    }

    @SuppressWarnings("unchecked")
    private List<Element> getMetadataElements(Element record) {
        return record.getChild("metadata", OAI_NS).getChildren();
    }

    /**
     * Compare last-harvest on the item versus the last time the item was updated on
     * the OAI provider side. If ours is more recent, forgot this item, since it's
     * probably a left-over from a previous harvesting attempt
     * @param  harvestedItem stored harvested item
     * @param  header        the header element
     * @return               true if the local item last harvest is after the OAI
     *                       provided datestamp, false otherwise
     */
    private boolean isLocalItemMoreRecent(HarvestedItem harvestedItem, Element header) {
        Date OAIDatestamp = Utils.parseISO8601Date(header.getChildText("datestamp", OAI_NS));
        Date itemLastHarvest = harvestedItem.getHarvestDate();
        return itemLastHarvest != null && OAIDatestamp.before(itemLastHarvest);
    }

    private IngestionCrosswalk getIngestionCrosswalk(String name, String repositoryIdentifier) {
        Object crosswalk = pluginService.getNamedPlugin(IngestionCrosswalk.class, name);
        if (crosswalk == null) {
            throw new IllegalArgumentException("No IngestionCrosswalk found by name: " + name);
        }

        if (crosswalk instanceof CERIFIngestionCrosswalk) {
            ((CERIFIngestionCrosswalk) crosswalk).setIdPrefix(GENERATE + "OAI" + SPLIT + repositoryIdentifier + SPLIT);
        }

        return (IngestionCrosswalk) crosswalk;
    }

    private String getItemIdentifier(Element record) {
        return getHeader(record).getChild("identifier", OAI_NS).getText();
    }

    private String getMetadataIdentifier(Element record) {
        List<Element> metadataElements = getMetadataElements(record);
        if (CollectionUtils.isEmpty(metadataElements)) {
            return null;
        }

        return metadataElements.get(0).getAttributeValue("id");
    }

    /**
     * Scan an item's metadata, looking for the value "identifier.*". If it meets the parameters that identify it as
     * valid handle
     * as set in dspace.cfg (harvester.acceptedHandleServer and harvester.rejectedHandlePrefix), use that handle
     * instead of
     * minting a new one.
     *
     * @param item a newly created, but not yet installed, DSpace Item
     * @return null or the handle to be used.
     */
    private String extractHandle(Item item) {
        String[] acceptedHandleServers = configurationService.getArrayProperty("oai.harvester.acceptedHandleServer");
        if (acceptedHandleServers == null) {
            acceptedHandleServers = new String[] {"hdl.handle.net"};
        }

        String[] rejectedHandlePrefixes = configurationService.getArrayProperty("oai.harvester.rejectedHandlePrefix");
        if (rejectedHandlePrefixes == null) {
            rejectedHandlePrefixes = new String[] {"123456789"};
        }

        List<MetadataValue> values = itemService.getMetadata(item, "dc", "identifier", Item.ANY, Item.ANY);

        if (CollectionUtils.isEmpty(values)) {
            return null;
        }

        for (MetadataValue value : values) {
            //     0   1       2         3   4
            //   http://hdl.handle.net/1234/12
            String[] urlPieces = value.getValue().split("/");
            if (urlPieces.length != 5) {
                continue;
            }

            for (String server : acceptedHandleServers) {
                if (urlPieces[2].equals(server)) {
                    for (String prefix : rejectedHandlePrefixes) {
                        if (!urlPieces[3].equals(prefix)) {
                            return urlPieces[3] + "/" + urlPieces[4];
                        }
                    }

                }
            }
        }

        return null;
    }


    /**
     * Process a date, converting it to RFC3339 format, setting the timezone to UTC
     * and subtracting time padding from the config file.
     *
     * @param  date       source Date
     * @param  dateFormat the date format
     * @return            a string in the format 'yyyy-mm-ddThh:mm:ssZ' and
     *                    converted to UTC timezone
     */
    private String formatDate(Date date, String dateFormat) {
        int timePad = configurationService.getIntProperty("oai.harvester.timePadding");

        if (timePad == 0) {
            timePad = 120;
        }

        return formatDate(date, timePad, dateFormat);
    }

    /**
     * Process a date, converting it to RFC3339 format, setting the timezone to UTC
     * and subtracting time padding from the config file.
     *
     * @param  date       source Date
     * @param  secondsPad number of seconds to subtract from the date
     * @param  dateFormat the date format
     * @return            a string in the specified format and converted to UTC
     *                    timezone
     */
    private String formatDate(Date date, int secondsPad, String dateFormat) {

        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, -1 * secondsPad);
        date = calendar.getTime();

        return formatter.format(date);
    }

    private String getRepositoryIdentifier(HarvestedCollection harvestRow) {

        String oaiSource = harvestRow.getOaiSource();
        OAIHarvesterResponseDTO iden = oaiHarvesterClient.identify(oaiSource);

        return Optional.ofNullable(iden.getDocument())
            .map( document -> document.getRootElement().getChild("Identify", OAI_NS))
            .map(identifyElement -> identifyElement.getChild("description", OAI_NS))
            .map(description -> description.getChild("oai-identifier", OAI_IDENTIFIER_NS))
            .map(oaiIdentifier -> oaiIdentifier.getChild("repositoryIdentifier", OAI_IDENTIFIER_NS))
            .map(repositoryIdentifier -> repositoryIdentifier.getText())
            .orElse(valueOf(harvestRow.getID()));

    }

    /**
     * Query OAI-PMH server for the granularity of its datestamps.
     */
    private String getDateGranularityFromServer(String oaiSource) {
        OAIHarvesterResponseDTO iden = oaiHarvesterClient.identify(oaiSource);
        Element identifyElement = iden.getDocument().getRootElement().getChild("Identify", OAI_NS);
        if (identifyElement == null) {
            return null;
        }
        Element granularityElement = identifyElement.getChild("granularity", OAI_NS);
        if (granularityElement == null) {
            return null;
        }
        return granularityElement.getText().replace("T", "'T'").replace("Z", "'Z'").replace("DD", "dd");
    }

    /**
     * Query the OAI-PMH provider for a specific metadata record.
     *
     * @param oaiSource      the address of the OAI-PMH provider
     * @param itemOaiId      the OAI identifier of the target item
     * @param metadataPrefix the OAI metadataPrefix of the desired metadata
     * @return list of JDOM elements corresponding to the metadata entries in the located record.
     * @throws IOException                  A general class of exceptions produced by failed or interrupted I/O
     *                                      operations.
     * @throws ParserConfigurationException XML parsing error
     * @throws SAXException                 if XML processing error
     * @throws TransformerException         if XML transformer error
     * @throws HarvestingException          if harvesting error
     */
    @SuppressWarnings("unchecked")
    private List<Element> getMetadataRecord(String oaiSource, String itemOaiId, String metadataPrefix)
        throws IOException, ParserConfigurationException, SAXException, TransformerException, HarvestingException {
        OAIHarvesterResponseDTO responseDTO = oaiHarvesterClient.getRecord(oaiSource, itemOaiId, metadataPrefix);
        if (responseDTO.hasErrors()) {
            throw new HarvestingException(
                "OAI server returned the following errors during getDescMD execution: " + responseDTO.getErrors());
        }

        Element root = responseDTO.getDocument().getRootElement();
        return root.getChild("GetRecord", OAI_NS).getChild("record", OAI_NS).getChild("metadata", OAI_NS).getChildren();
    }

    /**
     * Return all available metadata formats
     *
     * @return a list containing a map for each supported  metadata format
     */
    public static List<Map<String,String>> getAvailableMetadataFormats() {
        List<Map<String,String>> configs = new ArrayList<>();
        String metaString = "oai.harvester.metadataformats.";
        Enumeration<String> pe = Collections.enumeration(
            DSpaceServicesFactory.getInstance().getConfigurationService()
                                 .getPropertyKeys("oai.harvester.metadataformats")
        );
        while (pe.hasMoreElements()) {
            String key = (String) pe.nextElement();
            String metadataString = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(key);

            String id = key.substring(metaString.length());
            String label;
            String namespace = "";

            if (metadataString.indexOf(',') != -1) {
                label = metadataString.substring(metadataString.indexOf(',') + 2);
                namespace = metadataString.substring(0, metadataString.indexOf(','));
            } else {
                label = id + "(" + metadataString + ")";
            }

            Map<String,String> config = new HashMap<>();
            config.put("id", id);
            config.put("label", label);
            config.put("namespace", namespace);

            configs.add(config);
        }

        return configs;
    }

    /**
     * Generate and send an email to the administrator. Prompted by errors
     * encountered during harvesting.
     *
     * @param status the current status of the collection, usually
     *               HarvestedCollection.STATUS_OAI_ERROR or
     *               HarvestedCollection.STATUS_UNKNOWN_ERROR
     * @param ex     the Exception that prompted this action
     */
    private void alertAdmin(Collection targetCollection, int status, Exception ex) {
        try {
            String recipient = configurationService.getProperty("alert.recipient");

            if (StringUtils.isNotBlank(recipient)) {
                Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), "harvesting_error"));
                email.addRecipient(recipient);
                email.addArgument(targetCollection.getID());
                email.addArgument(new Date());
                email.addArgument(status);

                String stackTrace;

                if (ex != null) {
                    email.addArgument(ex.getMessage());

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                } else {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        } catch (Exception e) {
            log.warn("Unable to send email alert", e);
        }
    }

    public OAIHarvesterClient getOaiHarvesterClient() {
        return oaiHarvesterClient;
    }

    public void setOaiHarvesterClient(OAIHarvesterClient oaiHarvesterClient) {
        this.oaiHarvesterClient = oaiHarvesterClient;
    }

}
