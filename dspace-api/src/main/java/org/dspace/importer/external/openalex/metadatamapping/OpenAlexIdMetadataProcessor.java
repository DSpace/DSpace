/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.openalex.metadatamapping;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.AbstractJsonPathMetadataProcessor;

/**
 * @author adamo.fapohunda at 4science.com
 **/
public class OpenAlexIdMetadataProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(OpenAlexIdMetadataProcessor.class);

    private String path;

    private String toBeReplaced;

    private String replacement;

    @Override
    protected String getStringValue(JsonNode node) {
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Input must be a non-null JsonNode containing a text value");
        }
        String idStr = node.asText();
        if (toBeReplaced == null || toBeReplaced.isEmpty() || replacement == null) {
            return idStr;
        }
        return idStr.replaceAll(toBeReplaced, replacement);
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

    public void setToBeReplaced(String toBeReplaced) {
        this.toBeReplaced = toBeReplaced;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
