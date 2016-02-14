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
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class EditBitstreamPolicies extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head =message("xmlui.Submission.submit.EditBitstreamPolicies.head");
    protected static final Message T_submit_save = message("xmlui.general.save");

    protected static final Message T_submit_add_policy = message("xmlui.Submission.submit.AccessStep.submit_add_policy");

    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public EditBitstreamPolicies()
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
	}


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException{

        Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Request request = ObjectModelHelper.getRequest(objectModel);

        Division div = body.addInteractiveDivision("submit-edit-bitstream-policy", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);

        AccessStepUtil asu = new AccessStepUtil(context);

        // list Policies already added
        asu.addTablePolicies(div, submissionInfo.getBitstream(), collection);

        List form = div.addList("submit-edit-policy", List.TYPE_FORM);
        form.setHead(T_head);

        asu.addListGroups(request.getParameter("group_id"), form, errorFlag, collection);

        // radio buttons: Item will be visible / Embargo Access + date
        asu.addAccessRadios(request.getParameter("open_access_radios"), request.getParameter("embargo_until_date"), form, errorFlag, submissionInfo.getBitstream());

	    asu.addName(request.getParameter("name"), form, errorFlag);

        // Reason
        asu.addReason(request.getParameter("reason"), form, errorFlag);

        // Add Policy Button
        boolean isAdvancedFormEnabled= DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);
        if(isAdvancedFormEnabled){
            Button addPolicy = form.addItem().addButton(org.dspace.submit.step.AccessStep.FORM_ACCESS_BUTTON_ADD);
            addPolicy.setValue(T_submit_add_policy);
        }

        div.addHidden("bitstream_id").setValue(submissionInfo.getBitstream().getID().toString());

        // Note, not standard control actions, this page just goes back to the upload step.
        Item actions = form.addItem();
        actions.addButton("bitstream_list_submit_save").setValue(T_submit_save);
    }
}
