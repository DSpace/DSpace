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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.step.AccessStep;

/**
 * 
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class JSPAccessStep extends JSPStep
{
    /** JSP which displays the step to the user * */
    private static final String DISPLAY_JSP = "/submit/access-step.jsp";

    /** JSP which edits the selected resource policy to the user * */
    private static final String EDIT_POLICY_JSP = "/submit/edit-policy.jsp";

    private static final String REVIEW_JSP = "/submit/review-policy.jsp";
    
    private boolean advanced = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPAccessStep.class);
    
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    
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
        // Tell JSPStepManager class to load "access-step.jsp"
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
        String buttonPressed = UIUtil.getSubmitButton(request,
                AbstractProcessingStep.NEXT_BUTTON);

        if (status == AccessStep.STATUS_EDIT_POLICY)
        {
            JSPStepManager.showJSP(request, response, subInfo, EDIT_POLICY_JSP);
        }
        else if (buttonPressed.equals(AccessStep.FORM_ACCESS_BUTTON_ADD)
           || buttonPressed.startsWith("submit_delete_edit_policies_")
           || buttonPressed.equals(AccessStep.FORM_EDIT_BUTTON_CANCEL)
           || buttonPressed.equals(AccessStep.FORM_EDIT_BUTTON_SAVE)
           || status > 0)
        {
            // Here's some sample error message processing!
            if (status > 0 && status != AccessStep.STATUS_EDIT_POLICY)
            {
                request.setAttribute("error_id", Integer.valueOf(status));
            }

            // special processing for this error message
            JSPStepManager.showJSP(request, response, subInfo, DISPLAY_JSP);
        }
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
        
        // Policies List
        List<ResourcePolicy> rpolicies = new ArrayList<ResourcePolicy>();
        try
        {
            rpolicies = authorizeService.findPoliciesByDSOAndType(context, subInfo.getSubmissionItem().getItem(), ResourcePolicy.TYPE_CUSTOM);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }

        Item item = subInfo.getSubmissionItem().getItem();        
        request.setAttribute("submission.item.isdiscoverable", item.isDiscoverable());
        request.setAttribute("submission.item.rpolicies", rpolicies);
        request.setAttribute("advancedEmbargo", advanced);
        return REVIEW_JSP;
    }

}
