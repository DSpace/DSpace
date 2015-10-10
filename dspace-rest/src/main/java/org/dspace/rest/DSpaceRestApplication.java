package org.dspace.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.web.filter.RequestContextFilter;

public class DSpaceRestApplication extends ResourceConfig {

    public DSpaceRestApplication() {
        register(JacksonFeature.class);
        packages("org.dspace.rest");
    }
}
