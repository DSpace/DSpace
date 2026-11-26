/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrDocument;
import org.dspace.content.authority.Choices;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.RawMetadataValue;
import org.dspace.discovery.indexobject.ItemIndexFactoryImpl;

/**
 * {@link MetadataSource} that reads metadata values from a Solr document.
 *
 * <p>Values are read from the {@code field + "_stored"} field produced by
 * {@link ItemIndexFactoryImpl}. Each entry is a single string whose segments are separated
 * by {@link ItemIndexFactoryImpl#STORE_SEPARATOR} in the order:</p>
 *
 * <pre>
 * value | preferred label | variants | authority | language
 * </pre>
 *
 * <p>Only the value, authority and language segments are used. The indexer writes the literal
 * {@code "null"} in place of an absent segment, so that literal is mapped to {@code null}. The
 * authority confidence is not stored in the index and is therefore always reported as
 * {@link Choices#CF_UNSET}. An entry without a separator is treated as a plain value, with
 * {@code null} language and authority.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SolrMetadataSource implements MetadataSource {

    private static final String STORED_SUFFIX = "_stored";

    private static final String NULL_LITERAL = "null";

    private static final int VALUE_SEGMENT = 0;

    private static final int AUTHORITY_SEGMENT = 3;

    private static final int LANGUAGE_SEGMENT = 4;

    private static final Pattern STORE_SEPARATOR_PATTERN =
            Pattern.compile(Pattern.quote(ItemIndexFactoryImpl.STORE_SEPARATOR));

    private final SolrDocument document;

    /**
     * Create a metadata source over the given Solr document.
     *
     * @param document the Solr document to read, must not be {@code null}
     */
    public SolrMetadataSource(SolrDocument document) {
        this.document = document;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The values are read from the {@code field + "_stored"} field. An absent field
     * yields an empty list.</p>
     */
    @Override
    public List<RawMetadataValue> getValues(String field) {
        Collection<Object> storedValues = document.getFieldValues(field + STORED_SUFFIX);
        if (storedValues == null || storedValues.isEmpty()) {
            return Collections.emptyList();
        }
        List<RawMetadataValue> values = new ArrayList<>(storedValues.size());
        for (Object storedValue : storedValues) {
            RawMetadataValue value = toRawMetadataValue(storedValue);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Map one stored entry to a raw metadata value.
     *
     * @param storedEntry the stored entry, may be {@code null}
     * @return the mapped value, or {@code null} when the entry carries no usable value
     */
    private RawMetadataValue toRawMetadataValue(Object storedEntry) {
        if (storedEntry == null) {
            return null;
        }
        String[] segments = STORE_SEPARATOR_PATTERN.split(storedEntry.toString());
        if (segments.length <= VALUE_SEGMENT) {
            return null;
        }
        String value = nullOrValue(segments[VALUE_SEGMENT]);
        if (value == null) {
            return null;
        }
        return new RawMetadataValue(value, segmentOrNull(segments, LANGUAGE_SEGMENT),
                segmentOrNull(segments, AUTHORITY_SEGMENT), Choices.CF_UNSET);
    }

    /**
     * Read an optional segment from a stored entry.
     *
     * @param segments the stored entry segments
     * @param index    the zero-based segment index
     * @return the segment value, or {@code null} when the segment is absent or null
     */
    private static String segmentOrNull(String[] segments, int index) {
        return index < segments.length ? nullOrValue(segments[index]) : null;
    }

    /**
     * Translate the indexer's null placeholder into a real {@code null}.
     *
     * @param segment the segment read from the index
     * @return {@code null} for the placeholder, the segment otherwise
     */
    private static String nullOrValue(String segment) {
        return NULL_LITERAL.equals(segment) ? null : segment;
    }

}
