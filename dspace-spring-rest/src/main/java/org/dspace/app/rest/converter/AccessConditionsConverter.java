/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DefaultAccessConditionRest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



/**
 * Converter to translate ResourcePolicy into human readable value configuration.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class AccessConditionsConverter extends DSpaceConverter<ResourcePolicy, DefaultAccessConditionRest> {

	@Autowired
	ConfigurationService configurationService;
	
	@Override
	public DefaultAccessConditionRest fromModel(ResourcePolicy obj) {
		DefaultAccessConditionRest model = new DefaultAccessConditionRest();
		model.setPolicyType("openaccess");
		if (obj.getGroup() != null) {
			model.setGroupUuid(obj.getGroup().getID());			
			if (Group.ADMIN.equals(obj.getGroup().getName())) {
				model.setPolicyType("administrator");
			}
			else {
				if(obj.getStartDate()!=null) {
					model.setPolicyType("embargo");
					model.setEndDate(obj.getStartDate());
				}
				else {
					if(obj.getEndDate()!=null) {
						model.setPolicyType("lease");
						model.setEndDate(obj.getEndDate());
					}
				}
			}
		}
		return model;
	}

	@Override
	public ResourcePolicy toModel(DefaultAccessConditionRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

}
