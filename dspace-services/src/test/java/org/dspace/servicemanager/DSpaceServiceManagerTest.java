/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.servicemanager.example.ConcreteExample;
import org.dspace.servicemanager.fakeservices.FakeService1;
import org.dspace.servicemanager.spring.SpringAnnotationBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing the main DSpace service manager.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceServiceManagerTest {
    public static String SPRING_TEST_CONFIG_FILE = "spring/spring-test-services.xml";

    DSpaceServiceManager dsm;
    DSpaceConfigurationService configurationService;

    @Before
    public void init() {
        configurationService = new DSpaceConfigurationService();

        dsm = new DSpaceServiceManager(configurationService, SPRING_TEST_CONFIG_FILE);
    }

    @After
    public void shutdown() {
        if (dsm != null) {
            dsm.shutdown();
        }
        dsm = null;
        configurationService = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        dsm.startup();
        dsm.shutdown();
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#startup()}.
     */
    @Test
    public void testStartup() {
        // testing we can start this up with cleared config
        configurationService.clear();
        dsm.startup();
    }

    @Test
    public void testStartupWithConfig() {
        // testing we can start this up a real config
        dsm.startup();
    }

    /**
     * Test method for
     * {@link org.dspace.servicemanager.DSpaceServiceManager#registerService(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testRegisterService() {
        dsm.startup();

        String name = "myNewService";
        dsm.registerService(name, "AZ");
        String service = dsm.getServiceByName(name, String.class);
        assertNotNull(service);
        assertEquals("AZ", service);

        try {
            dsm.registerService("fakey", (Object) null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.dspace.servicemanager.DSpaceServiceManager#registerServiceClass(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testRegisterServiceClass() {
        dsm.startup();

        SampleAnnotationBean sab = dsm.registerServiceClass("newAnnote", SampleAnnotationBean.class);
        assertNotNull(sab);

        SampleAnnotationBean sampleAnnotationBean = dsm.getServiceByName("newAnnote", SampleAnnotationBean.class);
        assertNotNull(sampleAnnotationBean);
        assertEquals(sampleAnnotationBean, sab);
        sampleAnnotationBean = null;
        sab = null;


        try {
            dsm.registerService("fakey", (Class<?>) null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#unregisterService(java.lang.String)}.
     */
    @Test
    public void testUnregisterService() {
        dsm.startup();

        String name = "myNewService";
        dsm.registerService(name, "AZ");
        String service = dsm.getServiceByName(name, String.class);
        assertNotNull(service);
        assertEquals("AZ", service);

        dsm.unregisterService(name);
    }

    /**
     * Test method for
     * {@link org.dspace.servicemanager.DSpaceServiceManager#getServiceByName(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testGetServiceByName() {
        configurationService.clear();
        dsm.startup();

        ConcreteExample concrete = dsm.getServiceByName(ConcreteExample.class.getName(), ConcreteExample.class);
        assertNotNull(concrete);
        assertEquals("azeckoski", concrete.getName());
        concrete = null;

        SampleAnnotationBean sab = dsm
            .getServiceByName(SampleAnnotationBean.class.getName(), SampleAnnotationBean.class);
        assertNotNull(sab);
        assertEquals(null, sab.getSampleValue());
        sab = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#getServicesByType(java.lang.Class)}.
     */
    @Test
    public void testGetServicesByType() {
        dsm.startup();

        int currentSize = dsm.getServicesByType(ConcreteExample.class).size();
        assertTrue(currentSize > 0);

        List<ConcreteExample> l = dsm.getServicesByType(ConcreteExample.class);
        assertNotNull(l);
        assertEquals("azeckoski", l.get(0).getName());
        l = null;

        List<SampleAnnotationBean> l2 = dsm.getServicesByType(SampleAnnotationBean.class);
        assertNotNull(l2);
        assertTrue(l2.size() >= 1);
        l2 = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#getServicesNames()}.
     */
    @Test
    public void testGetServicesNames() {
        dsm.startup();

        List<String> names = dsm.getServicesNames();
        assertNotNull(names);
        assertTrue(names.size() >= 3);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#isServiceExists(java.lang.String)}.
     */
    @Test
    public void testIsServiceExists() {
        dsm.startup();

        String name = ConcreteExample.class.getName();
        boolean exists = dsm.isServiceExists(name);
        assertTrue(exists);

        name = SampleAnnotationBean.class.getName();
        exists = dsm.isServiceExists(name);
        assertTrue(exists);

        name = SpringAnnotationBean.class.getName();
        exists = dsm.isServiceExists(name);
        assertTrue(exists);

        exists = dsm.isServiceExists("XXXXXXXXXXXXXXX");
        assertFalse(exists);
    }

    @Test
    public void testGetServices() {
        dsm.startup();

        Map<String, Object> services = dsm.getServices();
        assertNotNull(services);
        assertTrue(services.size() > 3);
    }

    @Test
    public void testPushConfig() {
        dsm.startup();

        Map<String, Object> properties = new HashMap<>();
        properties.put("some.test.thing", "A value");
        dsm.pushConfig(properties);

        // TODO need to do a better test here
    }

    @Test
    public void testRegisterProviderLifecycle() {
        dsm.startup();

        // this tests to see if the lifecycle of a provider is working
        String serviceName = "azeckoski.FakeService1";
        FakeService1 service = new FakeService1();
        assertEquals(0, service.getTriggers());

        // now we register it and the init should be called
        dsm.registerService(serviceName, service);
        assertNotNull(service.getConfigurationService());
        assertEquals("init", service.getSomething());
        assertEquals(1, service.getTriggers());

        // now we do a config change
        Map<String, Object> properties = new HashMap<>();
        properties.put("azeckoski.FakeService1.something", "THING");
        dsm.pushConfig(properties);
        assertEquals("config:THING", service.getSomething());
        assertEquals(2, service.getTriggers());

        // now we unregister
        dsm.unregisterService(serviceName);
        assertEquals("shutdown", service.getSomething());
        assertEquals(3, service.getTriggers());

        service = null;
        properties = null;
    }

    public static class TestService {

        public int value = 0;

        @PostConstruct
        public void init() {
            value++;
        }

        @PreDestroy
        public void shutdown() {
            value++;
        }

    }

}
