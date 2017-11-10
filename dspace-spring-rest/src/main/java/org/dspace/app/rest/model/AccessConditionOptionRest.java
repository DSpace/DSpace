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
public class AccessConditionOptionRest {

	private AccessConditionTypeEnum type;
	
	private UUID groupUuid;
	
	private UUID selectGroupUuid;
	
	private boolean hasDate;
	
	private Date maxEndDate;

	public UUID getGroupUuid() {
		return groupUuid;
	}

	public void setGroupUuid(UUID groupUuid) {
		this.groupUuid = groupUuid;
	}

	public void setType(AccessConditionTypeEnum type) {
		this.type = type;
	}

	public UUID getSelectGroupUuid() {
		return selectGroupUuid;
	}

	public void setSelectGroupUuid(UUID selectGroupUuid) {
		this.selectGroupUuid = selectGroupUuid;
	}

	public boolean isHasDate() {
		return hasDate;
	}

	public void setHasDate(boolean hasDate) {
		this.hasDate = hasDate;
	}

	public Date getMaxEndDate() {
		return maxEndDate;
	}

	public void setMaxEndDate(Date maxEndDate) {
		this.maxEndDate = maxEndDate;
	}

	public AccessConditionTypeEnum getType() {
		return type;
	}

}
