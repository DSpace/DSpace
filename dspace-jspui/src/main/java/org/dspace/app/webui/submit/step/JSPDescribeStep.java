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
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.submit.step.DescribeStep;

/**
 * Describe step for DSpace JSP-UI submission process. Handles the pages that gather
 * descriptive information (i.e. metadata) for an item being submitted into
 * DSpace.
 * <P>
 * This JSPStep class works with the SubmissionController servlet
 * for the JSP-UI.
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
 * @see org.dspace.submit.step.DescribeStep
 *
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPDescribeStep extends JSPStep
{
    /** JSP which displays HTML for this Class * */
    private static final String DISPLAY_JSP = "/submit/edit-metadata.jsp";
    
    /** JSP which reviews information gathered by DISPLAY_JSP * */
    private static final String REVIEW_JSP = "/submit/review-metadata.jsp";

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPDescribeStep.class);

    /** Constructor */
    public JSPDescribeStep() throws ServletException
    {
        //just call DescribeStep's constructor
        super();
    }

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
        // display edit metadata page
        showEditMetadata(context, request, response, subInfo);
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
        // check what submit button was pressed in User Interface
        String buttonPressed = UIUtil.getSubmitButton(request, DescribeStep.NEXT_BUTTON);

        // this shouldn't happen...but just in case!
        if (subInfo.getSubmissionItem() == null)
        {
            // In progress submission is lost
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
            return;
        }

        // if user added an extra input field, stay on the same page
        if (status == DescribeStep.STATUS_MORE_INPUT_REQUESTED)
        {
            // reload this same JSP to display extra input boxes
            showEditMetadata(context, request, response, subInfo);
        }
        // if one of the "Remove This Entry" buttons was pressed
        else if (buttonPressed.indexOf("remove") > -1)
        {
            // reload this same JSP to display removed entries
            showEditMetadata(context, request, response, subInfo);
        }
        else if (status == DescribeStep.STATUS_MISSING_REQUIRED_FIELDS)
        {
            List<String> missingFields = DescribeStep.getErrorFields(request);

            // return to current edit metadata screen if any fields missing
            if (missingFields.size() > 0)
            {
                subInfo.setJumpToField(missingFields.get(0));
                subInfo.setMissingFields(missingFields);

                // reload this same JSP to display missing fields info
                showEditMetadata(context, request, response, subInfo);
            }
        }
    }

    /**
     * Show the page which displays all the Initial Questions to the user
     *
     * @param context
     *            current DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     *
     */
    private void showEditMetadata(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws SQLException, ServletException, IOException
    {
        Locale sessionLocale = null;
        sessionLocale = UIUtil.getSessionLocale(request);
        String formFileName = I18nUtil.getInputFormsFileName(sessionLocale);
        
        // determine collection
        Collection c = subInfo.getSubmissionItem().getCollection();

        // requires configurable form info per collection
        try
        {
            request.setAttribute("submission.inputs", DescribeStep.getInputsReader(formFileName).getInputs(c
                    .getHandle()));
        }
        catch (DCInputsReaderException e)
        {
            throw new ServletException(e);
        }


        // forward to edit-metadata JSP
        JSPStepManager.showJSP(request, response, subInfo, DISPLAY_JSP);
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
