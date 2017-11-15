package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

public class LicenseReplacePatchOperation extends ReplacePatchOperation<String> {

	@Autowired
	ItemService itemService;
	
	@Override
	void replace(Context context, Request currentRequest, WorkspaceItem source, String string, Object value) throws Exception {
		Item item = source.getItem();
		EPerson submitter = context.getCurrentUser();

		// remove any existing DSpace license (just in case the user
		// accepted it previously)
		itemService.removeDSpaceLicense(context, item);

		String license = LicenseUtils.getLicenseText(context.getCurrentLocale(), source.getCollection(), item,
				submitter);
		
		LicenseUtils.grantLicense(context, item, license, (String)value);
	}

	@Override
	protected Class<String[]> getClassForEvaluation() {
		return String[].class;
	}


}
