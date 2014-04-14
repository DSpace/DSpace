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
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This is a confirmation page informing the user that they have 
 * completed the submission of the item. It tells them what to 
 * expect next, i.e. the workflow, and gives the option to go home 
 * or start another submission.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class CompletedStep extends AbstractSubmissionStep
{

	/** Language Strings **/ 
	protected static final Message T_head = 
        message("xmlui.Submission.submit.CompletedStep.head"); 
	protected static final Message T_info1 = 
        message("xmlui.Submission.submit.CompletedStep.info1"); 
    protected static final Message T_go_submission = 
        message("xmlui.Submission.submit.CompletedStep.go_submission");
	protected static final Message T_submit_again = 
        message("xmlui.Submission.submit.CompletedStep.submit_again"); 

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public CompletedStep()
	{
		this.requireHandle = true;
	}

	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{	
		Division div = body.addInteractiveDivision("submit-complete",contextPath+"/handle/"+handle+"/submit", Division.METHOD_POST,"primary submission");
		div.setHead(T_head);
		
		div.addPara(T_info1);
		
		div.addPara().addXref(contextPath+"/submissions",T_go_submission);
	     
	    div.addPara().addButton("submit_again").setValue(T_submit_again);
	    div.addHidden("handle").setValue(handle);
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
        //nothing to review, since submission is now Completed!
        return null;
    }
}
