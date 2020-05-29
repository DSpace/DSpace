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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.deduplication.model.DuplicateDecisionObjectRest;
import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.model.DuplicateDecisionValue;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.DSpaceObject;
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
 * Utility class used to search for duplicates inside the dedup solr core.
 *
 */
public class DedupUtils {

    /** log4j logger */
    private static Logger log = Logger.getLogger(DedupUtils.class);

    private DedupService dedupService;

    @Autowired(required = true)
    private DeduplicationService deduplicationService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    public DuplicateInfoList findSignatureWithDuplicate(Context context, String signatureType, int resourceType,
            int limit, int offset, int rule) throws SearchServiceException, SQLException {
        return findPotentialMatch(context, signatureType, resourceType, limit, offset, rule);
    }

    public Map<String, Integer> countSignaturesWithDuplicates(String query, int resourceTypeId)
            throws SearchServiceException {
        Map<String, Integer> results = new HashMap<String, Integer>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceTypeId);
        if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn")) {
            solrQuery.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
        }
        QueryResponse response = dedupService.search(solrQuery);

        FacetField facetField = response.getFacetField(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD);
        if (facetField != null) {
            for (Count count : facetField.getValues()) {
                solrQuery = new SolrQuery();
                solrQuery.setQuery(query);
                solrQuery.setRows(0);
                solrQuery.setFacet(true);
                solrQuery.setFacetMinCount(1);
                solrQuery.addFacetField(count.getName());
                solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
                if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn")) {
                    solrQuery.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
                }
                solrQuery.addFilterQuery(count.getAsFilterQuery());
                response = dedupService.search(solrQuery);

                FacetField facetField2 = response.getFacetField(count.getName());

                results.put(count.getName(), facetField2.getValueCount());
            }
        }

        return results;
    }

    public Map<String, Integer> countSuggestedDuplicate(String query, int resourceTypeId)
            throws SearchServiceException {
        Map<String, Integer> results = new HashMap<String, Integer>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        boolean ignoreSubmitterSuggestion = configurationService.getBooleanProperty(
                "deduplication.tool.duplicatechecker.ignore.submitter.suggestion", true);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + (ignoreSubmitterSuggestion ? SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription()
                        : "verify*"));
        if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn")) {
            solrQuery.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
        }
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceTypeId);
        QueryResponse response = dedupService.search(solrQuery);
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            Long numbers = response.getResults().getNumFound();
            results.put("onlyreported", numbers.intValue());
        }

        return results;
    }

    /**
     * @param context
     * @param targetItemID
     * @param resourceType
     * @param signatureType
     * @param isInWorkflow  set null to retrieve all (ADMIN)
     * @return
     * @throws SQLException
     * @throws SearchServiceException
     */
    private List<DuplicateItemInfo> findDuplicate(Context context, UUID targetItemID, Integer resourceType,
            String signatureType, Boolean isInWorkflow) throws SQLException, SearchServiceException {

        SolrQuery findDuplicateBySignature = new SolrQuery();
        findDuplicateBySignature.setQuery((isInWorkflow == null ? SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED
                : (isInWorkflow ? SolrDedupServiceImpl.SUBQUERY_WF_MATCH_OR_REJECTED_OR_VERIFY
                        : SolrDedupServiceImpl.SUBQUERY_WS_MATCH_OR_REJECTED_OR_VERIFY)));
        findDuplicateBySignature.addFilterQuery(SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":" + targetItemID);
        findDuplicateBySignature.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceType);
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

        findDuplicateBySignature.setFields("dedup.ids", "dedup.note", "dedup.flag");

        if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn")) {
            findDuplicateBySignature.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
        }

        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<DuplicateItemInfo> dupsInfo = new ArrayList<DuplicateItemInfo>();
        QueryResponse response = dedupService.search(findDuplicateBySignature);
        SolrDocumentList solrDocumentList = response.getResults();
        for (SolrDocument solrDocument : solrDocumentList) {
            Collection<Object> match = (Collection<Object>) solrDocument.getFieldValues("dedup.ids");

            if (match != null && !match.isEmpty()) {
                for (Object matchItem : match) {
                    UUID itemID = UUID.fromString((String) matchItem);
                    if (!itemID.equals(targetItemID)) {
                        DuplicateItemInfo info = new DuplicateItemInfo();
                        Item duplicateItem = itemService.find(context, itemID);

                        if (duplicateItem == null) {
                            // found a zombie reference in solr, ignore it
                            continue;
                        }

                        info.setDuplicateItem(duplicateItem);
                        info.setDuplicateItemType(ItemUtils.getItemStatus(context, duplicateItem));

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
                        dupsInfo.add(info);
                        break;
                    }
                }
            }
        }

        return dupsInfo;

    }

    private boolean hasStoredDecision(UUID firstItemID, UUID secondItemID, DuplicateDecisionType decisionType)
            throws SQLException, SearchServiceException {

        QueryResponse response = dedupService.findDecisions(firstItemID, secondItemID, decisionType);

        return !response.getResults().isEmpty();
    }

    public boolean matchExist(Context context, UUID itemID, UUID targetItemID, Integer resourceType,
            String signatureType, Boolean isInWorkflow) throws SQLException, SearchServiceException {
        boolean exist = false;
        List<DuplicateItemInfo> potentialDuplicates = findDuplicate(context, itemID, resourceType, null, isInWorkflow);
        for (DuplicateItemInfo match : potentialDuplicates) {
            if (match.getDuplicateItem().getID().toString().equals(targetItemID.toString())) {
                exist = true;
                break;
            }
        }

        return exist;

    }

    public boolean rejectAdminDups(Context context, UUID firstId, UUID secondId, Integer type)
            throws SQLException, AuthorizeException {
        if (firstId == secondId) {
            return false;
        }
        if (!AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context)) {
            throw new AuthorizeException(
                    "Only the administrator can reject the duplicate in the administrative section");
        }
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);

        Deduplication row = null;
        try {

            row = deduplicationService.uniqueDeduplicationByFirstAndSecond(context, sortedIds[0], sortedIds[1]);
            if (row != null) {
                row.setAdminId(context.getCurrentUser().getID());
                row.setAdminTime(new Date());
                row.setAdminDecision(DeduplicationFlag.REJECTADMIN.getDescription());

                deduplicationService.update(context, row);
            } else {
                row = new Deduplication();
                row.setAdminId(context.getCurrentUser().getID());
                row.setFirstItemId(sortedIds[0]);
                row.setSecondItemId(sortedIds[1]);
                row.setAdminTime(new Date());
                row.setAdminDecision(DeduplicationFlag.REJECTADMIN.getDescription());

                row = deduplicationService.create(context, row);
            }
            dedupService.buildDecision(context, firstId, secondId, DeduplicationFlag.REJECTADMIN, null);
            return true;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Mark all the potential duplicates for the specified signature and item as
     * fake.
     * 
     * @param context
     * @param itemID
     * @param signatureID
     * @return false if no potential duplicates are found
     * @throws SQLException
     * @throws AuthorizeException
     * @throws SearchServiceException
     */
    public boolean rejectAdminDups(Context context, UUID itemID, String signatureType, int resourceType)
            throws SQLException, AuthorizeException, SearchServiceException {

        DuplicateSignatureInfo dsi = findPotentialMatchByID(context, signatureType, resourceType, itemID);

        boolean found = false;
        for (DSpaceObject item : dsi.getItems()) {
            if (item != null) {
                if (item.getID() == itemID) {
                    found = true;
                    break;
                }
            }
        }
        if (found && dsi.getNumItems() > 1) {
            for (DSpaceObject item : dsi.getItems()) {
                if (item != null) {
                    if (item.getID() != itemID) {
                        rejectAdminDups(context, itemID, item.getID(), resourceType);
                    }
                }
            }
        }
        return true;

    }

    public void rejectAdminDups(Context context, List<DSpaceObject> items, String signatureID)
            throws SQLException, AuthorizeException, SearchServiceException {
        for (DSpaceObject item : items) {
            rejectAdminDups(context, item.getID(), signatureID, item.getType());
        }
    }

    public void verify(Context context, int dedupId, UUID firstId, UUID secondId, int type, boolean toFix, String note,
            boolean check) throws SQLException, AuthorizeException {
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        firstId = sortedIds[0];
        secondId = sortedIds[1];
        Item firstItem = ContentServiceFactory.getInstance().getItemService().find(context, firstId);
        Item secondItem = ContentServiceFactory.getInstance().getItemService().find(context, secondId);
        if (AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context, firstItem,
                Constants.WRITE)
                || AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context,
                        secondItem, Constants.WRITE)) {
            Deduplication row = deduplicationService.uniqueDeduplicationByFirstAndSecond(context, firstId, secondId);

            if (row != null) {
                String submitterDecision = row.getSubmitterDecision();
                if (check && StringUtils.isNotBlank(submitterDecision)) {
                    row.setSubmitterDecision(submitterDecision);
                }
            } else {
                row = deduplicationService.create(context, new Deduplication());
            }

            row.setFirstItemId(firstId);
            row.setSecondItemId(secondId);
            row.setTofix(toFix);
            row.setFake(false);
            row.setReaderNote(note);
            row.setReaderId(context.getCurrentUser().getID());
            row.setReaderTime(new Date());
            if (check) {
                row.setWorkflowDecision(DeduplicationFlag.VERIFYWF.getDescription());
            } else {
                row.setSubmitterDecision(DeduplicationFlag.VERIFYWS.getDescription());
            }

            deduplicationService.update(context, row);
            dedupService.buildDecision(context, firstId, secondId,
                    check ? DeduplicationFlag.VERIFYWF : DeduplicationFlag.VERIFYWS, note);
        } else {
            throw new AuthorizeException("Only authorize users can access to the deduplication");
        }
    }

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

    private Deduplication retrieveDuplicationRow(Context context, UUID firstId, UUID secondId) throws SQLException {

        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        Deduplication row = null;
        row = deduplicationService.uniqueDeduplicationByFirstAndSecond(context, sortedIds[0], sortedIds[1]);
        if (row == null) {
            row = deduplicationService.create(context, new Deduplication());
        }

        return row;
    }

    public void setDuplicateDecision(Context context, UUID firstId, UUID secondId, Integer type,
            DuplicateDecisionObjectRest decisionObject)
            throws AuthorizeException, SQLException, SearchServiceException {

        if (hasAuthorization(context, firstId, secondId)) {
            Deduplication row = retrieveDuplicationRow(context, firstId, secondId);
            boolean toFix = false;
            boolean fake = false;
            boolean isWorkflow = decisionObject.getType() == DuplicateDecisionType.WORKFLOW;
            DeduplicationFlag decisionFlag = decisionObject.getDecisionFlag();
            String decisionDesc = decisionFlag.getDescription();
            String readerNote = null;
            String epersonNote = null;
            UUID readerId = null;
            UUID epersonId = null;
            Date readerTime = null;
            Date epersonTime = null;

            if (decisionObject.getValue() == DuplicateDecisionValue.REJECT) {
                fake = isWorkflow ? false : true;
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

            deduplicationService.update(context, row);

            if (hasStoredDecision(firstId, secondId, decisionObject.getType())) {
                dedupService.removeStoredDecision(firstId, secondId, decisionObject.getType());
            }

            dedupService.buildDecision(context, firstId, secondId, decisionFlag, decisionObject.getNote());
            dedupService.commit();
        } else {
            throw new AuthorizeException("Only authorize users can access to the deduplication");
        }
    }

    public boolean validateDecision(DuplicateDecisionObjectRest decisionObject) {
        boolean valid = false;

        switch (decisionObject.getType()) {
            case WORKSPACE:
            case WORKFLOW:
                valid = (decisionObject.getValue() == DuplicateDecisionValue.REJECT
                        || decisionObject.getValue() == DuplicateDecisionValue.VERIFY
                        || decisionObject.getValue() == null);
                break;
            case ADMIN:
                valid = decisionObject.getValue() == DuplicateDecisionValue.REJECT;
                break;

            default:
                // no action
                break;
        }

        return valid;
    }

    public boolean rejectDups(Context context, UUID firstId, UUID secondId, Integer type, boolean notDupl, String note,
            boolean check) throws SQLException {
        UUID[] sortedIds = new UUID[] { firstId, secondId };
        Arrays.sort(sortedIds);
        Deduplication row = null;
        try {

            row = deduplicationService.uniqueDeduplicationByFirstAndSecond(context, sortedIds[0], sortedIds[1]);

            Item firstItem = ContentServiceFactory.getInstance().getItemService().find(context, firstId);
            Item secondItem = ContentServiceFactory.getInstance().getItemService().find(context, secondId);
            if (AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context, firstItem,
                    Constants.WRITE)
                    || AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeActionBoolean(context,
                            secondItem, Constants.WRITE)) {

                if (row != null) {
                    String submitterDecision = row.getSubmitterDecision();
                    if (check && StringUtils.isNotBlank(submitterDecision)) {
                        row.setSubmitterDecision(submitterDecision);
                    }
                } else {
                    row = deduplicationService.create(context, new Deduplication());
                }

                row.setEpersonId(context.getCurrentUser().getID());
                row.setFirstItemId(sortedIds[0]);
                row.setSecondItemId(sortedIds[1]);
                row.setRejectTime(new Date());
                row.setNote(note);
                row.setFake(notDupl);
                if (check) {
                    row.setWorkflowDecision(DeduplicationFlag.REJECTWF.getDescription());
                } else {
                    row.setSubmitterDecision(DeduplicationFlag.REJECTWS.getDescription());
                }
                deduplicationService.update(context, row);
                dedupService.buildDecision(context, firstId, secondId,
                        check ? DeduplicationFlag.REJECTWF : DeduplicationFlag.REJECTWS, note);
                return true;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    private DuplicateInfoList findPotentialMatch(Context context, String signatureType, int resourceType, int start,
            int rows, int rule) throws SearchServiceException, SQLException {

        DuplicateInfoList dil = new DuplicateInfoList();

        if (StringUtils.isNotEmpty(signatureType)) {
            if (!StringUtils.contains(signatureType, "_signature")) {
                signatureType += "_signature";
            }
        }
        SolrQuery solrQueryExternal = new SolrQuery();

        solrQueryExternal.setRows(0);

        String subqueryNotInRejected = null;

        switch (rule) {
            case 1:
                subqueryNotInRejected = SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFY;
                break;
            case 2:
                subqueryNotInRejected = SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF;
                break;
            default:
                subqueryNotInRejected = SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED;
                break;
        }

        solrQueryExternal.setQuery(subqueryNotInRejected);

        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":" + signatureType);

        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceType);
        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
        if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn")) {
            solrQueryExternal.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
        }
        solrQueryExternal.setFacet(true);
        solrQueryExternal.setFacetMinCount(1);
        solrQueryExternal.addFacetField(signatureType);
        solrQueryExternal.setFacetSort(FacetParams.FACET_SORT_COUNT);

        QueryResponse responseFacet = getDedupService().search(solrQueryExternal);

        FacetField facetField = responseFacet.getFacetField(signatureType);

        List<DuplicateInfo> result = new ArrayList<DuplicateInfo>();

        int index = 0;
        for (Count facetHit : facetField.getValues()) {
            if (index >= start + rows) {
                break;
            }
            if (index >= start) {
                String name = facetHit.getName();

                SolrQuery solrQueryInternal = new SolrQuery();

                solrQueryInternal.setQuery(subqueryNotInRejected);

                solrQueryInternal
                        .addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":" + signatureType);
                solrQueryInternal.addFilterQuery(facetHit.getAsFilterQuery());
                solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceType);
                solrQueryInternal.setRows(Integer.MAX_VALUE);
                solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
                if (configurationService.getBooleanProperty("deduplication.tool.duplicatechecker.ignorewithdrawn")) {
                    solrQueryInternal.addFilterQuery("-" + SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD + ":true");
                }
                QueryResponse response = getDedupService().search(solrQueryInternal);

                SolrDocumentList solrDocumentList = response.getResults();

                DuplicateSignatureInfo dsi = new DuplicateSignatureInfo(signatureType, name);

                for (SolrDocument solrDocument : solrDocumentList) {

                    List<String> signatureTypeList = (List<String>) (solrDocument.getFieldValue(signatureType));
                    for (String signatureTypeString : signatureTypeList) {
                        if (name.equals(signatureTypeString)) {

                            dsi.setSignature(signatureTypeString);
                            List<String> ids = (List<String>) solrDocument
                                    .getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

                            for (String obj : ids) {
                                Item item = ContentServiceFactory.getInstance().getItemService().find(context,
                                        UUID.fromString(obj));
                                if (item != null) {
                                    if (!(dsi.getItems().contains(item))) {
                                        dsi.getItems().add(item);
                                    }
                                }
                            }

                            result.add(dsi);
                        } else {
                            dsi.getOtherSignature().add(signatureTypeString);
                        }
                    }
                }
            }
            index++;
        }

        dil.setDsi(result);
        dil.setSize(facetField.getValues().size());
        return dil;
    }

    private DuplicateSignatureInfo findPotentialMatchByID(Context context, String signatureType, int resourceType,
            UUID itemID) throws SearchServiceException, SQLException {
        if (StringUtils.isNotEmpty(signatureType)) {
            if (!StringUtils.contains(signatureType, "_signature")) {
                signatureType += "_signature";
            }
        }
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery(SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":" + itemID);

        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":" + signatureType);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceType);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());

        QueryResponse response = getDedupService().search(solrQuery);

        SolrDocumentList solrDocumentList = response.getResults();

        DuplicateSignatureInfo dsi = new DuplicateSignatureInfo(signatureType);
        for (SolrDocument solrDocument : solrDocumentList) {

            String signatureTypeString = (String) ((List) (solrDocument.getFieldValue(signatureType))).get(0);

            dsi.setSignature(signatureTypeString);

            List<String> ids = (List<String>) solrDocument.getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

            for (String obj : ids) {
                Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(obj));
                if (!(dsi.getItems().contains(item))) {
                    dsi.getItems().add(item);
                }
            }
        }

        return dsi;
    }

    public DedupService getDedupService() {
        return dedupService;
    }

    public void setDedupService(DedupService dedupService) {
        this.dedupService = dedupService;
    }

    public void commit() {
        dedupService.commit();
    }

    public List<DuplicateItemInfo> getDuplicateByIDandType(Context context, UUID itemID, int typeID,
            boolean isInWorkflow) throws SQLException, SearchServiceException {
        return getDuplicateByIdAndTypeAndSignatureType(context, itemID, typeID, null, isInWorkflow);
    }

    public List<DuplicateItemInfo> getDuplicateByIdAndTypeAndSignatureType(Context context, UUID itemID, int typeID,
            String signatureType, boolean isInWorkflow) throws SQLException, SearchServiceException {
        return findDuplicate(context, itemID, typeID, signatureType, isInWorkflow);
    }

    public List<DuplicateItemInfo> getAdminDuplicateByIdAndType(Context context, UUID itemID, int typeID)
            throws SQLException, SearchServiceException {
        return findDuplicate(context, itemID, typeID, null, null);
    }

    public DuplicateInfoList findSuggestedDuplicate(Context context, int resourceType, int start, int rows)
            throws SearchServiceException, SQLException {

        DuplicateInfoList dil = new DuplicateInfoList();

        SolrQuery solrQueryInternal = new SolrQuery();

        solrQueryInternal.setQuery(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED);

        solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":" + resourceType);
        boolean ignoreSubmitterSuggestion = configurationService.getBooleanProperty(
                "deduplication.tool.duplicatechecker.ignore.submitter.suggestion", true);
        if (ignoreSubmitterSuggestion) {
            solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                    + SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription());
        } else {
            solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":verify*");
        }

        QueryResponse response = getDedupService().search(solrQueryInternal);

        SolrDocumentList solrDocumentList = response.getResults();

        List<DuplicateInfo> result = new ArrayList<DuplicateInfo>();

        int index = 0;

        for (SolrDocument solrDocument : solrDocumentList) {
            if (index >= start + rows) {
                break;
            }
            DuplicateSignatureInfo dsi = new DuplicateSignatureInfo("suggested",
                    (String) solrDocument.getFirstValue("_version_"));

            List<String> ids = (List<String>) solrDocument.getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

            for (String obj : ids) {
                Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(obj));
                if (item != null) {
                    if (!(dsi.getItems().contains(item))) {
                        dsi.getItems().add(item);
                    }
                }
            }
            result.add(dsi);
            index++;
        }

        dil.setDsi(result);
        dil.setSize(solrDocumentList.getNumFound());
        return dil;
    }
}
