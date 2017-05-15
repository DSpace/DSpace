/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir;

import java.util.*;
import javax.annotation.*;
import org.apache.axiom.om.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.metadatamapping.contributor.*;
import org.springframework.stereotype.*;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 14/12/12
 * Time: 11:02
 */
@Component
public class ScidirMetadataFieldMapping extends AbstractMetadataFieldMapping<OMElement> {

    @Override
    @Resource(name="scidirMetadataFieldMap")
    public void setMetadataFieldMap(Map<MetadataFieldConfig, MetadataContributor<OMElement>> metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }


}
