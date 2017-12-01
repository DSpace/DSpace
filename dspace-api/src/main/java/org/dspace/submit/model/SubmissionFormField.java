/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class SubmissionFormField {
	private String label;
	private boolean mandatory;
	private boolean repeatable;
	private String mandatoryMessage;
	private String hints;

	private List<SelectableMetadata> selectableMetadata;
	private List<LanguageFormField> languageCodes;
	
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

	public List<LanguageFormField> getLanguageCodes() {
		if(languageCodes==null) {
			languageCodes = new ArrayList<LanguageFormField>();
		}
		return languageCodes;
	}

	public void setLanguageCodes(List<LanguageFormField> languageCodes) {
		this.languageCodes = languageCodes;
	}

}
