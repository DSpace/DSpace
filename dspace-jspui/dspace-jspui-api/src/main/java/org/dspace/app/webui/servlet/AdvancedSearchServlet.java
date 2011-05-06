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

import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.core.Context;

/**
 * Servlet for constructing the advanced search form
 * 
 * @author gam
 * @version $Revision: 5845 $
 */
public class AdvancedSearchServlet extends DSpaceServlet
{
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // just build a list of top-level communities and pass along to the jsp
        Community[] communities = Community.findAllTop(context);

        request.setAttribute("communities", communities);

        JSPManager.showJSP(request, response, "/search/advanced.jsp");
    }
}
