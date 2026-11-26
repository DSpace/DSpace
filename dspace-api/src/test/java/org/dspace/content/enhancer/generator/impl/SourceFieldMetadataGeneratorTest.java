/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.generator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.RawMetadataValue;
import org.junit.Before;
import org.junit.Test;

/**
 * Pure unit tests for {@link SourceFieldMetadataGenerator}, driven by an in-memory
 * {@link MetadataSource} fake.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SourceFieldMetadataGeneratorTest {

    private static final String SOURCE_FIELD = "person.identifier.orcid";

    private SourceFieldMetadataGenerator generator;
    private FakeMetadataSource source;

    @Before
    public void setUp() {
        generator = new SourceFieldMetadataGenerator();
        generator.setSourceField(SOURCE_FIELD);
        source = new FakeMetadataSource();
    }

    @Test
    public void testGetSourceFieldReturnsConfiguredField() {
        assertEquals(SOURCE_FIELD, generator.getSourceField());
    }

    @Test
    public void testExtractSingleValue() {
        source.add(SOURCE_FIELD, new RawMetadataValue("0000-0001-2345-6789", null, null, -1));

        List<MetadataValueDTO> result = generator.extract(source);

        assertEquals(1, result.size());
        MetadataValueDTO dto = result.get(0);
        assertEquals("person", dto.getSchema());
        assertEquals("identifier", dto.getElement());
        assertEquals("orcid", dto.getQualifier());
        assertEquals("0000-0001-2345-6789", dto.getValue());
        assertNull(dto.getLanguage());
        assertNull(dto.getAuthority());
        assertEquals(-1, dto.getConfidence());
    }

    @Test
    public void testExtractMultipleValuesPreservesOrder() {
        source.add(SOURCE_FIELD, new RawMetadataValue("first", "en", null, -1));
        source.add(SOURCE_FIELD, new RawMetadataValue("second", "it", null, -1));

        List<MetadataValueDTO> result = generator.extract(source);

        assertEquals(2, result.size());
        assertEquals("first", result.get(0).getValue());
        assertEquals("en", result.get(0).getLanguage());
        assertEquals("second", result.get(1).getValue());
        assertEquals("it", result.get(1).getLanguage());
    }

    @Test
    public void testExtractEmptyValues() {
        List<MetadataValueDTO> result = generator.extract(source);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractPreservesAuthorityAndConfidence() {
        source.add(SOURCE_FIELD, new RawMetadataValue("Jane Doe", "en", "authority-key", 600));

        List<MetadataValueDTO> result = generator.extract(source);

        assertEquals(1, result.size());
        MetadataValueDTO dto = result.get(0);
        assertEquals("Jane Doe", dto.getValue());
        assertEquals("en", dto.getLanguage());
        assertEquals("authority-key", dto.getAuthority());
        assertEquals(600, dto.getConfidence());
    }

    @Test
    public void testExtractWithoutAuthority() {
        source.add(SOURCE_FIELD, new RawMetadataValue("John Doe", "en", null, -1));

        List<MetadataValueDTO> result = generator.extract(source);

        assertEquals(1, result.size());
        assertNull(result.get(0).getAuthority());
        assertEquals(-1, result.get(0).getConfidence());
    }

    @Test
    public void testExtractReadsTheConfiguredSourceField() {
        source.add(SOURCE_FIELD, new RawMetadataValue("value", null, null, -1));

        generator.extract(source);

        assertEquals(SOURCE_FIELD, source.getLastRequestedField());
    }

    /**
     * In-memory {@link MetadataSource} fake returning canned values per field and recording the
     * last requested field.
     */
    private static class FakeMetadataSource implements MetadataSource {

        private final Map<String, List<RawMetadataValue>> values = new HashMap<>();
        private String lastRequestedField;

        public void add(String field, RawMetadataValue value) {
            values.computeIfAbsent(field, key -> new ArrayList<>()).add(value);
        }

        public String getLastRequestedField() {
            return lastRequestedField;
        }

        @Override
        public List<RawMetadataValue> getValues(String field) {
            lastRequestedField = field;
            return values.getOrDefault(field, Collections.emptyList());
        }
    }

}
