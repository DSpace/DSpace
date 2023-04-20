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
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.Bundle;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Class for PATCH REMOVE operations on the primary bitstream of bundles
 * <br><code>
 * curl -X PATCH http://${dspace.server.url}/api/core/bundles/<:bundle-uuid>
 * -H "Content-Type: application/json"
 * -d '[{"op": "remove", "path": "/primarybitstream"}]'
 * </code>
 */
@Component
public class BundlePrimaryBitstreamRemoveOperation<R> extends PatchOperation<R> {

    private static final String OPERATION_PATH_PRIMARY_BITSTREAM = "/primarybitstream";

    @Override
    public R perform(Context context, R resource, Operation operation) throws SQLException {
        if (supports(resource, operation)) {
            Bundle bundle = (Bundle) resource;
            if (bundle.getPrimaryBitstream() == null) {
                throw new DSpaceBadRequestException("Bundle '" + bundle.getName()
                                                           + "' does not have a primary bitstream.");
            }
            bundle.unsetPrimaryBitstreamID();
            return resource;
        } else {
            throw new DSpaceBadRequestException("BundlePrimaryBitstreamRemoveOperation " +
                                                    "does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Bundle
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE)
            && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_PRIMARY_BITSTREAM));
    }
}
