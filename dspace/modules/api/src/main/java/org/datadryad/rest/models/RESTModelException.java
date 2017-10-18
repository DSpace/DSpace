/*
 */
package org.datadryad.rest.models;

public class RESTModelException extends Exception {
    public RESTModelException() {
        super();
    }

    public RESTModelException(String message) {
        super(message);
    }

    public RESTModelException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public RESTModelException(Throwable throwable) {
        super(throwable);
    }
}
