package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

public class LicenseRemovePatchOperation extends RemovePatchOperation<String> {

	@Autowired
	ItemService itemService;
	
	@Override
	void remove(Context context, Request currentRequest, WorkspaceItem source, String string, Object value) throws Exception {
		Item item = source.getItem();
		itemService.removeDSpaceLicense(context, item);
	}

	@Override
	protected Class<String[]> getClassForEvaluation() {
		return String[].class;
	}

}
