/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Class for PATCH REPLACE operations on the primary bitstream of bundles
 * <br><code>
 * curl -X PATCH http://${dspace.server.url}/api/core/bundles/<:bundle-uuid>
 * -H "Content-Type: application/json"
 * -d '[{"op": "replace", "path": "/primaryBitstreamUUID", "value": "<:bitstream-uuid>"}]'
 * </code>
 */
@Component
public class BundlePrimaryBitstreamUUIDReplaceOperation<R> extends PatchOperation<R> {

    private static final String OPERATION_PATH_PRIMARY_BITSTREAM_UUID = "/primaryBitstreamUUID";

    @Autowired
    BitstreamService bitstreamService;

    @Override
    public R perform(Context context, R resource, Operation operation) throws SQLException {
        checkOperationValue(operation.getValue());
        if (supports(resource, operation)) {
            Bundle bundle = (Bundle) resource;
            if (bundle.getPrimaryBitstream() == null) {
                throw new DSpaceBadRequestException("Bundle '" + bundle.getName()
                                                        + "' does not have a primary bitstream.");
            }

            UUID uuid;
            try {
                uuid = UUID.fromString((String) operation.getValue());
            } catch (Exception e) {
                throw new DSpaceBadRequestException("No valid UUID was given.");
            }

            Bitstream bitstream = bitstreamService.find(context, uuid);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No bitstream found with id: " + uuid);
            }
            if (!bundle.getBitstreams().contains(bitstream)) {
                throw new UnprocessableEntityException("Bundle '" + bundle.getName()
                           + "' does not contain bitstream with id: " + uuid);
            }

            bundle.setPrimaryBitstreamID(bitstream);
            return resource;
        } else {
            throw new DSpaceBadRequestException("BundlePrimaryBitstreamUUIDReplaceOperation " +
                                                    "does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Bundle
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
            && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_PRIMARY_BITSTREAM_UUID));
    }
}
