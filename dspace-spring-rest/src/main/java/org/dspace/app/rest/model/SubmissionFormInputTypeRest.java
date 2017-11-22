/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The InputFormField REST Resource. It is not addressable directly, only used
 * as inline object in the InputForm resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@JsonInclude(value = Include.NON_NULL)
public class SubmissionFormInputTypeRest {
	private String type;
	private String regex;
	private AuthorityRest authority;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public AuthorityRest getAuthority() {
		return authority;
	}

	public void setAuthority(AuthorityRest authority) {
		this.authority = authority;
	}
}
