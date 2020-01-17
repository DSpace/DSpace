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
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchUtils {

    @Autowired
    private BundleService bundleService;

    /**
     * Validates a move operation by assuring bitream exist and the parameters
     * supplied by the move operations are not out of bounds.
     * @param context
     * @param from
     * @param to
     * @param uuid
     * @throws SQLException
     */
    public void validateMoveOperation(Context context, int from, int to, UUID uuid) throws SQLException {
        Bundle bundle = bundleService.find(context, uuid);
        final int totalAmount = bundle.getBitstreams().size();
        if (totalAmount < 1) {
            throw new DSpaceBadRequestException(
                createMoveExceptionMessage(bundle, from, to, "No bitstreams found.")
            );
        }
        if (from >= totalAmount) {
            throw new DSpaceBadRequestException(
                createMoveExceptionMessage(bundle, from, to,
                    "\"from\" location out of bounds. Latest available position: " + (totalAmount - 1))
            );
        }
        if (to >= totalAmount) {
            throw new DSpaceBadRequestException(
                createMoveExceptionMessage(bundle, from, to,
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
     * Modifies move operation paths by removing "_link" and "/href" from
     * the path. Required when reordering bundle bitstreams (and perhaps in other
     * situations). If a non-move operation is supplied the node is
     * unchanged.
     * @param jsonNode original json node
     * @return json node with modified paths
     */
    public JsonNode modifyMoveOperation(JsonNode jsonNode) {
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
     * Modifies JsonNode patch operation. Replaces the value for the
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
     * @param bundle    The bundle we're performing a move operation on
     * @param from      The "from" location
     * @param to        The "to" location
     * @param message   A message to add after the prefix
     * @return The created message
     */
    private String createMoveExceptionMessage(Bundle bundle, int from, int to, String message) {
        return "Failed moving bitstreams of bundle with id " +
            bundle.getID() + " from location " + from + " to " + to + ": " + message;
    }

}
