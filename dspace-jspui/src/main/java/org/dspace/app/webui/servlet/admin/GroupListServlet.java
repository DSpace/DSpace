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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Servlet browsing through groups and selecting them
 *
 *  * @version $Revision$
 */
public class GroupListServlet extends DSpaceServlet
{
	private final transient GroupService groupService
             = EPersonServiceFactory.getInstance().getGroupService();
	
    @Override
	protected void doDSGet(Context context,
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException, SQLException, AuthorizeException
	{
		// Are we for selecting a single or multiple groups?
		boolean multiple = UIUtil.getBoolParameter(request, "multiple");
		
		// What are we sorting by?  Name is default
		int sortBy = GroupService.NAME;

		// What's the index of the first group to show?  Default is 0
		int first = UIUtil.getIntParameter(request, "first");
		if (first == -1)
        {
            first = 0;
        }

		// Retrieve the e-people in the specified order
		List<Group> groups = groupService.findAll(context, sortBy);
		
		// Set attributes for JSP
		request.setAttribute("sortby", sortBy);
		request.setAttribute("first", first);
		request.setAttribute("groups", groups);
		if (multiple)
		{
			request.setAttribute("multiple", Boolean.TRUE);
		}
		
		JSPManager.showJSP(request, response, "/tools/group-select-list.jsp");
	}
}
