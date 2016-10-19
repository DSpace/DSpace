/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Servlet browsing through groups and selecting them
 *
 *  * @version $Revision$
 */
public class GroupListServlet extends DSpaceServlet
{
	protected void doDSGet(Context context,
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException, SQLException, AuthorizeException
	{
		// Are we for selecting a single or multiple groups?
		boolean multiple = UIUtil.getBoolParameter(request, "multiple");
		
		// What are we sorting by?  Name is default
		int sortBy = Group.NAME;
		
		String sbParam = request.getParameter("sortby");

		if (sbParam != null && sbParam.equals("id"))
		{
			sortBy = Group.ID;
		}
		
		// What's the index of the first group to show?  Default is 0
		int first = UIUtil.getIntParameter(request, "first");
		if (first == -1)
        {
            first = 0;
        }
        int offset = UIUtil.getIntParameter(request, "offset");
        if (first == -1)
        {
            first = 0;
        }
        if (offset == -1)
        {
            offset = 0;
        }		

		// Retrieve the e-people in the specified order
		Group[] groups;
		
        String search = request.getParameter("search");
        if (search != null && !search.equals(""))
        {
            groups = Group.search(context, search);
            request.setAttribute("offset", Integer.valueOf(offset));
        }
        else
        {
            // Retrieve the e-people in the specified order
            groups = Group.findAll(context, sortBy);
            request.setAttribute("offset", Integer.valueOf(0));
        }    
        
		// Set attributes for JSP
		request.setAttribute("sortby", Integer.valueOf(sortBy));
		request.setAttribute("first",  Integer.valueOf(first));
		request.setAttribute("groups", groups);
        request.setAttribute("search", search);
		if (multiple)
		{
			request.setAttribute("multiple", Boolean.TRUE);
		}
		
		JSPManager.showJSP(request, response, "/tools/group-select-list.jsp");
	}
}
