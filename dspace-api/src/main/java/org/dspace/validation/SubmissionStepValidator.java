/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import java.util.List;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;

/**
 * Interface to support step validation on submission process.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface SubmissionStepValidator {

    /**
     * Validate the given {@link InProgressSubmission} object in relation to the
     * supplied submission step config.
     *
     * @param  context the DSpace context
     * @param  obj     the object to validate
     * @param  config  the submission step configuration
     * @return         the validation errors, if any
     */
    List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config);

    /**
     * The name of the submission step validable with this validator.
     *
     * @return the submission step name
     */
    String getName();
}
