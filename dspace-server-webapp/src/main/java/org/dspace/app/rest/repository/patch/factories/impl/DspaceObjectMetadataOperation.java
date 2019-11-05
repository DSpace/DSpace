/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

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
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.springframework.stereotype.Component;

/**
 * Class for PATCH operations on Dspace Objects' metadata
 * Options (can be done on other dso than Item also):
 *      - ADD metadata (with schema.identifier.qualifier) value of a dso (here: Item)
 *          <code>
 *              curl -X PATCH http://${dspace.url}/api/items/<:id-item> -H "
 *              Content-Type: application/json" -d '[{ "op": "add", "path": "
 *              /metadata/schema.identifier.qualifier(/0|-)}", "value": "metadataValue"]'
 *          </code>
 *      - REMOVE metadata
 *          <code>
 *              curl -X PATCH http://${dspace.url}/api/items/<:id-item> -H "
 *              Content-Type: application/json" -d '[{ "op": "remove",
 *              "path": "/metadata/schema.identifier.qualifier(/0|-)}"]'
 *          </code>
 *      - REPLACE metadata
 *          <code>
 *              curl -X PATCH http://${dspace.url}/api/items/<:id-item> -H "
 *              Content-Type: application/json" -d '[{ "op": "replace", "path": "
 *              /metadata/schema.identifier.qualifier}", "value": "metadataValue"]'
 *          </code>
 *      - ORDERING metadata
 *          <code>
 *              curl -X PATCH http://${dspace.url}/api/items/<:id-item> -H "
 *              Content-Type: application/json" -d '[{ "op": "move",
 *              "from": "/metadata/schema.identifier.qualifier/index"
 *              "path": "/metadata/schema.identifier.qualifier/newIndex"}]'
 *          </code>
 *
 * @author Maria Verdonck (Atmire) on 30/10/2019
 */
@Component
public class DspaceObjectMetadataOperation<R extends RestModel> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String METADATA_PATH = "/metadata";
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonPatchConverter jsonPatchConverter = new JsonPatchConverter(objectMapper);

    /**
     * Implements the patch operation for metadata operations.
     *
     * @param resource  the rest model.
     * @param operation the metadata patch operation.
     * @return the updated rest model.
     */
    @Override
    public R perform(R resource, Operation operation) {
        DSpaceObjectRest dSpaceObjectRest = (DSpaceObjectRest) resource;
        List<Operation> operations = new ArrayList<Operation>();
        operations.add(operation);
        dSpaceObjectRest.setMetadata(applyMetadataPatch(
                jsonPatchConverter.convert(new Patch(operations)),
                dSpaceObjectRest.getMetadata()));
        return (R) dSpaceObjectRest;
    }

    /**
     * Apply the actual metadata patch by replacing the original metadata node
     *  with the newly created one based on the patch body
     * @param patch             Metadata patch used for the replacement
     * @param metadataRest      Original metadata rest object
     * @return  Newly created metadata node
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

    @Override
    public boolean supports(RestModel R, String path) {
        return ((path.equals(METADATA_PATH) || path.startsWith(METADATA_PATH + "/")) && R instanceof DSpaceObjectRest);
    }
}
