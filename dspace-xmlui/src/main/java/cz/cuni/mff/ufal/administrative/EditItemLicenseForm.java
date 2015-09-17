/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.administrative;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.aspect.administrative.item.ViewItem;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;


/**
 * Display information about the item and allow the user to change 
 * 
 * @author Amir Kamran
 */


public class EditItemLicenseForm extends AbstractDSpaceTransformer {
	
	Logger log = Logger.getLogger(EditItemLicenseForm.class);

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");	
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
	
	protected static final Message T_head_define = message("xmlui.Submission.submit.UFALLicenseStep.define.head");
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

	
	private static final Message T_save = message("xmlui.general.save");
	private static final Message T_update = message("xmlui.general.update");
	private static final Message T_return = message("xmlui.general.return");

	
	private static final Message T_title = message("xmlui.administrative.item.EditItemLicenseForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditItemLicenseForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.EditItemLicenseForm.head1");
	private static final Message T_help = message("xmlui.administrative.item.EditItemLicenseForm.help");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		boolean define = parameters.getParameterAsBoolean("define", false);
		
		Item item = Item.find(context, itemID);
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();

		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-license", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative edit-item-license");
		main.setHead(T_option_head);

		String tabLink = baseURL + "&edit_license";
		
		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		ViewItem.add_options(context, eperson, options, baseURL, ViewItem.T_option_license, tabLink);

		if(!define) {
						
			List editLicenseForm = main.addList("currentLicense", List.TYPE_FORM, "well well-light");
			editLicenseForm.setHead(T_head1);
			
			Metadatum[] dc_rights_uri = item.getMetadata("dc", "rights", "uri", Item.ANY);
			
			//String licenseName = null;
			String licenseURI = null;
				            
			if(dc_rights_uri!=null && dc_rights_uri.length!=0) {
				//licenseName = dc_rights[0].value;
				licenseURI = dc_rights_uri[0].value;
			} 
			
			IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
			functionalityManager.openSession();
			LicenseDefinition license = functionalityManager.getLicenseByDefinition(licenseURI);
									
			if(license != null) {
				org.dspace.app.xmlui.wing.element.Item licenseSec = editLicenseForm.addItem(null, "alert alert-info");
				log.debug("License found - " + license.getName());
				licenseSec.addHighlight("").addXref(license.getDefinition(), license.getName(), "label label-info");
				licenseSec.addHighlight("").addXref(baseURL + "&remove_license", message("xmlui.administrative.item.EditItemLicenseForm.remove_license"));
				//licenseSec.addButton("remove_license").setValue(message("xmlui.administrative.item.EditItemLicenseForm.remove_license"));
			} else {
				log.debug("No license attached with the item.");
				editLicenseForm.addItem(null, "alert").addContent(message("xmlui.administrative.item.EditItemLicenseForm.no_license"));
				license = new LicenseDefinition();
				license.setLicenseId(-1);
			}
			
			
			java.util.List<LicenseDefinition> licenses = functionalityManager.getAllLicenses();

			Select licenseSelect = editLicenseForm.addItem().addSelect("licenseSelect");
			licenseSelect.setLabel("Select License");
			licenseSelect.setHelp(T_help);
			licenseSelect.setMultiple(false);		
			licenseSelect.addOption(true, -1, "-Select License-");
			for(LicenseDefinition lic : licenses) {
				if(lic.getLicenseId() != license.getLicenseId()) {
					licenseSelect.addOption(lic.getLicenseId(), lic.getName());
				}
			}
			
			editLicenseForm.addItem().addXref(baseURL + "&edit_license_define", "Define New License");
			
			org.dspace.app.xmlui.wing.element.Item buttonList = editLicenseForm.addItem();
			
			buttonList.addButton("update_item_license").setValue(T_update);
			buttonList.addButton("return").setValue(T_return);			
			
			functionalityManager.closeSession();
        
        } else {
        	
        	LicenseForm.defineLicenseForm(main);

        }
		
		main.addPara().addHidden("administrative-continue").setValue(knot.getId());

	}
	
}
