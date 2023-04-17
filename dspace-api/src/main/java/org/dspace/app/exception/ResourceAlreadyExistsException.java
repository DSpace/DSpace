/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.exception;

/**
 * This class provides an exception to be used when trying to save a resource
 * that already exists.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a ResourceAlreadyExistsException with a message and the already
     * existing resource.
     *
     * @param message  the error message
     */
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }


}
