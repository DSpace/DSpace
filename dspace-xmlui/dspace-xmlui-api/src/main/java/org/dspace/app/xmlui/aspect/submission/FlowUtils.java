/*
 * FlowUtils.java
 *
 * Version: $Revision: 1.21 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * This is a utility class to aid in the submission flow scripts. 
 * Since data validation is cumbersome inside a flow script this 
 * is a collection of methods to preform processing at each step 
 * of the flow, the flow script will ties these operations 
 * together in a meaningful order but all actually processing 
 * is done through these various processes.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (modified for Configurable Submission)
 */

public class FlowUtils {

    private static Logger log = Logger.getLogger(FlowUtils.class);
    
    /** Where the submissionInfo is stored on an HTTP Request object */
    private final static String DSPACE_SUBMISSION_INFO = "dspace.submission.info";

	/**
	 * Return the InProgressSubmission, either workspaceItem or workflowItem, 
	 * depending on the id provided. If the id begins with an S then it is a
	 * considered a workspaceItem. If the id begins with a W then it is
	 * considered a workflowItem.
	 * 
	 * @param context
	 * @param inProgressSubmissionID
	 * @return The InprogressSubmission or null if non found
	 */
	public static InProgressSubmission findSubmission(Context context, String inProgressSubmissionID) throws SQLException
	{
		char type = inProgressSubmissionID.charAt(0);
		int id = Integer.valueOf(inProgressSubmissionID.substring(1));
		
		if (type == 'S')
		{
			return WorkspaceItem.find(context, id);
		}
		else if (type == 'W')
		{
			return WorkflowItem.find(context, id);
		}
		return null;
	}
    
    
	/**
	 * Return the workspace identified by the given id, the id should be 
	 * prepended with the character S to signify that it is a workspace 
	 * instead of a workflow.
	 * 
	 * @param context
	 * @param inProgressSubmissionID
	 * @return The found workspaceitem or null if none found.
	 */
	public static WorkspaceItem findWorkspace(Context context, String inProgressSubmissionID) throws SQLException
	{
		InProgressSubmission submission = findSubmission(context, inProgressSubmissionID);
		if (submission instanceof WorkspaceItem)
			return (WorkspaceItem) submission;
		return null;
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
	public static WorkflowItem findWorkflow(Context context, String inProgressSubmissionID) throws SQLException
	{
		InProgressSubmission submission = findSubmission(context, inProgressSubmissionID);
		if (submission instanceof WorkflowItem)
			return (WorkflowItem) submission;
		return null;
	}
	
    /**
     * Obtains the submission info for the current submission process. 
     * If a submissionInfo object has already been created
     * for this HTTP request, it is re-used, otherwise it is created.
     * 
     * @param objectModel
     *            the cocoon Objectmodel
     * @param workspaceID
     *            the workspaceID of the submission info to obtain         
     * 
     * @return a SubmissionInfo object
     */
    public static SubmissionInfo obtainSubmissionInfo(Map objectModel, String workspaceID) throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);
          
        //try loading subInfo from HTTP request
        SubmissionInfo subInfo = (SubmissionInfo) request.getAttribute(DSPACE_SUBMISSION_INFO);
        
        //get the submission represented by the WorkspaceID
        InProgressSubmission submission = findSubmission(context, workspaceID);

        //if no submission info, or wrong submission info, reload it!
        if ((subInfo == null && submission!=null) || 
            (subInfo!=null && submission!=null && subInfo.getSubmissionItem().getID()!=submission.getID()))
        {
            try
            {
                final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
                    .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
            
                // load submission info
                subInfo = SubmissionInfo.load(httpRequest, submission);
    
                // Set the session ID
                context.setExtraLogInfo("session_id="
                        + request.getSession().getId());
    
                // Store the submissionInfo in the request
                request.setAttribute(DSPACE_SUBMISSION_INFO, subInfo);
            }
            catch(Exception e)
            {
                throw new SQLException("Error loading Submission Info: " + e.getMessage());
            }
        }    
        else if(subInfo==null && submission==null)
        {
            throw new SQLException("Unable to load Submission Information, since WorkspaceID (ID:" + workspaceID + ") is not a valid in-process submission.");
        }

        return subInfo;
    }
	
	/**
     * Indicate the user has advanced to the given page within a given step. 
     * This will only actually do anything when it's a user initially entering 
     * a submission. It will increase the "stage reached" and "page reached"
     * columns - it will not "set back" where a user has reached.
     * 
     * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
     * @param step the step the user has just reached
     * @param page the page (within the step) the user has just reached
     */
    public static void setPageReached(Context context, String id, int step, int page)
            throws SQLException, AuthorizeException, IOException
    {
        InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			
			if (step > workspaceItem.getStageReached())
			{
				workspaceItem.setStageReached(step);
				workspaceItem.setPageReached(1);  //reset page to first page in new step
				workspaceItem.update();
				context.commit();
			}
			else if ((step == workspaceItem.getStageReached()) &&
					 (page > workspaceItem.getPageReached()))
			{
				workspaceItem.setPageReached(page);
				workspaceItem.update();
				context.commit();
			}
		}
    }
	
    /**
     * Find the maximum step the user has reached in the submission processes. 
     * If this submission is a workflow then return max-int.
     * 
     * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
     */
	public static int getMaximumStepReached(Context context, String id) throws SQLException {
		
		InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			int stage = workspaceItem.getStageReached();
			if (stage < 0)
				stage = 0;
			return stage;
		}
		
		// This is a workflow, return infinity.
		return Integer.MAX_VALUE;
	}
	
	/**
     * Find the maximum page (within the maximum step) that the user has 
     * reached in the submission processes. 
     * If this submission is a workflow then return max-int.
     * 
     * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
     */
	public static int getMaximumPageReached(Context context, String id) throws SQLException {
		
		InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			int page = workspaceItem.getPageReached();
			if (page < 0)
				page = 0;
			return page;
		}
		
		// This is a workflow, return infinity.
		return Integer.MAX_VALUE;
	}
	
	
	/**
	 * Get current step number
	 *
	 * @param stepAndPage
	 * 			a double representing the current step and page
	 * 			(e.g. 1.2 is page 2 of step 1)
	 * @return step number
	 */
	public static int getStep(double stepAndPage)
	{
		//split step and page (e.g. 1.2 is page 2 of step 1)
		String[] fields = Double.toString(stepAndPage).split("\\."); // split on period
        
		return Integer.parseInt(fields[0]);
	}
	
	
	/**
	 * Get number of the current page within the current step
	 *
	 *@param stepAndPage
	 * 			a double representing the current step and page
	 * 			(e.g. 1.2 is page 2 of step 1)
	 * @return page number (within current step)
	 */
	public static int getPage(double stepAndPage)
	{
		//split step and page (e.g. 1.2 is page 2 of step 1)
		String[] fields = Double.toString(stepAndPage).split("\\."); // split on period
        
		return Integer.parseInt(fields[1]);
	}
	
	/**
	 * Process the save or remove step. If the user has selected to 
	 * remove their submission then remove it.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace/workflow
	 * @param request The cocoon request object.
	 */
	public static void processSaveOrRemove(Context context, String id, Request request) throws SQLException, AuthorizeException, IOException
	{
		if (request.getParameter("submit_remove") != null)
		{
			// If they selected to remove the item then delete everything.
			WorkspaceItem workspace = findWorkspace(context,id);
			workspace.deleteAll();
	        context.commit();
		}
	}
	
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
	 * to select and preform.
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
        
        //Log this unclaim action
        log.info(LogManager.getHeader(context, "unclaim_workflow",
                "workflow_item_id=" + workflowItem.getID() + ",item_id="
                        + workflowItem.getItem().getID() + ",collection_id="
                        + workflowItem.getCollection().getID() 
                        + ",new_state=" + workflowItem.getState()));
	}
	
	/**
	 * Claim this task from the pool of unclaimed task so that this user may
	 * preform the task by either approving or rejecting it.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static void processClaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
		
       // Claim the task
       WorkflowManager.claim(context, workflowItem, context.getCurrentUser());
       
       context.commit();
       
       //log this claim information
       log.info(LogManager.getHeader(context, "claim_task", "workflow_item_id="
                   + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                   + "collection_id=" + workflowItem.getCollection().getID()
                   + "newowner_id=" + workflowItem.getOwner().getID() 
                   + "new_state=" + workflowItem.getState()));
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
			
			//Load the Submission Process for the collection this WSI is associated with
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
            
            //Submission rejected.  Log this information
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
	 * Return the HTML / DRI field name for the given input.
	 * 
	 * @param input
	 * @return field name as a String (e.g. dc_contributor_editor)
	 */
	public static String getFieldName(DCInput input)
	{
		String dcSchema = input.getSchema();
		String dcElement = input.getElement();
		String dcQualifier = input.getQualifier();
		if (dcQualifier != null && ! dcQualifier.equals(Item.ANY))
		{
			return dcSchema + "_" + dcElement + '_' + dcQualifier;
		}
		else
		{
			return dcSchema + "_" + dcElement;
		}

	}
    
    /**
     * Retrieves a list of all steps and pages within the
     * current submission process.
     * <P>
     * This returns an array of Doubles of the form "#.#"
     * where the former number is the step number, and the
     * latter is the page number.
     * <P>
     * This list may differ from the list of steps in the
     * progress bar if the current submission process includes
     * non-interactive steps which do not appear in the progress bar!
     * <P>
     * This method is used by the Manakin submission flowscript
     * (submission.js) to step forward/backward between steps. 
     * 
     * @param request
     *            The HTTP Servlet Request object
     * @param subInfo
     *            the current SubmissionInfo object
     *  
     */
    public static Double[] getListOfAllSteps(HttpServletRequest request, SubmissionInfo subInfo)
    {
        ArrayList listStepNumbers = new ArrayList();

        // loop through all steps
        for (int i = 0; i < subInfo.getSubmissionConfig().getNumberOfSteps(); i++)
        {
            // get the current step info
            SubmissionStepConfig currentStep = subInfo.getSubmissionConfig()
                    .getStep(i);
            String stepNumber = Integer.toString(currentStep
                    .getStepNumber());
            
            //Skip over the "Select Collection" step, since
            // a user is never allowed to return to that step or jump from that step
            if(currentStep.getId()!=null && currentStep.getId().equals(SubmissionStepConfig.SELECT_COLLECTION_STEP))
            {
                continue;
            }
            
            // default to just one page in this step
            int numPages = 1;

            try
            {
                // load the processing class for this step
                ClassLoader loader = subInfo.getClass().getClassLoader();
                Class stepClass = loader.loadClass(currentStep.getProcessingClassName());
   
                // call the "getNumberOfPages()" method of the class 
                // to get it's number of pages
                AbstractProcessingStep step = (AbstractProcessingStep) stepClass
                        .newInstance();

                // get number of pages from servlet
                numPages = step.getNumberOfPages(request, subInfo);
            }
            catch (Exception e)
            {
                log.error("Error loading step information from Step Class '"
                                + currentStep.getProcessingClassName()
                                + "' Error:", e);
            }

            // save each of the step's pages to the progress bar
            for (int j = 1; j <= numPages; j++)
            {
                String stepAndPage = stepNumber + "." + j;

                Double stepAndPageNum = Double.valueOf(stepAndPage);
                
                listStepNumbers.add(stepAndPageNum);
            }// end for each page
        }// end for each step

        //convert into an array of Doubles
        return (Double[]) listStepNumbers.toArray(new Double[listStepNumbers.size()]);
    }
}
