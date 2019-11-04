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
 * Base class for remove patch operations.
 *
 * @author Marie Verdonck
 */
public abstract class RemovePatchOperation<R extends RestModel>
        extends PatchOperation<R> {

    /**
     * Implements the patch operation for remove operations.
     * Before performing the remove operation this method checks
     * for a non-null operation value
     *
     * @param resource  the rest model.
     * @param operation the remove patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    @Override
    public R perform(R resource, Operation operation) {

        checkOperationValue(operation.getValue());
        return remove(resource, operation);

    }

    /**
     * Executes the remove patch operation.
     *
     * @param resource  the rest model.
     * @param operation the remove patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    abstract R remove(R resource, Operation operation);


}
