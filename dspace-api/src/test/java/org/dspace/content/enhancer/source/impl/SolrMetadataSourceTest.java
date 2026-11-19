/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.dspace.content.enhancer.source.RawMetadataValue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SolrMetadataSource}.
 *
 * <p>These tests are pure: they build a {@link SolrDocument} by hand and assert the mapping of the
 * real {@code field + "_stored"} projection produced by the item indexer.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SolrMetadataSourceTest {

    /**
     * Canonical projection separator written by {@code ItemIndexFactoryImpl}. It is hard-coded here on
     * purpose, so that a change of the production constant is caught by this test.
     */
    private static final String STORE_SEPARATOR = "\n|||\n";

    private static final String FIELD = "dc.contributor.author";

    private static final String STORED_FIELD = FIELD + "_stored";

    private SolrDocument document;

    @Before
    public void setUp() {
        document = new SolrDocument();
    }

    @Test
    public void testGetValuesMapsEveryStoredProjectionEntry() {
        document.addField(STORED_FIELD, stored("Alice", "Alice label", "null", "authority-alice", "en"));
        document.addField(STORED_FIELD, stored("Bob", "null", "bob###bobby", "null", "it"));

        List<RawMetadataValue> values = new SolrMetadataSource(document).getValues(FIELD);

        assertEquals(2, values.size());
        assertEquals(new RawMetadataValue("Alice", "en", "authority-alice", -1), values.get(0));
        assertEquals(new RawMetadataValue("Bob", "it", null, -1), values.get(1));
    }

    @Test
    public void testGetValuesReadsSingleStoredValue() {
        document.addField(STORED_FIELD, stored("Alice", "null", "null", "null", "en"));

        List<RawMetadataValue> values = new SolrMetadataSource(document).getValues(FIELD);

        assertEquals(List.of(new RawMetadataValue("Alice", "en", null, -1)), values);
    }

    @Test
    public void testGetValuesMapsEntryWithoutProjectionSegments() {
        document.addField(STORED_FIELD, "plain value");

        List<RawMetadataValue> values = new SolrMetadataSource(document).getValues(FIELD);

        assertEquals(List.of(new RawMetadataValue("plain value", null, null, -1)), values);
    }

    @Test
    public void testGetValuesSkipsEntriesWithoutUsableValue() {
        document.addField(STORED_FIELD, stored("null", "label", "null", "authority", "en"));

        List<RawMetadataValue> values = new SolrMetadataSource(document).getValues(FIELD);

        assertTrue(values.isEmpty());
    }

    @Test
    public void testGetValuesReturnsEmptyListWhenProjectionIsMissing() {
        document.addField(FIELD, "not a stored projection");

        assertTrue(new SolrMetadataSource(document).getValues(FIELD).isEmpty());
        assertTrue(new SolrMetadataSource(document).getValues("dc.subject").isEmpty());
    }

    @Test
    public void testAdapterHasNoDatabaseWorldDependency() throws IOException {
        String bytecode = readOwnBytecode();

        assertFalse("SolrMetadataSource must not depend on Context",
                bytecode.contains("org/dspace/core/Context"));
        assertFalse("SolrMetadataSource must not depend on ItemService",
                bytecode.contains("org/dspace/content/service/ItemService"));
    }

    /**
     * Build a projected entry in the format written by the item indexer.
     *
     * @param value     the metadata value
     * @param label     the preferred label, or the {@code "null"} placeholder
     * @param variants  the variants, or the {@code "null"} placeholder
     * @param authority the authority, or the {@code "null"} placeholder
     * @param language  the language, or the {@code "null"} placeholder
     * @return the projected entry
     */
    private static String stored(String value, String label, String variants, String authority, String language) {
        return value + STORE_SEPARATOR + label + STORE_SEPARATOR + variants + STORE_SEPARATOR
                + authority + STORE_SEPARATOR + language;
    }

    /**
     * Read the compiled adapter as raw text, including its constant pool, so that every type referenced
     * by fields, constructors, method signatures and method bodies is inspected.
     *
     * @return the adapter bytecode decoded one byte per character
     * @throws IOException if the adapter class file cannot be read
     */
    private static String readOwnBytecode() throws IOException {
        try (InputStream input = SolrMetadataSource.class.getResourceAsStream("SolrMetadataSource.class")) {
            assertNotNull("The compiled SolrMetadataSource class must be on the test classpath", input);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

}
