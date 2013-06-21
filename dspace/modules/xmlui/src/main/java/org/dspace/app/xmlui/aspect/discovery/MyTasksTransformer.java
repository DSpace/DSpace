package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.LogManager;
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
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.content.DCValue;
import org.dspace.doi.DryadDOIRegistrationHelper;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 25-nov-2011
 * Time: 14:08:51
 */
public class MyTasksTransformer extends DiscoverySubmissions{

    private static final Logger log = Logger.getLogger(MyTasksTransformer.class);

    protected static final Message T_title =
        message("xmlui.Submission.MyTasks.title");
    protected static final Message T_trail =
        message("xmlui.Submission.MyTasks.trail");
    private static final Message T_no_results =
            message("xmlui.Submission.MyTasks.no-results");

    private static final Message T_blackout_status =
            message("xmlui.Submission.MyTasks.blackout_status");
    private static final Message T_archived_status =
            message("xmlui.Submission.MyTasks.archived_status");
    private static final Message T_withdrawn_status =
            message("xmlui.Submission.MyTasks.withdrawn_status");
    private static final Message T_workflow_status =
            message("xmlui.Submission.MyTasks.workflow_status");
    private static final Message T_workspace_status =
            message("xmlui.Submission.MyTasks.workspace_status");

    private static final Message T_item_status =
        message("xmlui.Submission.MyTasks.item_status");
    private static final Message T_item_title =
        message("xmlui.Submission.MyTasks.item_title");
    private static final Message T_doi =
        message("xmlui.Submission.MyTasks.doi");
    private static final Message T_doi_registration_status =
        message("xmlui.Submission.MyTasks.doi_registration_status");

    private static final Message T_doi_not_registered =
        message("xmlui.Submission.MyTasks.doi_not_registered");
    private static final Message T_doi_registered =
        message("xmlui.Submission.MyTasks.doi_registered");

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        if(isStepFilterPage()){
            pageMeta.addTrailLink(contextPath + "/my-tasks",T_trail);
        }else{
            pageMeta.addTrail().addContent(T_trail);
        }
    }

    protected Message getNoResultsMessage() {
        return T_no_results;
    }

@Override
    protected void buildSearchResultsDivision(Division search) throws IOException, SQLException, WingException, SearchServiceException {
        try {

            DSpaceObject scope = getScope();
            SolrQuery solrQuery = getDefaultQueryArgs();

            //We need to show the first x tasks for each available to facet on task !
            solrQuery.addFacetField("WorkflowstepTask_filter");

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

        if (queryResults != null &&
                queryResults.getResults().getNumFound() > 0) {

            //For each of our possible steps loop retrieve the max of 10 results
            List<FacetField.Count> statusFilters  = queryResults.getFacetField("WorkflowstepTask_filter").getValues();
            if(statusFilters != null){
                List<FacetField.Count> workflowSteps = queryResults.getFacetField("WorkflowstepTask_filter").getValues();
                if(workflowSteps != null){
                    for (FacetField.Count workflowStep : workflowSteps) {
                        if(0 < workflowStep.getCount()){
                            renderResultBlock(results, workflowStep);
                        }
                    }
                }
            }


        } else {
            results.addPara(getNoResultsMessage());
        }
        //}// Empty query

    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        String itemIDString = ObjectModelHelper.getRequest(objectModel).getParameter("itemID");
        if(itemIDString != null) {
            try {
                int itemID = Integer.valueOf(itemIDString);
                // after submission
                Division statusDiv = body.addDivision("statusDiv");
                Item item = Item.find(context, itemID);

                DCValue[] titleValues = item.getMetadata("dc.title");
                String title = "";
                if(titleValues.length > 0) { title = titleValues[0].value; }

                Table t = statusDiv.addTable("statusTable", 3, 2, null);
                // Status row
                Row statusRow = t.addRow();
                statusRow.addCell(Cell.ROLE_HEADER).addContent(T_item_status);
                Cell c = statusRow.addCell(Cell.ROLE_DATA);
                if(DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(item)) {
                    c.addContent(T_blackout_status);
                } else if (item.isArchived()) {
                    c.addContent(T_archived_status);
                } else if(item.isWithdrawn()) {
                    c.addContent(T_withdrawn_status);
                } else if(WorkflowItem.findByItemId(context, itemID) != null) {
                    c.addContent(T_workflow_status);
                } else if(WorkspaceItem.findByItem(context, item) != null) {
                    c.addContent(T_workspace_status);
                }

                // DOI Row
                Row doiRow = t.addRow();
                doiRow.addCell(Cell.ROLE_HEADER).addContent(T_doi);
                doiRow.addCell(Cell.ROLE_DATA).addContent(DOIIdentifierProvider.getDoiValue(item));


                // Registration Status Row
                Row registrationRow = t.addRow();
                registrationRow.addCell(Cell.ROLE_HEADER).addContent(T_doi_registration_status);
                c = registrationRow.addCell(Cell.ROLE_DATA);

                DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);
                String dataciteMetadata = dis.lookupEzidRegistration(item);
                if(dataciteMetadata != null && dataciteMetadata.length() > 0) {
                    String linkTarget = dis.getEzidRegistrationURL(item);
                    c.addXref(linkTarget, T_doi_registered);
                } else {
                    // no DOI registration info
                    c.addContent(T_doi_not_registered);
                }

                Row titleRow = t.addRow();
                titleRow.addCell(Cell.ROLE_HEADER).addContent(T_item_title);
                titleRow.addCell(Cell.ROLE_DATA).addXref(contextPath + "/internal-item?itemID=" + itemIDString, title);
            } catch (NumberFormatException nfe) {

            }
        }
        super.addBody(body);
    }



    protected void renderResultBlock(Division results, FacetField.Count count) throws SearchServiceException, WingException, SQLException {
        //Perform a query for each of these workflow steps.
        SolrQuery solrQuery = getDefaultQueryArgs();
        solrQuery.addFilterQuery("WorkflowstepTask:" + count.getName());
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


        Table resultTable = workflowResultsDiv.addTable("results", solrResults.size(), 2);

        boolean showMoreUrl = false;
        if(solrResults.size() < solrResults.getNumFound()){
            showMoreUrl = true;
        }

        Row headerRow = resultTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.title"));
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.submitter"));
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.datafiles"));

        for (SolrDocument doc : solrResults) {
            DSpaceObject resultDSO = SearchUtils.findDSpaceObject(context, doc);

            if (resultDSO instanceof Item) {
                Item item = (Item) resultDSO;

                try {
                    WorkflowItem workflowItem = WorkflowItem.findByItemId(context, item.getID());
                    if(workflowItem == null){
                        continue;
                    }
                    String stepId = null;
                    String actionId = null;
                    Message taskMessage = null;
                    PoolTask poolTask = PoolTask.findByWorkflowIdAndEPerson(context, workflowItem.getID(),context.getCurrentUser().getID());
                    if(poolTask != null){
                        stepId = poolTask.getStepID();
                        actionId = poolTask.getActionID();
                    }else{
                        ClaimedTask claimedTask = ClaimedTask.findByWorkflowIdAndEPerson(context, workflowItem.getID(), context.getCurrentUser().getID());
                        if(claimedTask != null){
                            stepId = claimedTask.getStepID();
                            actionId = claimedTask.getActionID();
                        }
                    }

                    Row itemRow = resultTable.addRow();


                    String url = contextPath+"/handle/"+ workflowItem.getCollection().getHandle()+"/workflow?" +
                            "workflowID="+workflowItem.getID()+"&" +
                            "stepID="+stepId+"&" +
                            "actionID="+actionId;

                    itemRow.addCell().addXref(url ,item.getName());

                    EPerson submitter = item.getSubmitter();
                    if(submitter != null){
                        itemRow.addCell().addXref("mailto:" + submitter.getEmail(), submitter.getFullName());
                    }else{
                        itemRow.addCell().addContent("N/A");
                    }


                    itemRow.addCell().addContent(DryadWorkflowUtils.getDataFiles(context, item).length);
                } catch (Exception e) {
                    log.error(LogManager.getHeader(context, "Error while rendering task for item: " + item.getID(), ""), e);
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
                maskBuilder.append("&fq=WorkflowstepTask:").append(count.getName());
                moreUrl = maskBuilder.toString();
            }

            workflowResultsDiv.addPara().addXref(moreUrl).addContent(T_display_all);
        }
    }

    protected SolrQuery getDefaultQueryArgs() throws UIException {
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

        filterQueries.add("WorkflowEpersonId:" + eperson.getID());


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


    public void addViewMoreUrl(org.dspace.app.xmlui.wing.element.List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        //TODO: do this !
        String parameters = retrieveParameters(request);
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/discovery-my-tasks-search-filter?" + parameters + BrowseFacet.FACET_FIELD + "=" + fieldName,
                T_VIEW_MORE

        );
    }

    public Message getHead() {
        return T_title;
    }
    
    @Override
    protected String getDiscoverUrl(){
        return "my-tasks";
    }


    @Override
    public String getView() {
        return "myTasks";
    }
}
