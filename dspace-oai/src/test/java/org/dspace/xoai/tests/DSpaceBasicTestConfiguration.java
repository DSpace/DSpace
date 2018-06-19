/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests;

import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.dspace.xoai.services.impl.xoai.BaseDSpaceFilterResolver;
import org.dspace.xoai.tests.helpers.stubs.StubbedConfigurationService;
import org.dspace.xoai.tests.helpers.stubs.StubbedFieldResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class DSpaceBasicTestConfiguration {

    private final StubbedFieldResolver stubbedFieldResolver = new StubbedFieldResolver();

    @Bean
    public DSpaceFilterResolver dSpaceFilterResolver () {
        return new BaseDSpaceFilterResolver();
    }


    private StubbedConfigurationService configurationService = new StubbedConfigurationService();

    @Bean
    public ConfigurationService configurationService() {
        return configurationService;
    }

    @Bean
    public ContextService contextService () {
        return mock(ContextService.class);
    }

    @Bean
    public FieldResolver databaseService () {
        return stubbedFieldResolver;
    }
}
