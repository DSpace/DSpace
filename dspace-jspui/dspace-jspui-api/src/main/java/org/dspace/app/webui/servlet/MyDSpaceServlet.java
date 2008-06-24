/*
 * MyDSpaceServlet.java
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
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.itemexport.ItemExport;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.SupervisedItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.dao.WorkflowItemDAO;
import org.dspace.workflow.dao.WorkflowItemDAOFactory;
import org.dspace.uri.IdentifierService;

/**
 * Servlet for constructing the components of the "My DSpace" page
 * 
 * @author Robert Tansley
 * @author Jay Paz
 * @version $Id$
 */
public class MyDSpaceServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(MyDSpaceServlet.class);

    /** The main screen */
    public static final int MAIN_PAGE = 0;

    /** The remove item page */
    public static final int REMOVE_ITEM_PAGE = 1;

    /** The preview task page */
    public static final int PREVIEW_TASK_PAGE = 2;

    /** The perform task page */
    public static final int PERFORM_TASK_PAGE = 3;

    /** The "reason for rejection" page */
    public static final int REJECT_REASON_PAGE = 4;
    
    /** The "request export archive for download" page */
    public static final int REQUEST_EXPORT_ARCHIVE = 5;

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET displays the main page - everthing else is a POST
        showMainPage(context, request, response);
     }
    
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // First get the step
        int step = UIUtil.getIntParameter(request, "step");

        switch (step)
        {
        case MAIN_PAGE:
            processMainPage(context, request, response);

            break;

        case REMOVE_ITEM_PAGE:
            processRemoveItem(context, request, response);

            break;

        case PREVIEW_TASK_PAGE:
            processPreviewTask(context, request, response);

            break;

        case PERFORM_TASK_PAGE:
            processPerformTask(context, request, response);

            break;

        case REJECT_REASON_PAGE:
        	processRejectReason(context, request, response);

        	break;
        case REQUEST_EXPORT_ARCHIVE:
        	processExportArchive(context, request, response);

        	break;
        default:
        	log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
        			.getRequestLogInfo(request)));
        JSPManager.showIntegrityError(request, response);
        }
    }

    // ****************************************************************
    // ****************************************************************
    // METHODS FOR PROCESSING POSTED FORMS
    // ****************************************************************
    // ****************************************************************
    protected void processMainPage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        WorkflowItemDAO wfDAO = WorkflowItemDAOFactory.getInstance(context);
        WorkspaceItemDAO wsDAO = WorkspaceItemDAOFactory.getInstance(context);

        String buttonPressed = UIUtil.getSubmitButton(request, "submit_own");

        // Get workspace item, if any
        WorkspaceItem workspaceItem;

        try
        {
            int wsID = Integer.parseInt(request.getParameter("workspace_id"));
            workspaceItem = wsDAO.retrieve(wsID);
        }
        catch (NumberFormatException nfe)
        {
            workspaceItem = null;
        }

        // Get workflow item specified, if any
        WorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = wfDAO.retrieve(wfID);
        }
        catch (NumberFormatException nfe)
        {
            workflowItem = null;
        }

        // Respond to button press
        boolean ok = false;

        if (buttonPressed.equals("submit_new"))
        {
            // New submission: Redirect to submit
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/submit"));
            ok = true;
        }
        else if (buttonPressed.equals("submit_own"))
        {
            // Review own submissions
            showPreviousSubmissions(context, request, response);
            ok = true;
        }
        else if (buttonPressed.equals("submit_resume"))
        {
            // User clicked on a "Resume" button for a workspace item.
            String wsID = request.getParameter("workspace_id");
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/submit?resume=" + wsID));
            ok = true;
        }
        else if (buttonPressed.equals("submit_delete"))
        {
            // User clicked on a "Remove" button for a workspace item
            if (workspaceItem != null)
            {
                log.info(LogManager.getHeader(context, "confirm_removal",
                        "workspace_item_id=" + workspaceItem.getID()));

                request.setAttribute("workspace.item", workspaceItem);
                JSPManager.showJSP(request, response,
                        "/mydspace/remove-item.jsp");
            }
            else
            {
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
            }

            ok = true;
        }
        else if (buttonPressed.equals("submit_claim"))
        {
            // User clicked "take task" button on workflow task
            if (workflowItem != null)
            {
                log.info(LogManager.getHeader(context, "view_workflow",
                        "workflow_id=" + workflowItem.getID()));

                request.setAttribute("workflow.item", workflowItem);
                JSPManager.showJSP(request, response,
                        "/mydspace/preview-task.jsp");
                ok = true;
            }
        }
        else if (buttonPressed.equals("submit_perform"))
        {
            // User clicked "Do This Task" button on workflow task
            if (workflowItem != null)
            {
                log.info(LogManager.getHeader(context, "view_workflow",
                        "workflow_id=" + workflowItem.getID()));

                request.setAttribute("workflow.item", workflowItem);
                JSPManager.showJSP(request, response,
                        "/mydspace/perform-task.jsp");
                ok = true;
            }
        }
        else if (buttonPressed.equals("submit_return"))
        {
            // User clicked "Return to pool" button on workflow task
            if (workflowItem != null)
            {
                log.info(LogManager.getHeader(context, "unclaim_workflow",
                        "workflow_id=" + workflowItem.getID()));

                WorkflowManager.unclaim(context, workflowItem, context
                        .getCurrentUser());

                showMainPage(context, request, response);
                context.complete();
                ok = true;
            }
        }

        if (ok == false)
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Process input from remove item page
     * 
     * @param context
     *            current context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processRemoveItem(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_cancel");

        // Get workspace item
        WorkspaceItem workspaceItem;

        try
        {
            int wsID = Integer.parseInt(request.getParameter("workspace_id"));
            workspaceItem = WorkspaceItem.find(context, wsID);
        }
        catch (NumberFormatException nfe)
        {
            workspaceItem = null;
        }

        if (workspaceItem == null)
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);

            return;
        }

        // We have a workspace item
        if (buttonPressed.equals("submit_delete"))
        {
            // User has clicked on "delete"
            log.info(LogManager.getHeader(context, "remove_submission",
                    "workspace_item_id=" + workspaceItem.getID() + ",item_id="
                            + workspaceItem.getItem().getID()));
            workspaceItem.deleteAll();
            showMainPage(context, request, response);
            context.complete();
        }
        else 
        {
            // User has cancelled. Back to main page.
            showMainPage(context, request, response);
        }

    }

    /**
     * Process input from preview task page
     * 
     * @param context
     *            current context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processPreviewTask(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_cancel");

        // Get workflow item
        WorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = WorkflowItem.find(context, wfID);
        }
        catch (NumberFormatException nfe)
        {
            workflowItem = null;
        }

        if (workflowItem == null)
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);

            return;
        }

        if (buttonPressed.equals("submit_start"))
        {
            // User clicked "start" button to claim the task
            WorkflowManager.claim(context, workflowItem, context
                    .getCurrentUser());

            // Display "perform task" page
            request.setAttribute("workflow.item", workflowItem);
            JSPManager.showJSP(request, response, "/mydspace/perform-task.jsp");
            context.complete();
        }
        else
        {
            // Return them to main page
            showMainPage(context, request, response);
        }
    }

    /**
     * Process input from perform task page
     * 
     * @param context
     *            current context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processPerformTask(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_cancel");

        // Get workflow item
        WorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = WorkflowItem.find(context, wfID);
        }
        catch (NumberFormatException nfe)
        {
            workflowItem = null;
        }

        if (workflowItem == null)
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);

            return;
        }

        if (buttonPressed.equals("submit_approve"))
        {
            Item item = workflowItem.getItem();

            // Advance the item along the workflow
            WorkflowManager.advance(context, workflowItem, context
                    .getCurrentUser());

            if (item.isArchived())
            {
                String uri = IdentifierService.getURL(item).toString();

                request.setAttribute("identifier", uri);
                JSPManager.showJSP(request, response,
                        "/mydspace/in-archive.jsp");
            }
            else
            {
                JSPManager.showJSP(request, response,
                        "/mydspace/task-complete.jsp");
            }

            context.complete();
        }
        else if (buttonPressed.equals("submit_reject"))
        {
            // Submission rejected. Ask the user for a reason
            log.info(LogManager.getHeader(context, "get_reject_reason",
                    "workflow_id=" + workflowItem.getID() + ",item_id="
                            + workflowItem.getItem().getID()));

            request.setAttribute("workflow.item", workflowItem);
            JSPManager
                    .showJSP(request, response, "/mydspace/reject-reason.jsp");
        }
        else if (buttonPressed.equals("submit_edit"))
        {
            // FIXME: Check auth
            log.info(LogManager.getHeader(context, "edit_workflow_item",
                    "workflow_id=" + workflowItem.getID() + ",item_id="
                            + workflowItem.getItem().getID()));

            // Forward control to the submission interface
            // with the relevant item
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/submit?workflow=" + workflowItem.getID()));
        }
        else if (buttonPressed.equals("submit_pool"))
        {
            // Return task to pool
            WorkflowManager.unclaim(context, workflowItem, context
                    .getCurrentUser());
            showMainPage(context, request, response);
            context.complete();
        }
        else
        {
            // Cancelled. The user hasn't taken the task.
            // Just return to the main My DSpace page.
            showMainPage(context, request, response);
        }
    }

    /**
     * Process input from "reason for rejection" page
     * 
     * @param context
     *            current context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processRejectReason(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_cancel");

        // Get workflow item
        WorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = WorkflowItem.find(context, wfID);
        }
        catch (NumberFormatException nfe)
        {
            workflowItem = null;
        }

        if (workflowItem == null)
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);

            return;
        }

        if (buttonPressed.equals("submit_send"))
        {
            String reason = request.getParameter("reason");

            WorkspaceItem wsi = WorkflowManager.reject(context, workflowItem,
                    context.getCurrentUser(), reason);

            // Load the Submission Process for the collection this WSI is
            // associated with
            Collection c = wsi.getCollection();
            SubmissionConfigReader subConfigReader = new SubmissionConfigReader();
            SubmissionConfig subConfig = subConfigReader.getSubmissionConfig(c
                    .getIdentifier().getCanonicalForm(), false);

            // Set the "stage_reached" column on the workspace item
            // to the LAST page of the LAST step in the submission process
            // (i.e. the page just before "Complete")
            int lastStep = subConfig.getNumberOfSteps() - 2;
            wsi.setStageReached(lastStep);
            wsi.setPageReached(AbstractProcessingStep.LAST_PAGE_REACHED);
            wsi.update();

            JSPManager
                    .showJSP(request, response, "/mydspace/task-complete.jsp");
            context.complete();
        }
        else
        {
            request.setAttribute("workflow.item", workflowItem);
            JSPManager.showJSP(request, response, "/mydspace/perform-task.jsp");
        }
    }

    // ****************************************************************
    // ****************************************************************
    // METHODS FOR SHOWING FORMS
    // ****************************************************************
    // ****************************************************************

    /**
     * Show the main My DSpace page
     * 
     * @param context
     *            current context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void showMainPage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "view_mydspace", ""));
        EPerson currentUser = context.getCurrentUser();

        // FIXME: WorkflowManager should return arrays
        List ownedList = WorkflowManager.getOwnedTasks(context, currentUser);
        WorkflowItem[] owned = new WorkflowItem[ownedList.size()];
        owned = (WorkflowItem[]) ownedList.toArray(owned);

        // Pooled workflow items
        List pooledList = WorkflowManager.getPooledTasks(context, currentUser);
        WorkflowItem[] pooled = new WorkflowItem[pooledList.size()];
        pooled = (WorkflowItem[]) pooledList.toArray(pooled);

        // User's WorkflowItems
        WorkflowItem[] workflowItems = WorkflowItem.findByEPerson(context, currentUser);

        // User's PersonalWorkspace
        WorkspaceItem[] workspaceItems = WorkspaceItem.findByEPerson(context, currentUser);

        // User's authorization groups
        Group[] memberships = Group.allMemberGroups(context, currentUser);
        
        // Should the group memberships be displayed
        boolean displayMemberships = ConfigurationManager.getBooleanProperty("webui.mydspace.showgroupmemberships", false);

        SupervisedItem[] supervisedItems = SupervisedItem.findbyEPerson(
                context, currentUser);

        // Set attributes
        request.setAttribute("mydspace.user", currentUser);
        request.setAttribute("workspace.items", workspaceItems);
        request.setAttribute("workflow.items", workflowItems);
        request.setAttribute("workflow.owned", owned);
        request.setAttribute("workflow.pooled", pooled);
        request.setAttribute("group.memberships", memberships);
        request.setAttribute("display.groupmemberships", new Boolean(displayMemberships));
        request.setAttribute("supervised.items", supervisedItems);

        // Forward to main mydspace page
        JSPManager.showJSP(request, response, "/mydspace/main.jsp");
    }

    /**
     * Show the user's previous submissions.
     * 
     * @param context
     *            current context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void showPreviousSubmissions(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        ItemIterator subs = Item.findBySubmitter(context, context
                .getCurrentUser());

        // Turn the iterator into a list
        List subList = new LinkedList();

        while (subs.hasNext())
        {
            subList.add(subs.next());
        }

        Item[] items = new Item[subList.size()];

        for (int i = 0; i < subList.size(); i++)
        {
            items[i] = (Item) subList.get(i);
        }

        log.info(LogManager.getHeader(context, "view_own_submissions", "count="
                + items.length));

        request.setAttribute("user", context.getCurrentUser());
        request.setAttribute("items", items);

        JSPManager.showJSP(request, response, "/mydspace/own-submissions.jsp");
    }

    private void processExportArchive(Context context,
    		HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

    	if (request.getParameter("item_id") != null) {
    		Item item = null;
    		try {
    			item = Item.find(context, Integer.parseInt(request
    					.getParameter("item_id")));
    		} catch (Exception e) {
    			log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    					.getRequestLogInfo(request)));
    			JSPManager.showIntegrityError(request, response);
    			return;
    		}

    		if (item == null) {
    			log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    					.getRequestLogInfo(request)));
    			JSPManager.showIntegrityError(request, response);
    			return;
    		} else {
    			try {
    				ItemExport.createDownloadableExport(item, context);
    			} catch (Exception e) {
    				log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    						.getRequestLogInfo(request)));
    				JSPManager.showIntegrityError(request, response);
    				return;
    			}
    		}

    		// success
    		JSPManager.showJSP(request, response, "/mydspace/task-complete.jsp");
    	} else if (request.getParameter("collection_id") != null) {
    		Collection col = null;
    		try {
    			col = Collection.find(context, Integer.parseInt(request
    					.getParameter("collection_id")));
    		} catch (Exception e) {
    			log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    					.getRequestLogInfo(request)));
    			JSPManager.showIntegrityError(request, response);
    			return;
    		}

    		if (col == null) {
    			log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    					.getRequestLogInfo(request)));
    			JSPManager.showIntegrityError(request, response);
    			return;
    		} else {
    			try {
    				ItemExport.createDownloadableExport(col, context);
    			} catch (Exception e) {
    				log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    						.getRequestLogInfo(request)));
    				JSPManager.showIntegrityError(request, response);
    				return;
    			}
    		}
    		JSPManager.showJSP(request, response, "/mydspace/task-complete.jsp");
    	} else if (request.getParameter("community_id") != null) {
    		Community com = null;
    		try {
    			com = Community.find(context, Integer.parseInt(request
    					.getParameter("community_id")));
    		} catch (Exception e) {
    			log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    					.getRequestLogInfo(request)));
    			JSPManager.showIntegrityError(request, response);
    			return;
    		}

    		if (com == null) {
    			log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    					.getRequestLogInfo(request)));
    			JSPManager.showIntegrityError(request, response);
    			return;
    		} else {
    			try {
    				org.dspace.app.itemexport.ItemExport.createDownloadableExport(com, context);
    			} catch (Exception e) {
    				log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
    						.getRequestLogInfo(request)));
    				JSPManager.showIntegrityError(request, response);
    				return;
    			}
    		}
    		JSPManager.showJSP(request, response, "/mydspace/task-complete.jsp");
    	}


    }
}
