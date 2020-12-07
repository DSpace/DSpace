/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.util.SimpleMapConverter;

/**
 * implementation of {@link MetadataContributor} that returns values transformed according to a defined map, or
 * default value.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class MappedMetadataContributor implements MetadataContributor<String> {

    private final MetadataContributor<String> innerContributor;

    private final SimpleMapConverter mapConverter;

    public MappedMetadataContributor(MetadataContributor<String> innerContributor,
                                     SimpleMapConverter mapConverter) {
        this.innerContributor = innerContributor;
        this.mapConverter = mapConverter;
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
            metadatum.setValue(mapConverter.getValue(metadatum.getValue()));
        }
        return metadata;
    }
}
