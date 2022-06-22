/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.orcid.model.validator.OrcidValidationError;

/**
 * A Runtime exception that occurs when an ORCID object that must be send to
 * ORCID is not valid.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidValidationException extends RuntimeException {

    private static final long serialVersionUID = 3377335341871311369L;

    private final List<OrcidValidationError> errors;

    public OrcidValidationException(OrcidValidationError error) {
        this(List.of(error));
    }

    public OrcidValidationException(List<OrcidValidationError> errors) {
        super("Errors occurs during ORCID object validation");
        this.errors = errors;
    }

    public List<OrcidValidationError> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ". Error codes: " + formatErrors();
    }

    private String formatErrors() {
        return errors.stream()
            .map(error -> error.getCode())
            .collect(Collectors.joining(","));
    }

}
