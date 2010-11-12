/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.RequestUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Filter which displays facets on which a user can filter his discovery search
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SearchFacetFilter extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(BrowseFacet.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_starts_with = message("xmlui.Discovery.AbstractSearch.startswith");
    private static final Message T_starts_with_help = message("xmlui.Discovery.AbstractSearch.startswith.help");

    /**
     * The cache of recently submitted items
     */
    protected QueryResponse queryResults;
    /**
     * Cached validity object
     */
    protected SourceValidity validity;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

    private int DEFAULT_PAGE_SIZE = 10;


    private ConfigurationService config = null;

    private SearchService searchService = null;
    private static final Message T_go = message("xmlui.general.go");

    public SearchFacetFilter() {

        DSpace dspace = new DSpace();
        config = dspace.getConfigurationService();
        searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(),SearchService.class);

    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0";
            }

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle) {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceValidity validity = new DSpaceValidity();

                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso != null) {
                    // Add the actual collection;
                    validity.add(dso);
                }

                // add reciently submitted items, serialize solr query contents.
                QueryResponse response = getQueryResponse(dso);

                validity.add("numFound:" + response.getResults().getNumFound());

                for (SolrDocument doc : response.getResults()) {
                    validity.add(doc.toString());
                }

                for (SolrDocument doc : response.getResults()) {
                    validity.add(doc.toString());
                }

                for (FacetField field : response.getFacetFields()) {
                    validity.add(field.getName());

                    for (FacetField.Count count : field.getValues()) {
                        validity.add(count.getName() + count.getCount());
                    }
                }


                this.validity = validity.complete();
            }
            catch (Exception e) {
                // Just ignore all errors and return an invalid cache.
            }

            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }

    /**
     * Get the recently submitted items for the given community or collection.
     *
     * @param scope The collection.
     */
    protected QueryResponse getQueryResponse(DSpaceObject scope) {


        Request request = ObjectModelHelper.getRequest(objectModel);

        if (queryResults != null)
        {
            return queryResults;
        }

        queryArgs = new SolrQuery();

        //Make sure we add our default filters
        queryArgs.addFilterQuery(SearchUtils.getDefaultFilters("search"));


        queryArgs.setQuery("search.resourcetype: " + Constants.ITEM + ((request.getParameter("query") != null && !"".equals(request.getParameter("query"))) ? " AND (" + request.getParameter("query") + ")" : ""));
//        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(0);

        queryArgs.addFilterQuery(getParameterFilterQueries());


        //Set the default limit to 11
        //query.setFacetLimit(11);
        queryArgs.setFacetMinCount(1);

        //sort
        //TODO: why this kind of sorting ? Should the sort not be on how many times the value appears like we do in the filter by sidebar ?
        queryArgs.setFacetSort(config.getPropertyAsType("solr.browse.sort","lex"));

        queryArgs.setFacet(true);
        String facetField = request.getParameter(SearchFilterParam.FACET_FIELD);


        int offset = RequestUtils.getIntParameter(request, SearchFilterParam.OFFSET);
        if (offset == -1)
        {
            offset = 0;
        }
        if(facetField.endsWith(".year")){
//            TODO: dates are now handled in another way, throw this away?
            queryArgs.setParam(FacetParams.FACET_OFFSET, "0");
            queryArgs.setParam(FacetParams.FACET_LIMIT, "1000000");

        } else {
            queryArgs.setParam(FacetParams.FACET_OFFSET, String.valueOf(offset));

            //We add +1 so we can use the extra one to make sure that we need to show the next page
            queryArgs.setParam(FacetParams.FACET_LIMIT, String.valueOf(DEFAULT_PAGE_SIZE + 1));
        }


        if (scope != null) /* top level search / community */ {
            if (scope instanceof Community) {
                queryArgs.setFilterQueries("location:m" + scope.getID());
            } else if (scope instanceof Collection) {
                queryArgs.setFilterQueries("location:l" + scope.getID());
            }
        }


        boolean isDate = false;
        if(facetField.endsWith("_dt")){
            facetField = facetField.split("_")[0];
            isDate = true;
        }

        if (isDate) {

            queryArgs.setParam(FacetParams.FACET_DATE, facetField);
            queryArgs.setParam(FacetParams.FACET_DATE_GAP,"+1YEAR");

            Date lowestDate = getLowestDateValue(queryArgs.getQuery(), facetField, queryArgs.getFilterQueries());
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);

            DateFormat formatter = new SimpleDateFormat("yyyy");
            int maxEndYear = Integer.parseInt(formatter.format(lowestDate));

            //Since we have a date, we need to find the last year
            String startDate = "NOW/YEAR-" + SearchUtils.getConfig().getString("solr.date.gap", "10") + "YEARS";
            String endDate =  "NOW";
            int startYear =  thisYear - (offset + DEFAULT_PAGE_SIZE);
            // We shouldn't go lower then our max bottom year
            // Make sure to substract one so the bottom year is also counted !
            if(startYear < maxEndYear)
            {
                startYear = maxEndYear - 1;
            }

            if(0 < offset){
                //Say that we have an offset of 10 years
                //we need to go back 10 years (2010 - (2010 - 10))
                //(add one to compensate for the NOW in the start)
                int endYear = thisYear - offset + 1;

                endDate = "NOW/YEAR-" + (thisYear - endYear) + "YEARS";
                //Add one to the startyear to get one more result
                //When we select NOW, the current year is also used (so auto+1)
            }
            startDate = "NOW/YEAR-" + (thisYear - startYear) + "YEARS";

            queryArgs.setParam(FacetParams.FACET_DATE_START, startDate);
            queryArgs.setParam(FacetParams.FACET_DATE_END, endDate);

            System.out.println(startDate);
            System.out.println(endDate);



        } else {
            if(request.getParameter(SearchFilterParam.STARTS_WITH) != null)
            {
                queryArgs.setFacetPrefix(facetField, request.getParameter(SearchFilterParam.STARTS_WITH).toLowerCase());
            }

            queryArgs.addFacetField(facetField);
        }

        try {
            queryResults = searchService.search(queryArgs);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        return queryResults;
    }

        /**
     * Retrieves the lowest date value in the given field
     * @param query a solr query
     * @param dateField the field for which we want to retrieve our date
     * @param filterquery the filterqueries
     * @return the lowest date found, in a date object
     */
    private Date getLowestDateValue(String query, String dateField, String... filterquery){


        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields(dateField);
            solrQuery.setRows(1);
            solrQuery.setSortField(dateField, SolrQuery.ORDER.asc);
            solrQuery.setFilterQueries(filterquery);

            QueryResponse rsp = searchService.search(solrQuery);
            if(0 < rsp.getResults().getNumFound()){
                return (Date) rsp.getResults().get(0).getFieldValue(dateField);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = request.getParameter(SearchFilterParam.FACET_FIELD);

        pageMeta.addMetadata("title").addContent(message("xmlui.Discovery.AbstractSearch.type_" + facetField));


        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(message("xmlui.Discovery.AbstractSearch.type_" + facetField));
    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        SearchFilterParam browseParams = new SearchFilterParam(request);
        // Build the DRI Body
        Division div = body.addDivision("browse-by-" + request.getParameter(SearchFilterParam.FACET_FIELD), "primary");

        addBrowseJumpNavigation(div, browseParams, request);

        // Set up the major variables
        //Collection collection = (Collection) dso;

        // Build the collection viewer division.

        //Make sure we get our results
        queryResults = getQueryResponse(dso);
        if (this.queryResults != null) {

            java.util.List<FacetField> facetFields = this.queryResults.getFacetFields();
            if (facetFields == null)
            {
                facetFields = new ArrayList<FacetField>();
            }

            facetFields.addAll(this.queryResults.getFacetDates());


            if (facetFields.size() > 0) {
                FacetField field = facetFields.get(0);
                java.util.List<FacetField.Count> values = field.getValues();
                if(field.getGap() != null){
                    //We are dealing with dates so flip em, top date comes first
                    Collections.reverse(values);

                }

                Division results = body.addDivision("browse-by-" + field + "-results", "primary");

                results.setHead(message("xmlui.Discovery.AbstractSearch.type_" + browseParams.getFacetField()));
                if (values != null && 0 < values.size()) {



                    // Find our faceting offset
                    int offSet = 0;
                    try {
                        offSet = Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET));
                    } catch (NumberFormatException e) {
                        //Ignore
                    }

                    //Only show the nextpageurl if we have at least one result following our current results
                    String nextPageUrl = null;
                    if(field.getName().endsWith(".year")){
                        offSet = Util.getIntParameter(request, SearchFilterParam.OFFSET);
                        offSet = offSet == -1 ? 0 : offSet;

                        if ((offSet + DEFAULT_PAGE_SIZE) < values.size())
                        {
                            nextPageUrl = getNextPageURL(browseParams, request);
                        }
                    }else{
                        if (values.size() == (DEFAULT_PAGE_SIZE + 1))
                        {
                            nextPageUrl = getNextPageURL(browseParams, request);
                        }
                    }


                    int shownItemsMax;


                    if(field.getName().endsWith(".year")){
                        if((values.size() - offSet) < DEFAULT_PAGE_SIZE)
                        {
                            shownItemsMax = values.size();
                        }
                        else
                        {
                            shownItemsMax = DEFAULT_PAGE_SIZE;
                        }
                    }else{
                        shownItemsMax = offSet + (DEFAULT_PAGE_SIZE < values.size() ? values.size() - 1 : values.size());

                    }

                    // We put our total results to -1 so this doesn't get shown in the results (will be hidden by the xsl)
                    // The reason why we do this is because solr 1.4 can't retrieve the total number of facets found
                    results.setSimplePagination(-1, offSet + 1,
                                                    shownItemsMax, getPreviousPageURL(browseParams, request), nextPageUrl);

                    Table singleTable = results.addTable("browse-by-" + field + "-results", (int) (queryResults.getResults().getNumFound() + 1), 1);

                    List<String> filterQueries = new ArrayList<String>();
                    if(request.getParameterValues("fq") != null)
                    {
                        filterQueries = Arrays.asList(request.getParameterValues("fq"));
                    }

                    if(field.getName().endsWith(".year")){
                        int start = (values.size() - 1) - offSet;
                        int end = start - DEFAULT_PAGE_SIZE;
                        if(end < 0)
                        {
                            end = 0;
                        }
                        else
                        {
                            end++;
                        }
                        for(int i = start; end <= i; i--)
                        {
                            FacetField.Count value = values.get(i);

                            renderFacetField(browseParams, dso, field, singleTable, filterQueries, value);
                        }
                    }else{
                        int end = values.size();
                        if(DEFAULT_PAGE_SIZE < end)
                        {
                            end = DEFAULT_PAGE_SIZE;
                        }


                        for (int i = 0; i < end; i++) {
                            FacetField.Count value = values.get(i);

                            renderFacetField(browseParams, dso, field, singleTable, filterQueries, value);
                        }
                    }


                }else{
                    results.addPara(message("xmlui.discovery.SearchFacetFilter.no-results"));
                }
            }
        }
    }

    private void addBrowseJumpNavigation(Division div, SearchFilterParam browseParams, Request request)
            throws WingException
    {
        Division jump = div.addInteractiveDivision("filter-navigation", contextPath + "/search-filter",
                Division.METHOD_POST, "secondary navigation");

        Map<String, String> params = new HashMap<String, String>();
        params.putAll(browseParams.getCommonBrowseParams());
        // Add all the query parameters as hidden fields on the form
        for(Map.Entry<String, String> param : params.entrySet()){
            jump.addHidden(param.getKey()).setValue(param.getValue());
        }
        String[] filterQueries = getParameterFilterQueries();
        for (String filterQuery : filterQueries) {
            jump.addHidden("fq").setValue(filterQuery);
        }

        //We cannot create a filter for dates
        if(!browseParams.getFacetField().endsWith(".year")){
            // Create a clickable list of the alphabet
            org.dspace.app.xmlui.wing.element.List jumpList = jump.addList("jump-list", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "alphabet");

            //Create our basic url
            String basicUrl = generateURL("search-filter", params);
            //Add any filter queries
            basicUrl = addFilterQueriesToUrl(basicUrl);

            //TODO: put this back !
//            jumpList.addItemXref(generateURL("browse", letterQuery), "0-9");
            for (char c = 'A'; c <= 'Z'; c++)
            {
                String linkUrl = basicUrl + "&" +  SearchFilterParam.STARTS_WITH +  "=" + Character.toString(c).toLowerCase();
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

    private void renderFacetField(SearchFilterParam browseParams, DSpaceObject dso, FacetField field, Table singleTable, List<String> filterQueries, FacetField.Count value) throws SQLException, WingException, UnsupportedEncodingException {
        String displayedValue = value.getName();
        String filterQuery = value.getAsFilterQuery();
        if (field.getName().equals("location.comm") || field.getName().equals("location.coll")) {
            //We have a community/collection, resolve it to a dspaceObject
            displayedValue = SolrServiceImpl.locationToName(context, field.getName(), displayedValue);
        }
        if(field.getGap() != null){
            //We have a date get the year so we can display it
            DateFormat simpleDateformat = new SimpleDateFormat("yyyy");
            displayedValue = simpleDateformat.format(SolrServiceImpl.toDate(displayedValue));
            filterQuery = ClientUtils.escapeQueryChars(value.getFacetField().getName()) + ":" + displayedValue + "*";
        }

        Cell cell = singleTable.addRow().addCell();

        //No use in selecting the same filter twice
        if(filterQueries.contains(filterQuery)){
            cell.addContent(displayedValue + " (" + value.getCount() + ")");
        } else {
            //Add the basics
            Map<String, String> urlParams = new HashMap<String, String>();
            urlParams.putAll(browseParams.getCommonBrowseParams());
            String url = generateURL(contextPath + (dso == null ? "" : "/handle/" + dso.getHandle()) + "/discover", urlParams);
            //Add already existing filter queries
            url = addFilterQueriesToUrl(url);
            //Last add the current filter query
            url += "&fq=" + URLEncoder.encode(filterQuery, "UTF-8");
            cell.addXref(url, displayedValue + " (" + value.getCount() + ")"
            );
        }
    }

    private String getNextPageURL(SearchFilterParam browseParams, Request request) {
        int offSet = Util.getIntParameter(request, SearchFilterParam.OFFSET);
        if (offSet == -1)
        {
            offSet = 0;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.putAll(browseParams.getCommonBrowseParams());
        parameters.putAll(browseParams.getControlParameters());
        parameters.put(SearchFilterParam.OFFSET, String.valueOf(offSet + DEFAULT_PAGE_SIZE));

        //TODO: correct  comm/collection url
        // Add the filter queries
        String url = generateURL("search-filter", parameters);
        url = addFilterQueriesToUrl(url);

        return url;
    }

    private String getPreviousPageURL(SearchFilterParam browseParams, Request request) {
        //If our offset should be 0 then we shouldn't be able to view a previous page url
        if ("0".equals(queryArgs.get(FacetParams.FACET_OFFSET)) && Util.getIntParameter(request, "offset") == -1)
        {
            return null;
        }

        int offset = Util.getIntParameter(request, SearchFilterParam.OFFSET);
        if(offset == -1 || offset == 0)
        {
            return null;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.putAll(browseParams.getCommonBrowseParams());
        parameters.putAll(browseParams.getControlParameters());
        parameters.put(SearchFilterParam.OFFSET, String.valueOf(offset - DEFAULT_PAGE_SIZE));

        //TODO: correct  comm/collection url
        // Add the filter queries
        String url = generateURL("search-filter", parameters);
        url = addFilterQueriesToUrl(url);
        return url;
    }


    /**
     * Recycle
     */
    public void recycle() {
        // Clear out our item's cache.
        this.queryResults = null;
        this.validity = null;
        super.recycle();
    }

    public String addFilterQueriesToUrl(String url){
        String[] fqs = getParameterFilterQueries();
        if (fqs != null) {
            StringBuilder urlBuilder = new StringBuilder(url);
            for (String fq : fqs) {
                urlBuilder.append("&fq=").append(fq);
            }

            return urlBuilder.toString();
        }

        return url;
    }


    private String[] getParameterFilterQueries() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        return request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0];
    }

    private static class SearchFilterParam {
        private Request request;

        /** The always present commond params **/
        public static final String FACET_FIELD = "field";

        /** The browse control params **/
        public static final String OFFSET = "offset";
        public static final String STARTS_WITH = "starts_with";


        private SearchFilterParam(Request request){
            this.request = request;
        }

        public String getFacetField(){
            return request.getParameter(FACET_FIELD);
        }

        public Map<String, String> getCommonBrowseParams(){
            Map<String, String> result = new HashMap<String, String>();
            result.put(FACET_FIELD, request.getParameter(FACET_FIELD));
            return result;
        }

        public Map<String, String> getControlParameters(){
            Map<String, String> paramMap = new HashMap<String, String>();

            paramMap.put(OFFSET, request.getParameter(OFFSET));
            if(request.getParameter(STARTS_WITH) != null)
            {
                paramMap.put(STARTS_WITH, request.getParameter(STARTS_WITH));
            }

            return paramMap;
        }

    }
}
