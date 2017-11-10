/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class AccessConditionOption {

	private String type;
	
	private String groupName;
	
	private Boolean hasDate;
	
	private String dateLimit;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Boolean getHasDate() {
		return hasDate;
	}

	public void setHasDate(Boolean hasDate) {
		this.hasDate = hasDate;
	}

	public String getDateLimit() {
		return dateLimit;
	}

	public void setDateLimit(String dateLimit) {
		this.dateLimit = dateLimit;
	}
	
}
