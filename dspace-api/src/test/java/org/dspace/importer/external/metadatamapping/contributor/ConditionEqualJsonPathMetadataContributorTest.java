/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class ConditionEqualJsonPathMetadataContributorTest {

    private final String json = "{\"field\": \"expectedValue\"}";
    @Mock
    private JsonPathMetadataProcessor leftOperandProcessor;
    @Mock
    private SimpleJsonPathMetadataContributor metadatumContributor;
    @InjectMocks
    private ConditionEqualJsonPathMetadataContributor contributor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        contributor.setRightOperand("expectedValue");
    }

    @Test
    public void testContributeMetadata_MatchingCondition() {
        when(leftOperandProcessor.processMetadata(json)).thenReturn(Collections.singleton("expectedValue"));
        Collection<MetadatumDTO> expectedMetadata = List.of(new MetadatumDTO());
        when(metadatumContributor.contributeMetadata(json)).thenReturn(expectedMetadata);

        Collection<MetadatumDTO> result = contributor.contributeMetadata(json);

        assertEquals(expectedMetadata, result);
    }

    @Test
    public void testContributeMetadata_NonMatchingCondition() {
        when(leftOperandProcessor.processMetadata(json)).thenReturn(Collections.singleton("unexpectedValue"));

        Collection<MetadatumDTO> result = contributor.contributeMetadata(json);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testContributeMetadata_EmptyOperands() {
        when(leftOperandProcessor.processMetadata(json)).thenReturn(Collections.emptyList());

        Collection<MetadatumDTO> result = contributor.contributeMetadata(json);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testContributeMetadata_NullOperands() {
        when(leftOperandProcessor.processMetadata(json)).thenReturn(null);

        Collection<MetadatumDTO> result = contributor.contributeMetadata(json);

        assertTrue(result.isEmpty());
    }
}
