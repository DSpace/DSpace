/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import org.dspace.app.rest.RestResourceController;

/**
 * The Access Condition (ResourcePolicy) REST Resource
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class DefaultAccessConditionRest extends BaseObjectRest<Integer> {

	public static final String NAME = "defaultBitstreamsPolicies";
	public static final String CATEGORY = RestModel.CORE;

	private AccessConditionTypeEnum type;
	
	private UUID groupUuid;
	
	private Date endDate;
	
	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public String getType() {
		return type.toString();
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	public UUID getGroupUuid() {
		return groupUuid;
	}

	public void setGroupUuid(UUID groupUuid) {
		this.groupUuid = groupUuid;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void setType(AccessConditionTypeEnum type) {
		this.type = type;
	}

}
