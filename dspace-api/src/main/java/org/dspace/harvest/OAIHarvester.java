/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

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
import java.util.Set;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
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
import org.dspace.content.crosswalk.CrosswalkException;
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
import org.jdom.input.DOMBuilder;
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

    private DOMBuilder domBuilder = new DOMBuilder();

    private Namespace ORESerialNS;

    private String ORESerialKey;

    /**
     * Set the ORE options.
     */
    @PostConstruct
    private void setup() {
        Namespace ORESerializationNamespace = NamespaceUtils.getORENamespace();
        ORESerialNS = Namespace.getNamespace(ORESerializationNamespace.getURI());
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
    public void runHarvest(Context context, HarvestedCollection harvestRow)
        throws SQLException, IOException, AuthorizeException {

        if (harvestRow == null || !harvestedCollectionService.isHarvestable(harvestRow)) {
            throw new IllegalArgumentException("Provided collection is not set up for harvesting");
        }

        Collection targetCollection = harvestRow.getCollection();

        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.BATCH_EDIT);

        // figure out the relevant parameters
        String oaiSource = harvestRow.getOaiSource();
        String oaiSetId = harvestRow.getOaiSetId();

        // Set the metadata options
        String metadataKey = harvestRow.getHarvestMetadataConfig();
        Namespace metadataNS = NamespaceUtils.getDMDNamespace(metadataKey);

        if (metadataNS == null) {
            log.error(
                "No matching metadata namespace found for \"" + metadataKey + "\", see oai.cfg option \"oai.harvester" +
                    ".metadataformats.{MetadataKey} = {MetadataNS},{Display Name}\"");
            throw new IllegalArgumentException("Metadata declaration not found");
        }

        //If we have all selected then make sure that we do not include a set filter
        if ("all".equals(oaiSetId)) {
            oaiSetId = null;
        }

        Date lastHarvestDate = harvestRow.getHarvestDate();
        String fromDate = null;
        if (lastHarvestDate != null) {
            fromDate = processDate(harvestRow.getHarvestDate());
        }

        long totalListSize = 0;
        long currentRecord = 0;
        Date startTime = new Date();
        String toDate = processDate(startTime, 0);

        String dateGranularity;

        try {
            // obtain the desired descriptive metadata format and verify that the OAI server actually provides it
            // do the same thing for ORE, which should be encoded in Atom and carry its namespace
            String descMDPrefix = null;
            String OREPrefix;
            dateGranularity = oaiGetDateGranularity(oaiSource);
            if (fromDate != null) {
                fromDate = fromDate.substring(0, dateGranularity.length());
            }
            toDate = toDate.substring(0, dateGranularity.length());

            descMDPrefix = oaiHarvesterClient.resolveNamespaceToPrefix(oaiSource, metadataNS.getURI());
            OREPrefix = oaiHarvesterClient.resolveNamespaceToPrefix(oaiSource, ORESerialNS.getURI());
            if (descMDPrefix == null) {
                log.error("The OAI server does not support this metadata format");
                throw new HarvestingException(
                    "The OAI server does not support this metadata format: " + metadataNS.getURI());
            }
            if (OREPrefix == null && harvestRow.getHarvestType() != HarvestedCollection.TYPE_DMD) {
                throw new HarvestingException(
                    "The OAI server does not support ORE dissemination in the configured serialization format: " +
                        ORESerialNS
                            .getURI());
            }

            Document oaiResponse = null;
            Element root = null;
            String resumptionToken;

            // set the status indicating the collection is currently being processed
            harvestRow.setHarvestStatus(HarvestedCollection.STATUS_BUSY);
            harvestRow.setHarvestMessage("Collection harvesting is initializing...");
            harvestRow.setHarvestStartTime(startTime);
            harvestedCollectionService.update(context, harvestRow);

            context.commit();
            harvestRow = context.reloadEntity(harvestRow);

            // expiration timer starts
            int expirationInterval = configurationService.getIntProperty("oai.harvester.threadTimeout");
            if (expirationInterval == 0) {
                expirationInterval = 24;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(Calendar.HOUR, expirationInterval);
            Date expirationTime = calendar.getTime();

            // main loop to keep requesting more objects until we're done
            List<Element> records;

            OAIHarvesterResponseDTO responseDTO = oaiHarvesterClient.listRecords(oaiSource, fromDate,
                toDate, oaiSetId, descMDPrefix);

            while (responseDTO != null) {
                records = new ArrayList<Element>();
                oaiResponse = domBuilder.build(responseDTO.getDocument());

                if (responseDTO.hasErrors()) {
                    Set<String> errorSet = responseDTO.getErrors();
                    if (errorSet.contains("noRecordsMatch")) {
                        log.info("noRecordsMatch: OAI server did not contain any updates");
                        harvestRow.setHarvestStartTime(new Date());
                        harvestRow.setHarvestMessage("OAI server did not contain any updates");
                        harvestRow.setHarvestStatus(HarvestedCollection.STATUS_READY);
                        harvestedCollectionService.update(context, harvestRow);
                        return;
                    } else {
                        throw new HarvestingException(errorSet.toString());
                    }
                }

                root = oaiResponse.getRootElement();
                records.addAll(root.getChild("ListRecords", OAI_NS).getChildren("record", OAI_NS));
                Element resumptionElement = root.getChild("ListRecords", OAI_NS)
                    .getChild("resumptionToken", OAI_NS);
                if (resumptionElement != null && resumptionElement.getAttribute("completeListSize") != null) {
                    String value = resumptionElement.getAttribute("completeListSize").getValue();
                    if (StringUtils.isNotBlank(value)) {
                        totalListSize = Long.parseLong(value);
                    }
                }

                // Process the obtained records
                if (records != null && records.size() > 0) {
                    log.info("Found " + records.size() + " records to process");
                    for (Element record : records) {
                        // check for STOP interrupt from the scheduler
                        if (HarvestScheduler.getInterrupt() == HarvestScheduler.HARVESTER_INTERRUPT_STOP) {
                            throw new HarvestingException("Harvest process for " + targetCollection
                                .getID() + " interrupted by stopping the scheduler.");
                        }
                        // check for timeout
                        if (expirationTime.before(new Date())) {
                            throw new HarvestingException(
                                "runHarvest method timed out for collection " + targetCollection.getID());
                        }

                        currentRecord++;

                        processRecord(context, harvestRow, record, metadataKey, OREPrefix);
                        context.commit();
                        harvestRow = context.reloadEntity(harvestRow);
                    }
                }

                // keep going if there are more records to process
                resumptionToken = responseDTO.getResumptionToken();
                if (StringUtils.isEmpty(resumptionToken)) {
                    responseDTO = null;
                } else {
                    responseDTO = oaiHarvesterClient.listRecords(oaiSource, resumptionToken);
                }

                context.turnOffAuthorisationSystem();
                try {
                    collectionService.update(context, targetCollection);

                    harvestRow.setHarvestMessage(String
                                                     .format("Collection is currently being harvested (item %d of %d)",
                                                             currentRecord, totalListSize));
                    harvestedCollectionService.update(context, harvestRow);
                } finally {
                    //In case of an exception, make sure to restore our authentication state to the previous state
                    context.restoreAuthSystemState();
                }

                context.dispatchEvents();
                context.commit();
            }
        } catch (HarvestingException hex) {
            log.error("Harvesting error occurred while processing an OAI record: " + hex.getMessage(), hex);
            harvestRow.setHarvestMessage("Error occurred while processing an OAI record");

            // if the last status is also an error, alert the admin
            if (harvestRow.getHarvestMessage().contains("Error")) {
                alertAdmin(targetCollection, HarvestedCollection.STATUS_OAI_ERROR, hex);
            }
            harvestRow.setHarvestStatus(HarvestedCollection.STATUS_OAI_ERROR);
            harvestedCollectionService.update(context, harvestRow);
            context.complete();
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            harvestRow.setHarvestMessage("Unknown error occurred while generating an OAI response");
            harvestRow.setHarvestStatus(HarvestedCollection.STATUS_UNKNOWN_ERROR);
            harvestedCollectionService.update(context, harvestRow);
            alertAdmin(targetCollection, HarvestedCollection.STATUS_UNKNOWN_ERROR, ex);
            log.error("Error occurred while generating an OAI response: " + ex.getMessage() + " " + ex.getCause(), ex);
            context.complete();
            return;
        } finally {
            harvestedCollectionService.update(context, harvestRow);
            context.turnOffAuthorisationSystem();
            collectionService.update(context, targetCollection);
            context.restoreAuthSystemState();
        }

        // If we got to this point, it means the harvest was completely successful
        Date finishTime = new Date();
        long timeTaken = finishTime.getTime() - startTime.getTime();
        harvestRow.setHarvestStartTime(startTime);
        harvestRow.setHarvestMessage("Harvest from " + oaiSource + " successful");
        harvestRow.setHarvestStatus(HarvestedCollection.STATUS_READY);
        log.info(
            "Harvest from " + oaiSource + " successful. The process took " + timeTaken + " milliseconds. Harvested "
                + currentRecord + " items.");
        harvestedCollectionService.update(context, harvestRow);

        context.setMode(originalMode);
    }

    private void processRecord(Context context, HarvestedCollection harvestRow, Element record, String metadataKey,
        String OREPrefix)
        throws SQLException, AuthorizeException, IOException, CrosswalkException, HarvestingException,
        ParserConfigurationException, SAXException, TransformerException {
        WorkspaceItem wi = null;

        Collection targetCollection = harvestRow.getCollection();

        // grab the oai identifier
        String itemOaiID = record.getChild("header", OAI_NS).getChild("identifier", OAI_NS).getText();
        Element header = record.getChild("header", OAI_NS);

        // look up the item corresponding to the OAI identifier
        Item item = harvestedItemService.getItemByOAIId(context, itemOaiID, targetCollection);

        // Make sure the item hasn't been deleted in the mean time
        if (header.getAttribute("status") != null && header.getAttribute("status").getValue().equals("deleted")) {
            log.info("Item " + itemOaiID + " has been marked as deleted on the OAI server.");
            if (item != null) {
                collectionService.removeItem(context, targetCollection, item);
            }

            context.restoreAuthSystemState();
            return;
        }

        // If we are only harvesting descriptive metadata, the record should already contain all we need
        List<Element> descMD = record.getChild("metadata", OAI_NS).getChildren();
        IngestionCrosswalk MDxwalk = (IngestionCrosswalk) pluginService.getNamedPlugin(IngestionCrosswalk.class,
            metadataKey);

        // Otherwise, obtain the ORE ReM and initiate the ORE crosswalk
        IngestionCrosswalk ORExwalk = null;
        Element oreREM = null;
        if (harvestRow.getHarvestType() > 1) {
            oreREM = getMDrecord(harvestRow.getOaiSource(), itemOaiID, OREPrefix).get(0);
            ORExwalk = (IngestionCrosswalk) pluginService.getNamedPlugin(IngestionCrosswalk.class, this.ORESerialKey);
        }

        // Ignore authorization
        context.turnOffAuthorisationSystem();

        HarvestedItem hi;

        // found an item so we modify
        if (item != null) {
            log.debug("Item " + item.getHandle() + " was found locally. Using it to harvest " + itemOaiID + ".");

            // FIXME: check for null pointer if for some odd reason we don't have a matching hi
            hi = harvestedItemService.find(context, item);

            // Compare last-harvest on the item versus the last time the item was updated on the OAI provider side
            // If ours is more recent, forgo this item, since it's probably a left-over from a previous harvesting
            // attempt
            Date OAIDatestamp = Utils.parseISO8601Date(header.getChildText("datestamp", OAI_NS));
            Date itemLastHarvest = hi.getHarvestDate();
            if (itemLastHarvest != null && OAIDatestamp.before(itemLastHarvest)) {
                log.info("Item " + item
                    .getHandle() + " was harvested more recently than the last update time reported by the OAI " +
                             "server; skipping.");
                return;
            }

            // Otherwise, clear and re-import the metadata and bitstreams
            itemService.clearMetadata(context, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            if (descMD.size() == 1) {
                MDxwalk.ingest(context, item, descMD.get(0), true);
            } else {
                MDxwalk.ingest(context, item, descMD, true);
            }

            // Import the actual bitstreams
            if (harvestRow.getHarvestType() == 3) {
                log.info("Running ORE ingest on: " + item.getHandle());

                List<Bundle> allBundles = item.getBundles();
                for (Bundle bundle : allBundles) {
                    itemService.removeBundle(context, item, bundle);
                }
                ORExwalk.ingest(context, item, oreREM, true);
            }
        } else {
            // NOTE: did not find, so we create (presumably, there will never be a case where an item already
            // exists in a harvest collection but does not have an OAI_id)

            wi = workspaceItemService.create(context, targetCollection, false);
            item = wi.getItem();

            hi = harvestedItemService.create(context, item, itemOaiID);
            //item.setOaiID(itemOaiID);

            if (descMD.size() == 1) {
                MDxwalk.ingest(context, item, descMD.get(0), true);
            } else {
                MDxwalk.ingest(context, item, descMD, true);
            }

            if (harvestRow.getHarvestType() == 3) {
                ORExwalk.ingest(context, item, oreREM, true);
            }

            // see if a handle can be extracted for the item
            String handle = extractHandle(item);

            if (handle != null) {
                DSpaceObject dso = handleService.resolveToObject(context, handle);
                if (dso != null) {
                    throw new HarvestingException(
                        "Handle collision: attempted to re-assign handle '" + handle + "' to an incoming harvested " +
                            "item '" + hi
                            .getOaiID() + "'.");
                }
            }

            try {
                item = installItemService.installItem(context, wi, handle);
                // item = InstallItem.installItem(context, wi);
            } catch (SQLException | IOException | AuthorizeException se) {
                // clean up the workspace item if something goes wrong before
                workspaceItemService.deleteWrapper(context, wi);
                throw se;
            }
        }

        // Now create the special ORE bundle and drop the ORE document in it
        if (harvestRow.getHarvestType() == 2 || harvestRow.getHarvestType() == 3) {
            Bundle OREBundle = null;
            List<Bundle> OREBundles = itemService.getBundles(item, "ORE");
            Bitstream OREBitstream = null;

            if (OREBundles.size() > 0) {
                OREBundle = OREBundles.get(0);
            } else {
                OREBundle = bundleService.create(context, item, "ORE");
            }

            XMLOutputter outputter = new XMLOutputter();
            String OREString = outputter.outputString(oreREM);
            ByteArrayInputStream OREStream = new ByteArrayInputStream(OREString.getBytes());

            OREBitstream = bundleService.getBitstreamByName(OREBundle, "ORE.xml");

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

        //item.setHarvestDate(new Date());
        hi.setHarvestDate(new Date());

        // Add provenance that this item was harvested via OAI
        String provenanceMsg = "Item created via OAI harvest from source: "
            + harvestRow.getOaiSource() + " on " + new DCDate(hi.getHarvestDate())
            + " (GMT).  Item's OAI Record identifier: " + hi.getOaiID();
        itemService.addMetadata(context, item, "dc", "description", "provenance", "en", provenanceMsg);

        itemService.update(context, item);
        harvestedItemService.update(context, hi);


        //Clear the context cache
        context.uncacheEntity(wi);
        context.uncacheEntity(hi);
        context.uncacheEntity(item);

        // Stop ignoring authorization
        context.restoreAuthSystemState();
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

        if (values.size() > 0 && acceptedHandleServers != null) {
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
        }

        return null;
    }


    /**
     * Process a date, converting it to RFC3339 format, setting the timezone to UTC and subtracting time padding
     * from the config file.
     *
     * @param date source Date
     * @return a string in the format 'yyyy-mm-ddThh:mm:ssZ' and converted to UTC timezone
     */
    private String processDate(Date date) {
        Integer timePad = configurationService.getIntProperty("oai.harvester.timePadding");

        if (timePad == 0) {
            timePad = 120;
        }

        return processDate(date, timePad);
    }

    /**
     * Process a date, converting it to RFC3339 format, setting the timezone to UTC and subtracting time padding
     * from the config file.
     *
     * @param date       source Date
     * @param secondsPad number of seconds to subtract from the date
     * @return a string in the format 'yyyy-mm-ddThh:mm:ssZ' and converted to UTC timezone
     */
    private String processDate(Date date, int secondsPad) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, -1 * secondsPad);
        date = calendar.getTime();

        return formatter.format(date);
    }


    /**
     * Query OAI-PMH server for the granularity of its datestamps.
     */
    private String oaiGetDateGranularity(String oaiSource) {
        OAIHarvesterResponseDTO iden = oaiHarvesterClient.identify(oaiSource);
        return iden.getDocument().getElementsByTagNameNS(OAI_NS.getURI(), "granularity").item(0).getTextContent();
    }

    /**
     * Generate and send an email to the administrator. Prompted by errors encountered during harvesting.
     *
     * @param status the current status of the collection, usually HarvestedCollection.STATUS_OAI_ERROR or
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
    private List<Element> getMDrecord(String oaiSource, String itemOaiId, String metadataPrefix)
        throws IOException, ParserConfigurationException, SAXException, TransformerException, HarvestingException {
        OAIHarvesterResponseDTO responseDTO = oaiHarvesterClient.getRecord(oaiSource, itemOaiId, metadataPrefix);
        if (responseDTO.hasErrors()) {
            throw new HarvestingException(
                "OAI server returned the following errors during getDescMD execution: " + responseDTO.getErrors());
        }

        Document record = domBuilder.build(responseDTO.getDocument());
        Element root = record.getRootElement();

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

    public OAIHarvesterClient getOaiHarvesterClient() {
        return oaiHarvesterClient;
    }

    public void setOaiHarvesterClient(OAIHarvesterClient oaiHarvesterClient) {
        this.oaiHarvesterClient = oaiHarvesterClient;
    }

}
