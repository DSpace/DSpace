/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.dspace.content.enhancer.source.RawMetadataValue;
import org.dspace.discovery.indexobject.ItemIndexFactoryImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SolrMetadataSource}.
 *
 * <p>These tests build a {@link SolrDocument} by hand and check how the values written by the item
 * indexer are read back.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SolrMetadataSourceTest {

    private static final String FIELD = "dc.contributor.author";

    private static final String STORED_FIELD = FIELD + "_stored";

    private SolrDocument document;

    @Before
    public void setUp() {
        document = new SolrDocument();
    }

    @Test
    public void testGetValuesMapsEveryStoredEntry() {
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
    public void testGetValuesMapsEntryWithoutSeparators() {
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
    public void testGetValuesReturnsEmptyListWhenStoredFieldIsMissing() {
        document.addField(FIELD, "not a value written by the indexer");

        assertTrue(new SolrMetadataSource(document).getValues(FIELD).isEmpty());
        assertTrue(new SolrMetadataSource(document).getValues("dc.subject").isEmpty());
    }

    /**
     * Build an entry in the format written by the item indexer.
     *
     * @param value     the metadata value
     * @param label     the preferred label, or the {@code "null"} placeholder
     * @param variants  the variants, or the {@code "null"} placeholder
     * @param authority the authority, or the {@code "null"} placeholder
     * @param language  the language, or the {@code "null"} placeholder
     * @return the stored entry
     */
    private static String stored(String value, String label, String variants, String authority, String language) {
        String separator = ItemIndexFactoryImpl.STORE_SEPARATOR;
        return value + separator + label + separator + variants + separator + authority + separator + language;
    }

}
