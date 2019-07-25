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
 * Base class for replace patch operations.
 *
 * @author Michael Spalti
 */
public abstract class ReplacePatchOperation<R extends RestModel, T>
        extends PatchOperation<R, T> {

    /**
     * Implements the patch operation for replace operations.
     * Before performing the replace operation this method checks
     * for a non-null operation value and a non-null value on the rest model
     * (replace operations should only be applied to an existing value).
     *
     * @param resource  the rest model.
     * @param operation the replace patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    @Override
    public R perform(R resource, Operation operation) {

        checkOperationValue(operation.getValue());
        checkModelForExistingValue(resource, operation);
        return replace(resource, operation);

    }

    /**
     * Executes the replace patch operation.
     *
     * @param resource  the rest model.
     * @param operation the replace patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     * @throws UnprocessableEntityException
     */
    abstract R replace(R resource, Operation operation);

    /**
     * Replace operations are not allowed on non-existent values.
     * Null values may exist in the RestModel for certain fields
     * (usually non-boolean). This method should be implemented
     * to assure that the replace operation acts only on an existing value.
     *
     * @param resource the rest model.
     * @throws DSpaceBadRequestException
     */
    abstract void checkModelForExistingValue(R resource, Operation operation);


}
