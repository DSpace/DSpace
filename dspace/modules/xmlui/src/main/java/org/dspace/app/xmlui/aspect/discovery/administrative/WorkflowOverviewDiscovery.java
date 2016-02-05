package org.dspace.app.xmlui.aspect.discovery.administrative;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.xmlui.aspect.discovery.BrowseFacet;
import org.dspace.app.xmlui.aspect.discovery.SimpleSearch;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.PoolTask;
import org.dspace.workflow.WorkflowItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 15-nov-2011
 * Time: 8:48:59
 */
public class WorkflowOverviewDiscovery extends SimpleSearch {

    private static final Logger log = Logger.getLogger(WorkflowOverviewDiscovery.class);

    private static final Message T_no_results = message("xmlui.ArtifactBrowser.AbstractSearch.no_results");
    private static final Message T_VIEW_MORE = message("xmlui.discovery.AbstractFiltersTransformer.filters.view-more");
    protected static final Message T_title =
        message("xmlui.discovery.NonArchivedSearch.head");
    private static final Message T_display_all = message("xmlui.Discovery.DiscoverySubmissions.display-all");


    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException, AuthorizeException {
        pageMeta.addMetadata("title").addContent(T_title);

        if(!AuthorizeManager.isCuratorOrAdmin(ContextUtil.obtainContext(objectModel))){
            throw new AuthorizeException();
        }
    }

    @Override
    protected void buildSearchResultsDivision(Division search) throws IOException, SQLException, WingException, SearchServiceException, AuthorizeException {
        try {

            DSpaceObject scope = getScope();
            SolrQuery solrQuery = getDefaultQueryArgs();

            //We need to show the first x tasks for each available to facet on task !
            solrQuery.addFacetField("WorkflowstepTask_filter");
            solrQuery.addFacetField("DSpaceStatus_filter");

            this.queryResults = this.performSearch(solrQuery);
        }
        catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            queryResults = null;
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            queryResults = null;
        }

//        if (queryResults != null) {
//            search.addPara("result-query", "result-query")
//                    .addContent(T_result_query.parameterize(getQuery(), queryResults.getResults().getNumFound()));
//        }

        Division results = search.addDivision("search-results", "primary");

        DSpaceObject searchScope = getScope();
        if (queryResults != null &&
                queryResults.getResults().getNumFound() > 0) {

            List<FacetField.Count> statusFilters  = queryResults.getFacetField("DSpaceStatus_filter").getValues();
            for (FacetField.Count count : statusFilters) {
//                if(count.getName().equals("Submission")){
//                    Submissions are not rendered here
//                    continue;
//                }
                if(0 < count.getCount()){
                    if(count.getName().equals("Workflow")){
                        //Retrieve the seperate workflow steps
                        List<FacetField.Count> workflowSteps = queryResults.getFacetField("WorkflowstepTask_filter").getValues();
                        if(workflowSteps!=null){
                            for (FacetField.Count workflowStep : workflowSteps) {
                                if(0 < workflowStep.getCount()){
                                    renderResultBlock(results, workflowStep);
                                }
                            }
                        }
                    }else{
                        renderResultBlock(results, count);

                    }
                }
            }
        } else {
            results.addPara(T_no_results);
        }
        //}// Empty query
    }

    private void renderResultBlock(Division results, FacetField.Count count) throws SearchServiceException, WingException, SQLException, IOException, AuthorizeException {
        //Perform a query for each of these workflow steps.
        SolrQuery solrQuery = getDefaultQueryArgs();
        if(count.getFacetField().getName().equals("DSpaceStatus_filter")){
            solrQuery.addFilterQuery("DSpaceStatus:" + count.getName());
        }else{
            solrQuery.addFilterQuery("WorkflowstepTask:" + count.getName());
        }
        //Perform a search with our workflow step filter in place !
        QueryResponse queryResults = performSearch(queryArgs);

        Division workflowResultsDiv  = results.addDivision("search-results-" + count.getName());
        workflowResultsDiv.setHead(message("xmlui.Submission.Submissions.step.head." + count.getName()));


        SolrDocumentList solrResults = queryResults.getResults();

        if(isStepFilterPage()){
            // Pagination variables.
            int itemsTotal = (int) solrResults.getNumFound();
            int firstItemIndex = (int) solrResults.getStart() + 1;
            int lastItemIndex = (int) solrResults.getStart() + solrResults.size();

//                if (itemsTotal < lastItemIndex)
//                    lastItemIndex = itemsTotal;
            int currentPage = (int) (solrResults.getStart() / this.queryArgs.getRows()) + 1;
            int pagesTotal = (int) ((solrResults.getNumFound() - 1) / this.queryArgs.getRows()) + 1;
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("page", "{pageNum}");
            String pageURLMask = generateURL(parameters);
            //Check for facet queries ? If we have any add them
            String[] fqs = getParameterFilterQueries();
            if(fqs != null) {
                StringBuilder maskBuilder = new StringBuilder(pageURLMask);
                for (String fq : fqs) {
                    maskBuilder.append("&fq=").append(fq);
                }

                pageURLMask = maskBuilder.toString();
            }

            workflowResultsDiv.setMaskedPagination(itemsTotal, firstItemIndex,
                    lastItemIndex, currentPage, pagesTotal, pageURLMask);
        }


//        ReferenceSet referenceSet = workflowResultsDiv.addReferenceSet("search-results-repository",
//                ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");

        boolean showMoreUrl = false;
        if(solrResults.size() < solrResults.getNumFound()){
            showMoreUrl = true;
        }

        boolean isClaimedTask = false;
        if(count.getName().endsWith("_claimed")){
            isClaimedTask = true;
        }
        int row=1;
        if(solrResults.size()>0) {
            row=solrResults.size();
        }

        Table resultTable = workflowResultsDiv.addTable("results", row, isClaimedTask ? 4 : 3);

        Row headerRow = resultTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.title"));
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.submitter"));
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.datafiles"));
        if(isClaimedTask){
            headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.owner"));
        }

        for (SolrDocument doc : solrResults) {
            DSpaceObject resultDSO = SearchUtils.findDSpaceObject(context, doc);

            if (resultDSO instanceof Item) {
                Item item = (Item) resultDSO;
                Row itemRow = resultTable.addRow();

                String url;
                if(item.isArchived() || item.isWithdrawn()){
                    url = contextPath + "/admin/item?itemID=" + item.getID();
                }else{
                    //We most likely have a workflow item, find it
                    WorkflowItem workflowItem = WorkflowItem.findByItemId(context, item.getID());
                    if (workflowItem != null) {
                        //Attempt to find a task
                        List<PoolTask> pooltasks = PoolTask.find(context,workflowItem);
                        List<ClaimedTask> claimedtasks = ClaimedTask.find(context, workflowItem);


                        String step_id = null;
                        String action_id = null;
                        if(0 < pooltasks.size()){
                            step_id = pooltasks.get(0).getStepID();
                            action_id = pooltasks.get(0).getActionID();
                        }else
                        if(0 < claimedtasks.size()){
                            step_id = claimedtasks.get(0).getStepID();
                            action_id = claimedtasks.get(0).getActionID();
                        }
                        if(step_id != null && action_id != null){
//                            if(step_id.equals("reviewStep")){
                            url = contextPath+"/internal-item?itemID=" + item.getID();

//                            }else{
//                                url = contextPath+"/handle/"+workflowItem.getCollection().getHandle()+"/workflow?workflowID="+workflowItem.getID()+"&stepID="+step_id+"&actionID="+action_id;
//                            }

                        }else{
                            url = contextPath + "/admin/item?itemID=" + item.getID();
                        }
                    } else {
                        url = contextPath + "/admin/item?itemID=" + item.getID();
                    }
                }
                itemRow.addCell().addXref(url, item.getName());


                EPerson submitter = item.getSubmitter();
                if(submitter != null){
                    itemRow.addCell().addXref("mailto:" + submitter.getEmail(), submitter.getFullName());
                }else{
                    itemRow.addCell().addContent("N/A");
                }
                itemRow.addCell().addContent(DryadWorkflowUtils.getDataFiles(context, item).length);
                if(isClaimedTask){
                    try {
                        List<ClaimedTask> claimedTasks = ClaimedTask.find(context, WorkflowItem.findByItemId(context, item.getID()));
                        EPerson owner = EPerson.find(context, claimedTasks.get(0).getOwnerID());
                        itemRow.addCell().addXref("mailto:" + owner.getEmail()).addContent(owner.getFullName());
                    } catch (Exception e) {
                        log.error("Error while retrieving claimed user for item: " + item.getID());
                        itemRow.addCell().addContent("Error");
                    }
                }
            }
        }

        if(showMoreUrl && !isStepFilterPage()){
            //Add the show more url
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("page", "{pageNum}");
            String moreUrl = generateURL(parameters);
            String[] fqs = getParameterFilterQueries();
            if (fqs != null) {
                StringBuilder maskBuilder = new StringBuilder(moreUrl);
                for (String fq : fqs) {
                    maskBuilder.append("&fq=").append(fq);
                }
                if(count.getFacetField().getName().equals("DSpaceStatus_filter")){
                    maskBuilder.append("&fq=DSpaceStatus:").append(count.getName());
                }else{
                    maskBuilder.append("&fq=WorkflowstepTask:").append(count.getName());
                }
                moreUrl = maskBuilder.toString();
            }

            workflowResultsDiv.addPara().addXref(moreUrl).addContent(T_display_all);
        }
    }

    /**
     * A method indicating wether or not we are filtering on a certain step
     * If we are filtering different options become available
     * @return true if we are on a step filter page
     */
    private boolean isStepFilterPage(){
        Request request = ObjectModelHelper.getRequest(objectModel);
        String[] filterQueries = request.getParameterValues("fq");
        if(filterQueries != null){
            for(String fq : filterQueries){
                if(fq.startsWith("WorkflowstepTask:") || fq.startsWith("DSpaceStatus:")){
                    return true;
                }
            }
        }
        return false;
    }

    public void addViewMoreUrl(org.dspace.app.xmlui.wing.element.List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        String parameters = retrieveParameters(request);
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/non-archived-search-filter?" + parameters + BrowseFacet.FACET_FIELD + "=" + fieldName,
                T_VIEW_MORE
        );
    }



    private SolrQuery getDefaultQueryArgs() throws UIException {
        String query = getQuery();

        int page = getParameterPage();

        List<String> filterQueries = new ArrayList<String>();

        String location = ObjectModelHelper.getRequest(objectModel).getParameter("location");
        if(location != null){
            filterQueries.add("location:" + location);
        }

        String[] fqs = getSolrFilterQueries();

        if (fqs != null)
        {
            filterQueries.addAll(Arrays.asList(fqs));
        }


        try {
            queryArgs = this.prepareDefaultFilters(getView(), filterQueries.toArray(new String[filterQueries.size()]));
        } catch (SQLException e) {
            log.error(e);
            return null;
        }

        if (filterQueries.size() > 0) {
            queryArgs.addFilterQuery(filterQueries.toArray(new String[filterQueries.size()]));
        }


        queryArgs.setRows(getParameterRpp());

        String sortBy = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");

        String sortOrder = ObjectModelHelper.getRequest(objectModel).getParameter("order");


        //webui.itemlist.sort-option.1 = title:dc.title:title
        //webui.itemlist.sort-option.2 = dateissued:dc.date.issued:date
        //webui.itemlist.sort-option.3 = dateaccessioned:dc.date.accessioned:date
        //webui.itemlist.sort-option.4 = ispartof:dc.relation.ispartof:text

        if (sortBy != null) {
            if (sortOrder == null || sortOrder.equals("DESC"))
            {
                queryArgs.addSortField(sortBy, SolrQuery.ORDER.desc);
            }
            else
            {
                queryArgs.addSortField(sortBy, SolrQuery.ORDER.asc);
            }
        } else {
            queryArgs.addSortField("score", SolrQuery.ORDER.asc);
        }


        String groupBy = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");


        // Enable groupBy collapsing if designated
        if (groupBy != null && !groupBy.equalsIgnoreCase("none")) {
            /** Construct a Collapse Field Query */
            queryArgs.add("collapse.field", groupBy);
            queryArgs.add("collapse.threshold", "1");
            queryArgs.add("collapse.includeCollapsedDocs.fl", "handle");
            queryArgs.add("collapse.facet", "before");

            //queryArgs.a  type:Article^2

            // TODO: This is a hack to get Publications (Articles) to always be at the top of Groups.
            // TODO: I think the can be more transparently done in the solr solrconfig.xml with DISMAX and boosting
            /** sort in groups to get publications to top */
            queryArgs.addSortField("dc.type", SolrQuery.ORDER.asc);

        }


        queryArgs.setQuery(query != null && !query.trim().equals("") ? query : "*:*");

        if (page > 1)
        {
            queryArgs.setStart((page - 1) * queryArgs.getRows());
        }
        else
        {
            queryArgs.setStart(0);
        }


        return queryArgs;
    }

    /**
     * Query DSpace for a list of all items / collections / or communities that
     * match the given search query.
     *
     * @return The associated query results.
     */
    public QueryResponse performSearch(SolrQuery queryArgs) throws UIException, SearchServiceException {
        try {
            return getSearchService().search(ContextUtil.obtainContext(objectModel), queryArgs);
        } catch (SQLException e) {
            log.error("Error while retrieving context", e);
        }
        return null;
    }


    public Message getHead() {
        return T_title;
    }

    @Override
    protected String getDiscoverUrl(){
        return "workflow-overview";
    }

    @Override
    public String getView() {
        return "workflow-overview";
    }
}
