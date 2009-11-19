/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import org.dspace.kernel.mixins.ConfigChangeListener;
import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ServiceChangeListener;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.servicemanager.ServiceMixinManager.ServiceHolder;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.servicemanager.fakeservices.FakeService1;
import org.dspace.servicemanager.fakeservices.FakeService2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing the ability to manage service mixins,
 * runs without the kernel to test the functionality of this alone
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ServiceMixinManagerTest {

    protected ServiceMixinManager serviceMixinManager;

    @Before
    public void setUp() {
        serviceMixinManager = new ServiceMixinManager();

        // register a couple services as tests
        serviceMixinManager.registerService(FakeService2.class.getName(), new FakeService2());
        serviceMixinManager.registerService("fake2", new FakeService2());
        serviceMixinManager.registerService(FakeService1.class.getName(), new FakeService1(new DSpaceConfigurationService()));
    }

    @After
    public void shutDown() {
        serviceMixinManager.clear();
    }


    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getBiKey(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testGetBiKey() {
        String bikey = ServiceMixinManager.getBiKey("org.azeckoski.Test", Serializable.class);
        assertNotNull(bikey);
        assertEquals("org.azeckoski.Test/java.io.Serializable", bikey);

        bikey = ServiceMixinManager.getBiKey("org.azeckoski.Test", null);
        assertNotNull(bikey);
        assertEquals("org.azeckoski.Test", bikey);

        try {
            ServiceMixinManager.getBiKey(null, Serializable.class);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getServiceName(java.lang.String)}.
     */
    @Test
    public void testGetServiceName() {
        String bikey = "org.azeckoski.Test/java.io.Serializable";
        String serviceName = ServiceMixinManager.getServiceName(bikey);
        assertNotNull(serviceName);
        assertEquals("org.azeckoski.Test", serviceName);

        serviceName = ServiceMixinManager.getServiceName("org.azeckoski.Test");
        assertNotNull(serviceName);
        assertEquals("org.azeckoski.Test", serviceName);

        try {
            ServiceMixinManager.getServiceName(null);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getMixinName(java.lang.String)}.
     */
    @Test
    public void testGetMixinName() {
        String bikey = "org.azeckoski.Test/java.io.Serializable";
        String mixinName = ServiceMixinManager.getMixinName(bikey);
        assertNotNull(mixinName);
        assertEquals("java.io.Serializable", mixinName);

        mixinName = ServiceMixinManager.getMixinName("org.azeckoski.Test");
        assertNull(mixinName);

        try {
            ServiceMixinManager.getMixinName(null);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getMixin(java.lang.String)}.
     */
    @Test
    public void testGetMixin() {
        String bikey = "org.azeckoski.Test/java.io.Serializable";
        Class<?> mixin = ServiceMixinManager.getMixin(bikey);
        assertNotNull(mixin);
        assertEquals(Serializable.class, mixin);

        mixin = ServiceMixinManager.getMixin("org.azeckoski.Test");
        assertNull(mixin);

        try {
            ServiceMixinManager.getMixin(null);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#extractMixins(java.lang.Object)}.
     */
    @Test
    public void testExtractMixins() {
        List<Class<?>> mixins = ServiceMixinManager.extractMixins(new FakeService1());
        assertNotNull(mixins);
        assertEquals(5, mixins.size());
        assertTrue(mixins.contains(ConfigChangeListener.class));
        assertTrue(mixins.contains(ServiceChangeListener.class));
        assertTrue(mixins.contains(InitializedService.class));
        assertTrue(mixins.contains(ShutdownService.class));
        assertTrue(mixins.contains(Serializable.class));

        mixins = ServiceMixinManager.extractMixins(new FakeService2());
        assertNotNull(mixins);
        assertEquals(3, mixins.size());
        assertTrue(mixins.contains(InitializedService.class));
        assertTrue(mixins.contains(Comparable.class));
        assertTrue(mixins.contains(Serializable.class));

        try {
            ServiceMixinManager.extractMixins(null);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getServiceByName(java.lang.String)}.
     */
    @Test
    public void testGetServiceByName() {
        Object service = serviceMixinManager.getServiceByName(FakeService2.class.getName());
        assertNotNull(service);
        assertTrue(service instanceof FakeService2);

        service = serviceMixinManager.getServiceByName("fake2");
        assertNotNull(service);
        assertTrue(service instanceof FakeService2);

        service = serviceMixinManager.getServiceByName(FakeService1.class.getName());
        assertNotNull(service);
        assertTrue(service instanceof FakeService1);

        service = serviceMixinManager.getServiceByName("XXXXXXXXXXXXXX");
        assertNull(service);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getServiceByNameAndMixin(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testGetServiceByNameAndMixin() {
        ServiceChangeListener serviceChangeListener = serviceMixinManager.getServiceByNameAndMixin(FakeService1.class.getName(), ServiceChangeListener.class);
        assertNotNull(serviceChangeListener);

        FakeService1 service1 = serviceMixinManager.getServiceByNameAndMixin(FakeService1.class.getName(), null);
        assertNotNull(service1);

        Observer observer = serviceMixinManager.getServiceByNameAndMixin(FakeService1.class.getName(), Observer.class);
        assertNull(observer);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getRegisteredServiceNames()}.
     */
    @Test
    public void testGetRegisteredServiceNames() {
        List<String> names = serviceMixinManager.getRegisteredServiceNames();
        assertNotNull(names);
        assertEquals(3, names.size());
        assertTrue(names.contains("fake2"));
        assertTrue(names.contains(FakeService1.class.getName()));
        assertTrue(names.contains(FakeService2.class.getName()));
    }

    @Test
    public void testGetRegisteredServices() {
        List<Object> services = serviceMixinManager.getRegisteredServices();
        assertNotNull(services);
        assertEquals(3, services.size());
    }

    @Test
    public void testSize() {
        assertEquals(3, serviceMixinManager.size());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#registerService(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testRegisterService() {
        serviceMixinManager.registerService("aaronz", new FakeService2());
        assertEquals(4, serviceMixinManager.size());

        // should not increase the service count
        serviceMixinManager.registerService("aaronz", new FakeService2());
        assertEquals(4, serviceMixinManager.size());

        try {
            serviceMixinManager.registerService(null, new FakeService2());
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            serviceMixinManager.registerService("aaronz", null);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#unregisterServiceByName(java.lang.String)}.
     */
    @Test
    public void testUnregisterServiceByName() {
        assertEquals(3, serviceMixinManager.size());
        assertNotNull(serviceMixinManager.getServiceByName("fake2"));
        serviceMixinManager.unregisterServiceByName("fake2");
        assertEquals(2, serviceMixinManager.size());
        assertNull(serviceMixinManager.getServiceByName("fake2"));
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#unregisterServiceMixin(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testUnregisterServiceMixin() {
        assertEquals(3, serviceMixinManager.size());
        assertNotNull(serviceMixinManager.getServiceByName("fake2"));
        assertNotNull(serviceMixinManager.getServiceByNameAndMixin("fake2", Serializable.class));
        serviceMixinManager.unregisterServiceMixin("fake2", Serializable.class);
        assertEquals(3, serviceMixinManager.size());
        assertNotNull(serviceMixinManager.getServiceByName("fake2"));
        assertNull(serviceMixinManager.getServiceByNameAndMixin("fake2", Serializable.class));
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getServiceMixins(java.lang.String)}.
     */
    @Test
    public void testGetServiceMixins() {
        List<Class<? extends Object>> mixins = serviceMixinManager.getServiceMixins("fake2");
        assertNotNull(mixins);
        assertEquals(4, mixins.size());
        assertTrue(mixins.contains(FakeService2.class));
        assertTrue(mixins.contains(InitializedService.class));
        assertTrue(mixins.contains(Comparable.class));
        assertTrue(mixins.contains(Serializable.class));

        mixins = serviceMixinManager.getServiceMixins(FakeService1.class.getName());
        assertNotNull(mixins);
        assertEquals(6, mixins.size());
        assertTrue(mixins.contains(FakeService1.class));
        assertTrue(mixins.contains(ConfigChangeListener.class));
        assertTrue(mixins.contains(ServiceChangeListener.class));
        assertTrue(mixins.contains(InitializedService.class));
        assertTrue(mixins.contains(ShutdownService.class));
        assertTrue(mixins.contains(Serializable.class));

        mixins = serviceMixinManager.getServiceMixins("XXXXXXXX");
        assertNotNull(mixins);
        assertEquals(0, mixins.size());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getRegisteredServiceMixins()}.
     */
    @Test
    public void testGetRegisteredServiceMixins() {
        Map<String, List<Class<? extends Object>>> serviceMixins = serviceMixinManager.getRegisteredServiceMixins();
        assertNotNull(serviceMixins);
        assertEquals(3, serviceMixins.size());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getServicesByMixin(java.lang.Class)}.
     */
    @Test
    public void testGetServicesByMixin() {
        List<Serializable> slist = serviceMixinManager.getServicesByMixin(Serializable.class);
        assertNotNull(slist);
        assertEquals(3, slist.size());

        List<Observer> olist = serviceMixinManager.getServicesByMixin(Observer.class);
        assertNotNull(olist);
        assertEquals(0, olist.size());

        List<FakeService2> fs2l = serviceMixinManager.getServicesByMixin(FakeService2.class);
        assertNotNull(fs2l);
        assertEquals(2, fs2l.size());

        List<ServiceChangeListener> scll = serviceMixinManager.getServicesByMixin(ServiceChangeListener.class);
        assertNotNull(scll);
        assertEquals(1, scll.size());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.ServiceMixinManager#getServiceNamesByMixin(java.lang.Class)}.
     */
    @Test
    public void testGetServiceHoldersByMixin() {
        List<ServiceHolder<Serializable>> slist = serviceMixinManager.getServiceHoldersByMixin(Serializable.class);
        assertNotNull(slist);
        assertEquals(3, slist.size());

        List<ServiceHolder<Observer>> olist = serviceMixinManager.getServiceHoldersByMixin(Observer.class);
        assertNotNull(olist);
        assertEquals(0, olist.size());

        List<ServiceHolder<FakeService2>> fs2l = serviceMixinManager.getServiceHoldersByMixin(FakeService2.class);
        assertNotNull(fs2l);
        assertEquals(2, fs2l.size());

        List<ServiceHolder<ServiceChangeListener>> scll = serviceMixinManager.getServiceHoldersByMixin(ServiceChangeListener.class);
        assertNotNull(scll);
        assertEquals(1, scll.size());
        assertEquals(FakeService1.class.getName(), scll.get(0).getServiceName());
    }

}
