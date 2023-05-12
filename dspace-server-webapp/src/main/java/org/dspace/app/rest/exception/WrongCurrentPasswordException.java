/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This exception is thrown when the provided current password is wrong.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class WrongCurrentPasswordException extends RuntimeException {

    private static final long serialVersionUID = 7774965236190392985L;

    public WrongCurrentPasswordException(String message) {
        super(message);
    }

}
