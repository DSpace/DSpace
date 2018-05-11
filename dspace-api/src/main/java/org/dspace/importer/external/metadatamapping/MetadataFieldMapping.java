/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;

import java.util.Collection;

/**
 * Represents an interface for the mapping of the metadatum fields
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */

public interface MetadataFieldMapping<RecordType,QueryType> {

    /**
     * @param field MetadataFieldConfig representing what to map the value to
     * @param value The value to map to a MetadatumDTO
     * @return A metadatumDTO created from the field and value
     */
    public MetadatumDTO toDCValue(MetadataFieldConfig field, String value);


    /**
     * Create a collection of MetadatumDTO retrieved from a given RecordType
     * @param record Used to retrieve the MetadatumDTO
     * @return Collection of MetadatumDTO
     */
    public Collection<MetadatumDTO> resultToDCValueMapping(RecordType record);



}
