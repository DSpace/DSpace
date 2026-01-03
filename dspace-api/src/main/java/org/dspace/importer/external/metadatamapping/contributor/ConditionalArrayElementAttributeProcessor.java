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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;

/**
 * This Processor extracts values from a JSON array, but only when a condition
 * on another attribute is met. For example, to extract all values of
 * /names/value where /names/types contains "ror_display".
 *
 * Configurable via:
 *   pathToArray: e.g., /names
 *   elementAttribute: e.g., /value
 *   filterAttribute: e.g., /types
 *   requiredValueInFilter: e.g., ror_display
 *
 * Supports filtering when the filter attribute is either a JSON array or a single string.
 *
 * Example JSON:
 * {
 *   "items": [{
 *     "names": [
 *       { "types": ["label", "ror_display"], "value": "Instituto Federal do Piauí" },
 *       { "types": ["acronym"], "value": "IFPI" }
 *     ]
 *   }]
 * }
 * This processor can extract "Instituto Federal do Piauí" using proper configuration.
 *
 * Author: Jesiel (based on Mykhaylo Boychuk’s original processor)
 */
public class ConditionalArrayElementAttributeProcessor implements JsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger();

    private String pathToArray;
    private String elementAttribute;
    private String filterAttribute;
    private String requiredValueInFilter;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        Collection<String> results = new ArrayList<>();

        if (rootNode == null) {
            return results;
        }

        Iterator<JsonNode> array = rootNode.at(pathToArray).iterator();
        while (array.hasNext()) {
            JsonNode element = array.next();
            JsonNode filterNode = element.at(filterAttribute);

            boolean match = false;

            if (filterNode.isArray()) {
                for (JsonNode filterValue : filterNode) {
                    if (requiredValueInFilter.equalsIgnoreCase(filterValue.asString())) {
                        match = true;
                        break;
                    }
                }
            } else if (filterNode.isString()) {
                if (requiredValueInFilter.equalsIgnoreCase(filterNode.asString())) {
                    match = true;
                }
            }

            if (match) {
                JsonNode valueNode = element.at(elementAttribute);
                if (valueNode.isString()) {
                    results.add(valueNode.asString());
                } else if (valueNode.isArray()) {
                    for (JsonNode item : valueNode) {
                        if (item.isString() && StringUtils.isNotBlank(item.asString())) {
                            results.add(item.asString());
                        }
                    }
                }
            }
        }

        return results;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(json);
        } catch (JacksonException e) {
            log.error("Unable to process JSON response.", e);
            return null;
        }
    }

    public void setPathToArray(String pathToArray) {
        this.pathToArray = pathToArray;
    }

    public void setElementAttribute(String elementAttribute) {
        this.elementAttribute = elementAttribute;
    }

    public void setFilterAttribute(String filterAttribute) {
        this.filterAttribute = filterAttribute;
    }

    public void setRequiredValueInFilter(String requiredValueInFilter) {
        this.requiredValueInFilter = requiredValueInFilter;
    }
}