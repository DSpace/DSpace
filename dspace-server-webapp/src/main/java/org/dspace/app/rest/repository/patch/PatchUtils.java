package org.dspace.app.rest.repository.patch;

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchUtils {

    private static final String DISCOVERABLE_PATH = "/discoverable";
    private static final String WITHDRAWN_PATH = "/withdrawn";
    private static final String IN_ARCHIVE_PATH = "/inArchive";

    @Autowired
    private BundleService bundleService;

    /**
     * Validates a bundle move operation by assuring bitstream exist and that the parameters
     * supplied by the move operations are not out of bounds.
     * @param context dspace context
     * @param patch patch operation
     * @param uuid id of the target bundle
     * @throws SQLException
     */
    public void validateBundleMoveOperation(Context context, JsonNode patch, UUID uuid) throws SQLException {
        Bundle bundle = bundleService.find(context, uuid);
        final int totalAmount = bundle.getBitstreams().size();
        for (JsonNode operation : patch) {
            if (operation.get("op").asText().contentEquals("move")) {
                final int from = Integer.parseInt(getIndexFromPath(operation.get("from").asText()));
                final int to = Integer.parseInt(getIndexFromPath(operation.get("path").asText()));
                checkBoundaries(uuid, totalAmount, from, to);
            }
        }
    }

    /**
     * Method for checking the boundaries of a move operation against the target object.
     * @param uuid id of the target
     * @param totalAmount the count of resources in the target
     * @param from current location of the resource
     * @param to the location to which the resource will be moved
     */
    private void checkBoundaries(UUID uuid, int totalAmount, int from, int to) {
        if (totalAmount < 1) {
            throw new DSpaceBadRequestException(
                createMoveExceptionMessage(uuid, from, to, "Cannot find resources to move.")
            );
        }
        if (from >= totalAmount) {
            throw new DSpaceBadRequestException(
                createMoveExceptionMessage(uuid, from, to,
                    "\"from\" location out of bounds. Latest available position: " + (totalAmount - 1))
            );
        }
        if (to >= totalAmount) {
            throw new DSpaceBadRequestException(
                createMoveExceptionMessage(uuid, from, to,
                    "\"to\" location out of bounds. Latest available position: " + (totalAmount - 1))
            );
        }
    }

    /**
     * Extracts the move position from the original (unmodified) path.
     * @param path path received in the original request
     * @return requested position
     */
    public String getIndexFromPath(String path) {
        String[] partsOfPath = path.split("/");
        // Index of md being patched
        return (partsOfPath.length > 3) ? partsOfPath[3] : null;
    }

    /**
     * Checks for illegal patch operations in the item template. (Item templates cannot be discoverable).
     * @param item the DSpace object
     * @param jsonNode the patch
     */
    public void validateItemTemplatePatch(Item item, JsonNode jsonNode) {
        for (JsonNode operation : jsonNode) {
            String path = operation.get("path").asText();
            if (path.equals(DISCOVERABLE_PATH)) {
                if (operation.get("value").asBoolean()) {
                    if (item.getTemplateItemOf() != null) {
                        throw new UnprocessableEntityException("A template item cannot be discoverable.");
                    }
                }
            }
            if (path.equals(WITHDRAWN_PATH)) {
                if (operation.get("value").asBoolean()) {
                    if (item.getTemplateItemOf() != null) {
                        throw new UnprocessableEntityException("A template item cannot be withdrawn.");
                    }
                }
            }
            if (path.equals(IN_ARCHIVE_PATH)) {
                if (operation.get("value").asBoolean()) {
                    throw new DSpaceBadRequestException("A template item does not support this operation.");
                }
            }
        }
    }

    /**
     * Modifies move operation paths by removing "_link" and "/href" from
     * the path. Required when reordering bundle bitstreams (and perhaps in other
     * situations). If a non-move operation is supplied the node is
     * unchanged.
     * @param jsonNode original json node
     * @return json node with modified paths
     */
    public JsonNode modifyMoveOperations(JsonNode jsonNode) {
        for (JsonNode operation : jsonNode) {
            if (operation.get("op").asText().contentEquals("move")) {
                String path = replaceLinkPath(operation.get("path").asText());
                path = replaceHrefPath(path);
                modifyPatchOperation(operation, "path", path);
                if (operation.get("from") != null) {
                    String from = replaceLinkPath(operation.get("from").asText());
                    from = replaceHrefPath(from);
                    modifyPatchOperation(operation, "from", from);
                }
            }
        }
        return jsonNode;
    }

    /**
     * Modifies JsonNode patch operation by replacing the value for the
     * specified field.
     * @param node patch operation
     * @param fieldName field to replace
     * @param value the new value
     */
    private void modifyPatchOperation(JsonNode node, String fieldName, String value) {
        JsonNode newPathNode = new TextNode(value);
        ObjectNode nodeObj = (ObjectNode) node;
        nodeObj.remove(fieldName);
        nodeObj.set(fieldName, newPathNode);
    }

    /**
     * Removes "/_link" from beginning of path
     * @param path original path
     * @return modifed path
     */
    private String replaceLinkPath(String path) {
        Pattern p = Pattern.compile("^/_links", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(path);
        return m.replaceFirst("");
    }

    /**
     * Removes "/href" from end of path
     * @param path original path
     * @return modifed path
     */
    private String replaceHrefPath(String path) {
        Pattern p = Pattern.compile("/href$", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(path);
        return m.replaceFirst("");
    }

    /**
     * Create an exception message for the move operation
     *
     * @param uuid    The uuid of the resource
     * @param from      The "from" location
     * @param to        The "to" location
     * @param message   A message to add after the prefix
     * @return The created message
     */
    private String createMoveExceptionMessage(UUID uuid, int from, int to, String message) {
        return "Failed moving elements for object with id " +
            uuid + " from location " + from + " to " + to + ": " + message;
    }

}
