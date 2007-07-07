/*
 * ReviewStep.java
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

import javax.servlet.ServletException;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * This is a step of the item submission processes. This is where the user
 * reviews everything they have entered about the item up to this point, all
 * the metadata & files uploaded. The next step after this is to decide upon
 * licensing.
 * 
 * This step builds a form with four parts:
 * 
 * Part A: Review of initial questions
 * Part B: Review of each describe section
 * Part C: Files uploaded.
 * Part D: Standard control actions
 * 
 * There may be multilpe sections for part B.
 * 
 * @author Scott Phillips
 */
public class ReviewStep extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.ReviewStep.head");
    protected static final Message T_yes = 
        message("xmlui.Submission.submit.ReviewStep.yes");
    protected static final Message T_no = 
        message("xmlui.Submission.submit.ReviewStep.no");
    protected static final Message T_submit_jump = 
        message("xmlui.Submission.submit.ReviewStep.submit_jump");
    protected static final Message T_submit_jump_files = 
        message("xmlui.Submission.submit.ReviewStep.submit_jump_files");
    protected static final Message T_head1 = 
        message("xmlui.Submission.submit.ReviewStep.head1");
    protected static final Message T_head2 = 
        message("xmlui.Submission.submit.ReviewStep.head2");
    protected static final Message T_head3 = 
        message("xmlui.Submission.submit.ReviewStep.head3");
    protected static final Message T_multiple_titles = 
        message("xmlui.Submission.submit.ReviewStep.multiple_titles");
    protected static final Message T_published_before = 
        message("xmlui.Submission.submit.ReviewStep.published_before");
    protected static final Message T_no_metadata = 
        message("xmlui.Submission.submit.ReviewStep.no_metadata");
    protected static final Message T_unknown = 
        message("xmlui.Submission.submit.ReviewStep.unknown");
    protected static final Message T_known = 
        message("xmlui.Submission.submit.ReviewStep.known");
    protected static final Message T_supported = 
        message("xmlui.Submission.submit.ReviewStep.supported");


	/**
	 * The scope for the inputs set, it may hide some fields depending 
	 * upon it's configuration
	 */
	private static String SCOPE = "submit";

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public ReviewStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
	
	
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
		// Get the item, bitstreams and basic inputs set
		Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit";
		
		Bundle[] bundles = item.getBundles("ORIGINAL");
		Bitstream[] bitstreams = new Bitstream[0];
		if (bundles.length > 0)
		{
			bitstreams = bundles[0].getBitstreams();
		}
		DCInputSet inputSet = null;
		try 
		{
			inputSet = FlowUtils.getInputsReader().getInputs(submission.getCollection().getHandle());
		} 
		catch (ServletException se) 
		{
			throw new UIException(se);
		}
		int numberOfDescribePages = inputSet.getNumberPages();



		// Build the three part form
		Division div = body.addInteractiveDivision("submit-upload", actionURL, Division.METHOD_MULTIPART, "primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);
		
		List review = div.addList("submit-review", List.TYPE_FORM);
		review.setHead(T_head);   
		
		// Part A:
		//  Initial Questions
		List initial = review.addList("submit-review-initial",List.TYPE_FORM);
		initial.setHead(T_head1);
		
		Message multipleTitles = T_no;
		if (submission.hasMultipleTitles())
			multipleTitles = T_yes;
	
		Message publishedBefore = T_no;
		if (submission.isPublishedBefore())
			publishedBefore = T_yes;
		
		initial.addLabel(T_multiple_titles);
		initial.addItem(multipleTitles);
		initial.addLabel(T_published_before);
		initial.addItem(publishedBefore);
		
		Button jumpInitial = initial.addItem().addButton("submit_jump_0");
		jumpInitial.setValue(T_submit_jump);
		

		// Part B:
		//  Describe Pages
		for ( int i = 0; i < numberOfDescribePages; i++ )
		{
			DCInput[] inputs = inputSet.getPageRows(i, submission.hasMultipleTitles(), submission.isPublishedBefore());

			List describe = review.addList("submit-review-describe-"+i,List.TYPE_FORM);
			describe.setHead(T_head2.parameterize(i+1));

			for (DCInput input : inputs)
			{
				if (!input.isVisible(SCOPE))
					continue;

				String inputType = input.getInputType();
				String pairsName = input.getPairsType();
				DCValue[] values = new DCValue[0];

				if (inputType.equals("qualdrop_value"))
				{
					values = item.getMetadata(input.getSchema(), input.getElement(), Item.ANY, Item.ANY);
				}
				else
				{
					values = item.getMetadata(input.getSchema(), input.getElement(), input.getQualifier(), Item.ANY);
				}

				if (values.length == 0) 
				{
					describe.addLabel(input.getLabel());
					describe.addItem().addHighlight("italic").addContent(T_no_metadata);
				}
				else 
				{
					for (DCValue value : values)
					{
						String displayValue = null;
						if (inputType.equals("date"))
						{
							DCDate date = new DCDate(value.value);
							displayValue = date.toString();
						}
						else if (inputType.equals("dropdown"))
						{
							displayValue = input.getDisplayString(pairsName,value.value);
						}
						else if (inputType.equals("qualdrop_value"))
						{
							String qualifier = value.qualifier;
							String displayQual = input.getDisplayString(pairsName,qualifier);
							displayValue = displayQual + ":" + value.value;
						}
						else 
						{
							displayValue = value.value;
						}
						describe.addLabel(input.getLabel());
						describe.addItem(displayValue);
					} // For each DCValue
				} // If values exist
			}// For each input
			
			Button jumpDescribe = describe.addItem().addButton("submit_jump_"+(i+1));
			jumpDescribe.setValue(T_submit_jump);
		} // For the number of describe pages

		
		// Part C:
		//  Files uploaded
		List files = review.addList("submit-review-files",List.TYPE_FORM);
		files.setHead(T_head3);
		
		for (Bitstream bitstream : bitstreams)
		{
			BitstreamFormat bitstreamFormat = bitstream.getFormat();
			
			int id = bitstream.getID();
			String name = bitstream.getName();
			String url = contextPath+"/retrieve/"+id+"/"+name;
			String format = bitstreamFormat.getShortDescription();
			Message support = T_unknown;
			if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
				support = T_known;
			else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
				support = T_supported;
			
			org.dspace.app.xmlui.wing.element.Item file = files.addItem();
			file.addXref(url,name);
			file.addContent(" - "+format + " ( ");
			file.addContent(support);
			file.addContent(")");
			
		}
		
		Button jumpDescribe = files.addItem().addButton("submit_jump_"+(numberOfDescribePages+1));
		jumpDescribe.setValue(T_submit_jump_files);
		
		// Part D:
		//  Standard control actions.
		org.dspace.app.xmlui.wing.element.Item actions = review.addItem();
		actions.addButton("submit_previous").setValue(T_previous);
		actions.addButton("submit_save").setValue(T_save);
		actions.addButton("submit_next").setValue(T_next);

		div.addHidden("submission-continue").setValue(knot.getId()); 

	}


	/**
	 * Recycle
	 */
	public void recycle() 
	{
		super.recycle();
	}
}
