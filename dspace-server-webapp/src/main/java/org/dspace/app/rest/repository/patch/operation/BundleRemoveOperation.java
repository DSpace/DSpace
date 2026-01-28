/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.RESTBundleNotFoundException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * A PATCH operation for removing bundles in bulk from the repository.
 * Deleting a bundle will also delete all bitstreams contained within it.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/bundles -H "Content-Type: application/json"
 * -d '[
 *       {"op": "remove", "path": "/bundles/${bundle1UUID}"},
 *       {"op": "remove", "path": "/bundles/${bundle2UUID}"}
 *     ]'
 * </code>
 *
 * @author Ali Jaber - Atmire
 */
@Component
public class BundleRemoveOperation extends PatchOperation<Bundle> {
    @Autowired
    BundleService bundleService;
    @Autowired
    AuthorizeService authorizeService;

    public static final String OPERATION_PATH_BUNDLE_REMOVE = "/bundles/";

    @Override
    public Bundle perform(Context context, Bundle resource, Operation operation) throws SQLException {
        String bundleIDtoDelete = operation.getPath().replace(OPERATION_PATH_BUNDLE_REMOVE, "");
        Bundle bundleToDelete = bundleService.find(context, UUID.fromString(bundleIDtoDelete));
        if (bundleToDelete == null) {
            throw new RESTBundleNotFoundException(bundleIDtoDelete);
        }
        authorizeBundleRemoveAction(context, bundleToDelete, Constants.DELETE);

        try {
            bundleService.delete(context, bundleToDelete);
        } catch (AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return objectToMatch == null && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE) &&
            operation.getPath().trim().startsWith(OPERATION_PATH_BUNDLE_REMOVE);
    }

    /**
     * Authorize the bundle remove action.
     *
     * @param context   the DSpace context
     * @param bundle    the bundle to check authorization for
     * @param operation the operation constant (e.g., Constants.DELETE)
     * @throws SQLException if a database error occurs
     */
    public void authorizeBundleRemoveAction(Context context, Bundle bundle, int operation)
        throws SQLException {
        try {
            authorizeService.authorizeAction(context, bundle, operation);
        } catch (AuthorizeException e) {
            throw new AccessDeniedException("The current user is not allowed to remove the bundle", e);
        }
    }
}
