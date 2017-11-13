/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AccessConditionRest;
import org.dspace.app.rest.model.AccessConditionTypeEnum;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.authority.Choice;
import org.dspace.embargo.service.EmbargoService;
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
public class AccessConditionsConverter extends DSpaceConverter<ResourcePolicy, AccessConditionRest> {

	@Autowired
	ConfigurationService configurationService;
	
	@Override
	public AccessConditionRest fromModel(ResourcePolicy obj) {
		AccessConditionRest model = new AccessConditionRest();
		model.setType(AccessConditionTypeEnum.openaccess);
		if (obj.getGroup() != null) {
			model.setGroupUuid(obj.getGroup().getID());			
			if (Group.ADMIN.equals(obj.getGroup().getName())) {
				model.setType(AccessConditionTypeEnum.administrator);
			}
			else {
				if(obj.getStartDate()!=null) {
					model.setType(AccessConditionTypeEnum.embargo);
					model.setEndDate(obj.getStartDate());
				}
				else {
					if(obj.getEndDate()!=null) {
						model.setType(AccessConditionTypeEnum.lease);
						model.setEndDate(obj.getEndDate());
					}
				}
			}
			String networkAdministration = configurationService.getProperty("group.restricted.network.administration");
			if(StringUtils.isNotBlank(networkAdministration)) {
				if(networkAdministration.equals(obj.getGroup().getName())) {
					model.setType(AccessConditionTypeEnum.networkAdministration);
				}
			}
		}
		return model;
	}

	@Override
	public ResourcePolicy toModel(AccessConditionRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

}
