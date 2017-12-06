/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Testing the config service
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceConfigurationServiceTest {

    DSpaceConfigurationService configurationService;

    @Before
    public void init() {
        configurationService = new DSpaceConfigurationService();

        List<DSpaceConfig> l = new ArrayList<DSpaceConfig>();
        l.add( new DSpaceConfig("service.name", "DSpace") );
        l.add( new DSpaceConfig("sample.array", "itemA,itemB,itemC") );
        l.add( new DSpaceConfig("sample.number", "123") );
        l.add( new DSpaceConfig("sample.boolean", "true") );
        l.add( new DSpaceConfig("aaronz", "Aaron Zeckoski") );
        l.add( new DSpaceConfig("current.user", "${aaronz}") );
        l.add( new DSpaceConfig("test.key1", "This is a value") );
        l.add( new DSpaceConfig("test.key2", "This is key1=${test.key1}") );

        configurationService.loadConfiguration(l, true);
        l = null;
    }

    @After
    public void tearDown() {
        configurationService = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#replaceVariables(java.util.Map)}.
     */
    @Test(timeout=10000)
    public void testReplaceVariables() {

        List<DSpaceConfig> l = new ArrayList<DSpaceConfig>();
        l.add( new DSpaceConfig("service.name", "DSpace") );
        l.add( new DSpaceConfig("aaronz", "Aaron Zeckoski") );
        l.add( new DSpaceConfig("current.user", "${aaronz}") );
        l.add( new DSpaceConfig("test.key1", "This is a value") );
        l.add( new DSpaceConfig("test.key2", "This is key1=${test.key1}") );
        l.add( new DSpaceConfig("test.key3", "This is key2=${test.key2}") );
        int dirIdx = l.size();
        l.add( new DSpaceConfig("circular", "${circular}"));
        int indirIdx = l.size();
        l.add( new DSpaceConfig("indirect.circular", "${circular} square"));

        Map<String, DSpaceConfig> configMap = new HashMap<String, DSpaceConfig>();
        for (DSpaceConfig config : l) {
            configMap.put(config.getKey(), config);
        }
        configurationService.replaceVariables(configMap);

        assertEquals("all configuration list members should be map members",
                l.size(), configMap.size());
        assertEquals("DSpace", configMap.get("service.name").getValue());
        assertEquals("Aaron Zeckoski", configMap.get("aaronz").getValue());
        assertEquals("Aaron Zeckoski", configMap.get("current.user").getValue());
        assertEquals("This is a value", configMap.get("test.key1").getValue());
        assertEquals("This is key1=This is a value", configMap.get("test.key2").getValue());
        assertEquals("This is key2=This is key1=This is a value", configMap.get("test.key3").getValue());
        assertEquals("Direct circular reference should not be replaced",
                configMap.get("circular").getValue(), l.get(dirIdx).getValue());
        assertEquals("Indirect circular reference should not be replaced",
                configMap.get("indirect.circular").getValue(), l.get(indirIdx).getValue());
        
        //trash the references
        l = null;
        configMap = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getAllProperties()}.
     */
    @Test
    public void testGetAllProperties() {
        Map<String, String> props = configurationService.getAllProperties();
        assertNotNull(props);
        assertEquals(8, props.size());
        assertNotNull(props.get("service.name"));
        assertEquals("DSpace", props.get("service.name"));
        
        //trash the references
        props = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getProperties()}.
     */
    @Test
    public void testGetProperties() {
        Properties props = configurationService.getProperties();
        assertNotNull(props);
        assertEquals(8, props.size());
        assertNotNull(props.get("service.name"));
        assertEquals("DSpace", props.get("service.name"));
        
        //trash the references
        props = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getProperty(java.lang.String)}.
     */
    @Test
    public void testGetProperty() {
        String prop = configurationService.getProperty("service.name");
        assertNotNull(prop);
        assertEquals("DSpace", prop);
 
        prop = configurationService.getProperty("XXXXX");
        assertNull(prop);
        prop = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getPropertyAsType(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testGetPropertyAsTypeStringClassOfT() {
        String prop = configurationService.getPropertyAsType("service.name", String.class);
        assertNotNull(prop);
        assertEquals("DSpace", prop);

        String[] array = configurationService.getPropertyAsType("sample.array", String[].class);
        assertNotNull(array);
        assertEquals("itemA", array[0]);
        assertEquals("itemB", array[1]);
        assertEquals("itemC", array[2]);
        Integer number = configurationService.getPropertyAsType("sample.number", Integer.class);
        assertNotNull(number);
        assertEquals(new Integer(123), number);
        Boolean bool = configurationService.getPropertyAsType("sample.boolean", Boolean.class);
        assertNotNull(bool);
        assertEquals(Boolean.TRUE, bool);
        assertEquals(123, (int) configurationService.getPropertyAsType("sample.number", int.class) );
        assertEquals(true, (boolean) configurationService.getPropertyAsType("sample.boolean", boolean.class) );

        prop = configurationService.getPropertyAsType("XXXXX", String.class);
        assertNull(prop);
        prop = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getPropertyAsType(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testGetPropertyAsTypeStringT() {
        String prop = configurationService.getPropertyAsType("service.name", "DeeSpace");
        assertNotNull(prop);
        assertEquals("DSpace", prop);

        String[] array = configurationService.getPropertyAsType("sample.array", new String[] {"A","B"});
        assertNotNull(array);
        assertEquals("itemA", array[0]);
        assertEquals("itemB", array[1]);
        assertEquals("itemC", array[2]);

        Integer number = configurationService.getPropertyAsType("sample.number", new Integer(12345));
        assertNotNull(number);
        assertEquals(new Integer(123), number);

        Boolean bool = configurationService.getPropertyAsType("sample.boolean", Boolean.FALSE);
        assertNotNull(bool);
        assertEquals(Boolean.TRUE, bool);

        boolean b = configurationService.getPropertyAsType("sample.boolean", false);
        assertTrue(b);

        prop = configurationService.getPropertyAsType("XXXXX", "XXX");
        assertEquals("XXX", prop);
        prop = null;
    }

    @Test
    public void testGetPropertyAsTypeStringTBoolean() {
        String prop = configurationService.getProperty("service.fake.thing");
        assertNull(prop);

        prop = configurationService.getPropertyAsType("service.fake.thing", "Fakey", false);
        assertNotNull(prop);
        assertEquals("Fakey", prop);

        prop = configurationService.getProperty("service.fake.thing");
        assertNull(prop);

        prop = configurationService.getPropertyAsType("service.fake.thing", "Fakey", true);
        assertNotNull(prop);
        assertEquals("Fakey", prop);

        prop = configurationService.getProperty("service.fake.thing");
        assertNotNull(prop);
        assertEquals("Fakey", prop);
        prop = null;
    }

    @Test
    public void testSetProperty() {
        String prop = configurationService.getProperty("newOne");
        assertNull(prop);

        boolean changed = configurationService.setProperty("newOne", "1111111");
        assertTrue(changed);

        prop = configurationService.getProperty("newOne");
        assertNotNull(prop);
        assertEquals("1111111", prop);

        prop = configurationService.getProperty("newBool");
        assertNull(prop);

        changed = configurationService.setProperty("newBool", true);
        assertTrue(changed);

        prop = configurationService.getProperty("newBool");
        assertNotNull(prop);
        assertEquals("true", prop);

        changed = configurationService.setProperty("newBool", true);
        assertFalse(changed);

        changed = configurationService.setProperty("newBool", null);
        assertTrue(changed);

        prop = configurationService.getProperty("newBool");
        assertNull(prop);
        prop = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getConfiguration()}.
     */
    @Test
    public void testGetConfiguration() {
        assertNotNull( configurationService.getConfiguration() );
        assertEquals(8, configurationService.getConfiguration().size() );
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#loadConfig(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testLoadConfig() {
        assertEquals(8, configurationService.getConfiguration().size());
        configurationService.loadConfig("newA", "A");
        assertEquals(9, configurationService.getConfiguration().size());
        assertEquals("A", configurationService.getProperty("newA"));
        configurationService.loadConfig("newB", "service is ${service.name}");
        assertEquals(10, configurationService.getConfiguration().size());
        assertEquals("service is DSpace", configurationService.getProperty("newB"));

        configurationService.loadConfig("newA", "aaronz");
        assertEquals(10, configurationService.getConfiguration().size());
        assertEquals("aaronz", configurationService.getProperty("newA"));

    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#clear()}.
     */
    @Test
    public void testClear() {
        configurationService.clear();
        assertEquals(0,  configurationService.getAllProperties().size());
    }

    /**
     * Tests the ability of the system to properly extract system properties into the configuration
     */
    @Test
    public void testGetPropertiesFromSystem() {
        DSpaceConfigurationService dscs = new DSpaceConfigurationService();
        int size = dscs.getConfiguration().size();

        System.setProperty("dspace.az.system.config", "Hello");
        System.setProperty("not.dspace", "Adios");

        dscs = new DSpaceConfigurationService();
        assertEquals(size + 1, dscs.getConfiguration().size());
        assertEquals("Hello", dscs.getProperty("az.system.config"));
        
        dscs.clear();
        dscs = null;
    }

}
