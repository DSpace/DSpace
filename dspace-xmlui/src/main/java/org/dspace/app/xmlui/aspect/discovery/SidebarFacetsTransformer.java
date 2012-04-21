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
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.SidebarFacetConfiguration;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the side bar filters in discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SidebarFacetsTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent {


    private static final Logger log = Logger.getLogger(SidebarFacetsTransformer.class);

    /**
     * Cached query results
     */
    protected DiscoverResult queryResults;

    /**
     * Cached query arguments
     */
    protected DiscoverQuery queryArgs;

    /**
     * Cached validity object
     */
    protected SourceValidity validity;
    private static final Message T_FILTER_HEAD = message("xmlui.discovery.AbstractFiltersTransformer.filters.head");
    private static final Message T_VIEW_MORE = message("xmlui.discovery.AbstractFiltersTransformer.filters.view-more");

    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();
        
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
            {
                return HashUtil.hash(dso.getHandle());
            }else{
                return "0";
            }
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
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
                DSpaceValidity val = new DSpaceValidity();

                // Retrieve any facet results to add to the validity key
                performSearch();

                // Add the actual collection;
                if (dso != null)
                {
                    val.add(dso);
                }

                val.add("numFound:" + queryResults.getDspaceObjects().size());

                for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
                    val.add(resultDso);
                }

                for (String facetField : queryResults.getFacetResults().keySet()) {
                    val.add(facetField);

                    java.util.List<DiscoverResult.FacetResult> facetValues = queryResults.getFacetResults().get(facetField);
                    for (DiscoverResult.FacetResult facetValue : facetValues) {
                        val.add(facetValue.getAsFilterQuery() + facetValue.getCount());
                    }
                }

                this.validity = val.complete();
            }
            catch (Exception e) {
                log.error(e.getMessage(),e);
            }
            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }


    public void performSearch() throws SearchServiceException, UIException, SQLException {
        DSpaceObject dso = getScope();
        queryArgs = getQueryArgs(context, dso, getAllFilterQueries());
        //If we are on a search page performing a search a query may be used
        Request request = ObjectModelHelper.getRequest(objectModel);
        String query = request.getParameter("query");
        if(query != null && !"".equals(query)){
            queryArgs.setQuery(query);
        }

        //We do not need to retrieve any dspace objects, only facets
        queryArgs.setMaxResults(0);
        queryResults =  getSearchService().search(context, dso,  queryArgs);
    }

    @Override
    public void addOptions(Options options) throws SAXException, WingException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        try {
            performSearch();
        }catch (Exception e){
            log.error("Error while searching for sidebar facets", e);

            return;
        }

        if (this.queryResults != null) {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            java.util.List<String> fqs = new ArrayList<String>();
            if(request.getParameterValues("fq") != null){
                for (int i = 0; i < request.getParameterValues("fq").length; i++) {
                    String fq = request.getParameterValues("fq")[i];
                    fqs.add(getSearchService().toFilterQuery(context, fq).getFilterQuery());
                }
            }

            DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);
            java.util.List<SidebarFacetConfiguration> facets = discoveryConfiguration.getSidebarFacets();

            if (facets != null && 0 < facets.size()) {

                List browse = null;

                for (SidebarFacetConfiguration field : facets) {
                    //Retrieve our values
                    java.util.List<DiscoverResult.FacetResult> facetValues = queryResults.getFacetResult(field.getIndexFieldName());
                    //Check if we are dealing with a date, sometimes the facet values arrive as dates !
                    if(facetValues.size() == 0 && field.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                        facetValues = queryResults.getFacetResult(field.getIndexFieldName() + ".year");
                    }

                    int shownFacets = field.getFacetLimit()+1;

                    //This is needed to make sure that the date filters do not remain empty
                    if (facetValues != null && 0 < facetValues.size()) {

                        if(browse == null){
                            //Since we have a value it is save to add the sidebar (doing it this way will ensure that we do not end up with an empty sidebar)
                            browse = options.addList("discovery");

                            browse.setHead(T_FILTER_HEAD);
                        }

                        Iterator<DiscoverResult.FacetResult> iter = facetValues.iterator();

                        List filterValsList = browse.addList(field.getIndexFieldName());

                        filterValsList.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field.getIndexFieldName()));

                        for (int i = 0; i < shownFacets; i++) {

                            if (!iter.hasNext())
                            {
                                break;
                            }

                            DiscoverResult.FacetResult value = iter.next();

                            if (i < shownFacets - 1) {
                                String displayedValue = value.getDisplayedValue();
                                String filterQuery = value.getAsFilterQuery();

                                if (fqs.contains(filterQuery)) {
                                    filterValsList.addItem(Math.random() + "", "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                                } else {
                                    String paramsQuery = retrieveParameters(request);

                                    filterValsList.addItem().addXref(
                                            contextPath +
                                                    (dso == null ? "" : "/handle/" + dso.getHandle()) +
                                                    "/discover?" +
                                                    paramsQuery +
                                                    "fq=" +
                                                    URLEncoder.encode(filterQuery, "UTF-8"),
                                            displayedValue + " (" + value.getCount() + ")"
                                    );
                                }
                            }
                            //Show a view more url should there be more values, unless we have a date
                            if (i == shownFacets - 1 && !field.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)/*&& facetField.getGap() == null*/) {

                                addViewMoreUrl(filterValsList, dso, request, field.getIndexFieldName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the parameters used so it can be used in a url
     * @param request the cocoon request
     * @return the parameters used on this page
     */
    private String retrieveParameters(Request request) {
        StringBuffer result = new StringBuffer();
        Enumeration keys = request.getParameterNames();
        if(keys != null){
            while (keys.hasMoreElements()){
                String key = (String) keys.nextElement();
                //Ignore the page and submit button keys
                if(key != null && !"page".equals(key) && !key.startsWith("submit")){
                    String[] vals = request.getParameterValues(key);
                    for(String paramValue : vals){
                        result.append(key).append("=").append(paramValue);
                        result.append("&");
                    }
                }
            }
        }
        return result.toString();
    }

    private void addViewMoreUrl(List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        String parameters = retrieveParameters(request);
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/search-filter?" + parameters + BrowseFacet.FACET_FIELD + "=" + fieldName,
                T_VIEW_MORE

        );
    }

    public DiscoverQuery getQueryArgs(Context context, DSpaceObject scope, String... filterQueries) {
        DiscoverQuery queryArgs = new DiscoverQuery();

        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        java.util.List<SidebarFacetConfiguration> facets = discoveryConfiguration.getSidebarFacets();

        log.info("facets for scope, " + scope + ": " + (facets != null ? facets.size() : null));




        if (facets != null){
            queryArgs.setFacetMinCount(1);
        }

        //Add the default filters
        queryArgs.addFilterQueries(discoveryConfiguration.getDefaultFilterQueries().toArray(new String[discoveryConfiguration.getDefaultFilterQueries().size()]));
        queryArgs.addFilterQueries(filterQueries);

        /** enable faceting of search results */
        if (facets != null){
            for (SidebarFacetConfiguration facet : facets) {
                if(facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                    String dateFacet = facet.getIndexFieldName() + ".year";
                    try{
                        //Get a range query so we can create facet queries ranging from out first to our last date
                        //Attempt to determine our oldest & newest year by checking for previously selected filters
                        int oldestYear = -1;
                        int newestYear = -1;
                        for (String filterQuery : filterQueries) {
                            if(filterQuery.startsWith(dateFacet + ":")){
                                //Check for a range
                                Pattern pattern = Pattern.compile("\\[(.*? TO .*?)\\]");
                                Matcher matcher = pattern.matcher(filterQuery);
                                boolean hasPattern = matcher.find();
                                if(hasPattern){
                                    filterQuery = matcher.group(0);
                                    //We have a range
                                    //Resolve our range to a first & endyear
                                    int tempOldYear = Integer.parseInt(filterQuery.split(" TO ")[0].replace("[", "").trim());
                                    int tempNewYear = Integer.parseInt(filterQuery.split(" TO ")[1].replace("]", "").trim());

                                    //Check if we have a further filter (or a first one found)
                                    if(tempNewYear < newestYear || oldestYear < tempOldYear || newestYear == -1){
                                        oldestYear = tempOldYear;
                                        newestYear = tempNewYear;
                                    }

                                }else{
                                    if(filterQuery.indexOf(" OR ") != -1){
                                        //Should always be the case
                                        filterQuery = filterQuery.split(" OR ")[0];
                                    }
                                    //We should have a single date
                                    oldestYear = Integer.parseInt(filterQuery.split(":")[1].trim());
                                    newestYear = oldestYear;
                                    //No need to look further
                                    break;
                                }
                            }
                        }
                        //Check if we have found a range, if not then retrieve our first & last year by using solr
                        if(oldestYear == -1 && newestYear == -1){

                            DiscoverQuery yearRangeQuery = new DiscoverQuery();
                            yearRangeQuery.setMaxResults(1);
                            //Set our query to anything that has this value
                            yearRangeQuery.addFieldPresentQueries(dateFacet);
                            //Set sorting so our last value will appear on top
                            yearRangeQuery.setSortField(dateFacet + "_sort", DiscoverQuery.SORT_ORDER.asc);
                            yearRangeQuery.addFilterQueries(filterQueries);
                            yearRangeQuery.addSearchField(dateFacet);
                            DiscoverResult lastYearResult = getSearchService().search(context, scope, yearRangeQuery);


                            if(0 < lastYearResult.getDspaceObjects().size()){
                                java.util.List<DiscoverResult.SearchDocument> searchDocuments = lastYearResult.getSearchDocument(lastYearResult.getDspaceObjects().get(0));
                                if(0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues(dateFacet).size()){
                                    oldestYear = Integer.parseInt(searchDocuments.get(0).getSearchFieldValues(dateFacet).get(0));
                                }
                            }
                            //Now get the first year
                            yearRangeQuery.setSortField(dateFacet + "_sort", DiscoverQuery.SORT_ORDER.desc);
                            DiscoverResult firstYearResult = getSearchService().search(context, scope, yearRangeQuery);
                            if( 0 < firstYearResult.getDspaceObjects().size()){
                                java.util.List<DiscoverResult.SearchDocument> searchDocuments = firstYearResult.getSearchDocument(firstYearResult.getDspaceObjects().get(0));
                                if(0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues(dateFacet).size()){
                                    newestYear = Integer.parseInt(searchDocuments.get(0).getSearchFieldValues(dateFacet).get(0));
                                }
                            }
                            //No values found!
                            if(newestYear == -1 || oldestYear == -1)
                            {
                                continue;
                            }

                        }

                        int gap = 1;
                        //Attempt to retrieve our gap by the algorithm below
                        int yearDifference = newestYear - oldestYear;
                        if(yearDifference != 0){
                            while (10 < ((double)yearDifference / gap)){
                                gap *= 10;
                            }
                        }
                        // We need to determine our top year so we can start our count from a clean year
                        // Example: 2001 and a gap from 10 we need the following result: 2010 - 2000 ; 2000 - 1990 hence the top year
                        int topYear = (int) (Math.ceil((float) (newestYear)/gap)*gap);

                        if(gap == 1){
                            //We need a list of our years
                            //We have a date range add faceting for our field
                            //The faceting will automatically be limited to the 10 years in our span due to our filterquery
                            queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), 10, facet.getSortOrder()));
                        }else{
                            java.util.List<String> facetQueries = new ArrayList<String>();
                            //Create facet queries but limit then to 11 (11 == when we need to show a show more url)
                            for(int year = topYear; year > oldestYear && (facetQueries.size() < 11); year-=gap){
                                //Add a filter to remove the last year only if we aren't the last year
                                int bottomYear = year - gap;
                                //Make sure we don't go below our last year found
                                if(bottomYear < oldestYear)
                                {
                                    bottomYear = oldestYear;
                                }

                                //Also make sure we don't go above our newest year
                                int currentTop = year;
                                if((year == topYear))
                                {
                                    currentTop = newestYear;
                                }
                                else
                                {
                                    //We need to do -1 on this one to get a better result
                                    currentTop--;
                                }
                                facetQueries.add(dateFacet + ":[" + bottomYear + " TO " + currentTop + "]");
                            }
                            for (String facetQuery : facetQueries) {
                                queryArgs.addFacetQuery(facetQuery);
                            }
                        }
                    }catch (Exception e){
                        log.error(LogManager.getHeader(context, "Error in discovery while setting up date facet range", "date facet: " + dateFacet), e);
                    }
                }else{
                    int facetLimit = facet.getFacetLimit();
                    //Add one to our facet limit to make sure that if we have more then the shown facets that we show our show more url
                    facetLimit++;
                    queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), DiscoveryConfigurationParameters.TYPE_TEXT, facetLimit, facet.getSortOrder()));
                }
            }
        }
        return queryArgs;
    }

    /**
     * Returns all the filter queries for use by discovery
     *  This method returns more expanded filter queries then the getParameterFilterQueries
     * @return an array containing the filter queries
     */
    protected String[] getAllFilterQueries() {
        try {
            java.util.List<String> allFilterQueries = new ArrayList<String>();
            Request request = ObjectModelHelper.getRequest(objectModel);
            java.util.List<String> fqs = new ArrayList<String>();

            if(request.getParameterValues("fq") != null)
            {
                fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
            }

            String type = request.getParameter("filtertype");
            String value = request.getParameter("filter");

            if(value != null && !value.equals("")){
                allFilterQueries.add(getSearchService().toFilterQuery(context, (type.equals("*") ? "" : type), value).getFilterQuery());
            }

            //Add all the previous filters also
            for (String fq : fqs) {
                allFilterQueries.add(getSearchService().toFilterQuery(context, fq).getFilterQuery());
            }

            return allFilterQueries.toArray(new String[allFilterQueries.size()]);
        }
        catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            return null;
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


    @Override
    public void recycle() {
        queryResults = null;
        queryArgs = null;
        validity = null;
        super.recycle();
    }
}
