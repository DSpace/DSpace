/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.ExternalSourcesRestController;

/**
 * This class serves as a REST representation for an entry of external data
 */
public class ExternalSourceEntryRest extends BaseObjectRest<String> {

    public static final String NAME = "externalSourceEntry";
    public static final String PLURAL_NAME = "externalSourceEntries";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return ExternalSourcesRestController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    private String id;
    private String display;
    private String value;
    private String externalSource;
    private MetadataRest metadata = new MetadataRest();

    /**
     * Generic getter for the id
     * @return the id value of this ExternalSourceEntryRest
     */
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this ExternalSourceEntryRest
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generic getter for the display
     * @return the display value of this ExternalSourceEntryRest
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Generic setter for the display
     * @param display   The display to be set on this ExternalSourceEntryRest
     */
    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * Generic getter for the value
     * @return the value value of this ExternalSourceEntryRest
     */
    public String getValue() {
        return value;
    }

    /**
     * Generic setter for the value
     * @param value   The value to be set on this ExternalSourceEntryRest
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Generic getter for the externalSource
     * @return the externalSource value of this ExternalSourceEntryRest
     */
    public String getExternalSource() {
        return externalSource;
    }

    /**
     * Generic setter for the externalSource
     * @param externalSource   The externalSource to be set on this ExternalSourceEntryRest
     */
    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    /**
     * Generic getter for the metadata
     * @return the metadata value of this ExternalSourceEntryRest
     */
    public MetadataRest getMetadata() {
        return metadata;
    }

    /**
     * Generic setter for the metadata
     * @param metadata   The metadata to be set on this ExternalSourceEntryRest
     */
    public void setMetadata(MetadataRest metadata) {
        this.metadata = metadata;
    }
}
