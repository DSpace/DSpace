/*
 * WorkflowAbortServlet.java
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
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Servlet for aborting workflows
 * 
 * @author dstuve
 * @version $Revision$
 */
public class WorkflowAbortServlet extends DSpaceServlet
{
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response)
        throws AuthorizeException, IOException, ServletException
    {
        // Get displays list of workflows
        showWorkflows(c, request, response);
    }

    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_abort"))
        {
            // bring up the confirm page
            WorkflowItem wi = WorkflowItem.find(c, UIUtil.getIntParameter(
                    request, "workflow_id"));

            request.setAttribute("workflow", wi);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/workflow-abort-confirm.jsp");
        }
        else if (button.equals("submit_abort_confirm"))
        {
            // do the actual abort
            WorkflowItem wi = WorkflowItem.find(c, UIUtil.getIntParameter(
                    request, "workflow_id"));

            WorkflowManager.abort(c, wi, c.getCurrentUser());

            // now show what's left
            showWorkflows(c, request, response);
        }
        else
        {
            // must have been cancel
            showWorkflows(c, request, response);
        }

        // now commit the changes
        c.complete();
    }

    private void showWorkflows(Context c, HttpServletRequest request,
            HttpServletResponse response)
        throws AuthorizeException, IOException, ServletException
    {
        WorkflowItem[] w = WorkflowItem.findAll(c);

        request.setAttribute("workflows", w);
        JSPManager.showJSP(request, response, "/dspace-admin/workflow-list.jsp");
    }
}
