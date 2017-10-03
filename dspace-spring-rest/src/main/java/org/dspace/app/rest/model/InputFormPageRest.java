/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The InputFormPage REST Resource. It is not addressable directly, only used
 * as inline object in the InputForm resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@JsonInclude(value=Include.NON_NULL)
public class InputFormPageRest {
	private String header;
	private boolean mandatory;
	private List<InputFormFieldRest> fields;

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public List<InputFormFieldRest> getFields() {
		return fields;
	}
	
	public void setFields(List<InputFormFieldRest> fields) {
		this.fields = fields;
	}

	@JsonIgnore
	public ScopeEnum getScope() {
		ScopeEnum scope = fields.get(0).getScope();
		for (InputFormFieldRest field : fields) {
			if (!Objects.equals(field.getScope(), scope)) {
				return null;
			}
		}

		return scope;
	}

	@JsonIgnore
	public SubmissionVisibilityRest getVisibility() {
		SubmissionVisibilityRest visibility = fields.get(0).getVisibility();
		for (InputFormFieldRest field : fields) {
			if (!Objects.equals(field.getVisibility(), visibility)) {
				return null;
			}
		}

		return visibility;
	}
}
