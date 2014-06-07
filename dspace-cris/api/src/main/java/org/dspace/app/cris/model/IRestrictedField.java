/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

public interface IRestrictedField {
	public Integer getVisibility();
	public String getValue();
	public void setVisibility(Integer visibility);
	public void setValue(String value);
}
