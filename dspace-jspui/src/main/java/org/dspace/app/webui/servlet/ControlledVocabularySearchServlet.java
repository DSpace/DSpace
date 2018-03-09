/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * Servlet that provides funcionality for searching the repository using a
 * controlled vocabulary as a basis for selecting the search keywords.
 * 
 * @author Miguel Ferreira
 * @version $Revision$
 */
public class ControlledVocabularySearchServlet extends DSpaceServlet
{
    // the log
    private static final Logger log = Logger
            .getLogger(ControlledVocabularySearchServlet.class);

    // the jsp that displays the HTML version of controlled-vocabulary
    private static final String SEARCH_JSP = "/controlledvocabulary/search.jsp";

    // the jsp that will show the search results
    private static final String RESULTS_JSP = "/controlledvocabulary/results.jsp";

    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
    
    private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
    
    /**
     * Handles requests
     */
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        String action = request.getParameter("action") == null ? "" : request
                .getParameter("action");

        if (action.equals("search"))
        {
            List<String> keywords = extractKeywords(request);
            String query = join(keywords, " or ");
            doSearch(context, request, query);
            JSPManager.showJSP(request, response, RESULTS_JSP);
        }
        else if (action.equals("filter"))
        {
            String filter = request.getParameter("filter");
            request.getSession().setAttribute("conceptsearch.filter", filter);
            JSPManager.showJSP(request, response, SEARCH_JSP);
        }
        else
        {
            JSPManager.showJSP(request, response, SEARCH_JSP);
        }
    }

    /**
     * Collects the selected terms from the HTML taxonomy displayed on the
     * search form
     * 
     * @param request
     *            The HttpServletRequest
     * @return A Vector with the selected terms from the taxonomy.
     */
    private List<String> extractKeywords(HttpServletRequest request)
    {
        List<String> keywords = new ArrayList<>();
        Enumeration enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements())
        {
            String element = (String) enumeration.nextElement();
            if (element.startsWith("cb_"))
            {
                keywords.add("\"" + request.getParameter(element) + "\"");
            }
        }
        return keywords;
    }

    /**
     * Searches the repository and and puts the results in the request object
     * 
     * @param context
     *            The DSpace context
     * @param request
     *            The request object
     * @param query
     *            The query expression
     * @throws IOException
     * @throws SQLException
     */
    private void doSearch(Context context, HttpServletRequest request,
            String query) throws IOException, SQLException
    {
        // Get the query
        // String query = request.getParameter("query");
        int start = UIUtil.getIntParameter(request, "start");

        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }

        DiscoverQuery qArgs = new DiscoverQuery();

        // Ensure the query is non-null
        if (query == null)
        {
            query = "";
        }
        
        // Build log information
        String logInfo = "";

        // Get our location
        Community community = UIUtil.getCommunityLocation(request);
        Collection collection = UIUtil.getCollectionLocation(request);

        // get the start of the query results page
        qArgs.setQuery(query);
        qArgs.setStart(start);
        
        // Perform the search
        DiscoverResult qResults = null;
        try
        {
            if (collection != null)
            {
                logInfo = "collection_id=" + collection.getID() + ",";

                request.setAttribute("community", community);
                request.setAttribute("collection", collection);

                qResults = SearchUtils.getSearchService().search(context, collection, qArgs);
            }
            else if (community != null)
            {
                logInfo = "community_id=" + community.getID() + ",";

                request.setAttribute("community", community);

                qResults = SearchUtils.getSearchService().search(context, community, qArgs);
            }
            else
            {
                qResults = SearchUtils.getSearchService().search(context, qArgs);
            }
        }
        catch(SearchServiceException e)
        {
            throw new IOException(e);
        }
        
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
        log.info(LogManager.getHeader(context, "search", logInfo + "query=\""
                + query + "\",results=(" + resultsListComm.size() + ","
                + resultsListColl.size() + "," + resultsListItem.size() + ")"));

        // Pass in some page qualities
        // total number of pages
        long pageTotal = 1 + ((qResults.getTotalSearchResults() - 1) / qResults
                .getMaxResults());

        // current page being displayed
        long pageCurrent = 1 + (qResults.getStart() / qResults
                .getMaxResults());

        // pageLast = min(pageCurrent+9,pageTotal)
        long pageLast = ((pageCurrent + 9) > pageTotal) ? pageTotal
                : (pageCurrent + 9);

        // pageFirst = max(1,pageCurrent-9)
        long pageFirst = ((pageCurrent - 9) > 1) ? (pageCurrent - 9) : 1;

        // Pass the results to the display JSP
        request.setAttribute("items", resultsListItem);
        request.setAttribute("communities", resultsListComm);
        request.setAttribute("collections", resultsListColl);

        request.setAttribute("pagetotal", pageTotal);
        request.setAttribute("pagecurrent", pageCurrent);
        request.setAttribute("pagelast", pageLast);
        request.setAttribute("pagefirst", pageFirst);

        request.setAttribute("queryresults", qResults);

        // And the original query string
        request.setAttribute("query", query);

    }

    /**
     * Joins each element present in a list with a separator
     * 
     * @param list
     *            The list of elements
     * @param separator
     *            The separator that will be used between each element
     * @return A string with all the elements concatened and separated by the
     *         provided connector
     */
    public static String join(List<String> list, String separator)
    {
        StringBuilder result = new StringBuilder();
        for (String entry : list)
        {
            if (result.length() > 0)
            {
                result.append(separator);
            }

            result.append(entry);
        }

        return result.toString();
    }

    /**
     * Handle posts
     */
    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

}
