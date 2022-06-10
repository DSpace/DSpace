/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * This class is used for CrossRef's Live-Import to extract
 * attributes such as "given" and "family" from the array of authors/editors
 * and return them concatenated.
 * Beans are configured in the crossref-integration.xml file.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class CrossRefAuthorMetadataProcessor implements JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger();

    private String pathToArray;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        Iterator<JsonNode> authors = rootNode.at(pathToArray).iterator();
        Collection<String> values = new ArrayList<>();
        while (authors.hasNext()) {
            JsonNode author = authors.next();
            String givenName = author.at("/given").textValue();
            String familyName = author.at("/family").textValue();
            if (StringUtils.isNoneBlank(givenName) && StringUtils.isNoneBlank(familyName)) {
                values.add(givenName + " " + familyName);
            }
        }
        return values;
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

    public void setPathToArray(String pathToArray) {
        this.pathToArray = pathToArray;
    }

}