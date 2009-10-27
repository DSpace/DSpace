/*
 * EPersonListServlet.java
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

/**
 * Servlet browsing through e-people and selecting them
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class EPersonListServlet extends DSpaceServlet
{
	protected void doDSPost(Context context, HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, IOException, 
			SQLException, AuthorizeException 
	{
		doDSGet(context, request, response);
	}

	protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Are we for selecting a single or multiple epeople?
        boolean multiple = UIUtil.getBoolParameter(request, "multiple");

        // What are we sorting by. Lastname is default
        int sortBy = EPerson.LASTNAME;

        String sbParam = request.getParameter("sortby");

        if ((sbParam != null) && sbParam.equals("lastname"))
        {
            sortBy = EPerson.LASTNAME;
        }
        else if ((sbParam != null) && sbParam.equals("email"))
        {
            sortBy = EPerson.EMAIL;
        }
        else if ((sbParam != null) && sbParam.equals("id"))
        {
            sortBy = EPerson.ID;
        }
        else if ((sbParam != null) && sbParam.equals("language"))
        {
            sortBy = EPerson.LANGUAGE;
        }

        // What's the index of the first eperson to show? Default is 0
        int first = UIUtil.getIntParameter(request, "first");
        int offset = UIUtil.getIntParameter(request, "offset");
        if (first == -1)
        {
            first = 0;
        }
        if (offset == -1)
        {
            offset = 0;
        }
        

        EPerson[] epeople;
        String search = request.getParameter("search");
        if (search != null && !search.equals(""))
        {
            epeople = EPerson.search(context, search);
            request.setAttribute("offset", new Integer(offset));
        }
        else
        {
            // Retrieve the e-people in the specified order
            epeople = EPerson.findAll(context, sortBy);
            request.setAttribute("offset", new Integer(0));            
        }        
        
        // Set attributes for JSP
        request.setAttribute("sortby", new Integer(sortBy));
        request.setAttribute("first", new Integer(first));
        request.setAttribute("epeople", epeople);
        request.setAttribute("search", search);
        
        if (multiple)
        {
            request.setAttribute("multiple", new Boolean(true));
        }

        JSPManager.showJSP(request, response, "/tools/eperson-list.jsp");
    }
}
