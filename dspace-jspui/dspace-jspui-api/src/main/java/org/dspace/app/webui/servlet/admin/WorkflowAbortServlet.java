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
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
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
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        WorkflowItem[] w = WorkflowItem.findAll(c);

        request.setAttribute("workflows", w);
        JSPManager
                .showJSP(request, response, "/dspace-admin/workflow-list.jsp");
    }
}
