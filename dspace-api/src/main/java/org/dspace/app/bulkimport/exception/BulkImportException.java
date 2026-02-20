/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.exception;

/**
 * Exception for errors that occurs during the items bulk import via excel.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkImportException extends RuntimeException {

    private static final long serialVersionUID = -74730626862418515L;

    /**
     * Constructor with error message and cause.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public BulkImportException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with error message.
     *
     * @param message the error message
     */
    public BulkImportException(String message) {
        super(message);
    }

    /**
     * Constructor with error cause.
     *
     * @param cause the error cause
     */
    public BulkImportException(Throwable cause) {
        super(cause);
    }

}
