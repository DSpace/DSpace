/*
 * DSpaceServlet.java
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;

/**
 * Servlet for displaying item pages
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class DisplayItemServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DisplayItemServlet.class);
    

    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        Item item = null;

        // Get the item handle from the URL
        String handle = request.getPathInfo();

        if (handle != null)
        {
            // Remove leading slash
            if (handle.startsWith("/"))
            {
                handle = handle.substring(1);
            }

            // Attempt to resolve
            item = (Item) HandleManager.resolveToObject(context, handle);
        }

        // If everything is OK, display the item
        if (item != null)
        {
            // Ensure the user has authorisation
            AuthorizeManager.authorizeAction(context, item, Constants.READ);

            log.info(LogManager.getHeader(context,
                "view_item",
                "handle=" + handle));
            
            // Get the collections
            Collection[] collections = item.getCollections();

            // Get corresponding communities
            Community[] communities = new Community[collections.length];
            
            // FIXME: (maybe) We just grab the first community if there are
            // multiple communities
            for (int i = 0; i < collections.length; i++)
            {
                Community[] commsForThis = collections[i].getCommunities();
                communities[i] = commsForThis[0];
            }
            
            // Full or simple display?
            boolean displayAll = false;
            String modeParam = request.getParameter("mode");
            if (modeParam != null && modeParam.equalsIgnoreCase("full"))
            {
                displayAll = true;
            }

            
            // Set attributes and display
            request.setAttribute("display.all", new Boolean(displayAll));
            request.setAttribute("handle", handle);
            request.setAttribute("item", item);
            request.setAttribute("collections", collections);
            request.setAttribute("communities", communities);
            JSPManager.showJSP(request, response, "/display-item.jsp");
        }
        else
        {
            // Show an error
            JSPManager.showInvalidIDError(request,
                response,
                handle,
                Constants.ITEM);
        }
    }
}
