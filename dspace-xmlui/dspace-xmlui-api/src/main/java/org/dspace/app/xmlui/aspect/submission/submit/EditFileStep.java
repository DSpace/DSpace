/*
 * EditFileStep.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
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
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.xml.sax.SAXException;

/**
 * This is a sub step of the Upload step during item submission. This 
 * page allows the user to edit metadata about a bitstream (aka file) 
 * that has been uploaded. The user can change the format or change 
 * the file's description.
 * <P>
 * Since this page is a sub step, the normal control actions are not
 * present, the user only has the option of returning back to the 
 * upload step.
 * <P>
 * NOTE: As a sub step, it is called directly from the UploadStep class.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class EditFileStep extends AbstractStep
{
	
	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.EditFileStep.head");
    protected static final Message T_file = 
        message("xmlui.Submission.submit.EditFileStep.file");
    protected static final Message T_description = 
        message("xmlui.Submission.submit.EditFileStep.description");
    protected static final Message T_description_help = 
        message("xmlui.Submission.submit.EditFileStep.description_help");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.EditFileStep.info1");
    protected static final Message T_format_detected = 
        message("xmlui.Submission.submit.EditFileStep.format_detected");
    protected static final Message T_format_selected = 
        message("xmlui.Submission.submit.EditFileStep.format_selected");
    protected static final Message T_format_default = 
        message("xmlui.Submission.submit.EditFileStep.format_default");
    protected static final Message T_info2 = 
        message("xmlui.Submission.submit.EditFileStep.info2");
    protected static final Message T_format_user = 
        message("xmlui.Submission.submit.EditFileStep.format_user");
    protected static final Message T_format_user_help = 
        message("xmlui.Submission.submit.EditFileStep.format_user_help");
    protected static final Message T_submit_save = 
        message("xmlui.general.save");
    protected static final Message T_submit_cancel = 
        message("xmlui.general.cancel");

    /** The bitstream we are editing */
	private Bitstream bitstream;

	
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public EditFileStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
	
	
	/**
	 * Get the bitstream we are editing
	 */
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
	throws ProcessingException, SAXException, IOException
	{ 
		super.setup(resolver,objectModel,src,parameters);
		
		//the bitstream should be stored in our Submission Info object
        this.bitstream = submissionInfo.getBitstream();
	}

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit";
		
    	// Get the bitstream and all the various formats
		BitstreamFormat currentFormat = bitstream.getFormat();
        BitstreamFormat guessedFormat = FormatIdentifier.guessFormat(context, bitstream);
    	BitstreamFormat[] bitstreamFormats = BitstreamFormat.findNonInternal(context);
    	
        int itemID = submissionInfo.getSubmissionItem().getItem().getID();
    	String fileUrl = contextPath + "/bitstream/item/" + itemID + "/" + bitstream.getName();
    	String fileName = bitstream.getName();
    	
    	// Build the form that describes an item.
    	Division div = body.addInteractiveDivision("submit-edit-file", actionURL, Division.METHOD_POST, "primary submission");
    	div.setHead(T_submission_head);
    	addSubmissionProgressList(div);
    	
    	List edit = div.addList("submit-edit-file", List.TYPE_FORM);
        edit.setHead(T_head);    
        
        edit.addLabel(T_file);
        edit.addItem().addXref(fileUrl, fileName);
        
        Text description = edit.addItem().addText("description");
        description.setLabel(T_description);
        description.setHelp(T_description_help);
        description.setValue(bitstream.getDescription());
        
        edit.addItem(T_info1);
        if (guessedFormat != null)
        {
        	edit.addLabel(T_format_detected);
        	edit.addItem(guessedFormat.getShortDescription());
        }
        
        // System supported formats
        Select format = edit.addItem().addSelect("format");
        format.setLabel(T_format_selected);
        
        format.addOption(-1,T_format_default);
        for (BitstreamFormat bitstreamFormat : bitstreamFormats)
        {
        	String supportLevel = "Unknown";
        	if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
        		supportLevel = "known";
        	else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
        		supportLevel = "Supported";
        	String name = bitstreamFormat.getShortDescription()+" ("+supportLevel+")";
        	int id = bitstreamFormat.getID();
       
        	format.addOption(id,name);
        }
        if (currentFormat != null)
        {
        	format.setOptionSelected(currentFormat.getID());
        }
        else if (guessedFormat != null)
        {
        	format.setOptionSelected(guessedFormat.getID());
        }
        else
        {
        	format.setOptionSelected(-1);
        }
        
        edit.addItem(T_info2);
        
        // User supplied format
        Text userFormat = edit.addItem().addText("format_description");
        userFormat.setLabel(T_format_user);
        userFormat.setHelp(T_format_user_help);
        userFormat.setValue(bitstream.getUserFormatDescription());
        
        // add ID of bitstream we're editing
        div.addHidden("bitstream_id").setValue(bitstream.getID()); 
        
        // Note, not standard control actions, this page just goes back to the upload step.
        org.dspace.app.xmlui.wing.element.Item actions = edit.addItem();
        actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_edit_cancel").setValue(T_submit_cancel);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 
        
    }
    
}
