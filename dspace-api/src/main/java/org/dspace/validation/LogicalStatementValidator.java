/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;

/**
 * Implementation of {@link SubmissionStepValidator} that use the validation framework to
 * apply conditional checks.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class LogicalStatementValidator implements GlobalSubmissionValidator {

    private String errorKey;

    private List<String> paths;

    private List<String> metadataFields;

    private Filter filter;

    private DCInputsReader inputReader;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionConfig config) {

        boolean isValid = validate(context, obj.getItem());

        if (isValid) {
            return Collections.emptyList();
        }

        List<ValidationError> errors = new ArrayList<>();
        addPathErrors(errors);
        addMetadataErrors(errors, obj, config);

        return errors;
    }

    private boolean validate(Context context, Item item) {
        try {
            return filter.getResult(context, item);
        } catch (LogicalStatementException e) {
            throw new RuntimeException(e);
        }
    }

    private void addMetadataErrors(List<ValidationError> errors, InProgressSubmission<?> obj, SubmissionConfig config) {
        if (CollectionUtils.isEmpty(metadataFields)) {
            return;
        }

        for (SubmissionStepConfig step : config) {

            if (!SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(step.getType())) {
                continue;
            }

            List<String> stepMetadataFields = getSubmissionFormMetadata(step);
            for (String metadata : metadataFields) {
                if (stepMetadataFields.contains(metadata)) {
                    addError(errors, errorKey, "/" + OPERATION_PATH_SECTIONS + "/" + step.getId() + "/" + metadata);
                }
            }

        }
    }

    private void addPathErrors(List<ValidationError> errors) {
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        paths.forEach(path -> addError(errors, errorKey, path));
    }

    private List<String> getSubmissionFormMetadata(SubmissionStepConfig config) {
        try {
            return getInputReader().getSubmissionFormMetadata(config);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
    }

    public DCInputsReader getInputReader() {
        if (inputReader == null) {
            try {
                inputReader = new DCInputsReader();
            } catch (DCInputsReaderException e) {
                throw new RuntimeException(e);
            }
        }
        return inputReader;
    }

    public void setInputReader(DCInputsReader inputReader) {
        this.inputReader = inputReader;
    }

    public void setErrorKey(String errorKey) {
        this.errorKey = errorKey;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setMetadataFields(List<String> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

}
