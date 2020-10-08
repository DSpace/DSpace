/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.exception;

public class InvalidEnumeratedDataValueException extends Exception {

	private static final long serialVersionUID = -1237789070936139031L;

	private String requestedField;
	
	private String requestedValue;
	
	public InvalidEnumeratedDataValueException(String message) {
	    super(message);
	}
	
	public InvalidEnumeratedDataValueException(String message, String requestedField, String requestValue) {
		super(message);
		this.requestedField = requestedField;
		this.requestedValue = requestValue;
	}
	
	public String getRequestedField() {
		return requestedField;
	}
	
	
	public String getRequestedValue() {
		return requestedValue;
	}
}
