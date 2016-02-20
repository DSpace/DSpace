/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.DSpaceObject;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.sort.SortOption;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class AbstractDiscoveryTransformer extends AbstractDSpaceTransformer {

    protected static final String OFFSET = "offset";
    protected static String STARTS_WITH = "starts_with";
    private static final Message T_starts_with = message("xmlui.Discovery.AbstractSearch.startswith");
    protected static final Message T_starts_with_help = message("xmlui.Discovery.AbstractSearch.startswith.help");
    protected static final Message T_go = message("xmlui.general.go");

    protected abstract void setupQueryArgs();


    /**
     * Cached query results
     */
    protected DiscoverResult queryResults;

    /**
     * Cached query arguments
     */
    protected DiscoverQuery queryArgs;

    /**
     * The options for results per page
     */
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5, 10, 20, 40, 60, 80, 100};


    protected void addFacetResults(Division results) throws WingException, UnsupportedEncodingException,SQLException {
        if (this.queryResults != null) {

            Map<String, List<DiscoverResult.FacetResult>> facetFields = this.queryResults.getFacetResults();
            if (facetFields == null)
            {
                facetFields = new LinkedHashMap<String, List<DiscoverResult.FacetResult>>();
            }

//            facetFields.addAll(this.queryResults.getFacetDates());


            if (facetFields.size() > 0) {
                String facetField = facetFields.keySet().toArray(new String[facetFields.size()])[0];
                List<DiscoverResult.FacetResult> values = facetFields.get(facetField);



                if (values != null && 0 < values.size()) {

                    addSimplePagination(results,values.size());


                    List<String> filterQueries = new ArrayList<String>();
                    if(getParameterFilterQueries() != null)
                    {
                        filterQueries = getParameterFilterQueries();
                    }


                    int end = values.size();
                    if(getParameterRpp() < end)
                    {
                        end = getParameterRpp();
                    }
                    Table singleTable = results.addTable("browse-by-" + facetField + "-results", (int) (queryResults.getDspaceObjects().size() + 1), 1) ;
                    for (int i = 0; i < end; i++) {
                        DiscoverResult.FacetResult value = values.get(i);
                        renderFacetField(facetField, singleTable, filterQueries, value);
                    }
                }else{
                    results.addPara(message("xmlui.discovery.SearchFacetFilter.no-results"));
                }
            }
        }
    }



    protected String modifyValue(DiscoverResult.FacetResult value){
        return  value.getDisplayedValue()+ " (" + value.getCount() + ")";
    }


    protected void renderFacetField(String facetField, Table singleTable, List<String> filterQueries, DiscoverResult.FacetResult value) throws SQLException, WingException, UnsupportedEncodingException {

        String displayedValue = value.getDisplayedValue();
        String filterQuery = value.getAsFilterQuery();

        Cell cell = singleTable.addRow().addCell();

        //No use in selecting the same filter twice
        if(filterQueries.contains(filterQuery)){
            cell.addContent(displayedValue + " (" + value.getCount() + ")");
        } else {
            //Add the basics
            Map<String, String> urlParams = new HashMap<String, String>();
            urlParams.putAll(getFilteredParameters());
            String url = generateFacetURL(value);
            //Add already existing filter queries
            url = addFilterQueriesToUrl(url);
            //Last add the current filter query
            url += url.contains("?")?"&fq=":"?fq=" + URLEncoder.encode(filterQuery, "UTF-8");
            cell.addXref(url, modifyValue(value)
            );
        }
    }

    protected abstract String getField();

    protected abstract String generateFacetURL(DiscoverResult.FacetResult value) throws UnsupportedEncodingException;



    protected void addDSpaceObjectResults(Division results) throws WingException {
        ReferenceSet referenceSet = null;

         // Put in palce top level referenceset
        referenceSet = results.addReferenceSet("search-results-repository",
                ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");


        for (DSpaceObject resultDso : queryResults.getDspaceObjects())
        {


                referenceSet.addReference(resultDso);

        }

    }

    protected int getParameterPage() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("page"));
        }
        catch (Exception e) {
            return 1;
        }
    }

    protected int getParameterRpp() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("rpp"));
        }
        catch (Exception e) {
            return 10;
        }
    }

    protected String getParameterSortBy() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
        return s != null ? s : null;
    }

    protected String getParameterGroup() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");
        return s != null ? s : "none";
    }

    protected String getParameterOrder() {
        return ObjectModelHelper.getRequest(objectModel).getParameter("order");
    }

    protected String getParameterScope() {
        return ObjectModelHelper.getRequest(objectModel).getParameter("scope");
    }

    protected int getParameterEtAl() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("etal"));
        }
        catch (Exception e) {
            return 0;
        }
    }


    protected static final Message T_sort_by_relevance =
            message("xmlui.ArtifactBrowser.AbstractSearch.sort_by.relevance");

    protected static final Message T_sort_by = message("xmlui.ArtifactBrowser.AbstractSearch.sort_by");

    protected static final Message T_order = message("xmlui.ArtifactBrowser.AbstractSearch.order");
    protected static final Message T_order_asc = message("xmlui.ArtifactBrowser.AbstractSearch.order.asc");
    protected static final Message T_order_desc = message("xmlui.ArtifactBrowser.AbstractSearch.order.desc");

    protected static final Message T_rpp = message("xmlui.ArtifactBrowser.AbstractSearch.rpp");


    protected static final Message T_sort_head = message("xmlui.Discovery.SimpleSearch.sort_head");
    protected static final Message T_sort_button = message("xmlui.Discovery.SimpleSearch.sort_apply");


    protected Map<String, String> getFilteredParameters() {
        return getFilteredParameters((HashSet) null);
    }

    protected Map<String,String> getFilteredParameters(HashSet<String> filters){
        Map<String, String> fq = new HashMap<String, String>();
        fq.putAll(ObjectModelHelper.getRequest(objectModel).getParameters());
        if(filters!=null){
            for(String filter:filters){
                fq.remove(filter);
            }
        }
        return fq;
    }

    protected Map<String,String> getFilteredParameters(String[] filters){
        Map<String, String> fq = new HashMap<String, String>();
        fq.putAll(ObjectModelHelper.getRequest(objectModel).getParameters());
        if(filters!=null){
            for(String filter:filters){
                fq.remove(filter);
            }
        }
        return fq;
    }


    public String addFilterQueriesToUrl(String url) throws UnsupportedEncodingException {
        List<String> fqs = getParameterFilterQueries();
        if (fqs != null) {
            StringBuilder urlBuilder = new StringBuilder(url);
            for (String fq : fqs) {
                urlBuilder.append("&fq=").append(URLEncoder.encode(fq, "UTF-8"));
            }

            return urlBuilder.toString();
        }

        return url;
    }


    private String getNextPageURL() throws UnsupportedEncodingException {
        int offSet = getParameterOffSet();
        if (offSet == -1)
        {
            offSet = 0;
        }

        Map<String, String> parameters = getFilteredParameters();
        parameters.put(OFFSET, String.valueOf(offSet + getParameterRpp()));

        //TODO: correct  comm/collection url
        // Add the filter queries
        String url = generateURL(getURL(), parameters);
        url = addFilterQueriesToUrl(url);

        return url;
    }


    protected void buildSearchControls(Division div)
            throws WingException, SQLException {

        Division searchControlsDiv = div.addInteractiveDivision("search-controls",
                "browse-by", Division.METHOD_GET, "discover-sort-box search");

        org.dspace.app.xmlui.wing.element.List controlsList = searchControlsDiv.addList("search-controls", org.dspace.app.xmlui.wing.element.List.TYPE_FORM);

        addHiddenFormFields("search",ObjectModelHelper.getRequest(objectModel),searchControlsDiv);
        controlsList.setHead(T_sort_head);
        //Table controlsTable = div.addTable("search-controls", 1, 4);

        org.dspace.app.xmlui.wing.element.Item controlsItem = controlsList.addItem();
        // Create a control for the number of records to display
        buildPagination(controlsItem,T_rpp);


        // Create a drop down of the different sort columns available
        //
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);
        buildSortControl(controlsItem, discoveryConfiguration,T_sort_by);

        // Create a control to changing ascending / descending order

        buildOrderControl(controlsItem, discoveryConfiguration, T_order);

        controlsItem.addButton("submit").setValue(T_sort_button);


    }


    private String getPreviousPageURL() throws UnsupportedEncodingException {
        //If our offset should be 0 then we shouldn't be able to view a previous page url
        if (0 == queryArgs.getFacetOffset() && getParameterOffSet() == -1)
        {
            return null;
        }

        int offset = getParameterOffSet();
        if(offset == -1 || offset == 0)
        {
            return null;
        }

        Map<String, String> parameters = getFilteredParameters();
        parameters.put(OFFSET, String.valueOf(offset - getParameterRpp()));

        //TODO: correct  comm/collection url
        // Add the filter queries
        String url = generateURL(getURL(), parameters);
        url = addFilterQueriesToUrl(url);
        return url;
    }

    protected int getParameterOffSet() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("offset"));
        }
        catch (Exception e) {
            return 0;
        }
    }


    protected void addSimplePagination(Division results, int size) throws UnsupportedEncodingException {
        // Find our faceting offset
        int offSet = queryArgs.getFacetOffset();
        if(offSet == -1){
            offSet = 0;
        }

        //Only show the nextpageurl if we have at least one result following our current results
        String nextPageUrl = null;
        if (size >= (getParameterRpp()))
        {
            nextPageUrl = getNextPageURL();
        }



        int shownItemsMax = offSet + (getParameterRpp() < size ? size - 1 : size);



        // We put our total results to -1 so this doesn't get shown in the results (will be hidden by the xsl)
        // The reason why we do this is because solr 1.4 can't retrieve the total number of facets found
        results.setSimplePagination(-1, offSet+1,
                shownItemsMax, offSet==0?null:getPreviousPageURL(), nextPageUrl);
    }

    protected void buildOrderControl(org.dspace.app.xmlui.wing.element.Item controlsItem, DiscoveryConfiguration discoveryConfiguration, Message orderMessage) throws WingException {
        controlsItem.addContent(orderMessage);
        Select orderSelect = controlsItem.addSelect("order");

        String parameterOrder = getParameterOrder();
        if(parameterOrder == null && discoveryConfiguration.getSearchSortConfiguration() != null) {
            parameterOrder = discoveryConfiguration.getSearchSortConfiguration().getDefaultSortOrder().toString();
        }
        orderSelect.addOption(SortOption.ASCENDING.equalsIgnoreCase(parameterOrder), SortOption.ASCENDING, T_order_asc);
        orderSelect.addOption(SortOption.DESCENDING.equalsIgnoreCase(parameterOrder), SortOption.DESCENDING, T_order_desc);
    }

    protected void buildSortControl(org.dspace.app.xmlui.wing.element.Item controlsItem, DiscoveryConfiguration discoveryConfiguration, Message sortByMessage) throws WingException {
        controlsItem.addContent(sortByMessage);
        Select sortSelect = controlsItem.addSelect("sort_by");
        sortSelect.addOption(false, "score", T_sort_by_relevance);


        DiscoverySortConfiguration searchSortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        if(searchSortConfiguration != null){
            for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration.getSortFields()) {
                String sortField = SearchUtils.getSearchService().toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());

                String currentSort = getParameterSortBy();
                sortSelect.addOption((sortField.equals(currentSort) || sortFieldConfiguration.equals(searchSortConfiguration.getDefaultSort())), sortField,
                        message("xmlui.ArtifactBrowser.AbstractSearch.sort_by." + sortField));
            }
        }
    }

    protected void buildPagination(org.dspace.app.xmlui.wing.element.Item controlsItem,Message paginationMessage) throws WingException {
        controlsItem.addContent(paginationMessage);
        Select rppSelect = controlsItem.addSelect("rpp");
        for (int i : RESULTS_PER_PAGE_PROGRESSION) {
            rppSelect.addOption((i == getParameterRpp()), i, Integer.toString(i));
        }
    }

    protected abstract String getURL();

    protected List<String> getParameterFilterQueries() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        List<String> fqs = new ArrayList<String>(request.getParameters().size());
        if (request.getParameterValues("fq") != null) {
            fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
        }

        //Have we added a filter using the UI
        if (request.getParameter("filter") != null && !"".equals(request.getParameter("filter"))) {
            fqs.add((request.getParameter("filtertype").equals("*") ? "" : request.getParameter("filtertype") + ":") + request.getParameter("filter"));
        }
        return fqs;

    }


    /**
     * Since the layout is creating separate forms for each search part
     * this method will add hidden fields containing the values from other form parts
     *
     * @param type     the type of our form
     * @param request  the request

     * @param division the division that requires the hidden fields
     * @throws org.dspace.app.xmlui.wing.WingException will never occur
     */
    protected void addHiddenFormFields(String type, Request request, Division division) throws WingException {

        if (type.equals("search") || type.equals("filter")) {
            if (request.getParameter("rpp") != null) {
                division.addHidden("rpp").setValue(request.getParameter("rpp"));
            }
            if (request.getParameter(STARTS_WITH) != null) {
                division.addHidden(STARTS_WITH).setValue(request.getParameter(STARTS_WITH));
            }
            if (request.getParameter("sort_by") != null) {
                division.addHidden("sort_by").setValue(request.getParameter("sort_by"));
            }
            if (request.getParameter("order") != null && !request.getParameter("order").isEmpty()) {
                division.addHidden("order").setValue(request.getParameter("order"));
            }
        }
    }

    protected void addJumpList(Division div) throws SQLException, WingException, UnsupportedEncodingException {

        String action;

            action = contextPath + "/"+getURL();


        Division jump = div.addInteractiveDivision("filter-navigation", action,
                Division.METHOD_POST, "secondary navigation");


        // Add all the query parameters as hidden fields on the form
        for(Map.Entry<String, String> param : getFilteredParameters(new String[]{STARTS_WITH}).entrySet()){
            jump.addHidden(param.getKey()).setValue(param.getValue());
        }
        List<String> filterQueries = getParameterFilterQueries();
        for (String filterQuery : filterQueries) {
            jump.addHidden("fq").setValue(filterQuery);
        }

        //We cannot create a filter for dates
        // Create a clickable list of the alphabet
            org.dspace.app.xmlui.wing.element.List jumpList = jump.addList("jump-list", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "alphabet");

            //Create our basic url
            String basicUrl = generateURL(getURL()+"?", getFilteredParameters(new String[]{STARTS_WITH}));
            //Add any filter queries
            basicUrl = addFilterQueriesToUrl(basicUrl);

            //TODO: put this back !
//            jumpList.addItemXref(generateURL("browse", letterQuery), "0-9");
            for (char c = 'A'; c <= 'Z'; c++)
            {
                String linkUrl = basicUrl + "&" + STARTS_WITH +  "=" + Character.toString(c).toLowerCase();
                jumpList.addItemXref(linkUrl, Character
                        .toString(c));
            }

            // Create a free text field for the initial characters
            Para jumpForm = jump.addPara();
            jumpForm.addContent(T_starts_with);
            jumpForm.addText("starts_with").setHelp(T_starts_with_help);

            jumpForm.addButton("submit").setValue(T_go);
        }
}
