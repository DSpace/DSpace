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
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.xml.sax.SAXException;

/**
 * This is sort-of a step of the submission processes (not
 * an official "step", since it does not extend AbstractSubmissionStep). 
 * <P>
 * At any time during submission the user may leave the processe and either
 * leave it for later or remove the submission.
 * <P>
 * This form presents three options, 1) Go back, 2) save the work, 
 * or 3) remove it.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (small updates for Configurable Submission)
 */
public class SaveOrRemoveStep extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.info1");
    protected static final Message T_submit_back = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.submit_back");
    protected static final Message T_submit_save = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.submit_save");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.submit_remove");
	
    
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public SaveOrRemoveStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
    
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{	
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

		Division div = body.addInteractiveDivision("submit-save-or-cancel",actionURL, Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);
		
		List saveOrCancel = div.addList("submit-review", List.TYPE_FORM);
	
		saveOrCancel.setHead(T_head);
		saveOrCancel.addItem(T_info1);
		
        org.dspace.app.xmlui.wing.element.Item actions = saveOrCancel.addItem();
        actions.addButton("submit_back").setValue(T_submit_back);
        actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_remove").setValue(T_submit_remove);
	}
}
