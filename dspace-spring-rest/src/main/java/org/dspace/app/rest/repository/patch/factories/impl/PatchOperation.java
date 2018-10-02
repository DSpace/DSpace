/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

public abstract class PatchOperation<DSO extends DSpaceObject, T extends Object>
        implements ResourcePatchOperation<DSO> {

    public abstract void perform(Context context, DSO resource, Operation operation)
            throws SQLException, AuthorizeException, PatchBadRequestException;

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
     * Throws PatchBadRequestException for missing operation value.
     * @param value the value to check
     */
    public void checkOperationValue(T value) {
        if (value == null) {
            throw new PatchBadRequestException("No value provided for the operation.");
        }
    }

    protected abstract Class<T[]> getArrayClassForEvaluation();

    protected abstract Class<T> getClassForEvaluation();

}
