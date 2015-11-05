/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.submit.LicenseStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.core.LogManager;
import org.dspace.submit.AbstractProcessingStep;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * modified for LINDAT/CLARIN
 */
public class UFALLicenseStep extends LicenseStep {

	private static final Logger log = Logger.getLogger(UFALLicenseStep.class);

	protected static final Message T_select_error = message("xmlui.Submission.submit.UFALLicenseStep.notselect_error");
	
	protected static final Message T_distribution_license_head = message("xmlui.Submission.submit.LicenseStep.distribution_license_head");
	protected static final Message T_distribution_license_head_link = message("xmlui.Submission.submit.LicenseStep.distribution_license_head_link");
	protected static final Message T_resource_license_text = message("xmlui.Submission.submit.LicenseStep.resource_license_text");
	protected static final Message T_resource_license_text2 = message("xmlui.Submission.submit.LicenseStep.resource_license_text2");	
	protected static final Message T_license_selector_button = message("xmlui.Submission.submit.LicenseStep.license_selector_button");
	protected static final Message T_license_list_alert = message("xmlui.Submission.submit.LicenseStep.license_list_alert");
	protected static final Message T_license_list_alert_link = message("xmlui.Submission.submit.LicenseStep.license_list_alert_link");
	protected static final Message T_more_licenses_head = message("xmlui.Submission.submit.LicenseStep.morelicense_head");
	protected static final Message T_more_licenses_line1 = message("xmlui.Submission.submit.LicenseStep.morelicense_line1");
	protected static final Message T_more_licenses_line2 = message("xmlui.Submission.submit.LicenseStep.morelicense_line2");
	protected static final Message T_more_licenses_line3 = message("xmlui.Submission.submit.LicenseStep.morelicense_line3");
	protected static final Message T_more_licenses_line4 = message("xmlui.Submission.submit.LicenseStep.morelicense_line4");
	protected static final Message T_more_licenses_line5 = message("xmlui.Submission.submit.LicenseStep.morelicense_line5");
	protected static final Message T_license_detail = message("xmlui.Submission.submit.UFALLicenseStep.license_detail");
	protected static final Message T_license_not_supported = message("xmlui.Submission.submit.UFALLicenseStep.license_not_supported");
	protected static final Message T_review_msg= message("xmlui.Submission.submit.UFALLicenseStep.review_no_file");
	protected static final Message T_review_license_error= message("xmlui.Submission.submit.UFALLicenseStep.review_license_error");
	protected static final Message T_license_select_placeholder= message("xmlui.Submission.submit.UFALLicenseStep.option_placeholder");

	
	//
	// ctor
	//

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public UFALLicenseStep() {
		this.requireSubmission = true;
		this.requireStep = true;
	}

	//
	// methods
	//

	/**
	 * Add information for structural.xsl
	 * 
	 * @see structural.xsl in dri2xhtml theme jmisutka 2011/10/28
	 */
	@Override
	public void addPageMeta(PageMeta pageMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		super.addPageMeta(pageMeta);
		pageMeta.addMetadata("include-library", "licenseselect");
		pageMeta.addMetadata("include-library", "select2");
		pageMeta.addMetadata("include-library", "bootstrap-toggle");
	}

	/**
	 * Check if this is actually the creative commons license step
	 */
	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters parameters) throws ProcessingException, SAXException,
			IOException {
		super.setup(resolver, objectModel, src, parameters);
	}

	public List addBodySelectLicense(Body body) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {

		// Get the full text for the actual license
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle()
				+ "/submit/" + knot.getId() + ".continue";

		Division div = body.addInteractiveDivision("submit-license", actionURL, Division.METHOD_POST, "primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);

		// get one bundle (all should have the same licenses)
		// - select those ones which are already present
		org.dspace.content.Item item = submissionInfo.getSubmissionItem().getItem();

		// After the review step there can be the granted license..
		boolean file_uploaded = (0 < item.getBundles("ORIGINAL").length)
				&& (0 < item.getBundles("ORIGINAL")[0].getBitstreams().length);

		List controls = div.addList("controls", List.TYPE_FORM, "well well-light");

		List license_text = controls.addList("license-select", List.TYPE_FORM, "well well-white");
		
		Item distribution_head = license_text.addItem("head", "distribution_licnese_head");
		distribution_head.addHighlight("").addContent(T_distribution_license_head);
		distribution_head.addHighlight("").addXref(contextPath + "/page/contract", T_distribution_license_head_link, "target_blank");

		Item cb = license_text.addItem("license-decision", "license-decision-div");
		
		CheckBox decision = cb.addHighlight("license-decision-checkbox").addCheckBox("decision", "license_checkbox");

		// get the maximum page reached
		int maxStepReached = getMaxStepAndPageReached().getStep();
		int currentStep = getStep();

		// check the box if the step has already been completed
		if (currentStep < maxStepReached && this.errorFlag != org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED) {
			decision.addOption(true, "accept");
		} else {
			decision.addOption("accept");
		}
		
		cb.addHighlight("license-decision-message").addContent(T_decision_checkbox);

		Item info3 = license_text.addItem(null, "alert");
		info3.addHighlight("fa fa-warning fa-lg").addContent(" ");
		info3.addHighlight("").addContent(T_info3);

		List form = null;
		Select license_select = null;

		if (file_uploaded) {

			form = controls.addList("submit-ufal-license", List.TYPE_FORM, "");
			form.setHead("Select the resource license");
			
			Item selectorLink = form.addItem("", "alert alert-info");
			selectorLink.addHighlight("").addContent(T_resource_license_text);
			selectorLink.addHighlight("").addContent(T_license_list_alert);
			selectorLink.addHighlight("").addXref(contextPath + "/page/licenses", T_license_list_alert_link, "target_blank alert-link");
			selectorLink.addHighlight("").addContent(".");			
			selectorLink.addXref("#!", T_license_selector_button, "btn btn-repository licenseselector bold btn-block btn-lg");
			
			Item orLbl = form.addItem(null, "text-center");
			orLbl.addContent("- OR -");

			Item helpText2 = form.addItem();
			helpText2.addHighlight("license-resource-text").addContent(T_resource_license_text2);			
						
			Item notsupported = form.addItem("license-not-supported-message", "alert alert-danger alert-dismissible fade in hidden");
			notsupported.addContent(T_license_not_supported);
			
			license_select = form.addItem().addSelect("license");

			// add not available
			license_select.setMultiple(false);

			IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
			licenseManager.openSession();

			java.util.List<LicenseDefinition> license_defs = licenseManager.getAllLicenses();

		  	List help_list = form.addList("accordion.lic");
			
			if (license_defs != null && !license_defs.isEmpty()) {
				
		  		List help_licenses = help_list.addList("accordion-group.lic.1", List.TYPE_FORM, "hidden");
		  		help_licenses.addItem("accordion-heading.lic.1", null).addHighlight("bold").addContent(T_license_detail);
		  		List licenses = help_licenses.addList("accordion-body.lic.1").addList("license-list", List.TYPE_BULLETED);				

				// get one bundle (all should have the same licenses)
				// - select those ones which are already present
				Bitstream bitstream = item.getBundles()[0].getBitstreams()[0];

				final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
				String[] selected_license = request.getParameterValues("license");
				if(selected_license!=null && selected_license[0].equals("")) selected_license = null;
				int selectedId = (selected_license != null && selected_license.length > 0) ? Integer.parseInt(selected_license[0]) : -1;

				license_select.addOption(true, "", T_license_select_placeholder);
				
				for (LicenseDefinition license_def : license_defs) {
					boolean selected = license_def.getLicenseId() == selectedId;
					license_select.addOption(selected, license_def.getLicenseId(), license_def.getName());
					Item li = licenses.addItem();
					li.addHighlight(license_def.getLicenseLabel().getLabel()).addContent(license_def.getLicenseLabel().getTitle());
					li.addXref(license_def.getDefinition(), license_def.getName(), "target_blank", "license_" + license_def.getID());
				}

				java.util.List<LicenseDefinition> present_licenses = licenseManager
						.getLicenses(bitstream.getID());

				for (LicenseDefinition present_license : present_licenses) {
					license_select.setOptionSelected(present_license.getLicenseId());
				}

				licenseManager.closeSession();

			} else {
				String msg = "N/A";
				license_select.addOption(false, msg, msg);
				license_select.setDisabled();
			}

			List new_license_list = form.addList("no-license-match", List.TYPE_FORM, "alert");
			new_license_list.addItem("", "bold").addContent(T_more_licenses_head);
			new_license_list.addItem(T_more_licenses_line1);
			List new_license_body = new_license_list.addList("steps", List.TYPE_BULLETED);
			new_license_body.addItem(T_more_licenses_line2);
			new_license_body.addItem(T_more_licenses_line3);
			new_license_body.addItem(T_more_licenses_line4);
			new_license_body.addItem(T_more_licenses_line5);
		}

		// If user did not accept agreement
		if (this.errorFlag == org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED) {
			log.info(LogManager.getHeader(context, "reject_license", submissionInfo.getSubmissionLogInfo()));
			decision.addError(T_decision_error);
		}

		// If user did not select any license
		if (this.errorFlag == cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_NOT_SELECTED) {
			controls.addItem().addHighlight("error").addContent(T_select_error);
			license_select.addError(T_select_error);
			log.info(LogManager.getHeader(context, "notselect_license",
					submissionInfo.getSubmissionLogInfo()));
		}

		return controls;
	}

	@Override
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		List form = addBodySelectLicense(body);

		// add standard control/paging buttons
		addControlButtons(form);
		return;
	}

	@Override
	public List addReviewSection(List reviewList) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		// Create a new list section for this step (and set its heading)
		if (this.getPage() == 1) {
			List licenseSection = reviewList.addList("submit-review-"
                + this.stepAndPage, List.TYPE_FORM);

			licenseSection.setHead(T_head);
			
			IFunctionalities licenseManager = DSpaceApi
					.getFunctionalityManager();
			licenseManager.openSession();

			org.dspace.content.Item item = submissionInfo.getSubmissionItem()
					.getItem();
			// assert 0 < item.getBundles().length; //XXX: we allow submissions
			// with no items, not sure if this should hold
			if (item.getBundles("ORIGINAL").length > 0) {
				Bundle bundle = item.getBundles("ORIGINAL")[0];
				if (bundle.getBitstreams().length > 0) {
					Bitstream bitstream = bundle.getBitstreams()[0];
					java.util.List<LicenseDefinition> present_licenses = licenseManager
							.getLicenses(bitstream.getID());

					if (present_licenses != null && !present_licenses.isEmpty()) {
						for (LicenseDefinition license : present_licenses) {
							licenseSection.addItem().addXref(
									license.getDefinition(), license.getName(),
									"target_blank");
						}
						licenseManager.closeSession();
						return licenseSection;
					}else{
						Item errorMsg = licenseSection.addItem(null, "alert alert-danger");
						errorMsg.addContent(T_review_license_error);
						licenseManager.closeSession();
						return licenseSection;
					}
				}
			}
			licenseManager.closeSession();
			licenseSection.addItem(T_review_msg);
			return licenseSection;
		}
		return null;
	}

	public void addControlButtons(List controls, String name, Message value)
			throws WingException {
		// copied from parent
		Item actions = controls.addItem();
		// only have "<-Previous" button if not first step
		if (!isFirstStep()) {
			actions.addButton(AbstractProcessingStep.PREVIOUS_BUTTON).setValue(
					T_previous);
		}
		// always show "Save/Cancel"
		actions.addButton(AbstractProcessingStep.CANCEL_BUTTON)
				.setValue(T_save);

		// special
		actions.addButton(name).setValue(value);

		// If last step, show "Complete Submission"
		if (isLastStep()) {
			actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(
					T_complete);
			// otherwise, show "Next->"
		} else {
			actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(
					T_next);
		}
	}

}


