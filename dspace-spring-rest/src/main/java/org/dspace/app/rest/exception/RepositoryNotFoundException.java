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
 * This is the exception to capture details about call to inexistent resources
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "This endpoint is not found in the system")
public class RepositoryNotFoundException extends RuntimeException {
	String apiCategory;
	String model;

	public RepositoryNotFoundException(String apiCategory, String model) {
		this.apiCategory = apiCategory;
		this.model = model;
	}

}
