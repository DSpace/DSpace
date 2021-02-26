/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

import java.util.List;

import org.dspace.services.ConfigurationService;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class UploadConfiguration {

    private ConfigurationService configurationService;

    private String metadataDefinition;
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

    public String getMetadata() {
        return metadataDefinition;
    }

    public void setMetadata(String metadata) {
        this.metadataDefinition = metadata;
    }

    public Long getMaxSize() {
        if (maxSize == null) {
            maxSize = configurationService.getLongProperty("upload.max");
        }
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public Boolean isRequired() {
        if (required == null) {
            //defaults to true
            //don't store a local copy of the configuration property
            return configurationService.getBooleanProperty("webui.submit.upload.required", true);
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
