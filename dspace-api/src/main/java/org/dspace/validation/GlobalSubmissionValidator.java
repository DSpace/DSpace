/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import java.util.List;

import org.dspace.app.util.SubmissionConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;

/**
 * Interface to support global validation on submission process.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface GlobalSubmissionValidator {

    /**
     * Validate the given object based on the given submission configuration.
     *
     * @param  context          the DSpace Context
     * @param  obj              the object to validate
     * @param  submissionConfig the submission configuration
     * @return                  the validation errors, if any
     */
    List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionConfig submissionConfig);
}
