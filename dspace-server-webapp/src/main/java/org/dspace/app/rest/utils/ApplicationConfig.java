/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * This class provides extra configuration for our Spring Boot Application
 * <p>
 * NOTE: @ComponentScan on "org.dspace.app.configuration" provides a way for other DSpace modules or plugins
 * to "inject" their own Spring configurations / subpaths into our Spring Boot webapp.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Tim Donohue
 */
@Configuration
@EnableSpringDataWebSupport
@ComponentScan( {"org.dspace.app.rest.converter", "org.dspace.app.rest.repository", "org.dspace.app.rest.utils",
        "org.dspace.app.configuration"})
public class ApplicationConfig {
    // Allowed CORS origins. Defaults to * (everywhere)
    // Can be overridden in DSpace configuration
    @Value("${rest.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    public String[] getCorsAllowedOrigins() {
        if (corsAllowedOrigins != null) {
            return corsAllowedOrigins.split("\\s*,\\s*");
        }
        return null;
    }
}
