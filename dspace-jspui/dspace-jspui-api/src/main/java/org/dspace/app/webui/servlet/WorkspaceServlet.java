/*
 * WorkspaceServlet.java
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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
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
    private static Logger log = Logger.getLogger(WorkspaceServlet.class);
    
    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // just pass all requests to the same place.
        doDSPost(c, request, response);
    }
    
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
        WorkspaceItem wsItem = WorkspaceItem.find(context, wsItemID);
        
        // Ensure the user has authorisation
        Item item = wsItem.getItem();
        AuthorizeManager.authorizeAction(context, item, Constants.READ);
        
        log.info(LogManager.getHeader(context, 
            "View Workspace Item", 
            "workspace_item_id="+wsItemID));
        
        // set the attributes for the JSP
        request.setAttribute("wsItem", wsItem);
        
        JSPManager.showJSP(request, response, "/workspace/ws-main.jsp");
    }
    
}
