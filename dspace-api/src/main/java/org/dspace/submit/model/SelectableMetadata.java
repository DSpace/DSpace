package org.dspace.submit.model;

public class SelectableMetadata {
	private String metadata;
	private String label;
	private String authority;
	private Boolean closed;

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String key) {
		this.metadata = key;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}
	
	public String getAuthority() {
		return authority;
	}

	public Boolean isClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}
}
