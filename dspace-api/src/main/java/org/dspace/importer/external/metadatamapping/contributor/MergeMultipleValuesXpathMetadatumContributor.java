/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.*;
import org.apache.axiom.om.*;
import org.apache.commons.lang.*;
import org.dspace.importer.external.metadatamapping.*;

/**
 * Created by jonas - jonas@atmire.com on 20/05/16.
 */
public class MergeMultipleValuesXpathMetadatumContributor extends SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {

    @Override
    protected void addRetrievedValueToMetadata(List<MetadatumDTO> values, String value) {
        boolean existingField = false;

        for(int i = 0; i < values.size(); i++) {
            if(StringUtils.equals(values.get(i).getField(),getField().getField())){
                values.get(i).setValue(values.get(i).getValue() + " " + value);

                existingField = true;
                break;
            }
        }

        if(!existingField) {
            values.add(getMetadataFieldMapping().toDCValue(getField(), value));
        }
    }
}
