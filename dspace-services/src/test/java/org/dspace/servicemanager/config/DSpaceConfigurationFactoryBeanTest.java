/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import org.dspace.services.ConfigurationService;
import org.dspace.test.DSpaceAbstractKernelTest;
import static org.dspace.test.DSpaceAbstractTest.getKernel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Testing the DSpaceConfigurationFactoryBean to ensure it performs property
 * substitution in Spring XML configs (e.g. replacing ${dspace.dir} with the
 * value from dspace.cfg)
 * <P>
 * NOTE: This uses a TestDynamicPropertyBean bean defined in spring-test-beans.xml
 * for all tests.
 *
 * @author Tim Donohue
 */
public class DSpaceConfigurationFactoryBeanTest
    extends DSpaceAbstractKernelTest
{

    /**
     * Test that property substitution is working properly in Spring XML configs.
     * Properties in those configs (e.g. ${key}) should be dynamically replaced
     * with the corresponding value from our ConfigurationService
     */
    @Test
    public void testGetBeanSettingFromConfigurationService() {

        // Load configs from files
        ConfigurationService cfg = getKernel().getConfigurationService();
        assertNotNull("ConfigurationService returned null", cfg);
        assertNotNull("test config returned null", cfg.getProperty("testDynamicBean.property"));

        //Load example service which is configured using a dynamic property (which is specified in a config file)
        // See spring-test-beans.xml
        TestDynamicPropertyBean bean = getKernel().getServiceManager().getServiceByName("dynamicPropertyBean", TestDynamicPropertyBean.class);

        assertNotNull("Bean returned null", bean);
        assertNotNull("Bean.name() returned null", bean.getProperty());

        // The name of the ServiceExample bean should be the SAME as the value of "serviceExample.bean.name" in configuration,
        // as the spring-test-beans.xml uses ${serviceExample.bean.name} to set the name
        assertEquals("Bean.name() does not match configuration", cfg.getProperty("testDynamicBean.property"), bean.getProperty());
    }
}
