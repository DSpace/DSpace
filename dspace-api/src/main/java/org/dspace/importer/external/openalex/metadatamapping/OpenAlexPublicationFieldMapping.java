/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openalex.metadatamapping;

import java.util.Map;

import jakarta.annotation.Resource;
import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;

/**
 * An implementation of {@link AbstractMetadataFieldMapping} responsible for
 * defining the mapping of the OpenAlex Works metadatum fields on the DSpace
 * Publication metadatum fields
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexPublicationFieldMapping extends AbstractMetadataFieldMapping {

    @Override
    @Resource(name = "openalexPublicationsMetadataFieldMap")
    public void setMetadataFieldMap(Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }
}
