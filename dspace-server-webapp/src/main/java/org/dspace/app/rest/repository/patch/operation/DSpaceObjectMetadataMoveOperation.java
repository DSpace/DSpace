/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class for PATCH MOVE operations on Dspace Objects' metadata
 * Usage: (can be done on other dso than Item also):
 * - MOVE metadata (with schema.identifier.qualifier) value of a dso (here: Item)
 * from given index in from to given index in path
 * <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/items/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "move",
 * "from": "/metadata/schema.identifier.qualifier/indexToCopyFrom"
 * "path": "/metadata/schema.identifier.qualifier/indexToCopyTo"}]'
 * </code>
 *
 * @author Maria Verdonck (Atmire) on 18/11/2019
 */
@Component
public class DSpaceObjectMetadataMoveOperation<R extends DSpaceObject> extends PatchOperation<R> {

    @Autowired
    DSpaceObjectMetadataPatchUtils metadataPatchUtils;

    @Override
    public R perform(Context context, R resource, Operation operation) throws SQLException {
        DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(resource);
        MetadataField metadataField = metadataPatchUtils.getMetadataField(context, operation);
        String indexInPath = metadataPatchUtils.getIndexFromPath(operation.getPath());
        String indexToMoveFrom = metadataPatchUtils.getIndexFromPath(((MoveOperation) operation).getFrom());

        move(context, resource, dsoService, metadataField, indexInPath, indexToMoveFrom);
        return resource;
    }

    /**
     * Moves metadata of the dso from indexFrom to indexTo
     *
     * @param context       context patch is being performed in
     * @param dso           dso being patched
     * @param dsoService    service doing the patch in db
     * @param metadataField md field being patched
     * @param indexFrom     index we're moving metadata from
     * @param indexTo       index we're moving metadata to
     */
    private void move(Context context, DSpaceObject dso,
                      DSpaceObjectService dsoService, MetadataField metadataField, String indexFrom, String indexTo) {
        metadataPatchUtils.checkMetadataFieldNotNull(metadataField);
        try {
            dsoService.moveMetadata(context, dso, metadataField.getMetadataSchema().getName(),
                    metadataField.getElement(), metadataField.getQualifier(), Integer.parseInt(indexFrom),
                    Integer.parseInt(indexTo));
        } catch (SQLException e) {
            throw new DSpaceBadRequestException("SQLException in DspaceObjectMetadataMoveOperation.move trying to " +
                    "move metadata in dso.", e);
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return ((operation.getPath().startsWith(metadataPatchUtils.OPERATION_METADATA_PATH)
                || operation.getPath().equals(metadataPatchUtils.OPERATION_METADATA_PATH))
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_MOVE)
                && objectToMatch instanceof DSpaceObject);
    }
}
