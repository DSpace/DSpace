/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.exception;

/**
 * This class provides an exception to be used when a conflict on a resource
 * occurs.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResourceConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Object resource;

    /**
     * Create a ResourceConflictException with a message and the conflicting
     * resource.
     *
     * @param message  the error message
     * @param resource the resource that caused the conflict
     */
    public ResourceConflictException(String message, Object resource) {
        super(message);
        this.resource = resource;
    }

    public Object getResource() {
        return resource;
    }

}
