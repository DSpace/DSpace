/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import com.lyncode.jtwig.mvc.JtwigViewResolver;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.dspace.xoai.services.impl.xoai.DSpaceItemRepositoryResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static java.lang.Integer.MAX_VALUE;

@Import({
        BasicConfiguration.class
})
@Configuration
@EnableWebMvc
@ComponentScan("org.dspace.xoai.controller")
public class DSpaceWebappConfiguration extends WebMvcConfigurerAdapter {
    private static final String TWIG_HTML_EXTENSION = ".twig.html";
    private static final String VIEWS_LOCATION = "/WEB-INF/views/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/")
                .setCachePeriod(MAX_VALUE);
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Bean
    public ViewResolver viewResolver() {
        JtwigViewResolver viewResolver = new JtwigViewResolver();
        viewResolver.setPrefix(VIEWS_LOCATION);
        viewResolver.setSuffix(TWIG_HTML_EXTENSION);
        viewResolver.setCached(false);

        return viewResolver;
    }
    @Bean
    public ItemRepositoryResolver xoaiItemRepositoryResolver() {
        return new DSpaceItemRepositoryResolver();
    }

}
