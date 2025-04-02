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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * A metadata contributor that applies a given {@link MetadataContributor} to each object within
 * a JSON array at a specified path. This enables extracting metadata from multiple objects
 * within a JSON structure.
 *
 * <p>This class is useful for processing JSON arrays where each element represents an entity
 * that requires metadata extraction.</p>
 *
 * <p>Example JSON structure:</p>
 * <pre>
 * {
 *   "authors": [
 *     { "name": "John Doe", "id": "123" },
 *     { "name": "Jane Smith", "id": "456" }
 *   ]
 * }
 * </pre>
 *
 * <p>If the {@code pathToArray} is set to {@code "/authors"}, the {@code contributor} will
 * be applied to each author object in the array.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ArrayJsonPathMetadataContributor implements MetadataContributor<String> {

    private final static Logger log = LogManager.getLogger();

    private MetadataContributor<String> contributor;
    private String pathToArray;


    /**
     * Unused by this implementation. This method is required by the {@link MetadataContributor} interface.
     *
     * @param rt The metadata field mapping (not used in this implementation).
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<String, MetadataContributor<String>> rt) {

    }

    /**
     * Extracts metadata from each object in a JSON array located at {@code pathToArray}.
     * The configured {@code contributor} is applied to each element of the array.
     *
     * @param fullJson The JSON string containing the array.
     * @return A collection of {@link MetadatumDTO} objects extracted from the array elements.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {
        Collection<MetadatumDTO> values = new ArrayList<>();
        JsonNode jsonNode = convertStringJsonToJsonNode(fullJson);
        JsonNode node = jsonNode.at(pathToArray);
        if (node.isArray()) {
            for (JsonNode value : node) {
                values.addAll(contributor.contributeMetadata(value.toString()));
            }
        }
        return values;
    }

    /**
     * Converts a JSON string into a {@link JsonNode} object.
     *
     * @param json The JSON string to be parsed.
     * @return A {@link JsonNode} representation of the JSON string, or {@code null} if parsing fails.
     */
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

    /**
     * Sets the metadata contributor that will be applied to each element of the JSON array.
     *
     * @param contributor The {@link MetadataContributor} responsible for processing each array element.
     */
    public void setContributor(
        MetadataContributor<String> contributor) {
        this.contributor = contributor;
    }

    /**
     * Sets the JSONPath to the array in the JSON structure.
     *
     * @param pathToArray A JSONPath expression indicating the location of the array.
     */
    public void setPathToArray(String pathToArray) {
        this.pathToArray = pathToArray;
    }
}
