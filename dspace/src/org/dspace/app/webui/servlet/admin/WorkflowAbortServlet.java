/*
 * WorkflowAbort.java
 */

package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Servlet for aborting workflows
 * @author dstuve
 * @version $Revision$
 */
public class WorkflowAbortServlet extends DSpaceServlet
{
    protected void doDSGet(Context c,
                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Get displays list of workflows
        showWorkflows(c, request, response);
    }
    
    protected void doDSPost(Context c,
                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");
        
        if( button.equals("submit_abort") )
        {
            // bring up the confirm page
            WorkflowItem wi = WorkflowItem.find(c,
                UIUtil.getIntParameter(request, "workflow_id") );
            
            request.setAttribute("workflow", wi);
            JSPManager.showJSP(request, response,
                "/admin/workflow_abort_confirm.jsp");
        }
        else if( button.equals("submit_abort_confirm") )
        {
            // do the actual abort
            WorkflowItem wi = WorkflowItem.find(c,
                UIUtil.getIntParameter(request, "workflow_id") );

            WorkflowManager.abort(c, wi, c.getCurrentUser() );
            
            // now show what's left
            showWorkflows(c, request, response);
        }
        else
        {
            // must have been cancel
            showWorkflows(c, request, response);
        }
    }
    
    private void showWorkflows(Context c,
                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    
    {
        WorkflowItem [] w = WorkflowItem.findAll(c);
        
        request.setAttribute("workflows", w);
        JSPManager.showJSP(request, response, "/admin/WorkflowList.jsp" );
    }
}

