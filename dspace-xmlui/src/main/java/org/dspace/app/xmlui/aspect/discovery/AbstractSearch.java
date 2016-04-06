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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortConfiguration.SORT_ORDER;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * This is an abstract search page. It is a collection of search methods that
 * are common between different search implementation. An implementer must
 * implement at least three methods: addBody(), getQuery(), and generateURL().
 * <p/>
 * See the SimpleSearch implementation.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public abstract class AbstractSearch extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(AbstractSearch.class);

    /**
     * Language strings
     */
    private static final Message T_head1_community =
            message("xmlui.Discovery.AbstractSearch.head1_community");

    private static final Message T_head1_collection =
            message("xmlui.Discovery.AbstractSearch.head1_collection");

    private static final Message T_head1_none =
            message("xmlui.Discovery.AbstractSearch.head1_none");

    private static final Message T_no_results =
            message("xmlui.ArtifactBrowser.AbstractSearch.no_results");

    private static final Message T_all_of_dspace =
            message("xmlui.ArtifactBrowser.AbstractSearch.all_of_dspace");

    private static final Message T_sort_by_relevance =
            message("xmlui.Discovery.AbstractSearch.sort_by.relevance");

    private static final Message T_sort_by = message("xmlui.Discovery.AbstractSearch.sort_by.head");

    private static final Message T_rpp = message("xmlui.Discovery.AbstractSearch.rpp");
    private static final Message T_result_head_3 = message("xmlui.Discovery.AbstractSearch.head3");
    private static final Message T_result_head_2 = message("xmlui.Discovery.AbstractSearch.head2");

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

    /**
     * Cached validity object
     */
    private SourceValidity validity;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
   	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    protected MetadataExposureService metadataExposureService = UtilServiceFactory.getInstance().getMetadataExposureService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();


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
     * Generate the cache validity object.
     * <p/>
     * This validity object should never "over cache" because it will
     * perform the search, and serialize the results using the
     * DSpaceValidity object.
     */
    public SourceValidity getValidity() {
        if (this.validity == null) {
            try {
                DSpaceValidity validity = new DSpaceValidity();

                DSpaceObject scope = getScope();
                validity.add(context, scope);

                performSearch(scope);

                List<DSpaceObject> results = this.queryResults.getDspaceObjects();

                if (results != null) {
                    validity.add("total:"+this.queryResults.getTotalSearchResults());
                    validity.add("start:"+this.queryResults.getStart());
                    validity.add("size:" + results.size());

                    for (DSpaceObject dso : results) {
                        validity.add(context, dso);
                    }
                }

                Map<String, List<DiscoverResult.FacetResult>> facetResults = this.queryResults.getFacetResults();
                for(String facetField : facetResults.keySet()){
                    List<DiscoverResult.FacetResult> facetValues = facetResults.get(facetField);
                    for (DiscoverResult.FacetResult facetResult : facetValues)
                    {
                        validity.add(facetField + facetResult.getAsFilterQuery() + facetResult.getCount());
                    }
                }

                this.validity = validity.complete();
            } catch (RuntimeException re) {
                throw re;
            }
            catch (Exception e) {
                this.validity = null;
            }

            // add log message that we are viewing the item
            // done here, as the serialization may not occur if the cache is valid
            logSearch();
        }
        return this.validity;
    }


    /**
     * Build the resulting search DRI document.
     */
    public abstract void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException;

    /**
     * Build the main form that should be the only form that the user interface requires
     * This form will be used for all discovery queries, filters, ....
     * At the moment however this form is only used to track search result hits
     * @param searchDiv the division to add the form to
     */
    protected void buildMainForm(Division searchDiv) throws WingException, SQLException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        //We set our action to context path, since the eventual action will depend on which url we click on
        Division mainForm = searchDiv.addInteractiveDivision("main-form", getBasicUrl(), Division.METHOD_POST, "");

        String query = getQuery();
        //Indicate that the form we are submitting lists search results
        mainForm.addHidden("search-result").setValue(Boolean.TRUE.toString());
        mainForm.addHidden("query").setValue(query);

        mainForm.addHidden("current-scope").setValue(dso == null ? "" : dso.getHandle());
        Map<String, String[]> fqs = getParameterFilterQueries();
        if (fqs != null)
        {
            for (String parameter : fqs.keySet())
            {
                String[] values = fqs.get(parameter);
                if(values != null)
                {
                    for (String value : values)
                    {
                        mainForm.addHidden(parameter).setValue(value);
                    }
                }
            }
        }

        mainForm.addHidden("rpp").setValue(getParameterRpp());
        Hidden sort_by = mainForm.addHidden("sort_by");
        if(!StringUtils.isBlank(request.getParameter("sort_by")))
        {
            sort_by.setValue(request.getParameter("sort_by"));
        }else{
            sort_by.setValue("score");
        }

        Hidden order = mainForm.addHidden("order");
        if(getParameterOrder() != null)
        {
            order.setValue(request.getParameter("order"));
        }else{
            DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);
            order.setValue(discoveryConfiguration.getSearchSortConfiguration().getDefaultSortOrder().toString());
        }
        if(!StringUtils.isBlank(request.getParameter("page")))
        {
            mainForm.addHidden("page").setValue(request.getParameter("page"));
        }
    }

    protected abstract String getBasicUrl() throws SQLException;

    /**
     * Attach a division to the given search division named "search-results"
     * which contains results for this search query.
     *
     * @param search The search division to contain the search-results division.
     */
    protected void buildSearchResultsDivision(Division search)
            throws IOException, SQLException, WingException, SearchServiceException {

        try {
            if (queryResults == null) {

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

        Division results = search.addDivision("search-results", "primary");
        buildSearchControls(results);


        DSpaceObject searchScope = getScope();

        int displayedResults;
        long totalResults;
        float searchTime;

        if(queryResults != null && 0 < queryResults.getTotalSearchResults())
        {
            displayedResults = queryResults.getDspaceObjects().size();
            totalResults = queryResults.getTotalSearchResults();
            searchTime = ((float) queryResults.getSearchTime() / 1000) % 60;

            if (searchScope instanceof Community) {
                Community community = (Community) searchScope;
                String communityName = community.getName();
                results.setHead(T_head1_community.parameterize(displayedResults, totalResults, communityName, searchTime));
            } else if (searchScope instanceof Collection){
                Collection collection = (Collection) searchScope;
                String collectionName = collection.getName();
                results.setHead(T_head1_collection.parameterize(displayedResults, totalResults, collectionName, searchTime));
            } else {
                results.setHead(T_head1_none.parameterize(displayedResults, totalResults, searchTime));
            }
        }

        if (queryResults != null && 0 < queryResults.getDspaceObjects().size())
        {

            // Pagination variables.
            int itemsTotal = (int) queryResults.getTotalSearchResults();
            int firstItemIndex = (int) this.queryResults.getStart() + 1;
            int lastItemIndex = (int) this.queryResults.getStart() + queryResults.getDspaceObjects().size();

            //if (itemsTotal < lastItemIndex)
            //    lastItemIndex = itemsTotal;
            int currentPage = this.queryResults.getStart() / this.queryResults.getMaxResults() + 1;
            int pagesTotal = (int) ((this.queryResults.getTotalSearchResults() - 1) / this.queryResults.getMaxResults()) + 1;
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("page", "{pageNum}");
            String pageURLMask = generateURL(parameters);
            pageURLMask = addFilterQueriesToUrl(pageURLMask);

            results.setMaskedPagination(itemsTotal, firstItemIndex,
                    lastItemIndex, currentPage, pagesTotal, pageURLMask);

            // Look for any communities or collections in the mix
            org.dspace.app.xmlui.wing.element.List dspaceObjectsList = null;

            // Put it on the top of level search result list
            dspaceObjectsList = results.addList("search-results-repository",
                    org.dspace.app.xmlui.wing.element.List.TYPE_DSO_LIST, "repository-search-results");

            List<DSpaceObject> commCollList = new ArrayList<DSpaceObject>();
            List<Item> itemList = new ArrayList<Item>();
            for (DSpaceObject resultDso : queryResults.getDspaceObjects())
            {
                if(resultDso.getType() == Constants.COMMUNITY || resultDso.getType() == Constants.COLLECTION)
                {
                    commCollList.add(resultDso);
                }else
                if(resultDso.getType() == Constants.ITEM)
                {
                    itemList.add((Item) resultDso);
                }
            }

            if(CollectionUtils.isNotEmpty(commCollList))
            {
                org.dspace.app.xmlui.wing.element.List commCollWingList = dspaceObjectsList.addList("comm-coll-result-list");
                commCollWingList.setHead(T_result_head_2);
                for (DSpaceObject dso : commCollList)
                {
                	renderDSO(dso,commCollWingList);
                }
            }

            if(CollectionUtils.isNotEmpty(itemList))
            {
                org.dspace.app.xmlui.wing.element.List itemWingList = dspaceObjectsList.addList("item-result-list");
                if(CollectionUtils.isNotEmpty(commCollList))
                {
                    itemWingList.setHead(T_result_head_3);

                }
                for (Item resultDso : itemList)
                {
                	renderDSO( resultDso, itemWingList);
                }
            }

        } else {
            results.addPara(T_no_results);
        }
        //}// Empty query
    }

    protected String addFilterQueriesToUrl(String pageURLMask) throws UIException {
        Map<String, String[]> filterQueryParams = getParameterFilterQueries();
        if(filterQueryParams != null)
        {
            StringBuilder maskBuilder = new StringBuilder(pageURLMask);
            for (String filterQueryParam : filterQueryParams.keySet())
            {
                String[] filterQueryValues = filterQueryParams.get(filterQueryParam);
                if(filterQueryValues != null)
                {
                    for (String filterQueryValue : filterQueryValues)
                    {
                        maskBuilder.append("&").append(filterQueryParam).append("=").append(encodeForURL(filterQueryValue));
                    }
                }
            }

            pageURLMask = maskBuilder.toString();
        }
        return pageURLMask;
    }

    /**
     * Render the given item, add all snippets to the list 
     * @param highlightedResults the highlighted results
     * @param item the DSpace item to be rendered
     * @param dsoMetadata a list of DSpace objects  
     * @throws WingException
     * @throws SQLException Database failure in services this calls
     */
    protected void renderItem(Item item, DiscoverResult.DSpaceObjectHighlightResult highlightedResults, org.dspace.app.xmlui.wing.element.List dsoMetadata ) throws WingException, SQLException {
    	if(highlightedResults != null)
        {
            //Add the full text snippet (if available !)
            List<String> fullSnippets = highlightedResults.getHighlightResults("fulltext");
            if(CollectionUtils.isNotEmpty(fullSnippets))
            {
                StringBuilder itemName = new StringBuilder();
                itemName.append(item.getHandle()).append(":").append("fulltext");
                org.dspace.app.xmlui.wing.element.List fullTextFieldList = dsoMetadata.addList(itemName.toString());
                for (String snippet : fullSnippets)
                {
                    addMetadataField(fullTextFieldList, snippet);
                }
            }
        }
    }

    /**
     * Render the given collection, add the collection's parent to the list 
     * @param highlightedResults
     * @param collection the collection to be rendered
     * @param dsoMetadata
     * @throws WingException
     * @throws SQLException
     */
    protected void renderCollection(Collection collection, DiscoverResult.DSpaceObjectHighlightResult highlightedResults, org.dspace.app.xmlui.wing.element.List dsoMetadata ) throws WingException, SQLException {
    	String parent = collection.getCommunities().get(0).getName();
    	addMetadataField(dsoMetadata.addList(collection.getHandle() + ":parent"), parent);
    }

    /**
     * Render the given community, add the community's parent to the list
     * @param highlightedResults
     * @param community the community to be rendered
     * @param dsoMetadata
     * @throws WingException
     * @throws SQLException
     */
    protected void renderCommunity(Community community, DiscoverResult.DSpaceObjectHighlightResult highlightedResults, org.dspace.app.xmlui.wing.element.List dsoMetadata ) throws WingException, SQLException {
		if(!(community.getParentCommunities().isEmpty())){
		     String parent = community.getParentCommunities().get(0).getName();
		     addMetadataField(dsoMetadata.addList(community.getHandle() + ":parent"), parent);
		}
	}
    
    /**Render the given dso, call a more specific render, add all dso metadata to the list, which metadata will be rendered where depends on the xsl  
     * @param dso
     * @param dsoMetadata
     * @throws WingException
     * @throws SQLException
     */
    protected void renderDSO(DSpaceObject dso, org.dspace.app.xmlui.wing.element.List dsoMetadata) throws WingException, SQLException 
    {    		
    	DSpaceObjectService<DSpaceObject> dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
    	List<MetadataValue> allMetadata = dsoService.getMetadata(dso, "*", "*", "*", "*");
    	DiscoverResult.DSpaceObjectHighlightResult highlightedResults = queryResults.getHighlightedResults(dso);
    	dsoMetadata = dsoMetadata.addList(dso.getHandle() + ":" + dsoService.getTypeText(dso).toLowerCase());
		if(dso.getType() == Constants.ITEM)
    	{
    		renderItem((Item)dso,highlightedResults,dsoMetadata);
    	}
    	else if(dso.getType() == Constants.COLLECTION) 
    	{
    		renderCollection((Collection)dso,highlightedResults,dsoMetadata);
    	}
    	else if(dso.getType() == Constants.COMMUNITY)
    	{
    		renderCommunity((Community)dso,highlightedResults,dsoMetadata);
    	}//else is another kind of dso
		
		//add all metadata
        addAllMetadataFields(allMetadata,dso,highlightedResults,dsoMetadata);
		
        //check if this dso has a dc.identifier.uri, else I create it based on its handle
        String identifier_uri=dsoService.getMetadataFirstValue(dso, "dc", "identifier", "uri", "*");
        if(identifier_uri == null && dso.getHandle() != null)
		{
			String handlePrefix = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("handle.canonical.prefix");
			addMetadataField(dsoMetadata.addList(dso.getHandle() + ":dc.identifier.uri"), handlePrefix +dso.getHandle());
		}		
    }
    
    
    /**Add all metadata from the dso, if not hidden, to the list
     * 
     * @param allMetadata
     * @param dso
     * @param highlightedResults
     * @param dsoMetadata
     * @throws WingException
     * @throws SQLException
     */
    private void addAllMetadataFields(List<MetadataValue> allMetadata,DSpaceObject dso,DiscoverResult.DSpaceObjectHighlightResult highlightedResults,org.dspace.app.xmlui.wing.element.List dsoMetadata ) throws WingException, SQLException
    {
    	for(MetadataValue data : allMetadata){
    		MetadataField theMetadatafield = data.getMetadataField();
    		if(StringUtils.isNotBlank(data.getValue())
    			&& !metadataExposureService.isHidden(context, theMetadatafield.getMetadataSchema().getName() , theMetadatafield.getElement(), theMetadatafield.getQualifier()))
    		{
    			addMetadataField(highlightedResults, theMetadatafield.toString('.'), dsoMetadata.addList(dso.getHandle() +":"+theMetadatafield.toString('.')), data.getValue());
    		}
    	}
    }

    /**
     * Add the current value to the wing list
     * @param highlightedResults the highlighted results
     * @param metadataKey the metadata key {schema}.{element}.{qualifier}
     * @param metadataFieldList the wing list we need to add the metadata value to
     * @param value the metadata value
     * @throws WingException
     */
    protected void addMetadataField(DiscoverResult.DSpaceObjectHighlightResult highlightedResults, String metadataKey, org.dspace.app.xmlui.wing.element.List metadataFieldList, String value) throws WingException {
        if(value == null){
            //In the unlikely event that the value is null, do not attempt to render this
            return;
        }

        if(highlightedResults != null && highlightedResults.getHighlightResults(metadataKey) != null)
        {
            //Loop over all our highlighted results
            for (String highlight : highlightedResults.getHighlightResults(metadataKey))
            {
                //If our non-highlighted value matches our original one, ensure that the highlighted one is used
                DiscoverHitHighlightingField highlightConfig = queryArgs.getHitHighlightingField(metadataKey);
                //We might also have it configured for ALL !
                if(highlightConfig == null)
                {
                    highlightConfig = queryArgs.getHitHighlightingField("*");
                }
                switch (highlightConfig.getMaxChars())
                {
                    case DiscoverHitHighlightingField.UNLIMITED_FRAGMENT_LENGTH:
                        //Exact match required
                        //\r is not indexed in solr & will cause issues
                        if(highlight.replaceAll("</?em>", "").equals(value.replace("\r", "")))
                        {
                            value = highlight;
                        }
                        break;
                    default:
                        //Partial match allowed, only render the highlighted part (will also remove \r since this char is not indexed in solr & will cause issues
                        if(value.replace("\r", "").contains(highlight.replaceAll("</?em>", "")))
                        {
                            value = highlight;
                        }
                        break;
                }

            }
        }
        addMetadataField(metadataFieldList, value);
    }

    /**
     * Add our metadata value, this value will might contain the highlight ("<em></em>") tags, these will be removed & rendered as highlight wing fields.
     * @param metadataFieldList the metadata list we need to add the value to
     * @param value the metadata value to be rendered
     * @throws WingException
     */
    protected void addMetadataField(org.dspace.app.xmlui.wing.element.List metadataFieldList, String value) throws WingException {
        //We need to put everything in <em> tags in a highlight !
        org.dspace.app.xmlui.wing.element.Item metadataItem = metadataFieldList.addItem();
        while(value.contains("<em>") && value.contains("</em>"))
        {
            if(0 < value.indexOf("<em>"))
            {
                //Add everything before the <em> !
                metadataItem.addContent(value.substring(0, value.indexOf("<em>")));
            }
            metadataItem.addHighlight("highlight").addContent(StringUtils.substringBetween(value, "<em>", "</em>"));

            value = StringUtils.substringAfter(value, "</em>");

        }
        if(0 < value.length())
        {
            metadataItem.addContent(value);
        }
    }

    /**
     * Add options to the search scope field. This field determines in which
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
            for (Community community : communityService.findAllTop(context)) {
                scope.addOption(community.getHandle(), community.getName());
            }
        } else if (scopeDSO instanceof Community) {
            // The scope is a community, display all collections contained
            // within
            Community community = (Community) scopeDSO;
            scope.addOption("/", T_all_of_dspace);
            scope.addOption(community.getHandle(), community.getName());
            scope.setOptionSelected(community.getHandle());

            for (Collection collection : community.getCollections()) {
                scope.addOption(collection.getHandle(), collection.getName());
            }
        } else if (scopeDSO instanceof Collection) {
            // The scope is a collection, display all parent collections.
            Collection collection = (Collection) scopeDSO;
            scope.addOption("/", T_all_of_dspace);
            scope.addOption(collection.getHandle(), collection.getName());
            scope.setOptionSelected(collection.getHandle());

            List<Community> communities = communityService.getAllParents(context, collection.getCommunities().get(0));
            for (Community community : communities) {
                scope.addOption(community.getHandle(), community.getName());
            }
        }
    }
    
    /**
     *  Prepare DiscoverQuery given the current scope and query string
     * 
     *  @param scope the dspace object parent
     */
    public DiscoverQuery prepareQuery(DSpaceObject scope, String query, String[] fqs) throws UIException, SearchServiceException {
    	
    	this.queryArgs = new DiscoverQuery();
    	
    	int page = getParameterPage();
    	    	
    	// Escape any special characters in this user-entered query
        query = DiscoveryUIUtils.escapeQueryChars(query);

    	List<String> filterQueries = new ArrayList<String>();

        if (fqs != null) {
            filterQueries.addAll(Arrays.asList(fqs));
        }        

        //Add the configured default filter queries
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
        queryArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));

        if (filterQueries.size() > 0) {
        	queryArgs.addFilterQueries(filterQueries.toArray(new String[filterQueries.size()]));
        }

        queryArgs.setMaxResults(getParameterRpp());

        String sortBy = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
        DiscoverySortConfiguration searchSortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        if(sortBy == null){
            //Attempt to find the default one, if none found we use SCORE
            sortBy = "score";
            if(searchSortConfiguration != null){
                for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration.getSortFields()) {
                    if(sortFieldConfiguration.equals(searchSortConfiguration.getDefaultSort())){
                        sortBy = SearchUtils.getSearchService().toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());
                    }
                }
            }
        }
        String sortOrder = ObjectModelHelper.getRequest(objectModel).getParameter("order");
        if(sortOrder == null && searchSortConfiguration != null){
            sortOrder = searchSortConfiguration.getDefaultSortOrder().toString();
        }

        if (sortOrder == null || sortOrder.equalsIgnoreCase("DESC"))
        {
            queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.desc);
        }
        else
        {
            queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.asc);
        }


        String groupBy = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");


        // Enable groupBy collapsing if designated
        if (groupBy != null && !groupBy.equalsIgnoreCase("none")) {
            /** Construct a Collapse Field Query */
            queryArgs.addProperty("collapse.field", groupBy);
            queryArgs.addProperty("collapse.threshold", "1");
            queryArgs.addProperty("collapse.includeCollapsedDocs.fl", "handle");
            queryArgs.addProperty("collapse.facet", "before");

            //queryArgs.a  type:Article^2

            // TODO: This is a hack to get Publications (Articles) to always be at the top of Groups.
            // TODO: I think that can be more transparently done in the solr solrconfig.xml with DISMAX and boosting
            /** sort in groups to get publications to top */
            queryArgs.setSortField("dc.type", DiscoverQuery.SORT_ORDER.asc);

        }


        queryArgs.setQuery(query != null && !query.trim().equals("") ? query : null);

        if (page > 1)
        {
            queryArgs.setStart((page - 1) * queryArgs.getMaxResults());
        }
        else
        {
            queryArgs.setStart(0);
        }

        if(discoveryConfiguration.getHitHighlightingConfiguration() != null)
        {
            List<DiscoveryHitHighlightFieldConfiguration> metadataFields = discoveryConfiguration.getHitHighlightingConfiguration().getMetadataFields();
            for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : metadataFields)
            {
                queryArgs.addHitHighlightingField(new DiscoverHitHighlightingField(fieldConfiguration.getField(), fieldConfiguration.getMaxSize(), fieldConfiguration.getSnippets()));
            }
        }

        queryArgs.setSpellCheck(discoveryConfiguration.isSpellCheckEnabled());
        
        return queryArgs;
    }

    /**
     * Query DSpace for a list of all items / collections / or communities that
     * match the given search query.
     *
     *
     * @param scope the dspace object parent
     */
    public void performSearch(DSpaceObject scope) throws UIException, SearchServiceException {

        if (queryResults != null) {
            return;
        }
        
        this.queryResults = SearchUtils.getSearchService().search(context, scope, prepareQuery(scope, getQuery(), getFilterQueries()));
    }

    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @return an array containing the filter queries
     */
    protected Map<String, String[]> getParameterFilterQueries()
    {
        try {
            Map<String, String[]> result = new HashMap<String, String[]>();
            result.put("fq", ObjectModelHelper.getRequest(objectModel).getParameterValues("fq"));
            return result;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all the filter queries for use by solr
     * This method returns more expanded filter queries then the getParameterFilterQueries
     * @return an array containing the filter queries
     */
    protected String[] getFilterQueries() {
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

    /**
     * Determine if the scope of the search should fixed or is changeable by the
     * user.
     * <p/>
     * The search scope when performed by url, i.e. they are at the url handle/xxxx/xx/search
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
        this.queryArgs = null;
        this.queryResults = null;
        this.validity = null;
        super.recycle();
    }


    protected void buildSearchControls(Division div)
            throws WingException, SQLException {


        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);

        Division searchControlsGear = div.addDivision("masked-page-control").addDivision("search-controls-gear", "controls-gear-wrapper");


        /**
         * Add sort by options, the gear will be rendered by a combination of javascript & css
         */
        String currentSort = getParameterSortBy();
        org.dspace.app.xmlui.wing.element.List sortList = searchControlsGear.addList("sort-options", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "gear-selection");
        sortList.addItem("sort-head", "gear-head first").addContent(T_sort_by);
        DiscoverySortConfiguration searchSortConfiguration = discoveryConfiguration.getSearchSortConfiguration();

        org.dspace.app.xmlui.wing.element.List sortOptions = sortList.addList("sort-selections");
        boolean selected = ("score".equals(currentSort) || (currentSort == null && searchSortConfiguration.getDefaultSort() == null));
        sortOptions.addItem("relevance", "gear-option" + (selected ? " gear-option-selected" : "")).addXref("sort_by=score&order=" + searchSortConfiguration.getDefaultSortOrder(), T_sort_by_relevance);

        if (currentSort == null
                && searchSortConfiguration.getDefaultSort() != null)
        {
            currentSort = SearchUtils.getSearchService()
                    .toSortFieldIndex(
                            searchSortConfiguration.getDefaultSort()
                                    .getMetadataField(),
                            searchSortConfiguration.getDefaultSort().getType());
        }
        String sortOrder = getParameterOrder();
        if (sortOrder == null
                && searchSortConfiguration.getDefaultSortOrder() != null)
        {
            sortOrder = searchSortConfiguration.getDefaultSortOrder().name();
        }

        if(searchSortConfiguration.getSortFields() != null)
        {
            for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration.getSortFields())
            {
                String sortField = SearchUtils.getSearchService().toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());

                boolean selectedAsc = sortField.equals(currentSort)
                        && SORT_ORDER.asc.name().equals(sortOrder);
                boolean selectedDesc = sortField.equals(currentSort)
                        && SORT_ORDER.desc.name().equals(sortOrder);
                String sortFieldParam = "sort_by=" + sortField + "&order=";
                sortOptions.addItem(sortField, "gear-option" + (selectedAsc ? " gear-option-selected" : "")).addXref(sortFieldParam + "asc", message("xmlui.Discovery.AbstractSearch.sort_by." + sortField + "_asc"));
                sortOptions.addItem(sortField, "gear-option" + (selectedDesc ? " gear-option-selected" : "")).addXref(sortFieldParam + "desc", message("xmlui.Discovery.AbstractSearch.sort_by." + sortField + "_desc"));
            }
        }

        //Add the rows per page
        sortList.addItem("rpp-head", "gear-head").addContent(T_rpp);
        org.dspace.app.xmlui.wing.element.List rppOptions = sortList.addList("rpp-selections");
        for (int i : RESULTS_PER_PAGE_PROGRESSION)
        {
            rppOptions.addItem("rpp-" + i, "gear-option" + (i == getParameterRpp() ? " gear-option-selected" : "")).addXref("rpp=" + i, Integer.toString(i));
        }
    }

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
            dso = handleService.resolveToObject(context, scopeString);
        }

        return dso;
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
                + (queryArgs == null ? "" : queryArgs.getQuery()) + "\",results=(" + countCommunities + ","
                + countCollections + "," + countItems + ")"));
    }
}

