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
 * Validation result of {@link CrisLayoutToolValidator#validate}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class CrisLayoutToolValidationResult {

    private List<String> errors;

    private List<String> warnings;

    public CrisLayoutToolValidationResult() {
        this.errors = new ArrayList<String>();
        this.warnings = new ArrayList<String>();
    }

    public boolean isNotValid() {
        return !this.errors.isEmpty();
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

}
