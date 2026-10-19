/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openaire.metadatamapping;

import java.util.Map;

import jakarta.annotation.Resource;
import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;

/**
 * An implementation of {@link AbstractMetadataFieldMapping} responsible for
 * defining the mapping of the OpenAIRE metadatum fields on the DSpace metadatum
 * fields
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAIREPublicationFieldMapping extends AbstractMetadataFieldMapping {

    @Override
    @Resource(name = "openairePublicationsMetadataFieldMap")
    public void setMetadataFieldMap(Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }
}
