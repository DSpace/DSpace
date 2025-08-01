/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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
        assertEquals("one", instance.schema, "Incorrect schema");
        assertEquals("two", instance.element, "Incorrect element");
        assertEquals("three", instance.qualifier, "Incorrect qualifier");
    }

    @Test
    public void testConstruct2() {
        MetadataFieldName instance = new MetadataFieldName("one", "two");
        assertEquals("one", instance.schema, "Incorrect schema");
        assertEquals("two", instance.element, "Incorrect element");
        assertNull(instance.qualifier, "Incorrect qualifier");
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructNull() {
        assertThrows(NullPointerException.class, () -> {
            new MetadataFieldName("one", null);
        });
    }

    /**
     * Test of parse method using a 3-part name.
     */
    @Test
    public void testParse3() {
        String[] results = MetadataFieldName.parse(STRING_NAME_3);
        assertEquals("one", results[0], STRING_NAME_3);
        assertEquals("two", results[1], STRING_NAME_3);
        assertEquals("three", results[2], STRING_NAME_3);
    }

    /**
     * Test of parse method using a 2-part name.
     */
    @Test
    public void TestParse2() {
        String[] results = MetadataFieldName.parse(STRING_NAME_2);
        assertEquals("one", results[0], STRING_NAME_2);
        assertEquals("two", results[1], STRING_NAME_2);
        assertNull(results[2], STRING_NAME_2);
    }

    /**
     * Test of parse method using an illegal 1-part name.
     */
    @Test
    public void TestParse1() {
        assertThrows(IllegalArgumentException.class, () ->
            MetadataFieldName.parse("one"));
    }

    /**
     * Test of parse method using an illegal 0-part (empty) name.
     */
    @Test
    public void TestParse0() {
        assertThrows(IllegalArgumentException.class, () ->
            MetadataFieldName.parse(""));
    }

    /**
     * Test of parse method using an illegal null name.
     */
    @Test
    @SuppressWarnings("null")
    public void TestParseNull() {
        assertThrows(NullPointerException.class, () ->
            MetadataFieldName.parse(null));
    }

    /**
     * Test of toString method using a 3-part name.
     */
    @Test
    public void testToString3() {
        MetadataFieldName instance = new MetadataFieldName("one", "two", "three");
        String name = instance.toString();
        assertEquals("one.two.three", name, "Stringified name not assembled correctly");
    }

    /**
     * Test of toString method using a 2-part name.
     */
    @Test
    public void testToString2() {
        MetadataFieldName instance = new MetadataFieldName("one", "two");
        String name = instance.toString();
        assertEquals("one.two", name, "Stringified name not assembled correctly");
    }
}
