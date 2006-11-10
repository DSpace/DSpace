/*
 * JSPManager.java
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Methods for displaying UI pages to the user.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class JSPManager
{
    /*
     * All displaying of UI pages should be performed using this manager for
     * future-proofing, since any future localisation effort will probably use
     * this manager.
     */

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPManager.class);

    /**
     * Forwards control of the request to the display JSP passed in.
     * 
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param jsp
     *            the JSP page to display, relative to the webapps directory
     */
    public static void showJSP(HttpServletRequest request,
            HttpServletResponse response, String jsp) throws ServletException,
            IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader((Context) request
                    .getAttribute("dspace.context"), "view_jsp", jsp));
        }

        // For the moment, a simple forward
        request.getRequestDispatcher(jsp).forward(request, response);
    }

    /**
     * Display an internal server error message - for example, a database error
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     */
    public static void showInternalError(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        showJSP(request, response, "/error/internal.jsp");
    }

    /**
     * Display an integrity error message. Use when the POSTed data from a
     * request doesn't make sense.
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     */
    public static void showIntegrityError(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showJSP(request, response, "/error/integrity.jsp");
    }

    /**
     * Display an authorization failed error message. The exception should be
     * passed in if possible so that the error message can be descriptive.
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param exception
     *            the AuthorizeException leading to this error, passing in
     *            <code>null</code> will display default error message
     */
    public static void showAuthorizeError(HttpServletRequest request,
            HttpServletResponse response, AuthorizeException exception)
            throws ServletException, IOException
    {
        // FIXME: Need to work out which error message to display?
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        showJSP(request, response, "/error/authorize.jsp");
    }

    /**
     * Display an "invalid ID" error message. Passing in information about the
     * bad ID and what the ID was supposed to represent (collection etc.) should
     * result in a more descriptive and helpful error message.
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param badID
     *            the bad identifier, or <code>null</code>
     * @param type
     *            the type of object, from
     *            <code>org.dspace.core.Constants</code>, or <code>-1</code>
     *            for a default message
     */
    public static void showInvalidIDError(HttpServletRequest request,
            HttpServletResponse response, String badID, int type)
            throws ServletException, IOException
    {
        request.setAttribute("bad.id", badID);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        if (type != -1)
        {
            request.setAttribute("bad.type", new Integer(type));
        }

        showJSP(request, response, "/error/invalid-id.jsp");
    }
}
