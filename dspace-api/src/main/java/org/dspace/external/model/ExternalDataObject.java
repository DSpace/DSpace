/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.model;

import java.util.LinkedList;
import java.util.List;

import org.dspace.content.dto.MetadataValueDTO;

/**
 * The representation model object for external data
 */
public class ExternalDataObject {

    /**
     * This field determines the ID for the ExternalDataObject
     */
    private String id;
    /**
     * This field determines the value for the ExternalDataObject
     */
    private String value;
    /**
     * This field determines where the ExternalData came from
     */
    private String source;
    /**
     * The list of Metadata values. These our MetadataValueDTO because they won't exist in the DB
     */
    private List<MetadataValueDTO> metadata = new LinkedList<>();
    /**
     * The display value of the ExternalDataObject
     */
    private String displayValue;

    /**
     * Default constructor
     */
    public ExternalDataObject() {

    }

    /**
     * Constructor for the ExternalDataObject with as parameter the source of where it came from
     * @param source    The source where the ExternalDataObject came from
     */
    public ExternalDataObject(String source) {
        this.source = source;
    }

    /**
     * Generic getter for the source
     * @return  The source
     */
    public String getSource() {
        return source;
    }

    /**
     * Generic setter for the source
     * @param source    The source to be set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Generic getter for the Metadata
     * @return  The metadata
     */
    public List<MetadataValueDTO> getMetadata() {
        return metadata;
    }

    /**
     * Generic setter for the Metadata
     * @param metadata  The metadata to be set
     */
    public void setMetadata(List<MetadataValueDTO> metadata) {
        this.metadata = metadata;
    }

    /**
     * This method will add a Metadata value to the list of metadata values
     * @param metadataValueDTO The metadatavalue to be added
     */
    public void addMetadata(MetadataValueDTO metadataValueDTO) {
        if (metadata == null) {
            metadata = new LinkedList<>();
        }
        metadata.add(metadataValueDTO);
    }

    /**
     * Generic getter for the display value
     * @return  The display value
     */
    public String getDisplayValue() {
        return displayValue;
    }

    /**
     * Generic setter for the display value
     * @param displayValue  The display value to be set
     */
    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    /**
     * Generic getter for the ID
     * @return  The id
     */
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the ID
     * @param id    The id to be set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generic getter for the value
     * @return the value value of this ExternalDataObject
     */
    public String getValue() {
        return value;
    }

    /**
     * Generic setter for the value
     * @param value   The value to be set on this ExternalDataObject
     */
    public void setValue(String value) {
        this.value = value;
    }
}
