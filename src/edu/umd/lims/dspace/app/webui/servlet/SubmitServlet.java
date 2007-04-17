package edu.umd.lims.dspace.app.webui.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.sql.SQLException;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.DCInputsReader;
import org.dspace.app.webui.util.DCInput;
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
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.license.CreativeCommons;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

public class SubmitServlet extends org.dspace.app.webui.servlet.SubmitServlet
{
    /**
     * <pre>
     * Revision History:
     *
     *   2006/05/23: Ben
     *     - revert date.issued to being always required
     *
     *   2006/05/18: Ben
     *     - make date.issued optional for other than "published before"
     *     - don't clear date.issued when changing "published before" status
     *
     *   2005/06/15: Ben
     *     - use new configurable metadata function in 1.2.2:
     *       move required author and citation to input-forms.xml
     *
     *   2005/05/24: Ben
     *     - fixed bug if no mapped collections
     *
     *   2005/05/13: Ben
     *     - moved from org.dspace to edu.umd.lims.dspace
     *     - add mapped collections
     *
     *   2005/03/25: Ben
     *     - make author required in edit-metadata-1
     *     - make citation required in edit-metadata-1 if 
     *       previously published
     *
     * </pre>
     */

    public SubmitServlet()
    throws ServletException
    {
	super();
    }

     protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
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
        if ((step == SELECT_COLLECTION || step == -1) &&
	    request.getParameter("submit_cancel") == null &&
	    request.getParameter("submit_next") == null)
        {
	  // First we find the collection
	  int id = UIUtil.getIntParameter(request, "collection");
	  Collection col = Collection.find(context, id);

	  // Show all collections
	  Collection[] collections = Collection.findAuthorized(context, null,
								Constants.ADD);

	  log.info(LogManager.getHeader(context, "select_collection", ""));

	  request.setAttribute("collections", collections);
	  request.setAttribute("collection", col);
	  JSPManager.showJSP(request, response,
			     "/submit/select-collection.jsp");

	  return;
        }

	super.doDSPost(context, request, response);
    }

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
    protected void processSelectCollection(Context context,
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

        // First we find the list of collections to submit to
	int ids[] = UIUtil.getIntParameters(request, "mapcollections");
	if (ids == null) {
	  Collection[] cols = Collection.findAuthorized(context, null,
							Constants.ADD);

	  log.info(LogManager.getHeader(context, "select_collection", ""));

	  request.setAttribute("collections", cols);
	  JSPManager.showJSP(request, response,
			     "/submit/select-collection.jsp");

	  return;
	}

	Collection cols[] = new Collection[ids.length];
	Collection col = null;

	for (int i=0; i < cols.length; i++) {
	  cols[i] = Collection.find(context, ids[i]);
	  if (cols[i] == null) {
	    JSPManager.showInvalidIDError(request, response, ( new Integer(ids[i])).toString(), 
					  Constants.COLLECTION);
	    return;
	  }
	}

	// How many do we have?
	if (cols.length == 1) {
	  // No need to select a primary collection, only one was selected
	  col = cols[0];

	} else {
	  // Randomly select one of the collections to be the primary
	  Random r = new Random((new Date()).getTime());
	  col = cols[r.nextInt(cols.length)];
	}

	// Create the workspaceitem
	WorkspaceItem wi = WorkspaceItem.create(context, col, true);

	// Add mapped collections
	for (int i=0; i < cols.length; i++) {
	  if (!cols[i].equals(col)) {
	    wi.addMapCollection(cols[i]);
	  }
	}

	// Proceed to first step
	SubmissionInfo si = new SubmissionInfo();
	si.submission = wi;
	doStep(context, request, response, si, INITIAL_QUESTIONS);

	context.complete();
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
    protected void processVerifyPrune(Context context,
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
	  //item.clearDC("date", "issued", Item.ANY);
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

}
