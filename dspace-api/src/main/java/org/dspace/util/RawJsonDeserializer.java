/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Extension of {@link ValueDeserializer} that convert a json to a String.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class RawJsonDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt)
           throws JacksonException {
        JsonNode node = jp.readValueAsTree();
        return node.toString();
    }
}