/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.CopyOperation;
import org.dspace.app.rest.model.patch.FromOperation;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.springframework.data.rest.webmvc.json.patch.PatchException;

/**
 * Convert {@link JsonNode}s containing JSON Patch to/from {@link Patch} objects.
 *
 * Based on {@link org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class JsonPatchConverter implements PatchConverter<JsonNode> {

    private final @Nonnull ObjectMapper mapper;

    public JsonPatchConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Constructs a {@link Patch} object given a JsonNode.
     *
     * @param jsonNode a JsonNode containing the JSON Patch
     * @return a {@link Patch}
     */
    public Patch convert(JsonNode jsonNode) {

        if (!(jsonNode instanceof ArrayNode)) {
            throw new IllegalArgumentException("JsonNode must be an instance of ArrayNode");
        }

        ArrayNode opNodes = (ArrayNode) jsonNode;
        List<Operation> ops = new ArrayList<Operation>(opNodes.size());

        for (Iterator<JsonNode> elements = opNodes.elements(); elements.hasNext(); ) {

            JsonNode opNode = elements.next();

            String opType = opNode.get("op").textValue();
            String path = opNode.get("path").textValue();

            JsonNode valueNode = opNode.get("value");
            Object value = valueFromJsonNode(path, valueNode);
            String from = opNode.has("from") ? opNode.get("from").textValue() : null;

            //IDEA maybe if the operation have a universal name the PatchOperation can be retrieve here not in
            // WorkspaceItemRestRepository.evaluatePatch
            if (opType.equals("replace")) {
                ops.add(new ReplaceOperation(path, value));
            } else if (opType.equals("remove")) {
                ops.add(new RemoveOperation(path));
            } else if (opType.equals("add")) {
                ops.add(new AddOperation(path, value));
            } else if (opType.equals("copy")) {
                ops.add(new CopyOperation(path, from));
            } else if (opType.equals("move")) {
                ops.add(new MoveOperation(path, from));
            } else {
                throw new PatchException("Unrecognized operation type: " + opType);
            }
        }

        return new Patch(ops);
    }

    /**
     * Renders a {@link Patch} as a {@link JsonNode}.
     *
     * @param patch the patch
     * @return a {@link JsonNode} containing JSON Patch.
     */
    public JsonNode convert(Patch patch) {

        List<Operation> operations = patch.getOperations();
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ArrayNode patchNode = nodeFactory.arrayNode();

        for (Operation operation : operations) {

            ObjectNode opNode = nodeFactory.objectNode();
            opNode.set("op", nodeFactory.textNode(operation.getOp()));
            opNode.set("path", nodeFactory.textNode(operation.getPath()));

            if (operation instanceof FromOperation) {

                FromOperation fromOp = (FromOperation) operation;
                opNode.set("from", nodeFactory.textNode(fromOp.getFrom()));
            }

            Object value = operation.getValue();

            if (value != null) {
                opNode.set("value", value instanceof JsonValueEvaluator ? ((JsonValueEvaluator) value).getValueNode()
                        : mapper.valueToTree(value));
            }

            patchNode.add(opNode);
        }

        return patchNode;
    }

    private Object valueFromJsonNode(String path, JsonNode valueNode) {

        if (valueNode == null || valueNode.isNull()) {
            return null;
        } else if (valueNode.isTextual()) {
            return valueNode.asText();
        } else if (valueNode.isFloatingPointNumber()) {
            return valueNode.asDouble();
        } else if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else if (valueNode.isInt()) {
            return valueNode.asInt();
        } else if (valueNode.isLong()) {
            return valueNode.asLong();
        } else if (valueNode.isObject() || (valueNode.isArray())) {
            return new JsonValueEvaluator(mapper, valueNode);
        }

        throw new PatchException(
            String.format("Unrecognized valueNode type at path %s and value node %s.", path, valueNode));
    }


}
