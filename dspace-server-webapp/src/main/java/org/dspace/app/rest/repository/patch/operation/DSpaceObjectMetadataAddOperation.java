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
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.ProvenanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 Class for PATCH ADD operations on Dspace Objects' metadata
 * Usage: (can be done on other dso than Item also):
 * - ADD metadata (with schema.identifier.qualifier) value of a dso (here: Item) to end of list of md
 * <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/items/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "
 * /metadata/schema.identifier.qualifier(/0|-)}", "value": "metadataValue"]'
 * </code>
 * @author Maria Verdonck (Atmire) on 18/11/2019
 */
@Component
public class DSpaceObjectMetadataAddOperation<R extends DSpaceObject> extends PatchOperation<R> {

    private static final Logger log = LoggerFactory.getLogger(DSpaceObjectMetadataAddOperation.class);
    @Autowired
    DSpaceObjectMetadataPatchUtils metadataPatchUtils;
    @Autowired
    ProvenanceService provenanceService;

    @Override
    public R perform(Context context, R resource, Operation operation) throws SQLException {
        DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(resource);
        MetadataValueRest metadataValueToAdd = metadataPatchUtils.extractMetadataValueFromOperation(operation);
        MetadataField metadataField = metadataPatchUtils.getMetadataField(context, operation);
        String indexInPath = metadataPatchUtils.getIndexFromPath(operation.getPath());

        add(context, resource, dsoService, metadataField, metadataValueToAdd, indexInPath);
        return resource;
    }

    /**
     * Adds metadata to the dso (appending if index is 0 or left out, prepending if -)
     *
     * @param context       context patch is being performed in
     * @param dso           dso being patched
     * @param dsoService    service doing the patch in db
     * @param metadataField md field being patched
     * @param metadataValue value of md element
     * @param index         determines whether we're prepending (-) or appending (0) md value
     */
    private void add(Context context, DSpaceObject dso, DSpaceObjectService dsoService, MetadataField metadataField,
                     MetadataValueRest metadataValue, String index) {
        metadataPatchUtils.checkMetadataFieldNotNull(metadataField);
        int indexInt = 0;
        if (index != null && index.equals("-")) {
            indexInt = -1;
        }
        try {
            dsoService.addAndShiftRightMetadata(context, dso, metadataField.getMetadataSchema().getName(),
                    metadataField.getElement(), metadataField.getQualifier(), metadataValue.getLanguage(),
                    metadataValue.getValue(), metadataValue.getAuthority(), metadataValue.getConfidence(), indexInt);
            provenanceService.addMetadata(context, dso, metadataField);
        } catch (SQLException e) {
            String msg;
            msg = "SQLException in DspaceObjectMetadataAddOperation.add trying to add " +
                    "metadata to dso.";
            log.error(msg, e);
            throw new DSpaceBadRequestException(msg, e);
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (operation.getPath().startsWith(metadataPatchUtils.OPERATION_METADATA_PATH)
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                && objectToMatch instanceof DSpaceObject);
    }
}
