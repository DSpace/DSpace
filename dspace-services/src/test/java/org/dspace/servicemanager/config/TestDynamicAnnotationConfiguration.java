/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A test bean which we will configure to load its one property via @Value annotation
 * <P>
 * See DSpaceConfigurationFactoryBeanTest.
 *
 * @author Tim Donohue
 */
@Configuration
public class TestDynamicAnnotationConfiguration {
    // This setting should be loaded from the "testDynamicBean.property" configuration in local.properties
    @Value("${testDynamicBean.property}")
    private String value;

    @Bean
    public TestDynamicPropertyBean propertyBeanUsingAnnotation() {
        TestDynamicPropertyBean bean = new TestDynamicPropertyBean();
        bean.setProperty(value);
        return bean;
    }
}
