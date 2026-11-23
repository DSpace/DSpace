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
 * Exception thrown when applying a JSON Patch to an {@link org.dspace.content.edit.EditItem} would increase
 * the number of validation errors in the item.
 * <p>
 * This exception enforces the principle that edits to existing archived items (via EditItem) should not
 * degrade the quality or validity of the item. When a user attempts to modify an item through the
 * EditItem REST API using JSON Patch operations, the system validates the item both before and after
 * applying the patch. If the number of validation errors increases, this exception is thrown to reject
 * the operation.
 * <p>
 * <strong>Usage Context:</strong>
 * <ul>
 *   <li>Thrown by {@link org.dspace.app.rest.repository.EditItemRestRepository#patch} during EditItem updates</li>
 *   <li>Triggered when {@code numInitialErrors < editErrors} after applying a JSON Patch</li>
 *   <li>Returns HTTP 422 (Unprocessable Entity) with the full list of validation errors</li>
 * </ul>
 * <p>
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @see org.dspace.app.rest.repository.EditItemRestRepository#patch
 * @see org.dspace.validation.service.ValidationService
 * @see ValidationError
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