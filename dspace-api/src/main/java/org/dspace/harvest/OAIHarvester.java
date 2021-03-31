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
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.dspace.app.harvest.Harvest.LOG_DELIMITER;
import static org.dspace.app.harvest.Harvest.LOG_PREFIX;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;
import static org.dspace.content.Item.ANY;
import static org.dspace.content.MetadataSchemaEnum.CRIS;
import static org.dspace.harvest.HarvestedCollection.STATUS_BUSY;
import static org.dspace.harvest.HarvestedCollection.STATUS_READY;
import static org.dspace.harvest.HarvestedCollection.STATUS_RETRY;
import static org.dspace.harvest.model.OAIHarvesterAction.ADDITION;
import static org.dspace.harvest.model.OAIHarvesterAction.DELETION;
import static org.dspace.harvest.model.OAIHarvesterAction.NONE;
import static org.dspace.harvest.model.OAIHarvesterAction.UPDATE;
import static org.dspace.harvest.service.OAIHarvesterClient.OAI_IDENTIFIER_NS;
import static org.dspace.harvest.util.NamespaceUtils.METADATA_FORMATS_KEY;
import static org.dspace.util.ExceptionMessageUtils.getRootMessage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.service.ItemSearchService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
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
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.core.service.PluginService;
import org.dspace.handle.service.HandleService;
import org.dspace.harvest.model.OAIHarvesterAction;
import org.dspace.harvest.model.OAIHarvesterOptions;
import org.dspace.harvest.model.OAIHarvesterReport;
import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.dspace.harvest.model.OAIHarvesterValidationResult;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.dspace.harvest.service.OAIHarvesterEmailSender;
import org.dspace.harvest.service.OAIHarvesterValidator;
import org.dspace.harvest.util.NamespaceUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.ExceptionMessageUtils;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.ValidationService;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
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

    @Autowired
    private ValidationService validationService;

    @Autowired
    private WorkflowService<XmlWorkflowItem> workflowService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private OAIHarvesterValidator oaiHarvesterValidator;

    @Autowired
    private OAIHarvesterEmailSender oaiHarvesterEmailSender;

    /**
     * Performs a harvest cycle on this collection. This will query the remote
     * OAI-PMH provider, check for updates since last harvest, and ingest the
     * returned items.
     */
    public void runHarvest(Context context, HarvestedCollection harvestRow, OAIHarvesterOptions options) {

        context.setMode(Context.Mode.BATCH_EDIT);

        Date toDate = new Date();

        try {

            if (harvestRow == null || !harvestedCollectionService.isHarvestable(harvestRow)) {
                throw new HarvestingException("Provided collection is not set up for harvesting");
            }

            Date fromDate = harvestRow.getLastHarvestDate();
            if (isForceSynchronization(harvestRow.getCollection(), options)) {
                fromDate = null;
            }

            harvestRow = setBusyStatus(context, harvestRow, toDate);

            OAIHarvesterReport report = startHarvest(context, harvestRow, fromDate, toDate, options);

            if (report.noRecordImportFails()) {
                setReadyStatus(context, harvestRow, getReportMessage(report), toDate);
            } else {
                setRetryStatus(context, harvestRow, getReportMessage(report));
            }

            if (report.hasErrors()) {
                oaiHarvesterEmailSender.notifyCompletionWithErrors(getEmailRecipient(harvestRow), harvestRow, report);
            }

        } catch (NoRecordsMatchException nrme) {
            setReadyStatus(context, harvestRow, nrme.getMessage(), toDate);
            log.info(nrme.getMessage());
        } catch (Exception ex) {
            String message = formatErrorMessage(ex);
            log.error(message, ex);
            if (harvestRow != null) {
                setRetryStatus(context, harvestRow, message);
                oaiHarvesterEmailSender.notifyFailure(getEmailRecipient(harvestRow), harvestRow, ex);
            }
        }

    }

    private OAIHarvesterReport startHarvest(Context context, HarvestedCollection harvestRow, Date fromDate,
        Date toDate, OAIHarvesterOptions options) {

        String oaiSource = harvestRow.getOaiSource();

        Document identifyDocument = identify(oaiSource);

        String descriptiveMetadataFormat = getDescriptiveMetadataFormat(harvestRow);

        String dateFormat = getDateFormat(oaiSource, identifyDocument);
        String fromDateAsString = fromDate != null ? formatDate(fromDate, dateFormat) : null;
        String toDateAsString = formatDate(toDate, 0, dateFormat);

        String repositoryId = getRepositoryIdentifier(harvestRow, identifyDocument);

        OAIHarvesterResponseDTO responseDTO = oaiHarvesterClient.listRecords(oaiSource, fromDateAsString,
            toDateAsString, harvestRow.getOaiSetId(), descriptiveMetadataFormat);

        int totalRecordSize = getTotalRecordSize(responseDTO.getDocument());
        log.info("Found " + totalRecordSize + " records to harvest");

        OAIHarvesterReport report = new OAIHarvesterReport(totalRecordSize);

        processOAIHarvesterResponse(context, harvestRow, responseDTO, toDate, repositoryId, report, options);

        return report;

    }

    private void processOAIHarvesterResponse(Context context, HarvestedCollection harvestRow,
        OAIHarvesterResponseDTO responseDTO, Date toDate, String repositoryId, OAIHarvesterReport report,
        OAIHarvesterOptions options) {

        while (responseDTO != null) {

            if (responseDTO.hasErrors()) {
                handleResponseErrors(responseDTO.getErrors());
                return;
            }

            List<Element> records = getAllRecords(responseDTO.getDocument());

            // Process the obtained records
            harvestRow = processRecords(context, harvestRow, records, repositoryId, report, options);

            // keep going if there are more records to process
            String token = responseDTO.getResumptionToken();
            responseDTO = isNotEmpty(token) ? oaiHarvesterClient.listRecords(harvestRow.getOaiSource(), token) : null;

        }

    }

    private HarvestedCollection processRecords(Context context, HarvestedCollection harvestRow, List<Element> records,
        String repositoryId, OAIHarvesterReport report, OAIHarvesterOptions options) {

        UUID collectionId = harvestRow.getCollection().getID();
        Date expirationDate = getExpirationDate();

        log.info("Found " + records.size() + " records to process");
        for (Element record : records) {

            // check for STOP interrupt from the scheduler
            if (HarvestScheduler.getInterrupt() == HarvestScheduler.HARVESTER_INTERRUPT_STOP) {
                throw new HarvestingException(
                    "Harvest process for " + collectionId + " interrupted by stopping the scheduler."
                        + getReportMessage(report));
            }

            // check for timeout
            if (expirationDate.before(new Date())) {
                throw new HarvestingException(
                    "Harvesting timed out for collection " + collectionId + "." + getReportMessage(report));
            }

            Long startTimestamp = System.currentTimeMillis();

            try {

                processRecord(context, harvestRow, record, repositoryId, options, startTimestamp, report);

                harvestRow.setHarvestMessage(formatIntermediateMessage(report));
                harvestRow = updateHarvestRow(context, harvestRow);

                context.commit();

                harvestRow = reloadEntity(context, harvestRow);

                report.incrementSuccessCount();

            } catch (Exception ex) {
                log.error("An error occurs while process the record " + getItemIdentifier(record), ex);
                report.addError(getItemIdentifier(record), getRootMessage(ex), NONE.getAction());
                report.incrementFailureCount();
                harvestRow = rollbackAndReloadEntity(context, harvestRow);
                logRecord(context, options, harvestRow, false, startTimestamp, getItemIdentifier(record), NONE);
            }

        }

        return harvestRow;
    }

    private void processRecord(Context context, HarvestedCollection harvestRow, Element record, String repositoryId,
        OAIHarvesterOptions options, long startTime, OAIHarvesterReport report) throws Exception {

        Collection targetCollection = harvestRow.getCollection();
        String itemOaiID = getItemIdentifier(record);

        HarvestedItem harvestedItem = harvestedItemService.findByOAIId(context, itemOaiID, targetCollection);
        Item item = harvestedItem != null ? harvestedItem.getItem() : null;

        if (item == null) {
            item = searchItem(context, record, repositoryId, targetCollection);
            if (item != null) {
                harvestedItem = harvestedItemService.create(context, item, itemOaiID);
            }
        }

        if (hasDeletedStatus(record)) {
            log.info("Item " + itemOaiID + " has been marked as deleted on the OAI server.");
            if (item != null) {
                collectionService.removeItem(context, targetCollection, item);
            }

            logRecord(context, options, harvestRow, true, startTime, itemOaiID, DELETION);
            return;
        }

        context.turnOffAuthorisationSystem();

        if (item != null) {
            harvestedItem = updateItem(context, harvestedItem, harvestRow, record, repositoryId, options, startTime);
        } else {
            harvestedItem = createItem(context, harvestRow, record, repositoryId, options, startTime, report);
            item = harvestedItem.getItem();
        }

        context.uncacheEntity(harvestedItem.getItem());
        context.uncacheEntity(harvestedItem);

        context.restoreAuthSystemState();

    }

    private Item searchItem(Context context, Element record, String repositoryId, Collection collection) {
        Item item = itemSearchService.search(context, calculateCrisSourceId(record, repositoryId));
        return item != null && collection.equals(item.getOwningCollection()) ? item : null;
    }

    private void handleORE(Context context, HarvestedCollection harvestRow, String repositoryId, String itemOaiID,
        Item item) throws Exception {

        String OREPrefix = getOREPrefix(harvestRow);
        List<Element> metadata = getMetadataRecord(harvestRow.getOaiSource(), itemOaiID, OREPrefix);
        if (CollectionUtils.isEmpty(metadata)) {
            return;
        }

        Element oreREM = metadata.get(0);

        if (harvestRow.getHarvestType() == 3) {
            importBitstreams(context, harvestRow, repositoryId, item, oreREM);
        }

        createOREBundle(context, item, oreREM);

    }

    private HarvestedItem updateItem(Context context, HarvestedItem harvestedItem,
        HarvestedCollection harvestRow, Element record, String repositoryId, OAIHarvesterOptions options,
        long startTimestamp) throws Exception {

        Item item = harvestedItem.getItem();
        Collection collection = harvestRow.getCollection();
        String itemOaiID = harvestedItem.getOaiID();

        log.debug("Item " + item.getHandle() + " was found locally. Using it to harvest " + itemOaiID + ".");

        if (!isForceSynchronization(collection, options) && isLocalItemMoreRecent(harvestedItem, getHeader(record))) {
            log.info("Item " + item.getHandle() + " was harvested more recently than the last update time "
                + "reported by the OAI server; skipping.");
            return harvestedItem;
        }

        clearMetadata(context, item, collection);
        fillItemWithMetadata(context, harvestRow, record, item, repositoryId);

        harvestedItem.setHarvestDate(new Date());
        harvestedItemService.update(context, harvestedItem);

        if (harvestRow.getHarvestType() > 1) {
            handleORE(context, harvestRow, repositoryId, itemOaiID, item);
        }

        logRecord(context, options, harvestRow, true, startTimestamp, itemOaiID, UPDATE);

        return harvestedItem;
    }

    private HarvestedItem createItem(Context context, HarvestedCollection harvestRow, Element record,
        String repositoryId, OAIHarvesterOptions options, long startTimestamp, OAIHarvesterReport report)
        throws Exception {

        Collection targetCollection = harvestRow.getCollection();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, targetCollection, false);
        Item item = workspaceItem.getItem();
        String itemOaiID = getItemIdentifier(record);

        HarvestedItem harvestedItem = harvestedItemService.create(context, item, itemOaiID);

        fillItemWithMetadata(context, harvestRow, record, item, repositoryId);
        addProvenanceMetadata(context, harvestRow, item, harvestedItem);

        // see if a handle can be extracted for the item
        String handle = extractHandle(item);

        if (handle != null) {
            DSpaceObject dso = handleService.resolveToObject(context, handle);
            if (dso != null) {
                throw new HarvestingException("Handle collision: attempted to re-assign handle '" + handle + "' "
                    + "to an incoming harvested item '" + harvestedItem.getOaiID() + "'.");
            }
        }

        if (harvestRow.getHarvestType() > 1) {
            handleORE(context, harvestRow, repositoryId, itemOaiID, item);
        }

        boolean isRecordValid = true;
        if (isRecordValidationEnabled(targetCollection, options)) {
            isRecordValid = validateRecord(record, harvestRow, report);
        }

        boolean isItemValid = true;
        if (isItemValidationEnabled(targetCollection, options)) {
            isItemValid = validateItem(context, record, workspaceItem, report);
        }

        if (isItemValid) {
            installOrStartWorkflow(context, workspaceItem, handle, options.isSubmissionEnabled());
        }

        harvestedItem.setHarvestDate(new Date());
        harvestedItemService.update(context, harvestedItem);

        context.uncacheEntity(workspaceItem);

        logRecord(context, options, harvestRow, isItemValid && isRecordValid, startTimestamp, itemOaiID, ADDITION);

        return harvestedItem;
    }

    private void installOrStartWorkflow(Context context, WorkspaceItem workspaceItem, String handle,
        boolean submissionEnabled) throws Exception {
        try {

            if (submissionEnabled) {
                installItemService.installItem(context, workspaceItem, handle);
            } else {
                workflowService.start(context, workspaceItem);
            }

        } catch (SQLException | IOException | AuthorizeException se) {
            workspaceItemService.deleteWrapper(context, workspaceItem);
            throw se;
        }
    }

    private boolean validateItem(Context context, Element record, WorkspaceItem wsItem, OAIHarvesterReport report) {

        Item item = wsItem.getItem();
        List<ValidationError> errors = validationService.validate(context, wsItem);

        boolean isItemValid = CollectionUtils.isEmpty(errors);
        if (!isItemValid) {
            report.addError(getItemIdentifier(record), formatValidationErrors(errors), ADDITION.getAction());
            log.error("Item with id " + item.getID() + " invalid:" + formatValidationErrors(errors));
        }

        return isItemValid;
    }

    private boolean validateRecord(Element record, HarvestedCollection harvestRow, OAIHarvesterReport report) {

        List<Element> metadataElements = getMetadataElements(record);
        if (CollectionUtils.isEmpty(metadataElements)) {
            String recordIdentifier = getItemIdentifier(record);
            report.addError(recordIdentifier, "No metadata element found", NONE.getAction());
            return false;
        }

        if (metadataElements.size() > 1) {
            String recordIdentifier = getItemIdentifier(record);
            report.addError(recordIdentifier, "More than one metadata element found", ADDITION.getAction());
            return false;
        }

        OAIHarvesterValidationResult result = oaiHarvesterValidator.validate(metadataElements.get(0), harvestRow);
        if (result.isNotValid()) {
            String recordIdentifier = getItemIdentifier(record);
            report.addError(recordIdentifier, result.getMessages(), ADDITION.getAction());
            log.error("Record with id " + recordIdentifier + " invalid: " + result.getMessages());
        }

        return result.isValid();
    }

    private void fillItemWithMetadata(Context context, HarvestedCollection harvestRow, Element record, Item item,
        String repositoryId) throws Exception {

        String metadataConfig = harvestRow.getHarvestMetadataConfig();
        IngestionCrosswalk metadataElementCrosswalk = getIngestionCrosswalk(metadataConfig, harvestRow, repositoryId);

        List<Element> metadataElements = getMetadataElements(record);
        if (CollectionUtils.isNotEmpty(metadataElements)) {
            metadataElementCrosswalk.ingest(context, item, metadataElements.get(0), false);
        }

        if (CollectionUtils.isEmpty(itemService.getMetadata(item, CRIS.getName(), "sourceId", null, null))) {
            String crisSourceId = calculateCrisSourceId(record, repositoryId);
            itemService.addMetadata(context, item, CRIS.getName(), "sourceId", null, null, crisSourceId);
        }

        itemService.update(context, item);

    }

    private void clearMetadata(Context context, Item item, Collection collection) throws SQLException {

        Set<String> metadataFieldsToKeep = getMetadataFieldsToKeep();

        List<MetadataValue> metadataToRemove = item.getMetadata().stream()
            .filter(value -> !metadataFieldsToKeep.contains(value.getMetadataField().toString('.')))
            .collect(Collectors.toList());

        itemService.removeMetadataValues(context, item, metadataToRemove);

    }

    private void addProvenanceMetadata(Context context, HarvestedCollection harvestRow, Item item,
        HarvestedItem harvestedItem) throws SQLException {
        String provenanceMsg = "Item created via OAI harvest from source: "
            + harvestRow.getOaiSource() + " on " + new DCDate(harvestedItem.getHarvestDate())
            + " (GMT).  Item's OAI Record identifier: " + harvestedItem.getOaiID();
        itemService.addMetadata(context, item, "dc", "description", "provenance", "en", provenanceMsg);
    }

    private String calculateCrisSourceId(Element record, String repositoryId) {
        return repositoryId + SPLIT + getMetadataIdentifier(record);
    }

    private Set<String> getMetadataFieldsToKeep() {
        return Set.of(configurationService.getArrayProperty("oai.harvester.update.metadata-to-keep"));
    }

    private int getTotalRecordSize(Document document) {
        return Optional.ofNullable(document)
            .map(d -> d.getRootElement().getChild("ListRecords", OAI_NS))
            .map(listRecords -> listRecords.getChild("resumptionToken", OAI_NS))
            .filter(resumptionElement -> hasCompleteListSizeAttribute(resumptionElement))
            .map(resumptionElement -> Integer.parseInt(resumptionElement.getAttributeValue("completeListSize")))
            .orElseGet(() -> getAllRecords(document).size());
    }

    private boolean hasCompleteListSizeAttribute(Element resumptionElement) {
        return resumptionElement != null && isNotBlank(resumptionElement.getAttributeValue("completeListSize"));
    }

    private void importBitstreams(Context context, HarvestedCollection harvestRow, String repositoryId, Item item,
        Element oreREM) throws Exception {

        String ORESerialKey = NamespaceUtils.getORENamespace().getPrefix();
        IngestionCrosswalk oreCrosswalk = getIngestionCrosswalk(ORESerialKey, harvestRow, repositoryId);

        List<Bundle> allBundles = item.getBundles();
        for (Bundle bundle : allBundles) {
            itemService.removeBundle(context, item, bundle);
        }

        oreCrosswalk.ingest(context, item, oreREM, false);
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
        if (document == null) {
            return Collections.emptyList();
        }

        Element listRecordsElement = document.getRootElement().getChild("ListRecords", OAI_NS);
        if (listRecordsElement == null) {
            return Collections.emptyList();
        }

        List<Element> records = listRecordsElement.getChildren("record", OAI_NS);
        return records != null ? records : Collections.emptyList();
    }

    private String getOREPrefix(HarvestedCollection harvestRow) {
        String ORESerialURI = NamespaceUtils.getORENamespace().getURI();
        String OREPrefix = oaiHarvesterClient.resolveNamespaceToPrefix(harvestRow.getOaiSource(), ORESerialURI);

        if (OREPrefix == null && harvestRow.getHarvestType() != HarvestedCollection.TYPE_DMD) {
            String message = "The OAI server doesn't support ORE dissemination in the format: " + ORESerialURI;
            log.error(message);
            throw new HarvestingException(message);
        }
        return OREPrefix;
    }

    private String getDescriptiveMetadataFormat(HarvestedCollection harvestRow) {

        String oaiSource = harvestRow.getOaiSource();
        String metadataKey = harvestRow.getHarvestMetadataConfig();

        Namespace metadataNS = NamespaceUtils.getMetadataFormatNamespace(metadataKey);

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

    private void handleResponseErrors(Set<String> errors) {
        if (errors.size() == 1 && errors.contains("noRecordsMatch")) {
            throw new NoRecordsMatchException("noRecordsMatch: OAI server did not contain any updates");
        } else {
            throw new HarvestingException("OAI server response contains the following error codes: " + errors);
        }
    }

    private String formatErrorMessage(Exception ex) {
        String message = "Not recoverable error occurs: ";
        if (ex instanceof HarvestingException && StringUtils.isNotBlank(ex.getMessage())) {
            message = message + ex.getMessage();
        } else {
            message = message + ExceptionMessageUtils.getRootMessage(ex);
        }
        return message;
    }

    private List<String> formatValidationErrors(List<ValidationError> errors) {
        return errors.stream()
            .map(error -> error.getMessage() + " - " + error.getPaths())
            .collect(Collectors.toList());
    }

    private HarvestedCollection setBusyStatus(Context context, HarvestedCollection harvestRow, Date startDate) {
        harvestRow = reloadEntity(context, harvestRow);
        harvestRow.setHarvestMessage("Collection harvesting is initializing...");
        harvestRow.setHarvestStatus(STATUS_BUSY);
        harvestRow.setHarvestStartTime(startDate);
        return updateHarvestRow(context, harvestRow);
    }

    private HarvestedCollection setReadyStatus(Context context, HarvestedCollection harvestRow, String message,
        Date lastDate) {
        harvestRow = reloadEntity(context, harvestRow);
        harvestRow.setHarvestMessage(message);
        harvestRow.setHarvestStatus(STATUS_READY);
        harvestRow.setLastHarvested(lastDate);
        return updateHarvestRow(context, harvestRow);
    }

    private HarvestedCollection setRetryStatus(Context context, HarvestedCollection harvestRow, String message) {
        harvestRow = reloadEntity(context, harvestRow);
        harvestRow.setHarvestMessage(message);
        harvestRow.setHarvestStatus(STATUS_RETRY);
        return updateHarvestRow(context, harvestRow);
    }

    private HarvestedCollection updateHarvestRow(Context context, HarvestedCollection harvestRow) {
        try {
            harvestedCollectionService.update(context, harvestRow);
            context.commit();
            return reloadEntity(context, harvestRow);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private HarvestedCollection reloadEntity(Context context, HarvestedCollection harvestRow) {
        try {
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

        return Date.from(Instant.now().plus(expirationInterval, ChronoUnit.HOURS));
    }

    private String getDateFormat(String oaiSource, Document identifyDocument) {
        String dateFormat = getDateGranularity(oaiSource, identifyDocument);
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
        Element metadata = record.getChild("metadata", OAI_NS);
        return metadata != null ? metadata.getChildren() : Collections.emptyList();
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

    private IngestionCrosswalk getIngestionCrosswalk(String name, HarvestedCollection harvestRow, String repositoryId) {
        Object crosswalk = pluginService.getNamedPlugin(IngestionCrosswalk.class, name);
        if (crosswalk == null) {
            throw new IllegalArgumentException("No IngestionCrosswalk found by name: " + name);
        }

        if (crosswalk instanceof CERIFIngestionCrosswalk) {
            initializeCERIFIngestionCrosswalk((CERIFIngestionCrosswalk) crosswalk, harvestRow, repositoryId);
        }

        return (IngestionCrosswalk) crosswalk;
    }

    private void initializeCERIFIngestionCrosswalk(CERIFIngestionCrosswalk crosswalk, HarvestedCollection harvestRow,
        String repositoryId) {

        Collection coll = harvestRow.getCollection();

        String transformationDir = configurationService.getProperty("oai.harvester.tranformation-dir");

        String preTrasform = collectionService.getMetadataFirstValue(coll, "cris", "harvesting", "preTransform", ANY);
        String postTrasform = collectionService.getMetadataFirstValue(coll, "cris", "harvesting", "postTransform", ANY);

        crosswalk.setIdPrefix(repositoryId + SPLIT);
        if (StringUtils.isNotBlank(preTrasform)) {
            crosswalk.setPreTransformXsl(new File(transformationDir, preTrasform).getAbsolutePath());
        }
        if (StringUtils.isNotBlank(postTrasform)) {
            crosswalk.setPostTransformXsl(new File(transformationDir, postTrasform).getAbsolutePath());
        }

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

    private Document identify(String baseURL) {
        OAIHarvesterResponseDTO identifyResponse = oaiHarvesterClient.identify(baseURL);
        if (identifyResponse.hasErrors()) {
            String errorMessage = "An error occurs identifing the repository: " + identifyResponse.getErrors();
            log.error(errorMessage);
            throw new HarvestingException(errorMessage);
        }

        return identifyResponse.getDocument();

    }

    private String getRepositoryIdentifier(HarvestedCollection harvestRow, Document identifyDocument) {
        return Optional.ofNullable(identifyDocument)
            .map( document -> document.getRootElement().getChild("Identify", OAI_NS))
            .flatMap(identifyElement -> getOaiIdentifierElement(identifyElement))
            .map(oaiIdentifier -> oaiIdentifier.getChild("repositoryIdentifier", OAI_IDENTIFIER_NS))
            .map(repositoryId -> repositoryId.getText())
            .orElse(valueOf(harvestRow.getID()));

    }

    @SuppressWarnings("unchecked")
    private Optional<Element> getOaiIdentifierElement(Element identifyElement) {
        return identifyElement.getChildren("description", OAI_NS).stream()
            .map(description -> ((Element) description).getChild("oai-identifier", OAI_IDENTIFIER_NS))
            .filter(identifier -> identifier != null)
            .findFirst();
    }

    /**
     * Query OAI-PMH server for the granularity of its datestamps.
     */
    private String getDateGranularity(String oaiSource, Document identifyDocument) {
        return Optional.ofNullable(identifyDocument)
            .map(document -> document.getRootElement().getChild("Identify", OAI_NS))
            .map(identifyElement -> identifyElement.getChild("granularity", OAI_NS))
            .map(granularityElement -> granularityElement.getText())
            .map(granularity -> adaptDateFormat(granularity))
            .orElse(null);
    }

    private String adaptDateFormat(String granularity) {
        if (granularity.contains("T") && !granularity.contains("'T'")) {
            granularity = granularity.replace("T", "'T'");
        }

        if (granularity.contains("Z") && !granularity.contains("'Z'")) {
            granularity = granularity.replace("Z", "'Z'");
        }

        return granularity.replace("DD", "dd");
    }

    private HarvestedCollection rollbackAndReloadEntity(Context context, HarvestedCollection harvestRow) {
        try {
            context.rollback();
            return context.reloadEntity(harvestRow);
        } catch (SQLException ex) {
            throw new SQLRuntimeException(ex);
        }
    }

    private String formatIntermediateMessage(OAIHarvesterReport report) {
        int currentRecord = report.getCurrentRecord();
        int totalRecordSize = report.getTotalRecordSize();
        String message = "Collection is currently being harvested (item " + currentRecord;
        return totalRecordSize != 0 ? message + " of " + totalRecordSize + ")" : message + ")";
    }

    public String getReportMessage(OAIHarvesterReport report) {
        String message = "Imported " + report.getSuccessCount() + " records with success";
        if (report.noRecordImportFails()) {
            return message;
        }
        return message + " - Record import failures: " + report.getFailureCount();
    }

    private void logRecord(Context context, OAIHarvesterOptions options, HarvestedCollection harvestRow,
        boolean isValid, Long startTimestamp, String itemIdentifier, OAIHarvesterAction action) {

        Collection collection = harvestRow.getCollection();
        long duration = System.currentTimeMillis() - startTimestamp;

        String logMessage = new StringBuilder(LOG_PREFIX)
            .append(options.getProcessId()).append(LOG_DELIMITER)
            .append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())).append(LOG_DELIMITER)
            .append(itemIdentifier).append(LOG_DELIMITER)
            .append(harvestRow.getOaiSource()).append(LOG_DELIMITER)
            .append(harvestRow.getOaiSetId() != null ? harvestRow.getOaiSetId() : "").append(LOG_DELIMITER)
            .append(getParentCommunityName(context, collection)).append(LOG_DELIMITER)
            .append(collection.getID()).append(LOG_DELIMITER)
            .append(collectionService.getName(collection)).append(LOG_DELIMITER)
            .append(isValid).append(LOG_DELIMITER)
            .append(action).append(LOG_DELIMITER)
            .append(duration)
            .toString();

        log.trace(logMessage);

    }

    private String getParentCommunityName(Context context, Collection collection) {
        try {
            Community parentCommunity = (Community) collectionService.getParentObject(context, collection);
            return parentCommunity != null ? communityService.getName(parentCommunity) : "";
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
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
    @SuppressWarnings("unchecked")
    private List<Element> getMetadataRecord(String oaiSource, String itemOaiId, String metadataPrefix)
        throws IOException, ParserConfigurationException, SAXException, TransformerException, HarvestingException {
        OAIHarvesterResponseDTO responseDTO = oaiHarvesterClient.getRecord(oaiSource, itemOaiId, metadataPrefix);
        if (responseDTO.hasErrors()) {
            throw new HarvestingException("OAI server returned the following errors during "
                + "getRecord execution to retrieve metadata: " + responseDTO.getErrors());
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

    private String getEmailRecipient(HarvestedCollection harvestRow) {

        String defaultEmail = configurationService.getProperty("alert.recipient");
        Collection collection = harvestRow.getCollection();

        String email = collectionService.getMetadataFirstValue(collection, "cris", "harvesting", "email", ANY);

        if (StringUtils.isBlank(email)) {
            return defaultEmail;
        }

        if ("IDENTIFY".equalsIgnoreCase(email)) {
            return findAdminEmail(harvestRow).orElse(defaultEmail);
        }

        return email;
    }

    private Optional<String> findAdminEmail(HarvestedCollection harvestRow) {
        return Optional.ofNullable(identify(harvestRow.getOaiSource()))
            .map(document -> document.getRootElement().getChild("Identify", OAI_NS))
            .map(identifyElement -> identifyElement.getChild("adminEmail", OAI_NS))
            .map(emailElement -> emailElement.getText());
    }

    private boolean isForceSynchronization(Collection collection, OAIHarvesterOptions options) {
        return getOptionOrMetadataValue(collection, options.isForceSynchronization(),
            "cris.harvesting.forceSynchronization");
    }

    private boolean isItemValidationEnabled(Collection collection, OAIHarvesterOptions options) {
        return getOptionOrMetadataValue(collection, options.isItemValidationEnabled(),
            "cris.harvesting.itemValidationEnabled");
    }

    private boolean isRecordValidationEnabled(Collection collection, OAIHarvesterOptions options) {
        return getOptionOrMetadataValue(collection, options.isRecordValidationEnabled(),
            "cris.harvesting.recordValidationEnabled");
    }

    private boolean getOptionOrMetadataValue(Collection collection, Boolean optionValue, String metadataField) {
        return optionValue != null ? optionValue : toBoolean(getFistMetadatavalue(collection, metadataField));
    }

    private String getFistMetadatavalue(Collection collection, String metadataField) {
        return collectionService.getMetadataByMetadataString(collection, metadataField).stream()
            .map((metadatavalue) -> metadatavalue.getValue())
            .findFirst().orElse(null);
    }

    public OAIHarvesterClient getOaiHarvesterClient() {
        return oaiHarvesterClient;
    }

    public void setOaiHarvesterClient(OAIHarvesterClient oaiHarvesterClient) {
        this.oaiHarvesterClient = oaiHarvesterClient;
    }

    public OAIHarvesterEmailSender getOaiHarvesterEmailSender() {
        return oaiHarvesterEmailSender;
    }

    public void setOaiHarvesterEmailSender(OAIHarvesterEmailSender oaiHarvesterEmailSender) {
        this.oaiHarvesterEmailSender = oaiHarvesterEmailSender;
    }

}
