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
 * @author Roeland Dillen (roeland at atmire dot com)
 * Date: 18/09/12
 * Time: 14:41
 */

public interface  MetadataFieldMapping<RecordType,QueryType> {

        /* Using a given MetadataFieldConfig, return a MetadatumDTO retrieved from a value. */
        public MetadatumDTO toDCValue(MetadataFieldConfig field, String value);

        /* Implementations need to handle how the result is processed,filtered and returned. */
        public Collection<MetadatumDTO> resultToDCValueMapping(RecordType record);



}
