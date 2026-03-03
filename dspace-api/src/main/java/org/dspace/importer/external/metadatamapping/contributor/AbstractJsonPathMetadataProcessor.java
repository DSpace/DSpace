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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;


/**
 * An abstract implementation of {@link JsonPathMetadataProcessor} that processes JSON data
 * using a JSONPath expression. This class provides a base structure for extracting values
 * from a JSON object while allowing subclasses to define specific behaviors.
 *
 * The extraction process:
 * <ol>
 *     <li>Converts a JSON string into a {@link JsonNode} object.</li>
 *     <li>Retrieves a sub-node based on the JSONPath expression returned by {@link #getPath()}.</li>
 *     <li>Extracts values from the node and processes them using {@link #getStringValue(JsonNode)}.</li>
 *     <li>Returns a collection of extracted values.</li>
 * </ol>
 *
 * Subclasses must implement:
 * <ul>
 *     <li>{@link #getStringValue(JsonNode)} - Defines how values are extracted from a JSON node.</li>
 *     <li>{@link #getLogger()} - Provides a logger instance for error handling.</li>
 *     <li>{@link #getPath()} - Specifies the JSONPath used to extract data.</li>
 * </ul>
 *
 *
 * @see JsonPathMetadataProcessor
 * @see JsonNode
 * @see ObjectMapper
 * @see Logger
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public abstract class AbstractJsonPathMetadataProcessor implements JsonPathMetadataProcessor {

    /**
     * Extracts metadata from a JSON string using a predefined JSONPath.
     * The extracted values are processed and returned as a collection of strings.
     *
     * @param json The JSON string to process.
     * @return A collection of extracted string values.
     */
    @Override
    public Collection<String> processMetadata(String json) {
        Collection<String> values = new ArrayList<>();
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        JsonNode node = jsonNode.at(getPath());
        if (node.isArray()) {
            for (JsonNode value : node) {
                String nodeValue = getStringValue(value);
                if (StringUtils.isNotBlank(nodeValue)) {
                    values.add(nodeValue);
                }
            }
        } else if (!node.isNull() && StringUtils.isNotBlank(node.toString())) {
            String nodeValue = getStringValue(node);
            if (StringUtils.isNotBlank(nodeValue)) {
                values.add(nodeValue);
            }
        }
        return values;
    }

    /**
     * Extracts a string representation of the value from a {@link JsonNode}.
     * The implementation of this method must be provided by subclasses.
     *
     * @param node The JSON node from which to extract the value.
     * @return A string representation of the value.
     */
    protected abstract String getStringValue(JsonNode node);

    /**
     * Provides the logger for logging errors and messages.
     * The implementation of this method must be provided by subclasses.
     *
     * @return A {@link Logger} instance.
     */
    protected abstract Logger getLogger();

    /**
     * Returns the JSONPath expression used for extracting values from JSON.
     * The implementation of this method must be provided by subclasses.
     *
     * @return A string representing the JSONPath expression.
     */
    protected abstract String getPath();

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
            getLogger().error("Unable to process json response.", e);
        }
        return body;
    }
}
