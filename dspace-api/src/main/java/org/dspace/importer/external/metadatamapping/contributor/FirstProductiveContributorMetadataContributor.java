/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * Apply the first {@link MetadataContributor} that contributes at least one {@link MetadatumDTO}.
 */
public class FirstProductiveContributorMetadataContributor<T> implements MetadataContributor<T> {
    final private List<MetadataContributor<T>> contributors;
    public FirstProductiveContributorMetadataContributor(List<MetadataContributor<T>> contributors) {
        this.contributors = contributors;
    }

    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> rt) {
        for (var contributor : this.contributors) {
            contributor.setMetadataFieldMapping(rt);
        }
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        // Find the first subcontributor that produces anything, and pass on what it produces.
        return contributors.stream()
            .map(con -> con.contributeMetadata(t))
            .filter(coll -> !coll.isEmpty())
            .findFirst()

            // If every subcontributor produces nothing, produce nothing.
            .orElse(List.of());
    }
}
