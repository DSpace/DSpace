/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.springframework.stereotype.Component;

/**
 * Converter to convert from {@link DiscoveryConfiguration} objects to {@link DiscoveryConfigurationRest} objects.
 */
@Component
public class DiscoveryConfigurationConverter
    implements DSpaceConverter<DiscoveryConfiguration, DiscoveryConfigurationRest> {

    @Override
    public DiscoveryConfigurationRest convert(DiscoveryConfiguration configuration, Projection projection) {
        DiscoveryConfigurationRest discoveryConfigurationRest = new DiscoveryConfigurationRest();

        discoveryConfigurationRest.setId(configuration.getId());
        discoveryConfigurationRest.setProjection(projection);

        return discoveryConfigurationRest;
    }

    public Class<DiscoveryConfiguration> getModelClass() {
        return DiscoveryConfiguration.class;
    }
}
