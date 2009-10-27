/*
 * SubscribeServlet.java
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
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscribe;

/**
 * Servlet for constructing the components of the "My DSpace" page
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubscribeServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(SubscribeServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Simply show list of subscriptions
        showSubscriptions(context, request, response, false);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * Parameters: submit_unsubscribe - unsubscribe from a collection
         * submit_clear - clear all subscriptions submit_cancel - cancel update -
         * go to My DSpace.
         */
        String submit = UIUtil.getSubmitButton(request, "submit");
        EPerson e = context.getCurrentUser();

        if (submit.equals("submit_clear"))
        {
            // unsubscribe user from everything
            Subscribe.unsubscribe(context, e, null);

            // Show the list of subscriptions
            showSubscriptions(context, request, response, true);

            context.complete();
        }
        else if (submit.equals("submit_unsubscribe"))
        {
            int collID = UIUtil.getIntParameter(request, "collection");
            Collection c = Collection.find(context, collID);

            // Sanity check - ignore duff values
            if (c != null)
            {
                Subscribe.unsubscribe(context, e, c);
            }

            // Show the list of subscriptions
            showSubscriptions(context, request, response, true);

            context.complete();
        }
        else
        {
            // Back to "My DSpace"
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/mydspace"));
        }
    }

    /**
     * Show the list of subscriptions
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @param updated
     *            if <code>true</code>, write a message indicating that
     *            updated subscriptions have been stored
     */
    private void showSubscriptions(Context context, HttpServletRequest request,
            HttpServletResponse response, boolean updated)
            throws ServletException, IOException, SQLException
    {
        // Subscribed collections
        Collection[] subs = Subscribe.getSubscriptions(context, context
                .getCurrentUser());

        request.setAttribute("subscriptions", subs);
        request.setAttribute("updated", new Boolean(updated));

        JSPManager.showJSP(request, response, "/mydspace/subscriptions.jsp");
    }
}
