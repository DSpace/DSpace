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
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
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
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceImpl;
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
public abstract class AbstractFiltersTransformer extends AbstractDSpaceTransformer {


    private static final Logger log = Logger.getLogger(AbstractFiltersTransformer.class);

    
    public abstract String getView();
    
    /**
     * Cached query results
     */
    protected QueryResponse queryResults;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

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
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);


                DSpaceValidity val = new DSpaceValidity();

                // add reciently submitted items, serialize solr query contents.
                performSearch(dso);

                // Add the actual collection;
                if (dso != null)
                {
                    val.add(dso);
                }

                val.add("numFound:" + queryResults.getResults().getNumFound());

                for (SolrDocument doc : queryResults.getResults()) {
                    val.add(doc.toString());
                }

                for (SolrDocument doc : queryResults.getResults()) {
                    val.add(doc.toString());
                }

                for (FacetField field : queryResults.getFacetFields()) {
                    val.add(field.getName());

                    for (FacetField.Count count : field.getValues()) {
                        val.add(count.getName() + count.getCount());
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


    public abstract void performSearch(DSpaceObject object) throws SearchServiceException, UIException;

        /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @return The current scope.
     */
    protected DSpaceObject getScope() throws SQLException {
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

    protected SolrQuery prepareDefaultFilters(String scope, String ...filterQueries) {

        queryArgs = new SolrQuery();

        SearchUtils.SolrFacetConfig[] facets = SearchUtils.getFacetsForType(scope);

        log.info("facets for scope, " + scope + ": " + (facets != null ? facets.length : null));



        //Set the default limit to 10
        int max = 10;
        try{
            max = SearchUtils.getConfig().getInteger("search.facet.max");
        }catch (Exception e){
            //Ignore, only occurs if property isn't set in the config, default will be used then
        }
        //Add one to our facet limit to make sure that if we have more then the shown facets that we show our show more url
        max++;

        if (facets != null){
            queryArgs.setFacetLimit(max);
            queryArgs.setFacetMinCount(1);
            queryArgs.setFacet(true);
        }

        /** enable faceting of search results */
        if (facets != null){
            for (SearchUtils.SolrFacetConfig facet : facets) {


                if(facet.isDate()){

                    String dateFacet = facet.getFacetField();
                    try{
                        //Get a range query so we can create facet queries ranging from out first to our last date
                        //Attempt to determine our oldest & newest year by checking for previously selected filters
                        int oldestYear = -1;
                        int newestYear = -1;
                        for (String filterQuery : filterQueries) {
                            if(filterQuery.startsWith(facet.getFacetField() + ":")){
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

                            SolrQuery yearRangeQuery = new SolrQuery();
                            yearRangeQuery.setRows(1);
                            //Set our query to anything that has this value
                            yearRangeQuery.setQuery(facet.getFacetField() + ":[* TO *]");
                            //Set sorting so our last value will appear on top
                            yearRangeQuery.setSortField(dateFacet, SolrQuery.ORDER.asc);
                            yearRangeQuery.addFilterQuery(filterQueries);
                            QueryResponse lastYearResult = getSearchService().search(yearRangeQuery);
                            if(lastYearResult.getResults() != null && 0 < lastYearResult.getResults().size() && lastYearResult.getResults().get(0).getFieldValue(dateFacet) != null){
                                oldestYear = (Integer) lastYearResult.getResults().get(0).get(dateFacet);
                            }
                            //Now get the first year
                            yearRangeQuery.setSortField(dateFacet, SolrQuery.ORDER.desc);
                            QueryResponse firstYearResult = getSearchService().search(yearRangeQuery);
                            if(firstYearResult.getResults() != null && 0 < firstYearResult.getResults().size() && firstYearResult.getResults().get(0).getFieldValue(dateFacet) != null){
                                newestYear = (Integer) firstYearResult.getResults().get(0).get(dateFacet);
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
                            queryArgs.addFacetField(facet.getFacetField());
                        }else{
                            java.util.List<String> facetQueries = new ArrayList<String>();
                            //Create facet queries but limit then to 11 (11 == when we need to show a show more url)
                            for(int year = topYear; year > oldestYear && (facetQueries.size() < max); year-=gap){
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

                    queryArgs.addFacetField(facet.getFacetField());

                }
            }
        }
        //Add the default filters
        queryArgs.addFilterQuery(SearchUtils.getDefaultFilters(scope));

        return queryArgs;
    }


    @Override
    public void addOptions(Options options) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        java.util.List<String> fqs = Arrays.asList(request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0]);

        if (this.queryResults != null) {

            SearchUtils.SolrFacetConfig[] facets = SearchUtils.getFacetsForType(getView());

            if (facets != null && 0 < facets.length) {

                List browse = options.addList("discovery");

                browse.setHead(T_FILTER_HEAD);

                for (SearchUtils.SolrFacetConfig field : facets) {
                    //Retrieve our values
                    java.util.List<FilterDisplayValue> values = new ArrayList<FilterDisplayValue>();

                    int shownFacets = this.queryArgs.getFacetLimit();
                    FacetField facet = queryResults.getFacetField(field.getFacetField());
                    if(facet != null){
                        java.util.List<FacetField.Count> facetVals = facet.getValues();
                        if(facetVals == null)
                        {
                            continue;
                        }
                        for (FacetField.Count count : facetVals) {
                            values.add(new FilterDisplayValue(count.getName(), count.getCount(), count.getAsFilterQuery()));
                        }
                    }
                    if(field.isDate()){
                        if(0 < values.size()){
                            //Check for values comming from a facet field.
                            //If we have values we need to sort these & get the newest ones on top
                            //There is no other way for this since solr doesn't support facet sorting
                            TreeMap<String, FilterDisplayValue> sortedVals = new TreeMap<String, FilterDisplayValue>(Collections.reverseOrder());
                            for (FilterDisplayValue filterDisplayValue : values) {
                                //No need to show empty years
                                if(0 < filterDisplayValue.getCount())
                                {
                                    sortedVals.put(filterDisplayValue.getDisplayedVal(), filterDisplayValue);
                                }
                            }
                            //Make sure we retrieve our sorted values
                            values = Arrays.asList(sortedVals.values().toArray(new FilterDisplayValue[sortedVals.size()]));
                        }else{
                            //Attempt to retrieve it as a facet query
                            //Since our facet query result is returned as a hashmap we need to sort it by using a treemap
                            TreeMap<String, Integer> sortedFacetQueries = new TreeMap<String, Integer>(queryResults.getFacetQuery());
                            for(String facetQuery : sortedFacetQueries.descendingKeySet()){
                                if(facetQuery != null && facetQuery.startsWith(field.getFacetField())){
                                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                                    String name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                                    name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                                    Integer count = sortedFacetQueries.get(facetQuery);

                                    //No need to show empty years
                                    if(0 < count)
                                    {
                                        values.add(new FilterDisplayValue(name, count, facetQuery));
                                    }
                                }
                            }
                        }
                    }


                    //This is needed to make sure that the date filters do not remain empty
                    if (0 < values.size()) {

                        Iterator<FilterDisplayValue> iter = values.iterator();

                        List filterValsList = browse.addList(field.getFacetField());

                        filterValsList.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field.getFacetField().replace("_lc", "")));

                        for (int i = 0; i < shownFacets; i++) {

                            if (!iter.hasNext())
                            {
                                break;
                            }

                            FilterDisplayValue value = iter.next();

                            if (i < shownFacets - 1) {
                                String displayedValue = value.getDisplayedVal();
                                String filterQuery = value.getAsFilterQuery();
                                if (field.getFacetField().equals("location.comm") || field.getFacetField().equals("location.coll")) {
                                    //We have a community/collection, resolve it to a dspaceObject
                                    displayedValue = SolrServiceImpl.locationToName(context, field.getFacetField(), displayedValue);

                                }


                                if (fqs.contains(filterQuery)) {
                                    filterValsList.addItem(Math.random() + "", "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                                } else {
                                    String paramsQuery = "";
                                    Enumeration keys = request.getParameterNames();
                                    if(keys != null){
                                        while (keys.hasMoreElements()){
                                            String key = (String) keys.nextElement();
                                            if(key != null){
                                                paramsQuery += key + "=" + URLEncoder.encode(request.getParameter(key), "UTF-8");
                                                paramsQuery += "&";
                                            }
                                        }
                                    }

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
                            if (i == shownFacets - 1 && !field.isDate()/*&& facetField.getGap() == null*/) {

                                addViewMoreUrl(filterValsList, dso, request, field.getFacetField());
                            }
                        }
                    }
                }
            }
        }
    }

    private void addViewMoreUrl(List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        facet.addItem().addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/search-filter?" + BrowseFacet.FACET_FIELD + "=" + fieldName +
                        (request.getQueryString() != null ? "&" + request.getQueryString() : ""),
                T_VIEW_MORE

        );
    }

    @Override
    public void recycle() {
        queryResults = null;
        queryArgs = null;
    }

    private static final class FilterDisplayValue {
        private String asFilterQuery;
        private String displayedVal;
        private long count;

        private FilterDisplayValue(String displayedVal, long count, String asFilterQuery) {
            this.asFilterQuery = asFilterQuery;
            this.displayedVal = displayedVal;
            this.count = count;
        }

        public String getDisplayedVal() {
            return displayedVal;
        }

        public long getCount() {
            return count;
        }

        public String getAsFilterQuery(){
            return asFilterQuery;
        }
    }
}
