/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;

/**
 * Base class for add patch operations.
 *
 * @author Marie Verdonck
 */
public abstract class AddPatchOperation<R extends RestModel, T>
        extends PatchOperation<R, T> {

    /**
     * Implements the patch operation for add operations.
     * Before performing the add operation this method checks
     * for a non-null operation value.
     *
     * @param resource  the rest model.
     * @param operation the add patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    @Override
    public R perform(R resource, Operation operation) {
        checkOperationValue(operation.getValue());
        return add(resource, operation);

    }

    /**
     * Executes the add patch operation.
     *
     * @param resource  the rest model.
     * @param operation the add patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    abstract R add(R resource, Operation operation);


}
