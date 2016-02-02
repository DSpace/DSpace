/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resourcepolicy")
public class ResourcePolicy{
	
	public enum Action {
		READ, WRITE, DELETE;
	}
	
	private Integer id;
	private Action action;
	private String epersonId;   //UUID
	private String groupId;     //UUID
	private String resourceId;  //UUID
	private String resourceType;
	private String rpDescription;
	private String rpName;
	private String rpType;
	private Date startDate;
	private Date endDate;
	
	public ResourcePolicy() {}
	
	public ResourcePolicy(org.dspace.authorize.ResourcePolicy dspacePolicy) {
		this.id = dspacePolicy.getID();
		
		switch(dspacePolicy.getAction()) {
		case org.dspace.core.Constants.READ:
			this.action = Action.READ;
			break;
		case org.dspace.core.Constants.WRITE:
			this.action = Action.WRITE;
			break;
		case org.dspace.core.Constants.DELETE:
			this.action = Action.DELETE;
			break;
		}

		this.epersonId = dspacePolicy.getEPerson().getID().toString();
		this.groupId = dspacePolicy.getGroup().getID().toString();
		this.resourceId = dspacePolicy.getdSpaceObject().getID().toString();
		this.rpDescription = dspacePolicy.getRpDescription();
		this.rpName = dspacePolicy.getRpName();
		this.rpType = dspacePolicy.getRpType();
		this.startDate = dspacePolicy.getStartDate();
		this.endDate = dspacePolicy.getEndDate();
		switch(dspacePolicy.getdSpaceObject().getType()) {
		case org.dspace.core.Constants.BITSTREAM:
			this.resourceType = "bitstream";
			break;
		case org.dspace.core.Constants.ITEM:
			this.resourceType = "item";
			break;
		case org.dspace.core.Constants.COLLECTION:
			this.resourceType = "collection";
			break;
		case org.dspace.core.Constants.COMMUNITY:
			this.resourceType = "community";
			break;
		case org.dspace.core.Constants.BUNDLE:
			this.resourceType = "bundle";
			break;
		default:
			this.resourceType = "";
			break;
		}
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Action getAction() {
		return action;
	}
	
	@JsonIgnore
	public int getActionInt(){
		switch(action) {
		case READ:
			return org.dspace.core.Constants.READ;
		case WRITE:
			return org.dspace.core.Constants.WRITE;
		case DELETE:
			return org.dspace.core.Constants.DELETE;
		}
		return org.dspace.core.Constants.READ;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getEpersonId() {
		return epersonId;
	}

	public void setEpersonId(String epersonId) {
		this.epersonId = epersonId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getRpDescription() {
		return rpDescription;
	}

	public void setRpDescription(String rpDescription) {
		this.rpDescription = rpDescription;
	}

	public String getRpName() {
		return rpName;
	}

	public void setRpName(String rpName) {
		this.rpName = rpName;
	}

	public String getRpType() {
		return rpType;
	}

	public void setRpType(String rpType) {
		this.rpType = rpType;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
}
