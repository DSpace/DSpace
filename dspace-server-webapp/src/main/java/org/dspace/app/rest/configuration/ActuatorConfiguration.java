/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.configuration;

import java.util.Arrays;

import org.dspace.app.rest.DiscoverableEndpointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.hateoas.Link;

/**
 * Configuration class related to the actuator endpoints.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Configuration
public class ActuatorConfiguration {

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @EventListener(ApplicationReadyEvent.class)
    public void registerActuatorEndpoints() {
        discoverableEndpointsService.register(this, Arrays.asList(new Link(actuatorBasePath, "actuator")));
    }
}
