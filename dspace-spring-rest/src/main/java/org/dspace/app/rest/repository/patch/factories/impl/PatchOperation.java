/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * This patch class includes abstract and implemented methods that
 * can be used to type check objects derived from operation values.
 *
 * @author Michael Spalti
 */
public abstract class PatchOperation<R extends RestModel, T extends Object>
        implements ResourcePatchOperation<R> {

    public abstract RestModel perform(R resource, Operation operation)
            throws PatchBadRequestException;

    /**
     * Throws PatchBadRequestException for missing operation value.
     * @param value the value to test
     */
    public void checkOperationValue(Object value) {
        if (value == null) {
            throw new PatchBadRequestException("No value provided for the operation.");
        }
    }

    /**
     * Allows clients to use either a boolean or a string representation of boolean value.
     * @param value the operation value
     * @return the original or derived boolean value
     */
    public Boolean getBooleanOperationValue(Object value) throws PatchBadRequestException {
        Boolean bool;

        if (value instanceof String) {
            bool = BooleanUtils.toBooleanObject((String) value);
            if (bool == null) {
                // make sure the string was converted to boolean.
                throw new PatchBadRequestException("Boolean value not provided.");
            }
        } else {
            bool = (Boolean) value;
        }
        return bool;
    }

    // This is duplicated code (see org.dspace.app.rest.submit.factory.impl.PatchOperation)
    // If it stays here, it should be DRY. Current patch resource patch operations do not
    // use these methods since operation values are either strings or booleans.
    // These methods handle JsonValueEvaluator instances for json objects and arrays,
    // as returned by the JsonPatchConverter. A complete implementation of the PatchOperation
    // class will need these methods.
    public List<T> evaluateArrayObject(LateObjectEvaluator value) {
        List<T> results = new ArrayList<T>();
        T[] list = null;
        if (value != null) {
            LateObjectEvaluator object = (LateObjectEvaluator) value;
            list = (T[]) object.evaluate(getArrayClassForEvaluation());
        }

        for (T t : list) {
            results.add(t);
        }
        return results;
    }

    public T evaluateSingleObject(LateObjectEvaluator value) {
        T single = null;
        if (value != null) {
            LateObjectEvaluator object = (LateObjectEvaluator) value;
            single = (T) object.evaluate(getClassForEvaluation());
        }
        return single;
    }

    /**
     * This method should return the typed array to be used in the
     * LateObjectEvaluator evaluation of json arrays.
     * @return
     */
    protected abstract Class<T[]> getArrayClassForEvaluation();

    /**
     * This method should return the object type to be used in the
     * LateObjectEvaluator evaluation of json objects.
     * @return
     */
    protected abstract Class<T> getClassForEvaluation();

}
