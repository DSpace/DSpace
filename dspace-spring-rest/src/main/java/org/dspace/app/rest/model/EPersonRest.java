/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.List;

import org.dspace.app.rest.RestResourceController;

/**
 * The EPerson REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class EPersonRest extends DSpaceObjectRest {
	public static final String NAME = "eperson";
	public static final String CATEGORY = RestModel.EPERSON;
	private String netid;

	private Date lastActive;

	private boolean canLogIn;

	private String email;

	private boolean requireCertificate = false;

	private boolean selfRegistered = false;

	// FIXME this should be annotated with @JsonIgnore but right now only simple
	// rest resource can be embedded not list, see
	// https://jira.duraspace.org/browse/DS-3483
	private List<GroupRest> groups;

	@Override
	public String getType() {
		return NAME;
	}

	public String getNetid() {
		return netid;
	}

	public void setNetid(String netid) {
		this.netid = netid;
	}

	public Date getLastActive() {
		return lastActive;
	}

	public void setLastActive(Date lastActive) {
		this.lastActive = lastActive;
	}

	public boolean isCanLogIn() {
		return canLogIn;
	}

	public void setCanLogIn(boolean canLogIn) {
		this.canLogIn = canLogIn;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isRequireCertificate() {
		return requireCertificate;
	}

	public void setRequireCertificate(boolean requireCertificate) {
		this.requireCertificate = requireCertificate;
	}

	public boolean isSelfRegistered() {
		return selfRegistered;
	}

	public void setSelfRegistered(boolean selfRegistered) {
		this.selfRegistered = selfRegistered;
	}
	
	public List<GroupRest> getGroups() {
		return groups;
	}
	
	public void setGroups(List<GroupRest> groups) {
		this.groups = groups;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}
	
	@Override
	public Class getController() {
		return RestResourceController.class;
	}
}