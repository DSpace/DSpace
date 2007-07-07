/*
 * FlowUtils.java
 *
 * Version: $Revision: 1.21 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.DCSeriesNumber;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.license.CreativeCommons;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * This is a utility class to aid in the submission flow scripts. 
 * Since data validation is cumbersome inside a flow script this 
 * is a collection of methods to preform processing at each step 
 * of the flow, the flow script will ties these operations 
 * together in a meaningfull order but all actualy processing 
 * is done through these variaus processes.
 * 
 * @author Scott Phillips
 */

public class FlowUtils {

	/**
	 * A shared resource of the inputs reader. The 'inputs' are the 
	 * questions we ask the user to describe an item during the 
	 * submission process. The reader is a utility class to read 
	 * that configuration file.
	 */
	private static DCInputsReader INPUTS_READER = null;

	/**
	 * Ensure that the reader has been initialized, this method may be
	 * called multiple times with no ill-effect.
	 */
	public static void initializeInputsReader() throws ServletException
	{
		if (INPUTS_READER == null)
			INPUTS_READER = new DCInputsReader();
	}

	/**
	 * Return the inputs reader. Note, the reader must have been
	 * initialized before the reader can be accessed.
	 * 
	 * @return The input reader.
	 */
	public static DCInputsReader getInputsReader() 
	{
		return INPUTS_READER;
	}


	/**
	 * Return the InProgressSubmission, either workspaceItem or workflowItem, 
	 * depending on the id provided. If the id begins with an S then it is a
	 * considered a workspaceItem. If the id begins with a W then it is
	 * considered a workflowItem.
	 * 
	 * @param context
	 * @param inProgressSubmissionID
	 * @return The InprogressSubmission or null if non found
	 */
	public static InProgressSubmission findSubmission(Context context, String inProgressSubmissionID) throws SQLException
	{
		char type = inProgressSubmissionID.charAt(0);
		int id = Integer.valueOf(inProgressSubmissionID.substring(1));
		
		if (type == 'S')
		{
			return WorkspaceItem.find(context, id);
		}
		else if (type == 'W')
		{
			return WorkflowItem.find(context, id);
		}
		return null;
	}
	
	/**
	 * Return the workspace identified by the given id, the id should be 
	 * prepended with the character S to signify that it is a workspace 
	 * instead of a workflow.
	 * 
	 * @param context
	 * @param inProgressSubmissionID
	 * @return The found workspaceitem or null if none found.
	 */
	public static WorkspaceItem findWorkspace(Context context, String inProgressSubmissionID) throws SQLException
	{
		InProgressSubmission submission = findSubmission(context, inProgressSubmissionID);
		if (submission instanceof WorkspaceItem)
			return (WorkspaceItem) submission;
		return null;
	}
	
	/**
	 * Return the workflow identified by the given id, the id should be 
	 * prepended with the character S to signify that it is a workflow 
	 * instead of a workspace.
	 * 
	 * @param context
	 * @param inProgressSubmissionID
	 * @return The found workflowitem or null if none found.
	 */
	public static WorkflowItem findWorkflow(Context context, String inProgressSubmissionID) throws SQLException
	{
		InProgressSubmission submission = findSubmission(context, inProgressSubmissionID);
		if (submission instanceof WorkflowItem)
			return (WorkflowItem) submission;
		return null;
	}
	
	/**
	 * Return the number of describe pages for workflow or workspace items.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
	 * @return The number of pages in the submission process.
	 */
	public static int getNumberOfDescribePages(Context context, String id) throws UIException
	{
		try {
			InProgressSubmission submission = findSubmission(context, id);
			Collection collection = submission.getCollection();
			String handle = collection.getHandle();
			
			initializeInputsReader();
			int numberOfPages = getInputsReader().getInputs(handle).getNumberPages();
			return numberOfPages;
		} 
		catch (SQLException sqle)
		{
			throw new UIException(sqle);
		}
		catch (ServletException se)
		{
			throw new UIException(se);
		}
	}
	
	
	/**
     * Indicate the user has advanced to the given step. This will only
     * actually do anything when it's a user initially entering a submission. It
     * will only increase the "stage reached" column - it will not "set back"
     * where a user has reached.
     * 
     * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
     * @param step the step the user has just reached
     */
    public static void setStepReached(Context context, String id, int step)
            throws SQLException, AuthorizeException, IOException
    {
        InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			
			if (step > workspaceItem.getStageReached())
			{
				workspaceItem.setStageReached(step);
				workspaceItem.update();
				context.commit();
			}
		}
    }
	
    /**
     * Find the maximum step the user has reached in the submission processes. 
     * If this submission is a workflow then return max-int.
     * 
     * @param context The current DSpace content
	 * @param id The unique ID of the current workflow/workspace
     */
	public static int getMaximumStepReached(Context context, String id) throws SQLException {
		
		InProgressSubmission submission = findSubmission(context, id);
		
		if (submission instanceof WorkspaceItem)
		{
			WorkspaceItem workspaceItem = (WorkspaceItem) submission;
			int stage = workspaceItem.getStageReached();
			if (stage < 0)
				stage = 0;
			return stage;
		}
		
		// This is a workflow, return infinity.
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Process the save or remove step. If the user has selected to 
	 * remove their submission then remove it.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace/workflow
	 * @param request The cocoon request object.
	 */
	public static void processSaveOrRemove(Context context, String id, Request request) throws SQLException, AuthorizeException, IOException
	{
		if (request.getParameter("submit_remove") != null)
		{
			// If they selected to remove the item then delete everything.
			WorkspaceItem workspace = findWorkspace(context,id);
			workspace.deleteAll();
	        context.commit();
		}
	}
	
	/**
	 * Process the initial question step. This will record the decisions
	 * for weather the item has multiple titles or has been published before.
	 * Also if there are any metadata that needs to be removed then they 
	 * will be removed., i.e changed from multiple titles to a sinle title.
	 * 
	 * Note, there is no confirmation step on removing these items, it is
	 * expected that the user was warned before processing.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace/workflow
	 * @param request The cocoon request object.
	 */
	public static void processInitialQuestions(Context context, String id, Request request) throws SQLException, AuthorizeException, IOException
	{
		InProgressSubmission submission = findSubmission(context, id);
		Item item = submission.getItem();
		
		
		if (request.getParameter("multiple-titles") == null)
		{
			submission.setMultipleTitles(false);
			item.clearMetadata(MetadataSchema.DC_SCHEMA,"title","alternative", Item.ANY);
		}
		else
		{
			submission.setMultipleTitles(true);
		}
		
		if (request.getParameter("published-before") == null)
		{
			submission.setPublishedBefore(false);
			item.clearMetadata(MetadataSchema.DC_SCHEMA,"date","issued",Item.ANY);
			item.clearMetadata(MetadataSchema.DC_SCHEMA,"identifier","citation",Item.ANY);
			item.clearMetadata(MetadataSchema.DC_SCHEMA,"publisher",null,Item.ANY);
		}
		else
		{
			submission.setPublishedBefore(true);
		}
		
		item.update();
		submission.update();
		context.commit();
		
	}
	
	/**
	 * Process the describe item page. This step is processed in four parts. 
	 * First, all metadata displayed on the page is removed from the item. 
	 * Second, metadata entered by the user is re-entered into the item. Third, 
	 * all fields are checked to see if any required fields are missing. Finaly, 
	 * the item is updated.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace / workflow
	 * @param step The current step
	 * @param request The cocoon request object.
	 * 
	 * @return A comma seperated list of fields in error.
	 */
	public static String processDescribeItem(Context context, String id, int step, Request request) throws SQLException, UIException, ServletException, AuthorizeException
	{
		InProgressSubmission submission = findSubmission(context,id);
		Item item = submission.getItem();

		Collection collection = submission.getCollection();

		initializeInputsReader();
		DCInputSet inputSet = getInputsReader().getInputs(collection.getHandle());
		DCInput[] inputs = inputSet.getPageRows(step-1, submission.hasMultipleTitles(), submission.isPublishedBefore());

		//Step 1: 
		// clear out all item metadata defined on this page
		for (DCInput input : inputs)
		{
			String dcQualifier = input.getQualifier();
			if (dcQualifier == null && input.getInputType().equals("qualdrop_value"))
			{
				dcQualifier = Item.ANY;
			}
			item.clearMetadata(input.getSchema(), input.getElement(), dcQualifier, Item.ANY);
		}

		//Step 2:
		// Update the item for all the metadata on this page.

		for (DCInput input : inputs)
		{
//			String dcSchema = input.getSchema();
//			String dcElement = input.getElement();
			String dcQualifier = input.getQualifier();

			String fieldName = getFieldName(input);
			

			String inputType = input.getInputType();
			if (inputType.equals("name"))
			{
				readNames(request, fieldName, input, item);
			}
			else if (inputType.equals("date"))
			{
				readDate(request, fieldName, input, item);
			}
			else if (inputType.equals("series"))
			{
				readSeriesNumbers(request, fieldName, input, item);
			}
			else if (inputType.equals("twobox"))
			{
				// FIXME: this probably shouldn't be needed
				if ("ispartofseries".equals(dcQualifier))
				{
					// Ispartofseries should be series and not two-boxes.
					readSeriesNumbers(request, fieldName, input, item);
				}
				else
				{
					// We don't have a twobox field, instead it's just a
					// one box field that the theme can render in two columns.
					readText(request, fieldName, input, item);
				}
			}
			else if (inputType.equals("qualdrop_value"))
			{
				readQualdrop(request, fieldName, input, item);
			}
			else if ((inputType.equals("onebox")) || (inputType.equals("textarea")) || inputType.equals("dropdown"))
			{
				readText(request, fieldName, input, item);
			}
			else 
			{
				throw new UIException("Field "+ fieldName + " has an unknown input type: " + inputType);
			}
		}

		//Step 3:
		// Check to see if any fields are missing
		List<String> missing = new ArrayList<String>();
		for (DCInput input : inputs)
		{
			DCValue[] values = 
				item.getMetadata(input.getSchema(), input.getElement(), input.getQualifier(), Item.ANY);
			
			if (input.isRequired() && values.length == 0)
			{
				missing.add(getFieldName(input));
			}
		}
		
		String errors = null;
		for (String fieldName : missing)
		{
			if (errors == null)
				errors = fieldName;
			else
				errors += ","+fieldName;
		}
		

		//Step 4:
		// Save our changes
		item.update();
		context.commit();
		
		return errors;
	}

	/**
	 * Process the file upload step. This step allows the user to 
	 * upload a new file or remove a set of old files. First, all 
	 * selected files are removed (note it does not matter what submit 
	 * button was pressed). Second, any newly uploaded files are added 
	 * to the item. Third, the item is checked to ensure that there 
	 * exists at least one file. If not then an error is generated. 
	 * Finaly, the item is updated.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace / workflow
	 * @param request The cocoon request object.
	 * @return
	 */
	public static String processUpload(Context context, String id, Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		InProgressSubmission submission = findSubmission(context,id);
		Item item = submission.getItem();

		//Step 1:
		// Remove any selected files
		String[] removeIDs = request.getParameterValues("remove");
		if (removeIDs != null && removeIDs.length > 0)
		{
			Bundle[] bundles = item.getBundles("ORIGINAL");
			if (bundles.length > 0)
			{
				Bundle bundle = bundles[0];
				
				for (String removeID : removeIDs)
				{
					Bitstream bitstream = Bitstream.find(context,Integer.valueOf(removeID));
		
		            if (bitstream == null)
			            continue;

		            bundle.removeBitstream(bitstream);
				}
				
				Bitstream[] bitstreams = bundle.getBitstreams();
				
				if (bitstreams.length < 1)
	            {
	                item.removeBundle(bundles[0]);
	                item.update();
	            }
			}
		}
		
		
		//Step 2:
		// Upload a new file
		Object object = request.get("file");
		Part filePart = null;
		if (object instanceof Part)
			filePart = (Part) object;

		if (filePart != null && filePart.getSize() > 0)
		{
			InputStream is = filePart.getInputStream();

			Bitstream bitstream;
			Bundle[] bundles = item.getBundles("ORIGINAL");
			if (bundles.length < 1)
			{
				// set bundle's name to ORIGINAL
				bitstream = item.createSingleBitstream(is, "ORIGINAL");
			}
			else
			{
				// we have a bundle already, just add bitstream
				bitstream = bundles[0].createBitstream(is);
			}

			// Strip all but the last filename. It would be nice
			// to know which OS the file came from.
			String name = filePart.getUploadName();

			while (name.indexOf('/') > -1)
			{
				name = name.substring(name.indexOf('/') + 1);
			}

			while (name.indexOf('\\') > -1)
			{
				name = name.substring(name.indexOf('\\') + 1);
			}

			bitstream.setName(name);
			bitstream.setSource(filePart.getUploadName());
			bitstream.setDescription(request.getParameter("description"));

			// Identify the format
			BitstreamFormat format = FormatIdentifier.guessFormat(context, bitstream);
			bitstream.setFormat(format);

			// Update to DB
			bitstream.update();
			item.update();
		}
		
		//Step 4:
		// Determine if there is an error because no files have been uploaded.
		String errors = "file"; // error indicating no files are present.
		//FIXME: We should check a configuration paramater before making this an error.
		boolean allowEmptyItems = false;
		if (!allowEmptyItems)
		{
			Bundle[] bundles = item.getBundles("ORIGINAL");
			if (bundles.length > 0)
			{
				Bitstream[] bitstreams = bundles[0].getBitstreams();
				if (bitstreams.length > 0)
				{
					// At least one bitstream exists on the item, clear the error.
					errors = null;
				}
			}
		}
			
		//Step 3:
		// Save our changes
		context.commit();
		
		// Return null for no errors;
		return errors;
	}
	
	/**
	 * Prcosses the editing of a file. This step allows a user to 
	 * edit the description or format of a given file. First the 
	 * description is updated and next the format is updated. The 
	 * user may select a format from the list of available formats 
	 * or they may choose to set their own format string if it is 
	 * not available in the format registry.
	 * 
	 * @param context The current DSpace content
	 * @param bitstreamID The unique ID of the bitstream being editited
	 * @param request The cocoon request object.
	 */
	public static void processEditFile(Context context, int bitstreamID, Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		Bitstream bitstream = Bitstream.find(context, bitstreamID);
		BitstreamFormat currentFormat = bitstream.getFormat();

		//Step 1:
		// Update the bitstream's description
		String description = request.getParameter("description");
		if (description != null && description.length() > 0)
		{
			bitstream.setDescription(description);
		}
		
		
		//Step 2:
		// Update the bitstream's format
		String format = request.getParameter("format");
		int formatID = Integer.valueOf(format);
		if (formatID > 0)
		{
			if (currentFormat == null || currentFormat.getID() != formatID)
			{
				BitstreamFormat newFormat = BitstreamFormat.find(context, formatID);
				if (newFormat != null)
				{
					bitstream.setFormat(newFormat);
				}
			}
		}
		else
		{
			String userFormat = request.getParameter("user_format");
			
			if (userFormat != null && userFormat.length() > 0)
			{
				bitstream.setUserFormatDescription(userFormat);
			}
		}
		
		//Step 3:
		// Save our changes
		bitstream.update();
		context.commit();

	}
	
	/**
	 * Process the optional Creative Commons step. There are two possibiliets for 
	 * processing this step. First the user has clicked the remove button, in this 
	 * case the current Creative Commons license is removed from the item. The other 
	 * possibility is that a new license has been seleted by the user, in this case 
	 * the new license is added to the item.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace
	 * @param request The cocoon request object.
	 */
	public static void processCCLicense(Context context, String id, Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		InProgressSubmission submission = findSubmission(context,id);
		Item item = submission.getItem();
		
		if (request.getParameter("submit_ccremove") != null)
		{
			// user want's te remove the creative commons license.
			CreativeCommons.removeLicense(context, item);
		}
		
		String license_url = request.getParameter("license_url");
		if (license_url != null && license_url.length() > 0)
		{
			// Add the new license
			CreativeCommons.setLicense(context, item, license_url);
		}
		
	}
	
	/**
	 * Process the last step of submitting an item. Ensure that the user 
	 * has selected to accept the license, if so add the license to the 
	 * item. If the user did anything other than select to accept the 
	 * license then an error is returned.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace / workflow
	 * @param request The cocoon request object.
	 * @return A comma seperated list of fields in error (in this case just the decision field)
	 */
	public static String processLicense(Context context, String id, Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		InProgressSubmission submission = findSubmission(context,id);
		Item item = submission.getItem();
		String license = submission.getCollection().getLicense();
		
		if (request.getParameter("submit_complete") != null)
		{
			String decision = request.getParameter("decision");
			
			if ("accept".equals(decision))
			{
				// The user has selected the license
				item.licenseGranted(license, context.getCurrentUser());
				item.update();
				context.commit();
				
				// Return no errors, 
				return null;
			}
		}
		
		// If the user did not accept a license return an error until they do.
		return "decision";
	}
	
	/**
	 * This is less of a real step than the others, but when this is 
	 * called the item is completed and should be sent to the workflow 
	 * engine.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workspace
	 * @param request The cocoon request object.
	 * @return The handle of the current collection.
	 */
	public static String processCompleteSubmission(Context context, String id, Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkspaceItem workspaceItem = findWorkspace(context, id);
		String handle = workspaceItem.getCollection().getHandle();
		
		// Start the workflow
        WorkflowManager.start(context, workspaceItem);
        context.commit();
        
        return handle;
	}
	
	
	
	/**
	 * Update the provided workflowItem to advance to the next workflow 
	 * step. If this was the last thing needed before the item is 
	 * committed to the repository then return true, otherwise false.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static boolean processApproveTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
		Item item = workflowItem.getItem();
		
		// Advance the item along the workflow
        WorkflowManager.advance(context, workflowItem, context.getCurrentUser());

        // FIXME: This should be a return value from advance()
        // See if that gave the item a Handle. If it did,
        // the item made it into the archive, so we
        // should display a suitable page.
        String handle = HandleManager.findHandle(context, item);

        context.commit();
        
        if (handle != null)
        {
            return true;
        }
        else
        {
            return false;
        }
	}
	
	
	
	/**
	 * Return the given task back to the pool of unclaimed tasks for another user
	 * to select and preform.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static void processUnclaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
		
        // Return task to pool
        WorkflowManager.unclaim(context, workflowItem, context.getCurrentUser());
        
        context.commit();
	}
	
	/**
	 * Claim this task from the pool of unclaimed task so that this user may
	 * preform the task by either approving or rejecting it.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static void processClaimTask(Context context, String id) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
		
       // Claim the task
       WorkflowManager.claim(context, workflowItem, context.getCurrentUser());
       
       context.commit();
	}
	
	

	/**
	 * Reject the given task for the given reason. If the user did not provide
	 * a reason then an error is generated placing that field in error.
	 * 
	 * @param context The current DSpace content
	 * @param id The unique ID of the current workflow
	 */
	public static String processRejectTask(Context context, String id,Request request) throws SQLException, UIException, ServletException, AuthorizeException, IOException
	{
		WorkflowItem workflowItem = findWorkflow(context, id);
		
		String reason = request.getParameter("reason");

		if (reason != null && reason.length() > 1)
		{
			WorkflowManager.reject(context, workflowItem,context.getCurrentUser(), reason);
			context.commit();
			
			// Return no errors.
			return null;
		}
		else
		{
			// If the user did not supply a reason then
			// place the reason field in error.
			return "reason";
		}
	}
	
	
	
	
	
	
	
	
	
	
	
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
	private static void readNames(Request request, String fieldName, DCInput input, Item item)
	{
		// Names to add
		List<String> lastNames  = getCompositeFieldValues(request, fieldName, fieldName+"_last");
		List<String> firstNames = getCompositeFieldValues(request, fieldName, fieldName+"_first");


		// Put the names in the correct form
		for (int i = 0; i < lastNames.size(); i++)
		{
			String f = (String) firstNames.get(i);
			String l = (String) lastNames.get(i);

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
				item.addMetadata(input.getSchema(), input.getElement(), input.getQualifier(), null, 
						new DCPersonName(l, f).toString());
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
	private static void readText(Request request, String fieldName, DCInput input, Item item)
	{
		// Names to add
		List<String> texts  = getFieldValues(request, fieldName);

		for (String text : texts)
		{
			if (text == null || "".equals(text))
				continue;
			
			item.addMetadata(input.getSchema(),input.getElement(),input.getQualifier(),"en",text);
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
	private static void readDate(Request request, String fieldName, DCInput input, Item item) throws SQLException
	{
		// dates to add
		List<String> years  = getCompositeFieldValues(request, fieldName, fieldName+"_year");
		List<String> months = getCompositeFieldValues(request, fieldName, fieldName+"_month");
		List<String> days = getCompositeFieldValues(request, fieldName, fieldName+"_day");	


		// Put the names in the correct form
		for (int i = 0; i < years.size(); i++)
		{
			DCDate date  = new DCDate();
			int year,month,day;
			
			try {
				year = Integer.valueOf(years.get(i));
			} catch (NumberFormatException nfe) {
				year = 0;
			}
			try {
				month = Integer.valueOf(months.get(i));
			} catch (NumberFormatException nfe) {
				month = 0;
			}
			try {
				day = Integer.valueOf(days.get(i));
			} catch (NumberFormatException nfe) {
				day = 0;
			}

			if (year == 0)
				continue;
			
			date.setDateLocal(year,month,day, -1, -1, -1);
			
			item.addMetadata(input.getSchema(),input.getElement(),input.getQualifier(), null, date.toString());
		}
	}

	/**
	 * 
	 */
	private static void readQualdrop(Request request, String fieldName, DCInput input, Item item) throws SQLException
	{
		// dates to add
		List<String> qualifiers = getCompositeFieldValues(request, fieldName, fieldName+"_qualifier");
		List<String> values     = getCompositeFieldValues(request, fieldName, fieldName+"_value");	

		for (int i = 0; i < qualifiers.size(); i++)
		{
			String qualifier = qualifiers.get(i);
			String value = values.get(i);
			
			if ("".equals(qualifier))
				qualifier = null;

			
			if (value == null || "".equals(value))
				continue;
			
			item.addMetadata(input.getSchema(),input.getElement(),qualifier, null,value);
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
	private static void readSeriesNumbers(Request request, String fieldName, DCInput input, Item item)
	{
		// dates to add
		List<String> series  = getCompositeFieldValues(request, fieldName, fieldName+"_series");
		List<String> numbers = getCompositeFieldValues(request, fieldName, fieldName+"_number");

		// Put the names in the correct form
		for (int i = 0; i < series.size(); i++)
		{
			String s = series.get(i);
			String n = numbers.get(i);
			
			if ((s == null || "".equals(s)) && (n == null || "".equals(n)))
				continue;
			
			DCSeriesNumber seriesNumber = new DCSeriesNumber(s,n);

			item.addMetadata(input.getSchema(), input.getElement(), input.getQualifier(), null, seriesNumber.toString());
		}
	}

	
	/**
	 * Return the HTML / DRI field name for the given input.
	 * 
	 * @param input
	 * @return
	 */
	public static String getFieldName(DCInput input)
	{
		String dcSchema = input.getSchema();
		String dcElement = input.getElement();
		String dcQualifier = input.getQualifier();
		if (dcQualifier != null && ! dcQualifier.equals(Item.ANY))
		{
			return dcSchema + "_" + dcElement + '_' + dcQualifier;
		}
		else
		{
			return dcSchema + "_" + dcElement;
		}

	}
	
	/**
	 * Get values for a field from a form, since a field may have multiple 
	 * values this method will check for fieldName + "_n" untill it does not 
	 * find any more values. The result is a list of all values for a field.
	 * 
	 * If the value has been seleted to be removed then it is removed from 
	 * the list.
	 * 
	 * @param request
	 *            the request containing the form information
	 * @param compositeFieldName
	 * 			The fieldName of the composite field. 
	 * @param componentFieldName
	 * 			The fieldName of the component field
	 * @return a List of Strings
	 */
	private static List<String> getCompositeFieldValues(Request request, String compositeFieldName, String componentFieldName)
	{
		List<String> values = new ArrayList<String>();

		int i = -1;

		// Iterate through the values in the form.
		valueLoop: while (1 == 1)
		{
			i++;
			String value = null;

			// Get the component field's name
			if (i == 0)
				value = request.getParameter(componentFieldName);
			else
				value = request.getParameter(componentFieldName + "_" + i);

			// If this is null then it's the last one.
			if (value == null)
			{
				break valueLoop;
			}


			// Check to make sure that this value is not selected to be removed.
			String[] selected = request.getParameterValues(compositeFieldName + "_selected");

			if (selected != null)
			{
				for (String select : selected)
				{
					if (select.equals(compositeFieldName + "_" + i))
					{
						// Found, the user has requested that this value be deleted.
						continue valueLoop;
					}
				}
			}

			// Finaly add it to the list
			values.add(value.trim());
		}

		return values;
	}


	/**
	 * Get values for a field from a form, since a field may have multiple 
	 * values this method will check for fieldName + "_n" untill it does not 
	 * find any more values. The result is a list of all values for a field.
	 * 
	 * If the value has been seleted to be removed then it is removed from 
	 * the list.
	 * 
	 * @param request
	 *            the request containing the form information
	 * @param fieldName
	 * 			The fieldName of the composite field. 
	 * @return a List of Strings
	 */
	private static List<String> getFieldValues(Request request, String fieldName)
	{
		List<String> values = new ArrayList<String>();

		int i = -1;

		// Iterate through the values in the form.
		valueLoop: while (1 == 1)
		{
			i++;
			String value = null;

			// Get the component field's name
			if (i == 0)
				value = request.getParameter(fieldName);
			else
				value = request.getParameter(fieldName + "_" + i);

			// If this is null then it's the last one.
			if (value == null)
			{
				break valueLoop;
			}


			// Check to make sure that this value is not selected to be removed.
			String[] selected = request.getParameterValues(fieldName + "_selected");

			if (selected != null)
			{
				for (String select : selected)
				{
					if (select.equals(fieldName + "_" + i))
					{
						// Found, the user has requested that this value be deleted.
						continue valueLoop;
					}
				}
			}

			// Finaly add it to the list
			values.add(value.trim());
		}

		return values;
	}
}
