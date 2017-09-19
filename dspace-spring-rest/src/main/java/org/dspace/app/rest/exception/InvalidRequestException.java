package org.dspace.app.rest.exception;

/**
 * Created by tom on 19/09/2017.
 */
public class InvalidRequestException extends Exception {
    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
