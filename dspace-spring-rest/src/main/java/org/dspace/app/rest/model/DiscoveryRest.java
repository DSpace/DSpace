/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

public class DiscoveryRest extends DSpaceObjectRest {

    public static final String NAME = "discovery";

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
    
}