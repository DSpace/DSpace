/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

public class RorParentOrgUnitMetadataContributor extends SimpleJsonPathMetadataContributor {

    private String typeField;

    private String parentType;

    private String labelField;

    /**
     * Retrieve the metadata associated with the given object.
     * The toString() of the resulting object will be used.
     * 
     * @param fullJson A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {

        Collection<MetadatumDTO> metadata = new ArrayList<>();
        Collection<String> metadataValue = new ArrayList<>();

        JsonNode jsonNode = convertStringJsonToJsonNode(fullJson);
        JsonNode array = jsonNode.at(getQuery());
        if (!array.isArray()) {
            return metadata;
        }

        Iterator<JsonNode> nodes = array.iterator();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();

            if (!node.has(labelField)) {
                continue;
            }

            String type = node.has(typeField) ? node.get(typeField).asText() : null;
            String label = node.get(labelField).asText();

            if (parentType.equalsIgnoreCase(type)) {
                metadataValue.add(label);
            }

        }

        for (String value : metadataValue) {
            MetadatumDTO metadatumDto = new MetadatumDTO();
            metadatumDto.setValue(value);
            metadatumDto.setElement(getField().getElement());
            metadatumDto.setQualifier(getField().getQualifier());
            metadatumDto.setSchema(getField().getSchema());
            metadata.add(metadatumDto);
        }
        return metadata;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return body;
    }

    public String getTypeField() {
        return typeField;
    }

    public void setTypeField(String typeField) {
        this.typeField = typeField;
    }

    public String getLabelField() {
        return labelField;
    }

    public void setLabelField(String labelField) {
        this.labelField = labelField;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

}