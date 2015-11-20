/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.time.DateFormatUtils;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;


public class EditPolicyStep extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head =message("xmlui.Submission.submit.EditPolicyStep.head");
    protected static final Message T_submit_save = message("xmlui.general.save");
    protected static final Message T_submit_cancel =message("xmlui.general.cancel");

	private ResourcePolicy resourcePolicy;
    private Bitstream bitstream;


	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public EditPolicyStep()
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
		this.resourcePolicy = (ResourcePolicy) submissionInfo.get(org.dspace.submit.step.AccessStep.SUB_INFO_SELECTED_RP);
        this.bitstream=submissionInfo.getBitstream();
	}

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException{

        Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Division div = body.addInteractiveDivision("submit-edit-policy", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);
        List edit = div.addList("submit-edit-file", List.TYPE_FORM);
        edit.setHead(T_head);

        div.addHidden("policy_id").setValue(resourcePolicy.getID());

        // if come from EditBitstreamPolicies
        if(bitstream!=null)
            div.addHidden("bitstream_id").setValue(bitstream.getID().toString());

        AccessStepUtil asu = new AccessStepUtil(context);

        asu.addListGroups(String.valueOf(resourcePolicy.getGroup()), edit, errorFlag, collection);

        // radio buttons: Item will be visible / Embargo Access + date
        String selectedRadio=Integer.toString(AccessStep.RADIO_OPEN_ACCESS_ITEM_VISIBLE);
        if(resourcePolicy.getStartDate()!=null)
            selectedRadio = Integer.toString(AccessStep.RADIO_OPEN_ACCESS_ITEM_EMBARGOED);

        // this step is possible only in case of AdvancedForm
        String dateValue = "";
        if(resourcePolicy.getStartDate() != null){
            dateValue = DateFormatUtils.format(resourcePolicy.getStartDate(), "yyyy-MM-dd");
        }
        asu.addAccessRadios(selectedRadio, dateValue, edit, errorFlag, null);

	    asu.addName(resourcePolicy.getRpName(), edit, errorFlag);

        // Reason
        asu.addReason(resourcePolicy.getRpDescription(), edit, errorFlag);

        // Note, not standard control actions, this page just goes back to the upload step.
        org.dspace.app.xmlui.wing.element.Item actions = edit.addItem();
        actions.addButton(org.dspace.submit.step.AccessStep.FORM_EDIT_BUTTON_SAVE).setValue(T_submit_save);
		actions.addButton(org.dspace.submit.step.AccessStep.FORM_EDIT_BUTTON_CANCEL).setValue(T_submit_cancel);
    }
}
