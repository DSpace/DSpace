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
 * The Access Condition REST Resource. It is intent to be an human or REST
 * client understandable representation of the DSpace ResourcePolicy.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class ResourcePolicyRest implements RestModel {

	public static final String NAME = "accessCondition";

	private String policyType;
	
	private UUID groupUUID;
	
	private Date endDate;
	
	public UUID getGroupUUID() {
		return groupUUID;
	}

	public void setGroupUUID(UUID groupUuid) {
		this.groupUUID = groupUuid;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void setPolicyType(String type) {
		this.policyType = type;
	}
	
	public String getPolicyType() {
		return policyType;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
}
