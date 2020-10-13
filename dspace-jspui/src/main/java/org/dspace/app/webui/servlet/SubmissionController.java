/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSONUploadResponse;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.step.UploadStep;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;

import com.google.gson.Gson;

/**
 * Submission Manager servlet for DSpace. Handles the initial submission of
 * items, as well as the editing of items further down the line.
 * <p>
 * Whenever the submit servlet receives a GET request, this is taken to indicate
 * the start of a fresh new submission, where no collection has been selected,
 * and the submission process is started from scratch.
 * <p>
 * All other interactions happen via POSTs. Part of the post will normally be a
 * (hidden) "step" parameter, which will correspond to the form that the user
 * has just filled out. If this is absent, step 0 (select collection) is
 * assumed, meaning that it's simple to place "Submit to this collection"
 * buttons on collection home pages.
 * <p>
 * According to the step number of the incoming form, the values posted from the
 * form are processed (using the process* methods), and the item updated as
 * appropriate. The servlet then forwards control of the request to the
 * appropriate JSP (from jsp/submit) to render the next stage of the process or
 * an error if appropriate. Each of these JSPs may require that attributes be
 * passed in. Check the comments at the top of a JSP to see which attributes are
 * needed. All submit-related forms require a properly initialised
 * SubmissionInfo object to be present in the the "submission.info" attribute.
 * This holds the core information relevant to the submission, e.g. the item,
 * personal workspace or workflow item, the submitting "e-person", and the
 * target collection.
 * <p>
 * When control of the request reaches a JSP, it is assumed that all checks,
 * interactions with the database and so on have been performed and that all
 * necessary information to render the form is in memory. e.g. The
 * SubmitFormInfo object passed in must be correctly filled out. Thus the JSPs
 * do no error or integrity checking; it is the servlet's responsibility to
 * ensure that everything is prepared. The servlet is fairly diligent about
 * ensuring integrity at each step.
 * <p>
 * Each step has an integer constant defined below. The main sequence of the
 * submission procedure always runs from 0 upwards, until SUBMISSION_COMPLETE.
 * Other, not-in-sequence steps (such as the cancellation screen and the
 * "previous version ID verification" screen) have numbers much higher than
 * SUBMISSION_COMPLETE. These conventions allow the progress bar component of
 * the submission forms to render the user's progress through the process.
 * 
 * @see org.dspace.app.util.SubmissionInfo
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.app.webui.submit.JSPStepManager
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class SubmissionController extends DSpaceServlet
{
    // Steps in the submission process

    /** Selection collection step */
    public static final int SELECT_COLLECTION = 0;

    /** First step after "select collection" */
    public static final int FIRST_STEP = 1;
    
    /** For workflows, first step is step #0 (since Select Collection is already filtered out) */
    public static final int WORKFLOW_FIRST_STEP = 0;
    
    /** path to the JSP shown once the submission is completed */
    private static final String COMPLETE_JSP = "/submit/complete.jsp";
    
    private String tempDir = null;
    
    private static Object mutex = new Object();
    
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(SubmissionController.class);

    private static WorkspaceItemService workspaceItemService;
    
    private static BitstreamService bitstreamService;
    
    private static BundleService bundleService;
    
    private static WorkflowItemService workflowItemService;
    
    @Override
    public void init() throws ServletException {
    	super.init();
    	// this is a sort of HACK as we are injecting static services using the singleton nature of the servlet...
    	workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    	bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    	bundleService = ContentServiceFactory.getInstance().getBundleService();
    	workflowItemService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowItemService();    	
    }
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * Possible GET parameters:
         * 
         * resume= <workspace_item_id> - Resumes submitting the given workspace
         * item
         * 
         * workflow= <workflow_id> - Starts editing the given workflow item in
         * workflow mode
         * 
         * With no parameters, doDSGet() just calls doDSPost(), which continues
         * the current submission (if one exists in the Request), or creates a
         * new submission (if no existing submission can be found).
         */

        // try to get a workspace ID or workflow ID
        String workspaceID = request.getParameter("resume");
        String workflowID = request.getParameter("workflow");
        String resumableFilename = request.getParameter("resumableFilename");

        // If resuming a workspace item
        if (workspaceID != null)
        {
            try
            {
                // load the workspace item
                WorkspaceItem wi = workspaceItemService.find(context, Integer
                        .parseInt(workspaceID));

                //load submission information
                SubmissionInfo si = SubmissionInfo.load(request, wi);
                
                //TD: Special case - If a user is resuming a submission
                //where the submission process now has less steps, then
                //we will need to reset the stepReached in the database
                //(Hopefully this will never happen, but just in case!)
                if(getStepReached(si) >= si.getSubmissionConfig().getNumberOfSteps())
                {
                    //update Stage Reached to the last step in the Process
                    int lastStep = si.getSubmissionConfig().getNumberOfSteps()-1;
                    wi.setStageReached(lastStep);
                    
                    //flag that user is on last page of last step
                    wi.setPageReached(AbstractProcessingStep.LAST_PAGE_REACHED);
                    
                    //commit all changes to database immediately
                    workspaceItemService.update(context, wi);
                    
                    //update submission info
                    si.setSubmissionItem(wi);
                }
                    
                // start over at beginning of first step
                setBeginningOfStep(request, true);
                doStep(context, request, response, si, FIRST_STEP);
            }
            catch (NumberFormatException nfe)
            {
                log.warn(LogManager.getHeader(context, "bad_workspace_id",
                        "bad_id=" + workspaceID));
                JSPManager.showInvalidIDError(request, response, workspaceID,
                        -1);
            }
        }
        else if (workflowID != null) // if resuming a workflow item
        {
            try
            {
                // load the workflow item
                WorkflowItem wi = workflowItemService.find(context, Integer
                        .parseInt(workflowID));

                //load submission information
                SubmissionInfo si = SubmissionInfo.load(request, wi);
                
                // start over at beginning of first workflow step
                setBeginningOfStep(request, true);
                doStep(context, request, response, si, WORKFLOW_FIRST_STEP);
            }
            catch (NumberFormatException nfe)
            {
                log.warn(LogManager.getHeader(context, "bad_workflow_id",
                        "bad_id=" + workflowID));
                JSPManager
                        .showInvalidIDError(request, response, workflowID, -1);
            }
        }
        else if (!StringUtils.isEmpty(resumableFilename)) // if resumable.js asks whether a part of af file was received
        {
            if (request.getMethod().equals("GET"))
            {
                DoGetResumable(request, response);
            }
        }
        else
        {
            // otherwise, forward to doDSPost() to do usual processing
            doDSPost(context, request, response);
        }

    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	// Configuration of current step in Item Submission Process
        SubmissionStepConfig currentStepConfig;
        
        //need to find out what type of form we are dealing with
        String contentType = request.getContentType();

        // if multipart form, we have to wrap the multipart request
        // in order to be able to retrieve request parameters, etc.
        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            try
            {
                    request = wrapMultipartRequest(request);
                    
                    // check if the POST request was send by resumable.js
                    String resumableFilename = request.getParameter("resumableFilename");
                    
                    if (!StringUtils.isEmpty(resumableFilename))
                    {
                        log.debug("resumable Filename: '" + resumableFilename + "'.");
                        File completedFile = null;
                        try
                        {
                            log.debug("Starting doPostResumable method.");
                            completedFile = doPostResumable(request);
                        } catch(IOException e){
                            // we were unable to receive the complete chunk => initialize reupload
                            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        }
                        
                        if (completedFile == null)
                        {
                            // if a part/chunk was uploaded, but the file is not completly uploaded yet
                            log.debug("Got one file chunk, but the upload is not completed yet.");
                            return;
                        }
                        else
                        {
                            // We got the complete file. Assemble it and store
                            // it in the repository.
                            log.debug("Going to assemble file chunks.");

                            if (completedFile.length() > 0)
                            {
                                String fileName = completedFile.getName();
                                String filePath = tempDir + File.separator + fileName;
                                // Read the temporary file
                                InputStream fileInputStream = 
                                        new BufferedInputStream(new FileInputStream(completedFile));
                                
                                // to safely store the file in the repository
                                // we have to add it as a bitstream to the
                                // appropriate item (or to be specific its
                                // bundle). Instead of rewriting this code,
                                // we should use the same code, that's used for
                                // the "old" file upload (which is not using JS).
                                SubmissionInfo si = getSubmissionInfo(context, request);
                                UploadStep us = new UploadStep();
                                request.setAttribute(fileName + "-path", filePath);
                                request.setAttribute(fileName + "-inputstream", fileInputStream);
                                request.setAttribute(fileName + "-description", request.getParameter("description"));
                                int uploadResult = us.processUploadFile(context, request, response, si);

                                // cleanup our temporary file
                                if (!completedFile.delete())
                                {
                                    log.error("Unable to delete temporary file " + filePath);
                                }

                                // We already assembled the complete file.
                                // In case of any error it won't help to
                                // reupload the last chunk. That makes the error
                                // handling realy easy:
                                if (uploadResult != UploadStep.STATUS_COMPLETE)
                                {
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    return;
                                }
                            }
                            return;
                        }
                   }
            }
            catch (FileSizeLimitExceededException e)
            {
                log.warn("Upload exceeded upload.max");
                if (ConfigurationManager.getBooleanProperty("webui.submit.upload.progressbar", true))
                {
                    Gson gson = new Gson();
                    // old browser need to see this response as html to work            
                    response.setContentType("text/html");
                    JSONUploadResponse jsonResponse = new JSONUploadResponse();
                    jsonResponse.addUploadFileSizeLimitExceeded(
                            e.getActualSize(), e.getPermittedSize());
                    response.getWriter().print(gson.toJson(jsonResponse));
                    response.flushBuffer();                    
                }
                else
                {
                    JSPManager.showFileSizeLimitExceededError(request, response, e.getMessage(), e.getActualSize(), e.getPermittedSize());                    
                }
                return;
            }
            
            //also, upload any files and save their contents to Request (for later processing by UploadStep)
            uploadFiles(context, request);
        }
        
        // Reload submission info from request parameters
        SubmissionInfo subInfo = getSubmissionInfo(context, request);

        // a submission info object is necessary to continue
        if (subInfo == null)
        {
            // Work around for problem where people select "is a thesis", see
            // the error page, and then use their "back" button thinking they
            // can start another submission - it's been removed so the ID in the
            // form is invalid. If we detect the "removed_thesis" attribute we
            // display a friendly message instead of an integrity error.
            if (request.getSession().getAttribute("removed_thesis") != null)
            {
                request.getSession().removeAttribute("removed_thesis");
                JSPManager.showJSP(request, response,
                        "/submit/thesis-removed-workaround.jsp");

                return;
            }
            else
            {
                // If the submission info was invalid, throw an integrity error
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
                return;
            }
        }

        // First, check for a click on "Cancel/Save" button.
        if (UIUtil.getSubmitButton(request, "").equals(AbstractProcessingStep.CANCEL_BUTTON))
        {
        	// Get the current step
            currentStepConfig = getCurrentStepConfig(request, subInfo);
            
        	// forward user to JSP which will confirm 
            // the cancel/save request.
            doCancelOrSave(context, request, response, subInfo,
                    currentStepConfig);
        }
        // Special case - no InProgressSubmission yet
        // If no submission, we assume we will be going
        // to the "select collection" step.
        else if (subInfo.getSubmissionItem() == null)
        {
            // we have just started this submission
            // (or we have just resumed a saved submission)

            // do the "Select Collection" step
            doStep(context, request, response, subInfo, SELECT_COLLECTION);
        }
        else
        // otherwise, figure out the next Step to call!
        {
            // Get the current step
            currentStepConfig = getCurrentStepConfig(request, subInfo);

            //if user already confirmed the cancel/save request
            if (UIUtil.getBoolParameter(request, "cancellation"))
            {
                // user came from the cancel/save page, 
                // so we need to process that page before proceeding
                processCancelOrSave(context, request, response, subInfo, currentStepConfig);
            }
            //check for click on "<- Previous" button
            else if (UIUtil.getSubmitButton(request, "").startsWith(
                    AbstractProcessingStep.PREVIOUS_BUTTON))
            {
                // return to the previous step
                doPreviousStep(context, request, response, subInfo, currentStepConfig);
            }
            //check for click on Progress Bar
            else if (UIUtil.getSubmitButton(request, "").startsWith(
                    AbstractProcessingStep.PROGRESS_BAR_PREFIX))
            {
                // jumping to a particular step/page
                doStepJump(context, request, response, subInfo, currentStepConfig);
            }
            else
            {
                // by default, load step class to start 
                // or continue its processing
                doStep(context, request, response, subInfo, currentStepConfig.getStepNumber());
            }
        }
    }

    /**
     * Forward processing to the specified step.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            SubmissionInfo pertaining to this submission
     * @param stepNumber
     *            The number of the step to perform
     */
    private void doStep(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int stepNumber)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
    	SubmissionStepConfig currentStepConfig = null;
    	
        if (subInfo.getSubmissionConfig() != null)
        {
            // get step to perform
            currentStepConfig = subInfo.getSubmissionConfig().getStep(stepNumber);
        }
        else
        {
            log.fatal(LogManager.getHeader(context, "no_submission_process",
                    "trying to load step=" + stepNumber
                            + ", but submission process is null"));

            JSPManager.showInternalError(request, response);
        }

        // if this is the furthest step the user has been to, save that info
        if (!subInfo.isInWorkflow() && (currentStepConfig.getStepNumber() > getStepReached(subInfo)))
        {
            // update submission info
            userHasReached(context, subInfo, currentStepConfig.getStepNumber());
            
            // flag that we just started this step (for JSPStepManager class)
            setBeginningOfStep(request, true);
        }
       
        // save current step to request attribute
        saveCurrentStepConfig(request, currentStepConfig);

        log.debug("Calling Step Class: '"
                + currentStepConfig.getProcessingClassName() + "'");

        try
        {
            
            JSPStepManager stepManager = JSPStepManager.loadStep(currentStepConfig);
           
            //tell the step class to do its processing
            boolean stepFinished = stepManager.processStep(context, request, response, subInfo);
            
            //if this step is finished, continue to next step
            if(stepFinished)
            {
                // If we finished up an upload, then we need to change
                // the FileUploadRequest object back to a normal HTTPServletRequest
                if(request instanceof FileUploadRequest)
                {
                    request = ((FileUploadRequest)request).getOriginalRequest();
                }
                
                //retrieve any changes to the SubmissionInfo object
                subInfo = getSubmissionInfo(context, request);
                
                //do the next step!
                doNextStep(context, request, response, subInfo, currentStepConfig);
            }
            else
            {
                //commit & close context
                context.complete();
            }
        }
        catch (AuthorizeException ae)
        {
        	throw ae;
        }
        catch (Exception e)
        {
            log.error("Error loading step class'" + currentStepConfig.getProcessingClassName() + "':", e);
            JSPManager.showInternalError(request, response);
        }

    }

    /**
     * Forward processing to the next step.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            SubmissionInfo pertaining to this submission
     */
    private void doNextStep(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, SubmissionStepConfig currentStepConfig)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // find current Step number
        int currentStepNum;
        if (currentStepConfig == null)
        {
            currentStepNum = -1;
        }
        else
        {
            currentStepNum = currentStepConfig.getStepNumber();
        }

        // as long as there are more steps after the current step,
        // do the next step in the current Submission Process
        if (subInfo.getSubmissionConfig().hasMoreSteps(currentStepNum))
        {
            // update the current step & do this step
            currentStepNum++;
            
            //flag that we are going to the start of this next step (for JSPStepManager class)
            setBeginningOfStep(request, true);

            doStep(context, request, response, subInfo, currentStepNum);
        }
        else
        {
            //if this submission is in the workflow process, 
            //forward user back to relevant task page
            if(subInfo.isInWorkflow())
            {
                request.setAttribute("workflow.item", subInfo.getSubmissionItem());
                JSPManager.showJSP(request, response,
                        "/mydspace/perform-task.jsp");
            }
            else
            {
                // The Submission is COMPLETE!!
               
                // save our current Submission information into the Request object
                saveSubmissionInfo(request, subInfo);
    
                // forward to completion JSP
                showProgressAwareJSP(request, response, subInfo, COMPLETE_JSP);
        
            }
        }
    }

    /**
     * Forward processing to the previous step. This method is called if it is
     * determined that the "previous" button was pressed.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            SubmissionInfo pertaining to this submission
     */
    private void doPreviousStep(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, SubmissionStepConfig currentStepConfig)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        int result = doSaveCurrentState(context, request, response, subInfo, currentStepConfig);
        
        // find current Step number
        int currentStepNum;
        if (currentStepConfig == null)
        {
            currentStepNum = -1;
        }
        else
        {
            currentStepNum = currentStepConfig.getStepNumber();
        }

        int currPage=AbstractProcessingStep.getCurrentPage(request);
        double currStepAndPage = Double.parseDouble(currentStepNum+"."+currPage);
        // default value if we are in workflow
        double stepAndPageReached = -1;
        
        if (!subInfo.isInWorkflow())
        {
            stepAndPageReached = Double.parseDouble(getStepReached(subInfo)+"."+JSPStepManager.getPageReached(subInfo));
        }
        
        if (result != AbstractProcessingStep.STATUS_COMPLETE && currStepAndPage != stepAndPageReached)
        {
            doStep(context, request, response, subInfo, currentStepNum);
        }
        
        //Check to see if we are actually just going to a
        //previous PAGE within the same step.
        int currentPageNum = AbstractProcessingStep.getCurrentPage(request);
        
        boolean foundPrevious = false;
        
        //since there are pages before this one in this current step
        //just go backwards one page.
        if(currentPageNum > 1)
        {
            //decrease current page number
            AbstractProcessingStep.setCurrentPage(request, currentPageNum-1);
     
            foundPrevious = true;
            
            //send user back to the beginning of same step!
            //NOTE: the step should handle going back one page
            // in its doPreProcessing() method
            setBeginningOfStep(request, true);

            doStep(context, request, response, subInfo, currentStepNum);
        }
        // Since we cannot go back one page, 
        // check if there is a step before this step. 
        // If so, go backwards one step
        else if (currentStepNum > FIRST_STEP)
        {
            
            currentStepConfig = getPreviousVisibleStep(request, subInfo);
            
            if(currentStepConfig != null)
            {
                currentStepNum = currentStepConfig.getStepNumber();
                foundPrevious = true;
            }
                
            if(foundPrevious)
            {    
                //flag to JSPStepManager that we are going backwards
                //an entire step
                request.setAttribute("step.backwards", Boolean.TRUE);
                
                // flag that we are going back to the start of this step (for JSPStepManager class)
                setBeginningOfStep(request, true);
    
                doStep(context, request, response, subInfo, currentStepNum);
            }    
        }
        
        //if there is no previous, visible step, throw an error!
        if(!foundPrevious)
        {
            log.error(LogManager
                    .getHeader(context, "no_previous_visible_step",
                            "Attempting to go to previous step for step="
                                    + currentStepNum + "." +
                                    "NO PREVIOUS VISIBLE STEP OR PAGE FOUND!"));

            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Process a click on a button in the progress bar. This jumps to the step
     * whose button was pressed.
     * 
     * @param context
     *            DSpace context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            SubmissionInfo pertaining to this submission
     */
    private void doStepJump(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, SubmissionStepConfig currentStepConfig)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Find the button that was pressed. It would start with
        // "submit_jump_".
        String buttonPressed = UIUtil.getSubmitButton(request, "");

        int nextStep = -1; // next step to load
        int nextPage = -1; // page within the nextStep to load

        if (buttonPressed.startsWith("submit_jump_"))
        {
            // Button on progress bar pressed
            try
            {
                // get step & page info (in form: stepNum.pageNum) after
                // "submit_jump_"
                String stepAndPage = buttonPressed.substring(12);

                // split into stepNum and pageNum
                String[] fields = stepAndPage.split("\\."); // split on period
                nextStep = Integer.parseInt(fields[0]);
                nextPage = Integer.parseInt(fields[1]);
            }
            catch (NumberFormatException ne)
            {
                // mangled number
                nextStep = -1;
                nextPage = -1;
            }

            // Integrity check: make sure they aren't going
            // forward or backward too far
            if ((!subInfo.isInWorkflow() && nextStep < FIRST_STEP) ||
                    (subInfo.isInWorkflow() && nextStep < WORKFLOW_FIRST_STEP))
            {
                nextStep = -1;
                nextPage = -1;
            }

            // if trying to jump to a step you haven't been to yet
            if (!subInfo.isInWorkflow() && (nextStep > getStepReached(subInfo)))
            {
                nextStep = -1;
            }
        }

        if (nextStep == -1)
        {
            // Either no button pressed, or an illegal stage
            // reached. UI doesn't allow this, so something's
            // wrong if that happens.
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
        else
        {
            int result = doSaveCurrentState(context, request, response,
                    subInfo, currentStepConfig);

            // Now, if the request was a multi-part (file upload), we need to
            // get the original request back out, as the wrapper causes problems
            // further down the line.
            if (request instanceof FileUploadRequest)
            {
                FileUploadRequest fur = (FileUploadRequest) request;
                request = fur.getOriginalRequest();
            }

            int currStep = currentStepConfig.getStepNumber();
            int currPage = AbstractProcessingStep.getCurrentPage(request);
            double currStepAndPage = Double.parseDouble(currStep + "." + currPage);
            // default value if we are in workflow
            double stepAndPageReached = -1;
            
            if (!subInfo.isInWorkflow())
            {
                stepAndPageReached = Double.parseDouble(getStepReached(subInfo)+"."+JSPStepManager.getPageReached(subInfo));
            }
            
            if (result != AbstractProcessingStep.STATUS_COMPLETE
                    && currStepAndPage != stepAndPageReached)
            {
                doStep(context, request, response, subInfo, currStep);
            }
            else
            {
                // save page info to request (for the step to access)
                AbstractProcessingStep.setCurrentPage(request, nextPage);

                // flag that we are going back to the start of this step (for
                // JSPStepManager class)
                setBeginningOfStep(request, true);

                log.debug("Jumping to Step " + nextStep + " and Page "
                        + nextPage);

                // do the step (the step should take care of going to
                // the specified page)
                doStep(context, request, response, subInfo, nextStep);
            }
        }
    }

    /**
     * Respond to the user clicking "cancel/save" 
     * from any of the steps.  This method first calls
     * the "doPostProcessing()" method of the step, in 
     * order to ensure any inputs are saved.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            SubmissionInfo object
     * @param stepConfig
     *            config of step who's page the user clicked "cancel" on.
     */
    private void doCancelOrSave(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo,
            SubmissionStepConfig stepConfig) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // If this is a workflow item, we need to return the
        // user to the "perform task" page
        if (subInfo.isInWorkflow())
        {
            int result = doSaveCurrentState(context, request, response, subInfo, stepConfig);
            
            if (result == AbstractProcessingStep.STATUS_COMPLETE)
            {
                request.setAttribute("workflow.item", subInfo.getSubmissionItem());
                JSPManager.showJSP(request, response, "/mydspace/perform-task.jsp");                
            }
            else
            {
                int currStep=stepConfig.getStepNumber();
                doStep(context, request, response, subInfo, currStep);
            }
        }
        else
        {
            // if no submission has been started,
            if (subInfo.getSubmissionItem() == null)
            {
                // forward them to the 'cancelled' page,
                // since we haven't created an item yet.
                JSPManager.showJSP(request, response,
                        "/submit/cancelled-removed.jsp");
            }
            else
            {
                //tell the step class to do its processing (to save any inputs)
                //but, send flag that this is a "cancellation"
                setCancellationInProgress(request, true);
                
                int result = doSaveCurrentState(context, request, response, subInfo,
                        stepConfig);
                
                int currStep=stepConfig.getStepNumber();
                int currPage=AbstractProcessingStep.getCurrentPage(request);
                double currStepAndPage = Float.parseFloat(currStep+"."+currPage);
                double stepAndPageReached = Float.parseFloat(getStepReached(subInfo)+"."+JSPStepManager.getPageReached(subInfo));
                
                if (result != AbstractProcessingStep.STATUS_COMPLETE && currStepAndPage < stepAndPageReached){
                    setReachedStepAndPage(context, subInfo, currStep, currPage);
                }
                
                //commit & close context
                context.complete();
                
                // save changes to submission info & step info for JSP
                saveSubmissionInfo(request, subInfo);
                saveCurrentStepConfig(request, stepConfig);

                // forward to cancellation confirmation JSP
                showProgressAwareJSP(request, response, subInfo,
                        "/submit/cancel.jsp");
            }
        }
    }

    private int doSaveCurrentState(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo, SubmissionStepConfig stepConfig)
            throws ServletException
    {
        int result = -1;
        // As long as we're not uploading a file, go ahead and SAVE
        // all of the user's inputs for later
        try
        {
            // call post-processing on Step (to save any inputs from JSP)
            log
                    .debug("Cancel/Save or Jump/Previous Request: calling processing for Step: '"
                            + stepConfig.getProcessingClassName() + "'");

            try
            {
                // load the step class (using the current class loader)
                ClassLoader loader = this.getClass().getClassLoader();
                Class stepClass = loader.loadClass(stepConfig
                        .getProcessingClassName());

                // load the JSPStepManager object for this step
                AbstractProcessingStep step = (AbstractProcessingStep) stepClass
                        .newInstance();

                result = step.doProcessing(context, request, response, subInfo);
            }
            catch (Exception e)
            {
                log.error("Error loading step class'"
                        + stepConfig.getProcessingClassName() + "':", e);
                JSPManager.showInternalError(request, response);
            }
        }
        catch(Exception e)
        {
            throw new ServletException(e);
        }
        return result;
    }

    /**
     * Process information from "submission cancelled" page.
     * This saves the item if the user decided to "cancel & save",
     * or removes the item if the user decided to "cancel & remove".
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     */
    private void processCancelOrSave(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo, SubmissionStepConfig currentStepConfig) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_back");

        if (buttonPressed.equals("submit_back"))
        {
            // re-load current step at beginning
            setBeginningOfStep(request, true);
            doStep(context, request, response, subInfo, currentStepConfig
                    .getStepNumber());
        }
        else if (buttonPressed.equals("submit_remove"))
        {
            // User wants to cancel and remove
            // Cancellation page only applies to workspace items
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();

            workspaceItemService.deleteAll(context, wi);

            JSPManager.showJSP(request, response,
                    "/submit/cancelled-removed.jsp");

            context.complete();
        }
        else if (buttonPressed.equals("submit_keep"))
        {
            // Save submission for later - just show message
            JSPManager.showJSP(request, response, "/submit/saved.jsp");
        }
        else
        {
            doStepJump(context, request, response, subInfo, currentStepConfig);
        }
    }

    // ****************************************************************
    // ****************************************************************
    // MISCELLANEOUS CONVENIENCE METHODS
    // ****************************************************************
    // ****************************************************************

    /**
     * Show a JSP after setting attributes needed by progress bar
     * 
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     * @param jspPath
     *            relative path to JSP
     */
    private static void showProgressAwareJSP(HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, String jspPath)
            throws ServletException, IOException
    {
        saveSubmissionInfo(request, subInfo);

        JSPManager.showJSP(request, response, jspPath);
    }

    /**
     * Reloads a filled-out submission info object from the parameters in the
     * current request. If there is a problem, <code>null</code> is returned.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * 
     * @return filled-out submission info, or null
     */
    public static SubmissionInfo getSubmissionInfo(Context context,
            HttpServletRequest request) throws SQLException, ServletException
    {
        SubmissionInfo info = null;
        
        // Is full Submission Info in Request Attribute?
        if (request.getAttribute("submission.info") != null)
        {
            // load from cache
            info = (SubmissionInfo) request.getAttribute("submission.info");
        }
        else
        {
            
            
            // Need to rebuild Submission Info from Request Parameters
            if (request.getParameter("workflow_id") != null)
            {
                int workflowID = UIUtil.getIntParameter(request, "workflow_id");
                
                info = SubmissionInfo.load(request, workflowItemService.find(context, workflowID));
            }
            else if(request.getParameter("workspace_item_id") != null)
            {
                int workspaceID = UIUtil.getIntParameter(request,
                        "workspace_item_id");
                
                info = SubmissionInfo.load(request, workspaceItemService.find(context, workspaceID));
            }
            else
            {
                //by default, initialize Submission Info with no item
                info = SubmissionInfo.load(request, null);
            }
            
            // We must have a submission object if after the first step,
            // otherwise something is wrong!
            if ((getStepReached(info) > FIRST_STEP)
                    && (info.getSubmissionItem() == null))
            {
                log.warn(LogManager.getHeader(context,
                        "cannot_load_submission_info",
                        "InProgressSubmission is null!"));
                return null;
            }
               

            if (request.getParameter("bundle_id") != null)
            {
                UUID bundleID = UIUtil.getUUIDParameter(request, "bundle_id");
                info.setBundle(bundleService.find(context, bundleID));
            }

            if (request.getParameter("bitstream_id") != null)
            {
                UUID bitstreamID = UIUtil.getUUIDParameter(request,
                        "bitstream_id");
                info.setBitstream(bitstreamService.find(context, bitstreamID));
            }

            // save to Request Attribute
            saveSubmissionInfo(request, info);
        }// end if unable to load SubInfo from Request Attribute

        return info;
    }

    /**
     * Saves the submission info object to the current request.
     * 
     * @param request
     *            HTTP request
     * @param si
     *            the current submission info
     * 
     */
    public static void saveSubmissionInfo(HttpServletRequest request,
            SubmissionInfo si)
    {
        // save to request
        request.setAttribute("submission.info", si);
    }

    /**
     * Get the configuration of the current step from parameters in the request, 
     * along with the current SubmissionInfo object. 
     * If there is a problem, <code>null</code> is returned.
     * 
     * @param request
     *            HTTP request
     * @param si
     *            The current SubmissionInfo object
     * 
     * @return the current SubmissionStepConfig
     */
    public static SubmissionStepConfig getCurrentStepConfig(
            HttpServletRequest request, SubmissionInfo si)
    {
        int stepNum = -1;
        SubmissionStepConfig step = (SubmissionStepConfig) request
                .getAttribute("step");

        if (step == null)
        {
            // try and get it as a parameter
            stepNum = UIUtil.getIntParameter(request, "step");

            // if something is wrong, return null
            if (stepNum < 0 || si == null || si.getSubmissionConfig() == null)
            {
                return null;
            }
            else
            {
                return si.getSubmissionConfig().getStep(stepNum);
            }
        }
        else
        {
            return step;
        }
    }

    /**
     * Saves the current step configuration into the request.
     * 
     * @param request
     *            HTTP request
     * @param step
     *            The current SubmissionStepConfig
     */
    public static void saveCurrentStepConfig(HttpServletRequest request,
            SubmissionStepConfig step)
    {
        // save to request
        request.setAttribute("step", step);
    }

    /**
     * Checks if the current step is also the first "visibile" step in the item submission
     * process.
     * 
     * @param request
     *            HTTP request
     * @param si
     *            The current Submission Info
     * 
     * @return whether or not the current step is the first step
     */
    public static boolean isFirstStep(HttpServletRequest request,
            SubmissionInfo si)
    {
        SubmissionStepConfig step = getCurrentStepConfig(request, si);

        return ((step != null) && (getPreviousVisibleStep(request, si) == null));
    }
    
    /**
     * Return the previous "visibile" step in the item submission
     * process if any, <code>null</code> otherwise.
     * 
     * @param request
     *            HTTP request
     * @param si
     *            The current Submission Info
     * 
     * @return the previous step in the item submission process if any
     */
    public static SubmissionStepConfig getPreviousVisibleStep(HttpServletRequest request,
            SubmissionInfo si)
    {
        SubmissionStepConfig step = getCurrentStepConfig(request, si);

        SubmissionStepConfig currentStepConfig, previousStep = null;

        int currentStepNum = step.getStepNumber();
        
        //need to find a previous step that is VISIBLE to the user!
        while(currentStepNum>FIRST_STEP)
        {
            // update the current step & do this previous step
            currentStepNum--;
        
            //get previous step
            currentStepConfig = si.getSubmissionConfig().getStep(currentStepNum);
        
            if(currentStepConfig.isVisible())
            {
                previousStep = currentStepConfig;
                break;
            }
        }
        return previousStep;
    }

    /**
     * Get whether or not the current step has just begun. This helps determine
     * if we've done any pre-processing yet. If the step is just started, we
     * need to do pre-processing, otherwise we should be doing post-processing.
     * If there is a problem, <code>false</code> is returned.
     * 
     * @param request
     *            HTTP request
     * 
     * @return true if the step has just started (and JSP has not been loaded
     *         for this step), false otherwise.
     */
    public static boolean isBeginningOfStep(HttpServletRequest request)
    {
        Boolean stepStart = (Boolean) request.getAttribute("step.start");

        if (stepStart != null)
        {
            return stepStart.booleanValue();
        }
        else
        {
            return false;
        }
    }

    /**
     * Get whether or not the current step has just begun. This helps determine
     * if we've done any pre-processing yet. If the step is just started, we
     * need to do pre-processing, otherwise we should be doing post-processing.
     * If there is a problem, <code>false</code> is returned.
     * 
     * @param request
     *            HTTP request
     * @param beginningOfStep
     *            true if step just began
     */
    public static void setBeginningOfStep(HttpServletRequest request,
            boolean beginningOfStep)
    {
        request.setAttribute("step.start", Boolean.valueOf(beginningOfStep));
    }

    
    /**
     * Get whether or not a cancellation is in progress (i.e. the 
     * user clicked on the "Cancel/Save" button from any submission
     * page).
     * 
     * @param request
     *            HTTP request
     *            
     * @return true if a cancellation is in progress
     */
    public static boolean isCancellationInProgress(HttpServletRequest request)
    {
        Boolean cancellation = (Boolean) request.getAttribute("submission.cancellation");

        if (cancellation != null)
        {
            return cancellation.booleanValue();
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Sets whether or not a cancellation is in progress (i.e. the 
     * user clicked on the "Cancel/Save" button from any submission
     * page).
     * 
     * @param request
     *            HTTP request
     * @param cancellationInProgress
     *            true if cancellation is in progress
     */
    private static void setCancellationInProgress(HttpServletRequest request, boolean cancellationInProgress)
    {
        request.setAttribute("submission.cancellation", Boolean.valueOf(cancellationInProgress));
    }
    
    
    /**
     * Return the submission info as hidden parameters for an HTML form on a JSP
     * page.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @return HTML hidden parameters
     */
    public static String getSubmissionParameters(Context context,
            HttpServletRequest request) throws SQLException, ServletException
    {
        SubmissionInfo si = getSubmissionInfo(context, request);

        SubmissionStepConfig step = getCurrentStepConfig(request, si);

        String info = "";

        if ((si.getSubmissionItem() != null) && si.isInWorkflow())
        {
            info = info
                    + "<input type=\"hidden\" name=\"workflow_id\" value=\""
                    + si.getSubmissionItem().getID() + "\"/>";
        }
        else if (si.getSubmissionItem() != null)
        {
            info = info
                    + "<input type=\"hidden\" name=\"workspace_item_id\" value=\""
                    + si.getSubmissionItem().getID() + "\"/>";
        }

        if (si.getBundle() != null)
        {
            info = info + "<input type=\"hidden\" name=\"bundle_id\" value=\""
                    + si.getBundle().getID() + "\"/>";
        }

        if (si.getBitstream() != null)
        {
            info = info
                    + "<input type=\"hidden\" name=\"bitstream_id\" value=\""
                    + si.getBitstream().getID() + "\"/>";
        }

        if (step != null)
        {
            info = info + "<input type=\"hidden\" name=\"step\" value=\""
                    + step.getStepNumber() + "\"/>";
        }

        // save the current page from the current Step Servlet
        int page = AbstractProcessingStep.getCurrentPage(request);
        info = info + "<input type=\"hidden\" name=\"page\" value=\"" + page
                + "\"/>";

        // save the current JSP name to a hidden variable
        String jspDisplayed = JSPStepManager.getLastJSPDisplayed(request);
        info = info + "<input type=\"hidden\" name=\"jsp\" value=\""
                   + jspDisplayed + "\"/>";

        return info;
    }

   

    /**
     * Indicate the user has advanced to the given stage. This will only
     * actually do anything when it's a user initially entering a submission. It
     * will only increase the "stage reached" column - it will not "set back"
     * where a user has reached. Whenever the "stage reached" column is
     * increased, the "page reached" column is reset to 1, since you've now
     * reached page #1 of the next stage.
     * 
     * @param subInfo
     *            the SubmissionInfo object pertaining to the current submission
     * @param step
     *            the step the user has just reached
     */
    private void userHasReached(Context c, SubmissionInfo subInfo, int step)
            throws SQLException, AuthorizeException, IOException
    {
        if (!subInfo.isInWorkflow() && subInfo.getSubmissionItem() != null)
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();

            if (step > wi.getStageReached())
            {
                wi.setStageReached(step);
                wi.setPageReached(1); // reset page reached back to 1 (since
                                        // it's page 1 of the new step)
                workspaceItemService.update(c, wi);
            }
        }
    }
    
    /**
    * Set a specific step and page as reached. 
    * It will also "set back" where a user has reached.
    * 
    * @param subInfo
     *            the SubmissionInfo object pertaining to the current submission
    * @param step the step to set as reached, can be also a previous reached step
    * @param page the page (within the step) to set as reached, can be also a previous reached page
    */
    private void setReachedStepAndPage(Context c, SubmissionInfo subInfo, int step,
            int page) throws SQLException, AuthorizeException, IOException
    {
        if (!subInfo.isInWorkflow() && subInfo.getSubmissionItem() != null)
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();

            wi.setStageReached(step);
            wi.setPageReached(page);
            workspaceItemService.update(c, wi);
        }
    }

    
    /**
     * Find out which step a user has reached in the submission process. If the
     * submission is in the workflow process, this returns -1.
     * 
     * @param subInfo
     *            submission info object
     * 
     * @return step reached
     */
    public static int getStepReached(SubmissionInfo subInfo)
    {
        if (subInfo == null || subInfo.isInWorkflow() || subInfo.getSubmissionItem() == null)
        {
            return -1;
        }
        else
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();
            int i = wi.getStageReached();

            // Uninitialised workspace items give "-1" as the stage reached
            // this is a special value used by the progress bar, so we change
            // it to "FIRST_STEP"
            if (i == -1)
            {
                i = FIRST_STEP;
            }

            return i;
        }
    }

    
    /**
     * Wraps a multipart form request, so that its attributes and parameters can
     * still be accessed as normal.
     * 
     * @return wrapped multipart request object
     * 
     * @throws ServletException
     *             if there are no more pages in this step
     */
    private HttpServletRequest wrapMultipartRequest(HttpServletRequest request)
            throws ServletException, FileSizeLimitExceededException
    {
        HttpServletRequest wrappedRequest;

        try
        {
            // if not already wrapped
            if (!Class.forName("org.dspace.app.webui.util.FileUploadRequest")
                    .isInstance(request))
            {
                // Wrap multipart request
                wrappedRequest = new FileUploadRequest(request);

                return (HttpServletRequest) wrappedRequest;
            }
            else
            { // already wrapped
                return request;
            }
        }
        catch (FileSizeLimitExceededException e)
        {
            throw new FileSizeLimitExceededException(e.getMessage(),e.getActualSize(),e.getPermittedSize());
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }
    
    
    /**
     * Upload any files found on the Request or in assembledFiles, and save them back as 
     * Request attributes, for further processing by the appropriate user interface.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     */
    public void uploadFiles(Context context, HttpServletRequest request)
            throws ServletException
    {
        FileUploadRequest wrapper = null;
        String filePath = null;
        InputStream fileInputStream = null;

        try
        {
            // if we already have a FileUploadRequest, use it
            if (Class.forName("org.dspace.app.webui.util.FileUploadRequest")
                    .isInstance(request))
            {
                wrapper = (FileUploadRequest) request;
            }
            else
            {
                // Wrap multipart request to get the submission info
                wrapper = new FileUploadRequest(request);
            }
            
            log.debug("Did not recoginze resumable upload, falling back to "
                    + "simple upload.");
            Enumeration fileParams = wrapper.getFileParameterNames();
            while (fileParams.hasMoreElements()) 
            {
                String fileName = (String) fileParams.nextElement();

                File temp = wrapper.getFile(fileName);

                //if file exists and has a size greater than zero
                if (temp != null && temp.length() > 0) 
                {
                    // Read the temp file into an inputstream
                    fileInputStream = new BufferedInputStream(
                            new FileInputStream(temp));

                    filePath = wrapper.getFilesystemName(fileName);

                    // cleanup our temp file
                    if (!temp.delete()) 
                    {
                        log.error("Unable to delete temporary file");
                    }

                    //save this file's info to request (for UploadStep class)
                    request.setAttribute(fileName + "-path", filePath);
                    request.setAttribute(fileName + "-inputstream", fileInputStream);
                    request.setAttribute(fileName + "-description", wrapper.getParameter("description"));
                }
            }
        }
        catch (Exception e)
        {
            // Problem with uploading
            log.warn(LogManager.getHeader(context, "upload_error", ""), e);
            throw new ServletException(e);
        }
    }
    
    // Resumable.js uses HTTP Get to recognize whether a specific part/chunk of 
    // a file was uploaded already. This method handles those requests.
    protected void DoGetResumable(HttpServletRequest request, HttpServletResponse response) 
        throws IOException
    {
        if (ConfigurationManager.getProperty("upload.temp.dir") != null)
        {
            tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        }
        else
        {
            tempDir = System.getProperty("java.io.tmpdir");
        }

        String resumableIdentifier = request.getParameter("resumableIdentifier");
        String resumableChunkNumber = request.getParameter("resumableChunkNumber");
        long resumableCurrentChunkSize = 
                Long.valueOf(request.getParameter("resumableCurrentChunkSize"));

        tempDir = tempDir + File.separator + resumableIdentifier;

        File fileDir = new File(tempDir);

        // create a new directory for each resumableIdentifier
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        // use the String "part" and the chunkNumber as filename of a chunk
        String chunkPath = tempDir + File.separator + "part" + resumableChunkNumber;

        File chunkFile = new File(chunkPath);
        // if the chunk was uploaded already, we send a status code of 200
        if (chunkFile.exists()) {
            if (chunkFile.length() == resumableCurrentChunkSize) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            // The chunk file does not have the expected size, delete it and 
            // pretend that it wasn't uploaded already.
            chunkFile.delete();
        }
        // if we don't have the chunk send a http status code 404
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    // Resumable.js sends chunks of files using http post.
    // If a chunk was the last missing one, we have to assemble the file and
    // return it. If other chunks are missing, we just return null.
    protected File doPostResumable(HttpServletRequest request)
            throws FileSizeLimitExceededException, IOException, ServletException 
    {
        File completedFile = null;
        FileUploadRequest wrapper = null;
        
        if (ConfigurationManager.getProperty("upload.temp.dir") != null)
        {
            tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        }
        else {
            tempDir = System.getProperty("java.io.tmpdir");
        }
        
        try
        {
            // if we already have a FileUploadRequest, use it
            if (Class.forName("org.dspace.app.webui.util.FileUploadRequest").isInstance(request))
            {
                wrapper = (FileUploadRequest) request;
            } 
            else // if not wrap the mulitpart request to get the submission info
            {
                wrapper = new FileUploadRequest(request);
            }
        }
        catch (ClassNotFoundException ex)
        {
            // Cannot find a class that is part of the JSPUI?
            log.fatal("Cannot find class org.dspace.app.webui.util.FileUploadRequest");
            throw new ServletException("Cannot find class org.dspace.app.webui.util.FileUploadRequest.", ex);
        }

        String resumableIdentifier = wrapper.getParameter("resumableIdentifier");
        long resumableTotalSize = Long.valueOf(wrapper.getParameter("resumableTotalSize"));
        int resumableTotalChunks = Integer.valueOf(wrapper.getParameter("resumableTotalChunks"));

        String chunkDirPath = tempDir + File.separator + resumableIdentifier;
        File chunkDirPathFile = new File(chunkDirPath);
        boolean foundAll = true;
        long currentSize = 0l;
        
        // check whether all chunks were received.
        if(chunkDirPathFile.exists())
        {
            for (int p = 1; p <= resumableTotalChunks; p++) 
            {
                File file = new File(chunkDirPath + File.separator + "part" + Integer.toString(p));

                if (!file.exists()) 
                {
                    foundAll = false;
                    break;
                }
                currentSize += file.length();
            }
        }
        
        if (foundAll && currentSize >= resumableTotalSize) 
        {
            try {
                // assemble the file from it chunks.
                File file = makeFileFromChunks(tempDir, chunkDirPathFile, wrapper);
            
                if (file != null) 
                {
                    completedFile = file;
                }
            } catch (IOException ex) {
                // if the assembling of a file results in an IOException a
                // retransmission has to be triggered. Throw the IOException
                // here and handle it above.
                throw ex;
            }
        }

        return completedFile;
    }
    
    // assembles a file from it chunks
    protected File makeFileFromChunks(String tmpDir, File chunkDirPath, HttpServletRequest request) 
            throws IOException
    {
        int resumableTotalChunks = Integer.valueOf(request.getParameter("resumableTotalChunks"));
        String resumableFilename = request.getParameter("resumableFilename");
        String chunkPath = chunkDirPath.getAbsolutePath() + File.separator + "part";
        File destFile = null;

        String destFilePath = tmpDir + File.separator + resumableFilename;
        destFile = new File(destFilePath);
        InputStream is = null;
        OutputStream os = null;

        try {
            destFile.createNewFile();
            os = new FileOutputStream(destFile);

            for (int i = 1; i <= resumableTotalChunks; i++) 
            {
                File fi = new File(chunkPath.concat(Integer.toString(i)));
                try 
                {
                    is = new FileInputStream(fi);

                    byte[] buffer = new byte[1024];

                    int lenght;

                    while ((lenght = is.read(buffer)) > 0) 
                    {
                        os.write(buffer, 0, lenght);
                    }
                } 
                catch (IOException e) 
                {
                    // try to delete destination file, as we got an exception while writing it.
                    if(!destFile.delete())
                    {
                        log.warn("While writing an uploaded file an error occurred. "
                                + "We were unable to delete the damaged file: " 
                                + destFile.getAbsolutePath() + ".");
                    }
                    // throw IOException to handle it in the calling method
                    throw e;
                }
                finally {
                    try
                    {
                        if (is != null)
                        {
                            is.close();
                        }
                    }
                    catch (IOException ex)
                    {
                        // nothing to do here
                    }
                }
            }
        } 
        finally 
        {
            try 
            {
                if (os != null) 
                {
                    os.close();
                }
            } 
            catch (IOException ex) 
            {
                // nothing to do here
            }
            if (!deleteDirectory(chunkDirPath)) 
            {
                log.warn("Coudln't delete temporary upload path " + chunkDirPath.getAbsolutePath() + ", ignoring it.");
            }
        }
        return destFile;
    }

    public boolean deleteDirectory(File path) 
    {
        if (path.exists()) 
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) 
            {
                if (files[i].isDirectory()) 
                {
                    deleteDirectory(files[i]);
                } 
                else 
                {
                    files[i].delete();
                }
            }
        }
        
        return (path.delete());
    }

}
