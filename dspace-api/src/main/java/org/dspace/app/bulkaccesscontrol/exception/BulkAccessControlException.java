/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol.exception;

/**
 * Exception for errors that occurs during the bulk access control
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class BulkAccessControlException extends RuntimeException {

    private static final long serialVersionUID = -74730626862418515L;

    /**
     * Constructor with error message and cause.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public BulkAccessControlException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with error message.
     *
     * @param message the error message
     */
    public BulkAccessControlException(String message) {
        super(message);
    }

    /**
     * Constructor with error cause.
     *
     * @param cause the error cause
     */
    public BulkAccessControlException(Throwable cause) {
        super(cause);
    }

}
