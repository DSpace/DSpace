/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.scorer;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.external.model.ExternalDataObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;


/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class AuthorNamesScorerTest {

    @Mock
    private ItemService itemService;
    private AuthorNamesScorer authorNamesScorer;
    private Item researcher;
    private ExternalDataObject importRecord;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authorNamesScorer = new AuthorNamesScorer();

        authorNamesScorer.setContributorMetadata(List.of("dc.contributor.author"));
        authorNamesScorer.setNames(List.of("dc.title"));
        researcher = mock(Item.class);
        importRecord = mock(ExternalDataObject.class);
        ReflectionTestUtils.setField(authorNamesScorer, "itemService", itemService);
    }

    @Test
    public void testComputeEvidence_DiacriticStrings() {
        // Mock the metadata retrieval
        List<MetadataValue> metadataValues = List.of(new TestMetadataValue("ùúìíòóèéàá"));
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(metadataValues);

        // Mock the import record data
        when(importRecord.getMetadata()).thenReturn(List.of(new MetadataValueDTO(
            "dc", "contributor", "author", null, "uuiiooeeaa")));

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNotNull(evidence);
        assertTrue(evidence.getScore() > 0);
    }

    @Test
    public void testComputeEvidence_MatchingAuthor() {
        // Mock the metadata retrieval
        List<MetadataValue> metadataValues = List.of(new TestMetadataValue("Jose Marquez"));
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(metadataValues);

        // Mock the import record data
        when(importRecord.getMetadata()).thenReturn(List.of(new MetadataValueDTO(
            "dc", "contributor", "author", null, "José Márquez")));

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNotNull(evidence);
        assertEquals(100, evidence.getScore(), 0.0);
    }

    @Test
    public void testComputeEvidence_NoMatchingAuthor() {
        // Mock the metadata retrieval
        List<MetadataValue> metadataValues = List.of(new TestMetadataValue("Jose Marquez"));
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(metadataValues);

        // Mock the import record data (name does not match)
        when(importRecord.getMetadata()).thenReturn(List.of(new MetadataValueDTO(
            "dc", "contributor", "author", null, "John Doe")));

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNull(evidence);
    }

    @Test
    public void testComputeEvidence_PartialMatchWithNormalization() {
        // Mock the metadata retrieval
        List<MetadataValue> metadataValues = List.of(
            new TestMetadataValue("Miguel de Cervantes Saavedra"),
            new TestMetadataValue("Miguel de Cervantes S."),
            new TestMetadataValue("Miguel de Cervantes")
        );
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(metadataValues);

        // Mock the import record data
        when(importRecord.getMetadata()).thenReturn(List.of(new MetadataValueDTO(
            "dc", "contributor", "author", null, "Miguel de Cervantes")));

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNotNull(evidence);
        assertEquals(68, evidence.getScore(), 0.0);
    }

    @Test
    public void testComputeEvidence_NonLetterCharacters_Matching() {
        // Mock the metadata retrieval
        List<MetadataValue> metadataValues = List.of(
            new TestMetadataValue("Smith, Anna"));
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(metadataValues);

        // Mock the import record data (matching the second author)
        when(importRecord.getMetadata()).thenReturn(List.of(new MetadataValueDTO(
            "dc", "contributor", "author", null, "Anna Smith")));

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNotNull(evidence);
        assertTrue(evidence.getScore() > 0);
    }

    @Test
    public void testComputeEvidence_NoMetadataInItem() {
        // Mock the metadata retrieval
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(List.of());

        // Mock the import record data
        when(importRecord.getMetadata()).thenReturn(List.of(new MetadataValueDTO(
            "dc", "contributor", "author", null, "José Márquez")));

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNull(evidence);
    }

    @Test
    public void testComputeEvidence_EmptyNamesInImportRecord() {
        // Mock the metadata retrieval
        List<MetadataValue> metadataValues = List.of(new TestMetadataValue("Jose Marquez"));
        when(itemService.getMetadataByMetadataString(researcher, "dc.title")).thenReturn(metadataValues);

        // Mock the import record data (no author names)
        when(importRecord.getMetadata()).thenReturn(List.of());

        // Execute the scoring
        SuggestionEvidence evidence = authorNamesScorer.computeEvidence(researcher, importRecord);

        // Verify the result
        assertNull(evidence);
    }


    // Subclass to access the protected constructor
    private static class TestMetadataValue extends MetadataValue {
        public TestMetadataValue(String value) {
            super();
            setValue(value);
        }
    }

}