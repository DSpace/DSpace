/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonUtils {
    private ObjectMapper objectMapper = new ObjectMapper();

    public <T> T applyPatch(JsonNode patch, T inputObject, Class<T> outputClass) {
        try {
            JsonNode objectJson = objectMapper.valueToTree(inputObject);
            JsonPatch.applyInPlace(patch, objectJson);
            return objectMapper.treeToValue(objectJson, outputClass);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
