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
import static org.junit.Assert.assertNull;

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
     * Node ids (entryID, parent, child) must be compared as plain strings, not spliced into the XPath expression.
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testNodeIdIsNotEvaluatedAsXPath() throws ClassNotFoundException {
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService()
                .getNamedPlugin(Class.forName("org.dspace.content.authority.ChoiceAuthority"), "farm");
        assertNotNull(instance);

        Choice choice = instance.getChoice("s40", null);
        assertNotNull(choice);
        assertEquals("south 40", choice.label);

        // Spliced into the expression this would select //node[@id = 'x' or @label='south 40']
        assertNull(instance.getChoice("x' or @label='south 40", null));
        // ... and here the root node, returning its children
        Choices children = instance.getChoicesByParent("farm", "x' or @label='the farm", 0, 10, null);
        assertEquals(0, children.values.length);

        children = instance.getChoicesByParent("farm", "s40", 0, 10, null);
        assertEquals(1, children.values.length);
        assertEquals("pond", children.values[0].authority);

        Choice parent = instance.getParentChoice("farm", "pond", null);
        assertNotNull(parent);
        assertEquals("s40", parent.authority);
        // ... and here the parent of 'pond'
        assertNull(instance.getParentChoice("farm", "x' or @id='pond", null));
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
