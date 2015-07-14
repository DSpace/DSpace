/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.dspace.servicemanager.MockServiceManagerSystem;
import org.dspace.servicemanager.SampleAnnotationBean;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.servicemanager.example.ConcreteExample;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Testing the spring based service manager
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class TestSpringServiceManager {

    public static String SPRING_TEST_CONFIG_FILE = "spring/spring-test-services.xml";

    SpringServiceManager ssm;
    DSpaceConfigurationService configurationService;

    @Before
    public void init() {
        configurationService = new DSpaceConfigurationService();
        configurationService.loadConfig(SampleAnnotationBean.class.getName() + ".sampleValue", "beckyz");
        configurationService.loadConfig("fakeBean.fakeParam", "beckyz");

        ssm = new SpringServiceManager(new MockServiceManagerSystem(ssm), configurationService, true, true, SPRING_TEST_CONFIG_FILE);
    }

    @After
    public void shutdown() {
        if (ssm != null) {
            ssm.shutdown();
        }
        ssm = null;
        configurationService = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.spring.SpringServiceManager#startup()}.
     */
    @Test
    public void testStartup() {
        // testing we can start this up with null config
        configurationService.clear();
        ssm.startup();
    }

    @Test
    public void testStartupWithConfig() {
        // testing we can start this up a real config
        ssm.startup();
    }

    /**
     * Test method for {@link org.dspace.servicemanager.spring.SpringServiceManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        ssm.startup();
        ssm.shutdown();
    }

    /**
     * Test method for {@link org.dspace.servicemanager.spring.SpringServiceManager#getServiceByName(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testGetServiceByName() {
        configurationService.clear(); // no config
        ssm.startup();

        ConcreteExample concrete = ssm.getServiceByName(ConcreteExample.class.getName(), ConcreteExample.class);
        assertNotNull(concrete);
        assertEquals("azeckoski", concrete.getName());
        concrete = null;

        SampleAnnotationBean sab = ssm.getServiceByName(SampleAnnotationBean.class.getName(), SampleAnnotationBean.class);
        assertNotNull(sab);
        assertEquals(null, sab.getSampleValue());
        sab = null;
    }

    @Test
    public void testGetServiceByNameConfig() {
        ssm.startup();

        ConcreteExample concrete = ssm.getServiceByName(ConcreteExample.class.getName(), ConcreteExample.class);
        assertNotNull(concrete);
        assertEquals("azeckoski", concrete.getName());
        concrete = null;

        SampleAnnotationBean sab = ssm.getServiceByName(SampleAnnotationBean.class.getName(), SampleAnnotationBean.class);
        assertNotNull(sab);
        assertEquals("beckyz", sab.getSampleValue());
        sab = null;

        SpringAnnotationBean spr = ssm.getServiceByName(SpringAnnotationBean.class.getName(), SpringAnnotationBean.class);
        assertNotNull(spr);
        assertEquals("azeckoski", spr.getConcreteName());
        assertEquals("aaronz", spr.getExampleName());
        assertEquals(null, spr.getSampleValue());
        spr = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.spring.SpringServiceManager#getServicesByType(java.lang.Class)}.
     */
    @Test
    public void testGetServicesByType() {
        ssm.startup();

        List<ConcreteExample> l = ssm.getServicesByType(ConcreteExample.class);
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("azeckoski", l.get(0).getName());
        l = null;

        List<SampleAnnotationBean> l2 = ssm.getServicesByType(SampleAnnotationBean.class);
        assertNotNull(l2);
        assertEquals(1, l2.size());
        l2 = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.spring.SpringServiceManager#registerServiceClass(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testRegisterServiceClass() {
        ssm.startup();

        SampleAnnotationBean sab = ssm.registerServiceClass("newAnnote", SampleAnnotationBean.class);
        assertNotNull(sab);
        sab = null;

        List<SampleAnnotationBean> l = ssm.getServicesByType(SampleAnnotationBean.class);
        assertNotNull(l);
        assertEquals(2, l.size());
        l = null;

        try {
            ssm.registerService("fakey", (Class<?>)null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.spring.SpringServiceManager#registerService(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testRegisterService() {
        ssm.startup();

        String name = "myNewService";
        ssm.registerService(name, "AZ");
        String service = ssm.getServiceByName(name, String.class);
        assertNotNull(service);
        assertEquals("AZ", service);

        try {
            ssm.registerService("fakey", (Object)null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testUnregisterService() {
        ssm.startup();

        String name = "myNewService";
        ssm.registerService(name, "AZ");
        String service = ssm.getServiceByName(name, String.class);
        assertNotNull(service);
        assertEquals("AZ", service);

        ssm.unregisterService(name);
    }

    @Test
    public void testGetServicesNames() {
        ssm.startup();

        List<String> names = ssm.getServicesNames();
        assertNotNull(names);
        assertTrue(names.size() >= 3);
        names = null;
    }

    @Test
    public void testIsServiceExists() {
        ssm.startup();

        String name = ConcreteExample.class.getName();
        boolean exists = ssm.isServiceExists(name);
        assertTrue(exists);

        exists = ssm.isServiceExists("XXXXXXXXXXXXXXX");
        assertFalse(exists);
    }

}
