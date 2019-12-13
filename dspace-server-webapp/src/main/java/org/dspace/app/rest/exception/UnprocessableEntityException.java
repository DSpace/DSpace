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
 * Unprocessable request: Can be specified with a 422 (Unprocessable Entity)
 * response ([RFC4918], Section 11.2) when the server understands the patch
 * document and the syntax of the patch document appears to be valid, but the
 * server is incapable of processing the request. This might include attempts to
 * modify a resource in a way that would cause the resource to become invalid; A
 * response json object should be returned with more details about the exception
 *
 * TODO (i.e. the idx of the patch operation that fail, detail about the
 * failure on execution such as wrong idx in an array path, unsupported patch
 * operation, etc.)
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Unprocessable request")
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnprocessableEntityException(String message) {
        super(message);
    }

}
