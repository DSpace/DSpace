/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.patch;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.rest.webmvc.json.patch.PatchException;

/**
 * {@link LateObjectEvaluator} implementation that assumes values represented as JSON objects.
 *
 * Based on {@link org.springframework.data.rest.webmvc.json.patch.JsonLateObjectEvaluator}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class JsonValueEvaluator implements LateObjectEvaluator {

    private final @Nonnull ObjectMapper mapper;
    private final @Nonnull JsonNode valueNode;

    public JsonValueEvaluator(ObjectMapper mapper, JsonNode valueNode) {
        this.mapper = mapper;
        this.valueNode = valueNode;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator#evaluate(java.lang.Class)
     */
    @Override
    public <T> Object evaluate(Class<T> type) {

        try {
            return mapper.readValue(valueNode.traverse(), type);
        } catch (Exception e) {
            throw new PatchException(String.format("Could not read %s into %s!", valueNode, type), e);
        }
    }

    public JsonNode getValueNode() {
        return this.valueNode;
    }
}
