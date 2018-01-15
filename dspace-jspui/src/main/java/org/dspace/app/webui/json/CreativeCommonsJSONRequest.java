/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.license.CCLicenseField;
import org.dspace.license.CCLookup;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Luigi Andrea Pascarelli
 */
public class CreativeCommonsJSONRequest extends JSONRequest {

	private static Logger log = Logger.getLogger(CreativeCommonsJSONRequest.class);

	@Override
	public void doJSONRequest(Context context, HttpServletRequest req, HttpServletResponse resp)
			throws AuthorizeException, IOException {
		Gson json = new Gson();
		String selectedLicense = req.getParameter("license");

		List<CCLicenseField> dto = new ArrayList<CCLicenseField>();
		
		if (StringUtils.isNotBlank(selectedLicense)) {
			CCLookup cclookup = new CCLookup();
			
	        String ccLocale = ConfigurationManager.getProperty("cc.license.locale");
	        /** Default locale to 'en' */
	        ccLocale = (StringUtils.isNotBlank(ccLocale)) ? ccLocale : "en";
	        
			// output the license fields chooser for the license class type
			if (cclookup.getLicenseFields(selectedLicense, ccLocale) == null) {
				// do nothing
			} else {
				Collection<CCLicenseField> outerIterator = cclookup.getLicenseFields(selectedLicense, ccLocale);
				for(CCLicenseField cclicensefield : outerIterator) {
					if (cclicensefield.getId().equals("jurisdiction"))
						continue;
					dto.add(cclicensefield);
				}
			}
		}

		JsonElement tree = json.toJsonTree(dto);
		JsonObject jo = new JsonObject();
		jo.add("result", tree);
		resp.getWriter().write(jo.toString());
	}

}
