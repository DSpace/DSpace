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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;


/**
 * @author adamo.fapohunda at 4science.com
 **/
public abstract class AbstractJsonPathMetadataProcessor implements JsonPathMetadataProcessor {

    @Override
    public Collection<String> processMetadata(String json) {
        Collection<String> values = new ArrayList<>();
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        JsonNode node = jsonNode.at(getPath());
        if (node.isArray()) {
            for (JsonNode value : node) {
                String nodeValue = getStringValue(value);
                if (StringUtils.isNotBlank(nodeValue)) {
                    values.add(nodeValue);
                }
            }
        } else if (!node.isNull() && StringUtils.isNotBlank(node.toString())) {
            String nodeValue = getStringValue(node);
            if (StringUtils.isNotBlank(nodeValue)) {
                values.add(nodeValue);
            }
        }
        return values;
    }

    protected abstract String getStringValue(JsonNode node);

    protected abstract Logger getLogger();

    protected abstract String getPath();

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            getLogger().error("Unable to process json response.", e);
        }
        return body;
    }
}
