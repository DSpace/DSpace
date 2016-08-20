/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

import java.util.Collection;

/**
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public interface MetadataContributor<RecordType> {

    /**
     * Set the metadataFieldMapping
     * @param rt the MetadataFieldMapping object to set to the MetadataContributor
     */
    public void setMetadataFieldMapping(MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> rt);

    /**
     * Implementations have the responsibility to process/map their own type of metadata based on a given record
     * and return a collection of the generalised MetadatumDTO objects
     * @param t The recordType object to retrieve metadata from
     * @return A collection of MetadatumDTO objects, retrieve from the recordtype
     */
    public Collection<MetadatumDTO> contributeMetadata(RecordType t);
}
