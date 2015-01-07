/*
 */
package org.datadryad.rest.handler;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class HandlerException extends Exception {

    public HandlerException() {
        super();
    }

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public HandlerException(Throwable throwable) {
        super(throwable);
    }
}
