/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.dspace.AbstractDSpaceTest;
import org.dspace.core.factory.CoreServiceFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for DSpaceControlledVocabulary.
 *
 * @author mwood
 */
public class DSpaceControlledVocabularyTest extends AbstractDSpaceTest {
    public DSpaceControlledVocabularyTest() {
    }

    @BeforeClass
    public static void setUpClass()
        throws Exception {
    }

    @AfterClass
    public static void tearDownClass()
        throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getPluginNames method, of class DSpaceControlledVocabulary.
     */
/*
    @Test
    public void testGetPluginNames()
    {
        System.out.println("getPluginNames");
        String[] expResult = null;
        String[] result = DSpaceControlledVocabulary.getPluginNames();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getMatches method, of class DSpaceControlledVocabulary.
     * @throws java.io.IOException passed through.
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetMatches() throws IOException, ClassNotFoundException {
        System.out.println("getMatches");

        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        // Ensure that 'id' attribute is optional
        String text = "north 40";
        int start = 0;
        int limit = 10;
        String locale = null;
        // This "farm" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE), "farm");
        assertNotNull(instance);
        Choices result = instance.getMatches(text, start, limit, locale);
        assertNotEquals("At least one match expected", 0, result.values.length);
        assertEquals("north 40", result.values[0].value);
    }

    /**
     * Test of getMatches method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetMatchesNoLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        String labelPart = "Alge";
        int start = 0;
        int limit = 10;
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getMatches(labelPart, start, limit, null);
        assertNotEquals("At least one match expected", 0, result.values.length);
        assertEquals(idValue, result.values[0].value);
        assertEquals("Algeria", result.values[0].label);
    }

    /**
     * Test of getBestMatch method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetBestMatchIdValueNoLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getBestMatch(idValue, null);
        assertNotEquals("At least one match expected", 0, result.values.length);
        assertEquals(idValue, result.values[0].value);
        assertEquals("Algeria", result.values[0].label);
    }

    /**
     * Test of getMatches method of class DSpaceControlledVocabulary
     * using a localized controlled vocabulary with valid locale parameter
     * (localized label returned).
     * @throws java.lang.ClassNotFoundException if class under test cannot be found.
     */
    @Test
    public void testGetMatchesGermanLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        String labelPart = "Alge";
        int start = 0;
        int limit = 10;
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getMatches(labelPart, start, limit, "de");
        assertNotEquals("At least one match expected", 0, result.values.length);
        assertEquals(idValue, result.values[0].value);
        assertEquals("Algerien", result.values[0].label);
    }

    /**
     * Test of getBestMatch method of class DSpaceControlledVocabulary
     * using a localized controlled vocabulary with valid locale parameter
     * (localized label returned).
     * @throws java.lang.ClassNotFoundException if class under test cannot be found.
     */
    @Test
    public void testGetBestMatchIdValueGermanLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getBestMatch(idValue, "de");
        assertNotEquals("At least one match expected", 0, result.values.length);
        assertEquals(idValue, result.values[0].value);
        assertEquals("Algerien", result.values[0].label);
    }

    /**
     * Test of getChoice method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetChoiceNoLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choice result = instance.getChoice(idValue, null);
        assertEquals(idValue, result.value);
        assertEquals("Algeria", result.label);
    }

    /**
     * Test of getChoice method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with valid locale parameter (localized
     * label returned)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetChoiceGermanLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choice result = instance.getChoice(idValue, "de");
        assertEquals(idValue, result.value);
        assertEquals("Algerien", result.label);
    }

    /**
     * Test of getBestMatch method, of class DSpaceControlledVocabulary.
     */
/*
    @Test
    public void testGetBestMatch()
    {
        System.out.println("getBestMatch");
        String field = "";
        String text = "";
        int collection = 0;
        String locale = "";
        DSpaceControlledVocabulary instance = new DSpaceControlledVocabulary();
        Choices expResult = null;
        Choices result = instance.getBestMatch(field, text, collection, locale);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getLabel method, of class DSpaceControlledVocabulary.
     */
/*
    @Test
    public void testGetLabel()
    {
        System.out.println("getLabel");
        String field = "";
        String key = "";
        String locale = "";
        DSpaceControlledVocabulary instance = new DSpaceControlledVocabulary();
        String expResult = "";
        String result = instance.getLabel(field, key, locale);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
