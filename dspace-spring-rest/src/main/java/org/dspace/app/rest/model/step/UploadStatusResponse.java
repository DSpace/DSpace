package org.dspace.app.rest.model.step;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public abstract class UploadStatusResponse {

	private Boolean status;
	private String message;

	@JsonInclude(Include.NON_NULL)
	public String getMessage() {	
		return message;
	}

	@JsonInclude(Include.NON_NULL)
	public Boolean isStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
