package org.dspace.app.rest.submit.factory.impl;

import java.util.Date;
import java.util.List;

import org.dspace.app.rest.model.AccessConditionRest;
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

public class AccessConditionReplacePatchOperation extends ReplacePatchOperation<AccessConditionRest> {

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

		List<Bundle> bundle = itemService.getBundles(item, "ORIGINAL");
		;
		for (Bundle bb : bundle) {
			int idx = 0;
			for (Bitstream b : bb.getBitstreams()) {
				if (idx == Integer.parseInt(split[0])) {
					authorizeService.removeAllPolicies(context, b);
					AccessConditionRest[] newAccessConditions = evaluateObject((LateObjectEvaluator) value);
					for (AccessConditionRest newAccessCondition : newAccessConditions) {
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
	protected Class<AccessConditionRest[]> getClassForEvaluation() {
		return AccessConditionRest[].class;
	}

}
