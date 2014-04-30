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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.CollectionDropDown;
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
        else if (submit.equals("submit_subscribe"))
        {
            int collID = UIUtil.getIntParameter(request, "collection");
            Collection c = Collection.find(context, collID);

            // Sanity check - ignore duff values
            if (c != null)
            {
                Subscribe.subscribe(context, e, c);
            }

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
        // collections the currently logged in user can subscribe to
        Collection[] avail = Subscribe.getAvailableSubscriptions(context);
        
        // Subscribed collections
        Collection[] subs = Subscribe.getSubscriptions(context, context
                .getCurrentUser());

        request.setAttribute("availableSubscriptions", avail);
        request.setAttribute("subscriptions", subs);
        request.setAttribute("updated", Boolean.valueOf(updated));

        JSPManager.showJSP(request, response, "/mydspace/subscriptions.jsp");
    }
}
