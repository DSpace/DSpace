package org.dspace.app.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "This endpoint is not found in the system")
public class RepositoryNotFoundException extends RuntimeException {
	String model;

	public RepositoryNotFoundException(String model) {
		this.model = model;
	}

}
