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
        "org.dspace.app.configuration", "org.dspace.app.rest.link", "org.dspace.app.rest.converter.factory"})
public class ApplicationConfig {
    // Allowed CORS origins ("Access-Control-Allow-Origin" header)
    // Can be overridden in DSpace configuration
    @Value("${rest.cors.allowed-origins}")
    private String[] corsAllowedOrigins;

    // Whether to allow credentials (cookies) in CORS requests ("Access-Control-Allow-Credentials" header)
    // Defaults to true. Can be overridden in DSpace configuration
    @Value("${rest.cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    // Configured User Interface URL (default: http://localhost:4000)
    @Value("${dspace.ui.url:http://localhost:4000}")
    private String uiURL;

    /**
     * Return the array of allowed origins (client URLs) for the CORS "Access-Control-Allow-Origin" header
     * Used by Application class
     * @return Array of URLs
     */
    public String[] getCorsAllowedOrigins() {
        // Use "rest.cors.allowed-origins" if configured. Otherwise, default to the "dspace.ui.url" setting.
        if (corsAllowedOrigins != null) {
            return corsAllowedOrigins;
        } else if (uiURL != null) {
            return new String[] {uiURL};
        }
        return null;
    }

    /**
     * Return whether to allow credentials (cookies) on CORS requests. This is used to set the
     * CORS "Access-Control-Allow-Credentials" header in Application class.
     * @return true or false
     */
    public boolean getCorsAllowCredentials() {
        return corsAllowCredentials;
    }
}
