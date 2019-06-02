/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.services.model.Request;

/**
 * Class to abstract the HTTP PATCH method operation
 *
 * @param <T>
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class PatchOperation<T extends Object> {

    public abstract void perform(Context context, Request currentRequest, InProgressSubmission source,
            Operation operation)
        throws Exception;

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

    public String getAbsolutePath(String fullpath) {
        String[] path = fullpath.substring(1).split("/", 3);
        String absolutePath = "";
        if (path.length > 2) {
            absolutePath = path[2];
        }
        return absolutePath;
    }

    protected abstract Class<T[]> getArrayClassForEvaluation();

    protected abstract Class<T> getClassForEvaluation();

}