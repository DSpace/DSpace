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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Submission Definition REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */


public class SubmissionDefinitionRest extends BaseObjectRest<String> {
	public static final String NAME = "submissiondefinition";
	public static final String CATEGORY = RestModel.CONFIGURATION;

	private String name;
	
	@JsonProperty(value="isDefault")
	private boolean defaultConf;
	
	private List<SubmissionSectionRest> panels;

	@Override
	public String getId() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setPanels(List<SubmissionSectionRest> panels) {
		this.panels = panels;
	}
	
	@LinkRest(name=SubmissionSectionRest.ATTRIBUTE_NAME, linkClass = SubmissionSectionRest.class)
	@JsonIgnore
	public List<SubmissionSectionRest> getPanels() {
		return panels;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
	
	public void setDefaultConf(boolean isDefault) {
		this.defaultConf = isDefault;
	}
	
	public boolean isDefaultConf() {
		return defaultConf;
	}
	
	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}
}