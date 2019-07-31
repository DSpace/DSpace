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
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonPatch;
import org.dspace.app.rest.converter.JsonPatchConverter;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;

/**
 * Base class for DSpaceObject-based PATCH operations, providing common functionality.
 *
 * @param <R> the type of DSpaceObjectRest object the class is applicable to.
 */
public abstract class DSpaceObjectPatch<R extends DSpaceObjectRest> extends AbstractResourcePatch<R> {

    private static final String METADATA_PATH = "/metadata";

    private ObjectMapper objectMapper = new ObjectMapper();

    private JsonPatchConverter jsonPatchConverter = new JsonPatchConverter(objectMapper);

    /**
     * Applies the given patch operations to the given DSpaceObjectRest instance.
     *
     * This extends the default implementation by first applying metadata-based patch operations,
     * then applying any others.
     *
     * @param dsoRest the instance to apply the changes to.
     * @param operations the list of patch operations.
     * @return the modified DSpaceObectRest instance.
     */
    @Override
    public R patch(R dsoRest, List<Operation> operations) {
        List<Operation> metadataOperations = new ArrayList<>();
        List<Operation> otherOperations = new ArrayList<>();

        for (Operation operation : operations) {
            String path = operation.getPath();
            if (path.equals(METADATA_PATH) || path.startsWith(METADATA_PATH + "/")) {
                metadataOperations.add(operation);
            } else {
                otherOperations.add(operation);
            }
        }

        if (!metadataOperations.isEmpty()) {
            dsoRest.setMetadata(applyMetadataPatch(
                    jsonPatchConverter.convert(new Patch(metadataOperations)),
                    dsoRest.getMetadata()));
        }

        return super.patch(dsoRest, otherOperations);
    }

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
}
