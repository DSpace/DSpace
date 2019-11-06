/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.JsonPatchConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
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
public class DspaceObjectMetadataOperation<R extends DSpaceObject> extends PatchOperation<R> {
    private static final Logger log
            = org.apache.logging.log4j.LogManager.getLogger(DspaceObjectMetadataOperation.class);

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String METADATA_PATH = "/metadata";
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonPatchConverter jsonPatchConverter = new JsonPatchConverter(objectMapper);

    /**
     * Implements the patch operation for metadata operations.
     * @param context   context we're performing patch in
     * @param resource  the dso.
     * @param operation the metadata patch operation.
     * @return the updated dso
     */
    @Override
    public R perform(Context context, R resource, Operation operation) {
        DSpaceObject dSpaceObject = (DSpaceObject) resource;
        performPatchOperation(context, dSpaceObject, operation);
        return (R) dSpaceObject;
    }

    /**
     * Gets all the info about the metadata we're patching from the operation and sends it to the
     *      appropriate method to perform the actual patch
     * @param context       Context we're performing patch in
     * @param dso           object we're performing metadata patch on
     * @param operation     patch operation
     */
    private void performPatchOperation(Context context, DSpaceObject dso, Operation operation) {
        DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        String mdElement = StringUtils.substringBetween(operation.getPath(), METADATA_PATH + "/", "/");
        if (mdElement == null) {
            mdElement = StringUtils.substringAfter(operation.getPath(), METADATA_PATH + "/");
        }
        String[] seq = mdElement.split("\\.");
        String schema = seq.length > 1 ? seq[0] : null;
        String element = seq.length > 1 ? seq[1] : null;
        String qualifier = seq.length == 3 ? seq[2] : null;

        String[] parts = operation.getPath().split("/");
        String indexInPath = (parts.length > 3) ? parts[3] : null;
        String propertyOfMd = (parts.length > 4) ? parts[4] : null;

        try {
            MetadataValue metadataValue = null;
            if (operation.getValue() != null) {
                JsonNode valueNode = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                if (valueNode.isArray()) {
                    metadataValue = objectMapper.treeToValue(valueNode.get(0), MetadataValue.class);
                } else {
                    metadataValue = objectMapper.treeToValue(valueNode, MetadataValue.class);
                }
            }

            switch (operation.getOp()) {
                case "add":
                    add(context, dso, dsoService, schema, element, qualifier, metadataValue, indexInPath);
                    return;
                case "remove":
                    remove(context, dso, dsoService, schema, element, qualifier, indexInPath);
                    return;
                case "replace":
                    replace(context, dso, dsoService, schema, element, qualifier,
                            metadataValue, indexInPath, propertyOfMd);
                    return;
                case "move":
                    String[] partsFrom = ((MoveOperation)operation).getFrom().split("/");
                    String indexTo = (partsFrom.length > 3) ? partsFrom[3] : null;
                    move(context, dso, dsoService, schema, element, qualifier, indexInPath, indexTo);
                    return;
                default:
                    throw new DSpaceBadRequestException(
                            "This operation is not supported."
                    );
            }
        } catch (IOException e) {
            log.error("IOException in DspaceObjectMetadataOperation.performPatchOperation trying " +
                    "to map json from operation.value to MetadataValue class.", e);
        }
    }

    /**
     * Adds metadata to the dso (appending if index is 0 or left out, prepending if -)
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param schema            schema of md field being patched
     * @param element           element of md field being patched
     * @param qualifier         qualifier of md field being patched
     * @param metadataValue     value of md element
     * @param index             determines whether we're prepending (-) or appending (0) md value
     */
    private void add(Context context, DSpaceObject dso,
                     DSpaceObjectService dsoService, String schema, String element,
                     String qualifier, MetadataValue metadataValue, String index) {
        int indexInt = 0;
        if (index != null && index.equals("-")) {
            indexInt = -1;
        }
        try {
            dsoService.addAndShiftRightMetadata(context, dso, schema, element, qualifier,
                    metadataValue.getLanguage(), metadataValue.getValue(),
                    metadataValue.getAuthority(), metadataValue.getConfidence(), indexInt);
        } catch (SQLException e) {
            log.error("SQLException in DspaceObjectMetadataOperation.add trying to add metadata to dso.", e);
        }
    }

    /**
     * Removes a metadata from the dso at a given index (or all of that type if no index was given)
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param schema            schema of md field being patched
     * @param element           element of md field being patched
     * @param qualifier         qualifier of md field being patched
     * @param index             index at where we want to delete metadata
     */
    private void remove(Context context, DSpaceObject dso,
                     DSpaceObjectService dsoService, String schema, String element,
                     String qualifier, String index) {
        if (index == null) {
            //remove all metadata of this type
            try {
                dsoService.clearMetadata(context, dso, schema, element, qualifier, Item.ANY);
            } catch (SQLException e) {
                log.error("SQLException in DspaceObjectMetadataOperation.remove trying to " +
                        "remove metadata from dso.", e);
            }
        } else {
            //remove metadata at index
            List<MetadataValue> metadataValues = dsoService.getMetadata(dso, schema, element, qualifier, Item.ANY);
            try {
                int indexInt = Integer.parseInt(index);
                if (indexInt >= 0 && metadataValues.size() > indexInt
                        && metadataValues.get(indexInt) != null) {
                    //remove that metadata
                    dsoService.removeMetadataValues(context, dso,
                            Arrays.asList(metadataValues.get(Integer.parseInt(index))));
                } else {
                    throw new UnprocessableEntityException("There is no metadata of this type at that index");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("This index (" + index + ") is not valid nr", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new UnprocessableEntityException("There is no metadata of this type at that index");
            } catch (SQLException e) {
                log.error("SQLException in DspaceObjectMetadataOperation.remove trying to remove " +
                        "metadata from dso.", e);
            }
        }
    }

    /**
     * Replaces metadata in the dso; 4 cases:
     *      - If we replace everything: clearMetadata & add the new ones
     *      - If we replace for a single field: clearMetadata on the field & add the new ones
     *      - A single existing metadata value: Retrieve the metadatavalue object & make alerations directly on this object
     *      - A single existing metadata property: Retrieve the metadatavalue object & make alerations directly on this object
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param schema            schema of md field being patched
     * @param element           element of md field being patched
     * @param qualifier         qualifier of md field being patched
     * @param metadataValue     value of md element
     */
    private void replace(Context context, DSpaceObject dso,
                     DSpaceObjectService dsoService, String schema, String element,
                     String qualifier, MetadataValue metadataValue, String index, String propertyOfMd) {
        // replace entire set of metadata
        if (schema == null) {
            try {
                dsoService.clearMetadata(context, dso, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                // TODO How to add new md value if exists? No knowledge of seq
            } catch (SQLException e) {
                log.error("SQLException in DspaceObjectMetadataOperation.replace trying to remove" +
                        "and replace metadata from dso.", e);
            }
        }
        // replace all metadata for existing key
        if (schema != null && index == null) {
            try {
                dsoService.clearMetadata(context, dso, schema, element, qualifier, Item.ANY);
                this.add(context, dso, dsoService, schema, element, qualifier, metadataValue, null);
            } catch (SQLException e) {
                log.error("SQLException in DspaceObjectMetadataOperation.replace trying to remove " +
                        "and replace metadata from dso.", e);
            }
        }
        // replace single existing metadata value
        if (schema != null && index != null && propertyOfMd == null) {
            try {
                List<MetadataValue> metadataValues = dsoService.getMetadata(dso, schema, element,
                        qualifier, Item.ANY);
                int indexInt = Integer.parseInt(index);
                if (indexInt >= 0 && metadataValues.size() > indexInt
                        && metadataValues.get(indexInt) != null) {
                    // Alter this existing md
                    MetadataValue existingMdv = metadataValues.get(indexInt);
                    existingMdv.setAuthority(metadataValue.getAuthority());
                    existingMdv.setConfidence(metadataValue.getConfidence());
                    existingMdv.setLanguage(metadataValue.getLanguage());
                    existingMdv.setValue(metadataValue.getValue());
                } else {
                    throw new UnprocessableEntityException("There is no metadata of this type at that index");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("This index (" + index + ") is not valid nr", e);
            }
        }
        // replace single property of exiting metadata value
        if (schema != null && index != null && propertyOfMd != null) {
            try {
                List<MetadataValue> metadataValues = dsoService.getMetadata(dso, schema, element, qualifier, Item.ANY);
                int indexInt = Integer.parseInt(index);
                if (indexInt >= 0 && metadataValues.size() > indexInt && metadataValues.get(indexInt) != null) {
                    // Alter only asked propertyOfMd
                    MetadataValue existingMdv = metadataValues.get(indexInt);
                    if (propertyOfMd.equals("authority")) {
                        existingMdv.setAuthority(metadataValue.getAuthority());
                    }
                    if (propertyOfMd.equals("confidence")) {
                        existingMdv.setConfidence(metadataValue.getConfidence());
                    }
                    if (propertyOfMd.equals("language")) {
                        existingMdv.setLanguage(metadataValue.getLanguage());
                    }
                    if (propertyOfMd.equals("value")) {
                        existingMdv.setValue(metadataValue.getValue());
                    }
                } else {
                    throw new UnprocessableEntityException("There is no metadata of this type at that index");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("This index (" + index + ") is not valid nr", e);
            }
        }
    }

    /**
     * Moves metadata of the dso from indexFrom to indexTo
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param schema            schema of md field being patched
     * @param element           element of md field being patched
     * @param qualifier         qualifier of md field being patched
     * @param indexFrom         index we're moving metadata from
     * @param indexTo         index we're moving metadata to
     */
    private void move(Context context, DSpaceObject dso,
                     DSpaceObjectService dsoService, String schema, String element,
                     String qualifier, String indexFrom, String indexTo) {
        try {
            dsoService.moveMetadata(context, dso, schema, element, qualifier,
                    Integer.parseInt(indexFrom), Integer.parseInt(indexTo));
        } catch (SQLException e) {
            log.error("SQLException in DspaceObjectMetadataOperation.move trying to move metadata in dso.", e);
        }
    }

    @Override
    public boolean supports(DSpaceObject R, String path) {
        return ((path.startsWith(METADATA_PATH) || path.equals(METADATA_PATH)) && R instanceof DSpaceObject);
    }
}
