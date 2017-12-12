/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Submission "replace" patch operation
 * 
 * The replace operation allows to replace existent information with new one.
 * Attempt to use the replace operation without a previous accepted license must
 * return an error.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class LicenseReplacePatchOperation extends ReplacePatchOperation<String> {

	@Autowired
	ItemService itemService;

	@Override
	void replace(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
			throws Exception {
		Item item = source.getItem();
		List<Bundle> bunds = itemService.getBundles(item, "LICENSE");
		Assert.notEmpty(bunds);

		EPerson submitter = context.getCurrentUser();

		// remove any existing DSpace license (just in case the user
		// accepted it previously)
		itemService.removeDSpaceLicense(context, item);

		String license = LicenseUtils.getLicenseText(context.getCurrentLocale(), source.getCollection(), item,
				submitter);

		LicenseUtils.grantLicense(context, item, license, (String) value);
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
