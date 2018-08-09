/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT;

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
    private static SORT sortOrder;
    /**
     * The cache of recently submitted items
     */
    protected DiscoverResult queryResults;
    /**
     * Cached validity object
     */
    protected SourceValidity validity;

    /**
     * Cached query arguments
     */
    protected DiscoverQuery queryArgs;

    private int DEFAULT_PAGE_SIZE = 10;
    private int currentOffset = 0;


    private SearchService searchService = null;
    private static final Message T_go = message("xmlui.general.go");
    private static final Message T_rpp = message("xmlui.Discovery.AbstractSearch.rpp");
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5, 10, 20, 40, 60, 80, 100};

    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();


    public SearchFacetFilter() {

        searchService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(SearchService.class.getName(),SearchService.class);

    }

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = request.getParameter(SearchFilterParam.FACET_FIELD);

        if(StringUtils.isBlank(facetField))
        {
            throw new BadRequestException("Invalid " + SearchFilterParam.FACET_FIELD + " parameter");
        }
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     * @return the key.
     */
    @Override
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
     * <p>
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarchy, when this changes they will not be reflected in the cache.
     * @return the validity.
     */
    @Override
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceValidity newValidity = new DSpaceValidity();

                DSpaceObject dso = getScope();

                if (dso != null) {
                    // Add the actual collection;
                    newValidity.add(context, dso);
                }

                // add recently submitted items, serialize solr query contents.
                DiscoverResult response = getQueryResponse(dso);

                newValidity.add("numFound:" + response.getDspaceObjects().size());

                for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
                    newValidity.add(context, resultDso);
                }

                for (String facetField : queryResults.getFacetResults().keySet()) {
                    newValidity.add(facetField);

                    java.util.List<DiscoverResult.FacetResult> facetValues = queryResults.getFacetResults().get(facetField);
                    for (DiscoverResult.FacetResult facetValue : facetValues) {
                        newValidity.add(facetField + facetValue.getAsFilterQuery() + facetValue.getCount());
                    }
                }


                this.validity = newValidity.complete();
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
     * @return recently submitted items.
     */
    protected DiscoverResult getQueryResponse(DSpaceObject scope) {


        Request request = ObjectModelHelper.getRequest(objectModel);

        if (queryResults != null)
        {
            return queryResults;
        }

        queryArgs = new DiscoverQuery();

        //Make sure we add our default filters
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
        queryArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));


        queryArgs.setQuery(((request.getParameter("query") != null && !"".equals(request.getParameter("query").trim())) ? request.getParameter("query") : null));
//        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);
        queryArgs.setDSpaceObjectFilter(Constants.ITEM);

        queryArgs.setMaxResults(0);

        queryArgs.addFilterQueries(DiscoveryUIUtils.getFilterQueries(request, context));


        //Set the default limit to 11
        //query.setFacetLimit(11);
        queryArgs.setFacetMinCount(1);

        int offset = RequestUtils.getIntParameter(request, SearchFilterParam.OFFSET);
        if (offset == -1)
        {
            offset = 0;
        }
        queryArgs.setFacetOffset(offset);

            //We add +1 so we can use the extra one to make sure that we need to show the next page
//        queryArgs.setFacetLimit();

        String facetField = request.getParameter(SearchFilterParam.FACET_FIELD);
        DiscoverFacetField discoverFacetField;
        // Enumerations don't handle mixed cases, setting to uppercase to match convention
        SORT requestSortOrder = getSortOrder(request);
        if(request.getParameter(SearchFilterParam.STARTS_WITH) != null)
        {
            discoverFacetField = new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_TEXT, getPageSize() + 1, requestSortOrder, request.getParameter(SearchFilterParam.STARTS_WITH).toLowerCase());
        }else{
            discoverFacetField = new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_TEXT, getPageSize() + 1, requestSortOrder);
        }

        queryArgs.addFacetField(discoverFacetField);

        try {
            queryResults = searchService.search(context, scope, queryArgs);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        return queryResults;
    }

    private SORT getSortOrder(Request request) {
        String sortOrderString = request.getParameter("filterorder");
        // First check for an already configured sortOrder (provided a new one is not being set)
        if(sortOrder!=null && StringUtils.isBlank(sortOrderString)){
            return sortOrder;
        }
        // Default to sort on value if none found
        if(StringUtils.isBlank(sortOrderString) || SORT.valueOf(sortOrderString.toUpperCase())==null){
            sortOrder= SORT.VALUE;
        }else{
            sortOrder= SORT.valueOf(request.getParameter("filterorder").toUpperCase());
        }
        return sortOrder;
    }

        /**
     * Add a page title and trail links.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = request.getParameter(SearchFilterParam.FACET_FIELD);

        pageMeta.addMetadata("title").addContent(message("xmlui.Discovery.AbstractSearch.type_" + facetField));

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath, true);
        }

        pageMeta.addTrail().addContent(message("xmlui.Discovery.AbstractSearch.type_" + facetField));
    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        updateQueryResultsAndOffset(request, dso);
        SearchFilterParam browseParams = new SearchFilterParam(request);
        // Build the DRI Body
        Division div = body.addDivision("browse-by-" + request.getParameter(SearchFilterParam.FACET_FIELD), "primary");
        div.setHead(message("xmlui.Discovery.AbstractSearch.type_" + browseParams.getFacetField()));
        if(getSortOrder(request).equals(SORT.VALUE)){
        addBrowseJumpNavigation(div, browseParams, request);
        }
        addBrowseControls(div, browseParams);

        // Set up the major variables
        //Collection collection = (Collection) dso;

        // Build the collection viewer division.

        //Make sure we get our results
        if (this.queryResults != null) {

            Map<String, List<DiscoverResult.FacetResult>> facetFields = this.queryResults.getFacetResults();
            if (facetFields == null)
            {
                facetFields = new LinkedHashMap<>();
            }

//            facetFields.addAll(this.queryResults.getFacetDates());


            if (facetFields.size() > 0) {
                String facetField = facetFields.keySet().toArray(new String[facetFields.size()])[0];
                java.util.List<DiscoverResult.FacetResult> values = facetFields.get(facetField);

                Division results = body.addDivision("browse-by-" + facetField + "-results", "primary");

                if (values != null && 0 < values.size()) {



                    // Find our faceting offset
                    int offSet = queryArgs.getFacetOffset();
                    if(offSet == -1){
                        offSet = 0;
                    }
                    currentOffset=offSet;

                    //Only show the nextpageurl if we have at least one result following our current results
                    String nextPageUrl = null;
                    if (values.size() == (getPageSize() + 1))
                    {
                        nextPageUrl = getNextPageURL(browseParams, request);
                    }



                    int shownItemsMax = offSet + (getPageSize() < values.size() ? values.size() - 1 : values.size());



                    // We put our total results to -1 so this doesn't get shown in the results (will be hidden by the xsl)
                    // The reason why we do this is because solr 1.4 can't retrieve the total number of facets found
                    results.setSimplePagination((int) queryResults.getTotalSearchResults(), offSet + 1,
                                                    shownItemsMax, getPreviousPageURL(browseParams, request), nextPageUrl);

                    Table singleTable = results.addTable("browse-by-" + facetField + "-results", (int) (queryResults.getDspaceObjects().size() + 1), 1);

                    List<String> filterQueries = Arrays.asList(DiscoveryUIUtils.getFilterQueries(request, context));


                    int end = values.size();
                    if(getPageSize() < end)
                    {
                        end = getPageSize();
                    }
                    for (int i = 0; i < end; i++) {
                        DiscoverResult.FacetResult value = values.get(i);
                        renderFacetField(browseParams, dso, facetField, singleTable, filterQueries, value);
                    }
                }else{
                    results.addPara(message("xmlui.discovery.SearchFacetFilter.no-results"));
                }
            }
        }
    }

    private void updateQueryResultsAndOffset(Request request, DSpaceObject dso) {
        int configuredOffset=-1;
        boolean retainOffset = false;
        if (request.getParameters().containsKey("update")) {
            configuredOffset = currentOffset;
            retainOffset=true;
        }
        queryResults = getQueryResponse(dso);
        if (retainOffset) {
            queryArgs.setFacetOffset(configuredOffset);
        }
    }

    private void addBrowseJumpNavigation(Division div, SearchFilterParam browseParams, Request request)
            throws WingException, SQLException, UnsupportedEncodingException {
        String action;
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if(dso != null){
            action = contextPath + "/handle/" + dso.getHandle() + "/search-filter";
        }else{
            action = contextPath + "/search-filter";
        }

        Division jump = div.addInteractiveDivision("filter-navigation", action,
                Division.METHOD_POST, "secondary navigation");

        Map<String, String> params = new HashMap<>();
        params.putAll(browseParams.getCommonBrowseParams());
        // Add all the query parameters as hidden fields on the form
        for(Map.Entry<String, String> param : params.entrySet()){
            jump.addHidden(param.getKey()).setValue(param.getValue());
        }
        Map<String, String[]> filterQueries = DiscoveryUIUtils.getParameterFilterQueries(request);
        for (String parameter : filterQueries.keySet())
        {
            for (int i = 0; i < filterQueries.get(parameter).length; i++)
            {
                String value = filterQueries.get(parameter)[i];
                jump.addHidden(parameter).setValue(value);
            }
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

    private void renderFacetField(SearchFilterParam browseParams, DSpaceObject dso, String facetField, Table singleTable, List<String> filterQueries, DiscoverResult.FacetResult value) throws SQLException, WingException, UnsupportedEncodingException {
        String displayedValue = value.getDisplayedValue();
//        if(field.getGap() != null){
//            //We have a date get the year so we can display it
//            DateFormat simpleDateformat = new SimpleDateFormat("yyyy");
//            displayedValue = simpleDateformat.format(SolrServiceImpl.toDate(displayedValue));
//            filterQuery = ClientUtils.escapeQueryChars(value.getFacetField().getName()) + ":" + displayedValue + "*";
//        }

        Cell cell = singleTable.addRow().addCell();

        //No use in selecting the same filter twice
        if(filterQueries.contains(searchService.toFilterQuery(context,  facetField, value.getFilterType(), value.getAsFilterQuery()).getFilterQuery())){
            cell.addContent(displayedValue + " (" + value.getCount() + ")");
        } else {
            //Add the basics
            Map<String, String> urlParams = new HashMap<>();
            urlParams.putAll(browseParams.getCommonBrowseParams());
            String xrefURL = generateURL(contextPath + (dso == null ? "" : "/handle/" + dso.getHandle()) + "/discover", urlParams);
            //Add already existing filter queries
            xrefURL = addFilterQueriesToUrl(xrefURL);
            //Last add the current filter query
            xrefURL += "&filtertype=" + facetField;
            xrefURL += "&filter_relational_operator="+value.getFilterType();
            xrefURL += "&filter=" + URLEncoder.encode(value.getAsFilterQuery(), "UTF-8");
            cell.addXref(xrefURL, displayedValue + " (" + value.getCount() + ")"
            );
        }
    }

    private String getNextPageURL(SearchFilterParam browseParams, Request request) throws UnsupportedEncodingException, UIException {
        int offSet = Util.getIntParameter(request, SearchFilterParam.OFFSET);
        if (offSet == -1)
        {
            offSet = currentOffset;
        }

        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.putAll(browseParams.getCommonBrowseParams());
        urlParameters.putAll(browseParams.getControlParameters());
        urlParameters.put(SearchFilterParam.OFFSET, String.valueOf(offSet + getPageSize()));
        urlParameters.put(SearchFilterParam.ORDER, getSortOrder(request).name());

        // Add the filter queries
        String newURL = generateURL("search-filter", urlParameters);
        newURL = addFilterQueriesToUrl(newURL);

        return newURL;
    }

    private String getPreviousPageURL(SearchFilterParam browseParams, Request request) throws UnsupportedEncodingException, UIException {
        //If our offset should be 0 then we shouldn't be able to view a previous page url
        boolean currentOffsetSmallerOrEqualTO0 = currentOffset <= 0;
        if (0 == queryArgs.getFacetOffset() && Util.getIntParameter(request, "offset") == -1 && currentOffsetSmallerOrEqualTO0)
        {
            return null;
        }

        int offset = Util.getIntParameter(request, SearchFilterParam.OFFSET);
        if(currentOffsetSmallerOrEqualTO0 && (offset == -1 || offset == 0))
        {
            return null;
        }

        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.putAll(browseParams.getCommonBrowseParams());
        urlParameters.putAll(browseParams.getControlParameters());
        urlParameters.put(SearchFilterParam.ORDER, getSortOrder(request).name());
        String offSet = String.valueOf((currentOffset - getPageSize()<0)? 0:currentOffset - getPageSize());
        urlParameters.put(SearchFilterParam.OFFSET, offSet);

        // Add the filter queries
        String newURL = generateURL("search-filter", urlParameters);
        newURL = addFilterQueriesToUrl(newURL);
        return newURL;
    }

    @Override
    public void recycle() {
        // Clear out our item's cache.
        this.queryResults = null;
        this.queryArgs = null;
        this.validity = null;
        super.recycle();
    }

    public String addFilterQueriesToUrl(String url) throws UIException {
        Map<String, String[]> fqs = DiscoveryUIUtils.getParameterFilterQueries(ObjectModelHelper.getRequest(objectModel));
        if (fqs != null)
        {
            StringBuilder urlBuilder = new StringBuilder(url);
            for (String parameter : fqs.keySet())
            {
                for (int i = 0; i < fqs.get(parameter).length; i++)
                {
                    String value = fqs.get(parameter)[i];
                    urlBuilder.append("&").append(parameter).append("=").append(encodeForURL(value));
                }
            }
            return urlBuilder.toString();
        }

        return url;
    }

    private static class SearchFilterParam {
        private final Request request;

        /** The always present commond params **/
        public static final String QUERY = "query";
        public static final String FACET_FIELD = "field";

        /** The browse control params **/
        public static final String OFFSET = "offset";
        public static final String STARTS_WITH = "starts_with";
        public static final String ORDER = "order";


        private SearchFilterParam(Request request){
            this.request = request;
        }

        public String getFacetField(){
            return request.getParameter(FACET_FIELD);
        }

        public Map<String, String> getCommonBrowseParams(){
            Map<String, String> result = new HashMap<>();
            result.put(FACET_FIELD, request.getParameter(FACET_FIELD));
            if(request.getParameter(QUERY) != null)
                result.put(QUERY, request.getParameter(QUERY));
            if(request.getParameter("scope") != null){
                result.put("scope", request.getParameter("scope"));
            }
            return result;
        }

        public Map<String, String> getControlParameters(){
            Map<String, String> paramMap = new HashMap<>();

            paramMap.put(OFFSET, request.getParameter(OFFSET));
            if(request.getParameter(STARTS_WITH) != null)
            {
                paramMap.put(STARTS_WITH, request.getParameter(STARTS_WITH));
            }

            return paramMap;
        }
    }

    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @return The current scope.
     */
    private DSpaceObject getScope() throws SQLException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeString = request.getParameter("scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
        {
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        }
        else
        {
            // Get the search scope from the location parameter
            dso = handleService.resolveToObject(context, scopeString);
        }

        return dso;
    }
    protected int getPageSize() {
        try {
            int rpp =Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("rpp"));
            DEFAULT_PAGE_SIZE=rpp;
            return rpp;
        }
        catch (Exception e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    /**
     * Add the controls to changing sorting and display options.
     *
     * @param div
     * @param params
     * @throws WingException
     */
    private void addBrowseControls(Division div, SearchFilterParam params)
            throws WingException
    {
        // Prepare a Map of query parameters required for all links
        Map<String, String> queryParams = new HashMap<>();

        queryParams.putAll(params.getCommonBrowseParams());
        Request request = ObjectModelHelper.getRequest(objectModel);
        queryParams.put("order",request.getParameter("order"));
        String facetField = request.getParameter(SearchFilterParam.FACET_FIELD);
        Division controls = div.addInteractiveDivision("browse-controls", "search-filter?field="+facetField,
                Division.METHOD_POST, "browse controls");

        // Add all the query parameters as hidden fields on the form
        for (Map.Entry<String, String> param : queryParams.entrySet())
        {
            controls.addHidden(param.getKey()).setValue(param.getValue());
        }

        Para controlsForm = controls.addPara();
        // Create a control for the number of records to display
        controlsForm.addContent(T_rpp);

        Select rppSelect = controlsForm.addSelect("rpp");

        for (int i : RESULTS_PER_PAGE_PROGRESSION)
        {
            rppSelect.addOption((i == getPageSize()), i, Integer.toString(i));
        }
        controlsForm.addButton("update").setValue("update");
    }

}
