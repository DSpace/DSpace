/*
 * LocationServlet.java
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
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Servlet for handling requests within communities and collections.
 * This servlet extracts the community and collection ID from the URL.  It
 * then either displays the community or collection home page if the URL
 * after the community/collection portion is "/".  Otherwise, the request
 * is forwarded to the appropriate servlet.  Examples:
 * <P>
 * <code>/community/XX/</code> - Community <code>XX</code> home page<BR>
 * <code>/community/XX/collection/YY/</code> - Collection <code>YY</code> home
 * page<BR>
 * <code>/community/XX/foo</code> - Location is set to community <code>XX</code>
 * and request is forwarded to <code>/foo</code>.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class LocationServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceServlet.class);


    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        Community community = null;
        Collection collection = null;
        boolean locationOK = true;
        int slash;
        String path = request.getPathInfo();

        try
        {
            if (path == null)
            {
                locationOK = false;
            }
            else
            {
                // Grab the community ID.
                slash = path.indexOf('/', 1);

                if (slash > -1)
                {
                    String communityIdString = path.substring(1, slash);
                    int communityID = Integer.parseInt(communityIdString);
                    community = Community.find(context, communityID);

                    if (community == null)
                    {
                        locationOK = false;
                    }
                    else
                    {
                        path = path.substring(slash);
                    }
                }
                else
                {
                    locationOK = false;
                }

                if (path.startsWith("/collections/"))
                {
                    // We also have a collection location
                    slash = path.indexOf('/', 13);
                    if (slash > -1)
                    {
                        String collectionIdString = path.substring(13, slash);
                        int collectionID = Integer.parseInt(collectionIdString);
                        collection = Collection.find(context, collectionID);

                        if (collection == null)
                        {
                            locationOK = false;
                        }
                        else
                        {
                            path = path.substring(slash);
                        }
                    }
                    else
                    {
                        locationOK = false;
                    }
                }
            }
        }
        catch (NumberFormatException nfe)
        {
            // Mangled IDs
            locationOK = false;
        }
		
        if (locationOK)
        {
            // Location is fine
            request.setAttribute("dspace.community", community);
            request.setAttribute("dspace.collection", collection);

            // Show community or collection home page?
            if (path.equals("/"))
            {
                if (collection == null)
                {
                    showCommunityHome(context, request, response, community);
                }
                else
                {
                    showCollectionHome(context,
                        request,
                        response,
                        community,
                        collection);
                }
            }
            else
            {
                // Forward to another servlet
                request.getRequestDispatcher(path).forward(
                    request,
                    response);
            }
        }
        else
        {
            // Invalid location.
            
            // Was it a collection or community ID that was bad?
            int type = Constants.COMMUNITY;

            if (community != null)
            {
                // We found a collection OK so it must have been the community
                type = Constants.COLLECTION;
            }

            JSPManager.showInvalidIDError(request, response, null, type);
        }
    }


    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // A POST has the same effect as a GET.
        doDSGet(context, request, response);
    }


    /**
     * Show a community home page
     *
     * @param context    Context object
     * @param request    the HTTP request
     * @param response   the HTTP response
     * @param community  the Community home page
     */
    private void showCommunityHome(Context context,
        HttpServletRequest request,
        HttpServletResponse response,
        Community community )
        throws ServletException, IOException, SQLException
    {
        log.info(LogManager.getHeader(context, "view_community",
            "community_id=" + community.getID()));

        // Get the collections within the community
        Collection[] collections = community.getCollections();

        // FIXME: Logo
        
        // FIXME: Last submitted items

        request.setAttribute("community", community);
        request.setAttribute("collections", collections);
        JSPManager.showJSP(request, response, "/community-home.jsp");
    }


    /**
     * Show a collection home page
     *
     * @param context     Context object
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param community   the community the collection is in
     * @param collection  the Collection home page
     */
    private void showCollectionHome(Context context,
        HttpServletRequest request,
        HttpServletResponse response,
        Community community,
        Collection collection )
        throws ServletException, IOException, SQLException
    {
        log.info(LogManager.getHeader(context, "view_collection",
            "collection_id=" + collection.getID()));

        // FIXME: Logo
        
        // FIXME: Last submitted items

        request.setAttribute("community", community);
        request.setAttribute("collection", collection);
        JSPManager.showJSP(request, response, "/collection-home.jsp");
    }
}
