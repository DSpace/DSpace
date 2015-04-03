/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.submit.step;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.MetadataSchema;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * modified for LINDAT/CLARIN
 */
public class UFALLicenseStep extends org.dspace.submit.step.LicenseStep {

	/** log4j logger */
	private static Logger log = Logger.getLogger(UFALLicenseStep.class);

	// user did not select a license
	public static final int STATUS_LICENSE_NOT_SELECTED = 110;
	public static final int STATUS_LICENSE_DELETE = 121; // any num
	public static final int STATUS_LICENSE_DEFINE = 122;

	// treat nums above as errors
	public static final int STATUS_LICENSE_DEFINE_NO_NAME = 123;
	public static final int STATUS_LICENSE_DEFINE_NO_URL = 124;
	public static final int STATUS_LICENSE_DEFINE_ERROR = 125;
	public static final int STATUS_LICENSE_DELETE_ERROR = 126;

	public static final String DEFINELICENSE_BUTTON = "submit_define_license";
	public static final String DELETELICENSE_BUTTON = "submit_delete_license";
	public static final String DELETELICENSE_DELETE_BUTTON = "submit_delete_do_license";
	public static final String DEFINELICENSE_ADD_BUTTON = "submit_define_add_license";
	public static final String DEFINELICENSE_CANCEL_BUTTON = "submit_define_cancel_license";

	@Override
	public int doProcessing(Context context, HttpServletRequest request,
			HttpServletResponse response, SubmissionInfo subInfo)
			throws ServletException, IOException, SQLException,
			AuthorizeException {
		int current_page = AbstractProcessingStep.getCurrentPage(request);

		IFunctionalities iface = cz.cuni.mff.ufal.DSpaceApi.getFunctionalityManager();
		iface.openSession();
		// we want our page
		//
		if (1 == current_page) {

			String buttonPressed = Util.getSubmitButton(request, CANCEL_BUTTON);

			// go to new screen with license definition
			//
			if (buttonPressed.equals(DEFINELICENSE_BUTTON)) {
				return STATUS_LICENSE_DEFINE;

				// return from license definition / deletion
				//
			} else if (buttonPressed.equals(DEFINELICENSE_CANCEL_BUTTON)) {
				// process normal;y
				return STATUS_COMPLETE;

				// define new license
				//
			} else if (buttonPressed.equals(DEFINELICENSE_ADD_BUTTON)) {
				// add the license
				
				String license_name = request.getParameter("license_name");
				if (null == license_name || license_name.trim().isEmpty())
					return STATUS_LICENSE_DEFINE_NO_NAME;
				String license_url = request.getParameter("license_url");
				if (null == license_url || license_url.trim().isEmpty())
					return STATUS_LICENSE_DEFINE_NO_URL;
				
                int license_confirmation = Integer.parseInt(request.getParameter("license_confirmation"));
                String license_required = request.getParameter("license_required");

                int license_label = Integer.parseInt(request.getParameter("license_label"));

				// create new license
				EPerson submitter = subInfo.getSubmissionItem().getSubmitter();
				
				int userID = submitter.getID();

				boolean result = iface.defineLicense(license_name, userID, license_url, license_confirmation, license_required, license_label);
				
				if (result)
					return STATUS_COMPLETE;
				else
					return STATUS_LICENSE_DEFINE_ERROR;

				// go to delete licence screen
				//
			} else if (buttonPressed.equals(DELETELICENSE_BUTTON)) {
				return STATUS_LICENSE_DELETE;

				// do the licence deletion
				//
			} else if (buttonPressed.equals(DELETELICENSE_DELETE_BUTTON)) {				
				// add the license
				String license_name = request.getParameter("license_name");
				if (null == license_name || license_name.trim().isEmpty())
					return STATUS_LICENSE_DEFINE_NO_NAME;

				EPerson submitter = subInfo.getSubmissionItem().getSubmitter();
				int userID = submitter.getID();
				
				List<LicenseDefinition> licenses = iface.getAllLicenses();

				for (LicenseDefinition license_def : licenses) {
					if (license_def.getName().equals(license_name) && license_def.getUserRegistration().getEpersonId()==userID) {
						boolean status = iface.delete(LicenseDefinition.class, license_def);
						if (!status)
							return STATUS_LICENSE_DELETE_ERROR;
						break;
					}
				}

				// go to next step
				//
			} else if (buttonPressed.equals(NEXT_BUTTON)) {
				
				String decision = request.getParameter("decision");
				if (decision == null || !decision.equalsIgnoreCase("accept")) {
					return STATUS_LICENSE_REJECTED;
				}				
				
				// Add the license to all bitstreams(files)
				Item item = subInfo.getSubmissionItem().getItem();

				//After the review step there can be the granted license..
				boolean file_uploaded = (0 < item.getBundles("ORIGINAL").length) && (0 < item.getBundles("ORIGINAL")[0].getBitstreams().length); 
				if ( file_uploaded ) {
					String[] selected_license = request.getParameterValues("license");
					
					if (null == selected_license ) {
						// no decision made (this will cause Manakin to display an
						// error)
						return STATUS_LICENSE_NOT_SELECTED;
					}
					
					// a license was rejected
					log.info(LogManager.getHeader(context, "accept_license",
							subInfo.getSubmissionLogInfo()));
	
					assert 0 < item.getBundles().length;
					for (Bundle bundle : item.getBundles()) {
						Bitstream[] bitstreams = bundle.getBitstreams();
						for (Bitstream bitstream : bitstreams) {
							// first remove all old ones and then add new ones
							int resource_id = bitstream.getID();
							iface.detachLicenses(resource_id);
							for (String license_id : selected_license) {
								iface.attachLicense(Integer.parseInt(license_id), resource_id);
							}
						}
					}
					
					// update dc-rights
					// - for all licenses
					// - for all selected
					//
						
					
					String[] license_uris = new String[selected_license.length];
					String[] license_holders = new String[selected_license.length];
					String[] license_labels = new String[selected_license.length];
					// maybe, this is an abuse of dc.rights?
					String[] license_names = new String[selected_license.length];
					int i = 0;
					for (String license_id : selected_license) {
						for (LicenseDefinition license_def : iface.getAllLicenses()) {
							// if we find our license - use the values
							if (Integer.parseInt(license_id)==license_def.getLicenseId()) {
								license_uris[i] = license_def.getDefinition();
								license_holders[i] = license_def.getUserRegistration().getEmail();
								license_labels[i] = license_def.getLicenseLabel().getLabel();
								license_names[i] = license_def.getName();
								++i;
								break;
							}
							if (i >= selected_license.length)
								break;
						}
					}
										
					item.clearMetadata("dc", "rights", "holder", Item.ANY);
					item.clearMetadata("dc", "rights", "uri", Item.ANY);
					item.clearMetadata("dc", "rights", null, Item.ANY);
					
	            	item.clearMetadata("dc", "rights", "label", Item.ANY);
					
					//item.addMetadata(MetadataSchema.DC_SCHEMA, "rights", "holder",
					//		Item.ANY, license_holders);
					item.addMetadata(MetadataSchema.DC_SCHEMA, "rights", "uri",
							Item.ANY, license_uris);
					item.addMetadata(MetadataSchema.DC_SCHEMA, "rights", null,
							Item.ANY, license_names);
					
	            	item.addMetadata("dc", "rights", "label", Item.ANY, license_labels);					
	
					// Save changes to database
					item.update();
					
		            // For Default License between user and repo
		            EPerson submitter = context.getCurrentUser();

		            // remove any existing DSpace license (just in case the user
		            // accepted it previously)
		            item.removeDSpaceLicense();

		            // For Default License between user and repo					
		            String license = LicenseUtils.getLicenseText(context
		                    .getCurrentLocale(), subInfo.getSubmissionItem()
		                    .getCollection(), item, submitter);

		            LicenseUtils.grantLicense(context, item, license);
	
					// commit changes
					context.commit();
				}
			}

			// ehm, just simulating old behaviour
			//
		} else {

			request.setAttribute("submission.page", current_page - 1);
			int res = super.doProcessing(context, request, response, subInfo);
			request.setAttribute("submission.page", current_page);
			return res;
		}
	
		iface.closeSession();
		return STATUS_COMPLETE;
	}

	// add another page
	@Override
	public int getNumberOfPages(HttpServletRequest request,
			SubmissionInfo subInfo) throws ServletException {
		return super.getNumberOfPages(request, subInfo);
	}

	public static String getUserId(EPerson submitter) {
		return submitter.getEmail();
	}

}


