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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class MultipleMetadataContributorTest {

    private MultipleMetadataContributor<Object> multipleMetadataContributor;
    private MetadataFieldConfig field;
    private MetadataContributor<Object> contributor1;
    private MetadataContributor<Object> contributor2;

    @Before
    public void setUp() {
        field = new MetadataFieldConfig("dc", "title", null);
        contributor1 = mock(MetadataContributor.class);
        contributor2 = mock(MetadataContributor.class);

        multipleMetadataContributor =
            new MultipleMetadataContributor<>(field, new LinkedList<>(Arrays.asList(contributor1, contributor2)));
    }

    @Test
    public void testContributeMetadataWithoutDuplicates() {
        MetadatumDTO dto1 = new MetadatumDTO();
        dto1.setValue("Value1");
        MetadatumDTO dto2 = new MetadatumDTO();
        dto2.setValue("Value2");

        when(contributor1.contributeMetadata(Mockito.any())).thenReturn(List.of(dto1));
        when(contributor2.contributeMetadata(Mockito.any())).thenReturn(List.of(dto2));

        Collection<MetadatumDTO> result = multipleMetadataContributor.contributeMetadata(new Object());

        assertEquals(2, result.size());
    }

    @Test
    public void testContributeMetadataWithDuplicates() {
        MetadatumDTO dto1 = new MetadatumDTO();
        dto1.setValue("DuplicateValue");
        MetadatumDTO dto2 = new MetadatumDTO();
        dto2.setValue("DuplicateValue");

        when(contributor1.contributeMetadata(Mockito.any())).thenReturn(List.of(dto1));
        when(contributor2.contributeMetadata(Mockito.any())).thenReturn(List.of(dto2));

        multipleMetadataContributor.setEnsureUniqueValues(true);
        Collection<MetadatumDTO> result = multipleMetadataContributor.contributeMetadata(new Object());

        assertEquals(1, result.size());
    }

    @Test
    public void testContributeMetadataWithEmptyContributors() {
        multipleMetadataContributor = new MultipleMetadataContributor<>(field, new LinkedList<>());
        Collection<MetadatumDTO> result = multipleMetadataContributor.contributeMetadata(new Object());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testContributeMetadataWithNullValues() {
        MetadatumDTO dto1 = new MetadatumDTO();
        dto1.setValue(null);
        MetadatumDTO dto2 = new MetadatumDTO();
        dto2.setValue("ValidValue");

        when(contributor1.contributeMetadata(Mockito.any())).thenReturn(List.of(dto1));
        when(contributor2.contributeMetadata(Mockito.any())).thenReturn(List.of(dto2));

        Collection<MetadatumDTO> result = multipleMetadataContributor.contributeMetadata(new Object());

        assertEquals(2, result.size());
    }
}
