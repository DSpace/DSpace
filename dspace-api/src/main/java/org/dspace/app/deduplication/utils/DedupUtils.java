/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.deduplication.model.DuplicateDecisionObjectRest;
import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.model.DuplicateDecisionValue;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.deduplication.Deduplication;
import org.dspace.deduplication.service.DeduplicationService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.dspace.util.ItemUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utility class used to search for and manage duplicates inside the dedup solr core and database table.
 *
 * @author 4Science
 */
public class DedupUtils {

    /** log4j logger */
    private static Logger log = LogManager.getLogger(DedupUtils.class);

    private DedupService dedupService;

    @Autowired()
    private DeduplicationService deduplicationService;

    @Autowired()
    protected ConfigurationService configurationService;

    @Autowired()
    protected AuthorizeService authorizeService;

    /**
     *
     * Find potential duplicates of any signature type, given an item UUID and workflow state
     *
     * @param context       DSpace context
     * @param targetItemID  current item to match for duplicate detection
     * @param resourceType  item resource type
     * @param isInWorkflow  set null to retrieve all (ADMIN)
     * @return  List of potential duplicates
     * @throws SQLException
     * @throws SearchServiceException
     */
    private List<DuplicateItemInfo> findDuplicate(Context context, UUID targetItemID, Integer resourceType,
            Boolean isInWorkflow) throws SQLException, SearchServiceException {
        return findDuplicate(context, targetItemID, resourceType, null, isInWorkflow);
    }

    /**
     *
     * Find potential duplicates of a specific signature type, given an item UUID and workflow state
     *
     * @param context       DSpace context
     * @param targetItemID  current item to match for duplicate detection
     * @param resourceType  item resource type
     * @param isInWorkflow  set null to retrieve all (ADMIN)
     * @param signatureType type of signature to filter on
     * @return
     * @throws SQLException
     * @throws SearchServiceException
     */
    private List<DuplicateItemInfo> findDuplicate(Context context, UUID targetItemID, Integer resourceType,
            String signatureType, Boolean isInWorkflow) throws SQLException, SearchServiceException {

        // Set up base Solr query filters:
        // Potential duplicate not in workflow and not rejected, OR match unconfirmed and item in workflow or workspace
        SolrQuery findDuplicateBySignature = new SolrQuery();
        findDuplicateBySignature.setQuery((isInWorkflow == null ? SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED
                : (isInWorkflow ? SolrDedupServiceImpl.SUBQUERY_WF_MATCH_OR_REJECTED_OR_VERIFY
                        : SolrDedupServiceImpl.SUBQUERY_WS_MATCH_OR_REJECTED_OR_VERIFY)));
        // Potential duplicate matches current in-progress item ID
        findDuplicateBySignature.addFilterQuery(SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":" + targetItemID);
        // Potential duplicate matches current in-progress item resource type
        findDuplicateBySignature.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceType);

        // If the item is in workspace or workflow, allow reject or verify flags for the potential duplicate
        // Otherwise require a strict match flag
        String filter = "";
        if (isInWorkflow == null) {
            filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                    + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription();
        } else if (isInWorkflow) {
            filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":("
                    + SolrDedupServiceImpl.DeduplicationFlag.REJECTWF.getDescription() + " OR "
                    + SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription() + /* ")"; */" OR "
                    + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription() + ")";
        } else {
            filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":("
                    + SolrDedupServiceImpl.DeduplicationFlag.REJECTWS.getDescription() + " OR "
                    + SolrDedupServiceImpl.DeduplicationFlag.VERIFYWS.getDescription() + " OR "
                    + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription() + ")";
        }
        findDuplicateBySignature.addFilterQuery(filter);

        // Set fields
        findDuplicateBySignature.setFields("dedup.ids", "dedup.note", "dedup.flag");

        // If configured, ignore withdrawn items (default: true)
        if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn", true)) {
            findDuplicateBySignature.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
        }

        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<DuplicateItemInfo> dupsInfo = new ArrayList<>();

        // Perform actual search
        QueryResponse response = dedupService.search(findDuplicateBySignature);
        SolrDocumentList solrDocumentList = response.getResults();
        for (SolrDocument solrDocument : solrDocumentList) {
            Collection<Object> match = solrDocument.getFieldValues("dedup.ids");
            // Iterate IDs from this potential match
            if (match != null && !match.isEmpty()) {
                for (Object matchItem : match) {
                    UUID itemID = UUID.fromString((String) matchItem);
                    // If the match is not this exact item, it might be a duplicate. Get the underlying item
                    // and construct a new DuplicateItemInfo to add to the list of results
                    if (!itemID.equals(targetItemID)) {
                        DuplicateItemInfo info = new DuplicateItemInfo();

                        // Find and validate item
                        Item duplicateItem = itemService.find(context, itemID);
                        if (duplicateItem == null) {
                            // Found a zombie reference in solr, ignore it
                            continue;
                        }
                        if (!authorizeService.authorizeActionBoolean(context, duplicateItem, Constants.READ)) {
                            // The current user doesn't have READ authorisation to the duplicate item, ignore it
                            // This could be because:
                            // 1. the user is not an admin and the item is withdrawn
                            // 2. the item is archived but missing a READ policy or user doesn't apply to READ policy
                            // 3. the item is in workspace or workflow and the current user isn't submitter or reviewer
                            // ... and so on
                            continue;
                        }

                        // Set item and type
                        info.setDuplicateItem(duplicateItem);
                        info.setDuplicateItemType(ItemUtils.getItemStatus(context, duplicateItem));

                        // Set flag and decision notes on the DuplicateItemInfo
                        String flag = (String) solrDocument.getFieldValue("dedup.flag");
                        if (SolrDedupServiceImpl.DeduplicationFlag.VERIFYWS.getDescription().equals(flag)) {
                            info.setNote(DuplicateDecisionType.WORKSPACE,
                                    (String) solrDocument.getFieldValue("dedup.note"));
                            info.setDecision(DuplicateDecisionType.WORKSPACE, DuplicateDecisionValue.VERIFY);
                        } else if (SolrDedupServiceImpl.DeduplicationFlag.REJECTWS.getDescription().equals(flag)) {
                            info.setDecision(DuplicateDecisionType.WORKSPACE, DuplicateDecisionValue.REJECT);
                        } else if (SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription().equals(flag)) {
                            info.setNote(DuplicateDecisionType.WORKFLOW,
                                    (String) solrDocument.getFieldValue("dedup.note"));
                            info.setDecision(DuplicateDecisionType.WORKFLOW, DuplicateDecisionValue.VERIFY);
                        } else if (SolrDedupServiceImpl.DeduplicationFlag.REJECTWF.getDescription().equals(flag)) {
                            info.setDecision(DuplicateDecisionType.WORKFLOW, DuplicateDecisionValue.REJECT);
                        }

                        // Finally, add to the list and break from the 'each ID' loop, process the next Solr doc
                        dupsInfo.add(info);
                        break;
                    }
                }
            }
        }

        // Return list of potential duplicates
        return dupsInfo;

    }

    /**
     * Does this match have a decision (of a particular type) already stored?
     *
     * @param firstItemID       source item
     * @param secondItemID      target item
     * @param decisionType      decision type
     * @return
     * @throws SearchServiceException
     */
    private boolean hasStoredDecision(UUID firstItemID, UUID secondItemID, DuplicateDecisionType decisionType)
            throws SearchServiceException {
        // Search solr for matching decisions and return true if results is not empty
        QueryResponse response = dedupService.findDecisions(firstItemID, secondItemID, decisionType);
        return !response.getResults().isEmpty();
    }

    /**
     * Does a potential duplicate match exist in Solr given exact criteria? This is used to validate
     * decisions which need to refer to a valid match
     * @param context       DSpace context
     * @param itemID        source item ID
     * @param targetItemID  target item ID
     * @param resourceType  item resource type
     * @param isInWorkflow  is this item in workflow?
     * @return  true if the match exists, false if it does not
     * @throws SQLException
     * @throws SearchServiceException
     */
    public boolean matchExist(Context context, UUID itemID, UUID targetItemID, Integer resourceType,
                              Boolean isInWorkflow) throws SQLException, SearchServiceException {
        // Return true if there is a potential duplicate matching the passed ID, type and workflow state parameters
        // Otherwise, return false
        boolean exist = false;
        List<DuplicateItemInfo> potentialDuplicates = findDuplicate(context, itemID, resourceType, isInWorkflow);
        for (DuplicateItemInfo match : potentialDuplicates) {
            if (match.getDuplicateItem().getID().toString().equals(targetItemID.toString())) {
                exist = true;
                break;
            }
        }

        return exist;

    }

    /**
     * Verify a duplication entry
     * @param context       DSpace comment
     * @param dedupId       deduplication database ID
     * @param firstId       first item ID
     * @param secondId      second item ID
     * @param type          item resource type
     * @param toFix         "to fix" indicator
     * @param note          decision note
     * @param isWorkflow    is this item in workflow?
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void verify(Context context, int dedupId, UUID firstId, UUID secondId, int type, boolean toFix, String note,
            boolean isWorkflow) throws SQLException, AuthorizeException {
        // Sort item IDs
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        firstId = sortedIds[0];
        secondId = sortedIds[1];
        // Get DSpace items
        Item firstItem = ContentServiceFactory.getInstance().getItemService().find(context, firstId);
        Item secondItem = ContentServiceFactory.getInstance().getItemService().find(context, secondId);
        // Check that the current user has WRITE permission for at least one of the matching items
        if (hasAuthorization(context, firstId, secondId)) {
            // Try to get an existing database row for this duplication entry
            Deduplication row = deduplicationService.uniqueDeduplicationByFirstAndSecond(context, firstId, secondId);
            if (row != null) {
                // Duplication entry already exists. Not sure why the existing submitter decision gets set again here?
                String submitterDecision = row.getSubmitterDecision();
                if (isWorkflow && StringUtils.isNotBlank(submitterDecision)) {
                    row.setSubmitterDecision(submitterDecision);
                }
            } else {
                // Create a new row
                row = deduplicationService.create(context, new Deduplication());
            }

            // Set row data from method parameters
            row.setFirstItemId(firstId);
            row.setSecondItemId(secondId);
            row.setTofix(toFix);
            row.setFake(false);
            row.setReaderNote(note);
            row.setReaderId(context.getCurrentUser().getID());
            row.setReaderTime(new Date());
            // Set either workflow or workspace decision description
            if (isWorkflow) {
                row.setWorkflowDecision(DeduplicationFlag.VERIFYWF.getDescription());
            } else {
                row.setSubmitterDecision(DeduplicationFlag.VERIFYWS.getDescription());
            }

            // Update database and solr index
            deduplicationService.update(context, row);
            dedupService.buildDecision(context, firstId, secondId,
                    isWorkflow ? DeduplicationFlag.VERIFYWF : DeduplicationFlag.VERIFYWS, note);
        } else {
            throw new AuthorizeException("Only authorize users can access to the deduplication");
        }
    }

    /**
     * Check if the current user has write permission for either the first or second item
     * @param context   DSpace context
     * @param firstId   First item ID
     * @param secondId  Second item ID
     * @return  boolean indicating whether the user has authorization to create/update duplication entry
     */
    private Boolean hasAuthorization(Context context, UUID firstId, UUID secondId) {
        Item firstItem;
        Item secondItem;
        try {
            firstItem = ContentServiceFactory.getInstance().getItemService().find(context, firstId);
            secondItem = ContentServiceFactory.getInstance().getItemService().find(context, secondId);

            return (AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context,
                    firstItem, Constants.WRITE)
                    || AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context,
                            secondItem, Constants.WRITE));
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Retrieve a duplication row from the database
     * @param context   DSpace context
     * @param firstId   First item ID
     * @param secondId  Second item ID
     * @return  Duplication entry
     * @throws SQLException
     */
    private Deduplication retrieveDuplicationRow(Context context, UUID firstId, UUID secondId) throws SQLException {
        // Sort item IDs
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        Deduplication row;
        // Fetch row for this duplication entry or create and return new row
        row = deduplicationService.uniqueDeduplicationByFirstAndSecond(context, sortedIds[0], sortedIds[1]);
        if (row == null) {
            row = deduplicationService.create(context, new Deduplication());
        }

        return row;
    }

    /**
     * Set the decision made for a potential duplicate
     * @param context           DSpace context
     * @param firstId           First item ID
     * @param secondId          Second item ID
     * @param type              Item resource type
     * @param decisionObject    Decision object from patch add operation
     * @throws AuthorizeException
     * @throws SQLException
     * @throws SearchServiceException
     */
    public void setDuplicateDecision(Context context, UUID firstId, UUID secondId, Integer type,
            DuplicateDecisionObjectRest decisionObject)
            throws AuthorizeException, SQLException, SearchServiceException {

        // Check if the current user has WRITE permission to either item
        if (hasAuthorization(context, firstId, secondId)) {
            Deduplication row = retrieveDuplicationRow(context, firstId, secondId);
            // Set "to fix" and "fake" indicators to false
            boolean toFix = false;
            boolean fake = false;
            // Is this a workflow decision?
            boolean isWorkflow = decisionObject.getType() == DuplicateDecisionType.WORKFLOW;
            // Get the decision flag and description
            DeduplicationFlag decisionFlag = decisionObject.getDecisionFlag();
            String decisionDesc = decisionFlag.getDescription();
            // Set safe nulls for other decision information
            String readerNote = null;
            String epersonNote = null;
            UUID readerId = null;
            UUID epersonId = null;
            Date readerTime = null;
            Date epersonTime = null;

            // Set notes and indicators about decision-maker and decision
            // depending on whether it was made by a reviewer in workflow or a submitter in workspace
            if (decisionObject.getValue() == DuplicateDecisionValue.REJECT) {
                fake = !isWorkflow;
                epersonNote = decisionObject.getNote();
                epersonId = context.getCurrentUser().getID();
                epersonTime = new Date();
            } else if (decisionObject.getValue() == DuplicateDecisionValue.VERIFY) {
                toFix = true;
                readerNote = decisionObject.getNote();
                readerId = context.getCurrentUser().getID();
                readerTime = new Date();
            } else {
                decisionDesc = null;
            }

            // Set row values
            row.setFirstItemId(firstId);
            row.setSecondItemId(secondId);
            row.setTofix(toFix);
            row.setFake(fake);
            row.setEpersonId(epersonId);
            row.setRejectTime(epersonTime);
            row.setNote(epersonNote);
            row.setReaderId(readerId);
            row.setReaderTime(readerTime);
            row.setReaderNote(readerNote);
            if (isWorkflow) {
                row.setWorkflowDecision(decisionDesc);
            } else {
                row.setSubmitterDecision(decisionDesc);
            }

            // Update database entry
            deduplicationService.update(context, row);

            // Update solr index, removing existing stored decision if present
            if (hasStoredDecision(firstId, secondId, decisionObject.getType())) {
                dedupService.removeStoredDecision(firstId, secondId, decisionObject.getType());
            }
            dedupService.buildDecision(context, firstId, secondId, decisionFlag, decisionObject.getNote());
            dedupService.commit();
        } else {
            throw new AuthorizeException("Only authorize users can access to the deduplication");
        }
    }

    /**
     * Validate a decision object parsed by patch add operation
     * @param decisionObject    Duplication decision object
     * @return boolean indicating decision validity
     */
    public boolean validateDecision(DuplicateDecisionObjectRest decisionObject) {
        boolean valid = false;

        switch (decisionObject.getType()) {
            case WORKSPACE:
            case WORKFLOW:
                // If the decision type is workspace or workflow then the decision must be reject or verify
                valid = (decisionObject.getValue() == DuplicateDecisionValue.REJECT
                        || decisionObject.getValue() == DuplicateDecisionValue.VERIFY
                        || decisionObject.getValue() == null);
                break;
            case ADMIN:
                // If the decision type is admin then the decision must be reject
                valid = decisionObject.getValue() == DuplicateDecisionValue.REJECT;
                break;

            default:
                // no action
                break;
        }

        return valid;
    }

    /**
     * Get deduplication service. Required by Spring.
     * @return deduplication service
     */
    public DedupService getDedupService() {
        return dedupService;
    }

    /**
     * Set deduplication service. Required by Spring.
     * @param dedupService  deduplication service instance
     */
    public void setDedupService(DedupService dedupService) {
        this.dedupService = dedupService;
    }

    /**
     * Send a commit to the deduplication service
     */
    public void commit() {
        dedupService.commit();
    }

    /**
     * Get potential duplicates for a given item, item resource type and workflow state
     * @param context           DSpace context
     * @param itemID            Item ID
     * @param typeID            Item resource type
     * @param isInWorkflow      Workflow state
     * @return list of potential duplicates
     * @throws SQLException
     * @throws SearchServiceException
     */
    public List<DuplicateItemInfo> getDuplicateByIDandType(Context context, UUID itemID, int typeID,
            boolean isInWorkflow) throws SQLException, SearchServiceException {
        return getDuplicateByIdAndTypeAndSignatureType(context, itemID, typeID, null, isInWorkflow);
    }

    /**
     * Get potential duplicates for a given item, item resource type, signature type and workflow state
     * @param context           DSpace context
     * @param itemID            Item ID
     * @param typeID            Item resource type
     * @param signatureType     Duplicate signature type
     * @param isInWorkflow      Workflow state
     * @return list of potential duplicates
     * @throws SQLException
     * @throws SearchServiceException
     */
    public List<DuplicateItemInfo> getDuplicateByIdAndTypeAndSignatureType(Context context, UUID itemID, int typeID,
            String signatureType, boolean isInWorkflow) throws SQLException, SearchServiceException {
        return findDuplicate(context, itemID, typeID, signatureType, isInWorkflow);
    }

}
