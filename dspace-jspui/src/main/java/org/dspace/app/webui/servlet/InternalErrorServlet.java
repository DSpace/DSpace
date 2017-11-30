/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;

/**
 * Servlet for handling an internal server error
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class InternalErrorServlet extends HttpServlet
{
    /*
     * We don't extend DSpaceServlet in case it's context creation etc. that
     * caused the problem!
     */

    /** log4j category */
    private static Logger log = Logger.getLogger(InternalErrorServlet.class);

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        // Get the exception that occurred, if any
        Throwable t = (Throwable) request
                .getAttribute("javax.servlet.error.exception");

        String logInfo = UIUtil.getRequestLogInfo(request);

        // Log the error. Since we don't have a context, we need to
        // build the info "by hand"
        String logMessage = ":session_id=" + request.getSession().getId()
                + ":internal_error:" + logInfo;

        log.warn(logMessage, t);

        // Now we try and mail the designated user, if any
        UIUtil.sendAlert(request, (Exception) t);

        JSPManager.showJSP(request, response, "/error/internal.jsp");
    }

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }
}
