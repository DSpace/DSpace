/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl;

import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.temporal.ChronoField;

import com.lyncode.xoai.dataprovider.services.impl.BaseDateProvider;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.dspace.xoai.tests.DSpaceBasicTestConfiguration;
import org.dspace.xoai.tests.helpers.stubs.StubbedFieldResolver;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public abstract class AbstractQueryResolverTest {
    private final BaseDateProvider baseDateProvider = new BaseDateProvider();
    protected HandleResolver handleResolver = mock(HandleResolver.class);
    protected CollectionsService collectionsService = mock(CollectionsService.class);
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        applicationContext = new AnnotationConfigApplicationContext(DSpaceBasicTestConfiguration.class);
    }

    @After
    public void tearDown() {
        //Nullify all resources so that JUnit cleans them up
        applicationContext = null;
        handleResolver = null;
        collectionsService = null;
    }


    protected void autowire(Object obj) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(obj);
    }

    protected DSpaceFilterResolver getFilterResolver() {
        return applicationContext.getBean(DSpaceFilterResolver.class);
    }

    protected StubbedFieldResolver theFieldResolver() {
        return (StubbedFieldResolver) applicationContext.getBean(FieldResolver.class);
    }

    protected String escapedFromDate(Instant date) {
        return ClientUtils.escapeQueryChars(
            baseDateProvider.format(java.util.Date.from(dateWithMilliseconds(date, 0)))
                            .replace("Z", ".000Z"));
    }

    protected String escapedUntilDate(Instant date) {
        return ClientUtils.escapeQueryChars(
            baseDateProvider.format(java.util.Date.from(dateWithMilliseconds(date, 999)))
                            .replace("Z", ".999Z"));
    }

    // Return date with specified milliseconds value
    private Instant dateWithMilliseconds(Instant date, int milliseconds) {
        return date.with(ChronoField.MILLI_OF_SECOND, milliseconds);
    }
}
