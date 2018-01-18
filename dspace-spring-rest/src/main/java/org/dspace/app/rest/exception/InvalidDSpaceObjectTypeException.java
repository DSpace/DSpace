/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This class creates an Exception to be used when the given DSpaceObjectType is invalid
 */
public class InvalidDSpaceObjectTypeException extends InvalidRequestException {
    public InvalidDSpaceObjectTypeException(String message) {
        super(message);
    }
}
