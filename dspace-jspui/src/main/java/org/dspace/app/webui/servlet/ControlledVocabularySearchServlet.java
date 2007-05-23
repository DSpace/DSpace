/*
 * ConceptSearchServlet.java
 * 
 * Version: $Revision$
 * 
 * Date: $Date$
 * 
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts Institute of
 * Technology. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: -
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of the Hewlett-Packard Company nor
 * the name of the Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;

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
    private static Logger log = Logger
            .getLogger(ControlledVocabularySearchServlet.class);

    // the jsp that displays the HTML version of controlled-vocabulary
    private static final String SEARCH_JSP = "/controlledvocabulary/search.jsp";

    // the jsp that will show the search results
    private static final String RESULTS_JSP = "/controlledvocabulary/results.jsp";

    /**
     * Handles requests
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        String action = request.getParameter("action") == null ? "" : request
                .getParameter("action");

        if (action.equals("search"))
        {
            List keywords = extractKeywords(request);
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
    private List extractKeywords(HttpServletRequest request)
    {
        List keywords = new Vector();
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
        String advanced = request.getParameter("advanced");
        String fromAdvanced = request.getParameter("from_advanced");
        String advancedQuery = "";
        HashMap queryHash = new HashMap();

        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }

        List itemHandles = new ArrayList();
        List collectionHandles = new ArrayList();
        List communityHandles = new ArrayList();

        Item[] resultsItems;
        Collection[] resultsCollections;
        Community[] resultsCommunities;

        QueryResults qResults = null;
        QueryArgs qArgs = new QueryArgs();

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
        String newURL;

        // Build log information
        String logInfo = "";

        // Get our location
        Community community = UIUtil.getCommunityLocation(request);
        Collection collection = UIUtil.getCollectionLocation(request);

        // get the start of the query results page
        // List resultObjects = null;
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
        for (int i = 0; i < qResults.getHitHandles().size(); i++)
        {
            String myHandle = (String) qResults.getHitHandles().get(i);
            Integer myType = (Integer) qResults.getHitTypes().get(i);

            // add the handle to the appropriate lists
            switch (myType.intValue())
            {
            case Constants.ITEM:
                itemHandles.add(myHandle);

                break;

            case Constants.COLLECTION:
                collectionHandles.add(myHandle);

                break;

            case Constants.COMMUNITY:
                communityHandles.add(myHandle);

                break;
            }
        }

        int numCommunities = communityHandles.size();
        int numCollections = collectionHandles.size();
        int numItems = itemHandles.size();

        // Make objects from the handles - make arrays, fill them out
        resultsCommunities = new Community[numCommunities];
        resultsCollections = new Collection[numCollections];
        resultsItems = new Item[numItems];

        for (int i = 0; i < numItems; i++)
        {
            String myhandle = (String) itemHandles.get(i);

            Object o = HandleManager.resolveToObject(context, myhandle);

            resultsItems[i] = (Item) o;

            if (resultsItems[i] == null)
            {
                throw new SQLException("Query \"" + query
                        + "\" returned unresolvable handle: " + myhandle);
            }
        }

        for (int i = 0; i < collectionHandles.size(); i++)
        {
            String myhandle = (String) collectionHandles.get(i);

            Object o = HandleManager.resolveToObject(context, myhandle);

            resultsCollections[i] = (Collection) o;

            if (resultsCollections[i] == null)
            {
                throw new SQLException("Query \"" + query
                        + "\" returned unresolvable handle: " + myhandle);
            }
        }

        for (int i = 0; i < communityHandles.size(); i++)
        {
            String myhandle = (String) communityHandles.get(i);

            Object o = HandleManager.resolveToObject(context, myhandle);

            resultsCommunities[i] = (Community) o;

            if (resultsCommunities[i] == null)
            {
                throw new SQLException("Query \"" + query
                        + "\" returned unresolvable handle: " + myhandle);
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

        // Pass the results to the display JSP
        request.setAttribute("items", resultsItems);
        request.setAttribute("communities", resultsCommunities);
        request.setAttribute("collections", resultsCollections);

        request.setAttribute("pagetotal", new Integer(pageTotal));
        request.setAttribute("pagecurrent", new Integer(pageCurrent));
        request.setAttribute("pagelast", new Integer(pageLast));
        request.setAttribute("pagefirst", new Integer(pageFirst));

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
    public static String join(List list, String separator)
    {
        String result = "";
        Iterator iterator = list.listIterator();
        while (iterator.hasNext())
        {
            result += iterator.next().toString();
            if (iterator.hasNext())
                result += separator;
        }
        return result;
    }

    /**
     * Handle posts
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

}
