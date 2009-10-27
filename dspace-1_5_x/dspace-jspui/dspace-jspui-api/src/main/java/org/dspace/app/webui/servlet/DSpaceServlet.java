/*
 * DSpaceServlet.java
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Base class for DSpace servlets. This manages the initialisation of a context,
 * if a context has not already been initialised by an authentication filter. It
 * also grabs the original request URL for later use.
 * <P>
 * Unlike regular servlets, DSpace servlets should override the
 * <code>doDSGet</code> and <code>doDSPost</code> methods, which provide a
 * DSpace context to work with, and handle the common exceptions
 * <code>SQLException</code> and <code>AuthorizeException</code>.
 * <code>doGet</code> and <code>doPost</code> should be overridden only in
 * special circumstances.
 * <P>
 * Note that if all goes well, the context object passed in to
 * <code>doDSGet</code> and <code>doDSPut</code> must be completed
 * <em>after</em> a JSP has been displayed, but <code>before</code> the
 * method returns. If an error occurs (an exception is thrown, or the context is
 * not completed) the context is aborted after <code>doDSGet</code> or
 * <code>doDSPut</code> return.
 * 
 * @see org.dspace.core.Context
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DSpaceServlet extends HttpServlet
{
    /*
     * Because the error handling is indentical for GET and POST requests, to
     * make things easier, doGet and doPost are folded into one method which
     * then "re-separates" to GET and POST. We do not override "service" since
     * we wish to allow doGet and doPost to be overridden if necessary (for
     * example, by lightweight servlets that do not need a DSpace context
     * object).
     */

    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceServlet.class);

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Process an incoming request
     * 
     * @param request
     *            the request object
     * @param response
     *            the response object
     */
    private void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        Context context = null;

        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");

        // Get the URL from the request immediately, since forwarding
        // loses that information
        UIUtil.storeOriginalURL(request);

        try
        {
            // Obtain a context - either create one, or get the one created by
            // an authentication filter
            context = UIUtil.obtainContext(request);

            // Are we resuming a previous request that was interrupted for
            // authentication?
            request = Authenticate.getRealRequest(request);

            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "http_request", UIUtil
                        .getRequestLogInfo(request)));
            }

            // Invoke the servlet code
            if (request.getMethod().equals("POST"))
            {
                doDSPost(context, request, response);
            }
            else
            {
                doDSGet(context, request, response);
            }
        }
        catch (SQLException se)
        {
            // For database errors, we log the exception and show a suitably
            // apologetic error
            log.warn(LogManager.getHeader(context, "database_error", se
                    .toString()), se);

            // Also email an alert
            UIUtil.sendAlert(request, se);

            JSPManager.showInternalError(request, response);
        }
        catch (AuthorizeException ae)
        {
            /*
             * If no user is logged in, we will start authentication, since if
             * they authenticate, they might be allowed to do what they tried to
             * do. If someone IS logged in, and we got this exception, we know
             * they tried to do something they aren't allowed to, so we display
             * an error in that case.
             */
            if (context.getCurrentUser() != null ||
                Authenticate.startAuthentication(context, request, response))
            {
                // FIXME: Log the right info?
                // Log the error
                log.info(LogManager.getHeader(context, "authorize_error", ae
                        .toString()));

                JSPManager.showAuthorizeError(request, response, ae);
            }
        }
        catch (RuntimeException e)
        {
            // Catch and re-throw to ensure context aborted (via "finally")
            throw e;
        }
        catch (IOException ioe)
        {
            // Catch and re-throw to ensure context aborted (via "finally")
            throw ioe;
        }
        catch (ServletException sve)
        {
            // Catch and re-throw to ensure context aborted (via "finally")
            throw sve;
        }
        finally
        {
            // Abort the context if it's still valid
            if ((context != null) && context.isValid())
            {
                context.abort();
            }
        }
    }

    /**
     * Process an incoming HTTP GET. If an exception is thrown, or for some
     * other reason the passed in context is not completed, it will be aborted
     * and any changes made by this method discarded when this method returns.
     * 
     * @param context
     *            a DSpace Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * 
     * @throws SQLException
     *             if a database error occurs
     * @throws AuthorizeException
     *             if some authorization error occurs
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // If this is not overridden, we invoke the raw HttpServlet "doGet" to
        // indicate that GET is not supported by this servlet.
        super.doGet(request, response);
    }

    /**
     * Process an incoming HTTP POST. If an exception is thrown, or for some
     * other reason the passed in context is not completed, it will be aborted
     * and any changes made by this method discarded when this method returns.
     * 
     * @param context
     *            a DSpace Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * 
     * @throws SQLException
     *             if a database error occurs
     * @throws AuthorizeException
     *             if some authorization error occurs
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // If this is not overridden, we invoke the raw HttpServlet "doGet" to
        // indicate that POST is not supported by this servlet.
        super.doGet(request, response);
    }
}
