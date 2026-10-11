/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link EnhancedSimpleMetadataContributor} splits a value into several metadata values.
 */
public class EnhancedSimpleMetadataContributorTest {

    private EnhancedSimpleMetadataContributor contributor;

    @BeforeEach
    public void setUp() {
        contributor = new EnhancedSimpleMetadataContributor();
        contributor.setKey("1");
        contributor.setField(new MetadataFieldConfig("dc", "contributor", "author"));
    }

    @Test
    public void testSplitsOnDelimiterAndHonoursQuotes() {
        assertEquals(List.of("Author 1", "Author 2, something", "Author 3"),
                     split("Author 1, \"Author 2, something\", Author 3"));
    }

    @Test
    public void testSingleValueIsKept() {
        assertEquals(List.of("Doe, Jane"), split("\"Doe, Jane\""));
        assertEquals(List.of("Jane Doe"), split("Jane Doe"));
    }

    @Test
    public void testDoubledQuotesInsideQuotedValue() {
        assertEquals(List.of("The \"best\" author", "Other"), split("\"The \"\"best\"\" author\", Other"));
    }

    @Test
    public void testCustomDelimiter() {
        contributor.setDelimiter(';');
        assertEquals(List.of("Doe, Jane", "Smith, John"), split("Doe, Jane; Smith, John"));
    }

    @Test
    public void testUnparsableValueIsKeptWhole() {
        assertEquals(List.of("\"unterminated, value"), split("\"unterminated, value"));
        assertEquals(List.of(""), split(""));
    }

    private List<String> split(String value) {
        PlainMetadataKeyValueItem item = new PlainMetadataKeyValueItem();
        item.setKey("1");
        item.setValue(value);
        PlainMetadataSourceDto dto = new PlainMetadataSourceDto();
        dto.setMetadata(List.of(item));
        return contributor.contributeMetadata(dto).stream().map(MetadatumDTO::getValue).toList();
    }
}
