/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ConfigurationValueRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This class serves as a wrapper class to wrap the ConfigurationValueRest into a HAL resource
 */
@RelNameDSpaceResource(ConfigurationValueRest.NAME)
public class ConfigurationValueResource extends HALResource<ConfigurationValueRest> {

    public ConfigurationValueResource(ConfigurationValueRest configurationValueRest){
        super(configurationValueRest);
    }

}
