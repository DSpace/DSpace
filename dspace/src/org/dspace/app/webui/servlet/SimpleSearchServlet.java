/*
 * SimpleSearchServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.webui.servlet;


import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.TokenMgrError;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;


/**
 * Servlet for handling a simple search.
 * <P>
 * All metadata is search for the value contained in the "query" parameter.
 * If the "location" parameter is present, the user's location is switched
 * to that location using a redirect.  Otherwise, the user's current
 * location is used to constrain the query; i.e., if the user is "in" a
 * collection, only results from the collection will be returned.
 * <P>
 * The value of the "location" parameter should be ALL (which means no location),
 * a the ID of a community (e.g. "123"), or a community ID, then a slash, then
 * a collection ID, e.g. "123/456".
 *
 * @author Robert Tansley
 * @version $Id$
 */
public class SimpleSearchServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SimpleSearchServlet.class);

	
    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Get the query
        String query = request.getParameter("query");

        // Get the location parameter, if any
        String location = request.getParameter("location");
        String newURL;

        // If there is a location parameter, we should redirect to
        // do the search with the correct location.
        if (location != null && !location.equals(""))
        {
            // Do the redirect
            response.sendRedirect(response.encodeRedirectURL(
                    request.getContextPath() + location +
                    "simple-search?query=" + query));

            return;
        }

        // For the results
        List handles;

        // Build log information
        String logInfo = "";

        // Get our location
        Community community = UIUtil.getCommunityLocation(request);
        Collection collection = UIUtil.getCollectionLocation(request);

        Item[] items;
//        String[] handles;

        // Perform the search
        try
        {
            if (collection != null)
            {
                logInfo = "collection_id=" + collection.getID() + ",";

                request.setAttribute("collection", collection);
                request.setAttribute("community", community);
                handles = DSQuery.getItemResults(DSQuery.doQuery(query, collection));
            }
            else if (community != null)
            {
                logInfo = "community_id=" + community.getID() + ",";
                request.setAttribute("community", community);

                // Get the collections within the community for the dropdown box
                request.setAttribute("collection.array",
                    community.getCollections());

                handles = DSQuery.getItemResults(DSQuery.doQuery(query, community));
            }
            else
            {
                // Get all communities for dropdown box
                Community[] communities = Community.findAll(context);
                request.setAttribute("community.array", communities);

                handles = DSQuery.getItemResults(DSQuery.doQuery(query));
            }


            // Make Item objects from the handles
            items = new Item[handles.size()];
//            ItemIDs = new String[itemHandles.size()];

            for (int i = 0; i < handles.size(); i++)
            {
                String myhandle = (String) handles.get(i);
                
                Object o = HandleManager.resolveToObject(context, myhandle);
                
                // for now all returned objects are items
                items[i] = (Item)o;

                if (items[i] == null)
                {
                    throw new SQLException("Query \"" + query +
                        "\" returned unresolvable handle: " + myhandle);
                }

//                handles[i] = HandleManager.findHandle(context, items[i]);
            }

            // Log
            log.info(LogManager.getHeader(context,
                "search",
                logInfo + "query=\"" + query + "\",results=" + items.length));

        }
        catch (ParseException pe)
        {
            /*
             * A parse exception means there were some weird characters in
             * the query we couldn't resolve.  We'll pretend the search went
             * OK but with no results for the user, but log the error, since
             * this shouldn't really happen.
             */
            log.warn(LogManager.getHeader(context,
                "search_exception",
                logInfo + "query=\"" + query + "\""),
                pe);

            // Empty results
            items   = new Item[0];
            handles = (List)new ArrayList();
        }
	catch (TokenMgrError tme)
	{
            // Similar to parse exception
            log.warn(LogManager.getHeader(context,
                "search_exception",
                logInfo + "query=\"" + query + "\""),
                tme);

            // Empty results
            items   = new Item[0];
            handles = (List)new ArrayList();
	}

    String [] handlesarray = new String[handles.size()];
    handlesarray = (String [])handles.toArray(handlesarray);

        // Pass the results to the display JSP
        request.setAttribute("items", items);
        request.setAttribute("handles", handlesarray);

        // And the original query string
        request.setAttribute("query", query);

        JSPManager.showJSP(request, response, "/search/results.jsp");
    }
}
