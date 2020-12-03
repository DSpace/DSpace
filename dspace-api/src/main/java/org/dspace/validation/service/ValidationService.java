/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service;

import java.util.List;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;

/**
 * Service to validate the given {@link InProgressSubmission} instance.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ValidationService {

    /**
     * Validate the given {@link InProgressSubmission} instance for the given
     * {@link SubmissionStepConfig}.
     *
     * @param  context the DSpace context
     * @param  obj     the {@link InProgressSubmission} instance to validate
     * @param  config  the submission step configuration
     * @return         the validation errors, if any
     */
    List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config);
}
