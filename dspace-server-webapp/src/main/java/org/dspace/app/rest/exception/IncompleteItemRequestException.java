package org.dspace.app.rest.exception;

/**
 * Thrown to indicate that a mandatory Item Request attribute was not provided.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class IncompleteItemRequestException
        extends UnprocessableEntityException {
    public IncompleteItemRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompleteItemRequestException(String message) {
        super(message);
    }
}
