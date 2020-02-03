/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonPatch;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.MetadataRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter;
import org.springframework.data.rest.webmvc.json.patch.PatchException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.stereotype.Component;

/**
 * Base class for DSpaceObject-based PATCH operations.
 *
 * @param <R> the type of DSpaceObjectRest object the class is applicable to.
 */
@Component
public class ResourcePatch<R extends DSpaceObjectRest> {

    private static final String METADATA_PATH = "/metadata";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PatchUtils patchUtils;

    /**
     * This experimental method applies patches using Spring rather
     * than local patch implementations.  The metadata patches are handled separately
     * since these cannot be patched via Spring.  See discussion in:
     * https://github.com/DSpace/DSpace/pull/2591
     *
     * @param dsoRest the instance to apply the changes to.
     * @param jsonNode the <code>JsonNode</code> for patch operation.
     * @return the modified <code>DSpaceObjectRest</code> instance.
     */
    public R patch(R dsoRest, JsonNode jsonNode,  Class<R> domainClass) {
        // When reordering bitstreams in bundles the patch operation paths need to
        // be modified to align with the rest model.
        if (dsoRest instanceof BundleRest) {
            patchUtils.modifyMoveOperations(jsonNode);
        }
        // Add metadata operations to ArrayNode and track indicies to be removed later.
        List<JsonNode> metadataList = new ArrayList<>();
        ArrayNode metadataOperationsNode = objectMapper.valueToTree(metadataList);
        List<Integer> toRemove = new ArrayList<>();
        Integer index = 0;
        for (JsonNode operation : jsonNode) {
            String path = operation.get("path").asText();
            if (path.equals(METADATA_PATH) || path.startsWith(METADATA_PATH + "/")) {
                metadataOperationsNode.add(operation);
                toRemove.add(index);
            }
            index++;
        }
        // If metadata operations are found, apply here and remove the operations from the original patch request.
        if (metadataOperationsNode.size() != 0) {
            dsoRest.setMetadata(
                applyMetadataPatch(
                    metadataOperationsNode,
                    dsoRest.getMetadata())
            );
            toRemove.sort(Collections.reverseOrder());
            for (Integer idx : toRemove) {
                ((ArrayNode) jsonNode).remove(idx);
            }
        }
        // Apply non-metadata patch operations.
        applyResourcePatch(dsoRest, jsonNode, domainClass);
        return dsoRest;
    }

    /**
     * Creates a new MetadataRest object to which the patch has been applied.
     * @param patch the metadata patch request
     * @param metadataRest metadata rest object
     * @return modified rest object
     */
    private MetadataRest applyMetadataPatch(JsonNode patch, MetadataRest metadataRest) {
        try {
            ObjectNode objectNode = objectMapper.createObjectNode();
            JsonNode metadataNode = objectMapper.valueToTree(metadataRest);
            objectNode.replace("metadata", metadataNode);
            JsonPatch.applyInPlace(patch, objectNode);
            return objectMapper.treeToValue(objectNode.get("metadata"), MetadataRest.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Uses Spring patch to the REST resource's non-metadata fields.
     * @param dsoRest the rest model instance
     * @param jsonNode the patch request
     * @param domainClass the class type
     */
    private void applyResourcePatch(R dsoRest, JsonNode jsonNode, Class<R> domainClass) {
        try {
            new JsonPatchPatchConverter(objectMapper).convert(jsonNode).apply(dsoRest, domainClass);
        } catch (SpelEvaluationException | PatchException e) {
            throw new DSpaceBadRequestException(e.getMessage(), e);
        }
    }



}
