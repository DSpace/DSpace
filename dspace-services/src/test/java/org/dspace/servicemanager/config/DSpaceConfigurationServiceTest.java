/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import static org.junit.Assert.*;

import java.util.HashMap;
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

        // clear out default configs
        configurationService.clear();

        // Start fresh with out own set of 8 configs
        Map<String,Object> l = new HashMap<String,Object>();
        l.put("service.name", "DSpace");
        l.put("sample.array", "itemA,itemB,itemC");
        l.put("sample.number", "123");
        l.put("sample.boolean", "true");
        l.put("aaronz", "Aaron Zeckoski");
        l.put("current.user", "${aaronz}");
        l.put("test.key1", "This is a value");
        l.put("test.key2", "This is key1=${test.key1}");

        configurationService.loadConfiguration(l);
        l = null;
    }

    @After
    public void tearDown() {
        configurationService = null;
    }

    /**
     * A generic method to test that variable replacement is happening properly.
     */
    public void testVariableReplacement() {

        Map<String,Object> l = new HashMap<String,Object>();
        l.put("service.name", "DSpace");
        l.put("aaronz", "Aaron Zeckoski");
        l.put("current.user", "${aaronz}");
        l.put("test.key1", "This is a value");
        l.put("test.key2", "This is key1=${test.key1}");
        l.put("test.key3", "This is key2=${test.key2}");

        configurationService.loadConfiguration(l);

        assertEquals("DSpace", configurationService.getProperty("service.name"));
        assertEquals("Aaron Zeckoski", configurationService.getProperty("aaronz"));
        assertEquals("Aaron Zeckoski", configurationService.getProperty("current.user"));
        assertEquals("This is a value", configurationService.getProperty("test.key1"));
        assertEquals("This is key1=This is a value", configurationService.getProperty("test.key2"));
        assertEquals("This is key2=This is key1=This is a value", configurationService.getProperty("test.key3"));

        //trash the references
        l = null;
    }

    @Test(expected=IllegalStateException.class)
    public void testVariableReplacementCircular()
    {
        // add a circular reference
        configurationService.loadConfig("circular", "${circular}");

        // try to get the value (should throw an error)
        configurationService.getProperty("circular");
    }

    @Test(expected=IllegalStateException.class)
    public void testVariableReplacementIndirectCircular()
    {
        // add a circular reference
        configurationService.loadConfig("circular", "${circular}");
        // add an indirect reference to that circular reference
        configurationService.loadConfig("indirect.circular", "$indirect ${circular}");

        // try to get the value (should throw an error)
        configurationService.getProperty("indirect.circular");
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

        Boolean bool2 = configurationService.getPropertyAsType("INVALID.PROPERTY", Boolean.class);
        assertNotNull(bool2);
        assertEquals(Boolean.FALSE, bool2);

        boolean bool3 = configurationService.getPropertyAsType("INVALID.PROPERTY", boolean.class);
        assertNotNull(bool3);
        assertEquals(false, bool3);

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
        Object prop = configurationService.getPropertyValue("service.fake.thing");
        assertNull(prop);

        prop = configurationService.getPropertyAsType("service.fake.thing", "Fakey", false);
        assertNotNull(prop);
        assertEquals("Fakey", prop);

        prop = configurationService.getPropertyValue("service.fake.thing");
        assertNull(prop);

        prop = configurationService.getPropertyAsType("service.fake.thing", "Fakey", true);
        assertNotNull(prop);
        assertEquals("Fakey", prop);

        prop = configurationService.getPropertyValue("service.fake.thing");
        assertNotNull(prop);
        assertEquals("Fakey", prop);
        prop = null;
    }

    @Test
    public void testSetProperty() {
        Object prop = configurationService.getPropertyValue("newOne");
        assertNull(prop);

        boolean changed = configurationService.setProperty("newOne", "1111111");
        assertTrue(changed);

        prop = configurationService.getPropertyValue("newOne");
        assertNotNull(prop);
        assertEquals("1111111", prop);

        prop = configurationService.getPropertyValue("newBool");
        assertNull(prop);

        changed = configurationService.setProperty("newBool", true);
        assertTrue(changed);

        prop = configurationService.getPropertyValue("newBool");
        assertNotNull(prop);
        assertEquals(Boolean.TRUE, prop);

        changed = configurationService.setProperty("newBool", true);
        assertFalse(changed);

        changed = configurationService.setProperty("newBool", null);
        assertTrue(changed);

        prop = configurationService.getPropertyValue("newBool");
        assertNull(prop);
        prop = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getConfiguration()}.
     */
    @Test
    public void testGetConfiguration() {
        assertNotNull( configurationService.getConfiguration() );
        assertEquals(8, configurationService.getProperties().size() );
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#loadConfig(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testLoadConfig() {
        assertEquals(8, configurationService.getProperties().size());
        configurationService.loadConfig("newA", "A");
        assertEquals(9, configurationService.getProperties().size());
        assertEquals("A", configurationService.getProperty("newA"));
        configurationService.loadConfig("newB", "service is ${service.name}");
        assertEquals(10, configurationService.getProperties().size());
        assertEquals("service is DSpace", configurationService.getProperty("newB"));

        configurationService.loadConfig("newA", "aaronz");
        assertEquals(10, configurationService.getProperties().size());
        assertEquals("aaronz", configurationService.getProperty("newA"));

    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#clear()}.
     */
    @Test
    public void testClear() {
        configurationService.clear();
        assertEquals(0,  configurationService.getProperties().size());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#reloadConfig()}.
     */
    @Test
    public void testReloadConfig() {   
        // Initialize new config service
        DSpaceConfigurationService dscs = new DSpaceConfigurationService();
        int size = dscs.getProperties().size();

        // Add two new Sytem properties
        System.setProperty("Hello","World");
        System.setProperty("Tim", "Donohue");

        // Assert the new properties are not yet loaded
        assertEquals(size, dscs.getProperties().size());

        dscs.reloadConfig();

        // Assert the new properties now exist
        assertEquals(size + 2, dscs.getProperties().size());

        // Set a new value
        System.setProperty("Hello", "There");

        // Assert old value still in Configuration
        assertEquals("World", dscs.getProperty("Hello"));

        dscs.reloadConfig();

        // Now, should be new value
        assertEquals("There", dscs.getProperty("Hello"));

        // Clear set properties
        System.clearProperty("Hello");
        System.clearProperty("Tim");

        // Assert value not yet cleared from Configuration
        assertEquals("There", dscs.getProperty("Hello"));

        dscs.reloadConfig();

        // Now, should be null
        assertNull(dscs.getProperty("Hello"));

        dscs.clear();
        dscs = null;
    }
    
    /**
     * Tests the ability of the system to properly extract system properties into the configuration.
     * (NOTE: This ability to load system properties is specified in the test "config-definition.xml")
     */
    @Test
    public void testGetPropertiesFromSystem() {
        DSpaceConfigurationService dscs = new DSpaceConfigurationService();
        int size = dscs.getProperties().size();

        System.setProperty("dspace.system.config", "Hello");
        System.setProperty("another.property", "Adios");

        dscs.reloadConfig();
        
        assertEquals(size + 2, dscs.getProperties().size());
        assertEquals("Hello", dscs.getProperty("dspace.system.config"));
        assertEquals("Adios", dscs.getProperty("another.property"));

        System.clearProperty("dspace.system.config");
        System.clearProperty("another.property");
        dscs.clear();
        dscs = null;
    }

}
