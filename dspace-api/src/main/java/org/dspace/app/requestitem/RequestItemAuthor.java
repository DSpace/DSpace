/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

/**
 * Simple DTO to transfer data about the corresponding author for the Request
 * Copy feature
 * 
 * @author Andrea Bollini
 * 
 */
public class RequestItemAuthor {
	private String fullName;
	private String email;

	public RequestItemAuthor(String fullName, String email) {
		super();
		this.fullName = fullName;
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}
}
