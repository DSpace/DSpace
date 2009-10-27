/*
 * JSPSelectCollectionStep.java
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.submit.step.SelectCollectionStep;

/**
 * Step which controls selecting a Collection for the Item Submission process
 * for DSpace JSP-UI
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
 * doProcessing() method is called an the step completes immediately</li>
 * <li>Call doProcessing() method on appropriate AbstractProcessingStep after the user returns from the JSP, in order
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
 * @see org.dspace.submit.step.SelectCollectionStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class JSPSelectCollectionStep extends JSPStep
{
    /** JSP which displays HTML for this Class * */
    private static final String SELECT_COLLECTION_JSP = "/submit/select-collection.jsp";

    /** log4j logger */
    private static Logger log = Logger.getLogger(JSPSelectCollectionStep.class);

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
         * Possible parameters from JSP:
         * 
         * collection= <collection_id> - a collection that has already been
         * selected!
         * 
         * With no parameters, this servlet prepares for display of the Select
         * Collection JSP.
         */
        String collectionID = request.getParameter("collection");
        Collection col = null;

        if (collectionID != null)
        {
            col = Collection.find(context, Integer.parseInt(collectionID));
        }

        // if we already have a valid collection, then we can forward directly
        // to post-processing
        if (col != null)
        {
            log
                    .debug("Select Collection page skipped, since a Collection ID was already found.  Collection ID="
                            + collectionID);
        }
        else
        {
            // gather info for JSP page
            Community com = UIUtil.getCommunityLocation(request);

            Collection[] collections;

            if (com != null)
            {
                // In a community. Show collections in that community only.
                collections = Collection.findAuthorized(context, com,
                        Constants.ADD);
            }
            else
            {
                // Show all collections
                collections = Collection.findAuthorized(context, null,
                        Constants.ADD);
            }

            // This is a special case, where the user came back to this
            // page after not selecting a collection. This will display
            // the "Please select a collection" message to the user
            if (collectionID != null && Integer.parseInt(collectionID) == -1)
            {
                // specify "no collection" error message should be displayed
                request.setAttribute("no.collection", new Boolean(true));
            }

            // save collections to request for JSP
            request.setAttribute("collections", collections);

            // we need to load the select collection JSP
            JSPStepManager.showJSP(request, response, subInfo, SELECT_COLLECTION_JSP);
        }
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
        // if the user didn't select a collection,
        // send him/her back to "select a collection" page
        if (status == SelectCollectionStep.STATUS_NO_COLLECTION)
        {
            // specify "no collection" error message should be displayed
            request.setAttribute("no.collection", new Boolean(true));

            // reload this page, by re-calling doPreProcessing()
            doPreProcessing(context, request, response, subInfo);
        }
        else if (status == SelectCollectionStep.STATUS_INVALID_COLLECTION)
        {
            JSPManager.showInvalidIDError(request, response, request
                    .getParameter("collection"), Constants.COLLECTION);
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
        return NO_JSP;  //at this time, you cannot review what collection you selected.
    }
}
