/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.accesscondition;

import java.util.List;

import org.dspace.app.rest.model.SubmissionFormFieldRest;
import org.dspace.services.ConfigurationService;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class UploadConfiguration {
	
	private ConfigurationService configurationService;
	
	private List<SubmissionFormFieldRest> metadata;
	private List<AccessConditionOption> options;
	private Long maxSize;
	private Boolean required;
	private String name;
	
	public List<AccessConditionOption> getOptions() {
		return options;
	}

	public void setOptions(List<AccessConditionOption> options) {
		this.options = options;
	}

	public List<SubmissionFormFieldRest> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<SubmissionFormFieldRest> metadata) {
		this.metadata = metadata;
	}

	public Long getMaxSize() {
		if(maxSize==null) {
			maxSize = configurationService.getLongProperty("upload.max");
		}
		return maxSize;
	}
	
	public void setMaxSize(Long maxSize) {
		this.maxSize = maxSize;
	}

	public Boolean isRequired() {
		if(required==null) {
			required = configurationService.getBooleanProperty("webui.submit.upload.required");
		}
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
}
