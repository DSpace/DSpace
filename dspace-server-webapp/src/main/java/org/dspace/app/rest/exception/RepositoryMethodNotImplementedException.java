/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This is the exception to capture details about call to a methods not
 * exposed or not implemented by the repository
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED, reason = "This repository doesn't provide or implement the " +
    "requested method")
public class RepositoryMethodNotImplementedException extends RuntimeException {
    String model;
    String method;

    public RepositoryMethodNotImplementedException(String model, String method) {
        this.model = model;
        this.method = method;
    }

}
