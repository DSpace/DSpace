/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.submit.AbstractProcessingStep;

/**
 * License step for DSpace Submission Process. Processes the
 * user response to the license.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class LicenseStep extends AbstractProcessingStep
{
    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // user rejected the license
    public static final int STATUS_LICENSE_REJECTED = 1;

    /** log4j logger */
    private static Logger log = Logger.getLogger(LicenseStep.class);


    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = Util.getSubmitButton(request, CANCEL_BUTTON);

        boolean licenseGranted = false;

        // For Manakin:
        // Accepting the license means checking a box and clicking Next
        String decision = request.getParameter("decision");
        if (decision != null && decision.equalsIgnoreCase("accept")
                && buttonPressed.equals(NEXT_BUTTON))
        {
            licenseGranted = true;
        }
        // For JSP-UI: User just needed to click "I Accept" button
        else if (buttonPressed.equals("submit_grant"))
        {
            licenseGranted = true;
        }// JSP-UI: License was explicitly rejected
        else if (buttonPressed.equals("submit_reject"))
        {
            licenseGranted = false;
        }// Manakin UI: user didn't make a decision and clicked Next->
        else if (buttonPressed.equals(NEXT_BUTTON))
        {
            // no decision made (this will cause Manakin to display an error)
            return STATUS_LICENSE_REJECTED;
        }

        if (licenseGranted
                && (buttonPressed.equals("submit_grant") || buttonPressed
                        .equals(NEXT_BUTTON)))
        {
            // License granted
            log.info(LogManager.getHeader(context, "accept_license",
                    subInfo.getSubmissionLogInfo()));

            // Add the license to the item
            Item item = subInfo.getSubmissionItem().getItem();
            EPerson submitter = context.getCurrentUser();

            // remove any existing DSpace license (just in case the user
            // accepted it previously)
            itemService.removeDSpaceLicense(context, item);

            String license = LicenseUtils.getLicenseText(context
                    .getCurrentLocale(), subInfo.getSubmissionItem()
                    .getCollection(), item, submitter);

            LicenseUtils.grantLicense(context, item, license);

            // commit changes
            context.dispatchEvents();
        }

        // completed without errors
        return STATUS_COMPLETE;
    }


    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using submission-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        return 1;

    }

}
