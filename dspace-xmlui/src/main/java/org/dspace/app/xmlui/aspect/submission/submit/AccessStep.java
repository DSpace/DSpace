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
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class AccessStep extends AbstractSubmissionStep
{
    private static final Logger log = Logger.getLogger(LicenseStep.class);

    /** Language Strings **/
    protected static final Message T_head = message("xmlui.Submission.submit.AccessStep.head");
    protected static final Message T_submit_add_policy = message("xmlui.Submission.submit.AccessStep.submit_add_policy");
    protected static final Message T_private_settings = message("xmlui.Submission.submit.AccessStep.private_settings");
    protected static final Message T_private_settings_help = message("xmlui.Submission.submit.AccessStep.private_settings_help");
	protected static final Message T_private_label = message("xmlui.Submission.submit.AccessStep.private_settings_label");
	protected static final Message T_private_item = message("xmlui.Submission.submit.AccessStep.review_private_item");
	protected static final Message T_public_item = message("xmlui.Submission.submit.AccessStep.review_public_item");
	protected static final Message T_policy_head = message("xmlui.Submission.submit.AccessStep.new_policy_head");

    public static final int CHECKBOX_PRIVATE_ITEM=1;
    public static final int RADIO_OPEN_ACCESS_ITEM_VISIBLE=0;
    public static final int RADIO_OPEN_ACCESS_ITEM_EMBARGOED=1;

    private EditPolicyStep editPolicy= null;


	/**
     * Establish our required parameters, abstractStep will enforce these.
     */
    public AccessStep(){
        this.requireSubmission = true;
        this.requireStep = true;
    }

    /**
     * Check if user has requested to edit information about an
     * uploaded file
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver,objectModel,src,parameters);
        if(this.errorFlag==org.dspace.submit.step.AccessStep.STATUS_EDIT_POLICY
                || this.errorFlag==org.dspace.submit.step.AccessStep.EDIT_POLICY_STATUS_DUPLICATED_POLICY){
            this.editPolicy = new EditPolicyStep();
            this.editPolicy.setup(resolver, objectModel, src, parameters);
        }
    }


    public void addPageMeta(PageMeta pageMeta) throws WingException, SAXException, SQLException, AuthorizeException, IOException {
	    super.addPageMeta(pageMeta);
        pageMeta.addMetadata("javascript", "static").addContent("static/js/accessFormUtil.js");
    }


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException{

        // If we are actually editing information of an uploaded file,
        // then display that body instead!
        if(this.editPolicy!=null){
            editPolicy.addBody(body);
            return;
        }

        // Get our parameters and state
        Item item = submission.getItem();
        Collection collection = submission.getCollection();

        Request request = ObjectModelHelper.getRequest(objectModel);

        Division div = addMainDivision(body, collection);

        AccessStepUtil asu = new AccessStepUtil(context);

        List form = div.addList("submit-access-settings", List.TYPE_FORM);
        form.setHead(T_head);

        addPrivateCheckBox(request, form, item);

	    // list Policies already added
	    asu.addTablePolicies(div, item, collection);

	    form = div.addList("submit-add-item-policy", List.TYPE_FORM);
	    form.setHead(T_policy_head);

        asu.addListGroups(request.getParameter("group_id"), form, errorFlag, collection);

        // radio buttons: Item will be visible / Embargo Access + date

        asu.addAccessRadios(request.getParameter("open_access_radios"), request.getParameter("embargo_until_date"), form, errorFlag, item);

	    asu.addName(request.getParameter("name"), form, errorFlag);

        // Reason
        asu.addReason(request.getParameter("reason"), form, errorFlag);

        // Add Policy Button
        boolean isAdvancedFormEnabled=DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);
        if(isAdvancedFormEnabled){
            Button addPolicy = form.addItem().addButton(org.dspace.submit.step.AccessStep.FORM_ACCESS_BUTTON_ADD);
            addPolicy.setValue(T_submit_add_policy);
        }

        // add standard control/paging buttons
        addControlButtons(form);
    }

    private void addPrivateCheckBox(Request request, List form, Item item) throws WingException {
        CheckBox privateCheckbox = form.addItem().addCheckBox("private_option");
        privateCheckbox.setLabel(T_private_settings);
        privateCheckbox.setHelp(T_private_settings_help);
        if(request.getParameter("private_option")!=null || !item.isDiscoverable())
            privateCheckbox.addOption(true, CHECKBOX_PRIVATE_ITEM, T_private_label);
        else
            privateCheckbox.addOption(false, CHECKBOX_PRIVATE_ITEM, T_private_label);
    }

    private Division addMainDivision(Body body, Collection collection) throws WingException {
        // DIVISION: Main
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Division div = body.addInteractiveDivision("submit-restrict",actionURL, Division.METHOD_POST,"primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);
        return div;
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
	    List accessSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
	    accessSection.setHead(T_head);

	    Item item = submission.getItem();

	    accessSection.addLabel(T_private_settings);
	    accessSection.addItem(item.isDiscoverable() ? T_public_item : T_private_item);

	    AccessStepUtil asu = new AccessStepUtil(context);
	    asu.addListPolicies(accessSection, item, item.getOwningCollection());

	    return accessSection;
    }
}
