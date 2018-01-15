/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.submit.step;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.app.webui.servlet.SubmissionController;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.license.CCLicense;
import org.dspace.license.CCLookup;
import org.dspace.license.CreativeCommons;
import org.dspace.submit.step.LicenseStep;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

/**
 * License step for DSpace JSP-UI. Presents the user with license information
 * required for all items submitted into DSpace.
 * <P>
 * This JSPStep class works with the SubmissionController servlet
 * for the JSP-UI
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
 * @see org.dspace.submit.step.LicenseStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPCCLicenseStep extends JSPStep
{
    /** JSP which displays Creative Commons license information * */
    private static final String CC_LICENSE_JSP = "/submit/creative-commons.jsp";

    /** JSP which displays information after a license is rejected * */
    private static final String LICENSE_REJECT_JSP = "/submit/license-rejected.jsp";

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPCCLicenseStep.class);

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
        // Do we already have a CC license?
        Item item = subInfo.getSubmissionItem().getItem();
        boolean exists = CreativeCommons.hasLicense(context, item);
        request.setAttribute("cclicense.exists", Boolean.valueOf(exists));

        String ccLocale = ConfigurationManager.getProperty("cc.license.locale");
        /** Default locale to 'en' */
        ccLocale = (StringUtils.isNotBlank(ccLocale)) ? ccLocale : "en";
        request.setAttribute("cclicense.locale", ccLocale);
        
        CCLookup cclookup = new CCLookup();
        Collection<CCLicense> collectionLicenses = cclookup.getLicenses(ccLocale);
        request.setAttribute("cclicense.licenses", collectionLicenses);
        
        JSPStepManager.showJSP(request, response, subInfo, CC_LICENSE_JSP);

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
        String buttonPressed = Util.getSubmitButton(request, LicenseStep.CANCEL_BUTTON);

        //  JSP-UI Specific (only JSP UI has a "reject" button): 
        //  License was explicitly rejected
        if (buttonPressed.equals("submit_reject"))
        {
            // User has rejected license.
            log.info(LogManager.getHeader(context, "reject_license",
                    subInfo.getSubmissionLogInfo()));

            // If the license page was the 1st page in the Submission process,
            // then delete the submission if they reject the license!
            if (!subInfo.isInWorkflow()
                    && (SubmissionController.getStepReached(subInfo) <= SubmissionController.FIRST_STEP))
            {
                WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();
                wi.deleteAll();

                // commit changes
                context.commit();
            }

            // Show the license rejected page
            JSPManager.showJSP(request, response, LICENSE_REJECT_JSP);
        }

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
        return NO_JSP; //signing off on license does not require reviewing
    }
}
