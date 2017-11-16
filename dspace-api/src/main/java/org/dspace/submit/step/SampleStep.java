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

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

/**
 * This is a Sample Step class which can be used as template for creating new
 * custom Step processing classes!
 * <p>
 * Please Note: The basic methods you will want to override are described below.
 * However, obviously, you are completely free to create your own methods for
 * this Step, or override other methods. For more examples, look at the code
 * from one of the provided DSpace step classes in the "org.dspace.submit.step"
 * package.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * If you are utilizing the JSP-UI, you will also be required to create
 * a class which implements org.dspace.app.webui.submit.JSPStep, and provide
 * the necessary JSP-related methods.  There is a corresponding sample
 * of such a class at org.dspace.app.webui.submit.step.JSPSampleStep.
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class SampleStep extends AbstractProcessingStep
{

    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    public static final int STATUS_USER_INPUT_ERROR = 1;

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
        /*
         * In this method, you should do any processing of any user input (if
         * this step requires user input). If this step does not require user
         * interaction (i.e. it has no UI), then ALL of the backend processing
         * should occur in this method.
         * 
         * Processing may include, but is not limited to:
         * 
         * 1) Saving user input data to the database (e.g. saving metadata from
         * a web form that a user filled out) 2) Performing ALL backend
         * processing for non-interactive steps 3) Determine if any errors
         * occurred during processing, and if so, return those error flags.
         * 
         * For steps with user interaction, this method is called right after
         * the web form or page is submitted. For steps without user
         * interaction, this method is called whenever the step itself is
         * supposed to be processed.
         * 
         */

        /*
         * HINT:
         * 
         * If any errors occurred, its recommended to create a global "flag" to
         * represent that error. It's much easier then for the
         * JSP-UI or Manakin XML-UI to determine what to do with that error.
         * 
         * For example, if an error occurred, you may specify the following
         * return call:
         * 
         * return USER_INPUT_ERROR_FLAG;
         * 
         * (Note: this flag is defined at the top of this class)
         */

        // If no errors occurred, and there were no other special messages to
        // report to the doPostProcessing() method, just return STATUS_COMPLETE!
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
        /*
         * This method reports how many "pages" to put in
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
}
