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

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * Servlet that handles the controlled vocabulary
 * 
 * @author Miguel Ferreira
 * @version $Revision$
 */
public class ControlledVocabularyServlet extends DSpaceServlet
{
    // private static Logger log =
    // Logger.getLogger(ControlledVocabularyServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        String ID = "";
        String filter = "";
        String callerUrl = request.getParameter("callerUrl");

        if (request.getParameter("ID") != null)
        {
            ID = request.getParameter("ID");
        }

        if (request.getParameter("filter") != null)
        {
            filter = request.getParameter("filter");
        }

        request.getSession()
                .setAttribute("controlledvocabulary.filter", filter);
        request.getSession().setAttribute("controlledvocabulary.ID", ID);
        response.sendRedirect(callerUrl);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        doDSGet(context, request, response);
    }

}
