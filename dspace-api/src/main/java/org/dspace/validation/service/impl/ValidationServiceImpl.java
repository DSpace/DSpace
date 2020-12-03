/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.validation.Validation;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ValidationService} that cycle on all the
 * {@link Validation} instance defined in the context to validate the given
 * {@link InProgressSubmission} instance.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ValidationServiceImpl implements ValidationService {

    @Autowired
    private List<Validation> validations;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        return validations.stream()
            .filter(validation -> validation.getName().equals(config.getType()))
            .flatMap(validation -> validation.validate(context, obj, config).stream())
            .collect(Collectors.toList());
    }

}
