/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.itemexport.ItemExportException;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.app.itemimport.BTEBatchImportService;
import org.dspace.app.itemimport.BatchUpload;
import org.dspace.app.itemimport.factory.ItemImportServiceFactory;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SupervisedItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;

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
    private static final Logger log = Logger.getLogger(MyDSpaceServlet.class);

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

    /** The "request export migrate archive for download" page */
    public static final int REQUEST_MIGRATE_ARCHIVE = 6;

    public static final int REQUEST_BATCH_IMPORT_ACTION = 7;
    
    private final transient WorkspaceItemService workspaceItemService
             = ContentServiceFactory.getInstance().getWorkspaceItemService();

    private final transient BasicWorkflowService workflowService
             = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowService();

    private final transient BasicWorkflowItemService workflowItemService
             = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowItemService();
    
    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
    
    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();
    
    private final transient ItemExportService itemExportService
             =  ItemExportServiceFactory.getInstance().getItemExportService();
    
    private final transient ItemImportService itemImportService
             = ItemImportServiceFactory.getInstance().getItemImportService();
    
    private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
    
    private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
    
    private final transient GroupService groupService
             = EPersonServiceFactory.getInstance().getGroupService();
    
    private final transient SupervisedItemService supervisedItemService
             =  ContentServiceFactory.getInstance().getSupervisedItemService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET displays the main page - everthing else is a POST
        showMainPage(context, request, response);
     }
    
    @Override
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
            processExportArchive(context, request, response, false);

            break;

        case REQUEST_MIGRATE_ARCHIVE:
            processExportArchive(context, request, response, true);

            break;

        case REQUEST_BATCH_IMPORT_ACTION:
            processBatchImportAction(context, request, response);

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
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_own");

        // Get workspace item, if any
        WorkspaceItem workspaceItem;

        try
        {
            int wsID = Integer.parseInt(request.getParameter("workspace_id"));
            workspaceItem = workspaceItemService.find(context, wsID);
        }
        catch (NumberFormatException nfe)
        {
            workspaceItem = null;
        }

        // Get workflow item specified, if any
        BasicWorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = workflowItemService.find(context, wfID);
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

                workflowService.unclaim(context, workflowItem, context
                        .getCurrentUser());

                showMainPage(context, request, response);
                context.complete();
                ok = true;
            }
        }

        if (!ok)
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
            workspaceItem = workspaceItemService.find(context, wsID);
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
            workspaceItemService.deleteAll(context, workspaceItem);
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
        BasicWorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = workflowItemService.find(context, wfID);
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
            workflowService.claim(context, workflowItem, context
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
        BasicWorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = workflowItemService.find(context, wfID);
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
            workflowService.advance(context, workflowItem, context
                    .getCurrentUser());

            // FIXME: This should be a return value from advance()
            // See if that gave the item a Handle. If it did,
            // the item made it into the archive, so we
            // should display a suitable page.
            String handle = handleService.findHandle(context, item);

            if (handle != null)
            {
                String displayHandle = handleService.getCanonicalForm(handle);

                request.setAttribute("handle", displayHandle);
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
            workflowService.unclaim(context, workflowItem, context
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
        BasicWorkflowItem workflowItem;

        try
        {
            int wfID = Integer.parseInt(request.getParameter("workflow_id"));
            workflowItem = workflowItemService.find(context, wfID);
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

            WorkspaceItem wsi = workflowService.sendWorkflowItemBackSubmission(context, workflowItem,
                    context.getCurrentUser(), null, reason);

            // Load the Submission Process for the collection this WSI is
            // associated with
            Collection c = wsi.getCollection();
            SubmissionConfigReader subConfigReader = new SubmissionConfigReader();
            SubmissionConfig subConfig = subConfigReader.getSubmissionConfig(c
                    .getHandle(), false);

            // Set the "stage_reached" column on the workspace item
            // to the LAST page of the LAST step in the submission process
            // (i.e. the page just before "Complete")
            int lastStep = subConfig.getNumberOfSteps() - 2;
            wsi.setStageReached(lastStep);
            wsi.setPageReached(AbstractProcessingStep.LAST_PAGE_REACHED);
            workspaceItemService.update(context, wsi);

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

    private void processExportArchive(Context context,
            HttpServletRequest request, HttpServletResponse response, boolean migrate) throws ServletException, IOException{
    	
    	if (request.getParameter("item_id") != null) {
			Item item = null;
			try {
				item = itemService.find(context, UUID.fromString(request
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
					itemExportService.createDownloadableExport(item, context, migrate);
				} catch (ItemExportException iee) {
                    log.warn(LogManager.getHeader(context, "export_too_large_error", UIUtil
		                    .getRequestLogInfo(request)));
		            JSPManager.showJSP(request, response, "/mydspace/export-error.jsp");
		            return;
                }
                catch (Exception e) {
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
				col = collectionService.find(context, UUID.fromString(request
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
					itemExportService.createDownloadableExport(col, context, migrate);
				} catch (ItemExportException iee) {
                    log.warn(LogManager.getHeader(context, "export_too_large_error", UIUtil
		                    .getRequestLogInfo(request)));
		            JSPManager.showJSP(request, response, "/mydspace/export-error.jsp");
		            return;
                }
                catch (Exception e) {
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
				com = communityService.find(context, UUID.fromString(request
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
					itemExportService.createDownloadableExport(com, context, migrate);
				} catch (ItemExportException iee) {
                    log.warn(LogManager.getHeader(context, "export_too_large_error", UIUtil
		                    .getRequestLogInfo(request)));
		            JSPManager.showJSP(request, response, "/mydspace/export-error.jsp");
		            return;
                }
                catch (Exception e) {
					log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
		                    .getRequestLogInfo(request)));
		            JSPManager.showIntegrityError(request, response);
		            return;
				}
			}
			JSPManager.showJSP(request, response, "/mydspace/task-complete.jsp");
		}
    	
    	
    }
    
    private void processBatchImportAction(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	String buttonPressed = UIUtil.getSubmitButton(request, "submit_mapfile");

    	String uploadId = request.getParameter("uploadid");
    	
    	if (buttonPressed.equals("submit_mapfile")){
    		
    		String mapFilePath = null;
    		try {
    			mapFilePath = itemImportService.getImportUploadableDirectory(context.getCurrentUser()) + File.separator + uploadId + File.separator + "mapfile";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				showMainPage(context, request, response);
				return;
			}
    		
    		File file = new File(mapFilePath);
    		int length   = 0;
    		ServletOutputStream outStream = response.getOutputStream();
    		String mimetype ="application/octet-stream";
    		
    		response.setContentType(mimetype);
    		response.setContentLength((int)file.length());
    		String fileName = file.getName();

    		// sets HTTP header
    		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

    		byte[] byteBuffer = new byte[1024];
    		DataInputStream in = new DataInputStream(new FileInputStream(file));

    		// reads the file's bytes and writes them to the response stream
    		while ((in != null) && ((length = in.read(byteBuffer)) != -1))
    		{
    			outStream.write(byteBuffer,0,length);
    		}

    		in.close();
    		outStream.close();
    	}
    	else if (buttonPressed.equals("submit_delete")){
    		try {
				itemImportService.deleteBatchUpload(context, uploadId);
				showMainPage(context, request, response);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else if (buttonPressed.equals("submit_resume")){
    		// Set attributes
            request.setAttribute("uploadId", uploadId);
        	
        	//Get all collections
    		List<Collection> collections = collectionService.findAll(context);
    		request.setAttribute("collections", collections);

    		//Get all the possible data loaders from the Spring configuration
    		BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
    		List<String> inputTypes = dls.getFileDataLoaders();
    		request.setAttribute("input-types", inputTypes);
    		
            // Forward to main mydspace page
            JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");
    	}
    	else {
    		showMainPage(context, request, response);
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

        List<BasicWorkflowItem> ownedList = workflowService.getOwnedTasks(context, currentUser);

        // Pooled workflow items
        List<BasicWorkflowItem> pooledList = workflowService.getPooledTasks(context, currentUser);

        // User's WorkflowItems
        List<BasicWorkflowItem> workflowItems = workflowItemService.findBySubmitter(context, currentUser);

        // User's PersonalWorkspace
        List<WorkspaceItem> workspaceItems = workspaceItemService.findByEPerson(context, currentUser);

        // User's authorization groups
        List<Group> memberships = groupService.allMemberGroups(context, currentUser);
        
        // Should the group memberships be displayed
        boolean displayMemberships = ConfigurationManager.getBooleanProperty("webui.mydspace.showgroupmemberships", false);

        List<WorkspaceItem> supervisedItems = supervisedItemService.findbyEPerson(
                context, currentUser);
        // export archives available for download
        List<String> exportArchives = null;
        try{
        	exportArchives = itemExportService.getExportsAvailable(currentUser);
        }
        catch (Exception e) {
			// nothing to do they just have no export archives available for download
		}
        
        // imports available for view
        List<BatchUpload> importUploads = null;
        try{
        	importUploads = itemImportService.getImportsAvailable(currentUser);
        }
        catch (Exception e) {
			// nothing to do they just have no export archives available for download
		}
        
        
        // Set attributes
        request.setAttribute("mydspace.user", currentUser);
        request.setAttribute("workspace.items", workspaceItems);
        request.setAttribute("workflow.items", workflowItems);
        request.setAttribute("workflow.owned", ownedList);
        request.setAttribute("workflow.pooled", pooledList);
        request.setAttribute("group.memberships", memberships);
        request.setAttribute("display.groupmemberships", displayMemberships);
        request.setAttribute("supervised.items", supervisedItems);
        request.setAttribute("export.archives", exportArchives);
        request.setAttribute("import.uploads", importUploads);

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
        // Turn the iterator into a list
        List<Item> subList = new LinkedList<>();
        Iterator<Item> subs = itemService.findBySubmitter(context, context
                .getCurrentUser());

        while (subs.hasNext())
        {
            subList.add(subs.next());
        }

        log.info(LogManager.getHeader(context, "view_own_submissions", "count="
                + subList.size()));

        request.setAttribute("user", context.getCurrentUser());
        request.setAttribute("items", subList);

        JSPManager.showJSP(request, response, "/mydspace/own-submissions.jsp");
    }
}
