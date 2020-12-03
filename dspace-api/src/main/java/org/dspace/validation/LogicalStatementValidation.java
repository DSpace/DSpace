/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import java.util.Collections;
import java.util.List;

import org.dspace.app.util.SubmissionConfig;
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
public class LogicalStatementValidation implements GlobalSubmissionValidator {

    private final String errorKey;

    private final List<String> paths;

    private final List<String> metadata;

    private final Filter filter;

    public LogicalStatementValidation(String errorKey, List<String> paths, List<String> metadata, Filter filter) {
        this.errorKey = errorKey;
        this.paths = paths;
        this.filter = filter;
        this.metadata = metadata;
    }

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionConfig config) {

        boolean isValid = validate(context, obj.getItem());

        if (isValid) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private boolean validate(Context context, Item item) {
        try {
            return filter.getResult(context, item);
        } catch (LogicalStatementException e) {
            throw new RuntimeException(e);
        }
    }

}
