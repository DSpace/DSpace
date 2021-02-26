/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.exceptions;

public class InvalidSetSpecException extends Exception {
    public InvalidSetSpecException() {
    }

    public InvalidSetSpecException(String message) {
        super(message);
    }

    public InvalidSetSpecException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSetSpecException(Throwable cause) {
        super(cause);
    }
}
