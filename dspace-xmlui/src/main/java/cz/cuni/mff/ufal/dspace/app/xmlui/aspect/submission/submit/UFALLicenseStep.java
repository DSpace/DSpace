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
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.LicenseUtils;
import org.dspace.core.LogManager;
import org.dspace.submit.AbstractProcessingStep;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * modified for LINDAT/CLARIN
 */
public class UFALLicenseStep extends LicenseStep {

	private static final Logger log = Logger.getLogger(UFALLicenseStep.class);

	//private static final int DEFAULT_LICENSE_SUBMITTER = 1;

	protected static final Message T_license_detail = message("xmlui.Submission.submit.UFALLicenseStep.license_detail");
	protected static final Message T_license_select = message("xmlui.Submission.submit.UFALLicenseStep.license_select");
	protected static final Message T_license_select_help = message("xmlui.Submission.submit.UFALLicenseStep.license_select_help");
    protected static final Message T_no_file_uploaded = message("xmlui.Submission.submit.UFALLicenseStep.no_file_uploaded");
	protected static final Message T_license_select_delete_help = message("xmlui.Submission.submit.UFALLicenseStep.license_select_delete_help");
	protected static final Message T_select_error = message("xmlui.Submission.submit.UFALLicenseStep.notselect_error");
	protected static final Message T_define = message("xmlui.Submission.submit.UFALLicenseStep.define");
	protected static final Message T_delete = message("xmlui.Submission.submit.UFALLicenseStep.delete");
	protected static final Message T_define_add = message("xmlui.Submission.submit.UFALLicenseStep.define.add");
	protected static final Message T_define_cancel = message("xmlui.Submission.submit.UFALLicenseStep.define.noadd");
	protected static final Message T_license_name = message("xmlui.Submission.submit.UFALLicenseStep.define.name");
	protected static final Message T_license_url = message("xmlui.Submission.submit.UFALLicenseStep.define.url");
	protected static final Message T_license_confirmation = message("xmlui.Submission.submit.UFALLicenseStep.define.confirmation");
	protected static final Message T_license_no_confirmation = message("xmlui.Submission.submit.UFALLicenseStep.define.no_confirmation");
	protected static final Message T_license_once_confirmation = message("xmlui.Submission.submit.UFALLicenseStep.define.once_confirmation");
	protected static final Message T_license_always_confirmation = message("xmlui.Submission.submit.UFALLicenseStep.define.always_confirmation");	
	protected static final Message T_delete_error_name = message("xmlui.Submission.submit.UFALLicenseStep.delete.error");
	protected static final Message T_define_error = message("xmlui.Submission.submit.UFALLicenseStep.define.error");
	protected static final Message T_define_error_name = message("xmlui.Submission.submit.UFALLicenseStep.define.noname");
	protected static final Message T_define_error_url = message("xmlui.Submission.submit.UFALLicenseStep.define.nourl");
	protected static final Message T_head_define = message("xmlui.Submission.submit.UFALLicenseStep.define.head");
	protected static final Message T_head_delete = message("xmlui.Submission.submit.UFALLicenseStep.delete.head");
	protected static final Message T_empty_label = message("xmlui.Submission.submit.UFALLicenseStep.define.empty_label");

	//protected final static Map<String, String> required_licenses = initRequired();

	//
	// static
	//

	/*private static Map<String, String> initRequired() {
		Map<String, String> map = new HashMap<String, String>();
		// commercial
		map.put("allow commercial sharing and changing",
				"http://creativecommons.org/licenses/by/3.0/");
		map.put("allow commercial sharing and changing with same license",
				"http://creativecommons.org/licenses/by-sa/3.0/");
		map.put("allow commercial sharing",
				"http://creativecommons.org/licenses/by-nd/3.0/");
		// non - commercial
		map.put("allow non commercial sharing and changing",
				"http://creativecommons.org/licenses/by-nc/3.0/");
		map.put("allow non commercial sharing and changing with same license",
				"http://creativecommons.org/licenses/by-nc-sa/3.0/");
		map.put("allow non commercial sharing",
				"http://creativecommons.org/licenses/by-nc-nd/3.0/");
		return map;
	}*/

	private static void checkRequiredLicenses() {
		IFunctionalities licensesManager = DSpaceApi.getFunctionalityManager();
		licensesManager.openSession();
		java.util.List<LicenseDefinition> licenses = licensesManager.getAllLicenses();
		if (licenses == null) {
			// something bad happened
			return;
		}
		// delete present licenses from required_tmp
		/*Set<String> required_tmp = required_licenses.keySet();
		for (LicenseDefinition license : licenses) {
			String licenseName = license.getName();
			if (required_tmp.contains(licenseName))
				required_tmp.remove(licenseName);
		}
		// if not empty required_tmp - create them
		for (String key : required_tmp) {
			assert required_licenses.containsKey(key);
			licensesManager.defineLicense(key, DEFAULT_LICENSE_SUBMITTER, required_licenses.get(key), 0, 4);
		}*/
		
		licensesManager.closeSession();
	}

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

	public List addBodyDeleteLicense(Body body) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		// Get the full text for the actuial licese
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle()
				+ "/submit/" + knot.getId() + ".continue";

		Division div = body.addInteractiveDivision("submit-license-delete",
				actionURL, Division.METHOD_POST, "primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);

		List form = div.addList("submit-ufal-license-delete", List.TYPE_FORM);
		form.setHead(T_head_delete);

		Select license_select = form.addItem().addSelect("license_name");
		license_select.setLabel(T_license_select);
		license_select.setHelp(T_license_select_delete_help);

		// add not available
		license_select.setMultiple(false);
		IFunctionalities licensesManager = DSpaceApi.getFunctionalityManager();
		licensesManager.openSession();
		
		java.util.List<LicenseDefinition> licenses = licensesManager.getAllLicenses();

		boolean licence_was_added = false;
		
		if (licenses!=null && !licenses.isEmpty()) {
			for (LicenseDefinition license : licenses) {
				String license_name = license.getName();
				int license_user = license.getUserRegistration().getEpersonId();
				if (license_user!=eperson.getID())
					continue;
				license_select.addOption(false, license_name, license_name);
				licence_was_added = true;
			}
		}

		if (licenses.isEmpty() || !licence_was_added) {
			String msg = "N/A";
			license_select.addOption(false, msg, msg);
			license_select.setDisabled();
		}

		if (this.errorFlag == cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DELETE_ERROR) {
			form.addItem().addHighlight("error")
					.addContent(T_delete_error_name);
			log.info(LogManager.getHeader(context, "could not remove license",
					submissionInfo.getSubmissionLogInfo()));
		}

		licensesManager.closeSession();
		
		// fake new line
		form.addItem().addContent(" ");
		return form;
	}

	public List addBodyDefineLicense(Body body) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		// Get the full text for the actual license
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle()
				+ "/submit/" + knot.getId() + ".continue";

		Division div = body.addInteractiveDivision("submit-license-define",
				actionURL, Division.METHOD_POST, "primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);

		List form = div.addList("submit-ufal-license-define", List.TYPE_FORM);
		form.setHead(T_head_define);

		Text text = form.addItem().addText("license_name");
		text.setLabel(T_license_name);
		text = form.addItem().addText("license_url");
		text.setLabel(T_license_url);
		
		Select confirmation = form.addItem().addSelect("license_confirmation");
		confirmation.setLabel(T_license_confirmation);
		confirmation.addOption(0, T_license_no_confirmation);
		confirmation.addOption(2, T_license_once_confirmation);
		confirmation.addOption(1, T_license_always_confirmation);

		IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
		licenseManager.openSession();
		java.util.List<LicenseLabel> label_defs = (java.util.List<LicenseLabel>)licenseManager.getAll(LicenseLabel.class);
		if (label_defs!=null && !label_defs.isEmpty()) {
			
			Radio labels = form.addItem().addRadio("license_label", "license_labels");
			labels.setLabel("License Labels");

			boolean selected = true; // first option will be selected by default
			
			for (LicenseLabel l : label_defs) {
				String item_label = l.getLabel();
				if (!l.getTitle().isEmpty()) {
					item_label = String.format("%s (%s)", l.getTitle(), l.getLabel());
				}
				labels.addOption(selected, l.getLabelId(), item_label);
				selected = false;
			}
			
			licenseManager.closeSession();
		}
		
		if (this.errorFlag == cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DEFINE_NO_NAME) {
			form.addItem().addHighlight("error")
					.addContent(T_define_error_name);
			log.info(LogManager.getHeader(context, "no name for license",
					submissionInfo.getSubmissionLogInfo()));

		} else if (this.errorFlag == cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DEFINE_NO_URL) {
			form.addItem().addHighlight("error").addContent(T_define_error_url);
			log.info(LogManager.getHeader(context, "no urlfor license",
					submissionInfo.getSubmissionLogInfo()));

		} else if (this.errorFlag == cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DEFINE_ERROR) {
			form.addItem().addHighlight("error").addContent(T_define_error);
			log.info(LogManager.getHeader(context,
					"error while adding license",
					submissionInfo.getSubmissionLogInfo()));
		}

		return form;
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
    //After the review step there can be the granted license..
	boolean file_uploaded = (0 < item.getBundles("ORIGINAL").length) && (0 < item.getBundles("ORIGINAL")[0].getBitstreams().length);

	List controls = div.addList("controls", List.TYPE_FORM, "well well-light");
	//controls.setHead(T_head);


	List license_text = controls.addList("license-select", List.TYPE_FORM, "well well-white");
	
	license_text.setHead(T_decision_label);

	license_text.addItem().addContent(T_info1);
	license_text.addItem().addContent(T_info2);	
	
	Item dist_license = license_text.addItem("dist_license", "dist-license");
	
	CheckBox decision = dist_license.addCheckBox("decision", "bold");
	//decision.setLabel(T_decision_label);
	
	// get the maximum page reached 
	int maxStepReached = getMaxStepAndPageReached().getStep();
	int currentStep  = getStep();
	
	// check the box if the step has already been completed
	if (currentStep < maxStepReached) {
		decision.addOption(true, "accept",T_decision_checkbox);
	}
	else {
		decision.addOption("accept",T_decision_checkbox);	
	}
	
	// Default license settings from LicenseStep.java    
    String licenseText = LicenseUtils.getLicenseText(context.getCurrentLocale(), collection, submission.getItem(), submission.getSubmitter());

    license_text.addItem("distributionlicense", "hidden").addContent(licenseText);
    
    Item info3 = license_text.addItem(null, "alert bold");
    info3.addHighlight("fa fa-warning fa-lg").addContent(" ");
    info3.addHighlight("").addContent(T_info3);
    	
    List form = null;
    //if ( !file_uploaded ) {
        //div.addPara().addContent(T_no_file_uploaded);
                          
    //} else {
    if (file_uploaded) {
                              
	  	form = controls.addList("submit-ufal-license", List.TYPE_FORM, "");
	  	form.setHead("Resource License");
	  	
	    List ls = form.addList("license-selecotr", List.TYPE_GLOSS);	    
	    Item helpText = ls.addItem();
	    //helpText.addHighlight("fa fa-legal fa-2x pull-left").addContent(" ");
	    helpText.addHighlight("").addContent("If you know under which license you want to distribute your work, please select from the list. If you need help please use the license selector:");
	    ls.addItem().addXref("#!", "OPEN License Selector", "btn btn-link licenseselector bold");
  		
	  	Select license_select = ls.addItem().addSelect("license", "input-xxlarge");
  		license_select.setLabel(T_license_select);
	  	license_select.setHelp(T_license_select_help);
	  	
	  	Item detailLicenseLink = form.addItem(null, "alert");
	  	detailLicenseLink.addHighlight("fa fa-lg fa-question-circle").addContent(" ");
	  	detailLicenseLink.addHighlight("bold").addContent("To view more details about the licenses ");
	  	detailLicenseLink.addHighlight("bold").addXref(contextPath + "/page/licenses", "Click here", "target_blank");
	  	detailLicenseLink.addHighlight("bold").addContent(".");

  		// add not available
	  	license_select.setMultiple(false);
		  // quick check if we include all mandatory ones
  		checkRequiredLicenses();
		
	  	IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
	  	licenseManager.openSession();

  		java.util.List<LicenseDefinition> license_defs = licenseManager.getAllLicenses();
  		
	  	List help_list = form.addList("accordion.lic");

	  	if (license_defs!=null && !license_defs.isEmpty()) {

	  		List help_licenses = help_list.addList("accordion-group.lic.1", List.TYPE_FORM, "hidden");
	  		help_licenses.addItem("accordion-heading.lic.1", null).addHighlight("bold").addContent(T_license_detail);
	  		List licenses = help_licenses.addList("accordion-body.lic.1").addList("bulleted-list", List.TYPE_BULLETED);

  			// get one bundle (all should have the same licenses)
 	  		// - select those ones which are already present
  			Bitstream bitstream = item.getBundles()[0].getBitstreams()[0];
  			
  			final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);  			
  			String[] selected_license = request.getParameterValues("license");
  			int selectedId = (selected_license != null && selected_license.length > 0) ? Integer.parseInt(selected_license[0]) : -1;
			
	  		for (LicenseDefinition license_def : license_defs) {
	  			boolean selected = license_def.getLicenseId() == selectedId;
		  		license_select.addOption(selected, license_def.getLicenseId(), license_def.getName());
		  		licenses.addItem().addXref(license_def.getDefinition(), license_def.getName(), "target_blank", "_" + license_def.getID());
  			}
			
	  		java.util.List<LicenseDefinition> present_licenses = licenseManager.getLicenses(bitstream.getID());

		  	for(LicenseDefinition present_license : present_licenses) {
			  	license_select.setOptionSelected(present_license.getLicenseId());
  			}
			
		  	licenseManager.closeSession();
			
	  	} else {
		  	String msg = "N/A";
		  	license_select.addOption(false, msg, msg);
  			license_select.setDisabled();
	  	}

  		List new_license_list = controls.addList("no-license-match", List.TYPE_FORM, "alert alert-danger");
  		new_license_list.addItem("", "bold").addContent(message("xmlui.Submission.submit.LicenseStep.morelicense_head"));
  		new_license_list.addItem(message("xmlui.Submission.submit.LicenseStep.morelicense_line1"));
  		List new_license_body = new_license_list.addList("steps", List.TYPE_BULLETED);
  		new_license_body.addItem(message("xmlui.Submission.submit.LicenseStep.morelicense_line2"));
  		new_license_body.addItem(message("xmlui.Submission.submit.LicenseStep.morelicense_line3"));
  		new_license_body.addItem(message("xmlui.Submission.submit.LicenseStep.morelicense_line4"));
  		new_license_body.addItem(message("xmlui.Submission.submit.LicenseStep.morelicense_line5"));
    }
    	

	// If user did not check "I accept" checkbox 
	if(this.errorFlag==org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED)
	{
         log.info(LogManager.getHeader(context, "reject_license", submissionInfo.getSubmissionLogInfo()));

		decision.addError(T_decision_error);
	}

/*
 permission problems - moved to control panel
		Item item = controls.addItem();
		Button define_button = item
				.addButton(org.dspace.submit.step.UFALLicenseStep.DEFINELICENSE_BUTTON);
		define_button.setValue(T_define);
		Button delete_button = item
				.addButton(org.dspace.submit.step.UFALLicenseStep.DELETELICENSE_BUTTON);
		delete_button.setValue(T_delete);
*/
		// If user did not check "I accept" checkbox
		if (this.errorFlag == cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_NOT_SELECTED) {
			controls.addItem().addHighlight("error").addContent(T_select_error);
			log.info(LogManager.getHeader(context, "notselect_license",
					submissionInfo.getSubmissionLogInfo()));
		}

		return controls;
	}
	
	@Override
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
//		if (1 != this.getPage()) {
//			super.addBody(body);
//			return;

			// define new license - abusing errorFlag ;)
//		} else if (this.errorFlag >= org.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DEFINE) {
		if (this.errorFlag >= cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DEFINE) {
			List form = addBodyDefineLicense(body);
			addControlButtonsDefine(form);
			return;

			// delete new license
		} else if (this.errorFlag >= cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.STATUS_LICENSE_DELETE) {
			List form = addBodyDeleteLicense(body);
			addControlButtonsDelete(form);
			return;

			// add license
		} else {
			List form = addBodySelectLicense(body);			

			// add standard control/paging buttons
			addControlButtons(form);
			return;
		}

	}

	@Override
	public List addReviewSection(List reviewList) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		// Create a new list section for this step (and set its heading)
		if (this.getPage() == 1) {
			IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
			licenseManager.openSession();

			org.dspace.content.Item item = submissionInfo.getSubmissionItem()
					.getItem();
			//assert 0 < item.getBundles().length; //XXX: we allow submissions with no items, not sure if this should hold
			if(item.getBundles().length>0){
				Bundle bundle = item.getBundles()[0];
				if(bundle.getBitstreams().length>0){
					
					List licenseSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
					
					licenseSection.setHead(T_head);
					
					Bitstream bitstream = bundle.getBitstreams()[0];
					java.util.List<LicenseDefinition> present_licenses = licenseManager.getLicenses(bitstream.getID());

					if (present_licenses!=null && !present_licenses.isEmpty()) {
						for (LicenseDefinition license : present_licenses) {
							licenseSection.addItem().addXref(license.getDefinition(), license.getName(), "target_blank");
						}
						licenseManager.closeSession();
						return licenseSection;
					}
				}
				licenseManager.closeSession();
			}
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

	public void addControlButtonsDefine(List controls) throws WingException {
		Item actions = controls.addItem();
		actions.addButton(
				cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.DEFINELICENSE_CANCEL_BUTTON)
				.setValue(T_previous);
		actions.addButton(
				cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.DEFINELICENSE_ADD_BUTTON)
				.setValue(T_next);
	}

	public void addControlButtonsDelete(List controls) throws WingException {
		Item actions = controls.addItem();
		actions.addButton(
				cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.DEFINELICENSE_CANCEL_BUTTON)
				.setValue(T_previous);
		actions.addButton(
				cz.cuni.mff.ufal.dspace.submit.step.UFALLicenseStep.DELETELICENSE_DELETE_BUTTON)
				.setValue(T_next);
	}

}




