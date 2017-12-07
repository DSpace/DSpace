/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission License "remove" patch operation
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class LicenseRemovePatchOperation extends RemovePatchOperation<String> {

	@Autowired
	ItemService itemService;
	
	@Override
	void remove(Context context, Request currentRequest, WorkspaceItem source, String string, Object value) throws Exception {
		Item item = source.getItem();
		itemService.removeDSpaceLicense(context, item);
	}


	@Override
	protected Class<String[]> getArrayClassForEvaluation() {
		return String[].class;
	}

	@Override
	protected Class<String> getClassForEvaluation() {
		return String.class;
	}
}
