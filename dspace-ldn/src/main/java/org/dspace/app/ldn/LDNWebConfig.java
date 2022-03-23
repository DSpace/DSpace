/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static org.dspace.app.ldn.RdfMediaType.APPLICATION_JSON_LD;

import java.util.List;

import org.dspace.app.ldn.converter.JsonLdHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 */
@Configuration
public class LDNWebConfig implements WebMvcConfigurer {

    /**
     * @param configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("jsonld", APPLICATION_JSON_LD);
    }

    /**
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new JsonLdHttpMessageConverter());
    }

}
