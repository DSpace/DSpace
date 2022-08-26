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
 * When a request is malformed, we use this exception to indicate this to the client
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Not Found")
public class DSpaceFeedbackNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4631940402294095433L;

    public DSpaceFeedbackNotFoundException(String message) {
        this(message, null);
    }

    public DSpaceFeedbackNotFoundException(String message, Exception e) {
        super(message, e);
    }

}
