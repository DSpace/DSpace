/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;
import javax.servlet.Filter;

import org.dspace.app.rest.filter.DSpaceRequestContextFilter;
import org.dspace.app.rest.model.hateoas.DSpaceRelProvider;
import org.dspace.app.rest.parameter.resolver.SearchFilterResolver;
import org.dspace.app.rest.utils.ApplicationConfig;
import org.dspace.app.rest.utils.DSpaceConfigurationInitializer;
import org.dspace.app.rest.utils.DSpaceKernelInitializer;
import org.dspace.app.util.DSpaceContextListener;
import org.dspace.utils.servlet.DSpaceWebappServletFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.RelProvider;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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
public class Application extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private ApplicationConfig configuration;

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

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    @Bean
    protected RelProvider dspaceRelProvider() {
        return new DSpaceRelProvider();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                String[] corsAllowedOrigins = configuration.getCorsAllowedOrigins();
                if (corsAllowedOrigins != null) {
                    registry.addMapping("/api/**").allowedMethods(CorsConfiguration.ALL)
                        .allowedOrigins(corsAllowedOrigins).allowedHeaders("Authorization", "Content-Type",
                            "X-Requested-With", "accept", "Origin", "Access-Control-Request-Method",
                            "Access-Control-Request-Headers")
                        .exposedHeaders("Access-Control-Allow-Origin", "Authorization");
                }
            }

            @Override
            public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> argumentResolvers) {
                argumentResolvers.add(new SearchFilterResolver());
            }
        };
    }
}
