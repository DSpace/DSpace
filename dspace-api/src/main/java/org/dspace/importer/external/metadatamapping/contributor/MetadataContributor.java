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
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 11/01/13
 * Time: 09:18
 */
public interface MetadataContributor<RecordType> {

    public void setMetadataFieldMapping(MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> rt);

    public Collection<MetadatumDTO> contributeMetadata(RecordType t);
}
