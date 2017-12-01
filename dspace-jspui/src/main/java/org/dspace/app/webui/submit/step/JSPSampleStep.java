/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.step.SampleStep;

/**
 * This is a Sample Step class which can be used as template for creating new
 * custom JSP-UI Step classes!
 * <p>
 * Please Note: This class works in conjunction with the 
 * org.dspace.submit.step.SampleStep , which is a sample step
 * processing class!
 * <p>
 * This step can be added to any Submission process (for testing purposes) by
 * adding the following to the appropriate <submission-process> tag in the
 * /config/item-submission.xml:
 * 
 * <step> 
 * <heading>Sample</heading>
 * <processing-class>org.dspace.submit.step.SampleStep</processing-class>
 * <jspui-binding>org.dspace.app.webui.submit.step.JSPSampleStep</jspui-binding> 
 * <workflow-editable>true</workflow-editable>
 * </step>
 *
 * <P>
 * The following methods are called in this order:
 * <ul>
 * <li>Call doPreProcessing() method</li>
 * <li>If showJSP() was specified from doPreProcessing(), then the JSP
 * specified will be displayed</li>
 * <li>If showJSP() was not specified from doPreProcessing(), then the
 * doProcessing() method is called and the step completes immediately</li>
 * <li>Call doProcessing() method on appropriate AbstractProcessingStep after
 * the user returns from the JSP, in order to process the user input</li>
 * <li>Call doPostProcessing() method to determine if more user interaction is
 * required, and if further JSPs need to be called.</li>
 * <li>If there are more "pages" in this step then, the process begins again
 * (for the new page).</li>
 * <li>Once all pages are complete, control is forwarded back to the
 * SubmissionController, and the next step is called.</li>
 * </ul>
 * 
 * @see org.dspace.app.webui.servlet.SubmissionController
 * @see org.dspace.app.webui.submit.JSPStep
 * @see org.dspace.submit.step.SampleStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPSampleStep extends JSPStep
{
    /** JSP which displays the step to the user * */
    private static final String DISPLAY_JSP = "/submit/sample-step.jsp";

    
    /** JSP which displays information to be reviewed during 'verify step' * */
    private static final String REVIEW_JSP = "/submit/review-sample.jsp";

    /**
     * Do any pre-processing to determine which JSP (if any) is used to generate
     * the UI for this step. This method should include the gathering and
     * validating of all data required by the JSP. In addition, if the JSP
     * requires any variable to passed to it on the Request, this method should
     * set those variables.
     * <P>
     * If this step requires user interaction, then this method must call the
     * JSP to display, using the "showJSP()" method of the JSPStepManager class.
     * <P>
     * If this step doesn't require user interaction OR you are solely using
     * Manakin for your user interface, then this method may be left EMPTY,
     * since all step processing should occur in the doProcessing() method.
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
    public void doPreProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        /*
         * In this method, you should do any pre-processing required before the
         * Step's JSP can be displayed, and call the first JSP to display (using
         * the showJSP() method of JSPStepManager)
         * 
         * 
         * Pre-processing may include, but is not limited to:
         * 
         * 1) Gathering necessary data (to be displayed on the JSP) from
         * database or other DSpace classes. This data should be stored in the
         * HttpServletRequest object (using request.setAttribute()) so that your
         * JSP can access it! 2) Based on the input, determine which JSP you
         * actually want to load! This can be especially important for Steps
         * which use multiple JSPs. 3) For steps with multiple "pages", you may
         * want to use the inherited getCurrentPage() method to determine which
         * page you are currently on, so that you know which JSP to show!
         * 
         * In order for your JSP to load after this doPreProcessing() step, you
         * *MUST* call the inherited showJSP() method!
         * 
         * NOTE: a Step need not even use a JSP (if it's just a step which does
         * backend processing, and doesn't require a Web User Interface). For
         * these types of "processing-only" Steps, you should perform all of
         * your processing in the "doProcessing()" method, and this
         * doPreProcessing() method should be left EMPTY!
         * 
         */

        /*
         * FAQ:
         * 
         * 1) How many times is doPreProcessing() called for a single Step?
         * 
         * It's called once for each page in the step. So, if getNumberOfPages()
         * returns 1, it's called only once. If getNumberOfPages() returns 2,
         * it's called twice (once for each page), etc.
         */

        /*
         * Here's some example code that sets a Request attribute for the
         * sample-step.jsp to use, and then Loads the sample-step.jsp
         */
        // retrieve my DSpace URL from the Configuration File!
        String myDSpaceURL = ConfigurationManager.getProperty("dspace.url");
        request.setAttribute("my.url", myDSpaceURL);

        // Tell JSPStepManager class to load "sample-step.jsp"
        JSPStepManager.showJSP(request, response, subInfo, DISPLAY_JSP);
    }

    /**
     * Do any post-processing after the step's backend processing occurred (in
     * the doProcessing() method).
     * <P>
     * It is this method's job to determine whether processing completed
     * successfully, or display another JSP informing the users of any potential
     * problems/errors.
     * <P>
     * If this step doesn't require user interaction OR you are solely using
     * Manakin for your user interface, then this method may be left EMPTY,
     * since all step processing should occur in the doProcessing() method.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param status
     *            any status/errors reported by doProcessing() method
     */
    public void doPostProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int status)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        /*
         * In this method, you should do any post-processing of errors or
         * messages returned by doProcessing() method, to determine if the user
         * has completed this step or if further user input is required.
         * 
         * This method is called immediately AFTER doProcessing(), and any
         * errors or messages returned by doProcessing() are passed to this
         * method as an input.
         * 
         * Post-processing includes, but is not limited to: 1) Validating that
         * the user filled out required fields (and if not, forwarding him/her
         * back to appropriate JSP) 2) Determining which JSP to display based on
         * the error or status message which was passed into this method from
         * doProcessing()
         * 
         * NOTE: If this step has multiple "pages" defined (i.e. the below
         * getNumberOfPages() method returns > 1), then you DO NOT need to load
         * the next page here! After the post-processing of the first page
         * completes, the JSPStepManager class will call "doPreProcessing()"
         * automatically for page #2, etc.
         * 
         * Once post-processing completes, and the JSPStepManager class
         * determines there are no more pages to this step, control is sent back
         * to the SubmissionController (and the next step is called).
         */

        /***********************************************************************
         * IMPORTANT FUNCTIONS to be aware of :
         **********************************************************************/

        // This function retrieves the path of the JSP which just submitted its
        // form to this class (e.g. "/submit/sample-step.jsp", in this case)
        // String lastJSPDisplayed = JSPStepManager.getLastJSPDisplayed(request);

        // This function retrieves the number of the current "page"
        // within this Step. This is useful if your step actually
        // has more than one "page" within the Progress Bar. It can
        // help you determine which Page the user just came from,
        // as well as determine which JSP to load in doPreProcessing()
        // int currentPageNum = SampleStep.getCurrentPage(request);

        // This function returns the NAME of the button the user
        // just pressed in order to submit the form.
        // In this case, we are saying default to the "Next" button,
        // if it cannot be determined which button was pressed.
        // (requires you use the AbstractProcessingStep.PREVIOUS_BUTTON,
        // AbstractProcessingStep.NEXT_BUTTON, and AbstractProcessingStep.CANCEL_BUTTON
        // constants in your JSPs)
        String buttonPressed = UIUtil.getSubmitButton(request,
                AbstractProcessingStep.NEXT_BUTTON);

        // We also have some Button Name constants to work with.
        // Assuming you used these constants to NAME your submit buttons,
        // we can do different processing based on which button was pressed
        if (buttonPressed.equals(AbstractProcessingStep.NEXT_BUTTON))
        {
            // special processing for "Next" button
            // YOU DON'T NEED TO ATTEMPT TO REDIRECT/FORWARD TO THE NEXT PAGE
            // HERE,
            // the SubmissionController will do that automatically!
        }
        else if (buttonPressed.equals(AbstractProcessingStep.PREVIOUS_BUTTON))
        {
            // special processing for "Previous" button
            // YOU DON'T NEED TO ATTEMPT TO REDIRECT/FORWARD TO THE PREVIOUS
            // PAGE HERE,
            // the SubmissionController will do that automatically!
        }
        else if (buttonPressed.equals(AbstractProcessingStep.CANCEL_BUTTON))
        {
            // special processing for "Cancel/Save" button
            // YOU DON'T NEED TO ATTEMPT TO REDIRECT/FORWARD TO THE CANCEL/SAVE
            // PAGE HERE,
            // the SubmissionController will do that automatically!
        }

        // Here's some sample error message processing!
        if (status == SampleStep.STATUS_USER_INPUT_ERROR)
        {
            // special processing for this error message
        }

        /***********************************************************************
         * SAMPLE CODE (all of which is commented out)
         * 
         * (For additional sample code, see any of the existing JSPStep classes)
         **********************************************************************/

        /*
         * HOW-TO RELOAD PAGE BECAUSE OF INVALID INPUT!
         * 
         * If you have already validated the form inputs, and determined that
         * one or more is invalid, you can RELOAD the JSP by calling 
         * JSPStepManger.showJSP() like:
         * 
         * JSPStepManger.showJSP(request, response, subInfo, "/submit/sample-step.jsp");
         * 
         * You should make sure to pass a flag to your JSP to let it know which
         * fields were invalid, so that it can display an error message next to
         * them:
         * 
         * request.setAttribute("invalid-fields", listOfInvalidFields);
         */

        /*
         * HOW-TO GO TO THE NEXT "PAGE" IN THIS STEP
         * 
         * If this step has multiple "pages" that appear in the Progress Bar,
         * you can step to the next page AUTOMATICALLY by just NOT calling
         * "JSPStepManger.showJSP()" in your doPostProcessing() method.
         * 
         */

        /*
         * HOW-TO COMPLETE/END THIS STEP
         * 
         * In order to complete this step, just do NOT call JSPStepManger.showJSP()! Once all
         * pages are finished, the JSPStepManager class will report to the
         * SubmissionController that this step is now finished!
         */
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used by the SubmissionController to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        /*
         * This method tells the SubmissionController how many "pages" to put in
         * the Progress Bar for this Step.
         * 
         * Most steps should just return 1 (which means the Step only appears
         * once in the Progress Bar).
         * 
         * If this Step should be shown as multiple "Pages" in the Progress Bar,
         * then return a value higher than 1. For example, return 2 in order to
         * have this Step appear twice in a row within the Progress Bar.
         * 
         * If you return 0, this Step will not appear in the Progress Bar at
         * ALL! Therefore it is important for non-interactive steps to return 0.
         */

        // in most cases, you'll want to just return 1
        return 1;
    }
    
    /**
     * Return the URL path (e.g. /submit/review-metadata.jsp) of the JSP
     * which will review the information that was gathered in this Step.
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
        return REVIEW_JSP;
    }
    
}
