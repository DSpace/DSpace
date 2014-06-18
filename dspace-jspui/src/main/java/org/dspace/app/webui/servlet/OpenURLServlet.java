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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Simple servlet for open URL support. Presently, simply extracts terms from
 * open URL and redirects to search.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class OpenURLServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(OpenURLServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String query = "";

        // Extract open URL terms. Note: assume no repetition
        String title = request.getParameter("title");
        String authorFirst = request.getParameter("aufirst");
        String authorLast = request.getParameter("aulast");

        String logInfo = "";

        if (title != null)
        {
            query = query + " " + title;
            logInfo = logInfo + "title=\"" + title + "\",";
        }

        if (authorFirst != null)
        {
            query = query + " " + authorFirst;
            logInfo = logInfo + "aufirst=\"" + authorFirst + "\",";
        }

        if (authorLast != null)
        {
            query = query + " " + authorLast;
            logInfo = logInfo + "aulast=\"" + authorLast + "\",";
        }

        log.info(LogManager.getHeader(context, "openURL", logInfo
                + "dspacequery=" + query));

        response.sendRedirect(response.encodeRedirectURL(request
                .getContextPath()
                + "/simple-search?query=" + query));
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Same as a GET
        doDSGet(context, request, response);
    }
}
