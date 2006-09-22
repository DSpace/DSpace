/*
 * BrowseServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.Browse;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseScope;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;

/**
 * Servlet for browsing through indices. This can be used to browse authors,
 * items by date, or items by title. In the deployment description, the initial
 * parameter "browse" should be set to one of these values:
 * <p>
 * <ul>
 * <lI><code>titles</code>- for browsing items by title (the default)</li>
 * <lI><code>authors</code>- for browsing authors</li>
 * <lI><code>dates</code>- for browsing items by date</li>
 * </ul>
 * <p>
 * Hence there should be three instances of this servlet, one for each type of
 * browse.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class BrowseServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BrowseServlet.class);

    /** Is this servlet for browsing authors? */
    private boolean browseAuthors;

    /** Is this servlet for browsing items by title? */
    private boolean browseTitles;

    /** Is this servlet for browsing items by date? */
    private boolean browseDates;

    /** Is this servlet for browsing items by subject? */
    private boolean browseSubjects;

    public void init()
    {
        // Sort out what we're browsing - default is titles
        String browseWhat = getInitParameter("browse");

        browseAuthors = ((browseWhat != null) && browseWhat
                .equalsIgnoreCase("authors"));
        browseDates = ((browseWhat != null) && browseWhat
                .equalsIgnoreCase("dates"));
        browseSubjects = ((browseWhat != null) && browseWhat
                .equalsIgnoreCase("subjects"));

        browseTitles = ((!browseAuthors && !browseDates)&& !browseSubjects );
    }

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // We will resolve the HTTP request parameters into a scope
        BrowseScope scope = new BrowseScope(context);

        // Will need to know whether to highlight the "focus" point
        boolean highlight = false;

        // Build up log information
        String logInfo = "";

        // For browse by date, we'll need to work out the URL query string to
        // use when the user swaps the ordering, so that they stay at the same
        // point in the index
        String flipOrderingQuery = "";

        // Grab HTTP request parameters
        String focus = request.getParameter("focus");
        String startsWith = request.getParameter("starts_with");
        String top = request.getParameter("top");
        String bottom = request.getParameter("bottom");

        // The following three are specific to browsing items by date
        String month = request.getParameter("month");
        String year = request.getParameter("year");
        String order = request.getParameter("order");

        // For browse by date: oldest item first?
        boolean oldestFirst = false;

        if ((order != null) && order.equalsIgnoreCase("oldestfirst"))
        {
            oldestFirst = true;
        }

        if (browseDates && (year != null) && !year.equals("")
                && ((startsWith == null) || startsWith.equals("")))
        {
            // We're browsing items by date, the user hasn't typed anything
            // into the "year" text box, and they've selected a year from
            // the drop-down list. From this we work out where to start
            // the browse.
            startsWith = year;

            if ((month != null) & !month.equals("-1"))
            {
                // They've selected a month as well
                if (month.length() == 1)
                {
                    // Ensure double-digit month number
                    month = "0" + month;
                }

                startsWith = year + "-" + month;
            }
        }

        // Set the scope according to the parameters passed in
        if (focus != null)
        {
            // ----------------------------------------------
            // Browse should start at a specified focus point
            // ----------------------------------------------
            if (browseAuthors||browseSubjects)
            {
                // For browsing authors, focus is just a text value
                scope.setFocus(focus);
            }
            else
            {
                // For browsing items by title or date, focus is a Handle
                Item item = (Item) HandleManager
                        .resolveToObject(context, focus);

                if (item == null)
                {
                    // Handle is invalid one. Show an error.
                    JSPManager.showInvalidIDError(request, response, focus,
                            Constants.ITEM);

                    return;
                }

                scope.setFocus(item);
            }

            // Will need to highlight the focus
            highlight = true;

            logInfo = "focus=" + focus + ",";

            if (browseDates)
            {
                // if the date order is flipped, we'll keep the same focus
                flipOrderingQuery = "focus="
                        + URLEncoder.encode(focus, Constants.DEFAULT_ENCODING)
                        + "&amp;";
            }
        }
        else if (startsWith != null)
        {
            // ----------------------------------------------
            // Start the browse using user-specified text
            // ----------------------------------------------
            if (browseDates)
            {
                // if the date order is flipped, we'll keep the same focus
                flipOrderingQuery = "starts_with="
                        + URLEncoder.encode(startsWith,
                                Constants.DEFAULT_ENCODING) + "&amp;";

                /*
                 * When the user is browsing with the most recent items first,
                 * the browse code algorithm doesn't quite do what some people
                 * might expect. For example, if in the index there are entries:
                 * 
                 * Mar-2000 15-Feb-2000 6-Feb-2000 15-Jan-2000
                 * 
                 * and the user has selected "Feb 2000" as the start point for
                 * the browse, the browse algorithm will start at the first
                 * point in that index *after* "Feb 2000". "Feb 2000" would
                 * appear in the index above between 6-Feb-2000 and 15-Jan-2000.
                 * So, the browse code in this case will start the browse at
                 * "15-Jan-2000". This isn't really what users are likely to
                 * want: They're more likely to want the browse to start at the
                 * first Feb 2000 date, i.e. 15-Feb-2000. A similar scenario
                 * occurs when the user enters just a year. Our quick hack to
                 * produce this behaviour is to add "-32" to the startsWith
                 * variable, when sorting with most recent items first. This
                 * means the browse code starts at the topmost item in the index
                 * that matches the user's input, rather than the point in the
                 * index where the user's input would appear.
                 */
                if (!oldestFirst)
                {
                    startsWith = startsWith + "-32";
                }
            }

            scope.setFocus(startsWith);
            highlight = true;
            logInfo = "starts_with=" + startsWith + ",";
        }
        else if ((top != null) || (bottom != null))
        {
            // ----------------------------------------------
            // Paginating: put specified entry at top or bottom
            // ----------------------------------------------
            // Use a single value and a boolean to simplify the code below
            String val = bottom;
            boolean isTop = false;

            if (top != null)
            {
                val = top;
                isTop = true;
            }

            if (browseAuthors || browseSubjects)
            {
                // Value will be a text value for author browse
                scope.setFocus(val);
            }
            else
            {
                // Value is Handle if we're browsing items by title or date
                Item item = (Item) HandleManager.resolveToObject(context, val);

                if (item == null)
                {
                    // Handle is invalid one. Show an error.
                    JSPManager.showInvalidIDError(request, response, focus,
                            Constants.ITEM);

                    return;
                }

                scope.setFocus(item);
            }

            // This entry appears at the top or bottom, and so needs to have
            // 0 or 20 entries shown before it
            scope.setNumberBefore(isTop ? 0 : 20);

            logInfo = (isTop ? "top" : "bottom") + "=" + val + ",";

            if (browseDates)
            {
                // If the date order is flipped, we'll flip the table upside
                // down - i.e. the top will become the bottom and the bottom
                // the top.
                if (top != null)
                {
                    flipOrderingQuery = "bottom="
                            + URLEncoder
                                    .encode(top, Constants.DEFAULT_ENCODING)
                            + "&amp;";
                }
                else
                {
                    flipOrderingQuery = "top="
                            + URLEncoder.encode(bottom,
                                    Constants.DEFAULT_ENCODING) + "&amp;";
                }
            }
        }

        // ----------------------------------------------
        // If none of the above apply, no positioning parameters
        // set - use start of index
        // ----------------------------------------------
        // Are we in a community or collection?
        Community community = UIUtil.getCommunityLocation(request);
        Collection collection = UIUtil.getCollectionLocation(request);

        if (collection != null)
        {
            logInfo = logInfo + ",collection_id=" + collection.getID() + ",";
            scope.setScope(collection);
        }
        else if (community != null)
        {
            logInfo = logInfo + ",community_id=" + community.getID() + ",";
            scope.setScope(community);
        }

        BrowseInfo browseInfo;

        try 
        {
        	// Query the browse index
        	if (browseAuthors)
        	{
        		browseInfo = Browse.getAuthors(scope);
        	}
        	else if (browseDates)
        	{
        		browseInfo = Browse.getItemsByDate(scope, oldestFirst);
        	}
        	else if (browseSubjects)
        	{
        		browseInfo = Browse.getSubjects(scope);
        	}
        	else
        	{
        		browseInfo = Browse.getItemsByTitle(scope);
        	}
        }
        catch (SQLException sqle)
        {
        	// An invalid scope was given
        	JSPManager.showIntegrityError(request, response);
        	return;
        }

        // Write log entry
        String what = "title";

        if (browseAuthors)
        {
            what = "author";
        }
        else if (browseSubjects)
        {
            what = "subject";
        }
        else if (browseDates)
        {
            what = "date";
        }

        log.info(LogManager.getHeader(context, "browse_" + what, logInfo
                + "results=" + browseInfo.getResultCount()));

        if (browseInfo.getResultCount() == 0)
        {
            // No results!
            request.setAttribute("community", community);
            request.setAttribute("collection", collection);

            JSPManager.showJSP(request, response, "/browse/no-results.jsp");
        }
        else
        {
            // Work out what the query strings will be for the previous
            // and next pages
            if (!browseInfo.isFirst())
            {
                // Not the first page, so we'll need a "previous page" button
                // The top entry of the current page becomes the bottom
                // entry of the "previous page"
                String s;

                if (browseAuthors || browseSubjects) //aneesh
                {
                    s = (browseInfo.getStringResults())[0];
                }
                else
                {
                    Item firstItem = (browseInfo.getItemResults())[0];
                    s = firstItem.getHandle();
                }

                if (browseDates && oldestFirst)
                {
                    // For browsing by date, oldest first, we need
                    // to add the ordering parameter
                    request.setAttribute("previous.query",
                            "order=oldestfirst&amp;bottom="
                                    + URLEncoder.encode(s,
                                            Constants.DEFAULT_ENCODING));
                }
                else
                {
                    request.setAttribute("previous.query", "bottom="
                            + URLEncoder.encode(s, Constants.DEFAULT_ENCODING));
                }
            }

            if (!browseInfo.isLast())
            {
                // Not the last page, so we'll need a "next page" button
                // The bottom entry of the current page will be the top
                // entry in the next page
                String s;

                if (browseAuthors)
                {
                    String[] authors = browseInfo.getStringResults();
                    s = authors[authors.length - 1];
                }
                else if (browseSubjects)
                {
                    String[] subjects = browseInfo.getStringResults();
                    s = subjects[subjects.length - 1];
                }
                else
                {
                    Item[] items = browseInfo.getItemResults();
                    Item lastItem = items[items.length - 1];
                    s = lastItem.getHandle();
                }

                if (browseDates && oldestFirst)
                {
                    // For browsing by date, oldest first, we need
                    // to add the ordering parameter
                    request.setAttribute("next.query", "order=oldestfirst&amp;top="
                            + URLEncoder.encode(s, Constants.DEFAULT_ENCODING));
                }
                else
                {
                    request.setAttribute("next.query", "top="
                            + URLEncoder.encode(s, Constants.DEFAULT_ENCODING));
                }
            }

            // Set appropriate attributes and forward to results page
            request.setAttribute("community", community);
            request.setAttribute("collection", collection);
            request.setAttribute("browse.info", browseInfo);
            request.setAttribute("highlight", new Boolean(highlight));

            if (browseAuthors)
            {
                JSPManager.showJSP(request, response, "/browse/authors.jsp");
            }
            else if (browseSubjects)
            {
                JSPManager.showJSP(request, response, "/browse/subjects.jsp");
            }
            else if (browseDates)
            {
                request.setAttribute("oldest.first", new Boolean(oldestFirst));
                request.setAttribute("flip.ordering.query", flipOrderingQuery);
                JSPManager.showJSP(request, response,
                        "/browse/items-by-date.jsp");
            }
            else
            {
                JSPManager.showJSP(request, response,
                        "/browse/items-by-title.jsp");
            }
        }
    }
}
