package org.dspace.discovery.configuration;

public class DiscoveryViewFieldConfiguration {
	private boolean mandatory = false;
	private String field;
	private String decorator;
	private String separator = " ";
	private String preHtml = " ";
	private String postHtml = " ";
	
	public boolean isMandatory() {
		return mandatory;
	}
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getDecorator() {
		return decorator;
	}
	public void setDecorator(String decorator) {
		this.decorator = decorator;
	}
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	public String getSeparator() {
		return separator;
	}
	public String getPostHtml() {
		return postHtml;
	}
	public void setPostHtml(String postHtml) {
		this.postHtml = postHtml;
	}
	public String getPreHtml() {
		return preHtml;
	}
	public void setPreHtml(String preHtml) {
		this.preHtml = preHtml;
	}
}
