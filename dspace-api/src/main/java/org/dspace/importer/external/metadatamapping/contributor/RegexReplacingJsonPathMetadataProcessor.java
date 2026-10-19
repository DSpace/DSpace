/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A generic JSON metadata processor that extracts a string value from a JSON node
 * using a specified JSONPath and applies a regex-based replacement.
 *
 * This class extends {@link AbstractJsonPathMetadataProcessor} and provides functionality
 * to replace a matching regex pattern in the extracted text with a given replacement value.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @see AbstractJsonPathMetadataProcessor
 * @see JsonPathMetadataProcessor
 */
public class RegexReplacingJsonPathMetadataProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(RegexReplacingJsonPathMetadataProcessor.class);

    private String path;
    private String regexPattern;
    private String replacement;


    /**
     * Extracts the string value from the given JSON node and applies regex-based replacement.
     *
     * @param node The JSON node from which to extract the value.
     * @return The transformed string after applying the regex replacement.
     * @throws IllegalArgumentException if the node is null or does not contain a text value.
     */
    @Override
    protected String getStringValue(JsonNode node) {
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Input must be a non-null JsonNode containing a text value");
        }
        String idStr = node.asText();
        if (regexPattern == null || regexPattern.isEmpty() || replacement == null) {
            return idStr;
        }
        return idStr.replaceAll(regexPattern, replacement);
    }

    /**
     * Provides the logger instance for error handling.
     *
     * @return A {@link Logger} instance.
     */
    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Returns the JSONPath expression used to locate the value in the JSON structure.
     *
     * @return The JSONPath string.
     */
    @Override
    protected String getPath() {
        return path;
    }

    /**
     * Sets the JSONPath expression used for extraction.
     *
     * @param path The JSONPath string.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the regex pattern to be replaced in the extracted string.
     *
     * @param regexPattern The regular expression pattern.
     */
    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    /**
     * Sets the replacement string that will replace occurrences of the regex pattern.
     *
     * @param replacement The replacement string.
     */
    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
