/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Tests for DCInput field parsing, focusing on max-occurrences functionality.
 *
 * @author Hui Hwoo
 */
public class DCInputTest {

    private DCInput createInput(Map<String, String> fieldMap) {
        return new DCInput(fieldMap, new HashMap<String, List<String>>());
    }

    private Map<String, String> baseFieldMap() {
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("dc-schema", "dc");
        fieldMap.put("dc-element", "subject");
        fieldMap.put("label", "Subject");
        fieldMap.put("input-type", "onebox");
        fieldMap.put("hint", "Enter subjects");
        fieldMap.put("repeatable", "true");
        return fieldMap;
    }

    @Test
    public void testMaxOccurrencesDefault() {
        Map<String, String> fieldMap = baseFieldMap();
        DCInput input = createInput(fieldMap);

        assertEquals(-1, input.getMaxOccurrences());
        assertTrue(input.isRepeatable());
    }

    @Test
    public void testMaxOccurrencesParsesValidValue() {
        Map<String, String> fieldMap = baseFieldMap();
        fieldMap.put("max-occurrences", "5");
        DCInput input = createInput(fieldMap);

        assertEquals(5, input.getMaxOccurrences());
    }

    @Test
    public void testMaxOccurrencesParsesWithWhitespace() {
        Map<String, String> fieldMap = baseFieldMap();
        fieldMap.put("max-occurrences", "  10  ");
        DCInput input = createInput(fieldMap);

        assertEquals(10, input.getMaxOccurrences());
    }

    @Test
    public void testMaxOccurrencesInvalidValueDefaultsToUnlimited() {
        Map<String, String> fieldMap = baseFieldMap();
        fieldMap.put("max-occurrences", "abc");
        DCInput input = createInput(fieldMap);

        assertEquals(-1, input.getMaxOccurrences());
    }

    @Test
    public void testMaxOccurrencesEmptyValueDefaultsToUnlimited() {
        Map<String, String> fieldMap = baseFieldMap();
        fieldMap.put("max-occurrences", "");
        DCInput input = createInput(fieldMap);

        assertEquals(-1, input.getMaxOccurrences());
    }

    @Test
    public void testRepeatableFieldParsing() {
        Map<String, String> fieldMap = baseFieldMap();
        fieldMap.put("repeatable", "true");
        DCInput input = createInput(fieldMap);
        assertTrue(input.isRepeatable());

        fieldMap.put("repeatable", "false");
        input = createInput(fieldMap);
        assertFalse(input.isRepeatable());
    }
}
