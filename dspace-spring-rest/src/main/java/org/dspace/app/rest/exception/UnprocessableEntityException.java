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
 * Unprocessable request (e.g. missed mandatory field) specified with a 422 (Unprocessable Entity)
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Unprocessable request")
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException() {
        super("Error during request process or missed a mandatory field");
    }

    public UnprocessableEntityException(String message) {
        super(message);
    }
}
