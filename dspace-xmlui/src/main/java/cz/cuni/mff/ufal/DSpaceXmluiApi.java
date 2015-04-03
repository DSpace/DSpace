/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

// Import needed for app.xmlui.aspect.artifactbrowser.ItemViewer.java
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.HtmlHelper;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
// Import needed for cz.cuni.mff.ufal.UFALLicenceAgreement.java
//import org.dspace.license.CreativeCommons;
//import org.dspace.app.xmlui.wing.Message;
// Import needed for cz.cuni.mff.ufal.UFALLicenceAgreementAgreed.java
//import org.dspace.app.xmlui.wing.element.Item;
//import cz.cuni.mff.ufal.LindatClarinClientServerDBApp.IRequest ;

/*
 * This class is an extension of the DSpace source code. Initial version is 1.6.2
 */
public class DSpaceXmluiApi {

	private static final Logger log = Logger.getLogger(DSpaceXmluiApi.class);

	/**
	 * Function sets the error message that is then displayed.
	 */
	public static void app_xmlui_aspect_eperson_postError(Division div)
			throws org.dspace.app.xmlui.wing.WingException {
		String err = DSpaceApi.getFunctionalityManager().getErrorMessage(); 
		if ( err == null ) {
			err = "undefined";
		}
		log.warn("Displaying ERROR message app_xmlui_aspect_eperson_postError - " + err);
		if (DSpaceApi.getFunctionalityManager().isFunctionalityEnabled("lr.post.error") == false) {
			return;
		}
		
		div.addPara("errors", "alert alert-danger").addContent(err);
	}
	
	public static void app_xmlui_aspect_eperson_postError(Context context, Message head, Division div)
			throws org.dspace.app.xmlui.wing.WingException{

		Object o = context.fromCache(cz.cuni.mff.ufal.Headers.class, 1);
		if ( o != null ) 
		{
			final Map<String, java.util.List<String>> headers = ((cz.cuni.mff.ufal.Headers)o).get();
			div.setHead("Required contact details missing");
			String feedback = ConfigurationManager.getProperty("feedback.recipient");
			if (org.apache.commons.lang.StringUtils.isBlank(feedback)){
				log.error("feedback.recipient not set.");
			}
			Para para = div.addPara();
			
			String idp = find_idp(headers);
			para.addContent(
					String.format(
					"The authentication was successful; however, your identity provider (in most cases " +
					"it is your home institution - %s) did provide neither your " +
					"email, eppn (specific authentication id) nor targeted id (specific authentication id). " +
					"We need at least one of the properties mentioned above so our service can work " +
					"properly.\nTry to contact your identity provder and ask them for these attributes. If they ask for our entityID it is \"https://ufal-point.mff.cuni.cz/shibboleth/eduid/sp\". " +
					"\n\nFor more information please contact us at ", idp));
			para.addXref("mailto:"+feedback+"?subject=My IDP does not provide an email", feedback);
			para.addContent(". We might arrange a local account for you or try to resolve the issue with your identity provider. " +
					"Please include the following details in the email:");
			
			para = div.addPara("show-attr", null);
			para.addContent("Show details");
			
			Table table = div.addTable("shib-attr", 1, 1);			
			for(String headerName : headers.keySet() ){
				Row row = table.addRow();
				row.addCellContent(headerName);
				Cell cell = null;
				if(headerName.matches(".*mail.*")){
					cell = row.addCell(null, null, HtmlHelper.cls(HtmlHelper.header_class.NOT_OK));
				}
				else{
					cell = row.addCell();
				}
				cell.addContent((headers.get(headerName)).toString().replaceFirst("\\[(.*)\\]", "$1").replaceAll(";", "; "));
			}
			
			Division dialog = div.addDivision("dialog");
			dialog.addPara("The authentication was successful but " +
					"we have not received all the required information. " +
					"Please, read carefully the information on the following page. " +
					"If you need more help, do not hesitate to contact us using the provided link.");
			/*List list = dialog.addList("dialog-list", org.dspace.app.xmlui.wing.element.List.TYPE_ORDERED);
			list.addItem("Contact Your IDP");
			list.addItem("If Your IDP can't or refuses to help you, contact us");*/
			return;
		}
		
		if(head != null){
			div.setHead(head);
		}			
		app_xmlui_aspect_eperson_postError(div);

	}
	
	private static String find_idp( Map<String, java.util.List<String>> headers ) {
		java.util.List<String> ret = find_shibboleth_header_value(headers, "Shib-Identity-Provider");
		return ret == null ? "unknown" : ret.get(0);
	}
	private static java.util.List<String> find_shibboleth_header_value( 
			Map<String, java.util.List<String>> headers,
			String to_find) 
	{
		to_find = to_find.toLowerCase();
		for ( String key : headers.keySet() )
		{
			if ( key.toLowerCase().equals(to_find) ) {
				return headers.get(key);
			}
		}
		return null;
	}

	/*
	 * Function for app.xmlui.aspect.artifactbrowser.ItemViewer.java. This
	 * function adds and information about licence agreeement agreed before.
	 * 
	 * @param context is needed for getting information whether there is a
	 * licence connected with a resource
	 * 
	 * @param item is needed for getting information whether there is a licence
	 * connected with a resource
	 * 
	 * @param division is actual paragraph where information is commited
	 */
    @Deprecated
	public static void app_xmlui_aspect_artifactbrowser_ItemViewer(Logger log,
			Context context, Item item, Division division) {

		/*
		 * // First check the availibility of the plugin if (
		 * DSpaceApi.getFunctionalityManager().isFunctionalityEnabled (
		 * "itemViewer" ) == false ){ return; }
		 * 
		 * // Inner functions throws exceptions so it is preferable to make the
		 * function try - catch free (otherwise other source code definition in
		 * DSpace are needed) try{
		 * 
		 * // If there is any linked Creative Commons licence, we put a string
		 * onto the item record String handle = item.getHandle(); String user =
		 * ""; // should not happen normally, we require valid email if ( null
		 * != context.getCurrentUser() ) user =
		 * context.getCurrentUser().getEmail();
		 * 
		 * // We have the access rights on the level of the bitstreams. It
		 * cannot be done differently, even policies at DSpace are set on this
		 * level. There is several reasons for that - copies of files, deleting,
		 * ... Bundle[] bundles = item.getBundles(); String bitstreamID = "";
		 * Map<String, String> labels = new HashMap<String, String>();
		 * for(Bundle bundle: bundles){ Bitstream[] bitstreams =
		 * bundle.getBitstreams(); // We get the array of bitstreams for every
		 * bundle for(Bitstream bitstream:bitstreams){ bitstreamID =
		 * String.valueOf(bitstream.getID());
		 * 
		 * // get string representation // String licences_string = ""; for (
		 * java.util.List<String> license :
		 * DSpaceApi.getFunctionalityManager().getLicenses(bitstreamID) ) { if (
		 * !licences_string.isEmpty() ) { licences_string +="], ["; } String
		 * license_id = license.get(0); String resource_id = license.get(1);
		 * String license_label = license.get(2); String license_label_title =
		 * license.get(3);
		 * log.info("app_xmlui_aspect_artifactbrowser_ItemViewer: license_label: "
		 * + license_label); licences_string += license_id; if (license_label !=
		 * null && !license_label.isEmpty() &&
		 * !labels.containsKey(license_label)) { if
		 * (!license_label_title.isEmpty()) { license_label_title =
		 * String.format("%s (%s)", license_label_title, license_id); } else {
		 * license_label_title = license_id; } labels.put(license_label,
		 * license_label_title); } } if ( licences_string.isEmpty() ) {
		 * licences_string = "none."; }else { licences_string = "[" +
		 * licences_string + "]"; }
		 * 
		 * Map<String,String> licenses =
		 * DSpaceApi.getFunctionalityManager().userLicensesToAgree
		 * (user,bitstreamID);
		 * 
		 * // TODO: Empty string is disgusting! - user has NOT agreed everything
		 * needed to view the resource - the list of licenses is not empty if(
		 * !licenses.isEmpty() ){ division.addPara(new Message("message","File "
		 * + bitstream.getName() + " needs to agree licence(s): " +
		 * licences_string)); // User gets notion about security of the resource
		 * if it is secured }else
		 * if(DSpaceApi.getFunctionalityManager().isResourceProtected
		 * (bitstreamID)){ division.addPara(new Message("message","File " +
		 * bitstream.getName() +
		 * " is protected by licenses you have agreed: "+licences_string)); } //
		 * else the bitstream is not protected } }
		 * //log.info("app_xmlui_aspect_artifactbrowser_ItemViewer: labels here "
		 * + labels.toString()); if (!labels.isEmpty()) {
		 * //log.info("app_xmlui_aspect_artifactbrowser_ItemViewer: Got labels"
		 * ); List license_labels = division.addList("license_labels",
		 * List.TYPE_SIMPLE, "license_labels"); for (String license_label :
		 * labels.keySet()) { license_labels.addItem(license_label,
		 * labels.get(license_label)) //.addHighlight(license_label)
		 * .addContent(license_label); } }
		 * 
		 * if(CreativeCommons.hasLicense(context,item)){ String licenseURL =
		 * CreativeCommons.getLicenseURL(item); division.addPara(new
		 * Message("message",
		 * "This content is restricted by CC licence you have agreed! Details are meant below!"
		 * )); labels.put("CC", "Content is restricted by CC licence"); }
		 * 
		 * } catch ( Exception e ){ log.debug (
		 * "app_xmlui_aspect_artifactbrowser_ItemViewer: " + e.getMessage() ); }
		 * 
		 * /*
		 */
	}


	private static String getParameter(String name, Request request)
			throws Exception {
		if (request.getParameter(name) != null) {
			return request.getParameter(name);
		}
		throw new Exception("Parameter '" + name + "' not found!");
	}

	/*
	 * Function fills in the page with the user validation.
	 */
	public static void UFALUserValidation(Context context, String contextPath,
			Map objectModel, Body body) {
		Request request = ObjectModelHelper.getRequest(objectModel);

		try {
			String epersonID = getParameter("user_id", request);
			String shibbolethID = getParameter("random_no", request);

			Division userValidation = null;
			userValidation = body.addDivision("validation", "primary");
			userValidation.setHead("User validation");
			EPerson eperson = EPerson.find(context, Integer.valueOf(epersonID));
			
			IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
			functionalityManager.openSession();
			functionalityManager.verifyUser(eperson.getID());
			functionalityManager.closeSession();
			// eperson.setCanLogIn(true);
			// eperson.update();
			userValidation
					.addPara(new Message("message", "User '"
							+ eperson.getFullName() + "' with mail '"
							+ eperson.getEmail()
							+ "' has been successfully verified!"));

			Division addLicence = body.addDivision("add-licence",
					"primary licence");
			addLicence.addPara(new Message("message",
					"Add licence X to bitstream Y"));
			addLicence.addPara().addXref(
					contextPath + "/handle/ufal-page?addLicence=X",
					new Message("message", "X+Y"));

			/*
			 * Division addLicence = null ; addLicence =
			 * body.addDivision("licences", "primary licence add"); List
			 * bitstreams = null ; bitstreams =
			 * addLicence.addList("set-licence-to-bitstream",List.TYPE_FORM);
			 * bitstreams.setHead("Bistream");
			 * 
			 * List licences = null ; licences =
			 * addLicence.addList("add-licence-to-bitstream",List.TYPE_FORM);
			 * licences.setHead("Licence");
			 * 
			 * Button submitButton =
			 * licences.addItem().addButton("AddLicence","AddLicences");
			 */

		} catch (Exception e) {
			// throw new Exception("Something bad has happened!");
			log.info(e.getMessage());
		}

	}
};

