/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Group REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class GroupRest extends DSpaceObjectRest {
	public static final String NAME = "group";
	
	public static final String CATEGORY = RestModel.EPERSON;

	private String name;

	private boolean permanent;

	// FIXME this should be annotated with @JsonIgnore but right now only simple
	// rest resource can be embedded not list, see
	// https://jira.duraspace.org/browse/DS-3483
	private List<GroupRest> groups;

	@Override
	public String getCategory() {
		return CATEGORY;
	}
	
	@Override
	public String getType() {
		return NAME;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isPermanent() {
		return permanent;
	}

	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}

	public List<GroupRest> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupRest> groups) {
		this.groups = groups;
	}

	@Override
	@JsonIgnore
	public Class getController() {
		return RestResourceController.class;
	}
}