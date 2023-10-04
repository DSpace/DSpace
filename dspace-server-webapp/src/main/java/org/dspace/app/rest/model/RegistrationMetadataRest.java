/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegistrationMetadataRest extends MetadataValueRest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String overrides;

    public RegistrationMetadataRest(String value, String overrides) {
        super();
        this.value = value;
        this.overrides = overrides;
    }

    public RegistrationMetadataRest(String value) {
        this(value, null);
    }

    public String getOverrides() {
        return overrides;
    }

    public void setOverrides(String overrides) {
        this.overrides = overrides;
    }
}
