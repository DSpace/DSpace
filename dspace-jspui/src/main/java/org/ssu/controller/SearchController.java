package org.ssu.controller;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.discovery.DiscoverUtility;
import org.dspace.app.webui.discovery.DiscoverySearchRequestProcessor;
import org.dspace.app.webui.search.SearchProcessorException;
import org.dspace.app.webui.search.SearchRequestProcessor;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.core.PluginConfigurationError;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.jooq.lambda.Seq;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.ItemService;
import org.ssu.service.PaginationProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/")
public class SearchController {
    private static final Logger log = Logger.getLogger(SearchController.class);
    @Resource
    private ItemService itemService;
    @Resource
    private PaginationProcessor paginationProcessor;
    private transient SearchRequestProcessor internalLogic;
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    @PostConstruct
    private void init() {
        try {
            internalLogic = (SearchRequestProcessor) CoreServiceFactory.getInstance().getPluginService()
                    .getSinglePlugin(SearchRequestProcessor.class);
        } catch (PluginConfigurationError e) {
            log.warn(
                    "SimpleSearchServlet not properly configurated, please configure the SearchRequestProcessor plugin",
                    e);
        }
        if (internalLogic == null) {   // Discovery is the default search provider since DSpace 4.0
            internalLogic = new DiscoverySearchRequestProcessor();
        }
    }

    private <T> List<T> getQueryResultsByType(List<DSpaceObject> dspaceObjects, Class<T> targetType) {
        return dspaceObjects.stream()
                .filter(targetType::isInstance)
                .map(targetType::cast)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/simple-search")
    public ModelAndView simpleSearch(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException, SearchProcessorException, SearchServiceException, SortException {
        model = performSearchRequest(model, request, response);
        return model;
    }

    @RequestMapping(value = "/123456789/{itemId}/simple-search")
    public ModelAndView simpleSearchInCommunity(ModelAndView model, HttpServletRequest request, HttpServletResponse response, @PathVariable("itemId") String itemId) throws ServletException, IOException, SQLException, AuthorizeException, SearchProcessorException, SearchServiceException, SortException {
        model = performSearchRequest(model, request, response);
        model.addObject("handle", "/handle/123456789/" + itemId);
        return model;
    }

    private ModelAndView performSearchRequest(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException, SearchProcessorException, SearchServiceException, UnsupportedEncodingException, SortException {
        Context dspaceContext = UIUtil.obtainContext(request);
        DSpaceObject scope;
        try {
            scope = DiscoverUtility.getSearchScope(dspaceContext, request);
        } catch (IllegalStateException e) {
            throw new SearchProcessorException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new SearchProcessorException(e.getMessage(), e);
        }

        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration();
        DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(dspaceContext, request, scope, true);
        queryArgs.setSpellCheck(discoveryConfiguration.isSpellCheckEnabled());
        DiscoverResult qResults = SearchUtils.getSearchService().search(dspaceContext, scope, queryArgs);

        List<Item> resultsListItem = getQueryResultsByType(qResults.getDspaceObjects(), Item.class);


        Locale locale = dspaceContext.getCurrentLocale();
        List<ItemResponse> items = resultsListItem.stream()
                .map(item -> itemService.fetchItemresponseDataForItem(item, locale))
                .collect(Collectors.toList());

        List<String[]> appliedFilters = DiscoverUtility.getFilters(request);
        List<String> appliedFilterQueries = appliedFilters.stream()
                .map(filter -> String.format("%s::%s::%s", filter[0], filter[1], filter[2]))
                .collect(Collectors.toList());

        BiFunction<String[], Integer, String> joinHttpFilterParameters = (filter, index) ->
        {
            try {
                return String.format("&amp;filter_field_%d=%s&amp;filter_type_%d=%s&amp;filter_value_%d=%s", index, URLEncoder.encode(filter[0], "UTF-8"), index, URLEncoder.encode(filter[1], "UTF-8"), index, URLEncoder.encode(filter[2], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return "";
        };

        String httpFilters = Seq.zip(Seq.seq(appliedFilters), Seq.seq(IntStream.range(1, appliedFilters.size() + 1)))
                .filter(filter -> ArrayUtils.isNotEmpty(filter.v1))
                .filter(filter -> StringUtils.isNotEmpty(filter.v1[0]) && StringUtils.isNotEmpty(filter.v1[2]))
                .map(item -> joinHttpFilterParameters.apply(item.v1, item.v2))
                .collect(Collectors.joining(""));


        String query = request.getParameter("query");


        List<String> sortOptions = discoveryConfiguration.getSearchSortConfiguration().getSortFields().stream()
                .map(fieldConfiguration -> SearchUtils.getSearchService().toSortFieldIndex(fieldConfiguration.getMetadataField(), fieldConfiguration.getType()))
                .collect(Collectors.toList());

        List<DiscoverySearchFilterFacet> facets = Optional.ofNullable(qResults).map(results -> fetchEnabledFacets(discoveryConfiguration, appliedFilterQueries, qResults)).orElse(new ArrayList<>());
        Map<String, String> facetsCurrentPage = facets.stream().collect(Collectors.toMap(DiscoverySearchFilter::getIndexFieldName, facet -> Optional.ofNullable(request.getParameter(facet.getIndexFieldName() + "_page")).orElse("0")));
        Map<String, Integer> facetsLimit = facets.stream().collect(Collectors.toMap(DiscoverySearchFilter::getIndexFieldName, DiscoverySearchFilterFacet::getFacetLimit));

        model.addObject("facets", facets);
        model.addObject("facetsLimit", facetsLimit);
        model.addObject("facetCurrentPage", facetsCurrentPage);

        model = paginationProcessor.fillModelWithPaginationData(model, request, qResults);
        model.addObject("items", items);
        model.addObject("appliedFilters", appliedFilters);
        model.addObject("appliedFilterQueries", appliedFilterQueries);
        model.addObject("availableFilters", discoveryConfiguration.getSearchFilters());
        model.addObject("totalItems", qResults.getTotalSearchResults());
        model.addObject("startIndex", Math.max(qResults.getStart(), 1));
        model.addObject("finishIndex", Math.min(qResults.getStart() + qResults.getMaxResults(), qResults.getTotalSearchResults()));
        model.addObject("rpp", queryArgs.getMaxResults());
        model.addObject("httpFilters", httpFilters);
        model.addObject("order", queryArgs.getSortOrder().toString());
        model.addObject("sortOptions", sortOptions);
        model.addObject("queryresults", qResults);
        model.addObject("scope", scope);

        model.addObject("sortedBy", Optional.ofNullable(queryArgs.getSortField()).orElse(SortOption.getDefaultSortOption().getName()));
        model.addObject("queryEncoded", URLEncoder.encode(Optional.ofNullable(query).orElse(""), "UTF-8"));
        model.addObject("searchScope", scope != null ? scope.getHandle() : "");
        model.addObject("scopes", getScopes(scope, dspaceContext));
        request.setAttribute("dspace.context", dspaceContext);
        model.setViewName("search");
        return model;
    }


    private List<DiscoverySearchFilterFacet> fetchEnabledFacets(DiscoveryConfiguration discoveryConfiguration, List<String> appliedFilterQueries, DiscoverResult discoverResult) {
        List<DiscoverySearchFilterFacet> facetsConfiguration = Optional.ofNullable(discoveryConfiguration.getSidebarFacets()).orElse(new ArrayList<>());
        Predicate<String> isFacetMustBeShown = (facetName) -> discoverResult.getFacetResults().getOrDefault(facetName, discoverResult.getFacetResult(facetName + ".year")).stream()
                .map(currentFacet -> facetName + "::" + currentFacet.getFilterType() + "::" + currentFacet.getAsFilterQuery())
                .anyMatch(facetDescription -> !appliedFilterQueries.contains(facetDescription));

        return facetsConfiguration.stream()
                .filter(facet -> isFacetMustBeShown.test(facet.getIndexFieldName()))
                .collect(Collectors.toList());
    }

    private List<DSpaceObject> getScopes(DSpaceObject scope, Context context) throws SearchProcessorException {
        List<DSpaceObject> scopes = new ArrayList<DSpaceObject>();
        if (scope == null) {
            List<Community> topCommunities;
            try {
                topCommunities = communityService.findAllTop(context);
            } catch (SQLException e) {
                throw new SearchProcessorException(e.getMessage(), e);
            }
            for (Community com : topCommunities) {
                scopes.add(com);
            }
        } else {
            try {
                DSpaceObject pDso = ContentServiceFactory.getInstance().getDSpaceObjectService(scope)
                        .getParentObject(context, scope);
                while (pDso != null) {
                    // add to the available scopes in reverse order
                    scopes.add(0, pDso);
                    pDso = ContentServiceFactory.getInstance().getDSpaceObjectService(pDso)
                            .getParentObject(context, pDso);
                }
                scopes.add(scope);
                if (scope instanceof Community) {
                    List<Community> comms = ((Community) scope).getSubcommunities();
                    for (Community com : comms) {
                        scopes.add(com);
                    }
                    List<Collection> colls = ((Community) scope).getCollections();
                    for (Collection col : colls) {
                        scopes.add(col);
                    }
                }
            } catch (SQLException e) {
                throw new SearchProcessorException(e.getMessage(), e);
            }
        }
        return scopes;
    }
}
