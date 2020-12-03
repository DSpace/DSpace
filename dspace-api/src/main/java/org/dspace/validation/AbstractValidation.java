/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.validation.model.ValidationError;

/**
 * Abstract class to provide basic management of errors resulting from a validation on a submission
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class AbstractValidation implements Validation {

    private String name;

    /**
     * An unique name to identify the validation implementation
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add an error message (i18nKey) for a specific json path
     *
     * @param i18nKey
     *            the validation error message as a key to internationalize
     * @param path
     *            the json path that identify the wrong data in the submission. It could be as specific as a single
     *            value in a multivalued attribute or general of a "whole" section
     */
    public void addError(List<ValidationError> errors, String i18nKey, String path) {
        boolean found = false;
        if (StringUtils.isNotBlank(i18nKey)) {
            for (ValidationError error : errors) {
                if (i18nKey.equals(error.getMessage())) {
                    error.getPaths().add(path);
                    found = true;
                    break;
                }
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
