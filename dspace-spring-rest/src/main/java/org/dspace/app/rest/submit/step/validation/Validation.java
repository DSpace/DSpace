/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;

/**
 * Interface to support validation on submission process
 *
 * TODO should be supported InProgressSubmission (t.b.d)
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public interface Validation {

    String getName();

    List<? extends ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                       SubmissionStepConfig config) throws DCInputsReaderException, SQLException;

}
