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
 * This class is responsible for extracting and contributing author name metadata
 * from a JSON response using a specified query path.
 * <p>
 * The extracted name is split into given name and family name, and the metadata
 * is stored based on the field configuration.
 * </p>
 *
 * <p>
 * Example JSON structure:
 * </p>
 * <pre>
 * {
 *   "author": {
 *     "full_name": "John Doe"
 *   }
 * }
 * </pre>
 *
 * <p>
 * If the query path points to `author.full_name`, this class will extract
 * "John" as the given name and "Doe" as the family name.
 * </p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class OpenAlexAuthorNameContributor extends SimpleJsonPathMetadataContributor {

    private final static Logger log = LogManager.getLogger();

    private String query;
    private MetadataFieldConfig field;

    /**
     * Sets the JSON query path for extracting the author name.
     *
     * @param query the JSON path to the author name
     */
    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Sets the metadata field configuration that determines where
     * the extracted metadata should be stored.
     *
     * @param field the metadata field configuration
     */
    @Override
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Extracts and contributes metadata based on the configured JSON query path and field.
     * <p>
     * If the extracted author name contains both given and family names, it assigns
     * them accordingly based on the configured field.
     * </p>
     *
     * @param fullJson the JSON response containing author information
     * @return a collection of metadata entries representing the extracted name parts
     */
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
        if ("givenName".equals(field.getElement()) && StringUtils.isNotBlank(firstName)) {
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



