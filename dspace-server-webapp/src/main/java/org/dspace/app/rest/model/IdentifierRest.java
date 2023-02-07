/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * Implementation of IdentifierRest REST resource, representing some DSpace identifier
 * for use with the REST API
 *
 * @author Kim Shepherd <kim@shepherd.nz>
 */
public class IdentifierRest extends BaseObjectRest<String> implements RestModel {

    // Set names used in component wiring
    public static final String NAME = "identifier";
    public static final String PLURAL_NAME = "identifiers";
    private String value;
    private String identifierType;
    private String identifierStatus;

    // Empty constructor
    public IdentifierRest() {
    }

    /**
     * Constructor that takes a value, type and status for an identifier
     * @param value the identifier value         eg. https://doi.org/123/234
     * @param identifierType identifier type     eg. doi
     * @param identifierStatus identifier status eg. TO_BE_REGISTERED
     */
    public IdentifierRest(String value, String identifierType, String identifierStatus) {
        this.value = value;
        this.identifierType = identifierType;
        this.identifierStatus = identifierStatus;
    }

    /**
     * Return name for getType() - this is the section name
     * and not the type of identifier, see: identifierType string
     * @return
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    /**
     * Get the identifier value eg full DOI URL
     * @return identifier value eg. https://doi.org/123/234
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the identifier value
     * @param value identifier value, eg. https://doi.org/123/234
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get type of identifier eg 'doi' or 'handle'
     * @return  type string
     */
    public String getIdentifierType() {
        return identifierType;
    }

    /**
     * Set type of identifier
     * @param identifierType type string eg 'doi'
     */
    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    /**
     * Get status of identifier, if relevant
     * @return identifierStatus eg. null or TO_BE_REGISTERED
     */
    public String getIdentifierStatus() {
        return identifierStatus;
    }

    /**
     * Set status of identifier, if relevant
     * @param identifierStatus eg. null or TO_BE_REGISTERED
     */
    public void setIdentifierStatus(String identifierStatus) {
        this.identifierStatus = identifierStatus;
    }

    @Override
    public String getCategory() {
        return "pid";
    }

    @Override
    public String getId() {
        return getValue();
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }
}
