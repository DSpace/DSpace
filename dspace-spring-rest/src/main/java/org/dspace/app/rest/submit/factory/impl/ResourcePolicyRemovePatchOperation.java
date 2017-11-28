/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
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
public class ResourcePolicyRemovePatchOperation extends RemovePatchOperation<ResourcePolicyRest> {

	@Autowired
	ItemService itemService;
	
	@Autowired
	ResourcePolicyService resourcePolicyService;
	
	@Override
	void remove(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
			throws Exception {
		String[] split = path.split("/");
		String bitstreamIdx = split[0];
		String rpIdx = split[2];
		
		Item item = source.getItem();

		List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
		
		ResourcePolicy dpolicy = null;
		
		for (Bundle bb : bundle) {
			int idx = 0;
			for (Bitstream b : bb.getBitstreams()) {
				if (idx == Integer.parseInt(bitstreamIdx)) {
					List<ResourcePolicy> policies = resourcePolicyService.find(context, b, ResourcePolicy.TYPE_CUSTOM);
					int index = 0;
					for(ResourcePolicy policy : policies) {						
						Integer toDelete = Integer.parseInt(rpIdx);
						if(index == toDelete) {
							dpolicy = policy;
							b.getResourcePolicies().remove(policy);
							break;
						}
						index++;
					}
				}
				idx++;
			}
		}
		
		if(dpolicy!=null) {			
			resourcePolicyService.delete(context, dpolicy);
		}
		
	}

	@Override
	protected Class<ResourcePolicyRest[]> getClassForEvaluation() {
		return ResourcePolicyRest[].class;
	}

}
