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
 * Interface to support validation on submission process
 *
 * TODO should be supported InProgressSubmission (t.b.d)
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface Validation {

    String OPERATION_PATH_SECTIONS = "sections";

    List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config);

    String getName();
}
