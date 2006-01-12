/*
 * ItemsBySubjectServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
import org.dspace.browse.Browse;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseScope;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Displays the items with a particular subject.
 * 
 * @version $Revision$
 */
public class ItemsBySubjectServlet extends DSpaceServlet
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ItemsBySubjectServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // We will resolve the HTTP request parameters into a scope
        BrowseScope scope = new BrowseScope(context);

        // Get log information
        String logInfo = "";

        // Get the HTTP parameters
        String  subject = request.getParameter("subject");
        String order = request.getParameter("order");

        // How should we order the items?
        boolean orderByTitle;

        if ((order != null) && order.equalsIgnoreCase("title"))
        {
            orderByTitle = true;
            logInfo = "order=title";
        }
        else
        {
            orderByTitle = false;
            logInfo = "order=date";
        }

        // Get the community or collection scope
        Community community = UIUtil.getCommunityLocation(request);
        Collection collection = UIUtil.getCollectionLocation(request);

        if (collection != null)
        {
            logInfo = logInfo + ",collection_id=" + collection.getID();
            scope.setScope(collection);
        }
        else if (community != null)
        {
            logInfo = logInfo + ",community_id=" + community.getID();
            scope.setScope(community);
        }

        // Ensure subject is non-null
        if (subject == null)
        {
            subject = "";
        }

        // Do the browse
        scope.setFocus(subject);

        BrowseInfo browseInfo = Browse.getItemsBySubject(scope, orderByTitle);

        log.info(LogManager.getHeader(context, "items_by_subject", logInfo
                + ",result_count=" + browseInfo.getResultCount()));

        // Display the JSP
        request.setAttribute("community", community);
        request.setAttribute("collection", collection);
        request.setAttribute("subject", subject);
        request.setAttribute("order.by.title", new Boolean(orderByTitle));
        request.setAttribute("browse.info", browseInfo);

        JSPManager.showJSP(request, response, "/browse/items-by-subject.jsp");
    }
}
