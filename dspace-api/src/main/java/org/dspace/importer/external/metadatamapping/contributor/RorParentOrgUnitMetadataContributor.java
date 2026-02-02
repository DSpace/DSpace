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

/**
 * A ROR JsonPath Metadata processor that should be configured inside the {@code ror-integration.xml} file.
 * This allows the extraction of a given contributor with a specific mappings from the ROR JSON response.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class RorParentOrgUnitMetadataContributor extends SimpleJsonPathMetadataContributor {

    /**
     * Determines which field of the JSON detains the {@code type} of this
     * specific node (that needs to be mapped).
     *
     */
    private String typeField;

    /**
     * Determines which is the type of the main parent node that needs to be mapped.
     * It should match the value of the {@code typeField} of the JSON node.
     *
     */
    private String parentType;

    /**
     * Determines which is the field of the JSON that contains the value
     * that needs to be mapped into a {@code MetadatumDTO}.
     */
    private String labelField;

    /**
     * Creates a {@code MetadatumDTO} for each correctly mapped JSON node
     * of the ROR response.
     * Partial / Unmatched parent-type metadatum will be ignored from this mapping.
     * 
     * @param fullJson ROR response
     * @return a collection of read ROR metadata.
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