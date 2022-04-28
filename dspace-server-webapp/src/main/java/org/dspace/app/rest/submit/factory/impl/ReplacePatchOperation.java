/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * Class to manage HTTP PATCH method operation REPLACE
 *
 * @param <T>
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class ReplacePatchOperation<T extends Object> extends PatchOperation<T> {

    @Override
    public void perform(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation operation) throws Exception {
        replace(context, currentRequest, source, operation.getPath(), operation.getValue());
    }

    abstract void replace(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            String string, Object value) throws Exception;

}
