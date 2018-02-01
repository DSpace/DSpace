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
 * Malformed patch document (taken from rfc5789#section-2.2) - When the server
 * determines that the patch document provided by the client is not properly
 * formatted, it SHOULD return a 400 (Bad Request) response. The definition of
 * badly formatted depends on the patch document chosen.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad Request")
public class PatchBadRequestException extends RuntimeException {
	
	public PatchBadRequestException(String message) {
		super(message);
	}
	
}
