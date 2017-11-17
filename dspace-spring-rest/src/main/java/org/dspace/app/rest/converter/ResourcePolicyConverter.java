/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.core.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converter to translate ResourcePolicy into human readable value
 * configuration.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class ResourcePolicyConverter extends DSpaceConverter<ResourcePolicy, ResourcePolicyRest> {

	@Autowired
	ResourcePolicyService resourcePolicyService;
	
	@Override
	public ResourcePolicyRest fromModel(ResourcePolicy obj) {
		
		ResourcePolicyRest model = new ResourcePolicyRest();
		
		model.setId(obj.getID());
		
		model.setName(obj.getRpName());
		model.setDescription(obj.getRpDescription());
		model.setRpType(obj.getRpType());
		
		model.setAction(resourcePolicyService.getActionText(obj));
		
		model.setStartDate(obj.getStartDate());
		model.setEndDate(obj.getEndDate());
		
		if (obj.getGroup() != null) {
			model.setGroupUUID(obj.getGroup().getID());
		}
		
		if(obj.getEPerson() != null) {
			model.setEpersonUUID(obj.getEPerson().getID());
		}
		return model;
	}

	@Override
	public ResourcePolicy toModel(ResourcePolicyRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

}
