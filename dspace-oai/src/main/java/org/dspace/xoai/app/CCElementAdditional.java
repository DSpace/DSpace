/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.CCLookup;
import org.dspace.license.factory.LicenseServiceFactory;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.xoai.util.ItemUtils;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

/**
 * Utility class to build xml element to support Creative Commons information
 *
 */
public class CCElementAdditional implements XOAIItemCompilePlugin {

	private static Logger log = LogManager.getLogger(CCElementAdditional.class);

	private CreativeCommonsService creativeCommonsService;
	
	@Override
	public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {

		Element other;
		List<Element> elements = metadata.getElement();
		if (ItemUtils.getElement(elements, "others") != null) {
			other = ItemUtils.getElement(elements, "others");
		} else {
			other = ItemUtils.create("others");
		}
		String ccLicense = null;

		try {
			String licenseURL = getCreativeCommonsService().getLicenseURL(context, item);
			if (StringUtils.isNotBlank(licenseURL)) {
				CCLookup ccLookup = new CCLookup();
				ccLookup.issue(licenseURL);
				String licenseName = ccLookup.getLicenseName();
				ccLicense = licenseName + "|||" + licenseURL;
			}
		} catch (SQLException | IOException | AuthorizeException e) {
			log.error(e.getMessage(), e);
		}
		if (StringUtils.isNotBlank(ccLicense)) {
			other.getField().add(ItemUtils.createValue("cc", ccLicense));
		}
		return metadata;
	}

	public CreativeCommonsService getCreativeCommonsService() {
		if(creativeCommonsService==null) {
			creativeCommonsService = LicenseServiceFactory.getInstance().getCreativeCommonsService();
		}
		return creativeCommonsService;
	}

	public void setCreativeCommonsService(CreativeCommonsService creativeCommonsService) {
		this.creativeCommonsService = creativeCommonsService;
	}
}
