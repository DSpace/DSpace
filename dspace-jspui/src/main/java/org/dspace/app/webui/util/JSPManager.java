/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
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
        try {
            // For the moment, a simple forward
            // First test if the response is already committed (could happen by broken downloads),
            // if that is the case, forward won't work.
            if (!response.isCommitted())
            {
                request.getRequestDispatcher(jsp).forward(request, response);
            }
            else
            {
                log.warn("Couldn't show jsp, response is already commited.");
            }
        } catch (Exception e) {
             throw new ServletException(e);
        }
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
        request.setAttribute("bad.id", StringEscapeUtils.escapeHtml(badID));
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        if (type != -1)
        {
            request.setAttribute("bad.type", Integer.valueOf(type));
        }

        showJSP(request, response, "/error/invalid-id.jsp");
    }

    /**
     * Display a "file upload was too large" error message. Passing in information
     * about the size of the file uploaded, and the maximum file size limit so
     * the user knows why they encountered an error.
     * @param request
     * @param response
     * @param message
     * @param actualSize
     * @param permittedSize
     * @throws ServletException
     * @throws IOException
     */
    public static void showFileSizeLimitExceededError(HttpServletRequest request,
            HttpServletResponse response, String message, long actualSize, long permittedSize) throws ServletException, IOException
    {
        request.setAttribute("error.message", message);
        request.setAttribute("actualSize", actualSize);
        request.setAttribute("permittedSize", permittedSize);
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        showJSP(request, response, "/error/exceeded-size.jsp");
    }
}
