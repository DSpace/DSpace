/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class RegexExtractMetadataProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(RegexExtractMetadataProcessor.class);

    private String path;
    private String regexPattern;

    @Override
    protected String getStringValue(JsonNode node) {
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Input must be a non-null JsonNode containing a text value");
        }

        String text = node.asText();
        if (regexPattern == null || regexPattern.isEmpty()) {
            return text;
        }

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group();
        }

        return text;
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

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }
}

