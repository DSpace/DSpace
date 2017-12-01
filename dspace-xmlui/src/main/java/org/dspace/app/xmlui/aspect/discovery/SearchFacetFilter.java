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
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
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


    private SearchService searchService = null;
    private static final Message T_go = message("xmlui.general.go");

    public SearchFacetFilter() {

        DSpace dspace = new DSpace();
        searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(),SearchService.class);

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
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceValidity validity = new DSpaceValidity();

                DSpaceObject dso = getScope();

                if (dso != null) {
                    // Add the actual collection;
                    validity.add(dso);
                }

                // add recently submitted items, serialize solr query contents.
                DiscoverResult response = getQueryResponse(dso);

                validity.add("numFound:" + response.getDspaceObjects().size());

                for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
                    validity.add(resultDso);
                }

                for (String facetField : queryResults.getFacetResults().keySet()) {
                    validity.add(facetField);

                    java.util.List<DiscoverResult.FacetResult> facetValues = queryResults.getFacetResults().get(facetField);
                    for (DiscoverResult.FacetResult facetValue : facetValues) {
                        validity.add(facetField + facetValue.getAsFilterQuery() + facetValue.getCount());
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
        if(request.getParameter(SearchFilterParam.STARTS_WITH) != null)
        {
            discoverFacetField = new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_TEXT, DEFAULT_PAGE_SIZE + 1, DiscoveryConfigurationParameters.SORT.VALUE, request.getParameter(SearchFilterParam.STARTS_WITH).toLowerCase());
        }else{
            discoverFacetField = new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_TEXT, DEFAULT_PAGE_SIZE + 1, DiscoveryConfigurationParameters.SORT.VALUE);
        }


        queryArgs.addFacetField(discoverFacetField);


        try {
            queryResults = searchService.search(context, scope, queryArgs);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        return queryResults;
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
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath, true);
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

            Map<String, List<DiscoverResult.FacetResult>> facetFields = this.queryResults.getFacetResults();
            if (facetFields == null)
            {
                facetFields = new LinkedHashMap<String, List<DiscoverResult.FacetResult>>();
            }

//            facetFields.addAll(this.queryResults.getFacetDates());


            if (facetFields.size() > 0) {
                String facetField = facetFields.keySet().toArray(new String[facetFields.size()])[0];
                java.util.List<DiscoverResult.FacetResult> values = facetFields.get(facetField);

                Division results = body.addDivision("browse-by-" + facetField + "-results", "primary");

                results.setHead(message("xmlui.Discovery.AbstractSearch.type_" + browseParams.getFacetField()));
                if (values != null && 0 < values.size()) {



                    // Find our faceting offset
                    int offSet = queryArgs.getFacetOffset();
                    if(offSet == -1){
                        offSet = 0;
                    }

                    //Only show the nextpageurl if we have at least one result following our current results
                    String nextPageUrl = null;
                    if (values.size() == (DEFAULT_PAGE_SIZE + 1))
                    {
                        nextPageUrl = getNextPageURL(browseParams, request);
                    }



                    int shownItemsMax = offSet + (DEFAULT_PAGE_SIZE < values.size() ? values.size() - 1 : values.size());



                    // We put our total results to -1 so this doesn't get shown in the results (will be hidden by the xsl)
                    // The reason why we do this is because solr 1.4 can't retrieve the total number of facets found
                    results.setSimplePagination(-1, offSet + 1,
                                                    shownItemsMax, getPreviousPageURL(browseParams, request), nextPageUrl);

                    Table singleTable = results.addTable("browse-by-" + facetField + "-results", (int) (queryResults.getDspaceObjects().size() + 1), 1);

                    List<String> filterQueries = Arrays.asList(DiscoveryUIUtils.getFilterQueries(request, context));


                    int end = values.size();
                    if(DEFAULT_PAGE_SIZE < end)
                    {
                        end = DEFAULT_PAGE_SIZE;
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

        Map<String, String> params = new HashMap<String, String>();
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
            Map<String, String> urlParams = new HashMap<String, String>();
            urlParams.putAll(browseParams.getCommonBrowseParams());
            String url = generateURL(contextPath + (dso == null ? "" : "/handle/" + dso.getHandle()) + "/discover", urlParams);
            //Add already existing filter queries
            url = addFilterQueriesToUrl(url);
            //Last add the current filter query
            url += "&filtertype=" + facetField;
            url += "&filter_relational_operator="+value.getFilterType();
            url += "&filter=" + URLEncoder.encode(displayedValue, "UTF-8");
            cell.addXref(url, displayedValue + " (" + value.getCount() + ")"
            );
        }
    }

    private String getNextPageURL(SearchFilterParam browseParams, Request request) throws UnsupportedEncodingException, UIException {
        int offSet = Util.getIntParameter(request, SearchFilterParam.OFFSET);
        if (offSet == -1)
        {
            offSet = 0;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.putAll(browseParams.getCommonBrowseParams());
        parameters.putAll(browseParams.getControlParameters());
        parameters.put(SearchFilterParam.OFFSET, String.valueOf(offSet + DEFAULT_PAGE_SIZE));

        // Add the filter queries
        String url = generateURL("search-filter", parameters);
        url = addFilterQueriesToUrl(url);

        return url;
    }

    private String getPreviousPageURL(SearchFilterParam browseParams, Request request) throws UnsupportedEncodingException, UIException {
        //If our offset should be 0 then we shouldn't be able to view a previous page url
        if (0 == queryArgs.getFacetOffset() && Util.getIntParameter(request, "offset") == -1)
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
        private Request request;

        /** The always present commond params **/
        public static final String QUERY = "query";
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
            if(request.getParameter(QUERY) != null)
                result.put(QUERY, request.getParameter(QUERY));
            if(request.getParameter("scope") != null){
                result.put("scope", request.getParameter("scope"));
            }
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
            dso = HandleManager.resolveToObject(context, scopeString);
        }

        return dso;
    }

}
