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
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
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
 * Class has been adjusted to support the non lowercased facets
 */
public class SearchFilterTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

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
    private String field =null;
    //indicate if the query facet is the facet field we are rendering now
    //default to be false so the new facet search always starts with page 0
    //private boolean isQueryFacet=false;
    private ConfigurationService config = null;

    private SearchService searchService = null;
    private static final Message T_go = message("xmlui.general.go");

    public SearchFilterTransformer() {

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
    protected QueryResponse getQueryResponse(DSpaceObject scope) throws SQLException {


        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        field = parameters.getParameter("field", "");

        if (queryResults != null)
        {
            return queryResults;
        }

        queryArgs = new SolrQuery();

        //Make sure we add our default filters
        queryArgs.addFilterQuery(SearchUtils.getDefaultFilters(getView()));

        //get the current rendering facet

        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
        queryArgs.setQuery("search.resourcetype: " + Constants.ITEM + ((request.getParameter("query") != null && !"".equals(request.getParameter("query"))) ? " AND (" + request.getParameter("query") + ")" : ""));
//        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);
        }
        else
        {
            queryArgs.setQuery("search.resourcetype: " + Constants.ITEM);
        }
        queryArgs.setRows(0);

        queryArgs.addFilterQuery(getSolrFilterQueries());

        //Set the default limit to 11
        //query.setFacetLimit(11);
        queryArgs.setFacetMinCount(1);

        //sort
        //TODO: why this kind of sorting ? Should the sort not be on how many times the value appears like we do in the filter by sidebar ?
        queryArgs.setFacetSort(config.getPropertyAsType("solr.browse.sort","lex"));

        queryArgs.setFacet(true);
        String facetField = field;
        int offset=0;
        //only set the offeset when the search query equals current rendering facet
        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
        offset= RequestUtils.getIntParameter(request, SearchFilterParam.OFFSET);
            if (offset == -1)
            {
                offset = 0;
            }
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



        queryArgs.setFilterQueries("location:l2");
        boolean isDate = false;
        if(facetField.endsWith("_dt")){
            facetField = facetField.split("_")[0];
            isDate = true;
        }

        if (isDate) {

            queryArgs.setParam(FacetParams.FACET_DATE, facetField);
            queryArgs.setParam(FacetParams.FACET_DATE_GAP,"+1YEAR");

            Date lowestDate = getLowestDateValue(context, queryArgs.getQuery(), facetField, queryArgs.getFilterQueries());
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
            //only set the start_with when the search query equals current rendering facet
            if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
                if(request.getParameter(SearchFilterParam.STARTS_WITH) != null)
                {
                    queryArgs.setFacetPrefix(facetField, request.getParameter(SearchFilterParam.STARTS_WITH).toLowerCase());
                }
            }
            queryArgs.addFacetField(facetField);
        }

        try {
            queryResults = searchService.search(context, queryArgs);
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
    private Date getLowestDateValue(Context context, String query, String dateField, String... filterquery){


        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields(dateField);
            solrQuery.setRows(1);
            solrQuery.setSortField(dateField, SolrQuery.ORDER.asc);
            solrQuery.setFilterQueries(filterquery);

            QueryResponse rsp = searchService.search(context, solrQuery);
            if(0 < rsp.getResults().getNumFound()){
                return (Date) rsp.getResults().get(0).getFieldValue(dateField);
            }
        }catch (Exception e){
            log.error("Error while retrieving lowest date value", e);
        }
        return null;
    }

    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = field;

        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
        pageMeta.addMetadata("title").addContent(message("xmlui.Discovery.AbstractSearch.type_" + facetField));


        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(message("xmlui.Discovery.AbstractSearch.type_" + facetField));
        }
    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        SearchFilterParam browseParams=null;
        //get the current rendering facet
        field = parameters.getParameter("field", "");

        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
           browseParams= new SearchFilterParam(request);
        }
        else
        {
            browseParams=new SearchFilterParam(null);
        }
        // Build the DRI Body
        Division div = body.addDivision("browse-by-" + field, "form_"+field);

        addBrowseJumpNavigation(div, browseParams, request);

        // Set up the major variables
        //Collection collection = (Collection) dso;

        // Build the collection viewer division.


        queryResults = getQueryResponse(dso);
        if (this.queryResults != null) {

            List<FacetField> facetFields = this.queryResults.getFacetFields();
            if (facetFields == null)
            {
                facetFields = new ArrayList<FacetField>();
            }

            facetFields.addAll(this.queryResults.getFacetDates());


            if (facetFields.size() > 0) {
                FacetField facetField = facetFields.get(0);
                List<FacetField.Count> values = facetField.getValues();
                if(facetField.getGap() != null){
                    //We are dealing with dates so flip em, top date comes first
                    Collections.reverse(values);

                }

                Division results = body.addDivision("browse-by-" + field + "-results", "primary_"+field);

                results.setHead(message("xmlui.Discovery.AbstractSearch.type_" + field));
                if (values != null && 0 < values.size()) {

                    int offSet=0;

                    // Find our faceting offset
                    try {
                        offSet= Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET));
                    } catch (NumberFormatException e) {
                        //Ignore
                    }

                    String nextPageUrl = null;
                    if(facetField.getName().endsWith(".year")){
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


                    if(facetField.getName().endsWith(".year")){
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
                    if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
                    if(request.getParameterValues("fq") != null)
                    {
                        filterQueries = Arrays.asList(request.getParameterValues("fq"));
                    }
                    }

                    if(facetField.getName().endsWith(".year")){
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

                            renderFacetField(browseParams, dso, facetField, singleTable, filterQueries, value);
                        }
                    }else{
                        int end = values.size();
                        if(DEFAULT_PAGE_SIZE < end)
                        {
                            end = DEFAULT_PAGE_SIZE;
                        }


                        for (int i = 0; i < end; i++) {
                            FacetField.Count value = values.get(i);

                            renderFacetField(browseParams, dso, facetField, singleTable, filterQueries, value);
                        }
                    }
                    //don't need view more link anymore
//                    String url = ConfigurationManager.getProperty("dspace.url")+"/search-filter?query=&field="+field+"&fq=location:l2";
//		            results.addList("link-to-button-"+field).addItemXref(url,"View More");
                }else{
                    results.addPara(message("xmlui.discovery.SearchFacetFilter.no-results"));
                }

            }
        }
    }

    private void addBrowseJumpNavigation(Division div, SearchFilterParam browseParams, Request request)
            throws WingException, SQLException {
        Division jump = div.addInteractiveDivision("filter-navigation-"+field, contextPath + "/" + getSearchFilterUrl(),
                Division.METHOD_POST, "secondary navigation");

        Map<String, String> params = new HashMap<String, String>();
        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
            params.putAll(browseParams.getCommonBrowseParams());
        }

        //We cannot create a filter for dates
        if(!field.endsWith(".year")){
            // Create a clickable list of the alphabet
            org.dspace.app.xmlui.wing.element.List jumpList = jump.addList("jump-list-"+field, org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "alphabet");
            String basicUrl=null;
            if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
            //Create our basic url
            basicUrl = generateURL(getSearchFilterUrl(), params);
            //Add any filter queries
            basicUrl = addFilterQueriesToUrl(basicUrl);

            }
            else
            {
                //add the current rendering facet filter in the url
                basicUrl="?"+SearchFilterParam.FACET_FIELD+"="+field;
            }
            //TODO: put this back !
//            jumpList.addItemXref(generateURL("browse", letterQuery), "0-9");
            for (char c = 'A'; c <= 'Z'; c++)
            {
                String linkUrl = basicUrl + "&" +  SearchFilterParam.STARTS_WITH +  "=" + Character.toString(c).toLowerCase();
                jumpList.addItemXref(linkUrl, Character
                        .toString(c));
            }



            Para hiddenFrom=jump.addPara("hidden_form_"+field,"hidden_form_by");

            hiddenFrom.addHidden("field", field+"_hidden_" + field).setValue(field);
            //only add field when the query match the rend field
            if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
                // Add all the query parameters as hidden fields on the form
                for(Map.Entry<String, String> param : params.entrySet()){
                    if(!param.getKey().equals("field"))
                    {
                        hiddenFrom.addHidden(param.getKey(),param.getKey()+"_hidden_"+field).setValue(param.getValue());
                    }
                }
            }


            // Create a free text field for the initial characters
            Para jumpForm = jump.addPara();

            String[] filterQueries = getParameterFilterQueries();
            for (String filterQuery : filterQueries) {
                jumpForm.addHidden("fq","fq_hidden_"+field).setValue(filterQuery);
            }
            jumpForm.addContent(T_starts_with);
            jumpForm.addText("starts_with","starts_with_"+field);

            jumpForm.addButton("submit","submit_"+field).setValue(T_go);

            if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD)))
            {
                jump.addPara("choose_browse_by_"+field,"choose_browse_by").addContent(field);
            }
        }
    }

    private void renderFacetField(SearchFilterParam browseParams, DSpaceObject dso, FacetField facetField, Table singleTable, List<String> filterQueries, FacetField.Count value) throws SQLException, WingException, UnsupportedEncodingException {
        String displayedValue = value.getName();
        String filterQuery = value.getAsFilterQuery();
        if (facetField.getName().equals("location.comm") || facetField.getName().equals("location.coll")) {
            //We have a community/collection, resolve it to a dspaceObject
            displayedValue = SolrServiceImpl.locationToName(context, facetField.getName(), displayedValue);
        }
        if(facetField.getGap() != null){
            //We have a date get the year so we can display it
            DateFormat simpleDateformat = new SimpleDateFormat("yyyy");
            displayedValue = simpleDateformat.format(SolrServiceImpl.toDate(displayedValue));
            filterQuery = ClientUtils.escapeQueryChars(value.getFacetField().getName()) + ":" + displayedValue + "*";
        }

        Cell cell = singleTable.addRow().addCell();

        //No use in selecting the same filter twice
        if(filterQueries.contains(filterQuery)){
            String splitChar = SearchUtils.getConfig().getString("solr.facets.split.char");
            if(splitChar != null && displayedValue.indexOf(splitChar) != -1)
                displayedValue = displayedValue.substring(displayedValue.indexOf(splitChar) + splitChar.length(), displayedValue.length());
            cell.addContent(displayedValue + " (" + value.getCount() + ")");
        } else {
            //Add the basics
            Map<String, String> urlParams = new HashMap<String, String>();
            urlParams.putAll(browseParams.getCommonBrowseParams());
            String url = generateURL(contextPath + (dso == null ? "" : "/handle/" + dso.getHandle()) + "/" + getDiscoverUrl(), urlParams);
            //Add already existing filter queries
            url = addFilterQueriesToUrl(url);
            //Last add the current filter query
            url += "&fq=" + URLEncoder.encode(filterQuery, "UTF-8");
            String splitChar = SearchUtils.getConfig().getString("solr.facets.split.char");
            if(splitChar != null && displayedValue.indexOf(splitChar) != -1)
                displayedValue = displayedValue.substring(displayedValue.indexOf(splitChar) + splitChar.length(), displayedValue.length());

            cell.addXref(url, displayedValue + " (" + value.getCount() + ")"
            );
        }
    }

    private String getNextPageURL(SearchFilterParam browseParams, Request request) throws SQLException {
        int offSet=0;
        Map<String, String> parameters = new HashMap<String, String>();
        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
            offSet= Util.getIntParameter(request, SearchFilterParam.OFFSET);
            if (offSet == -1)
            {
                offSet = 0;
            }
        parameters.putAll(browseParams.getCommonBrowseParams());
        parameters.putAll(browseParams.getControlParameters());
        }
        parameters.put(SearchFilterParam.OFFSET, String.valueOf(offSet + DEFAULT_PAGE_SIZE));
        parameters.put(SearchFilterParam.FACET_FIELD, field);

        //TODO: correct  comm/collection url
        // Add the filter queries
        String url = generateURL(getSearchFilterUrl(), parameters);
        url = addFilterQueriesToUrl(url);

        return url;
    }

    private String getPreviousPageURL(SearchFilterParam browseParams, Request request) throws SQLException {
        int offset=0;
        Map<String, String> parameters = new HashMap<String, String>();
        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
            //If our offset should be 0 then we shouldn't be able to view a previous page url
            if ("0".equals(queryArgs.get(FacetParams.FACET_OFFSET)) && Util.getIntParameter(request, "offset") == -1)
            {
                return null;
            }

            offset = Util.getIntParameter(request, SearchFilterParam.OFFSET);

            parameters.putAll(browseParams.getCommonBrowseParams());
            parameters.putAll(browseParams.getControlParameters());
        }
        if(offset == -1 || offset == 0)
        {
            return null;
        }
        parameters.put(SearchFilterParam.FACET_FIELD, field);
        parameters.put(SearchFilterParam.OFFSET, String.valueOf(offset - DEFAULT_PAGE_SIZE));

        //TODO: correct  comm/collection url
        // Add the filter queries
        String url = generateURL(getSearchFilterUrl(), parameters);
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

    public String addFilterQueriesToUrl(String url) throws SQLException {
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


    public String[] getParameterFilterQueries() throws SQLException{
        Request request = ObjectModelHelper.getRequest(objectModel);
        java.util.List<String> fqs = new ArrayList<String>();
        if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
        if(request.getParameterValues("fq") != null)
        {
            fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
        }

        //Have we added a filter using the UI
        if(request.getParameter("filter") != null && !"".equals(request.getParameter("filter")))
        {
            fqs.add((request.getParameter("filtertype").equals("*") ? "" : request.getParameter("filtertype") + ":") + request.getParameter("filter"));
        }
        }
        return fqs.toArray(new String[fqs.size()]);
    }

    /**
     * Returns all the filter queries for use by solr
     *  This method returns more expanded filter queries then the getParameterFilterQueries
     * @return an array containing the filter queries
     */
    protected String[] getSolrFilterQueries() {
        try {
            java.util.List<String> allFilterQueries = new ArrayList<String>();
            Request request = ObjectModelHelper.getRequest(objectModel);
            java.util.List<String> fqs = new ArrayList<String>();
            String type=null;
            String value=null;
            if(field.equals(request.getParameter(SearchFilterParam.FACET_FIELD))){
            if(request.getParameterValues("fq") != null)
            {
                fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
            }


            type= request.getParameter("filtertype");
            value = request.getParameter("filter");
            }
            if(value != null && !value.equals("")){
                String exactFq = (type.equals("*") ? "" : type + ":") + value;
                fqs.add(exactFq + " OR " + exactFq + "*");
            }


            for (String fq : fqs) {
                //Do not put a wildcard after a range query
                if (fq.matches(".*\\:\\[.* TO .*\\](?![a-z 0-9]).*")) {
                    allFilterQueries.add(fq);
                }
                else
                {
                    allFilterQueries.add(fq.endsWith("*") ? fq : fq + " OR " + fq + "*");
                }
            }

            return allFilterQueries.toArray(new String[allFilterQueries.size()]);
        }
        catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            return null;
        }
    }

    public String getView(){
        return "search";
    }

    public String getSearchFilterUrl(){
        return "";
    }

    public String getDiscoverUrl(){
        return "discover";
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
            if(this.request!=null){
            result.put(FACET_FIELD, request.getParameter(FACET_FIELD));
            if(request.getParameter(QUERY) != null)
                result.put(QUERY, request.getParameter(QUERY));
            }
            return result;
        }

        public Map<String, String> getControlParameters(){
            Map<String, String> paramMap = new HashMap<String, String>();
            if(this.request!=null){
            paramMap.put(OFFSET, request.getParameter(OFFSET));
            if(request.getParameter(STARTS_WITH) != null)
            {
                paramMap.put(STARTS_WITH, request.getParameter(STARTS_WITH));
            }
            }
            return paramMap;
        }

    }
}
