/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.validation.GlobalSubmissionValidator;
import org.dspace.validation.SubmissionStepValidator;
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

    private List<SubmissionStepValidator> stepValidators;

    private List<GlobalSubmissionValidator> globalValidators;

    private SubmissionConfigReader submissionConfigReader;

    @Autowired
    public ValidationServiceImpl(List<SubmissionStepValidator> stepValidators,
        List<GlobalSubmissionValidator> globalValidators) {
        this.stepValidators = stepValidators;
        this.globalValidators = globalValidators;
    }

    @PostConstruct
    private void setup() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj) {
        Collection collection = obj.getCollection();
        if (collection == null) {
            return Collections.emptyList();
        }

        List<ValidationError> errors = new ArrayList<ValidationError>();

        SubmissionConfig submissionConfig = submissionConfigReader.getSubmissionConfigByCollection(collection);

        for (SubmissionStepConfig stepConfig : submissionConfig) {
            stepValidators.stream()
                .filter(validation -> validation.getName().equals(stepConfig.getType()))
                .flatMap(validation -> validation.validate(context, obj, stepConfig).stream())
                .forEach(errors::add);
        }

        globalValidators.stream()
            .flatMap(validator -> validator.validate(context, obj, submissionConfig).stream())
            .forEach(errors::add);

        return errors;

    }

}
