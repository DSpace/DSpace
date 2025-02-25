/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openalex.metadatamapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.SimpleJsonPathMetadataContributor;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexAuthorNameContributor extends SimpleJsonPathMetadataContributor {

    private final static Logger log = LogManager.getLogger();

    private String query;
    private MetadataFieldConfig field;

    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {
        Collection<MetadatumDTO> metadata = new ArrayList<>();

        if (field == null || field.getElement() == null) {
            return metadata;
        }

        JsonNode jsonNode = convertStringJsonToJsonNode(fullJson);
        JsonNode node = jsonNode.at(query);

        if (node.isArray() || node.isNull() || StringUtils.isBlank(node.asText())) {
            return metadata;
        }

        String fullName = getStringValue(node).trim();
        String[] nameParts = fullName.split(" ");

        if (nameParts.length < 1) {
            return metadata;
        }

        String firstName = nameParts.length > 1 ?
            String.join(" ", Arrays.copyOfRange(nameParts, 0, nameParts.length - 1)) : "";
        String lastName = nameParts[nameParts.length - 1];

        // Check field configuration and map accordingly
        if ("firstName".equals(field.getElement()) && StringUtils.isNotBlank(firstName)) {
            metadata.add(createMetadatum(field, firstName));
        } else if ("familyName".equals(field.getElement()) && StringUtils.isNotBlank(lastName)) {
            metadata.add(createMetadatum(field, lastName));
        }

        return metadata;
    }

    private MetadatumDTO createMetadatum(MetadataFieldConfig field, String value) {
        MetadatumDTO metadatum = new MetadatumDTO();
        metadatum.setValue(value);
        metadatum.setElement(field.getElement());
        metadatum.setQualifier(field.getQualifier());
        metadatum.setSchema(field.getSchema());
        return metadatum;
    }

    private String getStringValue(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber()) {
            return node.numberValue().toString();
        }
        log.error("It wasn't possible to convert the value of the following JsonNode:" + node.asText());
        return StringUtils.EMPTY;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return body;
    }


}



