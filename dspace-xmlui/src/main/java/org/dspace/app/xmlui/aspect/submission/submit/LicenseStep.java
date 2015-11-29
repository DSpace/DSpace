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

import org.apache.log4j.Logger;
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
import org.dspace.content.LicenseUtils;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

/**
 * This is the last step of the item submission processes. During this
 * step the user must agree to the collection's standard distribution
 * license. If the user can not agree to the license they they may either
 * save the submission until a later time or remove the submission completely.
 * 
 * This step will include the full license text inside the page using the
 * HTML fragment method.
 * 
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class LicenseStep extends AbstractSubmissionStep
{
    private static final Logger log = Logger.getLogger(LicenseStep.class);
    
    /** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.LicenseStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.LicenseStep.info1");
    protected static final Message T_info2 = 
        message("xmlui.Submission.submit.LicenseStep.info2");
    protected static final Message T_info3 = 
        message("xmlui.Submission.submit.LicenseStep.info3");
    protected static final Message T_decision_label = 
        message("xmlui.Submission.submit.LicenseStep.decision_label");
    protected static final Message T_decision_checkbox = 
        message("xmlui.Submission.submit.LicenseStep.decision_checkbox");
    protected static final Message T_decision_error = 
        message("xmlui.Submission.submit.LicenseStep.decision_error");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.LicenseStep.submit_remove");
    protected static final Message T_submit_complete = 
        message("xmlui.Submission.submit.LicenseStep.submit_complete");
	

    
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public LicenseStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}

    
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{

		// Get the full text for the actual licese
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
		String licenseText = LicenseUtils.getLicenseText(context
                .getCurrentLocale(), collection, submission.getItem(),
                submission.getSubmitter());
		
		Division div = body.addInteractiveDivision("submit-license",actionURL, Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);
		
		Division inner = div.addDivision("submit-license-inner");
		inner.setHead(T_head);
		inner.addPara(T_info1);
		inner.addPara(T_info2);
		
		
		// Add the actual text of the license:
		Division displayLicense = inner.addDivision("submit-license-standard-text","license-text");
		displayLicense.addSimpleHTMLFragment(true, licenseText);
		
		inner.addPara(T_info3);
		
		List controls = inner.addList("submit-review", List.TYPE_FORM);
		
		CheckBox decision = controls.addItem().addCheckBox("decision");
		decision.setLabel(T_decision_label);
		decision.addOption("accept",T_decision_checkbox);

		// If user did not check the "I accept" checkbox 
		if(this.errorFlag==org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED)
		{
			log.info(LogManager.getHeader(context, "reject_license", submissionInfo.getSubmissionLogInfo()));
			
			decision.addError(T_decision_error);
		}
		
		// add standard control/paging buttons
		addControlButtons(controls);
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
        // License step doesn't require reviewing
        return null;
    }
}
