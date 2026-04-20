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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * OAI-PMH webapp configuration. Replaces the old web.xml.
 * This webapp used JTwig in earlier versions and has been refactored to
 * use Thymeleaf instead.
 * <p>
 * This @Configuration class is automatically discovered by Spring Boot via a @ComponentScan
 * on the org.dspace.app.configuration package.
 * <p>
 *
 *
 * @author Kim Shepherd
 */
@Configuration
// Import additional configuration and beans from BasicConfiguration
@Import(BasicConfiguration.class)
// Scan for controllers in this package
@ComponentScan("org.dspace.xoai.controller")
public class OAIWebConfig implements WebMvcConfigurer {

    // Path where OAI is deployed. Defaults to "oai"
    // NOTE: deployment on this path is handled by org.dspace.xoai.controller.DSpaceOAIDataProvider
    @Value("${oai.path:oai}")
    private String oaiPath;

    private static final String VIEWS_LOCATION = "classpath:/templates/";
    private static final String HTML_EXTENSION = ".html";

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
     * Configure the Thymeleaf template resolver
     **/
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix(VIEWS_LOCATION);
        templateResolver.setSuffix(HTML_EXTENSION);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(true);
        return templateResolver;
    }

    /**
     * Configure the Thymeleaf template engine
     **/
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    /**
     * Configure the Thymeleaf view resolver
     **/
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");
        return viewResolver;
    }

    @Bean
    public ItemRepositoryResolver xoaiItemRepositoryResolver() {
        return new DSpaceItemRepositoryResolver();
    }
}