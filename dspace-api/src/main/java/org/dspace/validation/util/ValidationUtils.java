/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.validation.model.ValidationError;

/**
 * Utility class for validations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class ValidationUtils {

    private ValidationUtils() {

    }

    /**
     * Add an error message (i18nKey) for a specific json path.
     *
     * @param i18nKey the validation error message as a key to internationalize
     * @param path    the json path that identify the wrong data in the submission.
     *                It could be as specific as a single value in a multivalued
     *                attribute or general of a "whole" section
     */
    public static void addError(List<ValidationError> errors, String i18nKey, String path) {

        if (StringUtils.isBlank(i18nKey)) {
            return;
        }

        boolean found = false;
        for (ValidationError error : errors) {
            if (i18nKey.equals(error.getMessage())) {
                error.getPaths().add(path);
                found = true;
                break;
            }
        }

        if (!found) {
            ValidationError error = new ValidationError();
            error.setMessage(i18nKey);
            error.getPaths().add(path);
            errors.add(error);
        }

    }
}
