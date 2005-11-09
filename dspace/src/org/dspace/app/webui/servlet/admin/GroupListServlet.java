/*
 * GroupListServlet.java
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
		if (first == -1) first = 0;

		// Retrieve the e-people in the specified order
		Group[] groups = Group.findAll(context, sortBy);
		
		// Set attributes for JSP
		request.setAttribute("sortby", new Integer(sortBy));
		request.setAttribute("first",  new Integer(first));
		request.setAttribute("groups", groups);
		if (multiple)
		{
			request.setAttribute("multiple", new Boolean(true));
		}
		
		JSPManager.showJSP(request, response, "/tools/group-select-list.jsp");
	}
}
