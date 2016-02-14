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
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 18/09/12
 * Time: 14:41
 */

public interface  MetadataFieldMapping<RecordType,QueryType> {

        public MetadatumDTO toDCValue(MetadataFieldConfig field, String mf);

        public Collection<MetadatumDTO> resultToDCValueMapping(RecordType record);



}
