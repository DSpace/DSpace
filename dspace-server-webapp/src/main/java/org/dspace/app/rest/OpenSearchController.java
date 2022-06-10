/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.OpenSearchService;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.core.Utils;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;

/**
 * This class provides a controller for OpenSearch support.
 * It creates a namespace /opensearch in the DSpace REST webapp.
 *
 * @author Oliver Goldschmidt (o.goldschmidt at tuhh.de)
 */
@Controller
@RequestMapping("/opensearch")
public class OpenSearchController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();
    private static final String errorpath = "/error";
    private List<String> searchIndices = null;

    private CommunityService communityService;
    private CollectionService collectionService;
    private AuthorizeService authorizeService;
    private OpenSearchService openSearchService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    private Context context;

    @Autowired
    private ScopeResolver scopeResolver;

    /**
     * This method provides the OpenSearch query on the path /search
     * It will pass the result as a OpenSearchDocument directly to the client
     */
    @GetMapping("/search")
    public void search(HttpServletRequest request,
                         HttpServletResponse response,
                         @RequestParam(name = "query", required = false) String query,
                         @RequestParam(name = "start", required = false) Integer start,
                         @RequestParam(name = "rpp", required = false) Integer count,
                         @RequestParam(name = "format", required = false) String format,
                         @RequestParam(name = "sort", required = false) String sort,
                         @RequestParam(name = "sort_direction", required = false) String sortDirection,
                         @RequestParam(name = "scope", required = false) String dsoObject,
                         Model model) throws IOException, ServletException {
        context = ContextUtil.obtainContext(request);
        if (start == null) {
            start = 0;
        }
        if (count == null) {
            count = -1;
        }
        if (openSearchService == null) {
            openSearchService = UtilServiceFactory.getInstance().getOpenSearchService();
        }
        if (openSearchService.isEnabled()) {
            init();
            // get enough request parameters to decide on action to take
            if (format == null || "".equals(format)) {
                // default to atom
                format = "atom";
            }

            log.debug("Searching for " + query + " in format " + format);

            // do some sanity checking
            if (!openSearchService.getFormats().contains(format)) {
                // Since we are returning error response as HTML, escape any HTML in "format" param
                String err = "Format " + Utils.addEntities(format) + " is not supported.";
                response.setContentType("text/html");
                response.setContentLength(err.length());
                response.getWriter().write(err);
            }

            // then the rest - we are processing the query
            IndexableObject container = null;

            // support pagination parameters
            DiscoverQuery queryArgs = new DiscoverQuery();
            if (query == null) {
                query = "";
            } else {
                queryArgs.setQuery(query);
            }
            queryArgs.setStart(start);
            queryArgs.setMaxResults(count);
            queryArgs.setDSpaceObjectFilter(IndexableItem.TYPE);
            if (sort != null) {
                //this is the default sort so we want to switch this to date accessioned
                if (sortDirection != null && sortDirection.equals("DESC")) {
                    queryArgs.setSortField(sort + "_sort", SORT_ORDER.desc);
                } else {
                    queryArgs.setSortField(sort + "_sort", SORT_ORDER.asc);
                }
            } else {
                queryArgs.setSortField("dc.date.accessioned_dt", SORT_ORDER.desc);
            }
            if (dsoObject != null) {
                container = scopeResolver.resolveScope(context, dsoObject);
                DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
                        .getDiscoveryConfigurationByNameOrDso("site", container);
                queryArgs.setDiscoveryConfigurationName(discoveryConfiguration.getId());
                queryArgs.addFilterQueries(discoveryConfiguration.getDefaultFilterQueries()
                        .toArray(
                                new String[discoveryConfiguration.getDefaultFilterQueries()
                                        .size()]));
            }

            // Perform the search
            DiscoverResult qResults = null;
            try {
                qResults = SearchUtils.getSearchService().search(context,
                    container, queryArgs);
            } catch (SearchServiceException e) {
                log.error(LogHelper.getHeader(context, "opensearch", "query="
                            + queryArgs.getQuery()
                            + ",error=" + e.getMessage()), e);
                throw new RuntimeException(e.getMessage(), e);
            }

            // Log
            log.info("opensearch done, query=\"" + query + "\",results="
                        + qResults.getTotalSearchResults());

            // format and return results
            Map<String, String> labelMap = getLabels(request);
            List<IndexableObject> dsoResults = qResults.getIndexableObjects();
            Document resultsDoc = openSearchService.getResultsDoc(context, format, query,
                (int) qResults.getTotalSearchResults(), qResults.getStart(),
                qResults.getMaxResults(), container, dsoResults, labelMap);
            try {
                Transformer xf = TransformerFactory.newInstance().newTransformer();
                response.setContentType(openSearchService.getContentType(format));
                xf.transform(new DOMSource(resultsDoc),
                    new StreamResult(response.getWriter()));
            } catch (TransformerException e) {
                log.error(e);
                throw new ServletException(e.toString());
            }
        } else {
            log.debug("OpenSearch Service is disabled");
            String err = "OpenSearch Service is disabled";
            response.setStatus(404);
            response.setContentType("text/html");
            response.setContentLength(err.length());
            response.getWriter().write(err);
        }
    }

    /**
     * This method provides the OpenSearch servicedescription document on the path /service
     * It will pass the result as a OpenSearchDocument directly to the client
     */
    @GetMapping("/service")
    public void service(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        log.debug("Show OpenSearch Service document");
        if (openSearchService == null) {
            openSearchService = UtilServiceFactory.getInstance().getOpenSearchService();
        }
        if (openSearchService.isEnabled()) {
            String svcDescrip = openSearchService.getDescription(null);
            log.debug("opensearchdescription is " + svcDescrip);
            response.setContentType(openSearchService
                .getContentType("opensearchdescription"));
            response.setContentLength(svcDescrip.length());
            response.getWriter().write(svcDescrip);
        } else {
            log.debug("OpenSearch Service is disabled");
            String err = "OpenSearch Service is disabled";
            response.setStatus(404);
            response.setContentType("text/html");
            response.setContentLength(err.length());
            response.getWriter().write(err);
        }
    }

    /**
     * Internal method for controller initialization
     */
    private void init() {
        if (searchIndices == null) {
            searchIndices = new ArrayList<String>();
            DiscoveryConfiguration discoveryConfiguration = SearchUtils
                    .getDiscoveryConfiguration();
            searchIndices.add("any");
            for (DiscoverySearchFilter sFilter : discoveryConfiguration.getSearchFilters()) {
                searchIndices.add(sFilter.getIndexFieldName());
            }
        }
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    }

    public void setOpenSearchService(OpenSearchService oSS) {
        openSearchService = oSS;
    }


    /**
     * Internal method to get labels for the returned document
     */
    private Map<String, String> getLabels(HttpServletRequest request) {
        // TODO: get strings from translation file or configuration
        Map<String, String> labelMap = new HashMap<String, String>();
        labelMap.put(SyndicationFeed.MSG_UNTITLED, "notitle");
        labelMap.put(SyndicationFeed.MSG_LOGO_TITLE, "logo.title");
        labelMap.put(SyndicationFeed.MSG_FEED_DESCRIPTION, "general-feed.description");
        labelMap.put(SyndicationFeed.MSG_UITYPE, SyndicationFeed.UITYPE_JSPUI);
        for (String selector : SyndicationFeed.getDescriptionSelectors()) {
            labelMap.put("metadata." + selector, selector);
        }
        return labelMap;
    }
}
