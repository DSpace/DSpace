/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

import java.util.List;
import javax.inject.Inject;

import org.dspace.services.ConfigurationService;

/**
 * A collection of conditions to be met when uploading Bitstreams.
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class UploadConfiguration {

    private final ConfigurationService configurationService;

    private String metadataDefinition;
    private List<AccessConditionOption> options;
    private Long maxSize;
    private Boolean required;
    private String name;

    /**
     * Construct a bitstream uploading configuration.
     * @param configurationService DSpace configuration provided by the DI container.
     */
    @Inject
    public UploadConfiguration(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * The list of access restriction types from which a submitter may choose.
     * @return choices for restricting access to Bitstreams.
     */
    public List<AccessConditionOption> getOptions() {
        return options;
    }

    /**
     * Set the list of access restriction types from which to choose.
     * Required.  May be empty.
     * @param options choices for restricting access to Bitstreams.
     */
    public void setOptions(List<AccessConditionOption> options) {
        this.options = options;
    }

    /**
     * Name of the submission form to which these conditions are attached.
     * @return the form's name.
     */
    public String getMetadata() {
        return metadataDefinition;
    }

    /**
     * Name the submission form to which these conditions are attached.
     * @param metadata the form's name.
     */
    public void setMetadata(String metadata) {
        this.metadataDefinition = metadata;
    }

    /**
     * Limit on the maximum size of an uploaded Bitstream.
     * @return maximum upload size in bytes.
     */
    public Long getMaxSize() {
        if (maxSize == null) {
            maxSize = configurationService.getLongProperty("upload.max");
        }
        return maxSize;
    }

    /**
     * Limit the maximum size of an uploaded Bitstream.
     * @param maxSize maximum upload size in bytes.
     */
    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Is at least one Bitstream required when submitting a new Item?
     * @return true if a Bitstream is required.
     */
    public Boolean isRequired() {
        if (required == null) {
            //defaults to true
            //don't store a local copy of the configuration property
            return configurationService.getBooleanProperty("webui.submit.upload.required", true);
        }
        return required;
    }

    /**
     * Is at least one Bitstream required when submitting a new Item?
     * @param required true if a Bitstream is required.
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * The unique name of this configuration.
     * @return configuration's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Give this configuration a unique name.  Required.
     * @param name configuration's name.
     */
    public void setName(String name) {
        this.name = name;
    }
}
