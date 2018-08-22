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
 * Metadata contributor that removes the startstring from the beginning of a value
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class SimpleStartSubstringXpathMetadatumContributor extends SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {

    private String startString;

    public String getStartString() {
        return startString;
    }

    public void setStartString(String startString) {
        this.startString = startString;
    }

    @Override
    protected void addRetrievedValueToMetadata(List<MetadatumDTO> values, String value) {
        if(StringUtils.startsWith(value, startString)){
            values.add(getMetadataFieldMapping().toDCValue(getField(), StringUtils.substring(value,startString.length())));
        }
        else {
            values.add(getMetadataFieldMapping().toDCValue(getField(), value));
        }
    }
}
