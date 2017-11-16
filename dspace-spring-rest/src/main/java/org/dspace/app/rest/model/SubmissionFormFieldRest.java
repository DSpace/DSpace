/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import java.util.List;

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
public class SubmissionFormFieldRest {
	private String label;
	private boolean mandatory;
	private boolean repeatable;
	private String mandatoryMessage;
	private String hints;
	private SubmissionFormInputTypeRest input;
	private ScopeEnum scope;
	private SubmissionVisibilityRest visibility;
	private List<SelectableMetadata> selectableMetadata;

	public List<SelectableMetadata> getSelectableMetadata() {
		return selectableMetadata;
	}

	public void setSelectableMetadata(List<SelectableMetadata> selectableMetadata) {
		this.selectableMetadata = selectableMetadata;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public boolean isRepeatable() {
		return repeatable;
	}

	public void setRepeatable(boolean repeatable) {
		this.repeatable = repeatable;
	}

	public String getMandatoryMessage() {
		return mandatoryMessage;
	}

	public void setMandatoryMessage(String mandatoryMessage) {
		this.mandatoryMessage = mandatoryMessage;
	}

	public String getHints() {
		return hints;
	}

	public void setHints(String hints) {
		this.hints = hints;
	}

	public SubmissionFormInputTypeRest getInput() {
		return input;
	}

	public void setInput(SubmissionFormInputTypeRest input) {
		this.input = input;
	}

	public ScopeEnum getScope() {
		return scope;
	}

	public void setScope(ScopeEnum scope) {
		this.scope = scope;
	}

	public SubmissionVisibilityRest getVisibility() {
		return visibility;
	}

	public void setVisibility(SubmissionVisibilityRest visibility) {
		if (visibility != null && (visibility.getMain() != null || visibility.getOther() != null)) {
			this.visibility = visibility;
		}
	}
}
