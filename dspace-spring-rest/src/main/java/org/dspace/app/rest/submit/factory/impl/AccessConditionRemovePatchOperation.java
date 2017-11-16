/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.app.rest.model.AccessConditionRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "remove" operation to remove resource policies from the Bitstream 
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class AccessConditionRemovePatchOperation extends RemovePatchOperation<AccessConditionRest> {

	@Autowired
	ItemService itemService;
	
	@Autowired
	AuthorizeService authorizeService;
	
	@Override
	void remove(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
			throws Exception {
		String[] split = path.split("/");
		Item item = source.getItem();

		List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
		;
		for (Bundle bb : bundle) {
			int idx = 0;
			for (Bitstream b : bb.getBitstreams()) {
				if (idx == Integer.parseInt(split[0])) {
					authorizeService.removeAllPolicies(context, b);
				}
				idx++;
			}
		}
		
	}

	@Override
	protected Class<AccessConditionRest[]> getClassForEvaluation() {
		return AccessConditionRest[].class;
	}

}
