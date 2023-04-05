/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
