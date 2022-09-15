/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of IdentifierRest REST resource, representing some DSpace identifier
 * for use with the REST API
 *
 * @author Kim Shepherd <kim@shepherd.nz>
 */
public class IdentifierRest implements RestModel {

    // Set names used in component wiring
    public static final String NAME = "identifier";
    public static final String PLURAL_NAME = "identifiers";
    private String value;
    private String identifierType;
    private String identifierStatus;

    // Empty constructor
    public IdentifierRest() {
    }

    public IdentifierRest(String value, String identifierType, String identifierStatus) {
        this.value = value;
        this.identifierType = identifierType;
        this.identifierStatus = identifierStatus;
    }

    // Return name for getType()
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getIdentifierStatus() {
        return identifierStatus;
    }

    public void setIdentifierStatus(String identifierStatus) {
        this.identifierStatus = identifierStatus;
    }
}
