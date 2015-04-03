/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Bitstream;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * Display to the user that they they need to sign the licenses.
 * 
 * @author Amir Kamran
 */

public class UFALLicenceAgreement extends AbstractDSpaceTransformer {
	
	private static final Logger log = Logger.getLogger(UFALLicenceAgreement.class);
	
	private static final Message T_title = message("xmlui.ufal.UFALLicenceAgreement.title");

	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_trail_item = message("xmlui.ufal.UFALLicenceAgreement.trail_item");
	
	private static final Message T_trail_license_agreement = message("xmlui.ufal.UFALLicenceAgreement.trail_license_agreement");

	private static final Message T_head = message("xmlui.ufal.UFALLicenceAgreement.head");

	private static final Message T_para1 = message("xmlui.ufal.UFALLicenceAgreement.agree_message");
	
    private static final Message T_signer_message = message("xmlui.ufal.UFALLicenceAgreement.signer_message");
    private static final Message T_signer_agree = message("xmlui.ufal.UFALLicenceAgreement.signer_agree");
    private static final Message T_signer_head = message("xmlui.ufal.UFALLicenceAgreement.signer_head");

	public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {

		// Set the page title
		pageMeta.addMetadata("title").addContent(T_title);
		
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        HandleUtil.buildHandleTrail(item,pageMeta,contextPath);
        pageMeta.addTrailLink(contextPath + "/handle/" + item.getHandle(), T_trail_item);
		pageMeta.addTrail().addContent(T_trail_license_agreement);
		
	}

	public void addBody(Body body) throws WingException {
		Division licenceAgreement = null;
		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();
		
		try {

			licenceAgreement = body.addDivision("ufal-licence-agreement", "well");
			licenceAgreement.setHead(T_head);

			// First check the availibility of the plugin
			if (functionalityManager.isFunctionalityEnabled("lr.license.agreement") == false) {
				functionalityManager.setErrorMessage(message("xmlui.ufal.UFALLicenceAgreement.functionality_blocked").toString());
				DSpaceXmluiApi.app_xmlui_aspect_eperson_postError(licenceAgreement);
				return;
			}			

			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
			Request request = ObjectModelHelper.getRequest(objectModel);

			HttpSession session = request.getSession();
			
			ArrayList<ExtraLicenseField> errors = new ArrayList<ExtraLicenseField>();
			String err = request.getParameter("err");
			if(err!=null && !err.isEmpty()) {
				String temp[] = err.split(",");
				for(String error : temp) {
					ExtraLicenseField errField = ExtraLicenseField.valueOf(error);
					errors.add(errField);
					licenceAgreement.addPara("errors", "alert alert-danger").addContent(errField.getErrorMessage());
				}
			} else {
				err = "";
			}

			List list = licenceAgreement.addList("licenses-urls", List.TYPE_FORM, "");
			list.addItem().addContent(T_para1);


			boolean allzip = Boolean.parseBoolean(request.getParameter("allzip"));
			int bitstreamID = -1;
			String redirectURL = contextPath + "/handle/" + dso.getHandle();
			
			ArrayList<String> licenseRequiresExtra = new ArrayList<String>();
						
			if (allzip) {

				// this is probably not enough
				Item item = (Item)dso;
				Metadatum[] lic = item.getMetadata("dc", "rights", "uri", Item.ANY);
				String licURL = lic[0].value;

				LicenseDefinition license = functionalityManager.getLicenseByDefinition(licURL);
				list.addItem().addXref(license.getDefinition(), license.getName(), "target_blank");
				String lr = license.getRequiredInfo();
				if(lr!=null) {
					for(String r : lr.split(",")) {
						licenseRequiresExtra.add(r.trim());
					}
				}
				redirectURL += "/ufal-licence-agreement-agreed?handle=" + dso.getHandle() + "&allzip=true";
			} else {
			
				bitstreamID = Integer.parseInt(request.getParameter("bitstreamId"));
				int userID = context.getCurrentUser().getID();		
				java.util.List<LicenseDefinition> licenses = functionalityManager.getLicensesToAgree(userID, bitstreamID);

				Division licences_div = licenceAgreement.addDivision("licences-definition", "");
				
				List i = licences_div.addList("licenses-url", List.TYPE_FORM);
				
				for (LicenseDefinition license : licenses) {
				    i.addItem().addXref(license.getDefinition(), " " + license.getName(), "target_blank btn licence_to_sign fa fa-search fa-1x");
				    //XXX cumulate the extra requirements from all(?) licenses
					String lr = license.getRequiredInfo();
					if(lr!=null) {
						for(String r : lr.split(",")) {
							if(!licenseRequiresExtra.contains(r)) {
								licenseRequiresExtra.add(r.trim());
							}
						}
					}
				}
				redirectURL += "/ufal-licence-agreement-agreed?handle=" + dso.getHandle() + "&bitstreamId=" + bitstreamID;
			}
			
			licenceAgreement.addPara(" ");
			licenceAgreement.addPara(T_signer_head);
			Division form = licenceAgreement.addInteractiveDivision("license-form", redirectURL, Division.METHOD_POST);
			
			String personCredentials = context.getCurrentUser().getFirstName() + " " + context.getCurrentUser().getLastName();
			String personId = context.getCurrentUser().getEmail();
			
			String header_rend = "bold";
			Table table = form.addTable("user-info", 4 + licenseRequiresExtra.size(), 2);
			
			// Signer name row
            Row r = table.addRow();
            r.addCell(null, null, header_rend).addContent("Signer");
            Text nameTextField = r.addCell().addHighlight("bold").addText("name");
            nameTextField.setValue(personCredentials);
            nameTextField.setDisabled();
			
            // Signer ID (email address of the user)
            r = table.addRow();
            r.addCell(null, null, header_rend).addContent("User ID");
            Text t = r.addCell().addHighlight("bold").addText("user-id");
            t.setValue(personId);
            t.setDisabled();

            // Item handle
            r = table.addRow();
            r.addCell(null, null, header_rend).addContent("Item handle");
            t = r.addCell().addHighlight("bold").addText("item-handle");
            t.setValue(dso.getHandle());
            t.setDisabled();
            
            
            // Extra metadata: if license requires extra metadata it will create a new row with a textbox
            // if the data is not storeable and just a flag e.g. SEND_TOKEN a message will be displayed
            
            ArrayList<Message> messages = new ArrayList<Message>();
            
            for(String extra : licenseRequiresExtra) {
            	ExtraLicenseField exField = ExtraLicenseField.valueOf(extra);
            	if(exField.isMetadata()) {		            
		            if(errors.contains(exField)) {
		            	r = table.addRow(null, null, "error");
		            } else {
		            	r = table.addRow();
		            }
	            	r.addCell(null, null, header_rend).addContent(message("xmlui.ExtraLicenseField.submission." + extra));
	            	t = r.addCell().addHighlight("bold").addText("extra_" + exField.name());
	            	String val = (String)session.getAttribute("extra_" + exField.name());
	            	if(val!=null && !val.isEmpty()) {
	            		t.setValue(val);
	            		session.removeAttribute("extra_" + exField.name());
	            	} else
	            	if(exField.equals(ExtraLicenseField.ORGANIZATION)) {
	            		val = (String)functionalityManager.getRegisteredUser(eperson.getID()).getOrganization();
	            		t.setValue(val);
	            		session.removeAttribute("extra_" + exField.name());	            		
	            	}

            	} else {
            		form.addHidden("extra_" + exField.name());
            		messages.add(message("xmlui.ExtraLicenseField.submission." + extra));
            	}
            }

            // bitstream name row
			Bitstream bitstream = Bitstream.find(context, bitstreamID);
			if ( bitstream != null ) {
	            r = table.addRow();
	            r.addCell(null, null, header_rend).addContent("Bitstream");
	            t = r.addCell().addHighlight("bold").addText("bitstream-name");
	            t.setValue(bitstream.getName());
	            t.setDisabled();
			}
			
            // IP Address
            r = table.addRow();
            r.addCell(null, null, header_rend).addContent("IP Address");
            t = r.addCell().addHighlight("bold").addText("ip-address");
            t.setValue(request.getRemoteAddr().toString());
            t.setDisabled();

			
			// messages for extra metadata
			if(messages!=null && !messages.isEmpty()) {
				List msg_lst = form.addDivision("licence_msg_div", "alert alert-info").addList("license_msg_list");
				for(Message message :messages) {
					msg_lst.addItem().addHighlight("bold").addContent(message);
				}
			}
			
			
			// final license signing message and button
			Division footer_div = form.addDivision("licence-footer", "alert alert-danger");
			footer_div.addPara(null, "fa fa-warning fa-4x pull-right").addContent(" ");
			footer_div.addPara("footer-msg", "bold").addContent(T_signer_message);			
            list = footer_div.addList("licenses-footer", List.TYPE_FORM);
                        
            form.addPara().addButton("confirm_license", "btn btn-repository").setValue(T_signer_agree);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		functionalityManager.closeSession();
	}
}
