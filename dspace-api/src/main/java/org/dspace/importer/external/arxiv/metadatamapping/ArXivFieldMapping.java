/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.arxiv.metadatamapping;

import java.util.Map;
import javax.annotation.Resource;

import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;

public class ArXivFieldMapping extends AbstractMetadataFieldMapping {

    @Override
    @Resource(name = "arxivMetadataFieldMap")
    public void setMetadataFieldMap(Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
