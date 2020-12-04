/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;

/**
 * Unit tests for {@link MappedMetadataContributor}
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class MappedMetadataContributorTest {

    private MappedMetadataContributor contributor;


    @Test
    public void valuesAreMapped() {

        final MetadataContributor<String> innerContributor = new MetadataContributor<>() {
            @Override
            public void setMetadataFieldMapping(final MetadataFieldMapping<String, MetadataContributor<String>> rt) {
            }

            @Override
            public Collection<MetadatumDTO> contributeMetadata(final String t) {
                return Arrays.asList(
                    metadatum("dc", "first", "present", "validValue"),
                    metadatum("dc", "first", "absent", "absentValue"),
                    metadatum("dc", "first", "blank", ""),
                    metadatum("dc", "first", "null", null));
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
        };
        contributor = new MappedMetadataContributor(innerContributor,
            Collections.singletonMap("validValue", "valueReturned"),
            "defaultValue");

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
        final MetadataContributor<String> innerContributor = new MetadataContributor<>() {
            @Override
            public void setMetadataFieldMapping(final MetadataFieldMapping<String, MetadataContributor<String>> rt) {
            }

            @Override
            public Collection<MetadatumDTO> contributeMetadata(final String t) {
                return Collections.emptyList();
            }
        };

        contributor = new MappedMetadataContributor(innerContributor,
            Collections.singletonMap("validValue", "valueReturned"),
            "defaultValue");

        final Collection<MetadatumDTO> metadataCollection = contributor.contributeMetadata("foo");

        assertThat(metadataCollection, is(Collections.emptyList()));
    }
}
