/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.Filter;

import org.dspace.app.rest.filter.DSpaceRequestContextFilter;
import org.dspace.app.rest.model.hateoas.DSpaceLinkRelationProvider;
import org.dspace.app.rest.parameter.resolver.SearchFilterResolver;
import org.dspace.app.rest.utils.ApplicationConfig;
import org.dspace.app.rest.utils.DSpaceAPIRequestLoggingFilter;
import org.dspace.app.rest.utils.DSpaceConfigurationInitializer;
import org.dspace.app.rest.utils.DSpaceKernelInitializer;
import org.dspace.app.sitemap.GenerateSitemaps;
import org.dspace.app.solrdatabaseresync.SolrDatabaseResyncCli;
import org.dspace.app.util.DSpaceContextListener;
import org.dspace.utils.servlet.DSpaceWebappServletFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Define the Spring Boot Application settings itself. This class takes the place
 * of a web.xml file, and configures all Filters/Listeners as methods (see below).
 * <p>
 * NOTE: Requires a Servlet 3.0 container, e.g. Tomcat 7.0 or above.
 * <p>
 * NOTE: This extends SpringBootServletInitializer in order to allow us to build
 * a deployable WAR file with Spring Boot. See:
 * http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-create-a-deployable-war-file
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Tim Donohue
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class Application extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private ApplicationConfig configuration;

    @Scheduled(cron = "${sitemap.cron:-}")
    public void generateSitemap() throws IOException, SQLException {
        GenerateSitemaps.generateSitemapsScheduled();
    }

    @Scheduled(cron = "${solr-database-resync.cron:-}")
    public void solrDatabaseResync() throws Exception {
        SolrDatabaseResyncCli.runScheduled();
    }

    /**
     * Override the default SpringBootServletInitializer.configure() method,
     * passing it this Application class.
     * <p>
     * This is necessary to allow us to build a deployable WAR, rather than
     * always relying on embedded Tomcat.
     * <p>
     * See: http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-create-a-deployable-war-file
     *
     * @param application
     * @return
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // Pass this Application class, and our initializers for DSpace Kernel and Configuration
        // NOTE: Kernel must be initialized before Configuration
        return application.sources(Application.class)
                          .initializers(new DSpaceKernelInitializer(), new DSpaceConfigurationInitializer());
    }

    /**
     * Register the "DSpaceContextListener" so that it is loaded
     * for this Application.
     *
     * @return DSpaceContextListener
     */
    @Bean
    @Order(2)
    protected DSpaceContextListener dspaceContextListener() {
        // This listener initializes the DSpace Context object
        return new DSpaceContextListener();
    }

    /**
     * Register the DSpaceWebappServletFilter, which initializes the
     * DSpace RequestService / SessionService
     *
     * @return DSpaceWebappServletFilter
     */
    @Bean
    @Order(1)
    protected Filter dspaceWebappServletFilter() {
        return new DSpaceWebappServletFilter();
    }

    /**
     * Register the DSpaceRequestContextFilter, a Filter which checks for open
     * Context objects *after* a request has been fully processed, and closes them
     *
     * @return DSpaceRequestContextFilter
     */
    @Bean
    @Order(2)
    protected Filter dspaceRequestContextFilter() {
        return new DSpaceRequestContextFilter();
    }

    /**
     * Register the DSpaceAPIRequestLoggingFilter, a Filter that provides Mapped
     * Diagnostic Context for the DSpace Server Webapp
     *
     * @return DSpaceRequestContextFilter
     */
    @Bean
    @Order(3)
    protected Filter dspaceApiLoggingRequest() {
        return new DSpaceAPIRequestLoggingFilter();
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    @Bean
    protected LinkRelationProvider dspaceLinkRelationProvider() {
        return new DSpaceLinkRelationProvider();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {

        return new WebMvcConfigurer() {
            /**
             * Create a custom CORS mapping for the DSpace REST API (/api/ paths), based on configured allowed origins.
             * @param registry CorsRegistry
             */
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                // Get allowed origins for api and iiif endpoints.
                // The actuator endpoints are configured using management.endpoints.web.cors.* properties
                String[] corsAllowedOrigins = configuration
                    .getCorsAllowedOrigins(configuration.getCorsAllowedOriginsConfig());
                String[] iiifAllowedOrigins = configuration
                    .getCorsAllowedOrigins(configuration.getIiifAllowedOriginsConfig());

                boolean corsAllowCredentials = configuration.getCorsAllowCredentials();
                boolean iiifAllowCredentials = configuration.getIiifAllowCredentials();
                if (corsAllowedOrigins != null) {
                    registry.addMapping("/api/**").allowedMethods(CorsConfiguration.ALL)
                            // Set Access-Control-Allow-Credentials to "true" and specify which origins are valid
                            // for our Access-Control-Allow-Origin header
                            // for our Access-Control-Allow-Origin header
                            .allowCredentials(corsAllowCredentials).allowedOrigins(corsAllowedOrigins)
                            // Allow list of request preflight headers allowed to be sent to us from the client
                            .allowedHeaders("Accept", "Authorization", "Content-Type", "Origin", "X-On-Behalf-Of",
                                "X-Requested-With", "X-XSRF-TOKEN", "X-CORRELATION-ID", "X-REFERRER")
                            // Allow list of response headers allowed to be sent by us (the server) to the client
                            .exposedHeaders("Authorization", "DSPACE-XSRF-TOKEN", "Location", "WWW-Authenticate");
                }
                if (iiifAllowedOrigins != null) {
                    registry.addMapping("/iiif/**").allowedMethods(CorsConfiguration.ALL)
                            // Set Access-Control-Allow-Credentials to "true" and specify which origins are valid
                            // for our Access-Control-Allow-Origin header
                            .allowCredentials(iiifAllowCredentials).allowedOrigins(iiifAllowedOrigins)
                            // Allow list of request preflight headers allowed to be sent to us from the client
                            .allowedHeaders("Accept", "Authorization", "Content-Type", "Origin", "X-On-Behalf-Of",
                                "X-Requested-With", "X-XSRF-TOKEN", "X-CORRELATION-ID", "X-REFERRER")
                            // Allow list of response headers allowed to be sent by us (the server) to the client
                            .exposedHeaders("Authorization", "DSPACE-XSRF-TOKEN", "Location", "WWW-Authenticate");
                }
            }

            /**
             * Add a ViewController for the root path, to load HAL Browser
             * @param registry ViewControllerRegistry
             */
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                // Ensure accessing the root path will load the index.html of the HAL Browser
                registry.addViewController("/").setViewName("forward:/index.html");
            }

            /**
             * Add a new ResourceHandler to allow us to use WebJars.org to pull in web dependencies
             * dynamically for HAL Browser, etc.
             * @param registry ResourceHandlerRegistry
             */
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // First, "mount" the Hal Browser resources at the /browser path
                // NOTE: the hal-browser directory uses the version of the Hal browser, so this needs to be synced
                // with the org.webjars.hal-browser version in the POM
                registry
                    .addResourceHandler("/browser/**")
                    .addResourceLocations("/webjars/hal-browser/ad9b865/");

                // Make all other Webjars available off the /webjars path
                registry
                    .addResourceHandler("/webjars/**")
                    .addResourceLocations("/webjars/");
            }

            @Override
            public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> argumentResolvers) {
                argumentResolvers.add(new SearchFilterResolver());
            }
        };
    }
}
