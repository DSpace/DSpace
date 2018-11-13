/*
 * FlowUtils.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 17:02:24 +0000 (Sat, 11 Apr 2009) $
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

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.app.util.*;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.XSLUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.xml.sax.SAXException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.usagelogging.EventLogger;

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
 *
 * This class has been adjusted to support among other things
 *  the dryad overview class,
 *  the skipping of steps
 *  the dryad data model
 */

public class FlowUtils {

    private static final Message T_metadata_updated = new Message("default","The Item's metadata was successfully updated.");
    private static final Message T_metadata_added = new Message("default","New metadata was added.");

    private static final Logger log = Logger.getLogger(FlowUtils.class);

    /** Where the submissionInfo is stored on an HTTP Request object */
    private static final String DSPACE_SUBMISSION_INFO = "dspace.submission.info";

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
	public static InProgressSubmission findSubmission(Context context, String inProgressSubmissionID) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
		char type = inProgressSubmissionID.charAt(0);
        String temp = inProgressSubmissionID.substring(1);
		int id = Integer.valueOf(temp);

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
	public static WorkspaceItem findWorkspace(Context context, String inProgressSubmissionID) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
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
	public static WorkflowItem findWorkflow(Context context, String inProgressSubmissionID) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
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
    public static SubmissionInfo obtainSubmissionInfo(Map objectModel, String workspaceID) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        //try loading subInfo from HTTP request
        SubmissionInfo subInfo = (SubmissionInfo) request.getAttribute(DSPACE_SUBMISSION_INFO);

        InProgressSubmission submission=null;
        if(request.getRequestURI().contains("deposit-confirmed")){
            // find WorkflowItem
            submission = WorkflowItem.findByItemId(context, Integer.valueOf(workspaceID));
        }
        else{
            //get the submission represented by the WorkspaceID
            submission = findSubmission(context, workspaceID);
        }

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
     */
    public static void setPageReached(Context context, String id, int step, int page)
            throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
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
     * Set a specific step and page as reached.
     * It will also "set back" where a user has reached.
     *
     * @param context The current DSpace content
     * @param id The unique ID of the current workflow/workspace
     * @param step the step to set as reached, can be also a previous reached step
     * @param page the page (within the step) to set as reached, can be also a previous reached page
     */
    public static void setBackPageReached(Context context, String id, int step,
            int page) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        InProgressSubmission submission = findSubmission(context, id);

        if (submission instanceof WorkspaceItem)
        {
            WorkspaceItem workspaceItem = (WorkspaceItem) submission;

            workspaceItem.setStageReached(step);
            workspaceItem.setPageReached(page > 0 ? page : 1);
            workspaceItem.update();
            context.commit();
        }
    }

    /**
     * Find the maximum step the user has reached in the submission processes.
     * If this submission is a workflow then return max-int.
     *
     * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
     */
	public static int getMaximumStepReached(Context context, String id) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {

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
	public static int getMaximumPageReached(Context context, String id) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {

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
    public static void processSaveOrRemove(Context context, String id, Request request) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
		if (request.getParameter("submit_remove") != null)
		{
            // If they selected to remove the item then delete everything.
            WorkspaceItem workspace = findWorkspace(context,id);
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, workspace.getItem());
            for (Item dataFile : dataFiles) {
                try {
                    WorkspaceItem wsi = WorkspaceItem.findByItemId(context, dataFile.getID());
                    if(wsi != null)
                        wsi.deleteAll();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(LogManager.getHeader(context, "Error while deleting data file during a data package removal in the submission process", "Data file Id: " + dataFile.getID()), e);
                }
            }

			workspace.deleteAll();
	        context.commit();
		}
	}

//	/**
//	 * Update the provided workflowItem to advance to the next workflow
//	 * step. If this was the last thing needed before the item is
//	 * committed to the repository then return true, otherwise false.
//	 *
//	 * @param context The current DSpace content
//	 * @param id The unique ID of the current workflow
//	 */
//public static boolean processApproveTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
//	{
//		WorkflowItem workflowItem = findWorkflow(context, id);
//		Item item = workflowItem.getItem();
//
//		// Advance the item along the workflow
//        WorkflowManager.advance(context, workflowItem, context.getCurrentUser());
//
//        // FIXME: This should be a return value from advance()
//        // See if that gave the item a Handle. If it did,
//        // the item made it into the archive, so we
//        // should display a suitable page.
//        String handle = HandleManager.findHandle(context, item);
//
//        context.commit();
//
//        return (handle != null);
//	}
//
//
//
//	/**
//	 * Return the given task back to the pool of unclaimed tasks for another user
//	 * to select and preform.
//	 *
//	 * @param context The current DSpace content
//	 * @param id The unique ID of the current workflow
//	 */
//	public static void processUnclaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
//	{
//		WorkflowItem workflowItem = findWorkflow(context, id);
//
//        // Return task to pool
//        WorkflowManager.unclaim(context, workflowItem, context.getCurrentUser());
//
//        context.commit();
//
//        //Log this unclaim action
//        log.info(LogManager.getHeader(context, "unclaim_workflow",
//                "workflow_item_id=" + workflowItem.getID() + ",item_id="
//                        + workflowItem.getItem().getID() + ",collection_id="
//                        + workflowItem.getCollection().getID()
//                        + ",new_state=" + workflowItem.getState()));
//	}
//
//	/**
//	 * Claim this task from the pool of unclaimed task so that this user may
//	 * preform the task by either approving or rejecting it.
//	 *
//	 * @param context The current DSpace content
//	 * @param id The unique ID of the current workflow
//	 */
//	public static void processClaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
//	{
//		WorkflowItem workflowItem = findWorkflow(context, id);
//
//       // Claim the task
//       WorkflowManager.claim(context, workflowItem, context.getCurrentUser());
//
//       context.commit();
//
//       //log this claim information
//       log.info(LogManager.getHeader(context, "claim_task", "workflow_item_id="
//                   + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
//                   + "collection_id=" + workflowItem.getCollection().getID()
//                   + "newowner_id=" + workflowItem.getOwner().getID()
//                   + "new_state=" + workflowItem.getState()));
//	}
//
//
//	/**
//	 * Reject the given task for the given reason. If the user did not provide
//	 * a reason then an error is generated placing that field in error.
//	 *
//	 * @param context The current DSpace content
//	 * @param id The unique ID of the current workflow
//     * @param request The current request object
//	 */
//	public static String processRejectTask(Context context, String id,Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
//	{
//		WorkflowItem workflowItem = findWorkflow(context, id);
//
//		String reason = request.getParameter("reason");
//
//		if (reason != null && reason.length() > 1)
//		{
//            WorkspaceItem wsi = WorkflowManager.reject(context, workflowItem,context.getCurrentUser(), reason);
//
//			//Load the Submission Process for the collection this WSI is associated with
//            Collection c = wsi.getCollection();
//            SubmissionConfigReader subConfigReader = new SubmissionConfigReader();
//            SubmissionConfig subConfig = subConfigReader.getSubmissionConfig(c.getHandle(), false);
//
//            // Set the "stage_reached" column on the workspace item
//            // to the LAST page of the LAST step in the submission process
//            // (i.e. the page just before "Complete", which is at NumSteps-1)
//            int lastStep = subConfig.getNumberOfSteps()-2;
//            wsi.setStageReached(lastStep);
//            wsi.setPageReached(AbstractProcessingStep.LAST_PAGE_REACHED);
//            wsi.update();
//
//            context.commit();
//
//            //Submission rejected.  Log this information
//            log.info(LogManager.getHeader(context, "reject_workflow", "workflow_item_id="
//                    + wsi.getID() + "item_id=" + wsi.getItem().getID()
//                    + "collection_id=" + wsi.getCollection().getID()
//                    + "eperson_id=" + context.getCurrentUser().getID()));
//
//			// Return no errors.
//			return null;
//		}
//		else
//		{
//			// If the user did not supply a reason then
//			// place the reason field in error.
//			return "reason";
//		}
//	}


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

        //convert into an array of Doubles
        return listStepNumbers.toArray(new StepAndPage[listStepNumbers.size()]);
    }

    /**
     * Checks whether the given step is accessible
     * may be overridden to skip a step in some events
     * @param context dspace context
     * @param processingClassStr the class used for the processing of the step
     * @param subInfo the submissioninfo, may be required to find out if our submission step is required
     * @return
     *      By default this always returns true unless it is overridden in the processingclass
     */
    public static boolean isStepAccessible(Context context, String processingClassStr, SubmissionInfo subInfo){
        try {
            ClassLoader loader = subInfo.getClass().getClassLoader();
            Class stepClass = loader.loadClass(processingClassStr);
            AbstractProcessingStep step = (AbstractProcessingStep) stepClass.newInstance();

            //Yes this may sound stupid trying to find an item we have but we need an item who's metadata is up to date
            Item item = Item.find(context, subInfo.getSubmissionItem().getItem().getID());

            return step.isStepAccessible(context, item);
        } catch (Exception e) {
            log.error("Error loading step information from Step Class '" + processingClassStr + "' for accessibility Error:", e);
}

        return true;
    }

    public static boolean isStepViewableInProgressbar(Context context, String processingClassStr, SubmissionInfo subInfo){
        try {

            ClassLoader loader = subInfo.getClass().getClassLoader();
            Class stepClass = loader.loadClass(processingClassStr);
            AbstractProcessingStep step = (AbstractProcessingStep) stepClass.newInstance();

            return step.isStepShownInProgressBar();
        } catch (Exception e){
            log.error("Error loading step information from Step Class '" + processingClassStr + "' for accessibility Error:", e);
        }

        return true;
    }

    /**
     * Puts the given publication (& all it's datasets) into the reviewing process!
     * @param context the dspace context
     * @param wsPublication the publication to be sent to the reviewing process
     */
    public static void completePublicationSubmission(Context context, WorkspaceItem wsPublication, Request request) throws AuthorizeException, IOException, SQLException, TransformerException, WorkflowException, SAXException, WorkflowConfigurationException, MessagingException, ParserConfigurationException {
        //Re-find the publication (context may not be alive no more
        wsPublication = WorkspaceItem.find(context, wsPublication.getID());
        //First of all install our publication
        WorkflowItem wfPublication = WorkflowManager.start(context, wsPublication);
        wfPublication.getItem().clearMetadata("internal", "workflow", "submitted", Item.ANY);
        wfPublication.getItem().update();

        //Now start all the datasets
        Item[] datafiles = DryadWorkflowUtils.getDataFiles(context, wsPublication.getItem());
        for (Item datafile : datafiles) {
            //Should always be the case
            if(datafile != null){
                DCValue[] submitted = datafile.getMetadata("internal", "workflow", "submitted", Item.ANY);
                //Make sure that we have reached a state where the submitted param is to true
                if((0 < submitted.length && Boolean.valueOf(submitted[0].value))){
                    // Start the submission workflow
                    WorkspaceItem workspaceItem = WorkspaceItem.findByItemId(context, datafile.getID());
                    if(workspaceItem != null){
                        WorkflowItem workflowItem = WorkflowManager.start(context, workspaceItem);
                        workflowItem.getItem().clearMetadata("internal", "workflow", "submitted", Item.ANY);
                        workflowItem.update();
                    }
                }
            }
        }

        //Add the handle of our publication to it
        request.getSession().setAttribute("publication_handle", wfPublication.getItem().getHandle());
        //Also make sure that the page shows all the datasets that belong to this publication
        request.getSession().setAttribute("datasets_showall", Boolean.TRUE);
    }

    public static String processOverviewStep(Context context, Request request, HttpServletResponse response, String workItemID) throws SQLException, AuthorizeException, IOException, ServletException, TransformerException, WorkflowException, SAXException, WorkflowConfigurationException, MessagingException, ParserConfigurationException {
        InProgressSubmission workItem;
        if(workItemID.startsWith("S"))
            workItem = WorkspaceItem.find(context, Integer.parseInt(workItemID.substring(1, workItemID.length())));
        else
            workItem = WorkflowItem.find(context, Integer.parseInt(workItemID.substring(1, workItemID.length())));

        //First of all retrieve our publication
        org.dspace.content.Item publication = DryadWorkflowUtils.getDataPackage(context, workItem.getItem());
        InProgressSubmission dataset = null;
        //Retrieve our publication
        if(publication == null)
            publication = workItem.getItem();
        else                         {
            dataset = workItem;
        }


        String submitButton = Util.getSubmitButton(request, "submit_finish");

        if(request.getParameter(AbstractProcessingStep.NEXT_BUTTON) != null){

            //using the checkout step next button
            if(workItem instanceof WorkspaceItem){
                EventLogger.log(context, "submission-overview", "button=checkout");
                PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
                ShoppingCart shoppingCart = null;
                try{
                //create new transaction or update transaction id with item
                shoppingCart = paymentSystemService.getShoppingCartByItemId(context,publication.getID());
                }catch (Exception e)
                {
                    log.error("error when find shopping cart for item:"+publication.getID());
                }
                if(shoppingCart!=null)
                {
                    if(shoppingCart.getTotal()==0||shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)||shoppingCart.getStatus().equals(ShoppingCart.STATUS_VERIFIED))
                    {
                        //already entered cc information
                        finishSubmission(request, context, publication);
                        return request.getContextPath() + "/deposit-confirmed?itemID=" + publication.getID();
                    }

                }

                return request.getContextPath() + "/submit-checkout?workspaceID=" + workItem.getID();

            } else {
                //We have a workflow item & have finished editing, redir to the overview page
                ClaimedTask task = ClaimedTask.findByWorkflowIdAndEPerson(context, workItem.getID(), context.getCurrentUser().getID());
                String url = request.getContextPath() + "/handle" + workItem.getCollection().getHandle() + "/workflow?";
                url += "workflowID=" + workItem.getID();
                url += "&stepID=" + task.getStepID();
                url += "&actionID=" + task.getActionID();
                EventLogger.log(context, "submission-overview", "claimed_task_redirect=" + url);
                return url;
            }

        }
        else
        if(request.getParameter("submit-voucher") != null){
            //using the voucher form for the payment



            EventLogger.log(context, "submission-overview", "button=voucher");
        }
        else
        if(request.getParameter("submit_adddataset") != null){
            EventLogger.log(context, "submission-overview", "button=add_dataset");
            return addDataset(context, request, publication, (workItem instanceof WorkflowItem));
        }
        else
        if(request.getParameter("submit_edit_publication") != null){
            EventLogger.log(context, "submission-overview", "button=edit_publication");
            //Make sure that we go to our edit of our publication
            InProgressSubmission publicationSubmission = WorkflowItem.findByItemId(context, publication.getID());
            //Check for a workspace item if we haven't found a workflowitem
            if(publicationSubmission == null)
                publicationSubmission = WorkspaceItem.findByItemId(context, publication.getID());
            return gotoEditPublication(context, request, response, publicationSubmission);
        }else
        if(submitButton.startsWith("submit_delete_dataset_") || submitButton.startsWith("submit_delete_datapack_")){
            int deleteWorkspaceId = Integer.parseInt(submitButton.substring(submitButton.lastIndexOf("_") + 1, submitButton.length()));
            doDeleteDataset(context, deleteWorkspaceId, (workItem instanceof WorkspaceItem));
            //Check if we are deleting our dataset, if so redir to the publication overview page
            if(submitButton.startsWith("submit_delete_datapack_")){
                EventLogger.log(context, "submission-overview", "button=delete_datapack");
                //We are deleting our entire data package, redir to the submissions page
                return request.getContextPath() + "/submissions";
            }else
            if(dataset != null && deleteWorkspaceId == dataset.getID()){
                EventLogger.log(context, "submission-overview", "button=delete_dataset");
                if(publication.isArchived())
                    return request.getContextPath() + "/submissions";
                else
                    return request.getContextPath() + "/submit-overview?workspaceID=" + WorkspaceItem.findByItemId(context, publication.getID()).getID();
            }
            else
            //Check if we are deleting our publication, if so redir to the submissions page
            if(publication.getID() == deleteWorkspaceId)
                return request.getContextPath() + "/submissions";

        }else
        if(submitButton.startsWith("submit_edit_dataset_")){
            EventLogger.log(context, "submission-overview", "button=edit_dataset");
            int toEditWorkspaceId = Integer.parseInt(submitButton.substring(submitButton.lastIndexOf("_") + 1, submitButton.length()));
            return editDataset(context, request, response, toEditWorkspaceId, (workItem instanceof WorkspaceItem));
        }else
        if(submitButton.equals("submit_cancel")){
            EventLogger.log(context, "submission-overview", "button=cancel");
            //Send em back to the task
            WorkflowItem wf = WorkflowItem.findByItemId(context, publication.getID());

            ClaimedTask task = ClaimedTask.findByWorkflowIdAndEPerson(context, wf.getID(), context.getCurrentUser().getID());
            return request.getContextPath() + "/handle/" + publication.getHandle() + "/workflow?workflowID=" + wf.getID() + "&stepID=" + task.getStepID() + "&actionID=" + task.getActionID();
        }else
        if(submitButton.startsWith("submit_edit_metadata_")){
            EventLogger.log(context, "submission-overview", "button=edit_metadata");
            int toEditWorkflowId = Integer.parseInt(submitButton.substring(submitButton.lastIndexOf("_") + 1, submitButton.length()));
            return request.getContextPath() + "/submit-edit-metadata?wfItemID=" + toEditWorkflowId;
        }

        //Return null, since no redir is required
        return null;
    }

    public static String processCheckoutStep(Context context, Request request, HttpServletResponse response, String workItemID) throws SQLException, AuthorizeException, IOException, ServletException, TransformerException, WorkflowException, SAXException, WorkflowConfigurationException, MessagingException, ParserConfigurationException {
        InProgressSubmission workItem;
        if(workItemID.startsWith("S"))
            workItem = WorkspaceItem.find(context, Integer.parseInt(workItemID.substring(1, workItemID.length())));
        else
            workItem = WorkflowItem.find(context, Integer.parseInt(workItemID.substring(1, workItemID.length())));

        //First of all retrieve our publication
        org.dspace.content.Item publication = DryadWorkflowUtils.getDataPackage(context, workItem.getItem());
        InProgressSubmission dataset = null;
        //Retrieve our publication
        if(publication == null)
            publication = workItem.getItem();
        else                         {
            dataset = workItem;
        }


        String submitButton = Util.getSubmitButton(request, "submit_finish");

        if(request.getParameter(AbstractProcessingStep.NEXT_BUTTON) != null||request.getParameter("skip_payment") != null){
            if(workItem instanceof WorkspaceItem){
                EventLogger.log(context, "submission-checkout", "button=done");
                finishSubmission(request, context, publication);
                return request.getContextPath() + "/deposit-confirmed?itemID=" + publication.getID();
            } else {
                //We have a workflow item & have finished editing, redir to the overview page
                ClaimedTask task = ClaimedTask.findByWorkflowIdAndEPerson(context, workItem.getID(), context.getCurrentUser().getID());
                String url = request.getContextPath() + "/handle" + workItem.getCollection().getHandle() + "/workflow?";
                url += "workflowID=" + workItem.getID();
                url += "&stepID=" + task.getStepID();
                url += "&actionID=" + task.getActionID();
                return url;
            }

        }
        else
        if(request.getParameter("submit_cancel") != null){
            //go back to overview step
            return request.getContextPath() + "/submit-overview?workspaceID=" + workItem.getID();
        }


        //Return null, since no redir is required
        return null;
    }

    public static boolean processReAuthorization(Context context, String id,WorkflowActionConfig action,Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
        try{
//		WorkflowItem workflowItem = findWorkflow(context, id);
        WorkflowItem wfi = WorkflowItem.find(context, Integer.parseInt(id));
        Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
        WorkflowActionConfig actionConfig = action;//workflow.getStep(id).getActionConfig(actionId);
        WorkflowActionConfig wfPublication = WorkflowManager.doState(context, context.getCurrentUser(), request, Integer.parseInt(id), workflow, actionConfig);


        context.commit();
        if(wfPublication!=null&&wfPublication.getName().contains("reAuthorizationPayment"))
            return false;
        }catch (Exception e)
        {
            log.error("error when reauthorize payment:"+e.getMessage());
        }
        return true;
	}
    public static String processDepositConfirmedStep(Context context, Request request, HttpServletResponse response, String workItemID)
            throws SQLException, AuthorizeException, IOException, ServletException, TransformerException, WorkflowException, SAXException, WorkflowConfigurationException, MessagingException, ParserConfigurationException {
        return request.getContextPath() + "/submissions";
    }




    private static void finishSubmission(Request request, Context context, Item publication) throws SQLException {
        try {
            //We have completed everything time to start our dataset
            WorkspaceItem wsPublication = WorkspaceItem.findByItemId(context, publication.getID());

            //Retrieve the to upload bitstreams
            String[] toExportHandles = request.getParameterValues("export_item");
            if(toExportHandles != null && toExportHandles.length > 0){
                new ExportHandlesThread(request, context.getCurrentUser(), publication.getHandle(), toExportHandles).start();
            }
            //Start off by installing our publication
            WorkflowItem wfPublication = WorkflowManager.start(context, wsPublication);

            wfPublication.getItem().clearMetadata("internal", "workflow", "submitted", Item.ANY);
            wfPublication.getItem().update();

            //TODO: make sure only ONE email is sent out
            //The publication isn't archived, & we completed it so now get all the datasets linked to this publication & start em
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, wfPublication.getItem());
            for (Item dataFile : dataFiles) {
                //Should always be the case
                if(dataFile != null){
                    DCValue[] submitted = dataFile.getMetadata("internal", "workflow", "submitted", Item.ANY);
                    //Make sure that we have reached a state where the submitted param is to true
                    // Start the submission workflow
                    WorkspaceItem workspaceItem = WorkspaceItem.findByItemId(context, dataFile.getID());
                    if(workspaceItem != null){
                        //Start the data files without a notify, the notification will come from the data package
                        //TODO: no notify
                        WorkflowItem workflowItem = WorkflowManager.start(context, workspaceItem);
                        workflowItem.getItem().clearMetadata("internal", "workflow", "submitted", Item.ANY);
                        workflowItem.update();
                    }

                }
            }

			DryadDataPackage dryadDataPackage = DryadDataPackage.findByWorkflowItemId(context, wfPublication.getID());
            // look for stored manuscripts to see if its status has been updated since submission was started:
            Manuscript storedManuscript = JournalUtils.getStoredManuscriptForPackage(context, dryadDataPackage);
            if (storedManuscript != null) {
                if (storedManuscript.isAccepted()) {
                    // if the ms is accepted, push the item into curation from review
                    ApproveRejectReviewItem.processReviewPackageUsingManuscript(context, dryadDataPackage, storedManuscript);
                } else if (storedManuscript.isRejected()) {
                    // if it's rejected, keep it in the review queue, but move the manuscript number to former.
                    dryadDataPackage.setFormerManuscriptNumber(dryadDataPackage.getManuscriptNumber());
                    dryadDataPackage.setManuscriptNumber(null);
                }
            }
        } catch (Exception e){
            // adding an explicit rollback to save the integrity of the data
            // when an exception happens most likely spring doesn't throw it to
            // the Cocoon servlet that commit the transaction in every case.
            context.getDBConnection().rollback();
            log.error("Exception during CompleteSubmissionStep: ", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * It also puts the current data file into the awaiting completion workspace state
     * @param context the dspace context
     * @param request the request
     * @param publication the data package containing amont other things this data file
     * @throws SQLException ...
     * @throws AuthorizeException ...
     * @throws IOException ...
     * @throws ServletException ...
     * @return the url we are to be redirected to
     */
    public static String addDataset(Context context, HttpServletRequest request, Item publication, boolean fromWorkflow) throws SQLException, AuthorizeException, IOException, ServletException {
        boolean success = false;
        String redirectUrl = null;
        try{
            redirectUrl = DryadWorkflowUtils.createDataset(context, request, publication, fromWorkflow);
            success = true;
        } catch (Exception e){

            throw new ServletException(e);
        } finally {
            // commit changes to database
            if(success)
                context.commit();
            else
                context.getDBConnection().rollback();

        }
        return redirectUrl;
    }

    /**
     * Method that redirects the user to edit a workspace item.
     * It also puts the current data file into the awaiting completion workspace state
     * @param context the dspace context
     * @param request the request
     * @param response the response
     * @param toEditID the workspaceid for the item we wish to edit
     * @throws AuthorizeException ...
     * @throws SQLException ...
     * @throws IOException ...
     * @return the url we are to be redirected to
     */
    private static String editDataset(Context context, HttpServletRequest request, HttpServletResponse response, int toEditID, boolean isWorkspaceItem) throws AuthorizeException, SQLException, IOException {
        //Redirect us to the page where we get to edit the dataset
        String redirectUrl = request.getContextPath() + "/submit-edit?workitemID=" + (isWorkspaceItem ? "S" : "W") + toEditID + "&collHandle=" + ConfigurationManager.getProperty("submit.dataset.collection");
        context.commit();
        return redirectUrl;
    }

    /**
     * Method that deletes the given workspace Id
     * @param context the dspace context
     * @param toDeleteId the id of the workspace item we want to delete
     * @throws SQLException ...
     * @throws IOException ...
     * @throws AuthorizeException ...
     */
    private static void doDeleteDataset(Context context, int toDeleteId, boolean isWorkspaceItem) throws SQLException, AuthorizeException, IOException {
        InProgressSubmission toDeleteItem;
        if(isWorkspaceItem)
            toDeleteItem = WorkspaceItem.find(context, toDeleteId);
        else
            toDeleteItem = WorkflowItem.find(context, toDeleteId);

        //Check if we are removing a publication, if we have children remove em all !
        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, toDeleteItem.getItem());
        for (Item dataFile : dataFiles) {
            WorkspaceItem datafileItem = WorkspaceItem.findByItemId(context, dataFile.getID());
            //Found so delete it
            datafileItem.deleteAll();
        }
        if(isWorkspaceItem){
           ((WorkspaceItem) toDeleteItem).deleteAll();
        } else {
            WorkspaceItem wsi = WorkflowManager.rejectWorkflowItem(context, (WorkflowItem) toDeleteItem, null, null, null, false);
            wsi.deleteAll();
        }
        context.commit();
    }


    public static FlowResult doEditMetadata(Context context, int workflowItemId, Request request) throws AuthorizeException, IOException, SQLException, UIException {
        WorkflowItem workflowItem = WorkflowItem.find(context, workflowItemId);

        FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = workflowItem.getItem();


		// STEP 1:
		// Clear all metadata within the scope
		// Only metadata values within this scope will be considered. This
		// is so ajax request can operate on only a subset of the values.
		String scope = request.getParameter("scope");
		if ("*".equals(scope))
		{
			item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		}
		else
		{
			String[] parts = parseName(scope);
			item.clearMetadata(parts[0],parts[1],parts[2],Item.ANY);
		}

		// STEP 2:
		// First determine all the metadata fields that are within
		// the scope parameter
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		Enumeration parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{

			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("name_"))
			{
				// Check if the name is within the scope
				String parameterValue = request.getParameter(parameterName);
				if ("*".equals(scope) || scope.equals(parameterValue))
				{
					// Extract the index from the name.
					String indexString = parameterName.substring("name_".length());
					Integer index = Integer.valueOf(indexString);
					indexes.add(index);
				}
			}
		}

		// STEP 3:
		// Iterate over all the indexes within the scope and add them back in.
		for (Integer index=1; index <= indexes.size(); ++index)
		{
			String name = request.getParameter("name_"+index);
			String value = request.getParameter("value_"+index);
                        String authority = request.getParameter("value_"+index+"_authority");
                        String confidence = request.getParameter("value_"+index+"_confidence");
			String lang = request.getParameter("language_"+index);
			String remove = request.getParameter("remove_"+index);

			// the user selected the remove checkbox.
			if (remove != null)
				continue;

			// get the field's name broken up
			String[] parts = parseName(name);

                        // probe for a confidence value
                        int iconf = Choices.CF_UNSET;
                        if (confidence != null && confidence.length() > 0)
                            iconf = Choices.getConfidenceValue(confidence);
                        // upgrade to a minimum of NOVALUE if there IS an authority key
                        if (authority != null && authority.length() > 0 && iconf == Choices.CF_UNSET)
                            iconf = Choices.CF_NOVALUE;
                        item.addMetadata(parts[0], parts[1], parts[2], lang,
                                             value, authority, iconf);
		}

		item.update();
		context.commit();

		result.setContinue(true);

		result.setOutcome(true);
		result.setMessage(T_metadata_updated);

		return result;


    }

    public static FlowResult doAddMetadata(Context context, int worflowItemId, Request request) throws AuthorizeException, IOException, SQLException {
        WorkflowItem workflowItem = WorkflowItem.find(context, worflowItemId);

        FlowResult result = new FlowResult();
        result.setContinue(false);

        Item item = workflowItem.getItem();


        String fieldID = request.getParameter("field");
        String value = request.getParameter("value");
        String language = request.getParameter("language");

        MetadataField field = MetadataField.find(Integer.valueOf(fieldID));
        MetadataSchema schema = MetadataSchema.find(field.getSchemaID());

        item.addMetadata(schema.getName(), field.getElement(), field.getQualifier(), language, value);

        item.update();
        context.commit();

        result.setContinue(true);

        result.setOutcome(true);
        result.setMessage(T_metadata_added);

        return result;
    }


    /**
     * Redirects the user so he can edit the metadata of the current publication
     * @param context the dspace context
     * @param request the request
     * @param response the response
     * @param publication the publication we want to edit
     * @throws SQLException ...
     * @throws IOException ...
     * @throws AuthorizeException ...
     * @return the url we are to be redirected to
     */
    private static String gotoEditPublication(Context context, HttpServletRequest request, HttpServletResponse response, InProgressSubmission publication) throws SQLException, IOException, AuthorizeException {
        boolean success = false;
        String redirectUrl = null;
        try{
            redirectUrl = request.getContextPath() + "/submit-edit?workitemID=" + (publication instanceof WorkflowItem ? "W" : "S") + publication.getID() + "&collHandle=" + publication.getCollection().getHandle();

            success = true;
        } finally {
            // commit changes to database
            if(success)
                context.commit();
            else
                context.getDBConnection().rollback();
        }
        return redirectUrl;
    }

    private static class ExportHandlesThread extends Thread{
        private String datasetHandle;
        private Map<String, List<String>> reponameToHandles;
        private EPerson currentUser;
        private static Logger log = Logger.getLogger(ExportHandlesThread.class);
        private String threadId;

        private ExportHandlesThread(Request request, EPerson currentUser, String datasetHandle, String[] handles) {
            try{
                this.datasetHandle = datasetHandle;
                this.currentUser = currentUser;
                this.threadId = UUID.randomUUID().toString();
                reponameToHandles = new HashMap<String, List<String>>();
                for (String handle : handles) {
                    String repoName = request.getParameter("repo_name_" + handle);
                    List<String> toAddList = new ArrayList<String>();
                    if(reponameToHandles.get(repoName) != null)
                        toAddList = reponameToHandles.get(repoName);

                    toAddList.add(handle);

                    reponameToHandles.put(repoName, toAddList);
                }
                log.info("Created export handles thread with id: " + threadId);
            }catch (Exception e){
                log.error("Error while creating export handles thread user: " + currentUser.getID() + " dataset handle: " + datasetHandle + " toPushHandles: " + Arrays.toString(handles));
            }
        }

        @Override
        public void run() {
            super.run();
            try{
                //Put together our list of arguments
                ArrayList<String> startingArgs = new ArrayList<String>();
//            disseminate
                startingArgs.add("-d");
//            Handle of item to disseminate.
                startingArgs.add("-i");
                startingArgs.add(datasetHandle);
                startingArgs.add("-e");
                startingArgs.add(currentUser.getEmail());
//            Packager option to pass to plugin, "name=value" (repeatable)
                startingArgs.add("-o");
                startingArgs.add("xwalk=DRYAD-V3");
//            PackagerType
                startingArgs.add("-t");
                startingArgs.add("BAGIT");

                //For each repo name we have to call the packager once
                for(String repoName : reponameToHandles.keySet()){
                    List<String> handles = reponameToHandles.get(repoName);
                    Context context = null;
                    try{
                        //We start of by copying the starting arguments
                        ArrayList<String> args = (ArrayList<String>) startingArgs.clone();
                        Collections.copy(args, startingArgs);
                        args.add("-o");
                        args.add("repo=" + repoName);

                        if(0 < handles.size()){
                            args.add("-o");

                            String handleString = "";
                            for (int i = 0; i < handles.size(); i++) {
                                String handle = handles.get(i);

                                handleString += handle;
                                if(i != (handles.size() - 1))
                                    handleString += ";";
                            }
                            args.add("files=" + handleString);
                        }
                        args.add("-");

                        log.info("Calling the packager (for thread: " + threadId + ") with arguments: " + Arrays.toString(args.toArray(new String[args.size()])));
                        context = new Context();
                        PackageParameters pkgParams = new PackageParameters();
                        //-d option
                        //-i option
                        String identifier = datasetHandle;
                        //-e option
                        EPerson myEPerson = EPerson.findByEmail(context, currentUser.getEmail());
                        context.setCurrentUser(myEPerson);
                        //-o option
                        pkgParams.addProperty("files", "10255/dryad.42095");
                        //-t option
                        String packagerType ="BAGIT";
                        //-o option
                        pkgParams.addProperty("xwalk", "DRYAD-V3");
                        //-o option
                        pkgParams.addProperty("repo", "TreeBASE");
                        //- option
                        String sourceFile = "-";

                        //Packager.main(args.toArray(new String[args.size()]));
                        PackageDisseminator dip = (PackageDisseminator) PluginManager
                                .getNamedPlugin(PackageDisseminator.class, packagerType);
                        if (dip == null)
                        {
                            usageError("Error, Unknown package type: "+packagerType);
                        }

                        DSpaceObject dso = HandleManager.resolveToObject(context, identifier);
                        if (dso == null)
                        {
                            throw new IllegalArgumentException("Bad identifier/handle -- "
                                    + "Cannot resolve handle \"" + identifier + "\"");
                        }

                        //disseminate the requested object
                       // Packager.disseminate(context, dip, dso, pkgParams, sourceFile);
                        File pkgFile = new File(sourceFile);
                        System.out.println("\nDisseminating DSpace " + Constants.typeText[dso.getType()] +
                                " [ hdl=" + dso.getHandle() + " ] to " + sourceFile);
                        dip.disseminate(context, dso, pkgParams, pkgFile);
                        if(pkgFile!=null && pkgFile.exists())
                        {
                            System.out.println("\nCREATED package file: " + pkgFile.getCanonicalPath());
                        }


                    } catch (Exception e){
                        log.error("Error an unknown exception occurred when posting a handle in the export handles thread, logdata: [" + getLogData() + " repoName:" + repoName + " handlesToPost:" + Arrays.toString(handles.toArray(new String[handles.size()])) +  " ]");

                    } finally {
                        if(context != null)
                        {
                            context.abort();
                        }
                    }
                }
            }catch (Exception e){
                log.error("An unknown exception occurred when running the export handles thread, logdata: [" + getLogData() + "]", e);
            }
        }

        private String getLogData(){
            return "thread id: " + threadId + " eperson id: " + currentUser.getID() + " datasetHandle: " + datasetHandle;
        }
    }

    /**
     * Parse the given name into three parts, divided by an _. Each part should represent the
     * schema, element, and qualifier. You are guaranteed that if no qualifier was supplied the
     * third entry is null.
     *
     * @param name The name to be parsed.
     * @return An array of name parts.
     */
    private static String[] parseName(String name) throws UIException
    {
        String[] parts = new String[3];

        String[] split = name.split("_");
        if (split.length == 2) {
            parts[0] = split[0];
            parts[1] = split[1];
            parts[2] = null;
        } else if (split.length == 3) {
            parts[0] = split[0];
            parts[1] = split[1];
            parts[2] = split[2];
        } else {
            throw new UIException("Unable to parse metedata field name: "+name);
        }
        return parts;
    }

    // die from illegal command line
    private static void usageError(String msg)
    {
        System.out.println(msg);
        System.out.println(" (run with -h flag for details)");
        System.exit(1);
    }

    // returns the table cell that actions should be rendered in
    public static Cell renderDatasetItem(Context context, Table table, Item dataset) throws WingException, SQLException {
        // first row: buttons, title
        Row row = table.addRow("dataset-row-top", null, "dataset-row-top");
        Cell finalActionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
        Cell labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
        Cell dataCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");

        DryadDataFile dryadDataFile = new DryadDataFile(dataset);
        String datasetTitle = XSLUtils.getShortFileName(dryadDataFile.getTitle(), 50);

        labelCell.addContent("Title");
        dataCell.addContent(datasetTitle);

        // filename
        row = table.addRow("dataset-row", null, "dataset-row");
        Cell actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
        labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
        dataCell = row.addCell();

        String fileInfo;
        try {
            Bitstream mainFile = dryadDataFile.getFirstBitstream();
            fileInfo = mainFile.getName() + " (" + FileUtils.byteCountToDisplaySize(mainFile.getSize()) + ")";
        } catch (Exception e) {
            fileInfo = dryadDataFile.getItem().getName();
        }
        labelCell.addContent("Filename");
        dataCell.addXref(HandleManager.resolveToURL(context, dataset.getHandle()), fileInfo);

        // readme
        Bitstream readme = dryadDataFile.getREADME();
        if (readme != null) {
            row = table.addRow("dataset-row", null, "dataset-row");
            actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
            labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
            dataCell = row.addCell();

            String readmeFileInfo = readme.getName() + " (" + FileUtils.byteCountToDisplaySize(readme.getSize()) + ")";
            labelCell.addContent("README");
            dataCell.addContent(readmeFileInfo);
        }

        // description
        DCValue[] descriptions = dataset.getMetadata("dc", "description", null, org.dspace.content.Item.ANY);
        if (descriptions.length > 0) {
            row = table.addRow("dataset-row", null, "dataset-row");
            actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
            labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
            dataCell = row.addCell("dataset-description",null, 0, 0, "dataset-description");

            labelCell.addContent("Description");
            dataCell.addHighlight("dataset-description").addContent(descriptions[0].value);
        }

        // date added
        DCValue[] provenances = dataset.getMetadata("dc", "description", "provenance", org.dspace.content.Item.ANY);
        for (DCValue provenance : provenances) {
            Matcher uploadMatcher = Pattern.compile("File was uploaded at (.+)").matcher(provenance.value);
            if (uploadMatcher.matches()) {
                String uploadDate = uploadMatcher.group(1);
                row = table.addRow("dataset-row", null, "dataset-row");
                actionCell = row.addCell("dataset-action",null, 0, 0, "dataset-action");
                labelCell = row.addCell("dataset-label",null, 0, 0, "dataset-label");
                dataCell = row.addCell();

                labelCell.addContent("Date added");
                dataCell.addHighlight("dataset-date").addContent(uploadDate);
                break;
            }
        }


        return finalActionCell;
    }

	public static Map<String, List<Item>> retrieveInternalFileStatuses(Context context, Item item) {
    	Map<Integer, Boolean> dataFileSupersededStatus = new HashMap<Integer, Boolean>();
		DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);
		DCValue[] fileStatusMDVs = item.getMetadata("workflow.review.fileStatus");
		HashMap<Integer, Boolean> fileIsSupersededMap = new HashMap<Integer, Boolean>();
		ArrayList<Item> dataFiles = new ArrayList<Item>();
		for (DCValue fileStatusMDV : fileStatusMDVs) {
			fileIsSupersededMap.put(Integer.valueOf(fileStatusMDV.value), (fileStatusMDV.confidence == Choices.CF_REJECTED));
		}
		if (item.getMetadata("dc.relation.haspart").length > 0) {
			for (DCValue value : item.getMetadata("dc.relation.haspart")) {

				DSpaceObject obj = null;
				try {
					obj = dis.resolve(context, value.value);
				} catch (IdentifierNotFoundException e) {
					// just keep going
				} catch (IdentifierNotResolvableException e) {
					// just keep going
				}
				if (obj != null) {
					Boolean isSuperseded = fileIsSupersededMap.get(obj.getID());
					if (isSuperseded == null) isSuperseded = false;
					dataFileSupersededStatus.put(obj.getID(), isSuperseded);
					dataFiles.add((Item) obj);
				}
			}
		}
		ArrayList<Item> hasPartsList = new ArrayList<Item>();
		ArrayList<Item> supersededList = new ArrayList<Item>();
		for (Item obj : dataFiles) {
			if (dataFileSupersededStatus.get(obj.getID())) {
				supersededList.add(obj);
			} else {
				hasPartsList.add(obj);
			}
		}
		HashMap<String, List<Item>> resultMap = new HashMap<String, List<Item>>();
		resultMap.put("hasParts", hasPartsList);
		resultMap.put("superseded", supersededList);
		return resultMap;
	}

	public static void processReviewSaveChanges(Context context, HttpServletRequest request, int workflowID) {
    	try {
			Item publication = WorkflowItem.find(context, workflowID).getItem();
			Enumeration<String> parameters = request.getParameterNames();
			HashMap<String, Integer> filestatuses = new HashMap<String, Integer>();
			while (parameters.hasMoreElements()) {
				String paramName = parameters.nextElement();
				Matcher matcher = Pattern.compile("filestatus_(\\d+)").matcher(paramName);
				if (matcher.matches()) {
					filestatuses.put(matcher.group(1), Integer.valueOf(request.getParameter(paramName)));
				}
			}
			DCValue[] fileStatusMDVs = publication.getMetadata("workflow.review.fileStatus");
			publication.clearMetadata("workflow.review.fileStatus");
			for (DCValue fileStatusMDV : fileStatusMDVs) {
				fileStatusMDV.confidence = filestatuses.get(fileStatusMDV.value);
				publication.addMetadata(fileStatusMDV);
			}
			publication.update();
		} catch (Exception e) {
			log.error("Exception " + e.getMessage());
		}
	}
}
