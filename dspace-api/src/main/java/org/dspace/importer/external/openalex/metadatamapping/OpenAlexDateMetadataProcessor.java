/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openalex.metadatamapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.AbstractJsonPathMetadataProcessor;

/**
 * A metadata processor that extracts and validates date values from JSON data.
 * This processor retrieves a date string from a specified JSON path and ensures
 * that it is correctly formatted according to the ISO 8601 date standard.
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "published_date": "2023-05-10"
 * }
 * </pre>
 *
 * If the `path` is set to `"/published_date"`, the extracted value will be `"2023-05-10"`.
 *
 * <p>This class extends {@link AbstractJsonPathMetadataProcessor}, which handles JSON parsing and path extraction.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class OpenAlexDateMetadataProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(OpenAlexDateMetadataProcessor.class);

    private String path;


    /**
     * Extracts and validates a date value from a JSON node.
     * The extracted value must conform to the ISO 8601 format (YYYY-MM-DD).
     *
     * @param node The JSON node containing the date as a text value.
     * @return The parsed date as a string in ISO 8601 format.
     * @throws IllegalArgumentException If the node is null, not textual, or does not contain a valid date.
     */
    @Override
    protected String getStringValue(JsonNode node) {

        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Input must be a non-null JsonNode containing a text value");
        }

        try {
            String dateStr = node.asText();
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            return date.toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid ISO 8601 date format: " + e.getMessage(), e);
        }
    }

    /**
     * Provides the logger instance for error reporting.
     *
     * @return The {@link Logger} instance.
     */
    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Retrieves the JSON path expression used to extract the date value.
     *
     * @return The JSONPath expression as a string.
     */
    @Override
    protected String getPath() {
        return path;
    }

    /**
     * Sets the JSON path from which the date value should be extracted.
     *
     * @param path The JSONPath expression specifying the location of the date in the JSON structure.
     */
    public void setPath(String path) {
        this.path = path;
    }
}