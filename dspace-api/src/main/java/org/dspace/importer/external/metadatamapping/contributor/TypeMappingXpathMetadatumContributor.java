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
import org.dspace.importer.external.metadatamapping.*;

/**
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class TypeMappingXpathMetadatumContributor extends SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {

    private Map<String, String> typeMapping;
    private String defaultValue;

    @Override
    protected void addRetrievedValueToMetadata(List<MetadatumDTO> values, String value) {
        String matchedValue = defaultValue;

        for (Map.Entry<String, String> entry : typeMapping.entrySet()) {
            if (value.toLowerCase().matches("^.*" + entry.getKey().toLowerCase().replace("*", ".*") + ".*$")) {
                matchedValue = entry.getValue();
                break;
            }
        }

        values.add(getMetadataFieldMapping().toDCValue(getField(), matchedValue));
    }

    public Map<String, String> getTypeMapping() {
        return typeMapping;
    }

    public void setTypeMapping(Map<String, String> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
