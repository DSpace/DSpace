/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.submit.accesscondition.AccessConditionOption;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Upload Section Configuration REST Resource
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class SubmissionUploadRest extends BaseObjectRest<String> {
	
	public static final String NAME = "submissionupload";
	public static final String NAME_LINK_ON_PANEL = RestModel.CONFIGURATION;
	public static final String CATEGORY = RestModel.CONFIGURATION;

	private String name;
	
	private List<SubmissionFormFieldRest> metadata;

	private List<AccessConditionOptionRest> accessConditions;
	
	private boolean required;
	
	private Long maxSize;
	
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

	public List<AccessConditionOptionRest> getAccessConditions() {
		if(accessConditions==null) {
			accessConditions = new ArrayList<>();
		}
		return accessConditions;
	}

	public void setAccessConditions(List<AccessConditionOptionRest> accessConditions) {
		this.accessConditions = accessConditions;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public Long getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(Long maxSize) {
		this.maxSize = maxSize;
	}

	public List<SubmissionFormFieldRest> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<SubmissionFormFieldRest> metadata) {
		this.metadata = metadata;
	}
}