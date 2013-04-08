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
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.discovery.*;
import org.dspace.sort.SortOption;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * This is an abstract search page. It is a collection of search methods that
 * are common between diffrent search implementation. An implementer must
 * implement at least three methods: addBody(), getQuery(), and generateURL().
 * <p/>
 * See the implementors SimpleSearch.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 *
 * This class has been adjusted so that the search isn't displayed no more
 */
public abstract class AbstractSearch extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(AbstractSearch.class);


    /**
     * Language strings
     */
    private static final Message T_result_query =
            message("xmlui.ArtifactBrowser.AbstractSearch.result_query");

    private static final Message T_head1_community =
            message("xmlui.ArtifactBrowser.AbstractSearch.head1_community");

    private static final Message T_head1_collection =
            message("xmlui.ArtifactBrowser.AbstractSearch.head1_collection");

    private static final Message T_head1_none =
            message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");

    private static final Message T_head2 =
            message("xmlui.ArtifactBrowser.AbstractSearch.head2");

    private static final Message T_no_results =
            message("xmlui.ArtifactBrowser.AbstractSearch.no_results");

    private static final Message T_all_of_dspace =
            message("xmlui.ArtifactBrowser.AbstractSearch.all_of_dspace");

    protected static final Message T_sort_by_relevance =
            message("xmlui.ArtifactBrowser.AbstractSearch.sort_by.relevance");

    protected static final Message T_sort_by = message("xmlui.ArtifactBrowser.AbstractSearch.sort_by");

    protected static final Message T_order = message("xmlui.ArtifactBrowser.AbstractSearch.order");
    protected static final Message T_order_asc = message("xmlui.ArtifactBrowser.AbstractSearch.order.asc");
    protected static final Message T_order_desc = message("xmlui.ArtifactBrowser.AbstractSearch.order.desc");

    protected static final Message T_rpp = message("xmlui.ArtifactBrowser.AbstractSearch.rpp");

    /**
     * The options for results per page
     */
    protected static final int[] RESULTS_PER_PAGE_PROGRESSION = {5, 10, 20, 40, 60, 80, 100};

    /**
     * Cached validity object
     */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            String key = "";

            // Page Parameter
            key += "-" + getParameterPage();
            key += "-" + getParameterRpp();
            key += "-" + getParameterSortBy();
            key += "-" + getParameterOrder();
            key += "-" + getParameterEtAl();

            // What scope the search is at
            DSpaceObject scope = getScope();
            if (scope != null)
            {
                key += "-" + scope.getHandle();
            }

            // The actual search query.
            key += "-" + getQuery();

            return HashUtil.hash(key);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            // Ignore all errors and just don't cache.
            return "0";
        }
    }

    /**
     * Build the resulting search DRI document.
     */
    public abstract void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException;

    /**
     * Attach a division to the given search division named "search-results"
     * which contains results for this search query.
     *
     * @param search The search division to contain the search-results division.
     */
    protected void buildSearchResultsDivision(Division search)
            throws IOException, SQLException, WingException, SearchServiceException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        try {
            if (queryResults == null || queryResults.getResults() == null) {

                DSpaceObject scope = getScope();
                this.performSearch(scope);
            }
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

        DSpaceObject searchScope = getScope();

        if (searchScope instanceof Community) {
            Community community = (Community) searchScope;
            String communityName = community.getMetadata("name");
            results.setHead(T_head1_community.parameterize(communityName));
        } else if (searchScope instanceof Collection) {
            Collection collection = (Collection) searchScope;
            String collectionName = collection.getMetadata("name");
            results.setHead(T_head1_collection.parameterize(collectionName));
        } else {
            results.setHead(T_head1_none);
        }


        FacetField facet = queryResults.getFacetField("location.coll");
        if(facet != null){
            java.util.List<FacetField.Count> facetVals = facet.getValues();
            if(facetVals != null)
            {
                org.dspace.app.xmlui.wing.element.List list = results.addList("tabs");
                //ADD COLLECTION 2 (Dryad) first
                for (FacetField.Count count : facetVals) {
                    if(count.getName().equals("2")){
                        buildTabs(count,request,list);
                    }
                }
                //remove collection 7 (dryadlab)
                for (FacetField.Count count : facetVals) {
                    if(!count.getName().equals("3")&&!count.getName().equals("2")&&!count.getName().equals("7")){
                        buildTabs(count,request,list);
                    }
                }
            }

        }

        if (queryResults != null &&
                queryResults.getResults().getNumFound() > 0) {

            SolrDocumentList solrResults = queryResults.getResults();

            if(solrResults.size()==1){
                HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

                String baseURL = ConfigurationManager.getProperty("dspace.baseUrl");
                for (SolrDocument doc : solrResults) {
                    DSpaceObject resultDSO = SearchUtils.findDSpaceObject(context, doc);
                    if (resultDSO instanceof Item){
                        Item item =  (Item) resultDSO;
                        DCValue[] value = item.getMetadata("dc","identifier",null,Item.ANY) ;
                        String buildURL = null;
                        if(value!=null && value.length > 0){
                            String doi = value[0].value;
                            buildURL = baseURL+"/resource/"+doi;
                        }
                        else{
                            if(item.getHandle()!=null)
                                buildURL = baseURL+"/handle/"+item.getHandle();
                        }
                        if(buildURL!=null) {
                            httpResponse.sendRedirect(buildURL);
                            return;
                        }
                    }
                }
            }


            // Pagination variables.
            int itemsTotal = (int) solrResults.getNumFound();
            int firstItemIndex = (int) solrResults.getStart() + 1;
            int lastItemIndex = (int) solrResults.getStart() + solrResults.size();

            //if (itemsTotal < lastItemIndex)
            //    lastItemIndex = itemsTotal;
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

            results.setMaskedPagination(itemsTotal, firstItemIndex,
                    lastItemIndex, currentPage, pagesTotal, pageURLMask);

            // Look for any communities or collections in the mix
            ReferenceSet referenceSet = null;

            for (SolrDocument doc : solrResults) {

                DSpaceObject resultDSO =
                        SearchUtils.findDSpaceObject(context, doc);

                if (resultDSO instanceof Community
                        || resultDSO instanceof Collection) {
                    if (referenceSet == null) {
                        referenceSet = results.addReferenceSet("search-results-repository",
                                ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");
                        // Set a heading showing that we will be listing containers that matched:
                        referenceSet.setHead(T_head2);
                    }
                    referenceSet.addReference(resultDSO);
                }
            }


            // Put in palce top level referenceset
            referenceSet = results.addReferenceSet("search-results-repository",
                    ReferenceSet.TYPE_SUMMARY_LIST, null, "repository-search-results");


            for (SolrDocument doc : solrResults) {

                DSpaceObject resultDSO = SearchUtils.findDSpaceObject(context, doc);

                if (resultDSO instanceof Item) {
                    referenceSet.addReference(resultDSO);
                }
            }

        } else {
            results.addPara(T_no_results);
        }
        //}// Empty query
    }

    /**
     * Add options to the search scope field. This field determines in what
     * communities or collections to search for the query.
     * <p/>
     * The scope list will depend upon the current search scope. There are three
     * cases:
     * <p/>
     * No current scope: All top level communities are listed.
     * <p/>
     * The current scope is a community: All collections contained within the
     * community are listed.
     * <p/>
     * The current scope is a collection: All parent communities are listed.
     *
     * @param scope The current scope field.
     */
    protected void buildScopeList(Select scope) throws SQLException,
            WingException {

        DSpaceObject scopeDSO = getScope();
        if (scopeDSO == null) {
            // No scope, display all root level communities
            scope.addOption("/", T_all_of_dspace);
            scope.setOptionSelected("/");
            for (Community community : Community.findAllTop(context)) {
                scope.addOption(community.getHandle(), community.getMetadata("name"));
            }
        } else if (scopeDSO instanceof Community) {
            // The scope is a community, display all collections contained
            // within
            Community community = (Community) scopeDSO;
            scope.addOption("/", T_all_of_dspace);
            scope.addOption(community.getHandle(), community.getMetadata("name"));
            scope.setOptionSelected(community.getHandle());

            for (Collection collection : community.getCollections()) {
                scope.addOption(collection.getHandle(), collection.getMetadata("name"));
            }
        } else if (scopeDSO instanceof Collection) {
            // The scope is a collection, display all parent collections.
            Collection collection = (Collection) scopeDSO;
            scope.addOption("/", T_all_of_dspace);
            scope.addOption(collection.getHandle(), collection.getMetadata("name"));
            scope.setOptionSelected(collection.getHandle());

            Community[] communities = collection.getCommunities()[0]
                    .getAllParents();
            for (Community community : communities) {
                scope.addOption(community.getHandle(), community.getMetadata("name"));
            }
        }
    }

    /**
     * Query DSpace for a list of all items / collections / or communities that
     * match the given search query.
     *
     * @return The associated query results.
     */
    public void performSearch(DSpaceObject scope) throws UIException, SearchServiceException {

        if (queryResults != null)
        {
            return;
        }


        String query = getQuery();

        //DSpaceObject scope = getScope();

        int page = getParameterPage();

        List<String> filterQueries = new ArrayList<String>();
        //remove the old collection and community filter so we can use solr to collect all the collection information
        String[] location = ObjectModelHelper.getRequest(objectModel).getParameterValues("fq");

        boolean found = false;
        if(location!=null){
            for(String loc : location)
            {
                if(loc.startsWith("location.coll:"))
                {
                    found = true;
                }
            }
        }
        if(!found)
        {
            filterQueries.add("{!tag=dt}location.coll:2");
        }




        String[] fqs = getSolrFilterQueries();
        for(int i=0;i<fqs.length;i++){
            if(fqs[i].contains("location.coll:")){

                String tmp= fqs[i].replaceAll("location.coll","{!tag=dt}location.coll");
                fqs[i] = tmp;
            }

        }

        if (fqs != null)
        {
            filterQueries.addAll(Arrays.asList(fqs));
        }


        try {
            queryArgs = this.prepareDefaultFilters(getView(), filterQueries.toArray(new String[filterQueries.size()]));
        } catch (SQLException e) {
            log.error(e);
            return;
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


        queryArgs.setQuery(query != null && !query.trim().equals("") ? query : "DSpaceStatus:Archived");

        if (page > 1)
        {
            queryArgs.setStart((page - 1) * queryArgs.getRows());
        }
        else
        {
            queryArgs.setStart(0);
        }


        queryArgs.addFacetField("{!ex=dt}location.coll");
        queryArgs.add("f.location.coll.facet.mincount","0");



        // Use mlt
        // queryArgs.add("mlt", "true");

        // The fields to use for similarity. NOTE: if possible, these should have a stored TermVector
        // queryArgs.add("mlt.fl", "author");

        // Minimum Term Frequency - the frequency below which terms will be ignored in the source doc.
        // queryArgs.add("mlt.mintf", "1");

        // Minimum Document Frequency - the frequency at which words will be ignored which do not occur in at least this many docs.
        // queryArgs.add("mlt.mindf", "1");

        //queryArgs.add("mlt.q", "");

        // mlt.minwl
        // minimum word length below which words will be ignored.

        // mlt.maxwl
        // maximum word length above which words will be ignored.

        // mlt.maxqt
        // maximum number of query terms that will be included in any generated query.

        // mlt.maxntp
        // maximum number of tokens to parse in each example doc field that is not stored with TermVector support.

        // mlt.boost
        // [true/false] set if the query will be boosted by the interesting term relevance.

        // mlt.qf
        // Query fields and their boosts using the same format as that used in DisMaxRequestHandler. These fields must also be specified in mlt.fl.


        //filePost.addParameter("fl", "handle, "search.resourcetype")");
        //filePost.addParameter("field", "search.resourcetype");

        //Set the default limit to 11
        /*
        ClientUtils.escapeQueryChars(location)
        //f.category.facet.limit=5

        for(Enumeration en = request.getParameterNames(); en.hasMoreElements();)
        {
        	String key = (String)en.nextElement();
        	if(key.endsWith(".facet.limit"))
        	{
        		filePost.addParameter(key, request.getParameter(key));
        	}
        }
        */

        try {
            this.queryResults = getSearchService().search(ContextUtil.obtainContext(objectModel), queryArgs);
        } catch (SQLException e) {
            log.error("Error while retrieving context", e);
        }
    }



    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @return an array containing the filter queries
     */
    protected String[] getParameterFilterQueries(){
        try {
            return ObjectModelHelper.getRequest(objectModel).getParameterValues("fq");
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all the filter queries for use by solr
     *  This method returns more expanded filter queries then the getParameterFilterQueries
     * @return an array containing the filter queries
     */
    protected String[] getSolrFilterQueries() {
        try {
            return ObjectModelHelper.getRequest(objectModel).getParameterValues("fq");
        }
        catch (Exception e) {
            return null;
        }
    }

    protected String[] getFacetsList() {
        try {
            return ObjectModelHelper.getRequest(objectModel).getParameterValues("fl");
        }
        catch (Exception e) {
            return null;
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
            return SearchUtils.getConfig().getInt("solr.results.per.page", 10);
        }
    }

    protected String getParameterSortBy() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
        return s != null ? s : "score";
    }

    protected String getParameterGroup() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");
        return s != null ? s : "none";
    }

    protected String getParameterOrder() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("order");
        return s != null ? s : "DESC";
    }

    protected int getParameterEtAl() {
        try {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("etal"));
        }
        catch (Exception e) {
            return 0;
        }
    }

    /**
     * Determine if the scope of the search should fixed or is changeable by the
     * user.
     * <p/>
     * The search scope when preformed by url, i.e. they are at the url handle/xxxx/xx/search
     * then it is fixed. However at the global level the search is variable.
     *
     * @return true if the scope is variable, false otherwise.
     */
    protected boolean variableScope() throws SQLException {
        return (HandleUtil.obtainHandle(objectModel) == null);
    }

    /**
     * Extract the query string. Under most implementations this will be derived
     * from the url parameters.
     *
     * @return The query string.
     */
    protected abstract String getQuery() throws UIException;

    /**
     * Generate a url to the given search implementation with the associated
     * parameters included.
     *
     * @param parameters
     * @return The post URL
     */
    protected abstract String generateURL(Map<String, String> parameters)
            throws UIException;


    /**
     * Recycle
     */
    public void recycle() {
        validity = null;
        queryResults = null;
        queryArgs = null;
        super.recycle();
    }


    protected void buildSearchControls(Division div)
            throws WingException {




        Table controlsTable = div.addTable("search-controls", 1, 3);
        //Table controlsTable = div.addTable("search-controls", 1, 4);
        Row controlsRow = controlsTable.addRow(Row.ROLE_DATA);

        // Create a control for the number of records to display
        Cell rppCell = controlsRow.addCell();
        rppCell.addContent(T_rpp);
        Select rppSelect = rppCell.addSelect("rpp");
        for (int i : RESULTS_PER_PAGE_PROGRESSION) {
            rppSelect.addOption((i == getParameterRpp()), i, Integer.toString(i));
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


        // Create a control for the number of authors per item to display
        // FIXME This is currently disabled, as the supporting functionality
        // is not currently present in xmlui
        //if (isItemBrowse(info))
        //{
        //    controlsForm.addContent(T_etal);
        //    Select etalSelect = controlsForm.addSelect(BrowseParams.ETAL);
        //
        //    etalSelect.addOption((info.getEtAl() < 0), 0, T_etal_all);
        //    etalSelect.addOption(1 == info.getEtAl(), 1, Integer.toString(1));
        //
        //    for (int i = 5; i <= 50; i += 5)
        //    {
        //        etalSelect.addOption(i == info.getEtAl(), i, Integer.toString(i));
        //    }
        //}
    }

    protected void logSearch() {
        int countCommunities = 0;
        int countCollections = 0;
        int countItems = 0;

        /**
         * TODO: Maybe we can create a default "type" facet for this
         * will give results for Items, Communities and Collection types
         * benefits... no iteration over results at all to sum types
         * leaves it upto solr...

         for (Object type : queryResults.getHitTypes())
         {
         if (type instanceof Integer)
         {
         switch (((Integer)type).intValue())
         {
         case Constants.ITEM:       countItems++;        break;
         case Constants.COLLECTION: countCollections++;  break;
         case Constants.COMMUNITY:  countCommunities++;  break;
         }
         }
         }
         */
        String logInfo = "";

        try {
            DSpaceObject dsoScope = getScope();

            if (dsoScope instanceof Collection) {
                logInfo = "collection_id=" + dsoScope.getID() + ",";
            } else if (dsoScope instanceof Community) {
                logInfo = "community_id=" + dsoScope.getID() + ",";
            }
        }
        catch (SQLException sqle) {
            // Ignore, as we are only trying to get the scope to add detail to the log message
        }

        log.info(LogManager.getHeader(context, "search", logInfo + "query=\""
                + queryArgs.getQuery() + "\",results=(" + countCommunities + ","
                + countCollections + "," + countItems + ")"));
    }

    public static String parameterReplace(String prefix,String s,String newNumber){
        String query=s;
        String part = s.substring(s.indexOf(prefix) + prefix.length());
        String collectionNumber="";
        boolean isNumber=true;
        while(isNumber){
            try{
                String number = part.substring(0, 1);
                Integer.parseInt(number);
                collectionNumber+=number;
                part=part.substring(1);
            }catch (Exception nfe){
                isNumber=false;
            }
        }
        query = s.replace(prefix+collectionNumber, prefix+newNumber);
        return query;
    }

    private void buildTabs(FacetField.Count count,Request request,org.dspace.app.xmlui.wing.element.List list)
            throws SQLException,WingException
    {
        String filterQuery = count.getAsFilterQuery();
        String paramsQuery=request.getQueryString();
        Collection coll = Collection.find(context,Integer.parseInt(count.getName()));
        org.dspace.app.xmlui.wing.element.Item collectionLink = list.addItem();
        if(paramsQuery.contains("fq=location.coll"))  {
            if(paramsQuery.contains("fq=location.coll:"+count.getName())||paramsQuery.contains("fq=location.coll%3A"+count.getName()))

            {
                collectionLink.addHidden("selected");
            }
        }
        else{
            if(count.getName().equals("2"))
            {
                collectionLink.addHidden("selected");
            }
        }




        if(request.getQueryString().contains("&fq=location.coll:")){
            paramsQuery = parameterReplace("&fq=location.coll:",paramsQuery,count.getName());
        }
        else if(request.getQueryString().contains("&fq=location.coll%3A")){
            paramsQuery = parameterReplace("&fq=location.coll%3A",paramsQuery,count.getName());
        }
        else{
            paramsQuery=paramsQuery+"&fq=location.coll:" + count.getName();
        }

        if(request.getQueryString().contains("&page=")){
            paramsQuery = parameterReplace("&page=",paramsQuery,"1");
        }


        collectionLink.addXref(contextPath + "/" + getDiscoverUrl() + "?" +paramsQuery,coll.getName() + " (" + count.getCount() + ")" );
    }
}
