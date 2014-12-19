package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.sort.SortOption;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 14-sep-2011
 * Time: 13:37:50
 *
 * This class renders all items in the workflow/in progress/archived that belong to the submitter
 */
public class DiscoverySubmissions extends SimpleSearch {

    protected static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    protected static final Message T_trail =
        message("xmlui.Submission.Submissions.trail");
    protected static final Message T_untitled =
        message("xmlui.Submission.Submissions.untitled");
    protected static final Message T_s_submit_remove =
            message("xmlui.Submission.Submissions.submit_submit_remove");



    private static final Logger log = Logger.getLogger(DiscoverySubmissions.class);

    private static final Message T_no_results = message("xmlui.ArtifactBrowser.AbstractSearch.no_results");
    protected static final Message T_VIEW_MORE = message("xmlui.discovery.AbstractFiltersTransformer.filters.view-more");
    protected static final Message T_title =
        message("xmlui.Submission.Submissions.title");
    protected static final Message T_display_all = message("xmlui.Discovery.DiscoverySubmissions.display-all");

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        if(isStepFilterPage()){
            pageMeta.addTrailLink(contextPath + "/submissions",T_trail);
        }else{
            pageMeta.addTrail().addContent(T_trail);
        }
    }

    @Override
    protected void buildSearchResultsDivision(Division search) throws IOException, SQLException, WingException, SearchServiceException {
        try {

            DSpaceObject scope = getScope();
            SolrQuery solrQuery = getDefaultQueryArgs();

            //We need to show the first x tasks for each available to facet on task !
            solrQuery.addFacetField("Workflowstep_filter");
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

        if (queryResults != null &&
                queryResults.getResults().getNumFound() > 0) {

            //For each of our possible steps loop retrieve the max of 10 results
            List<FacetField.Count> statusFilters  = queryResults.getFacetField("DSpaceStatus_filter").getValues();
            if(statusFilters != null){
                for (FacetField.Count count : statusFilters) {
                    if(count.getName().equals("Withdrawn")){
                        //Withdrawn items are not rendered here
                        continue;
                    }
                    
                    if(0 < count.getCount()){
                        if(count.getName().equals("Workflow")){
                            List<FacetField.Count> workflowSteps = queryResults.getFacetField("Workflowstep_filter").getValues();
                            if(workflowSteps != null){
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
            }


        } else {
            results.addPara(getNoResultsMessage());
        }
        //}// Empty query

    }

    protected Message getNoResultsMessage() {
        return T_no_results;
    }

    protected void renderResultBlock(Division results, FacetField.Count count) throws SearchServiceException, WingException, SQLException {
        //Perform a query for each of these workflow steps.
        SolrQuery solrQuery = getDefaultQueryArgs();
        if(count.getFacetField().getName().equals("DSpaceStatus_filter")){
            solrQuery.addFilterQuery("DSpaceStatus:" + count.getName());
        }else{
            solrQuery.addFilterQuery("Workflowstep:" + count.getName());
        }
        //Perform a search with our workflow step filter in place !
        QueryResponse queryResults = performSearch(queryArgs);

        Division workflowResultsDiv  = results.addInteractiveDivision("search-results-" + count.getName(),contextPath+"/submissions", Division.METHOD_POST);

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

        int row=1;
        if(solrResults.size()>0)
        {
           row=solrResults.size();
        }
        Table resultTable = workflowResultsDiv.addTable("results", row, 2);

        boolean showMoreUrl = false;
        if(solrResults.size() < solrResults.getNumFound()){
            showMoreUrl = true;
        }

        Row headerRow = resultTable.addRow(Row.ROLE_HEADER);

        if(count.getName().equalsIgnoreCase("Submission")){
            headerRow.addCell().addContent(message(""));
        }

        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.title"));
        headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.datafiles"));
        // add 'Last updated' col for the 'Unfinished Submissions' table for the div
        //      <div id="aspect.discovery.DiscoverySubmissions.div.search-results-Submission"
        // but not for the div
        //      <div id="aspect.discovery.DiscoverySubmissions.div.search-results-Archived"
        if (count.getName().equals("Submission")) {
            headerRow.addCell().addContent(message("xmlui.Submission.result-table.head.lastupdated"));
        }
        
        boolean showResult=false;
        for (SolrDocument doc : solrResults) {
            DSpaceObject resultDSO = SearchUtils.findDSpaceObject(context, doc);

            if (resultDSO instanceof Item) {
                showResult = true;
                Item item = (Item) resultDSO;
                Row itemRow = resultTable.addRow();

                if(item.isArchived()){
                    //Add the item url
                    itemRow.addCell().addXref(HandleManager.resolveToURL(context, item.getHandle()), item.getName());
                }else{

                    WorkspaceItem workspaceItem = WorkspaceItem.findByItem(context, item);

                    if(workspaceItem != null){

                        CheckBox selected = itemRow.addCell("workspaceitemcell_checkbox", Cell.ROLE_DATA, "inlineRow").addCheckBox("workspaceID");
                        selected.setLabel("select");
                        selected.addOption(workspaceItem.getID());

                        String title = item.getName();
                        if(title == null){
                            itemRow.addCell().addXref(contextPath + "/submit?workspaceID=" + workspaceItem.getID(), T_untitled);
                        }else{
                            itemRow.addCell().addXref(contextPath + "/submit?workspaceID=" + workspaceItem.getID(), title);
                        }

                    }else{

                        try {
                            WorkflowItem workflowItem = WorkflowItem.findByItemId(context, item.getID());
                            ClaimedTask claimedTask = null;
                            if (workflowItem != null) {
                                claimedTask = ClaimedTask.findByWorkflowIdAndEPerson(context, workflowItem.getID(), context.getCurrentUser().getID());
                            } else {
                                log.error("workflowItem is null for item " + item.getID());
                            }
                            if(claimedTask != null){
                                //We have a task for this item, so allow him to perform this task !
                                String url = contextPath+"/handle/"+ workflowItem.getCollection().getHandle()+"/workflow?" +
                                        "workflowID="+workflowItem.getID()+"&" +
                                        "stepID="+claimedTask.getStepID()+"&" +
                                        "actionID="+claimedTask.getActionID();

                                itemRow.addCell().addXref(url ,item.getName());
                            }else{
                                itemRow.addCell().addContent(item.getName());
                            }
                        } catch (AuthorizeException e) {
                            log.error(LogManager.getHeader(context, "Error while retrieving the workflow item for item with id: " + item.getID(), ""), e);
                            itemRow.addCell().addContent(item.getName());
                        } catch (IOException e) {
                            log.error(LogManager.getHeader(context, "Error while retrieving the workflow item for item with id: " + item.getID(), ""), e);
                            itemRow.addCell().addContent(item.getName());
                        }
                    }
                }
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, item);
                // add 'Number of data files' value
                itemRow.addCell().addContent(dataFiles.length);

                // add 'Last updated' value for the 'Unfinished submissions' table
                if (count.getName().equals("Submission")) {
                    Date lastModifiedDate = item.getLastModified();
                    //Check our data files if one has been altered after this one
                    for (Item dataFile : dataFiles) {
                        if (dataFile.getLastModified().after(lastModifiedDate))
                            lastModifiedDate = dataFile.getLastModified();
                    }
                    itemRow.addCell().addContent(lastModifiedDate.toString());
                }
            }
        }


        if (count.getName().equalsIgnoreCase("Submission")&&showResult) {
            headerRow = resultTable.addRow();
            Cell lastCell = headerRow.addCell(0,5);
            lastCell.addButton("submit_submissions_remove").setValue(T_s_submit_remove);
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
                    maskBuilder.append("&fq=Workflowstep:").append(count.getName());
                }
                moreUrl = maskBuilder.toString();
            }

            workflowResultsDiv.addPara().addXref(moreUrl).addContent(T_display_all);
        }
    }

    protected void buildSearchControls(Division div)
                throws WingException {
        Table controlsTable = div.addTable("search-controls", 1, 3);
        //Table controlsTable = div.addTable("search-controls", 1, 4);
        Row controlsRow = controlsTable.addRow(Row.ROLE_DATA);

        // Create a control for the number of records to display
        Cell rppCell = controlsRow.addCell();
        if(isStepFilterPage()){
            rppCell.addContent(T_rpp);
            Select rppSelect = rppCell.addSelect("rpp");
            for (int i : RESULTS_PER_PAGE_PROGRESSION) {
                rppSelect.addOption((i == getParameterRpp()), i, Integer.toString(i));
            }

        }

        /*
        Cell groupCell = controlsRow.addCell();
        try {
            // Create a drop down of the different sort columns available
            groupCell.addContent(T_group_by);
            Select groupSelect = groupCell.addSelect("group_by");
            groupSelect.addOption(false, "none", T_group_by_none);


            String[] groups = {"publication_grp"};
            for (String group : groups) {
                groupSelect.addOption(group.equals(getParameterGroup()), group,
                        message("xmlui.ArtifactBrowser.AbstractSearch.group_by." + group));
            }

        }
        catch (Exception se) {
            throw new WingException("Unable to get group options", se);
        }
        */

        Cell sortCell = controlsRow.addCell();
        // Create a drop down of the different sort columns available
        sortCell.addContent(T_sort_by);
        Select sortSelect = sortCell.addSelect("sort_by");
        sortSelect.addOption(false, "score", T_sort_by_relevance);
        for (String sortField : SearchUtils.getSortFields()) {
            sortField += "_sort";
            sortSelect.addOption((sortField.equals(getParameterSortBy())), sortField,
                    message("xmlui.ArtifactBrowser.AbstractSearch.sort_by." + sortField));
        }

        // Create a control to changing ascending / descending order
        Cell orderCell = controlsRow.addCell();
        orderCell.addContent(T_order);
        Select orderSelect = orderCell.addSelect("order");
        orderSelect.addOption(SortOption.ASCENDING.equals(getParameterOrder()), SortOption.ASCENDING, T_order_asc);
        orderSelect.addOption(SortOption.DESCENDING.equals(getParameterOrder()), SortOption.DESCENDING, T_order_desc);
    }


    public void addViewMoreUrl(org.dspace.app.xmlui.wing.element.List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        String parameters = retrieveParameters(request);
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/discovery-my-tasks-search-filter?" + parameters + BrowseFacet.FACET_FIELD + "=" + fieldName,
                T_VIEW_MORE

        );
    }

    /**
     * A method indicating wether or not we are filtering on a certain step
     * If we are filtering different options become available
     * @return true if we are on a step filter page
     */
    protected boolean isStepFilterPage(){
        Request request = ObjectModelHelper.getRequest(objectModel);
        String[] filterQueries = request.getParameterValues("fq");
        if(filterQueries != null){
            for(String fq : filterQueries){
                if(fq.startsWith("Workflowstep:") || fq.startsWith("DSpaceStatus:") || fq.startsWith("WorkflowstepTask:")){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Query DSpace for a list of all items / collections / or communities that
     * match the given search query.
     *
     * @return The associated query results.
     */
    public void performSearch(DSpaceObject scope) throws UIException, SearchServiceException {
        performSearch(getDefaultQueryArgs());

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

        filterQueries.add("SubmitterName_filter:\"" + eperson.getName() + "\"");


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
	 * Returns all the filter queries for use by solr This method returns more
	 * expanded filter queries then the getParameterFilterQueries
	 *
	 * @return an array containing the filter queries
	 */
	protected String[] getSolrFilterQueries() {
		try {
			java.util.List<String> allFilterQueries = new ArrayList<String>();
			Request request = ObjectModelHelper.getRequest(objectModel);
			java.util.List<String> fqs = new ArrayList<String>();

			if (request.getParameterValues("fq") != null) {
				fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
			}

			String type = request.getParameter("filtertype");
			String value = request.getParameter("filter");

			if (value != null
					&& !value.equals("")
					&& request
							.getParameter("submit_search-filter-controls_add") != null) {
				String exactFq = (type.equals("*") ? "" : type + ":") + value;
				fqs.add(exactFq + " OR " + exactFq + "*");
			}

			for (String fq : fqs) {
				// Do not put a wildcard after a range query
				if (fq.matches(".*\\:\\[.* TO .*\\](?![a-z 0-9]).*")) {
					allFilterQueries.add(fq);
				}
				else {
					allFilterQueries.add(fq.endsWith("*") ? fq : fq + " OR "
							+ fq + "*");
				}
			}
            //Check if our current user is an admin, if so we need to show only workflow tasks assigned to him
            Context context = ContextUtil.obtainContext(objectModel);
            if(AuthorizeManager.isAdmin(context)){
                StringBuffer adminQuery = new StringBuffer();
                EPerson currentUser = context.getCurrentUser();
                adminQuery.append("WorkflowEpersonId:").append(currentUser.getID());
                //Retrieve all the groups this user is a part of
                Set<Integer> groupIdentifiers = Group.allMemberGroupIDs(this.context, currentUser);
                for(int groupId : groupIdentifiers){
                    adminQuery.append(" OR WorkflowGroupId:").append(groupId);
                }
                adminQuery.append(" OR (SubmitterName_filter:").append(context.getCurrentUser().getName())
                          .append(" AND DSpaceStatus:Submission)");
                allFilterQueries.add(adminQuery.toString());
            }


			return allFilterQueries
					.toArray(new String[allFilterQueries.size()]);
		}
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e) {
            log.error("Error while retrieving solr filter queries", e);
			return null;
		}
	}

    public Message getHead() {
        return T_title;
    }

    @Override
    protected String getDiscoverUrl(){
        return "submissions";
    }

    @Override
    public String getView() {
        return "discoverySubmissions";
    }
}
