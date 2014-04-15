/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 */

package edu.umd.lib.dspace.app.webui.servlet.admin;

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
import org.dspace.content.Collection;

/**
 * Servlet browsing through collections and selecting them
 *
 *  * @version $Revision: 3705 $
 */
public class CollectionListServlet extends DSpaceServlet
{
	protected void doDSGet(Context context,
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException, SQLException, AuthorizeException
	{
		// Are we for selecting a single or multiple collections?
		boolean multiple = UIUtil.getBoolParameter(request, "multiple");
		
		// What's the index of the first collection to show?  Default is 0
		int first = UIUtil.getIntParameter(request, "first");
		if (first == -1) first = 0;

		// Retrieve the e-people in the specified order
		Collection[] collections = Collection.findAll(context);
		
		// Set attributes for JSP
		request.setAttribute("first",  new Integer(first));
		request.setAttribute("collections", collections);
		if (multiple)
		{
			request.setAttribute("multiple", new Boolean(true));
		}
		
		JSPManager.showJSP(request, response, "/tools/collection-select-list.jsp");
	}
}
