/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validation result of {@link DynamicLayoutToolValidator#validate}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class DynamicLayoutToolValidationResult {

    private List<String> errors;

    private List<String> warnings;

    /**
     * Creates a validation result with empty error and warning lists.
     */
    public DynamicLayoutToolValidationResult() {
        this.errors = new ArrayList<String>();
        this.warnings = new ArrayList<String>();
    }

    /**
     * Returns whether not valid.
     */
    public boolean isNotValid() {
        return !this.errors.isEmpty();
    }

    /**
     * Adds an error message to this validation result.
     *
     * @param error the error message to add
     */
    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * Returns the errors.
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns the warnings.
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Adds a warning message to this validation result.
     *
     * @param warning the warning message to add
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

}
