/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * 
 * @author mwood
 */
public class MetadataFieldNameTest {
    private static final String STRING_NAME_3 = "one.two.three";
    private static final String STRING_NAME_2 = "one.two";

    public MetadataFieldNameTest() {
    }

    @Test
    public void testConstruct3() {
        MetadataFieldName instance = new MetadataFieldName("one", "two", "three");
        assertEquals("Incorrect schema", "one", instance.schema);
        assertEquals("Incorrect element", "two", instance.element);
        assertEquals("Incorrect qualifier", "three", instance.qualifier);
    }

    @Test
    public void testConstruct2() {
        MetadataFieldName instance = new MetadataFieldName("one", "two");
        assertEquals("Incorrect schema", "one", instance.schema);
        assertEquals("Incorrect element", "two", instance.element);
        assertNull("Incorrect qualifier", instance.qualifier);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructNull() {
        new MetadataFieldName("one", null);
    }

    /**
     * Test of parse method using a 3-part name.
     */
    @Test
    public void testParse3() {
        String[] results = MetadataFieldName.parse(STRING_NAME_3);
        assertEquals(STRING_NAME_3, "one", results[0]);
        assertEquals(STRING_NAME_3, "two", results[1]);
        assertEquals(STRING_NAME_3, "three", results[2]);
    }

    /**
     * Test of parse method using a 2-part name.
     */
    @Test
    public void TestParse2() {
        String[] results = MetadataFieldName.parse(STRING_NAME_2);
        assertEquals(STRING_NAME_2, "one", results[0]);
        assertEquals(STRING_NAME_2, "two", results[1]);
        assertNull(STRING_NAME_2, results[2]);
    }

    /**
     * Test of parse method using an illegal 1-part name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void TestParse1() {
        MetadataFieldName.parse("one");
    }

    /**
     * Test of parse method using an illegal 0-part (empty) name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void TestParse0() {
        MetadataFieldName.parse("");
    }

    /**
     * Test of parse method using an illegal null name.
     */
    @Test(expected = NullPointerException.class)
    @SuppressWarnings("null")
    public void TestParseNull() {
        MetadataFieldName.parse(null);
    }

    /**
     * Test of toString method using a 3-part name.
     */
    @Test
    public void testToString3() {
        MetadataFieldName instance = new MetadataFieldName("one", "two", "three");
        String name = instance.toString();
        assertEquals("Stringified name not assembled correctly", "one.two.three", name);
    }

    /**
     * Test of toString method using a 2-part name.
     */
    @Test
    public void testToString2() {
        MetadataFieldName instance = new MetadataFieldName("one", "two");
        String name = instance.toString();
        assertEquals("Stringified name not assembled correctly", "one.two", name);
    }
}
