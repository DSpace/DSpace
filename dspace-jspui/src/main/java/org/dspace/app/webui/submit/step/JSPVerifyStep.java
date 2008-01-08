/*
 * JSPVerifyStep.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.webui.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.webui.servlet.SubmissionController;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.submit.step.VerifyStep;

/**
 * Verify step for DSpace. Presents the user with a verification screen for all
 * information entered about the item being submitted.
 * <P>
 * This JSPStepManager class works with the SubmissionController servlet
 * for the JSP-UI
 * <P>
 * The following methods are called in this order:
 * <ul>
 * <li>Call doPreProcessing() method</li>
 * <li>If showJSP() was specified from doPreProcessing(), then the JSP
 * specified will be displayed</li>
 * <li>If showJSP() was not specified from doPreProcessing(), then the
 * doProcessing() method is called an the step completes immediately</li>
 * <li>Call doProcessing() method after the user returns from the JSP, in order
 * to process the user input</li>
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
 * @see org.dspace.submit.step.VerifyStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPVerifyStep extends JSPStep
{
    /** JSP which displays initial questions * */
    public static final String VERIFY_JSP = "/submit/review.jsp";

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPVerifyStep.class);

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
        // load the current submission process config
        // to get the list of steps the user went through
        SubmissionConfig subProcessConfig = subInfo.getSubmissionConfig();

        // create a HashMap of step data to review for the Verify JSP
        // This HashMap will be the following format:
        // key = stepNumber.pageNumber
        // value = path to review JSP for this Step (which will load the users
        // answers)
        LinkedHashMap reviewData = new LinkedHashMap();

        // this shouldn't happen...but just in case!
        if (subInfo.getProgressBarInfo() == null)
        {
            // progress bar information is lost!
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
            return;
        }

        // loop through the steps in our Progress Bar
        // (since these are steps the user visited)
        Iterator stepIterator = subInfo.getProgressBarInfo().keySet()
                .iterator();
        while (stepIterator.hasNext())
        {
            // remember, the keys of the progressBar hashmap is in the
            // format: stepNumber.pageNumber
            String stepAndPage = (String) stepIterator.next();

            // extract out the step number (which is before the period)
            String[] fields = stepAndPage.split("\\."); // split on period
            int stepNum = Integer.parseInt(fields[0]);

            // only include this step if it is BEFORE the current "verify" step
            // (We cannot review steps that we haven't gotten to!!)
            if (stepNum < SubmissionController.getCurrentStepConfig(request,
                    subInfo).getStepNumber())
            {
                // load this step's information
                SubmissionStepConfig s = subProcessConfig.getStep(stepNum);

                try
                {
                    JSPStepManager stepManager = JSPStepManager.loadStep(s);
                
                    // get this step's review JSP
                    String reviewJSP = stepManager.getReviewJSP(context, request, response, subInfo);

                    if ((reviewJSP != null) && (reviewJSP.length() > 0))
                    {
                        // save the path to this steps JSP to our reviewData Hashmap
                        // (with the key = stepNum.pageNum)
                        reviewData.put(stepAndPage, reviewJSP);
                    }
                }
                catch(Exception e)
                {
                    log.error("Problem loading Review JSP for step #" + s.getStepNumber() + ".  ", e);
                    JSPManager.showIntegrityError(request, response);
                    return;
                }
            }
        }

        if (reviewData.isEmpty())
        {
            // need review data for this page to work!
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
            return;
        }

        // save our data to review to request for the Verify JSP
        request.setAttribute("submission.review", reviewData);

        // forward to verify JSP
        JSPStepManager.showJSP(request, response, subInfo, VERIFY_JSP);
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
     * @param status
     *            any status/errors reported by doProcessing() method
     */
    public void doPostProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int status)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // nothing to do from the Verify Step.
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
        return NO_JSP; //no review JSP, since this is the verification step
    }
}
