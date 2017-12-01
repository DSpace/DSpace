package org.dspace.submit.model;

public class LanguageFormField {
	
	private String display;
	
	private String code;

	public LanguageFormField(String code, String display) {
		this.code = code;
		this.display = display;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	
	
}
