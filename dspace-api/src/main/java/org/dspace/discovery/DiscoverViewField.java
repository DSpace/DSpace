/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;


/**
 */
public class DiscoverViewField {
	private boolean mandatory = false;
	private String field;
	private String decorator;
	
	public DiscoverViewField(String field, String decorator, boolean mandatory) {
		this.decorator = decorator;
		this.field = field;
		this.mandatory = mandatory;
	}
	
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

}
