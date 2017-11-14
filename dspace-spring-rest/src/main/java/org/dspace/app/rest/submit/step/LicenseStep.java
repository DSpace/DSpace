/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.step.DataLicense;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Request;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * License step for DSpace Spring Rest. Expose the license information about the in progress submission.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class LicenseStep extends org.dspace.submit.step.LicenseStep implements AbstractRestProcessingStep {

	private static final String DC_RIGHTS_DATE = "dc.rights.date";

	@Override
	public DataLicense getData(WorkspaceItem obj, SubmissionStepConfig config) throws Exception {
		DataLicense result = new DataLicense();
		Bitstream bitstream = bitstreamService.getBitstreamByName(obj.getItem(), Constants.LICENSE_BUNDLE_NAME, Constants.LICENSE_BITSTREAM_NAME);
		if(bitstream!=null) {
			String acceptanceDate = bitstreamService.getMetadata(bitstream, DC_RIGHTS_DATE);
			result.setAcceptanceDate(acceptanceDate);
			result.setUrl(configurationService.getProperty("dspace.url")+"/api/"+BitstreamRest.CATEGORY +"/"+ English.plural(BitstreamRest.NAME) + "/" + bitstream.getID() + "/content");
		}
		return result;
	}

	@Override
	public void doPatchProcessing(Context context, Request currentRequest, WorkspaceItem source, String operation,
			String target, String index, Object value) throws Exception {
        
		if("acceptanceDate".equals(target)) {
			Item item = source.getItem();
			EPerson submitter = context.getCurrentUser();

			switch (operation) {
			case "replace":
				// remove any existing DSpace license (just in case the user
				// accepted it previously)
				itemService.removeDSpaceLicense(context, item);

				String license = LicenseUtils.getLicenseText(context.getCurrentLocale(), source.getCollection(), item,
						submitter);
				
				LicenseUtils.grantLicense(context, item, license, (String)value);
				break;
			case "remove":
				itemService.removeDSpaceLicense(context, item);
				break;
			default:
				throw new RuntimeException("Operation " + operation + " not yet implemented!");
			}
		}
	}
}
