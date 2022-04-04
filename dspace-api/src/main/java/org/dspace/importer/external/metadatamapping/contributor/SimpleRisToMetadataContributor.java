/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * Metadata contributor that takes a record defined as Map<String,List<String>>
 * and turns it into metadatums configured in fieldToMetadata
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SimpleRisToMetadataContributor implements MetadataContributor<Map<String,List<String>>> {

    protected Map<String, MetadataFieldConfig> fieldToMetadata;

    protected MetadataFieldMapping<Map<String,List<String>>,
              MetadataContributor<Map<String,List<String>>>> metadataFieldMapping;

    public SimpleRisToMetadataContributor() {}

    public SimpleRisToMetadataContributor(Map<String, MetadataFieldConfig> fieldToMetadata) {
        this.fieldToMetadata = fieldToMetadata;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Map<String, List<String>> record) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String field : fieldToMetadata.keySet()) {
            List<String> fieldValues = record.get(field);
            if (Objects.nonNull(fieldValues)) {
                for (String value : fieldValues) {
                    values.add(metadataFieldMapping.toDCValue(fieldToMetadata.get(field), value));
                }
            }
        }
        return values;
    }

    public Map<String, MetadataFieldConfig> getFieldToMetadata() {
        return fieldToMetadata;
    }

    public void setFieldToMetadata(Map<String, MetadataFieldConfig> fieldToMetadata) {
        this.fieldToMetadata = fieldToMetadata;
    }

    public MetadataFieldMapping<Map<String, List<String>>,
           MetadataContributor<Map<String, List<String>>>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    public void setMetadataFieldMapping(MetadataFieldMapping<Map<String, List<String>>,
                                        MetadataContributor<Map<String, List<String>>>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

}