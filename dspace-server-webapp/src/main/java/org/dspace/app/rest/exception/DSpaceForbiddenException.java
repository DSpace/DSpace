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
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Forbidden request")
public class DSpaceForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 8869331967657914409L;

    public DSpaceForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public DSpaceForbiddenException(String message) {
        super(message);
    }

}