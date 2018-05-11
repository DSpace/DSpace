/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.discovery;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.OpenSearchService;
import org.dspace.app.webui.search.SearchProcessorException;
import org.dspace.app.webui.search.SearchRequestProcessor;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.w3c.dom.Document;

public class DiscoverySearchRequestProcessor implements SearchRequestProcessor
{
    private static final int ITEMMAP_RESULT_PAGE_SIZE = 50;

    private static String msgKey = "org.dspace.app.webui.servlet.FeedServlet";

    /** log4j category */
    private static Logger log = Logger.getLogger(DiscoverySearchRequestProcessor.class);

    // locale-sensitive metadata labels
    private Map<String, Map<String, String>> localeLabels = null;

    private List<String> searchIndices = null;
    
    private OpenSearchService openSearchService;
    
    private CommunityService communityService;
    
    private CollectionService collectionService;
    
    private AuthorizeService authorizeService;
    
    public synchronized void init()
    {
        if (localeLabels == null)
        {
            localeLabels = new HashMap<String, Map<String, String>>();
        }
        
        if (searchIndices == null)
        {
            searchIndices = new ArrayList<String>();
            DiscoveryConfiguration discoveryConfiguration = SearchUtils
                    .getDiscoveryConfiguration();
            searchIndices.add("any");
            for (DiscoverySearchFilter sFilter : discoveryConfiguration.getSearchFilters())
            {
                searchIndices.add(sFilter.getIndexFieldName());
            }
        }
        openSearchService = UtilServiceFactory.getInstance().getOpenSearchService();
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    }

    public void doOpenSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException,
            IOException, ServletException
    {
        init();
        
        // dispense with simple service document requests
        String scope = request.getParameter("scope");
        if (scope != null && "".equals(scope))
        {
            scope = null;
        }
        String path = request.getPathInfo();
        if (path != null && path.endsWith("description.xml"))
        {
            String svcDescrip = openSearchService.getDescription(scope);
            response.setContentType(openSearchService
                    .getContentType("opensearchdescription"));
            response.setContentLength(svcDescrip.length());
            response.getWriter().write(svcDescrip);
            return;
        }

        // get enough request parameters to decide on action to take
        String format = request.getParameter("format");
        if (format == null || "".equals(format))
        {
            // default to atom
            format = "atom";
        }

        // do some sanity checking
        if (!openSearchService.getFormats().contains(format))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // then the rest - we are processing the query
        DSpaceObject container;
        try
        {
            container = DiscoverUtility.getSearchScope(context,
                    request);
        }
        catch (Exception e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }
        DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(context,
                request, container, false);
        String query = queryArgs.getQuery();

        // Perform the search
        DiscoverResult qResults = null;
        try
        {
            qResults = SearchUtils.getSearchService().search(context,
                    container, queryArgs);
        }
        catch (SearchServiceException e)
        {
            log.error(
                    LogManager.getHeader(context, "opensearch", "query="
                            + queryArgs.getQuery() + ",scope=" + scope
                            + ",error=" + e.getMessage()), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        // Log
        log.info(LogManager.getHeader(context, "opensearch",
                "scope=" + scope + ",query=\"" + query + "\",results=("
                        + qResults.getTotalSearchResults() + ")"));

        // format and return results
        Map<String, String> labelMap = getLabels(request);
        List<DSpaceObject> dsoResults = qResults.getDspaceObjects();
        Document resultsDoc = openSearchService.getResultsDoc(context, format, query,
                (int) qResults.getTotalSearchResults(), qResults.getStart(),
                qResults.getMaxResults(), container, dsoResults, labelMap);
        try
        {
            Transformer xf = TransformerFactory.newInstance().newTransformer();
            response.setContentType(openSearchService.getContentType(format));
            xf.transform(new DOMSource(resultsDoc),
                    new StreamResult(response.getWriter()));
        }
        catch (TransformerException e)
        {
            log.error(e);
            throw new ServletException(e.toString());
        }
    }

    private Map<String, String> getLabels(HttpServletRequest request)
    {
        // Get access to the localized resource bundle
        Locale locale = UIUtil.getSessionLocale(request);
        Map<String, String> labelMap = localeLabels.get(locale.toString());
        if (labelMap == null)
        {
            labelMap = getLocaleLabels(locale);
            localeLabels.put(locale.toString(), labelMap);
        }
        return labelMap;
    }

    private Map<String, String> getLocaleLabels(Locale locale)
    {
        Map<String, String> labelMap = new HashMap<String, String>();
        labelMap.put(SyndicationFeed.MSG_UNTITLED, I18nUtil.getMessage(msgKey + ".notitle", locale));
        labelMap.put(SyndicationFeed.MSG_LOGO_TITLE, I18nUtil.getMessage(msgKey + ".logo.title", locale));
        labelMap.put(SyndicationFeed.MSG_FEED_DESCRIPTION, I18nUtil.getMessage(msgKey + ".general-feed.description", locale));
        labelMap.put(SyndicationFeed.MSG_UITYPE, SyndicationFeed.UITYPE_JSPUI);
        for (String selector : SyndicationFeed.getDescriptionSelectors())
        {
            labelMap.put("metadata." + selector, I18nUtil.getMessage(SyndicationFeed.MSG_METADATA + selector, locale));
        }
        return labelMap;
    }
    
    public void doSimpleSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException,
            IOException, ServletException
    {
        init();
        List<Item> resultsItems;
        List<Collection> resultsCollections;
        List<Community> resultsCommunities;
        DSpaceObject scope;
        try
        {
            scope = DiscoverUtility.getSearchScope(context, request);
        }
        catch (IllegalStateException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }
        catch (SQLException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }

        DiscoveryConfiguration discoveryConfiguration = SearchUtils
                .getDiscoveryConfiguration(scope);
        List<DiscoverySortFieldConfiguration> sortFields = discoveryConfiguration
                .getSearchSortConfiguration().getSortFields();
        List<String> sortOptions = new ArrayList<String>();
        for (DiscoverySortFieldConfiguration sortFieldConfiguration : sortFields)
        {
            String sortField = SearchUtils.getSearchService().toSortFieldIndex(
                    sortFieldConfiguration.getMetadataField(),
                    sortFieldConfiguration.getType());
            sortOptions.add(sortField);
        }
        request.setAttribute("sortOptions", sortOptions);
        
        DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(context,
                request, scope, true);

        queryArgs.setSpellCheck(discoveryConfiguration.isSpellCheckEnabled()); 
        
        List<DiscoverySearchFilterFacet> availableFacet = discoveryConfiguration
                .getSidebarFacets();
        
        request.setAttribute("facetsConfig",
                availableFacet != null ? availableFacet
                        : new ArrayList<DiscoverySearchFilterFacet>());
        int etal = UIUtil.getIntParameter(request, "etal");
        if (etal == -1)
        {
            etal = ConfigurationManager
                    .getIntProperty("webui.itemlist.author-limit");
        }

        request.setAttribute("etal", etal);

        String query = queryArgs.getQuery();
        request.setAttribute("query", query);
        request.setAttribute("queryArgs", queryArgs);
        List<DiscoverySearchFilter> availableFilters = discoveryConfiguration
                .getSearchFilters();
        request.setAttribute("availableFilters", availableFilters);

        List<String[]> appliedFilters = DiscoverUtility.getFilters(request);
        request.setAttribute("appliedFilters", appliedFilters);
        List<String> appliedFilterQueries = new ArrayList<String>();
        for (String[] filter : appliedFilters)
        {
            appliedFilterQueries.add(filter[0] + "::" + filter[1] + "::"
                    + filter[2]);
        }
        request.setAttribute("appliedFilterQueries", appliedFilterQueries);
        List<DSpaceObject> scopes = new ArrayList<DSpaceObject>();
        if (scope == null)
        {
            List<Community> topCommunities;
            try
            {
                topCommunities = communityService.findAllTop(context);
            }
            catch (SQLException e)
            {
                throw new SearchProcessorException(e.getMessage(), e);
            }
            for (Community com : topCommunities)
            {
                scopes.add(com);
            }
        }
        else
        {
            try
            {
				DSpaceObject pDso = ContentServiceFactory.getInstance().getDSpaceObjectService(scope)
						.getParentObject(context, scope);
                while (pDso != null)
                {
                    // add to the available scopes in reverse order
                    scopes.add(0, pDso);
                    pDso = ContentServiceFactory.getInstance().getDSpaceObjectService(pDso)
    						.getParentObject(context, pDso);
                }
                scopes.add(scope);
                if (scope instanceof Community)
                {
                    List<Community> comms = ((Community) scope).getSubcommunities();
                    for (Community com : comms)
                    {
                        scopes.add(com);
                    }
                    List<Collection> colls = ((Community) scope).getCollections();
                    for (Collection col : colls)
                    {
                        scopes.add(col);
                    }
                }
            }
            catch (SQLException e)
            {
                throw new SearchProcessorException(e.getMessage(), e);
            }
        }
        request.setAttribute("scope", scope);
        request.setAttribute("scopes", scopes);

        // Perform the search
        DiscoverResult qResults = null;
        try
        {
            qResults = SearchUtils.getSearchService().search(context, scope,
                    queryArgs);
            
            List<Community> resultsListComm = new ArrayList<Community>();
            List<Collection> resultsListColl = new ArrayList<Collection>();
            List<Item> resultsListItem = new ArrayList<Item>();

            for (DSpaceObject dso : qResults.getDspaceObjects())
            {
                if (dso instanceof Item)
                {
                    resultsListItem.add((Item) dso);
                }
                else if (dso instanceof Collection)
                {
                    resultsListColl.add((Collection) dso);

                }
                else if (dso instanceof Community)
                {
                    resultsListComm.add((Community) dso);
                }
            }

            // Log
            log.info(LogManager.getHeader(context, "search", "scope=" + scope
                    + ",query=\"" + query + "\",results=("
                    + resultsListComm.size() + ","
                    + resultsListColl.size() + "," + resultsListItem.size()
                    + ")"));

            // Pass in some page qualities
            // total number of pages
            long pageTotal = 1 + ((qResults.getTotalSearchResults() - 1) / qResults
                    .getMaxResults());

            // current page being displayed
            long pageCurrent = 1 + (qResults.getStart() / qResults
                    .getMaxResults());

            // pageLast = min(pageCurrent+3,pageTotal)
            long pageLast = ((pageCurrent + 3) > pageTotal) ? pageTotal
                    : (pageCurrent + 3);

            // pageFirst = max(1,pageCurrent-3)
            long pageFirst = ((pageCurrent - 3) > 1) ? (pageCurrent - 3) : 1;

            // Pass the results to the display JSP
            request.setAttribute("items", resultsListItem);
            request.setAttribute("communities", resultsListComm);
            request.setAttribute("collections", resultsListColl);

            request.setAttribute("pagetotal", new Long(pageTotal));
            request.setAttribute("pagecurrent", new Long(pageCurrent));
            request.setAttribute("pagelast", new Long(pageLast));
            request.setAttribute("pagefirst", new Long(pageFirst));
            request.setAttribute("spellcheck", qResults.getSpellCheckQuery());
            
            request.setAttribute("queryresults", qResults);

            try
            {
                if (authorizeService.isAdmin(context))
                {
                    // Set a variable to create admin buttons
                    request.setAttribute("admin_button", new Boolean(true));
                }
            }
            catch (SQLException e)
            {
                throw new SearchProcessorException(e.getMessage(), e);            }

            if ("submit_export_metadata".equals(UIUtil.getSubmitButton(request,
                    "submit")))
            {
                exportMetadata(context, response, resultsListItem);
            }
        }
        catch (SearchServiceException e)
        {
            log.error(
                    LogManager.getHeader(context, "search", "query="
                            + queryArgs.getQuery() + ",scope=" + scope
                            + ",error=" + e.getMessage()), e);
            request.setAttribute("search.error", true);
            request.setAttribute("search.error.message", e.getMessage());
        }

        JSPManager.showJSP(request, response, "/search/discovery.jsp");
    }

    /**
     * Export the search results as a csv file
     * 
     * @param context
     *            The DSpace context
     * @param response
     *            The request object
     * @param items
     *            The result items
     * @throws IOException
     * @throws ServletException
     */
    protected void exportMetadata(Context context,
            HttpServletResponse response, List<Item> items) throws IOException,
            ServletException
    {
        // Log the attempt
        log.info(LogManager.getHeader(context, "metadataexport",
                "exporting_search"));

        // Export a search view
        MetadataExport exporter = new MetadataExport(context, items.iterator(), false);

        // Perform the export
        DSpaceCSV csv = exporter.export();

        // Return the csv file
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=search-results.csv");
        PrintWriter out = response.getWriter();
        out.write(csv.toString());
        out.flush();
        out.close();
        log.info(LogManager.getHeader(context, "metadataexport",
                "exported_file:search-results.csv"));
        return;
    }

    /**
     * Method for constructing the discovery advanced search form
     * 
     * author: Andrea Bollini
     */
    @Override
    public void doAdvancedSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException,
            IOException, ServletException
            {
                // just redirect to the simple search servlet.
                // The advanced form is always displayed with Discovery togheter with
                // the search result
                // the first access to the advanced form performs a search for
                // "anythings" (SOLR *:*)
                response.sendRedirect(request.getContextPath() + "/simple-search");
            }

    /**
     * Method for searching authors in item map
     * 
     * author: gam
     */
    @Override
    public void doItemMapSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException, ServletException, IOException
    {
        init();
        String queryString = (String) request.getParameter("query");
        Collection collection = (Collection) request.getAttribute("collection");
        int page = UIUtil.getIntParameter(request, "page")-1;
        int offset = page > 0? page * ITEMMAP_RESULT_PAGE_SIZE:0;
        String idx = (String) request.getParameter("index");
        if (StringUtils.isNotBlank(idx) && !idx.equalsIgnoreCase("any"))
        {
            queryString = idx + ":(" + queryString + ")";
        }
        DiscoverQuery query = new DiscoverQuery();
        query.setQuery(queryString);
        query.addFilterQueries("-location:l"+collection.getID());
        query.setMaxResults(ITEMMAP_RESULT_PAGE_SIZE);
        query.setStart(offset);

        DiscoverResult results = null;
        try
        {
            results = SearchUtils.getSearchService().search(context, query);
        }
        catch (SearchServiceException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }

        Map<String, Item> items = new HashMap<String, Item>();

        List<DSpaceObject> resultDSOs = results.getDspaceObjects();
        for (DSpaceObject dso : resultDSOs)
        {
            if (dso != null && dso.getType() == Constants.ITEM)
            {
                // no authorization check is required as discovery is right aware
                Item item = (Item) dso;
                items.put(item.getID().toString(), item);
            }
        }

        request.setAttribute("browsetext", queryString);
        request.setAttribute("items", items);
        request.setAttribute("more", results.getTotalSearchResults() > offset + ITEMMAP_RESULT_PAGE_SIZE);
        request.setAttribute("browsetype", "Add");
        request.setAttribute("page", page > 0 ? page + 1 : 1);
        
        JSPManager.showJSP(request, response, "itemmap-browse.jsp");
    }
    
    @Override
    public String getI18NKeyPrefix()
    {
        return "jsp.search.filter.";
    }
    
    @Override
    public List<String> getSearchIndices()
    {
        init();
        return searchIndices;
    }
}
