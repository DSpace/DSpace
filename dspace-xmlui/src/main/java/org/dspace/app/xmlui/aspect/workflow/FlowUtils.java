/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.workflow;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.util.*;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * This is a utility class to aid in the workflow flow scripts.
 * Since data validation is cumbersome inside a flow script this
 * is a collection of methods to perform processing at each step
 * of the flow, the flow script will ties these operations
 * together in a meaningful order but all actually processing
 * is done through these various processes.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */

public class FlowUtils {

    private static final Logger log = Logger.getLogger(FlowUtils.class);

   	/**
	 * Update the provided workflowItem to advance to the next workflow
	 * step. If this was the last thing needed before the item is
	 * committed to the repository then return true, otherwise false.
	 *
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static boolean processApproveTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
		Item item = workflowItem.getItem();

		// Advance the item along the workflow
        WorkflowManager.advance(context, workflowItem, context.getCurrentUser());

        // FIXME: This should be a return value from advance()
        // See if that gave the item a Handle. If it did,
        // the item made it into the archive, so we
        // should display a suitable page.
        String handle = HandleManager.findHandle(context, item);

        context.commit();

        if (handle != null)
        {
            return true;
        }
        else
        {
            return false;
        }
	}



	/**
	 * Return the given task back to the pool of unclaimed tasks for another user
	 * to select and perform.
	 *
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static void processUnclaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);

        // Return task to pool
        WorkflowManager.unclaim(context, workflowItem, context.getCurrentUser());

        context.commit();

        // Log this unclaim action
        log.info(LogManager.getHeader(context, "unclaim_workflow",
                "workflow_item_id=" + workflowItem.getID() + ",item_id="
                        + workflowItem.getItem().getID() + ",collection_id="
                        + workflowItem.getCollection().getID()
                        + ",new_state=" + workflowItem.getState()));
	}

	/**
	 * Claim this task from the pool of unclaimed task so that this user may
	 * perform the task by either approving or rejecting it.
	 *
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static void processClaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
        if(workflowItem.getState() != WorkflowManager.WFSTATE_STEP1POOL &&
                workflowItem.getState() != WorkflowManager.WFSTATE_STEP2POOL &&
                workflowItem.getState() != WorkflowManager.WFSTATE_STEP3POOL){
            // Only allow tasks in the pool to be claimed !
            throw new AuthorizeException("Error while claiming task: this task has already been claimed !");
        }

       // Claim the task
       WorkflowManager.claim(context, workflowItem, context.getCurrentUser());

       context.commit();

       // log this claim information
       log.info(LogManager.getHeader(context, "claim_task", "workflow_item_id="
                   + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                   + "collection_id=" + workflowItem.getCollection().getID()
                   + "newowner_id=" + workflowItem.getOwner().getID()
                   + "new_state=" + workflowItem.getState()));
	}

    /**
     * Verifies if the currently logged in user has proper rights to perform the workflow task on the item
     * @param context the current dspace context
     * @param workflowItemId the identifier of the workflow item
     * @throws org.dspace.authorize.AuthorizeException thrown if the user doesn't have sufficient rights to perform the task at hand
     * @throws java.sql.SQLException is thrown when something is wrong with the database
     */
    public static void authorizeWorkflowItem(Context context, String workflowItemId) throws AuthorizeException, SQLException {
        WorkflowItem workflowItem = WorkflowItem.find(context, Integer.parseInt(workflowItemId.substring(1)));
        if((workflowItem.getState() == WorkflowManager.WFSTATE_STEP1 ||
                workflowItem.getState() == WorkflowManager.WFSTATE_STEP2 ||
                workflowItem.getState() == WorkflowManager.WFSTATE_STEP3) && workflowItem.getOwner().getID() != context.getCurrentUser().getID()){
            throw new AuthorizeException("You are not allowed to perform this task.");
        }else
        if((workflowItem.getState() == WorkflowManager.WFSTATE_STEP1POOL ||
                workflowItem.getState() == WorkflowManager.WFSTATE_STEP2POOL ||
                workflowItem.getState() == WorkflowManager.WFSTATE_STEP3POOL)){
            // Verify if the current user has the current workflowItem among his pooled tasks
            boolean hasPooledTask = false;
            List<WorkflowItem> pooledTasks = WorkflowManager.getPooledTasks(context, context.getCurrentUser());
            for (WorkflowItem pooledItem : pooledTasks) {
                if(pooledItem.getID() == workflowItem.getID()){
                    hasPooledTask = true;
                }
            }
            if(!hasPooledTask){
                throw new AuthorizeException("You are not allowed to perform this task.");
            }

        }
    }


	/**
	 * Reject the given task for the given reason. If the user did not provide
	 * a reason then an error is generated placing that field in error.
	 *
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
     * @param request The current request object
	 */
	public static String processRejectTask(Context context, String id,Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);

		String reason = request.getParameter("reason");

		if (reason != null && reason.length() > 1)
		{
            WorkspaceItem wsi = WorkflowManager.reject(context, workflowItem,context.getCurrentUser(), reason);

            // Load the Submission Process for the collection this WSI is associated with
            Collection c = wsi.getCollection();
            SubmissionConfigReader subConfigReader = new SubmissionConfigReader();
            SubmissionConfig subConfig = subConfigReader.getSubmissionConfig(c.getHandle(), false);

            // Set the "stage_reached" column on the workspace item
            // to the LAST page of the LAST step in the submission process
            // (i.e. the page just before "Complete", which is at NumSteps-1)
            int lastStep = subConfig.getNumberOfSteps()-2;
            wsi.setStageReached(lastStep);
            wsi.setPageReached(AbstractProcessingStep.LAST_PAGE_REACHED);
            wsi.update();

            context.commit();

            // Submission rejected.  Log this information
            log.info(LogManager.getHeader(context, "reject_workflow", "workflow_item_id="
                    + wsi.getID() + "item_id=" + wsi.getItem().getID()
                    + "collection_id=" + wsi.getCollection().getID()
                    + "eperson_id=" + context.getCurrentUser().getID()));

			// Return no errors.
			return null;
		}
		else
		{
			// If the user did not supply a reason then
			// place the reason field in error.
			return "reason";
		}
	}

    /**
     * Return the workflow identified by the given id, the id should be
     * prepended with the character S to signify that it is a workflow
     * instead of a workspace.
     *
     * @param context
     * @param inProgressSubmissionID
     * @return The found workflowitem or null if none found.
     */
    public static WorkflowItem findWorkflow(Context context, String inProgressSubmissionID) throws SQLException, AuthorizeException, IOException {
        int id = Integer.valueOf(inProgressSubmissionID.substring(1));
        return WorkflowItem.find(context, id);
    }
}
