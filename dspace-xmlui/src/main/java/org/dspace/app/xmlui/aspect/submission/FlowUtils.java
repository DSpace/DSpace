/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

/**
 * This is a utility class to aid in the submission flow scripts. 
 * Since data validation is cumbersome inside a flow script this 
 * is a collection of methods to perform processing at each step 
 * of the flow, the flow script will ties these operations 
 * together in a meaningful order but all actually processing 
 * is done through these various processes.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (modified for Configurable Submission)
 */

public class FlowUtils {

    private static final Logger log = Logger.getLogger(FlowUtils.class);
    
    /** Where the submissionInfo is stored on an HTTP Request object */
    private static final String DSPACE_SUBMISSION_INFO = "dspace.submission.info";

    protected static final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected static final WorkflowItemService workflowService = WorkflowServiceFactory.getInstance().getWorkflowItemService();

	/**
	 * Return the InProgressSubmission, either workspaceItem or workflowItem,
	 * depending on the id provided. If the id begins with an S then it is a
	 * considered a workspaceItem. If the id begins with a W then it is
	 * considered a workflowItem.
	 *
	 * @param context session context.
	 * @param inProgressSubmissionID the submission's ID.
	 * @return The InprogressSubmission or null if non found
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static InProgressSubmission findSubmission(Context context, String inProgressSubmissionID)
            throws SQLException, AuthorizeException, IOException {
		char type = inProgressSubmissionID.charAt(0);
		int id = Integer.valueOf(inProgressSubmissionID.substring(1));
		
		if (type == 'S')
		{
			return workspaceItemService.find(context, id);
		}
		else if (type == 'W' || type == 'X') {
            return workflowService.find(context, id);
        }
		return null;
	}

	/**
	 * Return the workspace identified by the given id, the id should be 
	 * prepended with the character S to signify that it is a workspace 
	 * instead of a workflow.
	 * 
	 * @param context session context.
	 * @param inProgressSubmissionID identifier of the submission.
	 * @return The found workspace item, or null if none found.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static WorkspaceItem findWorkspace(Context context, String inProgressSubmissionID)
            throws SQLException, AuthorizeException, IOException {
		InProgressSubmission submission = findSubmission(context, inProgressSubmissionID);
		if (submission instanceof WorkspaceItem)
        {
            return (WorkspaceItem) submission;
        }
		return null;
	}

	/**
     * Obtains the submission info for the current submission process.
     * If a submissionInfo object has already been created
     * for this HTTP request, it is re-used, otherwise it is created.
     *
     * @param objectModel
     *            the Cocoon object model
     * @param workspaceID
     *            the workspaceID of the submission info to obtain         
     * 
     * @return a SubmissionInfo object.
     * @throws java.sql.SQLException on error.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static SubmissionInfo obtainSubmissionInfo(Map objectModel, String workspaceID)
            throws SQLException, IOException, AuthorizeException {
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
                throw new SQLException("Error loading Submission Info: " + e.getMessage(), e);
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
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
				workspaceItemService.update(context, workspaceItem);
			}
			else if ((step == workspaceItem.getStageReached()) &&
					 (page > workspaceItem.getPageReached()))
			{
				workspaceItem.setPageReached(page);
                workspaceItemService.update(context, workspaceItem);
			}
		}
    }
	
    /**
     * Set a specific step and page as reached. 
     * It will also "set back" where a user has reached.
     * 
     * @param context The current DSpace content
     * @param id The unique ID of the current workflow/workspace
     * @param step the step to set as reached, can be also a previous reached step
     * @param page the page (within the step) to set as reached, can be also a previous reached page
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    public static void setBackPageReached(Context context, String id, int step,
            int page) throws SQLException, AuthorizeException, IOException
    {
        InProgressSubmission submission = findSubmission(context, id);

        if (submission instanceof WorkspaceItem)
        {
            WorkspaceItem workspaceItem = (WorkspaceItem) submission;

            workspaceItem.setStageReached(step);
            workspaceItem.setPageReached(page > 0 ? page : 1);
            workspaceItemService.update(context, workspaceItem);
        }
    }
    
    /**
     * Find the maximum step the user has reached in the submission processes. 
     * If this submission is a workflow then return max-int.
     * 
     * @param context The current DSpace context.
	 * @param id The unique ID of the current workflow/workspace.
     * @return step that has been reached.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
	public static int getMaximumStepReached(Context context, String id)
            throws SQLException, AuthorizeException, IOException {
		
		InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			int stage = workspaceItem.getStageReached();
			if (stage < 0)
            {
                stage = 0;
            }
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
     * @return the furthest page reached.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
	public static int getMaximumPageReached(Context context, String id)
            throws SQLException, AuthorizeException, IOException {

		InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			int page = workspaceItem.getPageReached();
			if (page < 0)
            {
                page = 0;
            }
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
	 */
	public static void processSaveOrRemove(Context context, String id, Request request)
            throws SQLException, AuthorizeException, IOException
	{
		if (request.getParameter("submit_remove") != null)
		{
			// If they selected to remove the item then delete everything.
			WorkspaceItem workspace = findWorkspace(context,id);
			workspaceItemService.deleteAll(context, workspace);
		}
	}
	
	/**
	 * Return the HTML / DRI field name for the given input.
	 * 
	 * @param input the given input.
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
     * @return the list of steps and pages.
     *  
     */
    public static StepAndPage[] getListOfAllSteps(HttpServletRequest request, SubmissionInfo subInfo)
    {
        ArrayList<StepAndPage> listStepNumbers = new ArrayList<StepAndPage>();

        // loop through all steps
        for (int i = 0; i < subInfo.getSubmissionConfig().getNumberOfSteps(); i++)
        {
            // get the current step info
            SubmissionStepConfig currentStep = subInfo.getSubmissionConfig()
                    .getStep(i);
            int stepNumber = currentStep.getStepNumber();
            
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
                Class<?> stepClass = loader.loadClass(currentStep.getProcessingClassName());
   
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
                StepAndPage stepAndPageNum = new StepAndPage(stepNumber,j);
                
                listStepNumbers.add(stepAndPageNum);
            }// end for each page
        }// end for each step

        //convert into an array and return that
        return listStepNumbers.toArray(new StepAndPage[listStepNumbers.size()]);
    }
}
