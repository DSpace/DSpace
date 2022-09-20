/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;

import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * Wrapper class used to split another MetadataContributor's output into distinct values.
 * The split is performed by matching a regular expression against the wrapped MetadataContributor's output.
 *
 * @author Philipp Rumpf (philipp.rumpf@uni-bamberg.de)
 */

public class SplitMetadataContributor<T> implements MetadataContributor<T> {
    private final MetadataContributor<T> innerContributor;
    private final String regex;

    /**
     * @param innerContributor      The MetadataContributor whose output is split
     * @param regex                 A regular expression matching the separator between different values
     */
    public SplitMetadataContributor(MetadataContributor<T> innerContributor, String regex) {
        this.innerContributor = innerContributor;
        this.regex = regex;
    }

    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> rt) {

    }

    /**
     * Each metadatum returned by the wrapped MetadataContributor is split into one or more metadata values
     * based on the provided regular expression.
     *
     * @param t The recordType object to retrieve metadata from
     * @return The collection of processed metadata values
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        Collection<MetadatumDTO> metadata = innerContributor.contributeMetadata(t);
        ArrayList<MetadatumDTO> splitMetadata = new ArrayList<>();
        for (MetadatumDTO metadatumDTO : metadata) {
            String[] split = metadatumDTO.getValue().split(regex);
            for (String splitItem : split) {
                MetadatumDTO splitMetadatumDTO = new MetadatumDTO();
                splitMetadatumDTO.setSchema(metadatumDTO.getSchema());
                splitMetadatumDTO.setElement(metadatumDTO.getElement());
                splitMetadatumDTO.setQualifier(metadatumDTO.getQualifier());
                splitMetadatumDTO.setValue(splitItem);
                splitMetadata.add(splitMetadatumDTO);
            }
        }
        return splitMetadata;
    }
}
