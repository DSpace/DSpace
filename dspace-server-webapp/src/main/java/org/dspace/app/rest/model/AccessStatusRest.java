/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * The Access Status REST Resource.
 */
public class AccessStatusRest implements RestModel {
    public static final String NAME = "accessStatus";
    public static final String PLURAL_NAME = NAME;

    String status;

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    /**
     * The plural name is the same as the singular name
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    public AccessStatusRest() {
        setStatus(null);
    }

    public AccessStatusRest(String status) {
        setStatus(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
