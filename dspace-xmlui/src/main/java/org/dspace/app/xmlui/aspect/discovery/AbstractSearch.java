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
 * <p>
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
     * @return the key.
     */
    @Override
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
        } catch (SQLException | UIException e) {
            // Ignore all errors and just don't cache.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p>
     * This validity object should never "over cache" because it will
     * perform the search, and serialize the results using the
     * DSpaceValidity object.
     * @return the validity.
     */
    @Override
    public SourceValidity getValidity() {
        if (this.validity == null) {
            try {
                DSpaceValidity newValidity = new DSpaceValidity();

                DSpaceObject scope = getScope();
                newValidity.add(context, scope);

                performSearch(scope);

                List<DSpaceObject> results = this.queryResults.getDspaceObjects();

                if (results != null) {
                    newValidity.add("total:"+this.queryResults.getTotalSearchResults());
                    newValidity.add("start:"+this.queryResults.getStart());
                    newValidity.add("size:" + results.size());

                    for (DSpaceObject dso : results) {
                        newValidity.add(context, dso);
                    }
                }

                Map<String, List<DiscoverResult.FacetResult>> facetResults = this.queryResults.getFacetResults();
                for(String facetField : facetResults.keySet()){
                    List<DiscoverResult.FacetResult> facetValues = facetResults.get(facetField);
                    for (DiscoverResult.FacetResult facetResult : facetValues)
                    {
                        newValidity.add(facetField + facetResult.getAsFilterQuery() + facetResult.getCount());
                    }
                }

                this.validity = newValidity.complete();
            } catch (RuntimeException re) {
                throw re;
            }
            catch (SQLException | UIException | SearchServiceException e) {
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
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     * @throws java.sql.SQLException whenever.
     * @throws java.io.IOException whenever.
     * @throws org.dspace.authorize.AuthorizeException whenever.
     */
    @Override
    public abstract void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException;

    /**
     * Build the main form that should be the only form that the user interface requires
     * This form will be used for all discovery queries, filters, ....
     * At the moment however this form is only used to track search result hits
     * @param searchDiv the division to add the form to
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws java.sql.SQLException passed through.
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
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.dspace.discovery.SearchServiceException passed through.
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
        catch (SQLException | UIException | SearchServiceException e) {
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
            Map<String, String> urlParameters = new HashMap<>();
            urlParameters.put("page", "{pageNum}");
            String pageURLMask = generateURL(urlParameters);
            pageURLMask = addFilterQueriesToUrl(pageURLMask);

            results.setMaskedPagination(itemsTotal, firstItemIndex,
                    lastItemIndex, currentPage, pagesTotal, pageURLMask);

            // Look for any communities or collections in the mix
            org.dspace.app.xmlui.wing.element.List dspaceObjectsList = null;

            // Put it on the top of level search result list
            dspaceObjectsList = results.addList("search-results-repository",
                    org.dspace.app.xmlui.wing.element.List.TYPE_DSO_LIST, "repository-search-results");

            List<DSpaceObject> commCollList = new ArrayList<>();
            List<Item> itemList = new ArrayList<>();
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
                    DiscoverResult.DSpaceObjectHighlightResult highlightedResults = queryResults.getHighlightedResults(dso);
                    if(dso.getType() == Constants.COMMUNITY)
                    {
                        //Render our community !
                        org.dspace.app.xmlui.wing.element.List communityMetadata = commCollWingList.addList(dso.getHandle() + ":community");

                        renderCommunity((Community) dso, highlightedResults, communityMetadata);
                    }else
                    if(dso.getType() == Constants.COLLECTION)
                    {
                        //Render our collection !
                        org.dspace.app.xmlui.wing.element.List collectionMetadata = commCollWingList.addList(dso.getHandle() + ":collection");

                        renderCollection((Collection) dso, highlightedResults, collectionMetadata);
                    }
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
                    DiscoverResult.DSpaceObjectHighlightResult highlightedResults = queryResults.getHighlightedResults(resultDso);
                    renderItem(itemWingList, resultDso, highlightedResults);
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
     * Render the given item, all metadata is added to the given list, which metadata will be rendered where depends on the xsl
     * @param dspaceObjectsList a list of DSpace objects
     * @param item the DSpace item to be rendered
     * @param highlightedResults the highlighted results
     * @throws WingException passed through.
     * @throws SQLException Database failure in services this calls
     */
    protected void renderItem(org.dspace.app.xmlui.wing.element.List dspaceObjectsList, Item item, DiscoverResult.DSpaceObjectHighlightResult highlightedResults) throws WingException, SQLException {
        org.dspace.app.xmlui.wing.element.List itemList = dspaceObjectsList.addList(item.getHandle() + ":item");

        List<MetadataField> metadataFields = metadataFieldService.findAll(context);
        for (MetadataField metadataField : metadataFields)
        {
            //Retrieve the schema for this field
            String schema = metadataField.getMetadataSchema().getName();
            //Check if our field isn't hidden
            if (!metadataExposureService.isHidden(context, schema, metadataField.getElement(), metadataField.getQualifier()))
            {
                //Check if our metadata field is highlighted
                StringBuilder metadataKey = new StringBuilder();
                metadataKey.append(schema).append(".").append(metadataField.getElement());
                if (metadataField.getQualifier() != null)
                {
                    metadataKey.append(".").append(metadataField.getQualifier());
                }

                StringBuilder itemName = new StringBuilder();
                itemName.append(item.getHandle()).append(":").append(metadataKey.toString());


                List<MetadataValue> itemMetadata = itemService.getMetadata(item, schema, metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
                if(!CollectionUtils.isEmpty(itemMetadata))
                {
                    org.dspace.app.xmlui.wing.element.List metadataFieldList = itemList.addList(itemName.toString());
                    for (MetadataValue metadataValue : itemMetadata)
                    {
                        String value = metadataValue.getValue();
                        addMetadataField(highlightedResults, metadataKey.toString(), metadataFieldList, value);
                    }
                }
            }
        }

        //Check our highlighted results, we may need to add non-metadata (like our full text)
        if(highlightedResults != null)
        {
            //Also add the full text snippet (if available !)
            List<String> fullSnippets = highlightedResults.getHighlightResults("fulltext");
            if(CollectionUtils.isNotEmpty(fullSnippets))
            {
                StringBuilder itemName = new StringBuilder();
                itemName.append(item.getHandle()).append(":").append("fulltext");

                org.dspace.app.xmlui.wing.element.List fullTextFieldList = itemList.addList(itemName.toString());

                for (String snippet : fullSnippets)
                {
                    addMetadataField(fullTextFieldList, snippet);
                }
            }
        }
    }

    /**
     * Render the given collection, all collection metadata is added to the list
     * @param collection the collection to be rendered
     * @param highlightedResults the highlighted results
     * @param collectionMetadata list of metadata values.
     * @throws WingException passed through.
     */
    protected void renderCollection(Collection collection,
            DiscoverResult.DSpaceObjectHighlightResult highlightedResults,
            org.dspace.app.xmlui.wing.element.List collectionMetadata)
            throws WingException {

        String description = collectionService.getMetadata(collection, "introductory_text");
        String description_abstract = collectionService.getMetadata(collection, "short_description");
        String description_table = collectionService.getMetadata(collection, "side_bar_text");
        String identifier_uri = handleService.getCanonicalPrefix() + collection.getHandle();
        String provenance = collectionService.getMetadata(collection, "provenance_description");
        String rights = collectionService.getMetadata(collection, "copyright_text");
        String rights_license = collectionService.getMetadata(collection, "license");
        String title = collection.getName();

        if(StringUtils.isNotBlank(description))
        {
            addMetadataField(highlightedResults, "dc.description", collectionMetadata.addList(collection.getHandle() + ":dc.description"), description);
        }
        if(StringUtils.isNotBlank(description_abstract))
        {
            addMetadataField(highlightedResults, "dc.description.abstract", collectionMetadata.addList(collection.getHandle() + ":dc.description.abstract"), description_abstract);
        }
        if(StringUtils.isNotBlank(description_table))
        {
            addMetadataField(highlightedResults, "dc.description.tableofcontents", collectionMetadata.addList(collection.getHandle() + ":dc.description.tableofcontents"), description_table);
        }
        if(StringUtils.isNotBlank(identifier_uri))
        {
            addMetadataField(highlightedResults, "dc.identifier.uri", collectionMetadata.addList(collection.getHandle() + ":dc.identifier.uri"), identifier_uri);
        }
        if(StringUtils.isNotBlank(provenance))
        {
            addMetadataField(highlightedResults, "dc.provenance", collectionMetadata.addList(collection.getHandle() + ":dc.provenance"), provenance);
        }
        if(StringUtils.isNotBlank(rights))
        {
            addMetadataField(highlightedResults, "dc.rights", collectionMetadata.addList(collection.getHandle() + ":dc.rights"), rights);
        }
        if(StringUtils.isNotBlank(rights_license))
        {
            addMetadataField(highlightedResults, "dc.rights.license", collectionMetadata.addList(collection.getHandle() + ":dc.rights.license"), rights_license);
        }
        if(StringUtils.isNotBlank(title))
        {
            addMetadataField(highlightedResults, "dc.title", collectionMetadata.addList(collection.getHandle() + ":dc.title"), title);
        }
    }

    /**
     * Render the given collection, all collection metadata is added to the list
     * @param community the community to be rendered
     * @param highlightedResults the highlighted results
     * @param communityMetadata list of metadata values.
     * @throws WingException passed through.
     */

    protected void renderCommunity(Community community,
            DiscoverResult.DSpaceObjectHighlightResult highlightedResults,
            org.dspace.app.xmlui.wing.element.List communityMetadata)
            throws WingException {
        String description = communityService.getMetadata(community, "introductory_text");
        String description_abstract = communityService.getMetadata(community, "short_description");
        String description_table = communityService.getMetadata(community, "side_bar_text");
        String identifier_uri = handleService.getCanonicalPrefix() + community.getHandle();
        String rights = communityService.getMetadata(community, "copyright_text");
        String title = community.getName();

        if(StringUtils.isNotBlank(description))
        {
            addMetadataField(highlightedResults, "dc.description",
                    communityMetadata.addList(community.getHandle() + ":dc.description"),
                    description);
        }
        if(StringUtils.isNotBlank(description_abstract))
        {
            addMetadataField(highlightedResults, "dc.description.abstract",
                    communityMetadata.addList(community.getHandle() + ":dc.description.abstract"),
                    description_abstract);
        }
        if(StringUtils.isNotBlank(description_table))
        {
            addMetadataField(highlightedResults, "dc.description.tableofcontents",
                    communityMetadata.addList(community.getHandle() + ":dc.description.tableofcontents"),
                    description_table);
        }
        if(StringUtils.isNotBlank(identifier_uri))
        {
            addMetadataField(highlightedResults, "dc.identifier.uri",
                    communityMetadata.addList(community.getHandle() + ":dc.identifier.uri"),
                    identifier_uri);
        }
        if(StringUtils.isNotBlank(rights))
        {
            addMetadataField(highlightedResults, "dc.rights",
                    communityMetadata.addList(community.getHandle() + ":dc.rights"),
                    rights);
        }
        if(StringUtils.isNotBlank(title))
        {
            addMetadataField(highlightedResults, "dc.title",
                    communityMetadata.addList(community.getHandle() + ":dc.title"),
                    title);
        }
    }

    /**
     * Add the current value to the wing list
     * @param highlightedResults the highlighted results
     * @param metadataKey the metadata key {schema}.{element}.{qualifier}
     * @param metadataFieldList the wing list we need to add the metadata value to
     * @param value the metadata value
     * @throws WingException passed through.
     */
    protected void addMetadataField(
            DiscoverResult.DSpaceObjectHighlightResult highlightedResults,
            String metadataKey,
            org.dspace.app.xmlui.wing.element.List metadataFieldList,
            String value)
            throws WingException {
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
     * Add our metadata value.  This value will might contain the highlight
     * ("{@code <em></em>}") tags.  These will be removed and rendered as highlight WING fields.
     *
     * @param metadataFieldList the metadata list we need to add the value to.
     * @param value the metadata value to be rendered
     * @throws WingException passed through.
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
     * <p>
     * The scope list will depend upon the current search scope. There are three
     * cases:
     * <ul>
     *  <li>No current scope: All top level communities are listed.
     *  <li>The current scope is a community: All collections contained within the
     *      community are listed.
     *  <li>The current scope is a collection: All parent communities are listed.
     * </ul>
     *
     * @param scope The current scope field.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @param scope the dspace object parent
     * @param query the query.
     * @param fqs the filter queries.
     * @return the prepared query.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws org.dspace.discovery.SearchServiceException passed through.
     */
    public DiscoverQuery prepareQuery(DSpaceObject scope, String query, String[] fqs)
            throws UIException, SearchServiceException {

    	this.queryArgs = new DiscoverQuery();
    	
    	int page = getParameterPage();
    	    	
    	// Escape any special characters in this user-entered query
        query = DiscoveryUIUtils.escapeQueryChars(query);

    	List<String> filterQueries = new ArrayList<>();

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
     * @param scope the dspace object parent
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws org.dspace.discovery.SearchServiceException passed through.
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
            Map<String, String[]> result = new HashMap<>();
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
     * <p>
     * The search scope when performed by URL, i.e. they are at the URL handle/xxxx/xx/search
     * then it is fixed. However at the global level the search is variable.
     *
     * @return true if the scope is variable, false otherwise.
     * @throws java.sql.SQLException passed through.
     */
    protected boolean variableScope() throws SQLException {
        return (HandleUtil.obtainHandle(objectModel) == null);
    }

    /**
     * Extract the query string. Under most implementations this will be derived
     * from the URL parameters.
     *
     * @return The query string.
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     */
    protected abstract String getQuery() throws UIException;

    /**
     * Generate a URL to the given search implementation with the associated
     * parameters included.
     *
     * @param parameters URL query parameters.
     * @return The post URL
     * @throws org.dspace.app.xmlui.utils.UIException whenever.
     */
    protected abstract String generateURL(Map<String, String> parameters)
            throws UIException;

    @Override
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
     * @throws java.sql.SQLException passed through.
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

