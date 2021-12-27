/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
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

    private final List<SubmissionStepValidator> stepValidators;

    private final List<GlobalSubmissionValidator> globalValidators;

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

        SubmissionConfig submissionConfig = submissionConfigReader
            .getSubmissionConfigByCollection(obj.getCollection());

        List<ValidationError> errors = new ArrayList<ValidationError>();

        errors.addAll(notHiddenStepsValidations(context, obj, submissionConfig));
        errors.addAll(globalValidations(context, obj, submissionConfig));

        return errors;

    }

    private List<ValidationError> notHiddenStepsValidations(Context context, InProgressSubmission<?> obj,
                                                            SubmissionConfig submissionConfig) {

        List<ValidationError> errors = new ArrayList<>();

        for (SubmissionStepConfig stepConfig : submissionConfig) {

            if (isStepHidden(stepConfig, obj)) {
                continue;
            }

            stepValidators.stream()
                          .filter(validation -> validation.getName().equals(stepConfig.getType()))
                          .flatMap(validation -> validation.validate(context, obj, stepConfig).stream())
                          .forEach(errors::add);

        }

        return errors;

    }

    private List<ValidationError> globalValidations(Context context, InProgressSubmission<?> obj,
                                                    SubmissionConfig submissionConfig) {

        return globalValidators.stream()
                               .flatMap(validator -> validator.validate(context, obj, submissionConfig).stream())
                               .collect(Collectors.toList());

    }

    private boolean isStepHidden(SubmissionStepConfig stepConfig, InProgressSubmission<?> obj) {
        return stepConfig.isHiddenForInProgressSubmission(obj);
    }


}
