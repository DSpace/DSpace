/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.search;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;

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
import org.dspace.app.util.OpenSearch;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.app.util.Util;
import org.dspace.app.webui.servlet.SimpleSearchServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.dspace.sort.SortOption;
import org.dspace.usage.UsageEvent;
import org.dspace.usage.UsageSearchEvent;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;

public class LuceneSearchRequestProcessor implements SearchRequestProcessor
{
    private static final int ITEMMAP_RESULT_PAGE_SIZE = 50;

    /** log4j category */
    private static Logger log = Logger.getLogger(SimpleSearchServlet.class);
    
    // locale-sensitive metadata labels
    private Map<String, Map<String, String>> localeLabels = null;
    
    private static String msgKey = "org.dspace.app.webui.servlet.FeedServlet";
    
    private List<String> searchIndices = null;
    
    public synchronized void init()
    {
        if (localeLabels == null)
        {
            localeLabels = new HashMap<String, Map<String, String>>();
        }
        
        if (searchIndices == null)
        {
            searchIndices = new ArrayList<String>();
            String definition;
           
            int idx = 1;
            
            while ( ((definition = ConfigurationManager.getProperty("jspui.search.index.display." + idx))) != null){
                String index = definition;
                searchIndices.add(index);
                idx++;
             }
            
            // backward compatibility
            if (searchIndices.size() == 0)
            {
                searchIndices.add("ANY");
                searchIndices.add("author");
                searchIndices.add("title");
                searchIndices.add("keyword");
                searchIndices.add("abstract");
                searchIndices.add("series");
                searchIndices.add("sponsor");
                searchIndices.add("identifier");
                searchIndices.add("language");
            }
        }
    }
    
    /**
     * <p>
     * All metadata is search for the value contained in the "query" parameter.
     * If the "location" parameter is present, the user's location is switched
     * to that location using a redirect. Otherwise, the user's current location
     * is used to constrain the query; i.e., if the user is "in" a collection,
     * only results from the collection will be returned.
     * <p>
     * The value of the "location" parameter should be ALL (which means no
     * location), a the ID of a community (e.g. "123"), or a community ID, then
     * a slash, then a collection ID, e.g. "123/456".
     * 
     * author: Robert Tansley
     */
    @Override
    public void doSimpleSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException, IOException, ServletException
    {
        try
        {
            // Get the query
            String query = request.getParameter("query");
            int start = UIUtil.getIntParameter(request, "start");
            String advanced = request.getParameter("advanced");
            String fromAdvanced = request.getParameter("from_advanced");
            int sortBy = UIUtil.getIntParameter(request, "sort_by");
            String order = request.getParameter("order");
            int rpp = UIUtil.getIntParameter(request, "rpp");
            String advancedQuery = "";

            // can't start earlier than 0 in the results!
            if (start < 0)
            {
                start = 0;
            }

            int collCount = 0;
            int commCount = 0;
            int itemCount = 0;

            Item[] resultsItems;
            Collection[] resultsCollections;
            Community[] resultsCommunities;

            QueryResults qResults = null;
            QueryArgs qArgs = new QueryArgs();
            SortOption sortOption = null;

            if (request.getParameter("etal") != null)
            {
                qArgs.setEtAl(UIUtil.getIntParameter(request, "etal"));
            }

            try
            {
                if (sortBy > 0)
                {
                    sortOption = SortOption.getSortOption(sortBy);
                    qArgs.setSortOption(sortOption);
                }

                if (SortOption.ASCENDING.equalsIgnoreCase(order))
                {
                    qArgs.setSortOrder(SortOption.ASCENDING);
                }
                else
                {
                    qArgs.setSortOrder(SortOption.DESCENDING);
                }
            }
            catch (Exception e)
            {
            }

            // Override the page setting if exporting metadata
            if ("submit_export_metadata".equals(UIUtil.getSubmitButton(request, "submit")))
            {
                qArgs.setPageSize(Integer.MAX_VALUE);
            }
            else if (rpp > 0)
            {
                qArgs.setPageSize(rpp);
            }
            
            // if the "advanced" flag is set, build the query string from the
            // multiple query fields
            if (advanced != null)
            {
                query = qArgs.buildQuery(request);
                advancedQuery = qArgs.buildHTTPQuery(request);
            }

            // Ensure the query is non-null
            if (query == null)
            {
                query = "";
            }

            // Get the location parameter, if any
            String location = request.getParameter("location");

            // If there is a location parameter, we should redirect to
            // do the search with the correct location.
            if ((location != null) && !location.equals(""))
            {
                String url = "";

                if (!location.equals("/"))
                {
                    // Location is a Handle
                    url = "/handle/" + location;
                }

                // Encode the query
                query = URLEncoder.encode(query, Constants.DEFAULT_ENCODING);

                if (advancedQuery.length() > 0)
                {
                    query = query + "&from_advanced=true&" + advancedQuery;
                }

                // Do the redirect
                response.sendRedirect(response.encodeRedirectURL(request
                        .getContextPath()
                        + url + "/simple-search?query=" + query));

                return;
            }

            // Build log information
            String logInfo = "";

            // Get our location
            Community community = UIUtil.getCommunityLocation(request);
            Collection collection = UIUtil.getCollectionLocation(request);

            // get the start of the query results page
            //        List resultObjects = null;
            qArgs.setQuery(query);
            qArgs.setStart(start);

            // Perform the search
            if (collection != null)
            {
                logInfo = "collection_id=" + collection.getID() + ",";

                // Values for drop-down box
                request.setAttribute("community", community);
                request.setAttribute("collection", collection);

                qResults = DSQuery.doQuery(context, qArgs, collection);
            }
            else if (community != null)
            {
                logInfo = "community_id=" + community.getID() + ",";

                request.setAttribute("community", community);

                // Get the collections within the community for the dropdown box
                request
                        .setAttribute("collection.array", community
                                .getCollections());

                qResults = DSQuery.doQuery(context, qArgs, community);
            }
            else
            {
                // Get all communities for dropdown box
                Community[] communities = Community.findAll(context);
                request.setAttribute("community.array", communities);

                qResults = DSQuery.doQuery(context, qArgs);
            }

            // now instantiate the results and put them in their buckets
            for (int i = 0; i < qResults.getHitTypes().size(); i++)
            {
                Integer myType = qResults.getHitTypes().get(i);

                // add the handle to the appropriate lists
                switch (myType.intValue())
                {
                case Constants.ITEM:
                    itemCount++;
                    break;

                case Constants.COLLECTION:
                    collCount++;
                    break;

                case Constants.COMMUNITY:
                    commCount++;
                    break;
                }
            }

            // Make objects from the handles - make arrays, fill them out
            resultsCommunities = new Community[commCount];
            resultsCollections = new Collection[collCount];
            resultsItems = new Item[itemCount];

            collCount = 0;
            commCount = 0;
            itemCount = 0;

            for (int i = 0; i < qResults.getHitTypes().size(); i++)
            {
                Integer myId    = qResults.getHitIds().get(i);
                String myHandle = qResults.getHitHandles().get(i);
                Integer myType  = qResults.getHitTypes().get(i);

                // add the handle to the appropriate lists
                switch (myType.intValue())
                {
                case Constants.ITEM:
                    if (myId != null)
                    {
                        resultsItems[itemCount] = Item.find(context, myId);
                    }
                    else
                    {
                        resultsItems[itemCount] = (Item)HandleManager.resolveToObject(context, myHandle);
                    }

                    if (resultsItems[itemCount] == null)
                    {
                        throw new SQLException("Query \"" + query
                                + "\" returned unresolvable item");
                    }
                    itemCount++;
                    break;

                case Constants.COLLECTION:
                    if (myId != null)
                    {
                        resultsCollections[collCount] = Collection.find(context, myId);
                    }
                    else
                    {
                        resultsCollections[collCount] = (Collection)HandleManager.resolveToObject(context, myHandle);
                    }

                    if (resultsCollections[collCount] == null)
                    {
                        throw new SQLException("Query \"" + query
                                + "\" returned unresolvable collection");
                    }

                    collCount++;
                    break;

                case Constants.COMMUNITY:
                    if (myId != null)
                    {
                        resultsCommunities[commCount] = Community.find(context, myId);
                    }
                    else
                    {
                        resultsCommunities[commCount] = (Community)HandleManager.resolveToObject(context, myHandle);
                    }

                    if (resultsCommunities[commCount] == null)
                    {
                        throw new SQLException("Query \"" + query
                                + "\" returned unresolvable community");
                    }

                    commCount++;
                    break;
                }
            }

            // Log
            log.info(LogManager.getHeader(context, "search", logInfo + "query=\""
                    + query + "\",results=(" + resultsCommunities.length + ","
                    + resultsCollections.length + "," + resultsItems.length + ")"));

            // Pass in some page qualities
            // total number of pages
            int pageTotal = 1 + ((qResults.getHitCount() - 1) / qResults
                    .getPageSize());

            // current page being displayed
            int pageCurrent = 1 + (qResults.getStart() / qResults.getPageSize());

            // pageLast = min(pageCurrent+9,pageTotal)
            int pageLast = ((pageCurrent + 9) > pageTotal) ? pageTotal
                    : (pageCurrent + 9);

            // pageFirst = max(1,pageCurrent-9)
            int pageFirst = ((pageCurrent - 9) > 1) ? (pageCurrent - 9) : 1;


            //Fire an event to log our search
            DSpaceObject scope = null;
            if(collection != null){
                scope = collection;
            }else
            if(community != null){
                scope = community;
            }
            logSearch(context, request, query, pageCurrent, scope);


            // Pass the results to the display JSP
            request.setAttribute("items", resultsItems);
            request.setAttribute("communities", resultsCommunities);
            request.setAttribute("collections", resultsCollections);

            request.setAttribute("pagetotal", Integer.valueOf(pageTotal));
            request.setAttribute("pagecurrent", Integer.valueOf(pageCurrent));
            request.setAttribute("pagelast", Integer.valueOf(pageLast));
            request.setAttribute("pagefirst", Integer.valueOf(pageFirst));

            request.setAttribute("queryresults", qResults);

            // And the original query string
            request.setAttribute("query", query);

            request.setAttribute("order",  qArgs.getSortOrder());
            request.setAttribute("sortedBy", sortOption);

            if (AuthorizeManager.isAdmin(context))
            {
                // Set a variable to create admin buttons
                request.setAttribute("admin_button", Boolean.TRUE);
            }
            
            if ((fromAdvanced != null) && (qResults.getHitCount() == 0))
            {
                // send back to advanced form if no results
                Community[] communities = Community.findAll(context);
                request.setAttribute("communities", communities);
                request.setAttribute("no_results", "yes");

                Map<String, String> queryHash = qArgs.buildQueryMap(request);

                if (queryHash != null)
                {
                    for (Map.Entry<String, String> entry : queryHash.entrySet())
                    {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                }

                JSPManager.showJSP(request, response, "/search/advanced.jsp");
            }
            else if ("submit_export_metadata".equals(UIUtil.getSubmitButton(request, "submit")))
            {
                exportMetadata(context, response, resultsItems);
            }
            else
            {
                JSPManager.showJSP(request, response, "/search/results.jsp");
            }
        }
        catch (IllegalStateException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }
        catch (SQLException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }
    }

    protected void logSearch(Context context, HttpServletRequest request, String query, int start, DSpaceObject scope) {
        UsageSearchEvent searchEvent = new UsageSearchEvent(
                UsageEvent.Action.SEARCH,
                request,
                context,
                null, Arrays.asList(query), scope);


        if(!StringUtils.isBlank(request.getParameter("rpp"))){
            searchEvent.setRpp(Integer.parseInt(request.getParameter("rpp")));
        }
        if(!StringUtils.isBlank(request.getParameter("sort_by"))){
            searchEvent.setSortBy(request.getParameter("sort_by"));
        }
        if(!StringUtils.isBlank(request.getParameter("order"))){
            searchEvent.setSortOrder(request.getParameter("order"));
        }
        if(!StringUtils.isBlank(request.getParameter("start"))){
            searchEvent.setPage(start);
        }

        //Fire our event
        new DSpace().getEventService().fireEvent(searchEvent);
    }


    /**
     * Method for constructing the advanced search form
     * 
     * author: gam
     */
    @Override
    public void doAdvancedSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException, ServletException, IOException
    {
        // just build a list of top-level communities and pass along to the jsp
        Community[] communities;
        try
        {
            communities = Community.findAllTop(context);
        }
        catch (SQLException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }

        request.setAttribute("communities", communities);

        JSPManager.showJSP(request, response, "/search/advanced.jsp");
    }

    /**
     * Method for producing OpenSearch-compliant search results, and the
     * OpenSearch description document.
     * <p>
     * The value of the "scope" parameter should be absent (which means no scope
     * restriction), or the handle of a community or collection, otherwise
     * parameters exactly match those of the SearchServlet.
     * </p>
     * 
     * author: Richard Rodgers
     */
    @Override
    public void doOpenSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException, IOException, ServletException
    {
        init();
        
        // dispense with simple service document requests
        String scope = request.getParameter("scope");
        if (scope !=null && "".equals(scope))
        {
                scope = null;
        }
        String path = request.getPathInfo();
        if (path != null && path.endsWith("description.xml"))
        {
                String svcDescrip = OpenSearch.getDescription(scope);
                response.setContentType(OpenSearch.getContentType("opensearchdescription"));
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
        if (! OpenSearch.getFormats().contains(format))
        {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
        }
        
        // then the rest - we are processing the query
        String query = request.getParameter("query");
        int start = Util.getIntParameter(request, "start");
        int rpp = Util.getIntParameter(request, "rpp");
        int sort = Util.getIntParameter(request, "sort_by");
        String order = request.getParameter("order");
        String sortOrder = (order == null || order.length() == 0 || order.toLowerCase().startsWith("asc")) ?
                         SortOption.ASCENDING : SortOption.DESCENDING;
        
        QueryArgs qArgs = new QueryArgs();
        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }
        qArgs.setStart(start);
        
        if (rpp > 0)
        {
            qArgs.setPageSize(rpp);
        }
        qArgs.setSortOrder(sortOrder);
        
        if (sort > 0)
        {
                try
                {
                        qArgs.setSortOption(SortOption.getSortOption(sort));
                }
                catch(Exception e)
                {
                        // invalid sort id - do nothing
                }
        }
        qArgs.setSortOrder(sortOrder);

        // Ensure the query is non-null
        if (query == null)
        {
            query = "";
        }

        // If there is a scope parameter, attempt to dereference it
        // failure will only result in its being ignored
        DSpaceObject container;
        try
        {
            container = (scope != null) ? HandleManager.resolveToObject(context, scope) : null;
        }
        catch (IllegalStateException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }
        catch (SQLException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }

        // Build log information
        String logInfo = "";

        // get the start of the query results page
        qArgs.setQuery(query);

        // Perform the search
        QueryResults qResults = null;
        if (container == null)
        {
                qResults = DSQuery.doQuery(context, qArgs);
        }
        else if (container instanceof Collection)
        {
            logInfo = "collection_id=" + container.getID() + ",";
            qResults = DSQuery.doQuery(context, qArgs, (Collection)container);
        }
        else if (container instanceof Community)
        {
            logInfo = "community_id=" + container.getID() + ",";
            qResults = DSQuery.doQuery(context, qArgs, (Community)container);
        }
        else
        {
            throw new IllegalStateException("Invalid container for search context");
        }
        
        // now instantiate the results
        DSpaceObject[] results = new DSpaceObject[qResults.getHitHandles().size()];
        for (int i = 0; i < qResults.getHitHandles().size(); i++)
        {
            String myHandle = qResults.getHitHandles().get(i);
            DSpaceObject dso;
            try
            {
                dso = HandleManager.resolveToObject(context, myHandle);
            }
            catch (IllegalStateException e)
            {
                throw new SearchProcessorException(e.getMessage(), e);
            }
            catch (SQLException e)
            {
                throw new SearchProcessorException(e.getMessage(), e);
            }
            if (dso == null)
            {
                throw new SearchProcessorException("Query \"" + query
                        + "\" returned unresolvable handle: " + myHandle);
            }
            results[i] = dso;
        }

        // Log
        log.info(LogManager.getHeader(context, "search", logInfo + "query=\"" + query + "\",results=(" + results.length + ")"));

        // format and return results
        Map<String, String> labelMap = getLabels(request);
        Document resultsDoc = OpenSearch.getResultsDoc(format, query,
                qResults.getHitCount(), qResults.getStart(),
                qResults.getPageSize(), container, results, labelMap);
        try
        {
            Transformer xf = TransformerFactory.newInstance().newTransformer();
            response.setContentType(OpenSearch.getContentType(format));
            xf.transform(new DOMSource(resultsDoc), new StreamResult(response.getWriter()));
        }
        catch (TransformerException e)
        {
            log.error(e);
            throw new ServletException(e.toString(), e);
        }
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
        String query = (String) request.getParameter("query");
        int page = UIUtil.getIntParameter(request, "page")-1;
        int offset = page > 0? page * ITEMMAP_RESULT_PAGE_SIZE:0;
        Collection collection = (Collection) request.getAttribute("collection");
        String idx = (String) request.getParameter("index");
        if (StringUtils.isNotBlank(idx) && !idx.equalsIgnoreCase("any"))
        {
            query = idx + ":(" + query + ")";
        }
        QueryArgs queryArgs = new QueryArgs();
        queryArgs.setQuery(query + " -location:l" + collection.getID());
        queryArgs.setPageSize(ITEMMAP_RESULT_PAGE_SIZE);
        queryArgs.setStart(offset);
        QueryResults results = DSQuery.doQuery(context, queryArgs);

        Map<Integer, Item> items = new HashMap<Integer, Item>();
        List<String> handles = results.getHitHandles();
        try
        {
            for (String handle : handles)
            {
                DSpaceObject resultDSO = HandleManager.resolveToObject(context, handle);
    
                if (resultDSO.getType() == Constants.ITEM)
                {
                    Item item = (Item) resultDSO;
                    if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ))
                    {
                        items.put(Integer.valueOf(item.getID()), item);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new SearchProcessorException(e.getMessage(), e);
        }

        request.setAttribute("browsetext", query);
        request.setAttribute("items", items);
        request.setAttribute("more", results.getHitCount() > offset + ITEMMAP_RESULT_PAGE_SIZE);
        request.setAttribute("browsetype", "Add");
        request.setAttribute("page", page > 0 ? page + 1 : 1);
        
        JSPManager.showJSP(request, response, "itemmap-browse.jsp");
    }

    /**
     * Export the search results as a csv file
     *
     * @param context The DSpace context
     * @param response The request object
     * @param items The result items
     * @throws IOException
     * @throws ServletException
     */
    protected void exportMetadata(Context context, HttpServletResponse response, Item[] items)
            throws IOException, ServletException
    {
        // Log the attempt
        log.info(LogManager.getHeader(context, "metadataexport", "exporting_search"));

        // Export a search view
        List<Integer> iids = new ArrayList<Integer>();
        for (Item item : items)
        {
            iids.add(item.getID());
        }
        ItemIterator ii = new ItemIterator(context, iids);
        MetadataExport exporter = new MetadataExport(context, ii, false);

        // Perform the export
        DSpaceCSV csv = exporter.export();

        // Return the csv file
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=search-results.csv");
        PrintWriter out = response.getWriter();
        out.write(csv.toString());
        out.flush();
        out.close();
        log.info(LogManager.getHeader(context, "metadataexport", "exported_file:search-results.csv"));
        return;
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
    
    @Override
    public String getI18NKeyPrefix()
    {
        return "jsp.search.advanced.type.";
    }
    
    @Override
    public List<String> getSearchIndices()
    {
        init();
        return searchIndices;
    }
}
