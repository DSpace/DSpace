/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static org.dspace.app.ldn.RdfMediaType.APPLICATION_JSON_LD;
import static org.dspace.app.ldn.RdfMediaType.TEXT_TURTLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.ldn.converter.JsonLdHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        Map<String, MediaType> mediaTypes = new HashMap<>();
        mediaTypes.put("jsonld", APPLICATION_JSON_LD);
        mediaTypes.put("json", APPLICATION_JSON);
        mediaTypes.put("xml", APPLICATION_XML);
        configurer.defaultContentType(TEXT_TURTLE).mediaTypes(mediaTypes);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new JsonLdHttpMessageConverter());
    }

}
