/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * .
 * The {@code InvertedIndexProcessor} is a {@link JsonPathMetadataProcessor} implementation that processes JSON metadata
 * by extracting an inverted index of words based on their positions in a given JSON structure.
 * The words are then concatenated into a single string, ordered by
 * their positional values.
 *
 * <p>It extends {@link AbstractJsonPathMetadataProcessor} to utilize JSONPath-based extraction
 * and integrates with DSpace metadata handling.</p>
 *
 *
 * <p>Usage:</p>
 * <pre>
 * {@code
 * <bean id="someProcessor" class="org.dspace.importer.external.openalex.metadatamapping.InvertedIndexProcessor">
 *     <property name="path" value="/some/json/path"/>
 * </bean>
 * }
 * </pre>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class InvertedIndexProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(InvertedIndexProcessor.class);

    private String path;

    /**
     * Extracts and processes metadata from a JSON node.
     * This method retrieves a mapping of words to their respective positions,
     * sorts them based on their position, and returns a concatenated string
     * representing the correctly ordered sequence of words.
     *
     * @param node the JSON node containing metadata
     * @return a space-separated string of words ordered by position, or an empty string if input is null/empty
     */
    @Override
    protected String getStringValue(JsonNode node) {
        if (node == null || node.isEmpty()) {
            return "";
        }

        JsonNode targetNode = node.at(path);

        if (targetNode.isMissingNode() || !targetNode.isObject()) {
            log.warn("The specified path {} is not a JSON array", path);
            return "";
        }

        SortedMap<Integer, String> positionMap = new TreeMap<>();
        targetNode.fields().forEachRemaining(entry -> entry.getValue()
                                                              .forEach(position ->
                                                                           positionMap
                                                                               .put(position.asInt(), entry.getKey())));

        return String.join(" ", positionMap.values());
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected String getPath() {
        return "";
    }

    public void setPath(String path) {
        this.path = path;
    }
}