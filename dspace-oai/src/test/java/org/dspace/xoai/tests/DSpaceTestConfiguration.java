/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests;

import com.lyncode.xoai.dataprovider.services.api.ItemRepository;
import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import com.lyncode.xoai.dataprovider.services.api.SetRepository;
import org.dspace.core.Context;
import org.dspace.xoai.services.api.cache.XOAICacheService;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.EarliestDateResolver;
import org.dspace.xoai.services.api.xoai.IdentifyResolver;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.dspace.xoai.services.api.xoai.SetRepositoryResolver;
import org.dspace.xoai.services.impl.cache.DSpaceEmptyCacheService;
import org.dspace.xoai.services.impl.xoai.DSpaceIdentifyResolver;
import org.dspace.xoai.tests.helpers.stubs.StubbedEarliestDateResolver;
import org.dspace.xoai.tests.helpers.stubs.StubbedResourceResolver;
import org.dspace.xoai.tests.helpers.stubs.StubbedSetRepository;
import org.dspace.xoai.tests.helpers.stubs.StubbedXOAIManagerResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Import(DSpaceBasicTestConfiguration.class)
@Configuration
@EnableWebMvc
public class DSpaceTestConfiguration extends WebMvcConfigurerAdapter {
    private static final String TWIG_HTML_EXTENSION = ".twig.html";
    private static final String VIEWS_LOCATION = "/WEB-INF/views/";


    @Bean
    public ContextService contextService() {
        return new ContextService() {
            @Override
            public Context getContext() throws ContextServiceException {
                return null;
            }
        };
    }

    private StubbedResourceResolver resourceResolver = new StubbedResourceResolver();

    @Bean
    public ResourceResolver resourceResolver() {
        return resourceResolver;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix(VIEWS_LOCATION);
        viewResolver.setSuffix(TWIG_HTML_EXTENSION);
//        viewResolver.setCached(true);
//        viewResolver.setTheme(null);

        return viewResolver;
    }


    @Bean
    public XOAIManagerResolver xoaiManagerResolver() {
        return new StubbedXOAIManagerResolver();
    }

    @Bean
    public XOAICacheService xoaiCacheService() {
        return new DSpaceEmptyCacheService();
    }

    private StubbedSetRepository setRepository = new StubbedSetRepository();

    @Bean StubbedSetRepository setRepository () {
        return setRepository;
    }

    @Bean
    public ItemRepositoryResolver itemRepositoryResolver() {
        return new ItemRepositoryResolver() {
            @Override
            public ItemRepository getItemRepository() throws ContextServiceException {
                try {
                    return null;
                } catch (Exception e) {
                    throw new ContextServiceException(e);
                }
            }
        };
    }
    @Bean
    public SetRepositoryResolver setRepositoryResolver () {
        return new SetRepositoryResolver() {
            @Override
            public SetRepository getSetRepository() throws ContextServiceException {
                return setRepository;
            }
        };
    }
    @Bean
    public IdentifyResolver identifyResolver () {
        return new DSpaceIdentifyResolver();
    }

    @Bean
    public EarliestDateResolver earliestDateResolver () {
        return new StubbedEarliestDateResolver();
    }
}
