/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;

/**
 * Base class for all resource patch operations.
 *
 * @author Michael Spalti
 */
public abstract class PatchOperation<R extends RestModel, T>
        implements ResourcePatchOperation<R> {

    /**
     * Updates the rest model by applying the patch operation.
     *
     * @param resource  the rest model.
     * @param operation the patch operation.
     * @return the updated rest model.
     * @throws DSpaceBadRequestException
     */
    public abstract R perform(R resource, Operation operation);

    /**
     * Throws PatchBadRequestException for missing operation value.
     *
     * @param value the value to test
     */
    void checkOperationValue(Object value) {
        if (value == null) {
            throw new DSpaceBadRequestException("No value provided for the operation.");
        }
    }

    /**
     * Allows clients to use either a boolean or a string representation of boolean value.
     *
     * @param value the operation value
     * @return the original or derived boolean value
     * @throws DSpaceBadRequestException
     */
    Boolean getBooleanOperationValue(Object value) {
        Boolean bool;

        if (value instanceof String) {
            bool = BooleanUtils.toBooleanObject((String) value);
            if (bool == null) {
                // make sure the string was converted to boolean.
                throw new DSpaceBadRequestException("Boolean value not provided.");
            }
        } else {
            bool = (Boolean) value;
        }
        return bool;
    }

    /**
     * This method should return the typed array to be used in the
     * LateObjectEvaluator evaluation of json arrays.
     *
     * @return
     */
    protected abstract Class<T[]> getArrayClassForEvaluation();

    /**
     * This method should return the object type to be used in the
     * LateObjectEvaluator evaluation of json objects.
     *
     * @return
     */
    protected abstract Class<T> getClassForEvaluation();

}
