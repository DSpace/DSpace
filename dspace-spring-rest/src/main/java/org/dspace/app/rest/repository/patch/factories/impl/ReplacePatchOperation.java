/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;

/**
 * The base class for replace operations.
 *
 * @param <R>
 * @param <T>
 */
public abstract class ReplacePatchOperation<R extends RestModel, T>
        extends PatchOperation<R, T> {

    /**
     * Executes the replace patch operation.
     *
     * @param resource
     * @param operation
     * @return
     * @throws PatchBadRequestException
     */
    abstract R replace(R resource, Operation operation)
            throws PatchBadRequestException;

    /**
     * Replace operations are not allowed on non-existent values.
     * Null values may exist in the RestModel for certain fields
     * (usually non-boolean).
     *
     * @param value the rest model value to be replaced.
     * @throws PatchBadRequestException
     */
    void checkModelForExistingValue(Object value) throws PatchBadRequestException {
        if (value == null) {
            throw new PatchBadRequestException("Attempting to replace a non-existent value.");
        }
    }
}
