/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import javax.annotation.Resource;

import org.dspace.app.rest.model.RestModel;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible of exposing configuration properties
 */
@Component(RestModel.CONFIGURATION + ".properties")
public class ConfigurationRepository {
    @Autowired
    private ConfigurationService configurationService;

    @Resource(name = "exposedConfigurationProperties")
    private ArrayList<String> exposedProperties;

    /**
     * Gets the value of a configuration property if it is exposed via REST
     *
     * @param property
     * @return
     */
    public String[] getValue(String property) {
        String[] propertyValues = configurationService.getArrayProperty(property);

        if (!exposedProperties.contains(property) || propertyValues.length == 0) {
            throw new ResourceNotFoundException("No such configuration property" + property);
        }

        return propertyValues;
    }
}
