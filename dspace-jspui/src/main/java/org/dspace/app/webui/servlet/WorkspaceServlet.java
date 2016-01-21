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

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Servlet for handling the workspace item
 *
 * @author Richard Jones
 * @version  $Revision$
 */
public class WorkspaceServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(WorkspaceServlet.class);

    private final transient WorkspaceItemService workspaceItemService
             = ContentServiceFactory.getInstance().getWorkspaceItemService();
    
    @Override
    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // just pass all requests to the same place.
        doDSPost(c, request, response);
    }
    
    @Override
    protected void doDSPost(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        
        String button = UIUtil.getSubmitButton(request, "submit_error");
        
        // direct the request to the relevant set of methods
        if (button.equals("submit_open"))
        {
            showMainPage(c, request, response);
        }
        else if (button.equals("submit_cancel"))
        {
            goToMyDSpace(c, request, response);
        }
        else if (button.equals("submit_error"))
        {
            showErrorPage(c, request, response);
        }
    }
    
    
    /**
     * Show error page if nothing has been <code>POST</code>ed to servlet
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void showErrorPage(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        int wsItemID = UIUtil.getIntParameter(request, "workspace_id");
        
        log.error(LogManager.getHeader(context, 
            "Workspace Item View Failed", 
            "workspace_item_id="+wsItemID));
        
        JSPManager.showJSP(request, response, "/workspace/ws-error.jsp");
    }
    
    /**
     * Return the user to the mydspace servlet
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void goToMyDSpace(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/mydspace"));
    }
    
    
    /**
     * show the workspace item home page
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void showMainPage(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // get the values out of the request
        int wsItemID = UIUtil.getIntParameter(request, "workspace_id");
        
        // get the workspace item
        WorkspaceItem wsItem = workspaceItemService.find(context, wsItemID);
        
        // Ensure the user has authorisation
        Item item = wsItem.getItem();
        authorizeService.authorizeAction(context, item, Constants.READ);
        
        log.info(LogManager.getHeader(context, 
            "View Workspace Item", 
            "workspace_item_id="+wsItemID));
        
        // set the attributes for the JSP
        request.setAttribute("wsItem", wsItem);
        
        JSPManager.showJSP(request, response, "/workspace/ws-main.jsp");
    }
    
}
