/*
 * Authenticate.java
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
package org.dspace.app.webui.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dspace.app.webui.SiteAuthenticator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;


/**
 * Methods for authenticating the user.  This is DSpace platform code, as
 * opposed to the site-specific authentication code, that resides in
 * implementations of the <code>org.dspace.app.webui.SiteAuthenticator</code>
 * interface.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class Authenticate
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Authenticate.class);

    /** The site authenticator */
    private static SiteAuthenticator siteAuth = null;

    /**
     * Get the site authenticator.  Reads the appropriate configuration
     * property.
     *
     * @return   the implementation of the SiteAuthenticator interface to
     *           use for this DSpace site.
     */
    public static SiteAuthenticator getSiteAuth()
    {
        if (siteAuth != null)
        {
            return siteAuth;
        }

        // Instantiate the site authenticator
        String siteAuthClassName = ConfigurationManager.getProperty("webui.site.authenticator");

        try
        {
            Class siteAuthClass = Class.forName(siteAuthClassName);
            siteAuth = (SiteAuthenticator) siteAuthClass.newInstance();
        } catch (Exception e)
        {
            // Problem instantiating
            if (siteAuthClassName == null)
            {
                siteAuthClassName = "null";
            }

            log.fatal(LogManager.getHeader(null, "no_site_authenticator",
                                           "webui.site.authenticator=" +
                                           siteAuthClassName), e);

            throw new IllegalStateException(e.toString());
        }

        return siteAuth;
    }

    /**
     * Return the request that the system should be dealing with, given the
     * request that the browse just sent.  If the incoming request is from
     * a redirect resulting from successful authentication, a request object
     * corresponding to the original request that prompted authentication is
     * returned.  Otherwise, the request passed in is returned.
     *
     * @param request   the incoming HTTP request
     * @return   the HTTP request the DSpace system should deal with
     */
    public static HttpServletRequest getRealRequest(HttpServletRequest request)
    {
        HttpSession session = request.getSession();

        if (session.getAttribute("resuming.request") != null)
        {
            // Get info about the interrupted request
            RequestInfo requestInfo = (RequestInfo) session.getAttribute("interrupted.request.info");

            HttpServletRequest actualRequest;

            if (requestInfo == null)
            {
                // Can't find the wrapped request information.
                // FIXME: Proceed with current request - correct?
                actualRequest = request;
            } else
            {
                /*
                 * Wrap the current request to make it look like the
                 * interruped one
                 */
                actualRequest = requestInfo.wrapRequest(request);
            }

            // Remove the info from the session so it isn't resumed twice
            session.removeAttribute("resuming.request");
            session.removeAttribute("interrupted.request.info");
            session.removeAttribute("interrupted.request.url");

            // Return the wrapped request
            return actualRequest;
        } else
        {
            return request;
        }
    }

    /**
     * Resume a previously interrupted request.  This is invoked when a user
     * has been successfully authenticated.  The request which led to
     * authentication will be resumed.
     *
     * @param request   <em>current</em> HTTP request
     * @param response  HTTP response
     */
    public static void resumeInterruptedRequest(HttpServletRequest request,
                                                HttpServletResponse response)
                                         throws IOException
    {
        HttpSession session = request.getSession();
        String originalURL = (String) session.getAttribute("interrupted.request.url");

        if (originalURL == null)
        {
            // If for some reason we don't have the original URL, redirect
            // to My DSpace
            originalURL = request.getContextPath() + "/mydspace";
        } else
        {
            // Set the flag in the session, so that when the redirect is
            // followed, we'll know to resume the interrupted request
            session.setAttribute("resuming.request", new Boolean(true));
        }

        // Send the redirect
        response.sendRedirect(response.encodeRedirectURL(originalURL));
    }

    /**
     * Start the authentication process.  This packages up the request that
     * led to authentication being required, and then invokes the site-specific
     * authentication method.
     *
     * @param context   current DSpace context
     * @param request   current HTTP request - the one that prompted
     *                  authentication
     * @param response  current HTTP response
     */
    public static void startAuthentication(Context context,
                                           HttpServletRequest request,
                                           HttpServletResponse response)
                                    throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        // Since we may be doing a redirect, make sure the redirect is not
        // cached
        response.addDateHeader("expires", 1);
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-control", "no-store");

        // Store the data from the request that led to authentication
        RequestInfo info = new RequestInfo(request);
        session.setAttribute("interrupted.request.info", info);

        // Store the URL of the request that led to authentication
        session.setAttribute("interrupted.request.url",
                             UIUtil.getOriginalURL(request));

        // Start up the site authenticator
        getSiteAuth().startAuthentication(context, request, response);
    }

    /**
     * Store information about the current user in the request and context
     *
     * @param context   DSpace context
     * @param request   HTTP request
     * @param eperson   the eperson logged in
     */
    public static void loggedIn(Context context, HttpServletRequest request,
                                EPerson eperson)
    {
        HttpSession session = request.getSession();

        context.setCurrentUser(eperson);

        // We store the current user in the request as an EPerson object...
        request.setAttribute("dspace.current.user", eperson);

        // and in the session as an ID
        session.setAttribute("dspace.current.user.id",
                             new Integer(eperson.getID()));
    }

    /**
     * Log the user out
     *
     * @param context   DSpace context
     * @param request   HTTP request
     */
    public static void loggedOut(Context context, HttpServletRequest request)
    {
        HttpSession session = request.getSession();

        context.setCurrentUser(null);
        request.removeAttribute("dspace.current.user");
        session.removeAttribute("dspace.current.user.id");
    }
}
