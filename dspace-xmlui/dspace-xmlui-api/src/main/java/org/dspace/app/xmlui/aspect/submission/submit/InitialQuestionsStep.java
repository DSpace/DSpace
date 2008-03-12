/*
 * InitialQuestionsStep.java
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

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * This is the first official step of the item submission processes. This
 * step will ask the user two questions which will direct whether future
 * questions will be asked of the user. Since they user may move next or
 * previous or even jump around between the stages some metadata values
 * may all ready be filled for selected values. i.e. if the user selected
 * that there may be multiple titles and then later comes back and unchecks
 * then multiple titles box. In this case these metadata entries are removed
 * from the item, a note is displayed informing the user of this discrepency.
 * 
 * Questions:
 *  - Multiple titles
 *  - Published Before
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class InitialQuestionsStep extends AbstractSubmissionStep
{
	/** Language Strings **/
    protected static final Message T_head= 
        message("xmlui.Submission.submit.InitialQuestionsStep.head");
    protected static final Message T_important_note= 
        message("xmlui.Submission.submit.InitialQuestionsStep.important_note");
    protected static final Message T_and= 
        message("xmlui.Submission.submit.InitialQuestionsStep.and");
    protected static final Message T_separator = 
        message("xmlui.Submission.submit.InitialQuestionsStep.separator");
    protected static final Message T_open= 
        message("xmlui.Submission.submit.InitialQuestionsStep.open_set");
    protected static final Message T_close= 
        message("xmlui.Submission.submit.InitialQuestionsStep.close_set");
    protected static final Message T_multiple_titles= 
        message("xmlui.Submission.submit.InitialQuestionsStep.multiple_titles");
    protected static final Message T_multiple_titles_help= 
        message("xmlui.Submission.submit.InitialQuestionsStep.multiple_titles_help");
    protected static final Message T_multiple_titles_note= 
        message("xmlui.Submission.submit.InitialQuestionsStep.multiple_titles_note");
    protected static final Message T_published_before= 
        message("xmlui.Submission.submit.InitialQuestionsStep.published_before");
    protected static final Message T_published_before_help= 
        message("xmlui.Submission.submit.InitialQuestionsStep.published_before_help");
    protected static final Message T_published_before_note= 
        message("xmlui.Submission.submit.InitialQuestionsStep.published_before_note");
    protected static final Message T_date_issued= 
        message("xmlui.Submission.submit.InitialQuestionsStep.date_issued");
    protected static final Message T_citation= 
        message("xmlui.Submission.submit.InitialQuestionsStep.citation");
    protected static final Message T_publisher= 
        message("xmlui.Submission.submit.InitialQuestionsStep.publisher");

    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public InitialQuestionsStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
    
    
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	// Get any metadata that may be removed by unselecting one of these options.
    	Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit";
		
		DCValue[] titles = item.getDC("title", "alternative", Item.ANY);
		
		DCValue[] dateIssued = item.getDC("date", "issued", Item.ANY);
        DCValue[] citation = item.getDC("identifier", "citation", Item.ANY);
        DCValue[] publisher = item.getDC("publisher", null, Item.ANY);
    	
    	
        // Generate a from asking the user two questions: multiple 
        // titles & published before.
    	Division div = body.addInteractiveDivision("submit-initial-questions", actionURL, Division.METHOD_POST, "primary submission");
    	div.setHead(T_submission_head);
    	addSubmissionProgressList(div);
    	
    	List form = div.addList("submit-initial-questions", List.TYPE_FORM);
        form.setHead(T_head);    
        
        CheckBox multipleTitles = form.addItem().addCheckBox("multiple_titles");
        multipleTitles.setLabel(T_multiple_titles);
        multipleTitles.setHelp(T_multiple_titles_help);
        multipleTitles.addOption("true");
        if (submission.hasMultipleTitles())
        {
        	multipleTitles.setOptionSelected("true");
        }
        
        // If any titles would be removed if the user unselected this box then 
        // warn the user!
        if (titles.length > 0)
        {
	        org.dspace.app.xmlui.wing.element.Item note = form.addItem();
	        note.addHighlight("bold").addContent(T_important_note);
	        note.addContent(T_multiple_titles_note);
	        for (int i=0; i< titles.length; i++)
	        {
	        	if (i > 0)
	        		note.addContent(T_separator);
	        	note.addContent("\"");
	        	note.addHighlight("bold").addContent(titles[i].value);
	        	note.addContent("\"");
	        }
        }
        
        CheckBox publishedBefore = form.addItem().addCheckBox("published_before");
        publishedBefore.setLabel(T_published_before);
        publishedBefore.setHelp(T_published_before_help);
        publishedBefore.addOption("true");
        if (submission.isPublishedBefore())
        {
        	publishedBefore.setOptionSelected("true");
        }
        
        // If any metadata would be removed if the user unselected the box then warn 
        // the user about what will be removed.
        if (dateIssued.length > 0 || citation.length > 0 || publisher.length > 0)
        {
        	org.dspace.app.xmlui.wing.element.Item note = form.addItem();
	        note.addHighlight("bold").addContent(T_important_note);
	        note.addContent(T_published_before_note);
	        
	        // Start a convoluted processes to build an english list of values.
	        // Format: Date Issued (value, value, value), Citation (value, value, value), Publisher (value, value, value)
	        if (dateIssued.length > 0)
	        {
	        	note.addHighlight("bold").addContent(T_date_issued);
	        	note.addContent(T_open);
	        	for (int i=0; i< dateIssued.length; i++)
	        	{
	        		if (i > 0)
		        		note.addContent(T_separator);
	        		note.addContent(dateIssued[i].value);
	        	}
	        	note.addContent(T_close);
	        }
	        
	        // Conjunction
	        if (dateIssued.length > 0 && (citation.length > 0 || publisher.length > 0))
	        	note.addContent(T_separator);
	        
	        if (dateIssued.length > 0 && citation.length > 0 && publisher.length == 0)
	        {
	        	note.addContent(T_and);
	        }
	        
	        // Citation
	        if (citation.length > 0)
	        {
	        	note.addHighlight("bold").addContent(T_citation);
	        	note.addContent(T_open);
	        	for (int i=0; i< citation.length; i++)
	        	{
	        		if (i > 0)
		        		note.addContent(T_separator);
	        		note.addContent(citation[i].value);
	        	}
	        	note.addContent(T_close);
	        }
	        
	        
	        // Conjunction
	        if (citation.length > 0 && publisher.length > 0)
	        {
	        	note.addContent(T_separator);
	        }
	        
	        if ((dateIssued.length > 0 || citation.length > 0) && publisher.length > 0)
	        {
	        	note.addContent(T_and);
	        }
	        
	        
	        // Publisher
	        if (publisher.length > 0)
	        {
	        	note.addHighlight("bold").addContent(T_publisher);
	        	note.addContent(T_open);
	        	for (int i=0; i< publisher.length; i++)
	        	{
	        		if (i > 0)
		        		note.addContent(T_separator);
	        		note.addContent(publisher[i].value);
	        	}
	        	note.addContent(T_close);
	        }
        }
        
        //add standard control/paging buttons
        addControlButtons(form);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 
        //Since we already warn users about the metadata pruning to happen
        //if they uncheck an already checked box, then
        //we can let the prune process occur immediately!
        div.addHidden("prune").setValue("true");
        
    }
    
    /** 
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in 
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().   This sublist is
     * the list you return from this method!
     * 
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return 
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!   
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //Create a new section for this Initial Questions information
        List initSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
        initSection.setHead(T_head);
        
        //add information to review
        Message multipleTitles = ReviewStep.T_no;
        if (submission.hasMultipleTitles())
            multipleTitles = ReviewStep.T_yes;
    
        Message publishedBefore = ReviewStep.T_no;
        if (submission.isPublishedBefore())
            publishedBefore = ReviewStep.T_yes;
        
        initSection.addLabel(T_multiple_titles);
        initSection.addItem(multipleTitles);
        initSection.addLabel(T_published_before);
        initSection.addItem(publishedBefore);
        
        //return this new review section
        return initSection;
    }
}
