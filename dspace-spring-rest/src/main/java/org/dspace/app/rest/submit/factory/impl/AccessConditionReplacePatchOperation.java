/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.Date;
import java.util.List;

import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * Submission "replace" operation to replace resource policies in the Bitstream
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class AccessConditionReplacePatchOperation extends ReplacePatchOperation<ResourcePolicyRest> {

	@Autowired
	BitstreamService bitstreamService;

	@Autowired
	ItemService itemService;

	@Autowired
	AuthorizeService authorizeService;

	@Autowired
	GroupService groupService;
	
	@Override
	void replace(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
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
					ResourcePolicyRest[] newAccessConditions = evaluateObject((LateObjectEvaluator) value);
					for (ResourcePolicyRest newAccessCondition : newAccessConditions) {
						String name = newAccessCondition.getPolicyType();
						Group group = groupService.find(context, newAccessCondition.getGroupUUID());
						//TODO manage error on select group
						Date startDate = null;
						Date endDate = null;
						if("embargo".equals(newAccessCondition.getPolicyType())) {
							startDate = newAccessCondition.getEndDate();
						}
						if("lease".equals(newAccessCondition.getPolicyType())) {
							endDate = newAccessCondition.getEndDate();
						}
						authorizeService.createResourcePolicy(context, b, group, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM, name, startDate, endDate);
						// TODO manage duplicate policy
					}
				}
				idx++;
			}
		}
	}

	@Override
	protected Class<ResourcePolicyRest[]> getClassForEvaluation() {
		return ResourcePolicyRest[].class;
	}

}
