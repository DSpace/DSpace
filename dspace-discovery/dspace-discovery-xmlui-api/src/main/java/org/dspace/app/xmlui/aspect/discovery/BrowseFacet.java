/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.RequestUtils;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.excalibur.source.SourceValidity;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.apache.log4j.Logger;
import org.dspace.discovery.*;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.net.URLEncoder;
import java.util.List;

/**
 * Dynamic browse by page (not used @ the moment)
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class BrowseFacet extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(BrowseFacet.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

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

    public static final String OFFSET = "offset";
    public static final String FACET_FIELD = "field";

    private ConfigurationService config = null;

    private SearchService searchService = null;

    public BrowseFacet() {

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
        queryArgs.addFilterQuery(SearchUtils.getDefaultFilters("browse"));


        queryArgs.setQuery("search.resourcetype: " + Constants.ITEM + ((request.getParameter("query") != null && !"".equals(request.getParameter("query"))) ? " AND (" + request.getParameter("query") + ")" : ""));
//        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(0);


//        TODO: change this !
        queryArgs.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                SolrQuery.ORDER.asc
        );
        queryArgs.addFilterQuery(getParameterFacetQueries());


        //Set the default limit to 11
        //query.setFacetLimit(11);
        queryArgs.setFacetMinCount(1);

        //sort
        //TODO: why this kind of sorting ? Should the sort not be on how many times the value appears like we do in the filter by sidebar ?
        queryArgs.setFacetSort(config.getPropertyAsType("solr.browse.sort","lex"));

        queryArgs.setFacet(true);

        int offset = RequestUtils.getIntParameter(request, OFFSET);
        if (offset == -1)
        {
            offset = 0;
        }
        queryArgs.setParam(FacetParams.FACET_OFFSET, String.valueOf(offset));

        //We add +1 so we can use the extra one to make sure that we need to show the next page
        queryArgs.setParam(FacetParams.FACET_LIMIT, String.valueOf(DEFAULT_PAGE_SIZE + 1));


        if (scope != null) /* top level search / community */ {
            if (scope instanceof Community) {
                queryArgs.setFilterQueries("location:m" + scope.getID());
            } else if (scope instanceof Collection) {
                queryArgs.setFilterQueries("location:l" + scope.getID());
            }
        }


        String facetField = request.getParameter(FACET_FIELD);
        boolean isDate = false;
        if(facetField.endsWith("_dt")){
            facetField = facetField.split("_")[0];
            isDate = true;
        }

        if (isDate) {

            queryArgs.setParam(FacetParams.FACET_DATE,new String[]{facetField});
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
            queryArgs.addFacetField(new String[]{facetField});
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
            log.error("Unable to get lowest date", e);
        }
        return null;
    }

    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = request.getParameter(FACET_FIELD);

        pageMeta.addMetadata("title").addContent(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + facetField + "_browse"));


        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + facetField + "_browse"));
    }


    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

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

                if (values != null && 0 < values.size()) {


                    Division results = body.addDivision("browse-by-" + field + "-results", "primary");

                    results.setHead(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + request.getParameter(FACET_FIELD) + "_browse"));

                    // Find our faceting offset
                    int offSet = 0;
                    try {
                        offSet = Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET));
                    } catch (NumberFormatException e) {
                        //Ignore
                    }

                    //Only show the nextpageurl if we have at least one result following our current results
                    String nextPageUrl = null;
                    if (values.size() == (DEFAULT_PAGE_SIZE + 1))
                    {
                        nextPageUrl = getNextPageURL(request);
                    }

                    results.setSimplePagination((int) queryResults.getResults().getNumFound(), offSet + 1,
                            (offSet + (values.size() - 1)), getPreviousPageURL(request), nextPageUrl);


                    Table singleTable = results.addTable("browse-by-" + field + "-results", (int) (queryResults.getResults().getNumFound() + 1), 1);

                    List<String> filterQueries = new ArrayList<String>();
                    if(request.getParameterValues("fq") != null)
                    {
                        filterQueries = Arrays.asList(request.getParameterValues("fq"));
                    }
                    for (int i = 0; i < values.size(); i++) {
                        FacetField.Count value = values.get(i);

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
                            cell.addXref(
                                    contextPath + (dso == null ? "" : "/handle/" + dso.getHandle()) +
                                            "/discover?" +
                                            "&fq=" +
                                            URLEncoder.encode(filterQuery, "UTF-8") +
                                            (request.getQueryString() != null ? "&" + request.getQueryString() : ""),
                                    displayedValue + " (" + value.getCount() + ")"
                            );
                        }
                    }

                }
            }
        }

        //DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        /*
        if (dso != null)
        {
            if (dso instanceof Collection)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Al" + dso.getID(), T_head_this_collection );
            }
            if (dso instanceof Community)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Am" + dso.getID(), T_head_this_community );
            }
        }

        browseGlobal.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2", T_head_all_of_dspace );
        */
    }

    private String getNextPageURL(Request request) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(FACET_FIELD, request.getParameter(FACET_FIELD));
        if (queryArgs.get(FacetParams.FACET_OFFSET) != null)
        {
            parameters.put(OFFSET, String.valueOf(Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET)) + DEFAULT_PAGE_SIZE));
        }

        // Add the filter queries
        String url = generateURL("browse-discovery", parameters);
        String[] fqs = getParameterFacetQueries();
        if (fqs != null) {
            StringBuilder urlBuilder = new StringBuilder(url);
            for (String fq : fqs) {
                urlBuilder.append("&fq=").append(fq);
            }

            url = urlBuilder.toString();
        }

        return url;
    }

    private String getPreviousPageURL(Request request) {
        //If our offset should be 0 then we shouldn't be able to view a previous page url
        if ("0".equals(queryArgs.get(FacetParams.FACET_OFFSET)))
        {
            return null;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(FACET_FIELD, request.getParameter(FACET_FIELD));
        if (queryArgs.get(FacetParams.FACET_OFFSET) != null)
        {
            parameters.put(OFFSET, String.valueOf(Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET)) - DEFAULT_PAGE_SIZE));
        }

        // Add the filter queries
        String url = generateURL("browse-discovery", parameters);
        String[] fqs = getParameterFacetQueries();
        if (fqs != null) {
            StringBuilder urlBuilder = new StringBuilder(url);
            for (String fq : fqs) {
                urlBuilder.append("&fq=").append(fq);
            }

            url = urlBuilder.toString();
        }

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

    public String[] getParameterFacetQueries() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        return request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0];
    }
}
