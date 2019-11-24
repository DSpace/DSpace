/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;

/**
 * Base class for move patch operations.
 */
public abstract class MovePatchOperation<R extends RestModel, T> extends PatchOperation<R, T> {

    /**
     * The index to move the object from
     */
    protected int from;

    /**
     * The index to move the object to
     */
    protected int to;

    /**
     * Implements the patch operation for move operations.
     * Before performing the move operation this method checks
     * if the arguments provided are valid
     *
     * @param resource  the rest model.
     * @param operation the move patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    @Override
    public R perform(R resource, Operation operation) {
        checkMoveOperation(operation);
        return move(resource, operation);
    }

    /**
     * Executes the move patch operation.
     *
     * @param resource  the rest model.
     * @param operation the move patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    abstract R move(R resource, Operation operation);

    /**
     * This method checks if the operation contains any invalid arguments. Invalid arguments include:
     * - from and path point to the same index
     * - either of the indexes are negative
     * @param operation the move patch operation.
     */
    private void checkMoveOperation(Operation operation) {
        if (!(operation instanceof MoveOperation)) {
            throw new DSpaceBadRequestException(
                    "Expected a MoveOperation, but received " + operation.getClass().getName() + " instead."
            );
        }
        from = getLocationFromPath(((MoveOperation)operation).getFrom());
        to = getLocationFromPath(operation.getPath());
        if (from == to) {
            throw new DSpaceBadRequestException(
                    "The \"from\" location must be different from the \"to\" location."
            );
        }
        if (from < 0) {
            throw new DSpaceBadRequestException("A negative \"from\" location was provided: " + from);
        }
        if (to < 0) {
            throw new DSpaceBadRequestException("A negative \"to\" location was provided: " + to);
        }
    }

    /**
     * Fetches and returns the index from a path argument
     * @param path the provided path (e.g. "/_links/bitstreams/1/href")
     */
    protected int getLocationFromPath(String path) {
        String[] parts = StringUtils.split(path, "/");
        String locationStr;
        if (parts.length > 1) {
            if (StringUtils.equals(parts[parts.length - 1], "href")) {
                locationStr = parts[parts.length - 2];
            } else {
                locationStr = parts[parts.length - 1];
            }
        } else {
            locationStr = parts[0];
        }
        return Integer.parseInt(locationStr);
    }
}

