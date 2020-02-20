/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.CopyOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class for PATCH COPY operations on Dspace Objects' metadata
 * Usage: (can be done on other dso than Item also):
 * - COPY metadata (with schema.identifier.qualifier) value of a dso (here: Item) from given index to end of list of md
 * <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/items/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "copy",
 * "from": "/metadata/schema.identifier.qualifier/indexToCopyFrom"
 * "path": "/metadata/schema.identifier.qualifier/-"}]'
 * </code>
 *
 * @author Maria Verdonck (Atmire) on 18/11/2019
 */
@Component
public class DSpaceObjectMetadataCopyOperation<R extends DSpaceObject> extends PatchOperation<R> {

    @Autowired
    DSpaceObjectMetadataPatchUtils metadataPatchUtils;

    @Override
    public R perform(Context context, R resource, Operation operation) throws SQLException {
        DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(resource);
        MetadataField metadataField = metadataPatchUtils.getMetadataField(context, operation);
        String[] partsFromCopy = ((CopyOperation) operation).getFrom().split("/");
        String indexToCopyFrom = (partsFromCopy.length > 3) ? partsFromCopy[3] : null;

        copy(context, resource, dsoService, metadataField, indexToCopyFrom);
        return resource;
    }

    /**
     * Copies metadata of the dso from indexFrom to new index at end of md
     *
     * @param context         context patch is being performed in
     * @param dso             dso being patched
     * @param dsoService      service doing the patch in db
     * @param metadataField   md field being patched
     * @param indexToCopyFrom index we're copying metadata from
     */
    private void copy(Context context, DSpaceObject dso, DSpaceObjectService dsoService, MetadataField metadataField,
                      String indexToCopyFrom) {
        metadataPatchUtils.checkMetadataFieldNotNull(metadataField);
        List<MetadataValue> metadataValues = dsoService.getMetadata(dso, metadataField.getMetadataSchema().getName(),
                metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
        try {
            int indexToCopyFromInt = Integer.parseInt(indexToCopyFrom);
            if (indexToCopyFromInt >= 0 && metadataValues.size() > indexToCopyFromInt
                    && metadataValues.get(indexToCopyFromInt) != null) {
                MetadataValue metadataValueToCopy = metadataValues.get(indexToCopyFromInt);
                MetadataValueRest metadataValueRestToCopy
                        = metadataPatchUtils.convertMdValueToRest(metadataValueToCopy);
                // Add metadata value to end of md list
                dsoService.addAndShiftRightMetadata(context, dso, metadataField.getMetadataSchema().getName(),
                        metadataField.getElement(), metadataField.getQualifier(), metadataValueRestToCopy.getLanguage(),
                        metadataValueRestToCopy.getValue(), metadataValueRestToCopy.getAuthority(),
                        metadataValueRestToCopy.getConfidence(), -1);
            } else {
                throw new UnprocessableEntityException("There is no metadata of this type at that index");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("This index (" + indexToCopyFrom + ") is not valid number.", e);
        } catch (SQLException e) {
            throw new DSpaceBadRequestException("SQLException in DspaceObjectMetadataCopyOperation.copy trying to " +
                    "add metadata to dso.", e);
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return ((operation.getPath().startsWith(metadataPatchUtils.OPERATION_METADATA_PATH)
                || operation.getPath().equals(metadataPatchUtils.OPERATION_METADATA_PATH))
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_COPY)
                && objectToMatch instanceof DSpaceObject);
    }
}
