/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests;

import static org.mockito.Mockito.mock;

import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.dspace.xoai.services.impl.xoai.BaseDSpaceFilterResolver;
import org.dspace.xoai.tests.helpers.stubs.StubbedFieldResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DSpaceBasicTestConfiguration {

    private final StubbedFieldResolver stubbedFieldResolver = new StubbedFieldResolver();

    @Bean
    public DSpaceFilterResolver dSpaceFilterResolver() {
        return new BaseDSpaceFilterResolver();
    }

    @Bean
    public ContextService contextService() {
        return mock(ContextService.class);
    }

    @Bean
    public FieldResolver databaseService() {
        return stubbedFieldResolver;
    }
}
