/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.dspace.services.ConfigurationService;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing the org.apache.commons.configuration2.spring.ConfigurationPropertiesFactoryBean to ensure it performs
 * property substitution in Spring XML configs (e.g. replacing ${dspace.dir} with the value from dspace.cfg)
 * <P>
 * NOTE: This uses a TestDynamicPropertyBean bean defined in spring-test-beans.xml for all tests. It also depends
 * on the org.springframework.beans.factory.config.PropertyPlaceholderConfigurer defined in
 * spring-dspace-core-services.xml
 *
 * @author Tim Donohue
 */
public class DSpaceConfigurationBeanTest
    extends DSpaceAbstractKernelTest {

    // Path to our main test config file (local.properties)
    private String propertyFilePath;

    @Before
    public void init() {
        // Save the path to our main test configuration file
        propertyFilePath = new DSpaceConfigurationService().getDSpaceHome(null) + File.separatorChar
            + DSpaceConfigurationService.DEFAULT_CONFIG_DIR + File.separatorChar + "local.properties";
    }

    /**
     * Test that property substitution is working properly in Spring XML configs.
     * Properties in those XML configs (e.g. ${key}) should be dynamically replaced
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
        TestDynamicPropertyBean bean = getKernel().getServiceManager().getServiceByName("dynamicPropertyBean",
                                                                                        TestDynamicPropertyBean.class);

        assertNotNull("Bean returned null", bean);
        assertNotNull("Bean.name() returned null", bean.getProperty());

        // The bean's getProperty() method should return the same value as "testDynamicBean.property" in DSpace's
        // configuration. This is cause bean's property is set to ${testDynamicBean.property} in spring-test-beans.xml
        assertEquals("Bean.getProperty() does not match configuration", cfg.getProperty("testDynamicBean.property"),
                     bean.getProperty());
    }

    /**
     * Test that property substitution is working properly in Spring PropertySource (e.g. @Value annotations)
     * Properties in those annotations, e.g. @Value("${key}"), should be dynamically replaced with the corresponding
     * value from our ConfigurationService
     */
    @Test
    public void testGetPropertySourceFromConfigurationService() {
        // Load configs from files
        ConfigurationService cfg = getKernel().getConfigurationService();
        assertNotNull("ConfigurationService returned null", cfg);
        assertNotNull("test config returned null", cfg.getProperty("testDynamicBean.property"));

        // Load test bean which is defined by TestDynamicAnnotationConfiguration
        TestDynamicPropertyBean bean = getKernel().getServiceManager().getServiceByName("propertyBeanUsingAnnotation",
                                                                                        TestDynamicPropertyBean.class);

        // The Test bean's property should be automatically set (see TestDynamicAnnotationConfiguration)
        String configValue = bean.getProperty();

        assertNotNull("PropertySource config returned null", configValue);

        // The value of "configValue" should be equal to "testDynamicBean.property" in our configuration.
        // This is because configValue is set via an @Value annotation in TestDynamicAnnotationConfiguration
        assertEquals("PropertySource config does not match configuration", cfg.getProperty("testDynamicBean.property"),
                     configValue);
    }

    /**
     * Test that automatic reloading of configuration via bean settings also works.
     *
     * @TODO: This test does not actually work yet, because Commons Configuration v2 doesn't yet have a
     * org.apache.commons.configuration2.spring.ConfigurationPropertiesFactoryBean that supports reloading properties.
     * NOTE: This test also fails in Commons Configuration v1 (e.g. in DSpace 6.x). So, it's possible we may not be
     * able to support reloadable properties via Spring beans (until Commons Configuration does)
     */
    /*@Test
    public void testReloadBeanSettingFromConfigurationService() throws ConfigurationException, InterruptedException {
        //Load example service which is configured using a dynamic property (which is specified in a config file)
        // See spring-test-beans.xml
        TestDynamicPropertyBean bean = getKernel().getServiceManager().getServiceByName("dynamicPropertyBean",
                                                                                        TestDynamicPropertyBean.class);
        assertNotNull("WeLoveDSpace", bean.getProperty());

        // Change the value of that Property in the file itself (using a separate builder instance)
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new Configurations()
            .propertiesBuilder(propertyFilePath);
        PropertiesConfiguration config = builder.getConfiguration();
        // Clear out current value. Add in a new value
        config.clearProperty("testDynamicBean.property");
        config.addProperty("testDynamicBean.property", "NewValue");
        // Save updates to file
        builder.save();

        // Check immediately. Property should be unchanged
        // NOTE: If this fails, then somehow the configuration reloaded *immediately*
        assertEquals("WeLoveDSpace", bean.getProperty());

        // Wait now for 3 seconds
        Thread.sleep(3_000);

        // Check again. Property should have reloaded
        // NOTE: reload time is set in config-definition.xml to reload every 2 seconds
        assertEquals("NewValue", bean.getProperty());
    } */
}
