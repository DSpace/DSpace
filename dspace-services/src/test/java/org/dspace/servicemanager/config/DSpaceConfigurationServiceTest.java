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
import mockit.Expectations;

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
    int numPropsLoaded;

    @Before
    public void init() {
        configurationService = new DSpaceConfigurationService();

        // clear out default configs (leaves us with an empty Configuration)
        configurationService.clear();

        // Start fresh with out own set of configs
        Map<String,Object> l = new HashMap<String,Object>();
        l.put("service.name", "DSpace");
        l.put("sample.array", "itemA,itemB,itemC");
        l.put("sample.number", "123");
        l.put("sample.boolean", "true");
        // 3 Billion cannot be stored as an "int" (max value 2^31-1)
        l.put("sample.long", "3000000000");
        l.put("aaronz", "Aaron Zeckoski");
        l.put("current.user", "${aaronz}");
        l.put("test.key1", "This is a value");
        l.put("test.key2", "This is key1=${test.key1}");

        // Record how many properties we initialized with (for below unit tests)
        numPropsLoaded=9;

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
    @Test
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
        assertEquals(numPropsLoaded, props.size());
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
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getArrayProperty(java.lang.String)}.
     */
    @Test
    public void testGetArrayProperty() {
        String[] array = configurationService.getArrayProperty("sample.array");
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals("itemA", array[0]);
        assertEquals("itemB", array[1]);
        assertEquals("itemC", array[2]);

        // Pass in default value
        array = configurationService.getArrayProperty("sample.array", new String[]{"Hey"});
        // Assert default value not used, since property exists
        assertEquals(3, array.length);

        array = configurationService.getArrayProperty("XXXXX");
        assertEquals(0, array.length);

        // Test default value
        array = configurationService.getArrayProperty("XXXXX", new String[]{"Hey"});
        assertEquals(1, array.length);
        assertEquals("Hey", array[0]);

        // Test escaping commas (with \,)
        configurationService.loadConfig("new.array", "A\\,B\\,C");
        array = configurationService.getArrayProperty("new.array");
        assertEquals(1, array.length);
        assertEquals("A,B,C", array[0]);
        configurationService.clearConfig("new.array");
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getBooleanProperty(java.lang.String)}.
     */
    @Test
    public void testGetBooleanProperty() {
        boolean b = configurationService.getBooleanProperty("sample.boolean");
        assertEquals(true, b);

        // Pass in default value
        b = configurationService.getBooleanProperty("sample.boolean", false);
        assertEquals(true, b);

        b = configurationService.getBooleanProperty("XXXXX");
        assertEquals(false, b);

        // Pass in default value
        b = configurationService.getBooleanProperty("XXXXX", true);
        assertEquals(true, b);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getIntProperty(java.lang.String)}.
     */
    @Test
    public void testGetIntProperty() {
        int i = configurationService.getIntProperty("sample.number");
        assertEquals(123, i);

        // Pass in default value
        i = configurationService.getIntProperty("sample.number", -1);
        assertEquals(123, i);

        i = configurationService.getIntProperty("XXXXX");
        assertEquals(0, i);

        // Pass in default value
        i = configurationService.getIntProperty("XXXXX", 345);
        assertEquals(345, i);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getLongProperty(java.lang.String)}.
     */
    @Test
    public void testGetLongProperty() {
        long l = configurationService.getLongProperty("sample.long");
        //NOTE: "L" suffix ensures number is treated as a long
        assertEquals(3000000000L, l);

        // Pass in default value
        l = configurationService.getLongProperty("sample.long", -1);
        assertEquals(3000000000L, l);

        l = configurationService.getLongProperty("XXXXX");
        assertEquals(0, l);

        // Pass in default value
        l = configurationService.getLongProperty("XXXXX", 3000000001L);
        assertEquals(3000000001L, l);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getHasProperty(java.lang.String)}.
     */
    @Test
    public void testHasProperty() {
        assertEquals(true, configurationService.hasProperty("sample.array"));
        assertEquals(true, configurationService.hasProperty("sample.number"));
        assertEquals(false, configurationService.hasProperty("XXXXX"));
        assertEquals(false, configurationService.hasProperty("samplearray"));
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

        // TEST setting a new Integer & retrieving using various methods
        Object prop = configurationService.getPropertyValue("newOne");
        assertNull(prop);

        boolean changed = configurationService.setProperty("newOne", "1111111");
        assertTrue(changed);

        prop = configurationService.getPropertyValue("newOne");
        assertNotNull(prop);
        assertEquals("1111111", prop);

        int i = configurationService.getIntProperty("newOne");
        assertEquals(1111111, i);

        // Test Setting a new Boolean and retrieving through various methods
        prop = configurationService.getPropertyValue("newBool");
        assertNull(prop);

        changed = configurationService.setProperty("newBool", true);
        assertTrue(changed);

        prop = configurationService.getPropertyValue("newBool");
        assertNotNull(prop);
        assertEquals(Boolean.TRUE, prop);

        boolean b = configurationService.getBooleanProperty("newBool");
        assertEquals(true, b);

        changed = configurationService.setProperty("newBool", true);
        assertFalse(changed);

        changed = configurationService.setProperty("newBool", null);
        assertTrue(changed);

        prop = configurationService.getPropertyValue("newBool");
        assertNull(prop);

        // Test Setting a new String and retrieving through various methods
        prop = configurationService.getPropertyValue("newString");
        assertNull(prop);

        changed = configurationService.setProperty("newString", "  Hi There      ");
        assertTrue(changed);

        // Assert strings are trimmed
        String s = configurationService.getProperty("newString");
        assertNotNull(s);
        assertEquals("Hi There", s);

        // Clear out our new props
        configurationService.clearConfig("newOne");
        configurationService.clearConfig("newBool");
        configurationService.clearConfig("newString");
        prop = null;

    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getConfiguration()}.
     */
    @Test
    public void testGetConfiguration() {
        assertNotNull( configurationService.getConfiguration() );
        assertEquals(numPropsLoaded, configurationService.getProperties().size() );
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#loadConfig(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testLoadConfig() {
        assertEquals(numPropsLoaded, configurationService.getProperties().size());
        configurationService.loadConfig("newA", "A");
        assertEquals(numPropsLoaded+1, configurationService.getProperties().size());
        assertEquals("A", configurationService.getProperty("newA"));
        configurationService.loadConfig("newB", "service is ${service.name}");
        assertEquals(numPropsLoaded+2, configurationService.getProperties().size());
        assertEquals("service is DSpace", configurationService.getProperty("newB"));

        configurationService.loadConfig("newA", "aaronz");
        assertEquals(numPropsLoaded+2, configurationService.getProperties().size());
        assertEquals("aaronz", configurationService.getProperty("newA"));

        // Clear out newly added props
        configurationService.clearConfig("newA");
        configurationService.clearConfig("newB");
        assertEquals(numPropsLoaded, configurationService.getProperties().size());
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

    /**
     * Tests the ability of the system to properly extract properties from files
     * (NOTE: The local.properties test file is specified in the test "config-definition.xml")
     */
    @Test
    public void testGetPropertiesFromFile() {

        DSpaceConfigurationService dscs = new DSpaceConfigurationService();

        // Test that property values are automatically trimmed of leading/trailing spaces
        // In local.properties, this value is something like "   test    "
        assertEquals("test", dscs.getProperty("prop.needing.trimmed"));

        dscs.clear();
        dscs = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.config.DSpaceConfigurationService#getDSpaceHome(java.lang.String)}.
     */
    @Test
    public void testGetDSpaceHomeSysProperty() {
        final DSpaceConfigurationService dscs = new DSpaceConfigurationService();

        // Set System Property for DSpace Home
        new Expectations(System.class) {{
            // return "/mydspace" two times
            System.getProperty(DSpaceConfigurationService.DSPACE_HOME); result = "/mydspace";
        }};
        // Ensure /mydspace looks like a valid DSpace home directory
        new Expectations(dscs.getClass()) {{
            dscs.isValidDSpaceHome("/mydspace"); result = true;
        }};

        // Assert Home is the same as System Property
        assertEquals("System property set", "/mydspace", dscs.getDSpaceHome(null));
    }

    @Test
    public void testGetDSpaceHomeSysPropertyOverride() {
        final DSpaceConfigurationService dscs = new DSpaceConfigurationService();

        // Set System Property for DSpace Home
        new Expectations(System.class) {{
            System.getProperty(DSpaceConfigurationService.DSPACE_HOME); result = "/mydspace";
        }};
        // Ensure /mydspace looks like a valid DSpace home directory
        new Expectations(dscs.getClass()) {{
            dscs.isValidDSpaceHome("/mydspace"); result = true;
        }};

        // Assert System Property overrides the value passed in, if it is valid
        assertEquals("System property override", "/mydspace", dscs.getDSpaceHome("/myotherdspace"));
    }

    @Test
    public void testGetDSpaceHomeNoSysProperty() {

        final DSpaceConfigurationService dscs = new DSpaceConfigurationService();

        // No system property set
        new Expectations(System.class) {{
            System.getProperty(DSpaceConfigurationService.DSPACE_HOME); result = null;
        }};
        // Ensure /mydspace looks like a valid DSpace home directory
        new Expectations(dscs.getClass()) {{
            dscs.isValidDSpaceHome("/mydspace"); result = true;
        }};

        // Assert provided home is used
        assertEquals("Home based on passed in value", "/mydspace", dscs.getDSpaceHome("/mydspace"));
    }
}
