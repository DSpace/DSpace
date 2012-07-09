/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import mockit.UsingMocksAndStubs;
import org.dspace.MockConfigurationManager;
import org.dspace.core.PluginManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.*;

/**
 * Unit tests for DSpaceControlledVocabulary.
 *
 * @author mwood
 */
@UsingMocksAndStubs(value=MockConfigurationManager.class)
public class DSpaceControlledVocabularyTest
{
    public DSpaceControlledVocabularyTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass()
            throws Exception
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
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
     */
    @Test
    public void testGetMatches() throws IOException, ClassNotFoundException
    {
        System.out.println("getMatches");

        // Set up the PluginManager
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";
        final String PLUGIN_NAME = "org.dspace.content.authority.DSpaceControlledVocabulary";

        MockConfigurationManager.setProperty("dspace.dir",
                System.getProperty("dspace.dir.static"));
        MockConfigurationManager.setProperty(
                "plugin.selfnamed." + PLUGIN_INTERFACE, PLUGIN_NAME);

        // Ensure that 'id' attribute is optional
        String field = null; // not used
        String text = "north 40";
        int collection = 0;
        int start = 0;
        int limit = 0;
        String locale = null;
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
                PluginManager.getNamedPlugin(Class.forName(PLUGIN_INTERFACE), "farm");
        assertNotNull(instance);
        Choices result = instance.getMatches(field, text, collection, start,
                limit, locale);
        assertEquals("the farm::north 40", result.values[0].value);
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
