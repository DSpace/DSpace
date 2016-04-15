/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.metadatamapping;

import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;

import javax.annotation.Resource;
import java.util.Map;

/**
 * An implementation of {@link AbstractMetadataFieldMapping}
 * Responsible for defining the mapping of the Pubmed metadatum fields on the DSpace metadatum fields
 *
 * @author jonas - (jonas at atmire dot com)
 */
public class PubmedFieldMapping extends AbstractMetadataFieldMapping {

    /** Defines which metadatum is mapped on which metadatum. Note that while the key must be unique it
     * only matters here for postprocessing of the value. The mapped MetadatumContributor has full control over
     * what metadatafield is generated.
     * @param metadataFieldMap The map containing the link between retrieve metadata and metadata that will be set to the item.
     */
    @Override
    @Resource (name = "pubmedMetadataFieldMap")
    public void setMetadataFieldMap(Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }
}
