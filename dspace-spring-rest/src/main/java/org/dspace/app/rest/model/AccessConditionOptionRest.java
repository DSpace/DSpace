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

/**
 * The Access Condition (ResourcePolicy) REST Resource
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class AccessConditionOptionRest {

	private String policyType;
	
	private UUID groupUUID;
	
	private UUID selectGroupUUID;
	
	private boolean hasDate;
	
	private Date maxEndDate;

	public UUID getGroupUUID() {
		return groupUUID;
	}

	public void setGroupUUID(UUID groupUuid) {
		this.groupUUID = groupUuid;
	}

	public void setPolicyType(String type) {
		this.policyType = type;
	}

	public UUID getSelectGroupUUID() {
		return selectGroupUUID;
	}

	public void setSelectGroupUUID(UUID selectGroupUuid) {
		this.selectGroupUUID = selectGroupUuid;
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

	public String getPolicyType() {
		return policyType;
	}

}
