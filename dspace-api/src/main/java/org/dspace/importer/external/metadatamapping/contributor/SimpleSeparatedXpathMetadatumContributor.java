/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.*;
import java.util.regex.*;
import org.apache.axiom.om.*;
import org.dspace.importer.external.metadatamapping.*;
import org.springframework.beans.factory.annotation.*;

/**
 * Created by jonas - jonas@atmire.com on 20/05/16.
 */
public class SimpleSeparatedXpathMetadatumContributor extends SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {

    private String separator;

    public String getSeparator() {
        return separator;
    }

    @Required
    public void setSeparator(String separator) {
        this.separator = Pattern.quote(separator);
    }


    @Override
    protected void addRetrievedValueToMetadata(List<MetadatumDTO> values, String value) {
        String[] separatedValues= value.split(separator);
        for(String valuePart: separatedValues){
            values.add(getMetadataFieldMapping().toDCValue(getField(), valuePart));
        }
    }
}
