/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.configuration;
import static java.lang.Integer.MAX_VALUE;

import org.dspace.xoai.app.BasicConfiguration;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.dspace.xoai.services.impl.xoai.DSpaceItemRepositoryResolver;
import org.jtwig.spring.JtwigViewResolver;
import org.jtwig.spring.boot.config.JtwigViewResolverConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * OAI-PMH webapp configuration. Replaces the old web.xml
 * <p>
 * This @Configuration class is automatically discovered by Spring Boot via a @ComponentScan
 * on the org.dspace.app.configuration package.
 * <p>
 *
 *
 * @author Tim Donohue
 */
@Configuration
// Import additional configuration and beans from BasicConfiguration
@Import(BasicConfiguration.class)
// Scan for controllers in this package
@ComponentScan("org.dspace.xoai.controller")
public class OAIWebConfig extends WebMvcConfigurerAdapter implements JtwigViewResolverConfigurer {

    // Path where OAI is deployed. Defaults to "oai"
    // NOTE: deployment on this path is handled by org.dspace.xoai.controller.DSpaceOAIDataProvider
    @Value("${oai.path:oai}")
    private String oaiPath;

    private static final String TWIG_HTML_EXTENSION = ".twig.html";
    private static final String VIEWS_LOCATION = "classpath:/templates/";

    /**
     * Ensure all resources under src/main/resources/static/ directory are available
     * off the /{oai.path}/static subpath
     **/
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + oaiPath + "/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(MAX_VALUE);
    }

    /**
     * Configure the Jtwig template engine for Spring Boot
     * Ensures Jtwig looks for templates in proper location with proper extension
     **/
    @Override
    public void configure(JtwigViewResolver viewResolver) {
        viewResolver.setPrefix(VIEWS_LOCATION);
        viewResolver.setSuffix(TWIG_HTML_EXTENSION);
    }

    @Bean
    public ItemRepositoryResolver xoaiItemRepositoryResolver() {
        return new DSpaceItemRepositoryResolver();
    }
}

