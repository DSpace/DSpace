/*
 * Authenticate.java
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
package org.dspace.app.webui.util;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Methods for authenticating the user. This is DSpace platform code, as opposed
 * to the site-specific authentication code, that resides in implementations of
 * the <code>org.dspace.eperson.AuthenticationMethod</code> interface.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Authenticate
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Authenticate.class);

    /**
     * Return the request that the system should be dealing with, given the
     * request that the browse just sent. If the incoming request is from a
     * redirect resulting from successful authentication, a request object
     * corresponding to the original request that prompted authentication is
     * returned. Otherwise, the request passed in is returned.
     * 
     * @param request
     *            the incoming HTTP request
     * @return the HTTP request the DSpace system should deal with
     */
    public static HttpServletRequest getRealRequest(HttpServletRequest request)
    {
        HttpSession session = request.getSession();

        if (session.getAttribute("resuming.request") != null)
        {
            // Get info about the interrupted request
            RequestInfo requestInfo = (RequestInfo) session
                    .getAttribute("interrupted.request.info");

            HttpServletRequest actualRequest;

            if (requestInfo == null)
            {
                // Can't find the wrapped request information.
                // FIXME: Proceed with current request - correct?
                actualRequest = request;
            }
            else
            {
                /*
                 * Wrap the current request to make it look like the interruped
                 * one
                 */
                actualRequest = requestInfo.wrapRequest(request);
            }

            // Remove the info from the session so it isn't resumed twice
            session.removeAttribute("resuming.request");
            session.removeAttribute("interrupted.request.info");
            session.removeAttribute("interrupted.request.url");

            // Return the wrapped request
            return actualRequest;
        }
        else
        {
            return request;
        }
    }

    /**
     * Resume a previously interrupted request. This is invoked when a user has
     * been successfully authenticated. The request which led to authentication
     * will be resumed.
     * 
     * @param request
     *            <em>current</em> HTTP request
     * @param response
     *            HTTP response
     */
    public static void resumeInterruptedRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException
    {
        HttpSession session = request.getSession();
        String originalURL = (String) session
                .getAttribute("interrupted.request.url");

        if (originalURL == null)
        {
            // If for some reason we don't have the original URL, redirect
            // to My DSpace
            originalURL = request.getContextPath() + "/mydspace";
        }
        else
        {
            // Set the flag in the session, so that when the redirect is
            // followed, we'll know to resume the interrupted request
            session.setAttribute("resuming.request", new Boolean(true));
        }

        // Send the redirect
        response.sendRedirect(response.encodeRedirectURL(originalURL));
    }

    /**
     * Start the authentication process. This packages up the request that led
     * to authentication being required, and then invokes the site-specific
     * authentication method.
     * 
     * If it returns true, the user was authenticated without any
     * redirection (e.g. by an X.509 certificate or other implicit method) so
     * the process that called this can continue and send its own response.
     * A "false" result means this method has sent its own redirect.
     *
     * @param context
     *            current DSpace context
     * @param request
     *            current HTTP request - the one that prompted authentication
     * @param response
     *            current HTTP response
     *
     * @return true if authentication is already finished (implicit method)
     */
    public static boolean startAuthentication(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        /*
         * Authenticate:
         * 1. try implicit methods first, since that may work without
         *    a redirect. return true if no redirect needed.
         * 2. if those fail, redirect to enter credentials.
         *    return false.
         */
        if (AuthenticationManager.authenticateImplicit(context, null, null,
                null, request) == AuthenticationMethod.SUCCESS)
        {
            loggedIn(context, request, context.getCurrentUser());
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            return true;
        }
        else
        {
        // Since we may be doing a redirect, make sure the redirect is not
        // cached
        response.addDateHeader("expires", 1);
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-control", "no-store");

        // Store the data from the request that led to authentication
        RequestInfo info = new RequestInfo(request);
        session.setAttribute("interrupted.request.info", info);

        // Store the URL of the request that led to authentication
        session.setAttribute("interrupted.request.url", UIUtil
                .getOriginalURL(request));

            /*
             * Grovel over authentication methods, counting the
             * ones with a "redirect" login page -- if there's only one,
             * go directly there.  If there is a choice, go to JSP chooser.
             */
            Iterator ai = AuthenticationManager.authenticationMethodIterator();
            AuthenticationMethod am;
            int count = 0;
            String url = null;
            while (ai.hasNext())
            {
                String s;
                am = (AuthenticationMethod)ai.next();
                if ((s = am.loginPageURL(context, request, response)) != null)
                {
                    url = s;
                    ++count;
                }
            }
            if (count == 1)
                response.sendRedirect(url);
            else
                JSPManager.showJSP(request, response, "/login/chooser.jsp");
        }
        return false;
    }

    /**
     * Store information about the current user in the request and context
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param eperson
     *            the eperson logged in
     */
    public static void loggedIn(Context context, HttpServletRequest request,
            EPerson eperson)
    {
        HttpSession session = request.getSession();

        context.setCurrentUser(eperson);
        
        boolean isAdmin = false;
        
        isAdmin = AuthorizeManager.isAdmin(context);
        request.setAttribute("is.admin", new Boolean(isAdmin));

        // We store the current user in the request as an EPerson object...
        request.setAttribute("dspace.current.user", eperson);

        // and in the session as an ID
        session.setAttribute("dspace.current.user.id", new Integer(eperson
                .getID()));

        // and the remote IP address to compare against later requests
        // so we can detect session hijacking.
        session.setAttribute("dspace.current.remote.addr",
                             request.getRemoteAddr());

    }

    /**
     * Log the user out
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     */
    public static void loggedOut(Context context, HttpServletRequest request)
    {
        HttpSession session = request.getSession();

        context.setCurrentUser(null);
        request.removeAttribute("is.admin");
        request.removeAttribute("dspace.current.user");
        session.removeAttribute("dspace.current.user.id");
    }
}
