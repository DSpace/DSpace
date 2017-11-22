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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Input Form REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class SubmissionFormRest extends BaseObjectRest<String> {
	public static final String NAME = "submissionform";
	public static final String NAME_LINK_ON_PANEL = RestModel.CONFIGURATION;
	public static final String CATEGORY = RestModel.CONFIGURATION;

	private String name;
	
	private List<SubmissionFormFieldRest> fields;

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
	
	@Override
	public String getType() {
		return NAME;
	}
	
	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}

	public List<SubmissionFormFieldRest> getFields() {
		return fields;
	}

	public void setFields(List<SubmissionFormFieldRest> fields) {
		this.fields = fields;
	}
}