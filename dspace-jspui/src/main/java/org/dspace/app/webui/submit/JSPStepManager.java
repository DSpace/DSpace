/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.webui.servlet.SubmissionController;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

/**
 * Manages and processes all JSP-UI classes for DSpace Submission steps.
 * <P>
 * This manager is utilized by the SubmissionController to appropriately
 * load each JSP-UI step, and process any information returned by each step
 * 
 * @see org.dspace.submit.AbstractProcessingStep
 * @see org.dspace.app.webui.servlet.SubmissionController
 * @see org.dspace.app.webui.submit.JSPStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPStepManager
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPStepManager.class);

    /**
     * Current Processing class for step that is being processed by the JSPStepManager
     * This is the class that performs processing of information entered in during a step
     */
    private AbstractProcessingStep stepProcessing = null;
    
    /**
     * Current JSP-UI binding class for step that is being processed by the JSPStepManager
     * This is the class that manages calling all JSPs, and determines if additional processing
     * of information (or confirmation) is necessary.
     */
    private JSPStep stepJSPUI = null;
    
    /** 
     * The SubmissionStepConfig object describing the current step 
     **/
    private SubmissionStepConfig stepConfig = null;

    /**
     * Initialize the current JSPStepManager object, by loading the 
     * specified step class.
     * 
     * @param stepConfig
     *            the SubmissionStepConfig object which describes
     *            this step's configuration in the item-submission.xml
     *            
     * @throws Exception
     *             if the JSPStep cannot be loaded or the class
     *             specified doesn't implement the JSPStep interface
     */
    public static JSPStepManager loadStep(SubmissionStepConfig stepConfig) throws Exception
    {
        
        JSPStepManager stepManager = new JSPStepManager();
        
        //save step configuration 
        stepManager.stepConfig = stepConfig;
        
        
        /*
         * First, load the step processing class (using the current class loader)
         */
        ClassLoader loader = stepManager.getClass().getClassLoader();
        Class stepClass = loader
                .loadClass(stepConfig.getProcessingClassName());

        Object stepInstance =  stepClass.newInstance();
        
        if(stepInstance instanceof AbstractProcessingStep)
        {
            // load the JSPStep interface for this step
            stepManager.stepProcessing = (AbstractProcessingStep) stepClass.newInstance();
        }
        else
        {
            throw new Exception("The submission step class specified by '" + stepConfig.getProcessingClassName() + 
                    "' does not extend the class org.dspace.submit.AbstractProcessingStep!" +
                    " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
        }
        
        
        /*
         * Next, load the step's JSPUI binding class (using the current class loader)
         * (Only load JSPUI binding class if specified...otherwise this is a non-interactive step)
         */
        if(stepConfig.getJSPUIClassName()!=null && stepConfig.getJSPUIClassName().length()>0)
        {
        	stepClass = loader
                	.loadClass(stepConfig.getJSPUIClassName());

        	stepInstance =  stepClass.newInstance();
        
	        if(stepInstance instanceof JSPStep)
	        {
	            // load the JSPStep interface for this step
	            stepManager.stepJSPUI = (JSPStep) stepClass.newInstance();
	        }
	        else
	        {
	            throw new Exception("The submission step class specified by '" + stepConfig.getJSPUIClassName() + 
	                    "' does not extend the class org.dspace.app.webui.JSPStep!" +
	                    " Therefore it cannot be used by the Configurable Submission for the JSP user interface!");
	        }
        }
        return stepManager;
    }

    
    /**
     * Initialize the current JSPStepManager object, to prepare for processing
     * of this step. This method is called by the SubmissionController, and its
     * job is to determine which "page" of the step the current user is on.
     * <P>
     * Once the page has been determined, this method also determines whether
     * the user is completing this page (i.e. there is user input data that
     * needs to be saved to the database) or beginning this page (i.e. the user
     * needs to be sent to the JSP for this page).
     * 
     * @param context
     *            a DSpace Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param subInfo
     *            submission info object
     * 
     * @return true if step is completed (successfully), false otherwise
     * 
     * @throws ServletException
     *             if a general error occurs
     * @throws IOException
     *             if a i/o error occurs
     * @throws SQLException
     *             if a database error occurs
     * @throws AuthorizeException
     *             if some authorization error occurs
     */
    public final boolean processStep(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * This method SHOULD NOT BE OVERRIDDEN, unless it's absolutely
         * necessary. If you override this method, make sure you call the
         * "doStepStart()" and "doStepEnd()" methods at the appropriate time in
         * your processStep() method.
         * 
         */

        /*
         * Figure out Current Page in this Step
         */
        int currentPage = -1;
        // if user came to this Step by pressing "Previous" button
        // from the following step, then we will have to calculate
        // the correct page to go to.
        if ((request.getAttribute("step.backwards") != null)
                && ((Boolean) request.getAttribute("step.backwards"))
                        .booleanValue())
        {
            // current page should be the LAST page in this step
            currentPage = getNumPagesInProgressBar(subInfo, this.stepConfig
                    .getStepNumber());

            AbstractProcessingStep.setCurrentPage(request, currentPage);
        }
        else
        {
            // retrieve current page number from request
            currentPage = AbstractProcessingStep.getCurrentPage(request);
        }

        /*
         * Update Last page reached (if necessary)
         */
        int pageReached = getPageReached(subInfo);

        // TD: Special Case, where an item was just rejected & returned to
        // Workspace. In this case, we have an invalid "pageReached" in
        // the database which needs updating
        if (pageReached == AbstractProcessingStep.LAST_PAGE_REACHED)
        {
            // the first time this flag is encountered, we have to update
            // the Database with the number of the last page in the current
            // StepReached

            // get the number of pages in the last step reached
            int lastPageNum = getNumPagesInProgressBar(subInfo,
                    SubmissionController.getStepReached(subInfo));

            // update database with the number of this last page
            updatePageReached(context, subInfo, lastPageNum);
        }
        // otherwise, check if pageReached needs updating
        // (it only needs updating if we're in the latest step reached,
        // and we've made it to a later page in that step)
        else if ((this.stepConfig.getStepNumber() == SubmissionController
                .getStepReached(subInfo))
                && (currentPage > pageReached))
        {
            // reset page number reached & commit to database
            updatePageReached(context, subInfo, currentPage);
        }

        /*
         * Determine whether we are Starting or Finishing this Step
         */
        // check if we just started this step
        boolean beginningOfStep = SubmissionController
                .isBeginningOfStep(request);

        // if this step has just been started, do beginning processing
        if (beginningOfStep)
        {
            return doStepStart(context, request, response, subInfo);
        }
        else
        // We just returned from a page (i.e. JSP), so we need to do any end
        // processing
        {
            return doStepEnd(context, request, response, subInfo);
        }
    }

    /**
     * Do the beginning of a Step. First do pre-processing, then display the JSP
     * (if there is one). If there's no JSP, just End the step immediately.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * 
     * @return true if the step is completed (no JSP was loaded), false
     *         otherwise
     * 
     */
    private boolean doStepStart(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        log.debug("Doing pre-processing for step " + this.getClass().getName());

        // first, do any pre-processing and get the JSP to display
        // (assuming that this step has an interface)
        if(stepJSPUI!=null)
        {
            stepJSPUI.doPreProcessing(context, request, response, subInfo);
        }

        // Complete this step, if this response has not already
        // been committed.
        //
        // Note: the response should only be "committed" if the user
        // has be forwarded on to a JSP page (in which case the step
        // is not finished!).
        if (!response.isCommitted())
        {
            // Otherwise, this is a non-interactive step!
            // There's no JSP to display, so only perform the processing
            // and forward back to the Submission Controller servlet

            log.debug("Calling processing for step "
                    + this.getClass().getName());
            int errorFlag = stepProcessing.doProcessing(context, request, response, subInfo);

            // if it didn't complete successfully, try and log this error!
            if (errorFlag != AbstractProcessingStep.STATUS_COMPLETE)
            {
                // see if an error message was defined!
                String errorMessage = stepProcessing.getErrorMessage(errorFlag);

                // if not defined, construct a dummy error
                if (errorMessage == null)
                {
                    errorMessage = "The doProcessing() method for "
                            + this.getClass().getName()
                            + " returned an error flag = "
                            + errorFlag
                            + ". "
                            + "It is recommended to define a custom error message for this error flag using the addErrorMessage() method!";
                }

                log.error(errorMessage);
            }

            return completeStep(context, request, response, subInfo);
        }
        else
        {
            return false; // step not completed
        }
    }

    /**
     * This method actually displays a JSP page for this "step". This method can
     * be called from "doPostProcessing()" to display an error page, etc.
     * 
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param pathToJSP
     *            context path to the JSP to display
     * 
     */
    public static final void showJSP(HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo,
            String pathToJSP) throws ServletException, IOException,
            SQLException
    {

        // As long as user is not currently cancelling
        // or saving submission, show the JSP specified
        if (!response.isCommitted()
                && !SubmissionController.isCancellationInProgress(request))
        {
            // set beginningOfStep flag to false
            // (so that we know it will be time for "post-processing"
            // after loading the JSP)
            SubmissionController.setBeginningOfStep(request, false);

            // save our current Submission information into the Request
            // object (so JSP has access to it)
            SubmissionController.saveSubmissionInfo(request, subInfo);

            // save our current page information to Request object
            //setCurrentPage(request, currentPage);

            // save the JSP we are displaying
            setLastJSPDisplayed(request, pathToJSP);

            // load JSP
            JSPManager.showJSP(request, response, pathToJSP);
        }
    }

    /**
     * Do the end of a Step. If "cancel" or "progress bar" was pressed, we need
     * to forward back to the SubmissionManagerServlet immediately. Otherwise,
     * do Post-processing, and figure out if this step is truly finished or not.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return true if the step is completed (successfully), false otherwise
     * 
     */
    private boolean doStepEnd(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // we've returned from the JSP page
        // and need to do the processing for this step
        log.debug("Calling processing for step " + this.getClass().getName());

        int status = stepProcessing.doProcessing(context, request, response, subInfo);

        log.debug("Calling post-processing for step "
                + this.getClass().getName());

        // After doing the processing, we have to do any post-processing
        // of any potential error messages, in case we need to re-display
        // the JSP
        stepJSPUI.doPostProcessing(context, request, response, subInfo, status);

        int currentPage = AbstractProcessingStep.getCurrentPage(request);
        
        // Assuming step didn't forward back to a JSP during
        // post-processing, then check to see if this is the last page!
        if (!response.isCommitted())
        {
            // check if this step has more pages!
            if (!hasMorePages(request, subInfo, currentPage))
            {
                // this step is complete! (return completion
                // status to SubmissionController)
                return completeStep(context, request, response, subInfo);
            }
            else
            {
                // otherwise, increment to the next page
                AbstractProcessingStep.setCurrentPage(request, currentPage + 1);

                // reset to beginning of step (so that pre-processing is run
                // again)
                SubmissionController.setBeginningOfStep(request, true);

                // recursively call processStep to reload this Step class for
                // the next page
                return processStep(context, request, response, subInfo);
            }
        }
        else
        {
            // this is the case when a response was already given
            // in doPostProcessing(), which means the user was
            // forwarded to a JSP page.
            return false;
        }
    }

    /**
     * This method completes the processing of this "step" and forwards the
     * request back to the SubmissionController (so that the next step can be
     * called).
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * 
     * @return true if step completed (successfully), false otherwise
     */
    protected final boolean completeStep(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.debug("Completing Step " + this.getClass().getName());

        // If a response has not already been sent to the web browser,
        // then forward control back to the SubmissionController
        // (and let the controller know this step is complete!
        //
        // Note: The only cases in which a response would have already
        // been sent to the web browser is if an error occurred or the
        // Submission Process had to immediately exit for some reason
        // (e.g. user rejected license, or tried to submit a thesis while theses
        // are blocked, etc.)
        if (!response.isCommitted())
        {
            // save our current Submission information into the Request object
            SubmissionController.saveSubmissionInfo(request, subInfo);

            // save the current step into the Request object
            SubmissionController.saveCurrentStepConfig(request, stepConfig);

            // reset current page back to 1
            AbstractProcessingStep.setCurrentPage(request, 1);

            // return success (true) to controller!
            return true;
        }
        else
        {
            return false; // couldn't return to controller since response is
                            // committed
        }
    }

    /**
     * Checks to see if there are more pages in the current step after the
     * specified page
     * 
     * @param request
     *            The HTTP Request object
     * @param subInfo
     *            The current submission information object
     * @param pageNumber
     *            The current page
     * 
     * @throws ServletException
     *             if there are no more pages in this step
     * 
     */
    protected final boolean hasMorePages(HttpServletRequest request,
            SubmissionInfo subInfo, int pageNumber) throws ServletException
    {
        int numberOfPages = stepProcessing.getNumberOfPages(request, subInfo);

        return (pageNumber < numberOfPages);
    }

    /**
     * Find out which page a user has reached in this particular step.
     * 
     * @param subInfo
     *            Submission information
     * 
     * @return page reached
     */
    public static final int getPageReached(SubmissionInfo subInfo)
    {
        if (subInfo.isInWorkflow() || subInfo.getSubmissionItem() == null)
        {
            return -1;
        }
        else
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();
            int i = wi.getPageReached();

            return i;
        }
    }

    /**
     * Sets the number of the page reached for the specified step
     * 
     * @param session
     *            HTTP session (where page reached is stored)
     * @param step
     *            the current Submission Process Step (which we want to
     *            increment the page reached)
     * @param pageNumber
     *            new page reached
     */
    private void updatePageReached(Context context, SubmissionInfo subInfo, int page)
            throws SQLException, AuthorizeException, IOException
    {
        if (!subInfo.isInWorkflow() && subInfo.getSubmissionItem() != null)
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();

            if (page > wi.getPageReached())
            {
            	WorkspaceItemService wis = ContentServiceFactory.getInstance()
            			.getWorkspaceItemService();
                wi.setPageReached(page);
                wis.update(context, wi);
            }
        }
    }

    /**
     * Return the number pages for the given step in the Submission Process
     * according to the Progress Bar. So, if "stepNum" is 7, this will return
     * the number of pages in step #7 (at least according to the progressBar)
     * 
     * @param subInfo
     *            current Submission Information object
     * @param stepNum
     *            the number of the step
     * 
     * @return the number of pages in the step
     */
    private static int getNumPagesInProgressBar(SubmissionInfo subInfo,
            int stepNum)
    {
        // get the keys of the progressBar information (key format:
        // stepNum.pageNum)
        Iterator keyIterator = subInfo.getProgressBarInfo().keySet().iterator();

        // default to last page being page #1
        int lastPage = 1;

        while (keyIterator.hasNext())
        {
            // find step & page info (format: stepNum.pageNum)
            String stepAndPage = (String) keyIterator.next();

            if (stepAndPage.startsWith(stepNum + "."))
            {
                // split into stepNum and pageNum
                String[] fields = stepAndPage.split("\\."); // split on period
                int page = Integer.parseInt(fields[1]);

                if (page > lastPage)
                {
                    // update last page found for this step
                    lastPage = page;
                }
            }
        }

        // return # of last page found for this step (which is also the number
        // of pages)
        return lastPage;
    }

    /**
     * Retrieves the context path of the last JSP that was displayed to the
     * user. This is useful in the "doPostProcessing()" method to determine
     * which JSP has just submitted form information.
     * 
     * @param request
     *            current servlet request object
     * 
     * @return pathToJSP The context path to the JSP page (e.g.
     *         "/submit/select-collection.jsp")
     */
    public static final String getLastJSPDisplayed(HttpServletRequest request)
    {
        String jspDisplayed = (String) request.getAttribute("jsp");

        if ((jspDisplayed == null) || jspDisplayed.length() == 0)
        {
            // try and retrieve the JSP name as a request parameter
            if (request.getParameter("jsp") == null)
            {
                jspDisplayed = "";
            }
            else
            {
                jspDisplayed = request.getParameter("jsp");
            }
        }

        return jspDisplayed;
    }

    /**
     * Saves the context path of the last JSP that was displayed to the user.
     * 
     * @param request
     *            current servlet request object
     * @param pathToJSP
     *            The context path to the JSP page (e.g.
     *            "/submit/select-collection.jsp")
     */
    private static final void setLastJSPDisplayed(HttpServletRequest request,
            String pathToJSP)
    {
        // save to request
        request.setAttribute("jsp", pathToJSP);
    }
    
    
    /**
     * Return the URL path (e.g. /submit/review-metadata.jsp) of the JSP
     * which will review the information that was gathered in the currently
     * loaded Step.
     * <P>
     * This Review JSP is loaded by the 'Verify' Step, in order to dynamically
     * generate a submission verification page consisting of the information
     * gathered in all the enabled submission steps.
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
    public String getReviewJSP(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
    {
        return stepJSPUI.getReviewJSP(context, request, response, subInfo);
    }
    
}
