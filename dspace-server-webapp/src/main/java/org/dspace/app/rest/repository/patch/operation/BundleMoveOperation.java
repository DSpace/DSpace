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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Bundle move patches.
 * This operation moves bitstreams within a bundle from one index to another.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/bundles/<:id-bundle> -H "
 * Content-Type: application/json" -d '[{ "op": "move", "path": "
 * /_links/bitstreams/1/href", "from": "/_links/bitstreams/0/href"]'
 * </code>
 */
@Component
public class BundleMoveOperation extends PatchOperation<Bundle> {

    @Autowired
    BundleService bundleService;

    @Autowired
    DSpaceObjectMetadataPatchUtils dspaceObjectMetadataPatchUtils;

    private static final String OPERATION_PATH_BUNDLE_MOVE = "/_links/bitstreams/";

    /**
     * Executes the move patch operation.
     *
     * @param bundle  the bundle in which we want to move files around.
     * @param operation the move patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     */
    @Override
    public Bundle perform(Context context, Bundle bundle, Operation operation) {
        try {
            MoveOperation moveOperation = (MoveOperation) operation;
            final int from = Integer.parseInt(dspaceObjectMetadataPatchUtils.getIndexFromPath(moveOperation.getFrom()));
            final int to = Integer.parseInt(dspaceObjectMetadataPatchUtils.getIndexFromPath(moveOperation.getPath()));

            int totalAmount = bundle.getBitstreams().size();

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

            bundleService.updateBitstreamOrder(context, bundle, from, to);
        } catch (SQLException | AuthorizeException e) {
            throw new DSpaceBadRequestException(e.getMessage(), e);
        }

        return bundle;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Bundle && operation.getOp().trim().equalsIgnoreCase(OPERATION_MOVE)
                && operation.getPath().trim().startsWith(OPERATION_PATH_BUNDLE_MOVE));
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
