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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This Processor allows to extract attribute values of an array.
 * For exaple to extract all values of secondAttribute,
 * "array":[
 *       {
 *        "firstAttribute":"first value",
 *        "secondAttribute":"second value"
 *       },
 *       {
 *        "firstAttribute":"first value",
 *        "secondAttribute":"second value"
 *        }
 * ]
 * 
 * it's possible configure a bean with
 * pathToArray=/array and elementAttribute=/secondAttribute
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ArrayElementAttributeProcessor implements JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger();

    private String pathToArray;

    private String elementAttribute;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        Iterator<JsonNode> array = rootNode.at(pathToArray).iterator();
        Collection<String> values = new ArrayList<>();
        while (array.hasNext()) {
            JsonNode element = array.next();
            String value = element.at(elementAttribute).textValue();
            if (StringUtils.isNoneBlank(value)) {
                values.add(value);
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

    public void setElementAttribute(String elementAttribute) {
        this.elementAttribute = elementAttribute;
    }

}