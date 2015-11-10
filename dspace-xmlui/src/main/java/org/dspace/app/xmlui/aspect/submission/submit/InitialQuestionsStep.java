/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.xml.sax.SAXException;

/**
 * This is the first official step of the item submission processes. This
 * step will ask the user two questions which will direct whether future
 * questions will be asked of the user. Since they user may move next or
 * previous or even jump around between the stages some metadata values
 * may already be filled for selected values. i.e. if the user selected
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

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

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
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

		java.util.List<MetadataValue> titles = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", "alternative", Item.ANY);
		
        java.util.List<MetadataValue> dateIssued = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
        java.util.List<MetadataValue> citation = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "identifier", "citation", Item.ANY);
        java.util.List<MetadataValue> publisher = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "publisher", null, Item.ANY);
    	
    	
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
        if (titles.size() > 0)
        {
	        org.dspace.app.xmlui.wing.element.Item note = form.addItem();
	        note.addHighlight("bold").addContent(T_important_note);
	        note.addContent(T_multiple_titles_note);
	        for (int i=0; i< titles.size(); i++)
	        {
	        	if (i > 0)
                {
                    note.addContent(T_separator);
                }
	        	note.addContent("\"");
	        	note.addHighlight("bold").addContent(titles.get(i).getValue());
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
        if (dateIssued.size() > 0 || citation.size() > 0 || publisher.size() > 0)
        {
        	org.dspace.app.xmlui.wing.element.Item note = form.addItem();
	        note.addHighlight("bold").addContent(T_important_note);
	        note.addContent(T_published_before_note);
	        
	        // Start a convoluted processes to build an english list of values.
	        // Format: Date Issued (value, value, value), Citation (value, value, value), Publisher (value, value, value)
	        if (dateIssued.size() > 0)
	        {
	        	note.addHighlight("bold").addContent(T_date_issued);
	        	note.addContent(T_open);
	        	for (int i=0; i< dateIssued.size(); i++)
	        	{
	        		if (i > 0)
                    {
                        note.addContent(T_separator);
                    }
	        		note.addContent(dateIssued.get(i).getValue());
	        	}
	        	note.addContent(T_close);
	        }
	        
	        // Conjunction
	        if (dateIssued.size() > 0 && (citation.size() > 0 || publisher.size() > 0))
            {
                note.addContent(T_separator);
            }
	        
	        if (dateIssued.size() > 0 && citation.size() > 0 && publisher.size() == 0)
	        {
	        	note.addContent(T_and);
	        }
	        
	        // Citation
	        if (citation.size() > 0)
	        {
	        	note.addHighlight("bold").addContent(T_citation);
	        	note.addContent(T_open);
	        	for (int i=0; i< citation.size(); i++)
	        	{
	        		if (i > 0)
                    {
                        note.addContent(T_separator);
                    }
	        		note.addContent(citation.get(i).getValue());
	        	}
	        	note.addContent(T_close);
	        }
	        
	        
	        // Conjunction
	        if (citation.size() > 0 && publisher.size() > 0)
	        {
	        	note.addContent(T_separator);
	        }
	        
	        if ((dateIssued.size() > 0 || citation.size() > 0) && publisher.size() > 0)
	        {
	        	note.addContent(T_and);
	        }
	        
	        
	        // Publisher
	        if (publisher.size() > 0)
	        {
	        	note.addHighlight("bold").addContent(T_publisher);
	        	note.addContent(T_open);
	        	for (int i=0; i< publisher.size(); i++)
	        	{
	        		if (i > 0)
                    {
                        note.addContent(T_separator);
                    }
	        		note.addContent(publisher.get(i).getValue());
	        	}
	        	note.addContent(T_close);
	        }
        }
        
        //add standard control/paging buttons
        addControlButtons(form);
        
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
        {
            multipleTitles = ReviewStep.T_yes;
        }
    
        Message publishedBefore = ReviewStep.T_no;
        if (submission.isPublishedBefore())
        {
            publishedBefore = ReviewStep.T_yes;
        }
        
        initSection.addLabel(T_multiple_titles);
        initSection.addItem(multipleTitles);
        initSection.addLabel(T_published_before);
        initSection.addItem(publishedBefore);
        
        //return this new review section
        return initSection;
    }
}
