/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.cris.configuration.ViewResolver;
import org.dspace.app.cris.deduplication.service.DedupService;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;

public class DedupUtils
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(DedupUtils.class);

    private DedupService dedupService;

    private ApplicationService applicationService;
    
    private DSpace dspace = new DSpace();

    public DuplicateInfoList findSignatureWithDuplicate(Context context,
            String signatureType, int resourceType, int limit, int offset, int rule)
                    throws SearchServiceException, SQLException
    {
        return findPotentialMatch(context, signatureType, resourceType, limit,
                offset, rule);
    }

    public Map<String, Integer> countSignaturesWithDuplicates(String query, int resourceTypeId)
            throws SearchServiceException
    {
        Map<String, Integer> results = new HashMap<String, Integer>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD+":"+ SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD+":"+resourceTypeId);
        if (ConfigurationManager.getBooleanProperty("deduplication",
                "tool.duplicatechecker.ignorewithdrawn"))
        {
            solrQuery.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
        }
        QueryResponse response = dedupService.search(solrQuery);

        FacetField facetField = response.getFacetField(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD);
        if (facetField != null)
        {
            for (Count count : facetField.getValues())
            {
                solrQuery = new SolrQuery();
                solrQuery.setQuery(query);
                solrQuery.setRows(0);
                solrQuery.setFacet(true);
                solrQuery.setFacetMinCount(1);
                solrQuery.addFacetField(count.getName());
                solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD+":"+ SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
                if (ConfigurationManager.getBooleanProperty("deduplication",
                        "tool.duplicatechecker.ignorewithdrawn"))
                {
                    solrQuery.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
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
            throws SearchServiceException
    {
        Map<String, Integer> results = new HashMap<String, Integer>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        boolean ignoreSubmitterSuggestion = ConfigurationManager.getBooleanProperty("deduplication", "tool.duplicatechecker.ignore.submitter.suggestion", true);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD+":"+ (ignoreSubmitterSuggestion?SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription():"verify*"));
        if (ConfigurationManager.getBooleanProperty("deduplication",
                "tool.duplicatechecker.ignorewithdrawn"))
        {
            solrQuery.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
        }
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD+":"+resourceTypeId);
        QueryResponse response = dedupService.search(solrQuery);
        if(response!=null && response.getResults()!=null && !response.getResults().isEmpty()) {
            Long numbers = response.getResults().getNumFound();
            results.put("onlyreported", numbers.intValue());
        }

        return results;
    }
    
    /**
     * @param context
     * @param id
     * @param resourceType
     * @param signatureType
     * @param isInWorkflow set null to retrieve all (ADMIN) 
     * @return
     * @throws SQLException
     * @throws SearchServiceException
     */
    private List<DuplicateItemInfo> findDuplicate(Context context, Integer id,
            Integer resourceType, String signatureType, Boolean isInWorkflow)
                    throws SQLException, SearchServiceException
    {
            ViewResolver resolver = dspace.getServiceManager().getServiceByName(CrisConstants.getEntityTypeText(resourceType) + "ViewResolver", ViewResolver.class);
        
            List<Integer> result = new ArrayList<Integer>();
            Map<Integer, String> verify = new HashMap<Integer,String>();

            SolrQuery findDuplicateBySignature = new SolrQuery();
            findDuplicateBySignature.setQuery((isInWorkflow == null?SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED:(isInWorkflow?SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF:SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFY)));
            findDuplicateBySignature
                    .addFilterQuery(SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":"
                            + id);
            findDuplicateBySignature.addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":"
                    + resourceType);
            String filter = "";
            if(isInWorkflow==null) {            
                filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription();            }
            else if(isInWorkflow) {
                filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":("
                    + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription() +" OR "+ SolrDedupServiceImpl.DeduplicationFlag.VERIFYWS.getDescription() + ")";
            }
            else {
                filter = SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription();
            }

            findDuplicateBySignature.addFilterQuery(filter);

            findDuplicateBySignature
                    .setFields("dedup.ids", "dedup.note", "dedup.flag");

            if (ConfigurationManager.getBooleanProperty("deduplication",
                    "tool.duplicatechecker.ignorewithdrawn"))
            {
                findDuplicateBySignature.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
            }

            QueryResponse response2 = dedupService
                    .search(findDuplicateBySignature);
            SolrDocumentList solrDocumentList2 = response2.getResults();
            for (SolrDocument solrDocument : solrDocumentList2)
            {
                Collection<Object> tmp = (Collection<Object>) solrDocument.getFieldValues("dedup.ids");
                if(tmp!=null && !tmp.isEmpty()) {
                    for(Object tttmp : tmp) {
                        String idtmp = (String)tttmp;
                        int parseInt = Integer.parseInt(idtmp);
                        if(parseInt!=id) {
                            String flag = (String)solrDocument.getFieldValue("dedup.flag");
                            if(SolrDedupServiceImpl.DeduplicationFlag.VERIFYWS.getDescription().equals(flag)) {
                                verify.put(parseInt, (String)solrDocument.getFieldValue("dedup.note"));
                            }
                            else {
                                result.add(parseInt);
                            }
                            break;
                        }
                    }
                }
            }
        
        List<DuplicateItemInfo> dupsInfo = new ArrayList<DuplicateItemInfo>();        
        for (Integer idResult : result) {
            DuplicateItemInfo info = new DuplicateItemInfo();            
            info.setRejected(false);
            info.setDuplicateItem(resolver.fillDTO(context, idResult, resourceType));
            if(verify.containsKey(idResult)) {
                info.setNote(verify.get(idResult));
                info.setCustomActions(context);
            }
            else {
                info.setDefaultActions(context, isInWorkflow);
            }                     
            dupsInfo.add(info);
        }
        
        return dupsInfo;
    }

    public boolean rejectAdminDups(Context context, Integer firstId,
            Integer secondId, Integer type) throws SQLException, AuthorizeException
    {
        if (firstId == secondId)
        {
            return false;
        }
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only the administrator can reject the duplicate in the administrative section");
        }
        Integer[] sortedIds = new Integer[] { firstId, secondId };
        Arrays.sort(sortedIds);
        
        TableRow row = null;
        try
        {
            row = DatabaseManager.querySingleTable(context, "dedup_reject",
                    "select * from dedup_reject where first_item_id = ? and second_item_id = ?",
                    sortedIds[0], sortedIds[1]);
            if (row != null)
            {
                row.setColumn("admin_id", context.getCurrentUser().getID());
                row.setColumn("admin_time", new Date());
                row.setColumn("resource_type_id", type);
                row.setColumn(SolrDedupServiceImpl.COLUMN_ADMIN_DECISION, DeduplicationFlag.REJECTADMIN.getDescription());
            }
            else
            {
                row = DatabaseManager.create(context, "dedup_reject");
                row.setColumn("first_item_id", sortedIds[0]);
                row.setColumn("second_item_id", sortedIds[1]);
                row.setColumn("admin_id", context.getCurrentUser().getID());
                row.setColumn("admin_time", new Date());
                row.setColumn("resource_type_id", type);
                row.setColumn(SolrDedupServiceImpl.COLUMN_ADMIN_DECISION, DeduplicationFlag.REJECTADMIN.getDescription());
            }
            DatabaseManager.update(context, row);
            
            dedupService.buildReject(context, firstId, secondId, type, DeduplicationFlag.REJECTADMIN, null);
            return true;
        }
        catch (Exception ex)
        {
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
    public boolean rejectAdminDups(Context context, int itemID,
            String signatureType, int resourceType) throws SQLException,
                    AuthorizeException, SearchServiceException
    {

        DuplicateSignatureInfo dsi = findPotentialMatchByID(context,
                signatureType, resourceType, itemID);

        boolean found = false;
        for (DSpaceObject item : dsi.getItems())
        {
            if(item!=null) {
                if (item.getID() == itemID)
                {
                    found = true;
                    break;
                }
            }
        }
        if (found && dsi.getNumItems() > 1)
        {
            for (DSpaceObject item : dsi.getItems())
            {
                if (item != null)
                {
                    if (item.getID() != itemID)
                    {
                        rejectAdminDups(context, itemID, item.getID(),
                                resourceType);
                    }
                }
            }
        }
        return true;

    }

    public void rejectAdminDups(Context context, List<DSpaceObject> items,
            String signatureID) throws SQLException, AuthorizeException,
                    SearchServiceException
    {
        for (DSpaceObject item : items)
        {
            rejectAdminDups(context, item.getID(), signatureID, item.getType());
        }
    }

    public void verify(Context context, int dedupId, int firstId,
            int secondId, int type, boolean toFix, String note, boolean check)
                    throws SQLException, AuthorizeException
    {
        Integer[] sortedIds = new Integer[] { firstId, secondId };
        Arrays.sort(sortedIds);
        firstId = sortedIds[0];
        secondId = sortedIds[1];
        Item firstItem = Item.find(context, firstId);
        Item secondItem = Item.find(context, secondId);
        if (AuthorizeManager.authorizeActionBoolean(context, firstItem,
                Constants.WRITE)
                || AuthorizeManager.authorizeActionBoolean(context, secondItem,
                        Constants.WRITE))
        {
            TableRow row = DatabaseManager.querySingle(context, "select * from dedup_reject where first_item_id = ? and second_item_id = ?", firstId, secondId);
            
            if(row!=null) {                
                int identifierRow = row.getIntColumn("dedup_reject_id");
                String submitterDecision = row.getStringColumn("submitter_decision");
                row = DatabaseManager.row("dedup_reject");
                row.setColumn("dedup_reject_id", identifierRow);
                if(check && StringUtils.isNotBlank(submitterDecision)) {
                    row.setColumn(SolrDedupServiceImpl.COLUMN_SUBMITTER_DECISION, submitterDecision);
                }                
            }
            else {
                row = DatabaseManager.create(context, "dedup_reject");
            }
            
            row.setColumn("first_item_id", firstId);
            row.setColumn("second_item_id", secondId);
            row.setColumn("resource_type_id", type);
            row.setColumn("tofix", toFix);
            row.setColumn("fake", false);
            row.setColumn("reader_note", note);
            row.setColumn("reader_id", context.getCurrentUser().getID());
            row.setColumn("reader_time", new Date());
            row.setColumn("resource_type_id", type);
            row.setColumn(check?SolrDedupServiceImpl.COLUMN_WORKFLOW_DECISION:SolrDedupServiceImpl.COLUMN_SUBMITTER_DECISION, check?DeduplicationFlag.VERIFYWF.getDescription():DeduplicationFlag.VERIFYWS.getDescription());

            DatabaseManager.update(context, row);
            dedupService.buildReject(context, firstId, secondId, type, check?DeduplicationFlag.VERIFYWF:DeduplicationFlag.VERIFYWS, note);
        }
        else
        {
            throw new AuthorizeException(
                    "Only authorize users can access to the deduplication");
        }
    }

    public boolean rejectDups(Context context, Integer firstId,
            Integer secondId, Integer type, boolean notDupl, String note,
            boolean check) throws SQLException
    {
        Integer[] sortedIds = new Integer[] { firstId, secondId };
        Arrays.sort(sortedIds);
        TableRow row = null;
        try
        {
            row = DatabaseManager.querySingle(context,
                    "select * from dedup_reject where first_item_id = ? and second_item_id = ?",
                    sortedIds[0], sortedIds[1]);

            Item firstItem = Item.find(context, firstId);
            Item secondItem = Item.find(context, secondId);
            if (AuthorizeManager.authorizeActionBoolean(context, firstItem,
                    Constants.WRITE)
                    || AuthorizeManager.authorizeActionBoolean(context,
                            secondItem, Constants.WRITE))
            {

                if(row!=null) {                
                    int identifierRow = row.getIntColumn("dedup_reject_id");
                    String submitterDecision = row.getStringColumn("submitter_decision");
                    row = DatabaseManager.row("dedup_reject");
                    row.setColumn("dedup_reject_id", identifierRow);
                    if(check && StringUtils.isNotBlank(submitterDecision)) {
                        row.setColumn(SolrDedupServiceImpl.COLUMN_SUBMITTER_DECISION, submitterDecision);
                    }                
                }
                else {
                    row = DatabaseManager.create(context, "dedup_reject");
                }
                
                row.setColumn("first_item_id", sortedIds[0]);
                row.setColumn("second_item_id", sortedIds[1]);
                row.setColumn("eperson_id", context.getCurrentUser().getID());
                row.setColumn("reject_time", new Date());
                row.setColumn("note", note);
                row.setColumn("fake", notDupl);
                row.setColumn("resource_type_id", type);
                if (check)
                {
                    row.setColumn(SolrDedupServiceImpl.COLUMN_WORKFLOW_DECISION,
                            DeduplicationFlag.REJECTWF.getDescription());
                }
                else
                {
                    row.setColumn(
                            SolrDedupServiceImpl.COLUMN_SUBMITTER_DECISION,
                            DeduplicationFlag.REJECTWS.getDescription());
                }
                DatabaseManager.update(context, row);
                dedupService.buildReject(context, firstId, secondId, type,
                        check ? DeduplicationFlag.REJECTWF
                                : DeduplicationFlag.REJECTWS,
                        note);
                return true;
            }
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    private DuplicateInfoList findPotentialMatch(Context context,
            String signatureType, int resourceType, int start, int rows, int rule)
                    throws SearchServiceException, SQLException
    {

        DuplicateInfoList dil = new DuplicateInfoList();

        if (StringUtils.isNotEmpty(signatureType))
        {
            if (!StringUtils.contains(signatureType, "_signature"))
            {
                signatureType += "_signature";
            }
        }
        SolrQuery solrQueryExternal = new SolrQuery();

        solrQueryExternal.setRows(0);
        
        String subqueryNotInRejected = null;
        
        switch (rule)
        {
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

        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":"
                + signatureType);
        
        solrQueryExternal
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD
                        + ":" + resourceType);
        solrQueryExternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
        if (ConfigurationManager.getBooleanProperty("deduplication",
                "tool.duplicatechecker.ignorewithdrawn"))
        {
            solrQueryExternal.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
        }
        solrQueryExternal.setFacet(true);
        solrQueryExternal.setFacetMinCount(1);
        solrQueryExternal.addFacetField(signatureType);
        solrQueryExternal.setFacetSort(FacetParams.FACET_SORT_COUNT);

        QueryResponse responseFacet = getDedupService().search(solrQueryExternal);

        FacetField facetField = responseFacet.getFacetField(signatureType);
        
        List<DuplicateInfo> result = new ArrayList<DuplicateInfo>();
        
        int index = 0;
        for (Count facetHit : facetField.getValues())
        {
            if (index >= start + rows)
            {
                break;
            }
            if (index >= start)
            {
                String name = facetHit.getName();

                SolrQuery solrQueryInternal = new SolrQuery();

                solrQueryInternal.setQuery(subqueryNotInRejected);
                
                solrQueryInternal.addFilterQuery(SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":"
                                + signatureType);
                solrQueryInternal.addFilterQuery(facetHit.getAsFilterQuery());                
                solrQueryInternal.addFilterQuery(
                        SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD + ":"
                                + resourceType);
                solrQueryInternal.setRows(Integer.MAX_VALUE);
                solrQueryInternal.addFilterQuery(
                        SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                                + SolrDedupServiceImpl.DeduplicationFlag.MATCH.getDescription());
                if (ConfigurationManager.getBooleanProperty("deduplication",
                        "tool.duplicatechecker.ignorewithdrawn"))
                {
                    solrQueryInternal.addFilterQuery("-"+SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD+":true");
                }
                QueryResponse response = getDedupService().search(solrQueryInternal);
                
                SolrDocumentList solrDocumentList = response.getResults();

                DuplicateSignatureInfo dsi = new DuplicateSignatureInfo(signatureType, name);
                
                for (SolrDocument solrDocument : solrDocumentList)
                {

                    List<String> signatureTypeList = (List<String>) (solrDocument
                            .getFieldValue(signatureType));
                    // Collection<Object> tmp = (Collection<Object>)
                    for (String signatureTypeString : signatureTypeList)
                    {
                        if(name.equals(signatureTypeString)) {
                            
                            dsi.setSignature(signatureTypeString);    
                            Integer resourceTypeString = (Integer) (solrDocument
                                    .getFieldValue(
                                            SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD));
                            List<String> ids = (List<String>) solrDocument
                                    .getFieldValue(
                                            SolrDedupServiceImpl.RESOURCE_IDS_FIELD);
                                                        
                            if (resourceTypeString < CrisConstants.CRIS_TYPE_ID_START)
                            {
    
                                for (String obj : ids)
                                {
                                    Item item = Item.find(context,
                                            Integer.parseInt(obj));
                                    if (item != null)
                                    {
                                        if (!(dsi.getItems().contains(item)))
                                        {
                                            dsi.getItems().add(item);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                for (String obj : ids)
                                {
                                    DSpaceObject dspaceObject = getApplicationService()
                                            .getEntityByCrisId(obj);
                                    if (!(dsi.getItems().contains(dspaceObject)))
                                    {
                                        dsi.getItems().add(dspaceObject);
                                    }
                                }
                            }
    
                            result.add(dsi);
                        }
                        else {
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

    private DuplicateSignatureInfo findPotentialMatchByID(Context context,
            String signatureType, int resourceType, int itemID)
                    throws SearchServiceException, SQLException
    {
        if (StringUtils.isNotEmpty(signatureType))
        {
            if (!StringUtils.contains(signatureType, "_signature"))
            {
                signatureType += "_signature";
            }
        }
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery(
                SolrDedupServiceImpl.RESOURCE_IDS_FIELD + ":" + itemID);

        solrQuery.addFilterQuery(
                SolrDedupServiceImpl.RESOURCE_SIGNATURETYPE_FIELD + ":"
                        + signatureType);
        solrQuery
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD
                        + ":" + resourceType);
        solrQuery.addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                + SolrDedupServiceImpl.DeduplicationFlag.MATCH
                        .getDescription());

        QueryResponse response = getDedupService().search(solrQuery);

        SolrDocumentList solrDocumentList = response.getResults();

        DuplicateSignatureInfo dsi = new DuplicateSignatureInfo(signatureType);
        for (SolrDocument solrDocument : solrDocumentList)
        {

            String signatureTypeString = (String) ((List) (solrDocument
                    .getFieldValue(signatureType))).get(0);

            dsi.setSignature(signatureTypeString);

            Integer resourceTypeString = (Integer) (solrDocument.getFieldValue(
                    SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD));
            List<String> ids = (List<String>) solrDocument
                    .getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

            if (resourceTypeString < CrisConstants.CRIS_TYPE_ID_START)
            {

                for (String obj : ids)
                {
                    Item item = Item.find(context, Integer.parseInt(obj));
                    if (!(dsi.getItems().contains(item)))
                    {
                        dsi.getItems().add(item);
                    }
                }
            }
            else
            {
                for (String obj : ids)
                {
                    DSpaceObject dspaceObject = getApplicationService()
                            .getEntityByCrisId(obj);
                    if (!(dsi.getItems().contains(dspaceObject)))
                    {
                        dsi.getItems().add(dspaceObject);
                    }
                }
            }

        }

        return dsi;
    }

    public DedupService getDedupService()
    {
        return dedupService;
    }

    public void setDedupService(DedupService dedupService)
    {
        this.dedupService = dedupService;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void commit()
    {
        dedupService.commit();        
    }

    public List<DuplicateItemInfo> getDuplicateByIDandType(Context context,
            int itemID, int typeID, boolean isInWorkflow) throws SQLException, SearchServiceException
    {      
        return getDuplicateByIdAndTypeAndSignatureType(context, itemID, typeID, null, isInWorkflow);
    }
    
    public List<DuplicateItemInfo> getDuplicateByIdAndTypeAndSignatureType(Context context,
            int itemID, int typeID, String signatureType, boolean isInWorkflow) throws SQLException, SearchServiceException
    {        
        return findDuplicate(context, itemID, typeID, signatureType, isInWorkflow);
    }

    public List<DuplicateItemInfo> getAdminDuplicateByIdAndType(Context context,
            int itemID, int typeID) throws SQLException, SearchServiceException
    {        
        return findDuplicate(context, itemID, typeID, null, null);
    }
    
    public DuplicateInfoList findSuggestedDuplicate(Context context,
            int resourceType, int start, int rows)
                    throws SearchServiceException, SQLException
    {

        DuplicateInfoList dil = new DuplicateInfoList();

        SolrQuery solrQueryInternal = new SolrQuery();

        solrQueryInternal
                .setQuery(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED);

        solrQueryInternal
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD
                        + ":" + resourceType);
        boolean ignoreSubmitterSuggestion = ConfigurationManager.getBooleanProperty("deduplication", "tool.duplicatechecker.ignore.submitter.suggestion", true);
        if(ignoreSubmitterSuggestion) {
            solrQueryInternal
                .addFilterQuery(SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":"
                        + SolrDedupServiceImpl.DeduplicationFlag.VERIFYWF.getDescription());
        }
        else {
            solrQueryInternal.addFilterQuery(
                    SolrDedupServiceImpl.RESOURCE_FLAG_FIELD + ":verify*");            
        }

        QueryResponse response = getDedupService().search(solrQueryInternal);

        SolrDocumentList solrDocumentList = response.getResults();

        List<DuplicateInfo> result = new ArrayList<DuplicateInfo>();

        int index = 0;

        for (SolrDocument solrDocument : solrDocumentList)
        {
            if (index >= start + rows)
            {
                break;
            }
            DuplicateSignatureInfo dsi = new DuplicateSignatureInfo("suggested",
                    (String) solrDocument.getFirstValue("_version_"));

            Integer resourceTypeString = (Integer) (solrDocument.getFieldValue(
                    SolrDedupServiceImpl.RESOURCE_RESOURCETYPE_FIELD));
            List<String> ids = (List<String>) solrDocument
                    .getFieldValue(SolrDedupServiceImpl.RESOURCE_IDS_FIELD);

            if (resourceTypeString < CrisConstants.CRIS_TYPE_ID_START)
            {

                for (String obj : ids)
                {
                    Item item = Item.find(context, Integer.parseInt(obj));
                    if (item != null)
                    {
                        if (!(dsi.getItems().contains(item)))
                        {
                            dsi.getItems().add(item);
                        }
                    }
                }
            }
            else
            {
                for (String obj : ids)
                {
                    DSpaceObject dspaceObject = getApplicationService()
                            .getEntityByCrisId(obj);
                    if (!(dsi.getItems().contains(dspaceObject)))
                    {
                        dsi.getItems().add(dspaceObject);
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
