/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Bundle move patches.
 * This operation moves bitstreams within a bundle from one index to another.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/bundles/<:id-bundle> -H "
 * Content-Type: application/json" -d '[{ "op": "move", "path": "
 * /_links/bitstreams/1/href", "from": "/_links/bitstreams/0/href"]'
 * </code>
 */
@Component
public class BundleMoveOperation extends MovePatchOperation<BundleRest, Integer> {

    @Autowired
    BundleService bundleService;

    @Autowired
    RequestService requestService;

    /**
     * Executes the move patch operation.
     *
     * @param resource  the rest model.
     * @param operation the move patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     */
    @Override
    public BundleRest move(BundleRest resource, Operation operation) {
        Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getServletRequest());
        try {
            Bundle bundle = bundleService.findByIdOrLegacyId(context, resource.getId());
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

        return resource;
    }

    /**
     * This method should return the typed array to be used in the
     * LateObjectEvaluator evaluation of json arrays.
     *
     * @return
     */
    @Override
    protected Class<Integer[]> getArrayClassForEvaluation() {
        return Integer[].class;
    }

    /**
     * This method should return the object type to be used in the
     * LateObjectEvaluator evaluation of json objects.
     *
     * @return
     */
    @Override
    protected Class<Integer> getClassForEvaluation() {
        return Integer.class;
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
