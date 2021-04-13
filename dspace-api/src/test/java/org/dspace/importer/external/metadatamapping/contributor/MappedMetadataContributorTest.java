/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.util.SimpleMapConverter;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link MappedMetadataContributor}
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class MappedMetadataContributorTest {

    @Test
    public void valuesAreMapped() {


        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList(
            metadatum("dc", "first", "present", "validValue"),
            metadatum("dc", "first", "absent", "absentValue"),
            metadatum("dc", "first", "blank", ""),
            metadatum("dc", "first", "null", null)));


        SimpleMapConverter mapConverter = mock(SimpleMapConverter.class);
        Mockito.when(mapConverter.getValue("validValue")).thenReturn("valueReturned");
        Mockito.when(mapConverter.getValue("absentValue")).thenReturn("defaultValue");

        MappedMetadataContributor contributor = new MappedMetadataContributor(innerContributor,
            mapConverter);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Map<String, String> metadata = contributedMetadata
            .stream()
            .collect(Collectors.toMap(dto ->
                    dto.getSchema() + "." +
                        dto.getElement() + "." +
                        dto.getQualifier(),
                MetadatumDTO::getValue));

        assertThat(metadata.get("dc.first.present"), is("valueReturned"));
        assertThat(metadata.get("dc.first.absent"), is("defaultValue"));
        assertThat(metadata.get("dc.first.blank"), is(""));
        assertThat(metadata.get("dc.first.null"), is(""));

    }

    @Test
    public void emptyMetadataListFromInner() {
        final MetadataContributor<String> innerContributor = metadataContributorReturning(Collections.emptyList());

        SimpleMapConverter mapConverter = mock(SimpleMapConverter.class);

        MappedMetadataContributor contributor = new MappedMetadataContributor(innerContributor,
            mapConverter);

        final Collection<MetadatumDTO> metadataCollection = contributor.contributeMetadata("foo");

        assertThat(metadataCollection, is(Collections.emptyList()));
        verifyNoInteractions(mapConverter);
    }

    private MetadatumDTO metadatum(final String schema, final String element, final String qualifier,
                                   final String value) {
        final MetadatumDTO metadatumDTO = new MetadatumDTO();
        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);
        return metadatumDTO;
    }

    private MetadataContributor<String> metadataContributorReturning(final List<MetadatumDTO> contributedMetadata) {
        return new MetadataContributor<>() {
            @Override
            public void setMetadataFieldMapping(final MetadataFieldMapping<String, MetadataContributor<String>> rt) {}

            @Override
            public Collection<MetadatumDTO> contributeMetadata(final String t) {
                return contributedMetadata;
            }
        };
    }
}
