/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * implementation of {@link MetadataContributor} that returns values transformed according to a defined map, or
 * default value.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class MappedMetadataContributor implements MetadataContributor<String> {

    private final MetadataContributor<String> innerContributor;

    private final Map<String, String> valuesMapping;

    private final String defaultValue;

    public MappedMetadataContributor(MetadataContributor<String> innerContributor,
                                     final Map<String, String> valuesMapping, final String defaultValue) {
        this.innerContributor = innerContributor;
        this.valuesMapping = valuesMapping;
        this.defaultValue = defaultValue;
    }

    @Override
    public void setMetadataFieldMapping(final MetadataFieldMapping<String, MetadataContributor<String>> rt) {
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(final String t) {
        final Collection<MetadatumDTO> metadata = innerContributor.contributeMetadata(t);
        for (final MetadatumDTO metadatum : metadata) {
            if (StringUtils.isBlank(metadatum.getValue())) {
                metadatum.setValue("");
                continue;
            }
            metadatum.setValue(valuesMapping.getOrDefault(metadatum.getValue(), defaultValue));
        }
        return metadata;
    }
}
