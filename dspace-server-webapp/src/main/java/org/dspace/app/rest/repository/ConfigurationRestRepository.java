/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.PropertyRest;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for exposing configuration properties
 */
@Component(PropertyRest.CATEGORY + "." + PropertyRest.NAME)
public class ConfigurationRestRepository extends DSpaceRestRepository<PropertyRest, String> {

    private ConfigurationService configurationService;
    private List<String> exposedProperties;

    @Autowired
    public ConfigurationRestRepository(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.exposedProperties = Arrays.asList(configurationService.getArrayProperty("rest.properties.exposed"));
    }

    /**
     * Gets the value of a configuration property if it is exposed via REST
     *
     * Example:
     * <pre>
     * {@code
     * curl http://<dspace.server.url>/api/config/properties/google.analytics.key
     *  -XGET \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...'
     * }
     * </pre>
     *
     * @param property the name of the property to check for
     * @return the property value
     */
    @Override
    @PreAuthorize("permitAll()")
    public PropertyRest findOne(Context context, String property) {
        if (!exposedProperties.contains(property) || !configurationService.hasProperty(property)) {
            throw new ResourceNotFoundException("No such configuration property: " + property);
        }

        String[] propertyValues = configurationService.getArrayProperty(property);

        PropertyRest propertyRest = new PropertyRest();
        propertyRest.setName(property);
        propertyRest.setValues(Arrays.asList(propertyValues));

        return propertyRest;
    }

    @Override
    public Page<PropertyRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed", "");
    }

    /**
     * Gets the value of all configuration properties exposed via REST
     * @param pageable pagination information
     * @return all configuration properties as an array
     */
    @SearchRestMethod(name = "exposed")
    public Page<PropertyRest> findAllExposed(Pageable pageable) {
        List<PropertyRest> restProperties = new ArrayList<>();
        int total = this.exposedProperties.size();

        for (String property: this.exposedProperties) {
            if (!configurationService.hasProperty(property)) {
                continue;
            }
            String[] propertyValues = configurationService.getArrayProperty(property);
            PropertyRest propertyRest = new PropertyRest();
            propertyRest.setName(property);
            propertyRest.setValues(Arrays.asList(propertyValues));

            restProperties.add(propertyRest);
        }

        return new PageImpl<>(restProperties, pageable, total);
    }

    @Override
    public Class<PropertyRest> getDomainClass() {
        return PropertyRest.class;
    }
}
