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
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SupervisedItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SupervisorService;

/**
 * Servlet to handle administration of the supervisory system
 *
 * @author Richard Jones
 * @version  $Revision$
 */
public class SuperviseServlet extends org.dspace.app.webui.servlet.DSpaceServlet
{
    
     /** log4j category */
    private static final Logger log = Logger.getLogger(SuperviseServlet.class);
    
    private final transient GroupService groupService
             = EPersonServiceFactory.getInstance().getGroupService();
    
    private final transient SupervisorService supervisorService
             = EPersonServiceFactory.getInstance().getSupervisorService();
    
    private final transient SupervisedItemService supervisedItemService
             = ContentServiceFactory.getInstance().getSupervisedItemService();
    
    private final transient WorkspaceItemService workspaceItemService
             = ContentServiceFactory.getInstance().getWorkspaceItemService();
    
    @Override
    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // pass all requests to the same place for simplicity
        doDSPost(c, request, response);
    }
    
    @Override
    protected void doDSPost(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        
        String button = UIUtil.getSubmitButton(request, "submit_base");
        
        //direct the request to the relevant set of methods
        if (button.equals("submit_add"))
        {
            showLinkPage(c, request, response);
        } 
        else if (button.equals("submit_view"))
        {
            showListPage(c, request, response);
        }
        else if (button.equals("submit_base"))
        {
            showMainPage(c, request, response);
        } 
        else if (button.equals("submit_link"))
        {
            // do form validation before anything else
            if (validateAddForm(c, request, response))
            {
                addSupervisionOrder(c, request, response);
                showMainPage(c, request, response);
            }
        }
        else if (button.equals("submit_remove"))
        {
            showConfirmRemovePage(c, request, response);
        }
        else if (button.equals("submit_doremove"))
        {
            removeSupervisionOrder(c, request, response);
            showMainPage(c, request, response);
        }
    }
    
    //**********************************************************************
    //****************** Methods for Page display **************************
    //**********************************************************************
    
    /**
     * Confirms the removal of a supervision order
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void showConfirmRemovePage(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // get the values from the request
        int wsItemID = UIUtil.getIntParameter(request,"siID");
        UUID groupID = UIUtil.getUUIDParameter(request,"gID");
        
        // get the workspace item and the group from the request values
        WorkspaceItem wsItem = workspaceItemService.find(context, wsItemID);
        Group group = groupService.find(context, groupID);
        
        // set the attributes for the JSP
        request.setAttribute("wsItem",wsItem);
        request.setAttribute("group", group);
        
        JSPManager.showJSP(request, response, "/dspace-admin/supervise-confirm-remove.jsp" );
        
    }
    
    /**
     * Displays the form to link groups to workspace items
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void showLinkPage(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // get all the groups
        List<Group> groups = groupService.findAll(context,1);
        
        // get all the workspace items
        List<WorkspaceItem> wsItems = workspaceItemService.findAll(context);
        
        // set the attributes for the JSP
        request.setAttribute("groups",groups);
        request.setAttribute("wsItems",wsItems);

        // set error message key when there is no workspace item
        if (wsItems.size() == 0)
        {
            request.setAttribute("errorKey", 
                "jsp.dspace-admin.supervise-no-workspaceitem.no-wsitems");
            JSPManager.showJSP(request, response, 
                "/dspace-admin/supervise-no-workspaceitem.jsp");
            
        }
        else
        { 
            JSPManager.showJSP(request, response, "/dspace-admin/supervise-link.jsp" );
        }
    }
    
    /**
     * Displays the options you have in the supervisor admin area
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void showMainPage(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        JSPManager.showJSP(request, response, "/dspace-admin/supervise-main.jsp");
    }
    
    /**
     * Displays the list of current settings for supervisors
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private void showListPage(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // get all the supervised items
        List<WorkspaceItem> si = supervisedItemService.getAll(context);
        
        // set the attributes for the JSP
        request.setAttribute("supervised",si);
        
        JSPManager.showJSP(request, response, "/dspace-admin/supervise-list.jsp" );
    }
    
    //**********************************************************************
    //*************** Methods for data manipulation ************************
    //**********************************************************************
    
    /**
     * Adds supervisory settings to the database
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    void addSupervisionOrder(Context context, 
        HttpServletRequest request, HttpServletResponse response)
    throws SQLException, AuthorizeException, ServletException, IOException
    {
        
        // get the values from the request
        UUID groupID = UIUtil.getUUIDParameter(request,"TargetGroup");
        int wsItemID = UIUtil.getIntParameter(request,"TargetWSItem");
        int policyType = UIUtil.getIntParameter(request, "PolicyType");
        Group group = groupService.find(context, groupID);
        WorkspaceItem wi = workspaceItemService.find(context, wsItemID);
        supervisorService.add(context, group, wi, policyType);
        
        log.info(LogManager.getHeader(context, 
            "Supervision Order Set", 
            "workspace_item_id="+wsItemID+",eperson_group_id="+groupID));
        
        context.complete();
    }
    
    /**
     * Remove the supervisory group and its policies from the database
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    void removeSupervisionOrder(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws SQLException, AuthorizeException, ServletException, IOException
    {
        
        // get the values from the request
        int wsItemID = UIUtil.getIntParameter(request,"siID");
        UUID groupID = UIUtil.getUUIDParameter(request,"gID");
        
        WorkspaceItem wi = workspaceItemService.find(context, wsItemID);
        Group group = groupService.find(context, groupID);
        
        supervisorService.remove(context, wi, group);
        
        log.info(LogManager.getHeader(context, 
            "Supervision Order Removed", 
            "workspace_item_id="+wsItemID+",eperson_group_id="+groupID));
        
        context.complete();
    }
    
    /**
     * validate the submitted form to ensure that there is not a supervision
     * order for this already.
     *
     * @param context the context of the request
     * @param request the servlet request
     * @param response the servlet response
     */
    private boolean validateAddForm(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        UUID groupID = UIUtil.getUUIDParameter(request,"TargetGroup");
        int wsItemID = UIUtil.getIntParameter(request,"TargetWSItem");
        
        // set error message key when no workspace item is selected
        if (wsItemID == -1)
        {
            request.setAttribute("errorKey", 
                "jsp.dspace-admin.supervise-no-workspaceitem.unselected");
            JSPManager.showJSP(request, response, 
                "/dspace-admin/supervise-no-workspaceitem.jsp" );
            return false;
        }
        
        WorkspaceItem wi = workspaceItemService.find(context, wsItemID);
        Group group = groupService.find(context, groupID);

        boolean invalid = supervisorService.isOrder(context, wi, group);
        
        if (invalid)
        {
            JSPManager.showJSP(request, response, 
                "/dspace-admin/supervise-duplicate.jsp");
            return false;
        } 
        else
        {
            return true;
        }
    }
   
}
