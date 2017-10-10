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
 * This is the exception to capture details about call to a search methods not
 * exposed by the repository
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "This repository doesn't provide the requested search method")
public class RepositorySearchMethodNotFoundException extends RuntimeException {
	String model;
	String method;
	public RepositorySearchMethodNotFoundException(String model, String method) {
		this.model = model;
		this.method = method;
	}

}
