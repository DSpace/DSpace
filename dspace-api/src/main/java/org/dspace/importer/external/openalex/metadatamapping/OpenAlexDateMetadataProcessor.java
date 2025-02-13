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
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexDateMetadataProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(OpenAlexDateMetadataProcessor.class);

    private String path;

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

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}