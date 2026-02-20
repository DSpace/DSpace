/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;
import java.util.ArrayList;
import java.util.List;

import org.dspace.validation.model.ValidationError;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UnprocessableEditException extends UnprocessableEntityException {

    private static final long serialVersionUID = 1L;

    private List<ValidationError> errors = new ArrayList<ValidationError>();

    public UnprocessableEditException(List<ValidationError> errors) {
        super(null);
        setErrors(errors);
    }

    public UnprocessableEditException(List<ValidationError> errors, String message) {
        super(message);
        setErrors(errors);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

}