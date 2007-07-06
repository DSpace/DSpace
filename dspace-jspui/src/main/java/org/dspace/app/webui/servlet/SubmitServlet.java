/*
 * SubmitServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.webui.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.SubmissionInfo;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.DCSeriesNumber;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.license.CreativeCommons;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Submission servlet for DSpace. Handles the initial submission of items, as
 * well as the editing of items further down the line.
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
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubmitServlet extends DSpaceServlet
{
    // Steps in the submission process

    /** Selection collection step */
    public static final int SELECT_COLLECTION = 0;

    /** Ask initial questions about the submission step */
    public static final int INITIAL_QUESTIONS = 1;

    /** Edit DC metadata first page step */
    public static final int EDIT_METADATA_1 = 2;

    /** Edit DC metadata last page step 
     * Current value allows up to 6 distinct pages.
     * If more are needed, renumber this value and those 
     * that follow it (up to SUBMISSION_COMPLETE), but
     * note that the progress bar will stretch badly beyond 6.
     */
    public static final int EDIT_METADATA_2 = 7;

    /**
     * Upload files step. Note this doesn't correspond to an actual page, since
     * the upload file step consists of a number of pages with no definite
     * order. This is just used for the progress bar.
     */
    public static final int UPLOAD_FILES = 8;

    /** Review submission step */
    public static final int REVIEW_SUBMISSION = 9;

    /** optional CC license step */
    public static final int CC_LICENSE = 10;

    /** Grant (deposit) license step */
    public static final int GRANT_LICENSE = 11;

    /** Submission completed step */
    public static final int SUBMISSION_COMPLETE = 12;
    
    // Steps which aren't part of the main sequence, but rather
    // short "diversions" are given high step numbers. The main sequence
    // is defined as being steps 0 to SUBMISSION_COMPLETE.

    /** Cancellation of a submission */
    public static final int SUBMISSION_CANCELLED = 101;

    /** List of uploaded files */
    public static final int FILE_LIST = 102;

    /** Choose file page */
    public static final int CHOOSE_FILE = 103;

    /** File format page */
    public static final int GET_FILE_FORMAT = 104;

    /** Error uploading file */
    public static final int UPLOAD_ERROR = 105;

    /** Change file description page */
    public static final int CHANGE_FILE_DESCRIPTION = 106;

    /**
     * Verify pruning of extra files, titles, dates as a result of changing an
     * answer to one of the initial questions
     */
    public static final int VERIFY_PRUNE = 107;

    /** log4j logger */
    private static Logger log = Logger.getLogger(SubmitServlet.class);
    
    /** hash of all submission forms details */
    private DCInputsReader inputsReader = null;
    
    private Locale langForm = null;
    
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
         * With no parameters, A GET starts a new submission. What happens
         * depends on the context of the user (where they are.) If they're not
         * in a community or collection, they can choose any collection from the
         * list of all collections. If they're in a community, the list of
         * collections they can choose from will be limited to those within the
         * current community. If the user has selected a collection, a new
         * submission will be started in that collection.
         */
        String workspaceID = request.getParameter("resume");

        if (workspaceID != null)
        {
            try
            {
                WorkspaceItem wi = WorkspaceItem.find(context, Integer
                        .parseInt(workspaceID));

                SubmissionInfo si = new SubmissionInfo();
                si.submission = wi;
                doStep(context, request, response, si, INITIAL_QUESTIONS);
            }
            catch (NumberFormatException nfe)
            {
                log.warn(LogManager.getHeader(context, "bad_workspace_id",
                        "bad_id=" + workspaceID));
                JSPManager.showInvalidIDError(request, response, workspaceID,
                        -1);
            }

            return;
        }

        String workflowID = request.getParameter("workflow");

        if (workflowID != null)
        {
            try
            {
                WorkflowItem wi = WorkflowItem.find(context, Integer
                        .parseInt(workflowID));

                SubmissionInfo si = new SubmissionInfo();
                si.submission = wi;
                doStep(context, request, response, si, INITIAL_QUESTIONS);
            }
            catch (NumberFormatException nfe)
            {
                log.warn(LogManager.getHeader(context, "bad_workflow_id",
                        "bad_id=" + workflowID));
                JSPManager
                        .showInvalidIDError(request, response, workflowID, -1);
            }

            return;
        }

        Community com = UIUtil.getCommunityLocation(request);
        Collection col = UIUtil.getCollectionLocation(request);

        if (col != null)
        {
            // In a collection, skip the "choose selection" stage
            // Create a workspace item
            WorkspaceItem wi = WorkspaceItem.create(context, col, true);

            // Proceed to first step
            SubmissionInfo si = new SubmissionInfo();
            si.submission = wi;
            doStep(context, request, response, si, INITIAL_QUESTIONS);
            context.complete();
        }
        else
        {
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

            log.info(LogManager.getHeader(context, "select_collection", ""));

            request.setAttribute("collections", collections);
            JSPManager.showJSP(request, response,
                    "/submit/select-collection.jsp");
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        Locale locale = context.getCurrentLocale();
     	
       	if (inputsReader == null || !langForm.equals(locale))
    	{
    		log.info("Dateiname="+I18nUtil.getInputFormsFileName(locale));
    		
    	    // read configurable submissions forms data
    		inputsReader = new DCInputsReader(I18nUtil.getInputFormsFileName(locale));
    		langForm = locale;
    	}
       	
        // First of all, we need to work out if this is a multipart request
        // The file upload page uses those
        String contentType = request.getContentType();

        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            // This is a multipart request, so it's a file upload
            processChooseFile(context, request, response);

            return;
        }

        // First get the step
        int step = UIUtil.getIntParameter(request, "step");

        // select collection is a special case - no submissioninfo object
        // If no step was given, we also assume "select collection"
        // (Submit button on collection home page or elsewhere)
        if ((step == SELECT_COLLECTION) || (step == -1))
        {
            processSelectCollection(context, request, response);

            return;
        }

        // Get submission info
        SubmissionInfo subInfo = getSubmissionInfo(context, request);

        if (subInfo == null)
        {
            /*
             * Work around for problem where people select "is a thesis", see
             * the error page, and then use their "back" button thinking they
             * can start another submission - it's been removed so the ID in the
             * form is invalid. If we detect the "removed_thesis" attribute we
             * display a friendly message instead of an integrity error.
             */
            if (request.getSession().getAttribute("removed_thesis") != null)
            {
                request.getSession().removeAttribute("removed_thesis");
                JSPManager.showJSP(request, response,
                        "/submit/thesis-removed-workaround.jsp");

                return;
            }
            else
            {
                /*
                 * If the submission info was invalid, set the step to an
                 * invalid number so an integrity error will be shown
                 */
                step = -1;
            }
        }
        
        if (step >= EDIT_METADATA_1 && step <= EDIT_METADATA_2)
        {
        	processEditMetadata(context, request, response, subInfo, step);
        	return;
        }

        switch (step)
        {
        case INITIAL_QUESTIONS:
            processInitialQuestions(context, request, response, subInfo);

            break;
  
        case GET_FILE_FORMAT:
            processGetFileFormat(context, request, response, subInfo);

            break;

        case FILE_LIST:
            processFileList(context, request, response, subInfo);

            break;

        case UPLOAD_ERROR:
            processUploadError(context, request, response, subInfo);

            break;

        case CHANGE_FILE_DESCRIPTION:
            processChangeFileDescription(context, request, response, subInfo);

            break;

        case REVIEW_SUBMISSION:
            processReview(context, request, response, subInfo);

            break;

        case CC_LICENSE:
            processCC(context, request, response, subInfo);

            break;

        case GRANT_LICENSE:
            processLicense(context, request, response, subInfo);

            break;

        case SUBMISSION_CANCELLED:
            processCancellation(context, request, response, subInfo);

            break;

        case VERIFY_PRUNE:
            processVerifyPrune(context, request, response, subInfo);

            break;

        default:
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    //****************************************************************
    //****************************************************************
    //             METHODS FOR PROCESSING POSTED FORMS
    //****************************************************************
    //****************************************************************

    /**
     * Process the selection collection stage, or the clicking of a "submit to
     * this collection" button on a collection home page.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processSelectCollection(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // The user might have clicked cancel. We don't do a
        // standard cancellation at this stage, since we don't
        // actually have an item to keep or remove yet.
        if (request.getParameter("submit_cancel") != null)
        {
            // Just send them to their "My DSpace" for now.
            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/mydspace"));

            return;
        }

        // First we find the collection
        int id = UIUtil.getIntParameter(request, "collection");
        Collection col = Collection.find(context, id);

        // Show an error if we don't have a collection
        if (col == null)
        {
            JSPManager.showInvalidIDError(request, response, request
                    .getParameter("collection"), Constants.COLLECTION);
        }
        else
        {
            WorkspaceItem wi = WorkspaceItem.create(context, col, true);

            // Proceed to first step
            SubmissionInfo si = new SubmissionInfo();
            si.submission = wi;
            doStep(context, request, response, si, INITIAL_QUESTIONS);

            context.complete();
        }
    }

    /**
     * process input from initial-questions.jsp
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
    private void processInitialQuestions(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_next");

        // Firstly, check for a click of the cancel button.
        if (buttonPressed.equals("submit_cancel"))
        {
            doCancellation(request, response, subInfo, INITIAL_QUESTIONS,
                    INITIAL_QUESTIONS);

            return;
        }

        // Get the values from the form
        boolean multipleTitles = UIUtil.getBoolParameter(request,
                "multiple_titles");
        boolean publishedBefore = UIUtil.getBoolParameter(request,
                "published_before");
        boolean multipleFiles = UIUtil.getBoolParameter(request,
                "multiple_files");
        boolean isThesis = ConfigurationManager
                .getBooleanProperty("webui.submit.blocktheses")
                && UIUtil.getBoolParameter(request, "is_thesis");

        if (isWorkflow(subInfo))
        {
            // Thesis question does not appear in workflow mode..
            isThesis = false;

            // Pretend "multiple files" is true in workflow mode
            // (There will always be the license file)
            multipleFiles = true;
        }

        // First and foremost - if it's a thesis, reject the submission
        if (isThesis)
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.submission;
            wi.deleteAll();

            // Remember that we've removed a thesis in the session
            request.getSession().setAttribute("removed_thesis",
                    new Boolean(true));

            // Display appropriate message
            JSPManager.showJSP(request, response, "/submit/no-theses.jsp");

            context.complete();

            return;
        }

        // Now check to see if the changes will remove any values
        // (i.e. multiple files, titles or an issue date.)
        boolean willRemoveTitles = false;
        boolean willRemoveDate = false;
        boolean willRemoveFiles = false;

        if (multipleTitles == false)
        {
            DCValue[] altTitles = subInfo.submission.getItem().getDC(
                    "title", "alternative", Item.ANY);

            willRemoveTitles = altTitles.length > 0;
        }

        if (publishedBefore == false)
        {
            DCValue[] dateIssued = subInfo.submission.getItem().getDC("date",
                    "issued", Item.ANY);
            DCValue[] citation = subInfo.submission.getItem().getDC(
                    "identifier", "citation", Item.ANY);
            DCValue[] publisher = subInfo.submission.getItem().getDC(
                    "publisher", null, Item.ANY);

            willRemoveDate = (dateIssued.length > 0) || (citation.length > 0)
                    || (publisher.length > 0);
        }

        if (multipleFiles == false)
        {
            // see if number of bitstreams in "ORIGINAL" bundle > 1
            // FIXME: Assumes multiple bundles, clean up someday...
            Bundle[] bundles = subInfo.submission.getItem().getBundles(
                    "ORIGINAL");

            if (bundles.length > 0)
            {
                Bitstream[] bitstreams = bundles[0].getBitstreams();

                willRemoveFiles = bitstreams.length > 1;
            }
        }

        // If anything is going to be removed from the item as a result
        // of changing the answer to one of the questions, we need
        // to inform the user and make sure that's OK
        if (willRemoveTitles || willRemoveDate || willRemoveFiles)
        {
            // Verify pruning of extra bits
            request.setAttribute("submission.info", subInfo);
            request
                    .setAttribute("multiple.titles",
                            new Boolean(multipleTitles));
            request.setAttribute("published.before", new Boolean(
                    publishedBefore));
            request.setAttribute("multiple.files", new Boolean(multipleFiles));
            request.setAttribute("will.remove.titles", new Boolean(
                    willRemoveTitles));
            request.setAttribute("will.remove.date",
                    new Boolean(willRemoveDate));
            request.setAttribute("will.remove.files", new Boolean(
                    willRemoveFiles));
            request.setAttribute("button.pressed", UIUtil.getSubmitButton(
                    request, "submit_next"));

            JSPManager.showJSP(request, response, "/submit/verify-prune.jsp");
        }
        else
        {
            // Nothing needs removing, so just make the changes
            subInfo.submission.setMultipleTitles(multipleTitles);
            subInfo.submission.setPublishedBefore(publishedBefore);

            // "Multiple files" irrelevant in workflow mode
            if (!isWorkflow(subInfo))
            {
                subInfo.submission.setMultipleFiles(multipleFiles);
            }

            subInfo.submission.update();

            // On to the next stage
            if (buttonPressed.equals("submit_next"))
            {
                // Update user's progress
                userHasReached(subInfo, EDIT_METADATA_1);

                // User has clicked "Next"
                doStep(context, request, response, subInfo, EDIT_METADATA_1);
            }
            else
            {
                // Progress bar button clicked
                doStepJump(context, request, response, subInfo);
            }

            context.complete();
        }
    }

    /**
     * Process input from "verify prune" step
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
    private void processVerifyPrune(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        if (request.getParameter("do_not_proceed") != null)
        {
            // User cancelled
            doStep(context, request, response, subInfo, INITIAL_QUESTIONS);

            return;
        }

        // User elected to proceed - do the pruning
        // Get the values from the form
        boolean multipleTitles = UIUtil.getBoolParameter(request,
                "multiple_titles");
        boolean publishedBefore = UIUtil.getBoolParameter(request,
                "published_before");

        // Multiple files question does not appear in workflow mode.
        // Since the submission will have a license, the answer to
        // this question will always be "yes"
        boolean multipleFiles = (isWorkflow(subInfo) || UIUtil
                .getBoolParameter(request, "multiple_files"));

        Item item = subInfo.submission.getItem();

        if (!multipleTitles)
        {
            item.clearDC("title", "alternative", Item.ANY);
        }

        if (publishedBefore == false)
        {
            item.clearDC("date", "issued", Item.ANY);
            item.clearDC("identifier", "citation", Item.ANY);
            item.clearDC("publisher", null, Item.ANY);
        }

        if (multipleFiles == false)
        {
            // remove all but first bitstream from bundle[0]
            // FIXME: Assumes multiple bundles, clean up someday...
            // (only messes with the first bundle.)
            Bundle[] bundles = item.getBundles("ORIGINAL");

            if (bundles.length > 0)
            {
                Bitstream[] bitstreams = bundles[0].getBitstreams();

                // Remove all but the first bitstream
                for (int i = 1; i < bitstreams.length; i++)
                {
                    bundles[0].removeBitstream(bitstreams[i]);
                }
            }
        }

        // Nothing needs removing, so just make the changes
        subInfo.submission.setMultipleTitles(multipleTitles);
        subInfo.submission.setPublishedBefore(publishedBefore);

        // "Multiple files" irrelevant in workflow mode
        if (!isWorkflow(subInfo))
        {
            subInfo.submission.setMultipleFiles(multipleFiles);
        }

        subInfo.submission.update();

        // Everything went OK if we get to here, so now response
        // to the original button press
        if (request.getParameter("submit_next") != null)
        {
            // Update user's progress
            userHasReached(subInfo, EDIT_METADATA_1);

            // User has clicked "Next"
            doStep(context, request, response, subInfo, EDIT_METADATA_1);

            context.complete();
        }
        else
        {
            // Progress bar button clicked
            doStepJump(context, request, response, subInfo);

            context.complete();
        }
    }

    /**
     * process input from edit-metadata.jsp
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param curStep
     *            page number in edit sequence, starting at zero
     */
    private void processEditMetadata(Context context,
        HttpServletRequest request,
        HttpServletResponse response,
        SubmissionInfo subInfo,
		int curStep)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_next");
		String defaultLanguage =
			ConfigurationManager.getProperty("default.language");

        // Firstly, check for a click of the cancel button.
        if (buttonPressed.equals("submit_cancel"))
        {
            doCancellation(request, response, subInfo, curStep, curStep);
            return;
        }

        Item item = subInfo.submission.getItem();
        
        // lookup applicable inputs
        Collection c = subInfo.submission.getCollection();
        DCInput[] inputs = inputsReader.getInputs(c.getHandle()).getPageRows(curStep - EDIT_METADATA_1,
									 subInfo.submission.hasMultipleTitles(),
									 subInfo.submission.isPublishedBefore());
 
        // clear out all item metadata defined for this collection and page
        for (int i = 0; i < inputs.length; i++)
        {
        	String dcQualifier = inputs[i].getQualifier();
        	if (dcQualifier == null && inputs[i].getInputType().equals("qualdrop_value"))
        	{
        		dcQualifier = Item.ANY;
        	}
            item.clearMetadata(inputs[i].getSchema(), inputs[i].getElement(), dcQualifier, Item.ANY);
        }

        // now update the item metadata.
      	String fieldName;
        boolean moreInput = false;
    	for (int j = 0; j < inputs.length; j++)
    	{
    	   String dcElement = inputs[j].getElement();
           String dcQualifier = inputs[j].getQualifier();
           String dcSchema = inputs[j].getSchema();
    	   if (dcQualifier != null && ! dcQualifier.equals(Item.ANY))
    	   {
    	   		fieldName = dcSchema + "_" + dcElement + '_' + dcQualifier;
    	   }
    	   else
    	   {
    	   		fieldName = dcSchema + "_" + dcElement;
    	   }

    	   String inputType = inputs[j].getInputType();
    	   if (inputType.equals("name"))
    	   {
    	   		readNames(request, item, dcSchema, dcElement, dcQualifier, 
    	   				  inputs[j].getRepeatable());
    	   }
    	   else if (inputType.equals("date"))
    	   {
    	   		readDate(request, item, dcSchema, dcElement, dcQualifier);
    	   }
    	   else if (inputType.equals("series"))
    	   {
    	   		readSeriesNumbers(request, item, dcSchema, dcElement, dcQualifier, 
    	   						  inputs[j].getRepeatable());
    	   }
    	   else if (inputType.equals("qualdrop_value"))
    	   {
    	      List quals = getRepeatedParameter(request, dcSchema + "_" + dcElement + "_qualifier");
    	      List vals = getRepeatedParameter(request, dcSchema + "_" + dcElement + "_value");
    	      for (int z = 0; z < vals.size(); z++)
    	      {
    	      		String thisQual = (String)quals.get(z);
    	      		if ( "".equals(thisQual) )
    	      		{
    	      		    thisQual = null;
    	      		}
    	      		String thisVal = (String)vals.get(z);
    	      		if (! buttonPressed.equals("submit_" + dcSchema + "_" + dcElement + "_remove_" + z) &&
    	      			! thisVal.equals(""))
    	      		{
    	      			item.addMetadata(dcSchema, dcElement, thisQual, null, thisVal);
    	      		}
    	      }
    	   }
    	   else if (inputType.equals("dropdown"))
    	   {
    	      String[] vals = request.getParameterValues(fieldName);
    	      if (vals != null)
    	      {
    	      	for (int z = 0; z < vals.length; z++)
    	      	{
                    if (!vals[z].equals(""))
                    {
                        item.addMetadata(dcSchema, dcElement, dcQualifier,
								defaultLanguage, vals[z]);
                    }
    	      	}
    	      }
    	   }
    	   else if ((inputType.equals("onebox")) || (inputType.equals("twobox")) || 
    	   			(inputType.equals("textarea")))
    	   {
    	   		readText(request, item, dcSchema, dcElement, dcQualifier, 
    	   				 inputs[j].getRepeatable(), defaultLanguage);
    	   }
    	   else 
    	   {
    	       throw new ServletException("Field "+ fieldName + 
    	       		                      " has an unknown input type: " + inputType);
    	   }

           // Proceed according to button pressed
    	   if (! moreInput && buttonPressed.equals("submit_" + fieldName + "_more"))
    	   {
    	      subInfo.moreBoxesFor = fieldName;
    	      subInfo.jumpToField = fieldName;
    	      moreInput = true;
    	   }
    	}
    	
        int nextStep = -1;
        if ( moreInput )
        {
        	nextStep = curStep;
        }
    	
        if (buttonPressed.equals("submit_prev"))
        {
            // NB: code here assumes steps are sequentially numbered!
            nextStep = curStep-1;
        }
        else if (buttonPressed.equals("submit_next"))
        {
        	// scan for missing fields
        	String gotoField = null;
        	subInfo.missingFields = new ArrayList();
        	//subInfo.missingRowNums = new Vector();
        	for (int i = 0; i < inputs.length; i++)
        	{
        	    String element = inputs[i].getElement();
        	    String qual = inputs[i].getQualifier();
                String schema = inputs[i].getSchema();
                log.info("  inner "+schema);
        	    DCValue[] valArray = item.getMetadata(schema, element, qual, Item.ANY);
        	    boolean isEmpty = (valArray.length == 0);
        	    if (inputs[i].isRequired() && isEmpty)
        	    {
        	    	subInfo.missingFields.add( new Integer(i) );
        	    	if (qual != null && !qual.equals("*"))
        	    	{
        	    		gotoField = schema + "_" + element + '_' + qual;
        	    	}
        	    	else
        	    	{
        	    		gotoField = schema + "_" + element;
        	    	}
        	    }
        	}
        	// return to current edit metadata screen if any fields missing
        	if (subInfo.missingFields.size() > 0 )
        	{
        		subInfo.jumpToField = gotoField;
        		nextStep = curStep;
        	}
        	else
        	{
        		// determine next step - skipping unused MD pages
        		int lastMDPage = EDIT_METADATA_1 + inputsReader.getNumberInputPages(c.getHandle()) - 1;
        		if ( curStep == lastMDPage )
        		{
        		    curStep = EDIT_METADATA_2;
        		}
        		userHasReached(subInfo, curStep+1);
        		nextStep = curStep+1;
        	}
        }
        else if (buttonPressed.indexOf("remove") > -1)
        {
            // Remove button pressed - stay with same form
            nextStep = curStep;
        }

        // Write changes to database
        subInfo.submission.update();

        if (nextStep != -1)
        {
            doStep(context, request, response, subInfo, nextStep);
        }
        else
        {
            doStepJump(context, request, response, subInfo);
        }

        context.complete();
    }

    /**
     * Process the input from the choose file page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processChooseFile(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        File temp = null;
        FileUploadRequest wrapper = null;
        SubmissionInfo subInfo = null;
        boolean ok = false;
        String buttonPressed = null;
        BitstreamFormat bf = null;
        Bitstream b = null;

        /*
         * To distinguish between an IOException caused by a problem with the
         * file upload and one caused by displaying a JSP, we do all the upload
         * handling in a try/catch block
         */
        try
        {
            // Wrap multipart request to get the submission info
            wrapper = new FileUploadRequest(request);

            subInfo = getSubmissionInfo(context, wrapper);
            buttonPressed = UIUtil.getSubmitButton(wrapper, "submit_next");

            if ((subInfo != null) && buttonPressed.equals("submit_next"))
            {
                // Create the bitstream
                Item item = subInfo.submission.getItem();

                temp = wrapper.getFile("file");

                if (temp != null && temp.length() > 0)
                {
                    // Read the temp file into a bitstream
                    InputStream is = new BufferedInputStream(
                            new FileInputStream(temp));

                    // do we already have a bundle?
                    Bundle[] bundles = item.getBundles("ORIGINAL");

                    if (bundles.length < 1)
                    {
                        // set bundle's name to ORIGINAL
                        b = item.createSingleBitstream(is, "ORIGINAL");
                    }
                    else
                    {
                        // we have a bundle already, just add bitstream
                        b = bundles[0].createBitstream(is);
                    }

                    // Strip all but the last filename. It would be nice
                    // to know which OS the file came from.
                    String noPath = wrapper.getFilesystemName("file");

                    while (noPath.indexOf('/') > -1)
                    {
                        noPath = noPath.substring(noPath.indexOf('/') + 1);
                    }

                    while (noPath.indexOf('\\') > -1)
                    {
                        noPath = noPath.substring(noPath.indexOf('\\') + 1);
                    }

                    b.setName(noPath);
                    b.setSource(wrapper.getFilesystemName("file"));
                    b.setDescription(wrapper.getParameter("description"));

                    // Identify the format
                    bf = FormatIdentifier.guessFormat(context, b);

                    b.setFormat(bf);
                    
                    // Update to DB
                    b.update();
                    item.update();

                    if (bf == null || !bf.isInternal())
                    {
                    	ok = true;
                    }
                    else
                    {
                    	log.info("Attempt to upload file format marked as internal system use only");
                    }
                }
            }
        }
        catch (IOException ie)
        {
            // Problem with uploading
            log.warn(LogManager.getHeader(context, "upload_error", ""), ie);
        }

        if (subInfo == null)
        {
            // In any event, if we don't have the submission info, the request
            // was malformed
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
        }
        else if (buttonPressed.equals("submit_cancel"))
        {
            doCancellation(request, response, subInfo,
                    SubmitServlet.CHOOSE_FILE, SubmitServlet.UPLOAD_FILES);
        }
        else if (buttonPressed.equals("submit_prev"))
        {
            // Slightly tricky; if the user clicks on "previous,"
            // and they haven't uploaded any files yet, the previous
            // screen is the edit metadata 2 page. If there are
            // upload files, we go back to the file list page,
            // without uploading the file they've specified.
            if (subInfo.submission.getItem().getBundles("ORIGINAL").length > 0)
            {
                // Have files, go to list
                showUploadFileList(request, response, subInfo, false, false);
            }
            else
            {
                // No files, go back to last edit metadata page
            	Collection c = subInfo.submission.getCollection();
            	int lastPage = EDIT_METADATA_1 + 
				               inputsReader.getNumberInputPages( c.getHandle() ) - 1;
                doStep(context, request, response, subInfo, lastPage);
            }
        }
        else if (buttonPressed.equals("submit_next"))
        {
            // "Next" pressed - the actual upload was handled above.
            if (ok)
            {
                // Uploaded etc. OK
                if (bf != null)
                {
                    // Format was identified
                    showUploadFileList(request, response, subInfo, true, false);
                }
                else
                {
                    // Format couldn't be identified
                    showGetFileFormat(context, request, response, subInfo, b);
                }

                context.complete();
            }
            else
            {
                // If we get here, there was a problem uploading, but we
                // still know which submission we're dealing with
            	showProgressAwareJSP(request, response,	subInfo,
            	 	  	     "/submit/upload-error.jsp");
            }
        }
        else
        {
            doStepJump(context, wrapper, response, subInfo);
        }

        // Remove temp file if it's still around
        if (temp != null)
        {
            temp.delete();
        }
    }

    /**
     * Process input from get file type page
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
    private void processGetFileFormat(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit");

        if (subInfo.bitstream != null)
        {
            // Did the user select a format?
            int typeID = UIUtil.getIntParameter(request, "format");

            BitstreamFormat format = BitstreamFormat.find(context, typeID);

            if (format != null)
            {
                subInfo.bitstream.setFormat(format);
            }
            else
            {
                String userDesc = request.getParameter("format_description");
                subInfo.bitstream.setUserFormatDescription(userDesc);
            }

            subInfo.bitstream.update();

            if (buttonPressed.equals("submit"))
            {
                showUploadFileList(request, response, subInfo, true, false);
            }
            else
            {
                doStepJump(context, request, response, subInfo);
            }

            context.complete();
        }
        else
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Process input from file list page
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
    private void processFileList(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_next");
        Item item = subInfo.submission.getItem();

        if (buttonPressed.equals("submit_cancel"))
        {
            doCancellation(request, response, subInfo, SubmitServlet.FILE_LIST,
                    SubmitServlet.UPLOAD_FILES);
        }
        else if (buttonPressed.equals("submit_prev"))
        {
            // In some cases, this might be expected to go back
            // to the "choose file" page, but that doesn't make
            // a great deal of sense, so go back to last edit metadata page.
        	Collection c = subInfo.submission.getCollection();
        	int lastPage = EDIT_METADATA_1 + 
			               inputsReader.getNumberInputPages( c.getHandle() ) - 1;
            doStep(context, request, response, subInfo, lastPage);
        }
        else if (buttonPressed.equals("submit_next"))
        {
            // Finished the uploading of files
            // FIXME Validation check here
            // set primary bitstream
            if (request.getParameter("primary_bitstream_id") != null)
            {
                Bundle[] bundles = item.getBundles("ORIGINAL");
                bundles[0].setPrimaryBitstreamID(new Integer(request
                        .getParameter("primary_bitstream_id")).intValue());
                bundles[0].update();
            }

            userHasReached(subInfo, REVIEW_SUBMISSION);
            doStep(context, request, response, subInfo, REVIEW_SUBMISSION);
            context.complete();
        }
        else if (buttonPressed.equals("submit_more"))
        {
            // set primary bitstream
            if (request.getParameter("primary_bitstream_id") != null)
            {
                Bundle[] bundles = item.getBundles("ORIGINAL");
                bundles[0].setPrimaryBitstreamID(new Integer(request
                        .getParameter("primary_bitstream_id")).intValue());
                bundles[0].update();
                context.commit();
            }

            // Upload another file
            showProgressAwareJSP(request, response, subInfo, "/submit/choose-file.jsp");
        }
        else if (buttonPressed.equals("submit_show_checksums"))
        {
            // Show the checksums
            showUploadFileList(request, response, subInfo, false, true);
        }
        else if (buttonPressed.startsWith("submit_describe_"))
        {
            // Change the description of a bitstream
            Bitstream bitstream;

            // Which bitstream does the user want to describe?
            try
            {
                int id = Integer.parseInt(buttonPressed.substring(16));
                bitstream = Bitstream.find(context, id);
            }
            catch (NumberFormatException nfe)
            {
                bitstream = null;
            }

            if (bitstream == null)
            {
                // Invalid or mangled bitstream ID
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);

                return;
            }

            // Display the form letting them change the description
            subInfo.bitstream = bitstream;
            showProgressAwareJSP(request, response, subInfo,
                		 "/submit/change-file-description.jsp");
        }
        else if (buttonPressed.startsWith("submit_remove_"))
        {
            // A "remove" button must have been pressed
            Bitstream bitstream;

            // Which bitstream does the user want to describe?
            try
            {
                int id = Integer.parseInt(buttonPressed.substring(14));
                bitstream = Bitstream.find(context, id);
            }
            catch (NumberFormatException nfe)
            {
                bitstream = null;
            }

            if (bitstream == null)
            {
                // Invalid or mangled bitstream ID
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);

                return;
            }

            // remove bitstream from bundle..
            // delete bundle if it's now empty
            Bundle[] bundles = bitstream.getBundles();

            bundles[0].removeBitstream(bitstream);

            Bitstream[] bitstreams = bundles[0].getBitstreams();

            // remove bundle if it's now empty
            if (bitstreams.length < 1)
            {
                item.removeBundle(bundles[0]);
                item.update();
            }

            showFirstUploadPage(context, request, response, subInfo);
            context.complete();
        }
        else if (buttonPressed.startsWith("submit_format_"))
        {
            // A "format is wrong" button must have been pressed
            Bitstream bitstream;

            // Which bitstream does the user want to describe?
            try
            {
                int id = Integer.parseInt(buttonPressed.substring(14));
                bitstream = Bitstream.find(context, id);
            }
            catch (NumberFormatException nfe)
            {
                bitstream = null;
            }

            if (bitstream == null)
            {
                // Invalid or mangled bitstream ID
                log.warn(LogManager.getHeader(context, "integrity_error",
                        UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);

                return;
            }

            subInfo.bitstream = bitstream;
            showGetFileFormat(context, request, response, subInfo, bitstream);
        }
        else
        {
            doStepJump(context, request, response, subInfo);
        }
    }

    /**
     * Process input from the upload error page
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
    private void processUploadError(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_next");

        // no real options on the page, just retry!
        if (buttonPressed.equals("submit"))
        {
            showProgressAwareJSP(request, response, subInfo,
            		             "/submit/choose-file.jsp");
        }
        else
        {
            doStepJump(context, request, response, subInfo);
        }
    }

    /**
     * Process input from the "change file description" page
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
    private void processChangeFileDescription(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        if (subInfo.bitstream != null)
        {
            subInfo.bitstream.setDescription(request
                    .getParameter("description"));
            subInfo.bitstream.update();

            if (request.getParameter("submit") != null)
            {
                showUploadFileList(request, response, subInfo, false, false);
            }
            else
            {
                doStepJump(context, request, response, subInfo);
            }

            context.complete();
        }
        else
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Process information from "submission cancelled" page
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
    private void processCancellation(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_back");

        if (buttonPressed.equals("submit_back"))
        {
            // User wants to continue with submission
            int previous = UIUtil.getIntParameter(request, "previous_step");

            doStep(context, request, response, subInfo, previous);
        }
        else if (buttonPressed.equals("submit_remove"))
        {
            // User wants to cancel and remove
            // Cancellation page only applies to workspace items
            WorkspaceItem wi = (WorkspaceItem) subInfo.submission;

            wi.deleteAll();

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
            doStepJump(context, request, response, subInfo);
        }
    }

    /**
     * Process button click on "review submission" page
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
    private void processReview(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_cancel");

        if (buttonPressed.equals("submit_cancel"))
        {
            doCancellation(request, response, subInfo, REVIEW_SUBMISSION,
                    REVIEW_SUBMISSION);
        }
        else if (buttonPressed.equals("submit_next"))
        {
            // If the user is performing the initial submission
            // of an item, we go to the grant license stage
            if (!isWorkflow(subInfo))
            {
                // proceed to next step conditional on CC
                int nextStep = CreativeCommons.isEnabled() ? CC_LICENSE
                        : GRANT_LICENSE;
                userHasReached(subInfo, nextStep);
                doStep(context, request, response, subInfo, nextStep);
                context.complete();
            }
            else
            {
                // The user is performing an edit as part
                // of a workflow task, so we take them
                // back to the relevant perform task page
                request.setAttribute("workflow.item", subInfo.submission);
                JSPManager.showJSP(request, response,
                        "/mydspace/perform-task.jsp");
            }
        }
        else if (buttonPressed.equals("submit_prev"))
        {
            // Back to file upload
            doStep(context, request, response, subInfo, UPLOAD_FILES);
        }
        else
        {
            doStepJump(context, request, response, subInfo);
        }
    }

    /**
     * Process the input from the license page
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
    private void processLicense(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_cancel");

        if (buttonPressed.equals("submit_grant"))
        {
            // License granted
            log.info(LogManager.getHeader(context, "accept_license",
                    getSubmissionLogInfo(subInfo)));

            // Add the license to the item
            Item item = subInfo.submission.getItem();
            EPerson submitter = context.getCurrentUser();

            // FIXME: Probably need to take this from the form at some point
            String license = subInfo.submission.getCollection().getLicense();

            item.licenseGranted(license, submitter);

            // Start the workflow
            WorkflowManager.start(context, (WorkspaceItem) subInfo.submission);

            // FIXME: pass in more information about what happens next?
            showProgressAwareJSP(request, response, subInfo, "/submit/complete.jsp");
            context.complete();
        }
        else if (request.getParameter("submit_reject") != null)
        {
            // User has rejected license.
            log.info(LogManager.getHeader(context, "reject_license",
                    getSubmissionLogInfo(subInfo)));

            // Show information page.
            JSPManager.showJSP(request, response,
                    "/submit/license-rejected.jsp");
        }
        else
        {
            doStepJump(context, request, response, subInfo);
        }
    }

    /**
     * Process the input from the CC license page
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
    private void processCC(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, "submit_next");

        // Firstly, check for a click of the cancel button.
        if (buttonPressed.equals("submit_cancel"))
        {
            doCancellation(request, response, subInfo, CC_LICENSE, CC_LICENSE);
        }
        else if (buttonPressed.equals("submit_prev"))
        {
            // Back to review submission
            doStep(context, request, response, subInfo, REVIEW_SUBMISSION);
        }
        else if (buttonPressed.equals("submit_next"))
        {
            // Update user's progress
            userHasReached(subInfo, GRANT_LICENSE);

            // User has clicked "Next"
            doStep(context, request, response, subInfo, GRANT_LICENSE);
            context.complete();
        }
        else if (buttonPressed.equals("submit_no_cc"))
        {
            // Skipping the CC license - remove any existing license selection
            CreativeCommons
                    .removeLicense(context, subInfo.submission.getItem());
            userHasReached(subInfo, GRANT_LICENSE);
            doStep(context, request, response, subInfo, GRANT_LICENSE);
            context.complete();
        }
        else
        {
            // RLR hack - need to distinguish between progress bar real
            // submission
            String ccLicenseUrl = request.getParameter("cc_license_url");
            if ((ccLicenseUrl != null) && (ccLicenseUrl.length() > 0))
            {
                Item item = subInfo.submission.getItem();

                // set the CC license
                CreativeCommons.setLicense(context, item, ccLicenseUrl);

                userHasReached(subInfo, GRANT_LICENSE);
                doStep(context, request, response, subInfo, GRANT_LICENSE);
                context.complete();
            }
            else
            {
                doStepJump(context, request, response, subInfo);
            }
        }
    }

    //****************************************************************
    //****************************************************************
    //             METHODS FOR SHOWING FORMS
    //****************************************************************
    //****************************************************************

    /**
     * Process a click on a buttonin the progress bar. to jump to a step. This
     * method should be called when it has been determined that no other button
     * has been pressed.
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
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException
    {
        // Find the button that was pressed. It would start with
        // "submit_jump_".
        String buttonPressed = UIUtil.getSubmitButton(request, "");

        // Now, if the request was a multi-part (file upload), we need to
        // get the original request back out, as the wrapper causes problems
        // further down the line.
        if (request instanceof FileUploadRequest)
        {
            FileUploadRequest fur = (FileUploadRequest) request;
            request = fur.getOriginalRequest();
        }

        int nextStep = -1;

        if (buttonPressed.startsWith("submit_jump_"))
        {
            // Button on progress bar pressed
            try
            {
                nextStep = Integer.parseInt(buttonPressed.substring(12));
            }
            catch (NumberFormatException ne)
            {
                // mangled number
                nextStep = -1;
            }

            // Integrity check: make sure they aren't going
            // forward or backward too far
            if (nextStep <= SubmitServlet.SELECT_COLLECTION)
            {
                nextStep = -1;
            }

            if (!isWorkflow(subInfo) && (nextStep > getStepReached(subInfo)))
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
            // Do the relevant step
            doStep(context, request, response, subInfo, nextStep);
        }
    }

    /**
     * Display the page for the relevant step. Pass in a step number between
     * SubmitServlet.INITIAL_QUESTIONS and SubmitServlet.SUBMISSION_COMPLETE -
     * other cases (such as cancellations and multi-file upload interactions)
     * are handled elsewhere.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            SubmissionInfo pertaining to this submission
     * @param step
     *            the step number to display
     */
    private void doStep(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int step)
            throws ServletException, IOException, SQLException
    {
        // determine collection
        Collection c = subInfo.submission.getCollection();
        
        if ( step >= EDIT_METADATA_1 && step <= EDIT_METADATA_2 )
        {
        	// requires configurable form info per collection
        	request.setAttribute( "submission.inputs", inputsReader.getInputs(c.getHandle()));
        	// also indicate page
           	request.setAttribute( "submission.page", new Integer(step) );
		showProgressAwareJSP(request, response, subInfo,
                  		     "/submit/edit-metadata.jsp");
            return;
        }

        switch (step)
        {
        case INITIAL_QUESTIONS:
            // requires configurable form info per collection
            request.setAttribute( "submission.inputs", inputsReader.getInputs(c.getHandle()));
            showProgressAwareJSP(request, response, subInfo,
                		 "/submit/initial-questions.jsp");
            break;
            
        /* EDIT_METADATA cases handled above */
            
        case UPLOAD_FILES:
            showFirstUploadPage(context, request, response, subInfo);

            break;

        case CHOOSE_FILE:
            showProgressAwareJSP(request, response, subInfo,
            		         "/submit/choose-file.jsp");
            break;

        case FILE_LIST:
            showUploadFileList(request, response, subInfo, false, false);
            break;

        case REVIEW_SUBMISSION:
            // requires configurable form info per collection
            request.setAttribute( "submission.inputs", inputsReader.getInputs(c.getHandle()));
            showProgressAwareJSP(request, response, subInfo,
            		         "/submit/review.jsp");
            break;

        case GRANT_LICENSE:
            request.setAttribute("license", getLicense(c, context));
            showProgressAwareJSP(request, response, subInfo,
            		         "/submit/show-license.jsp");
            break;

        case CC_LICENSE:

            // Do we already have a CC license?
            Item item = subInfo.submission.getItem();
            boolean exists =  CreativeCommons.hasLicense(context, item);
            request.setAttribute("cclicense.exists", new Boolean(exists) );
            showProgressAwareJSP(request, response, subInfo,
            		         "/submit/creative-commons.jsp");
            break;

        case SUBMISSION_COMPLETE:
            showProgressAwareJSP(request, response, subInfo,
            		         "/submit/complete.jsp");
            break;

        default:
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Respond to the user clicking "cancel".
     * 
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            SubmissionInfo object
     * @param step
     *            step corresponding to the page the user clicked "cancel" on.
     * @param displayStep
     *            the step the user had reached in terms of the progress bar.
     */
    private void doCancellation(HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int step,
            int displayStep) throws ServletException, IOException, SQLException
    {
        // If this is a workflow item, we need to return the
        // user to the "perform task" page
        if (isWorkflow(subInfo))
        {
            request.setAttribute("workflow.item", subInfo.submission);
            JSPManager.showJSP(request, response, "/mydspace/perform-task.jsp");
        }
        else
        {
            request.setAttribute("step", String.valueOf(step));
            request.setAttribute("display.step", String.valueOf(displayStep));
            showProgressAwareJSP(request, response, subInfo, "/submit/cancel.jsp");
        }
    }

    /**
     * Display the first appropriate page in the file upload sequence. Which
     * page this is depends on whether the user has uploaded any files in this
     * item already.
     * 
     * @param context
     *            the DSpace context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     */
    private void showFirstUploadPage(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws SQLException, ServletException,
            IOException
    {
        Bundle[] bundles = subInfo.submission.getItem().getBundles("ORIGINAL");

        if (bundles.length > 0)
        {
            // The item has files associated with it.
            showUploadFileList(request, response, subInfo, false, false);
        }
        else
        {
            // No items uploaded yet; show the "choose file" page
            doStep(context, request, response, subInfo, CHOOSE_FILE);
        }
    }

    /**
     * Show the upload file page
     * 
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     * @param justUploaded
     *            pass in true if the user just successfully uploaded a file
     * @param showChecksums
     *            pass in true if checksums should be displayed
     */
    private void showUploadFileList(HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo,
            boolean justUploaded, boolean showChecksums) throws SQLException,
            ServletException, IOException
    {
        // Set required attributes
        request.setAttribute("just.uploaded", new Boolean(justUploaded));
        request.setAttribute("show.checksums", new Boolean(showChecksums));

        // Always go to advanced view in workflow mode
        if (isWorkflow(subInfo) || subInfo.submission.hasMultipleFiles())
        {
            showProgressAwareJSP(request, response, subInfo,
       				 "/submit/upload-file-list.jsp");
        }
        else
        {
            // FIXME: Assume one and only one bitstream
            showProgressAwareJSP(request, response, subInfo,
       				 "/submit/show-uploaded-file.jsp");
        }
    }

    /**
     * Get the type of a file from the user
     * 
     * @param context
     *            context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param subInfo
     *            the SubmissionInfo object
     * @param bitstream
     *            the Bitstream to get the type of
     */
    private void showGetFileFormat(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo,
            Bitstream bitstream) throws SQLException, ServletException,
            IOException
    {
        BitstreamFormat[] formats = BitstreamFormat.findNonInternal(context);

        subInfo.bitstream = bitstream;

        request.setAttribute("bitstream.formats", formats);
 
        // What does the system think it is?
        BitstreamFormat guess = FormatIdentifier
                .guessFormat(context, bitstream);

        request.setAttribute("guessed.format", guess);

        showProgressAwareJSP(request, response, subInfo,
       		             "/submit/get-file-format.jsp");
    }

    //****************************************************************
    //****************************************************************
    //             MISCELLANEOUS CONVENIENCE METHODS
    //****************************************************************
    //****************************************************************
    
    /**
     * Show a JSP after setting attributes needed by progress bar
     * @param request	the request object
     * @param response  the response object
     * @param subInfo   the SubmissionInfo object
     * @param jspPath	relative path to JSP
     */
     private void showProgressAwareJSP(
            HttpServletRequest request,
	        HttpServletResponse response,
	        SubmissionInfo subInfo,
			String jspPath)
     		throws ServletException, IOException
     {
     	// all JSPs displaying the progress bar need to know the
     	// number of metadata edit pages
        subInfo.numMetadataPages =
        	inputsReader.getNumberInputPages(subInfo.submission.getCollection().getHandle());
        request.setAttribute("submission.info", subInfo);
        
        JSPManager.showJSP(request, response, jspPath);
     }

    /**
     * Get a filled-out submission info object from the parameters in the
     * current request. If there is a problem, <code>null</code> is returned.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * 
     * @return filled-out submission info, or null
     */
    private SubmissionInfo getSubmissionInfo(Context context,
            HttpServletRequest request) throws SQLException
    {
        SubmissionInfo info = new SubmissionInfo();

        if (request.getParameter("workflow_id") != null)
        {
            int workflowID = UIUtil.getIntParameter(request, "workflow_id");
            info.submission = WorkflowItem.find(context, workflowID);
        }
        else
        {
            int workspaceID = UIUtil.getIntParameter(request,
                    "workspace_item_id");
            info.submission = WorkspaceItem.find(context, workspaceID);
        }

        // Is something wrong?
        if (info.submission == null)
        {
            return null;
        }

        if (request.getParameter("bundle_id") != null)
        {
            int bundleID = UIUtil.getIntParameter(request, "bundle_id");
            info.bundle = Bundle.find(context, bundleID);
        }

        if (request.getParameter("bitstream_id") != null)
        {
            int bitstreamID = UIUtil.getIntParameter(request, "bitstream_id");
            info.bitstream = Bitstream.find(context, bitstreamID);
        }

        return info;
    }

    /**
     * Is the submission in the workflow process?
     * 
     * @param si
     *            the submission info
     * @return true if the submission is in the workflow process
     */
    public static boolean isWorkflow(SubmissionInfo si)
    {
        return ((si.submission != null) && si.submission instanceof WorkflowItem);
    }

    /**
     * Return the submission info as hidden parameters for an HTML form
     * 
     * @param si
     *            the submission info
     * @return HTML hidden parameters
     */
    public static String getSubmissionParameters(SubmissionInfo si)
    {
        String info = "";

        if (isWorkflow(si))
        {
            info = info + "<input type=\"hidden\" name=\"workflow_id\" value=\""
                    + si.submission.getID() + "\"/>";
        }
        else
        {
            info = info + "<input type=\"hidden\" name=\"workspace_item_id\" value=\""
                    + si.submission.getID() + "\"/>";
        }

        if (si.bundle != null)
        {
            info = info + "<input type=\"hidden\" name=\"bundle_id\" value=\""
                    + si.bundle.getID() + "\"/>";
        }

        if (si.bitstream != null)
        {
            info = info + "<input type=\"hidden\" name=\"bitstream_id\" value=\""
                    + si.bitstream.getID() + "\"/>";
        }

        return info;
    }

    /**
     * Return text information suitable for logging
     * 
     * @param si
     *            the submission info
     * @return the type and ID of the submission, bundle and/or bitstream for
     *         logging
     */
    public String getSubmissionLogInfo(SubmissionInfo si)
    {
        String info = "";

        if (isWorkflow(si))
        {
            info = info + "workflow_id=" + si.submission.getID();
        }
        else
        {
            info = info + "workspace_item_id" + si.submission.getID();
        }

        if (si.bundle != null)
        {
            info = info + ",bundle_id=" + si.bundle.getID();
        }

        if (si.bitstream != null)
        {
            info = info + ",bitstream_id=" + si.bitstream.getID();
        }

        return info;
    }

    /**
     * Indicate the user has advanced to the given stage. This will only
     * actually do anything when it's a user initially entering a submission. It
     * will only increase the "stage reached" column - it will not "set back"
     * where a user has reached.
     * 
     * @param subInfo
     *            the SubmissionInfo object pertaining to the current submission
     * @param step
     *            the step the user has just reached
     */
    private void userHasReached(SubmissionInfo subInfo, int step)
            throws SQLException, AuthorizeException, IOException
    {
        if (!isWorkflow(subInfo))
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.submission;

            if (step > wi.getStageReached())
            {
                wi.setStageReached(step);
                wi.update();
            }
        }
    }

    /**
     * Find out which step a user has reached in the submission process. If the
     * submission is in the workflow process, this returns REVIEW_SUBMISSION.
     * 
     * @param subInfo
     *            submission info object
     * 
     * @return step reached, between SELECT_COLLECTION and SUBMISSION_COMPLETE
     */
    public static int getStepReached(SubmissionInfo subInfo)
    {
        if (isWorkflow(subInfo))
        {
            return -1;
        }
        else
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.submission;
            int i = wi.getStageReached();

            // Uninitialised workspace items give "-1" as the stage reached
            // this is a special value used by the progress bar, so we change
            // it to "INITIAL_QUESTIONS"
            if (i == -1)
            {
                i = INITIAL_QUESTIONS;
            }

            return i;
        }
    }

    //****************************************************************
    //****************************************************************
    //       METHODS FOR FILLING DC FIELDS FROM METADATA FORMS
    //****************************************************************
    //****************************************************************

    /**
     * Set relevant DC fields in an item from name values in the form. Some
     * fields are repeatable in the form. If this is the case, and the field is
     * "contributor.author", the names in the request will be from the fields as
     * follows:
     * 
     * contributor_author_last_0 -> last name of first author
     * contributor_author_first_0 -> first name(s) of first author
     * contributor_author_last_1 -> last name of second author
     * contributor_author_first_1 -> first name(s) of second author
     * 
     * and so on. If the field is unqualified:
     * 
     * contributor_last_0 -> last name of first contributor contributor_first_0 ->
     * first name(s) of first contributor
     * 
     * If the parameter "submit_contributor_author_remove_n" is set, that value
     * is removed.
     * 
     * Otherwise the parameters are of the form:
     * 
     * contributor_author_last contributor_author_first
     * 
     * The values will be put in separate DCValues, in the form "last name,
     * first name(s)", ordered as they appear in the list. These will replace
     * any existing values.
     * 
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema 
     *            the DC schema
     * @param element
     *            the DC element
     * @param qualifier
     *            the DC qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     */
    private void readNames(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated)
    {
        String dcname = MetadataField.formKey(schema,element,qualifier);

        // Names to add
        List firsts = new LinkedList();
        List lasts = new LinkedList();

        if (repeated)
        {
            firsts = getRepeatedParameter(request, dcname + "_first");
            lasts = getRepeatedParameter(request, dcname + "_last");

            // Find out if the relevant "remove" button was pressed
            String buttonPressed = UIUtil.getSubmitButton(request, "");
            String removeButton = "submit_" + dcname + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                firsts.remove(valToRemove);
                lasts.remove(valToRemove);
            }
        }
        else
        {
            // Just a single name
            String lastName = request.getParameter(dcname + "_last");
            String firstNames = request.getParameter(dcname + "_first");

            lasts.add(lastName);
            firsts.add(firstNames);
        }

        // Remove existing values
        item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < lasts.size(); i++)
        {
            String f = (String) firsts.get(i);
            String l = (String) lasts.get(i);

            // only add if lastname is non-empty
            if ((l != null) && !((l.trim()).equals("")))
            {
                // Ensure first name non-null
                if (f == null)
                {
                    f = "";
                }

                // If there is a comma in the last name, we take everything
                // after that comma, and add it to the right of the
                // first name
                int comma = l.indexOf(',');

                if (comma >= 0)
                {
                    f = f + l.substring(comma + 1);
                    l = l.substring(0, comma);

                    // Remove leading whitespace from first name
                    while (f.startsWith(" "))
                    {
                        f = f.substring(1);
                    }
                }

                // Add to the database
                item.addMetadata(schema, element, qualifier, null, new DCPersonName(l, f)
                        .toString());
            }
        }
    }

    /**
     * Fill out an item's DC values from a plain standard text field. If the
     * field isn't repeatable, the input field name is called:
     * 
     * element_qualifier
     * 
     * or for an unqualified element:
     * 
     * element
     * 
     * Repeated elements are appended with an underscore then an integer. e.g.:
     * 
     * title_alternative_0 title_alternative_1
     * 
     * The values will be put in separate DCValues, ordered as they appear in
     * the list. These will replace any existing values.
     * 
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema 
     *            the short schema name
     * @param element
     *            the DC element
     * @param qualifier
     *            the DC qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     * @param lang
     *            language to set (ISO code)
     */
    private void readText(HttpServletRequest request, Item item, String schema, 
            String element, String qualifier, boolean repeated, String lang)
    {
        // FIXME: Of course, language should be part of form, or determined
        // some other way
        String dcname = MetadataField.formKey(schema,element,qualifier);

        // Values to add
        List vals = new LinkedList();

        if (repeated)
        {
            vals = getRepeatedParameter(request, dcname);

            // Find out if the relevant "remove" button was pressed
            String buttonPressed = UIUtil.getSubmitButton(request, "");
            String removeButton = "submit_" + dcname + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                vals.remove(valToRemove);
            }
        }
        else
        {
            // Just a single name
            String value = request.getParameter(dcname);
            if (value != null)
            {
                value.trim();
            }
            vals.add(value);
        }

        // Remove existing values
        item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < vals.size(); i++)
        {
            // Add to the database if non-empty
            String s = (String) vals.get(i);

            if ((s != null) && !s.equals(""))
            {
                item.addMetadata(schema, element, qualifier, lang, s);
            }
        }
    }

    /**
     * Fill out a DC date field with the value from a form. The date is taken
     * from the three parameters:
     * 
     * element_qualifier_year element_qualifier_month element_qualifier_day
     * 
     * The granularity is determined by the values that are actually set. If the
     * year isn't set (or is invalid)
     * 
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema 
     *            the DC schema
     * @param element
     *            the DC element
     * @param qualifier
     *            the DC qualifier, or null if unqualified
     * @throws SQLException 
     */
    private void readDate(HttpServletRequest request, Item item,
            String schema, String element, String qualifier) throws SQLException
    {
        String dcname = MetadataField.formKey(schema,element,qualifier);

        int year = UIUtil.getIntParameter(request, dcname + "_year");
        int month = UIUtil.getIntParameter(request, dcname + "_month");
        int day = UIUtil.getIntParameter(request, dcname + "_day");

        // FIXME: Probably should be some more validation
        // Make a standard format date
        DCDate d = new DCDate();

        d.setDateLocal(year, month, day, -1, -1, -1);

        item.clearMetadata(schema, element, qualifier, Item.ANY);

        if (year > 0)
        {
            // Only put in date if there is one!
            item.addMetadata(schema, element, qualifier, null, d.toString());
        }
    }

    /**
     * Set relevant DC fields in an item from series/number values in the form.
     * Some fields are repeatable in the form. If this is the case, and the
     * field is "relation.ispartof", the names in the request will be from the
     * fields as follows:
     * 
     * relation_ispartof_series_0 relation_ispartof_number_0
     * relation_ispartof_series_1 relation_ispartof_number_1
     * 
     * and so on. If the field is unqualified:
     * 
     * relation_series_0 relation_number_0
     * 
     * Otherwise the parameters are of the form:
     * 
     * relation_ispartof_series relation_ispartof_number
     * 
     * The values will be put in separate DCValues, in the form "last name,
     * first name(s)", ordered as they appear in the list. These will replace
     * any existing values.
     * 
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema 
     *            the DC schema
     * @param element
     *            the DC element
     * @param qualifier
     *            the DC qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     */
    private void readSeriesNumbers(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated)
        {
        String dcname =  MetadataField.formKey(schema,element,qualifier);

        // Names to add
        List series = new LinkedList();
        List numbers = new LinkedList();

        if (repeated)
        {
            series = getRepeatedParameter(request, dcname + "_series");
            numbers = getRepeatedParameter(request, dcname + "_number");

            // Find out if the relevant "remove" button was pressed
            String buttonPressed = UIUtil.getSubmitButton(request, "");
            String removeButton = "submit_" + dcname + "_remove_";

            if (buttonPressed.startsWith(removeButton))
            {
                int valToRemove = Integer.parseInt(buttonPressed
                        .substring(removeButton.length()));

                series.remove(valToRemove);
                numbers.remove(valToRemove);
            }
        }
        else
        {
            // Just a single name
            String s = request.getParameter(dcname + "_series");
            String n = request.getParameter(dcname + "_number");

            // Only put it in if there was a name present
            if ((s != null) && !s.equals(""))
            {
                series.add(s);
                numbers.add(n);
            }
        }

        // Remove existing values
        item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < series.size(); i++)
        {
            String s = ((String) series.get(i)).trim();
            String n = ((String) numbers.get(i)).trim();

            // Only add non-empty
            if (!s.equals("") || !n.equals(""))
            {
                item.addMetadata(schema, element, qualifier, null, new DCSeriesNumber(s, n)
                        .toString());
            }
        }
    }
    
    /**
     * Get the license that users must grant before submitting to this
     * collection. If the collection does not have a specific license, the
     * site-wide default is returned.
     * 
     * @return the license for this collection
     */
     public String getLicense(Collection c, Context context)
     {
         // FIXME: attributes of collections are not yet i18n
         String license = c.getLicenseCollection();

         if ((license == null) || license.equals(""))
         {
             // Fallback to site-wide default for current Locale
             license = ConfigurationManager.getLicenseText(I18nUtil.getDefaultLicense(context));
         }
         return license;
     }
     
    /**
     * Get repeated values from a form. If "foo" is passed in, values in the
     * form of parameters "foo_0", "foo_1", etc. are returned.
     * 
     * @param request
     *            the HTTP request containing the form information
     * @param param
     *            the repeated parameter
     * 
     * @return a List of Strings
     */
    private List getRepeatedParameter(HttpServletRequest request, String param)
    {
        List vals = new LinkedList();

        int i = 0;
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast)
        {
            String s = request.getParameter(param + "_" + i);

            // We're only going to add non-null values
            if (s != null)
            {
                vals.add(s.trim());
            }
            else
            {
                // If the value was null (as opposed to present,
                // but empty) we've reached the last name
                foundLast = true;
            }

            i++;
        }

        return vals;
    }
}
