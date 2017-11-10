package org.dspace.submit.model;

import java.util.List;

public class SubmissionFormField {
	private String label;
	private boolean mandatory;
	private boolean repeatable;
	private String mandatoryMessage;
	private String hints;

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

}
