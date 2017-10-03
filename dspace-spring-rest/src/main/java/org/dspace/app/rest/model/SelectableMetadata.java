/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * The SelectableMetadata REST Resource. It is not addressable directly, only
 * used as inline object in the InputForm resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class SelectableMetadata {
	private MetadataFieldRest field;
	private String label;
	private AuthorityRest authority;

	public MetadataFieldRest getField() {
		return field;
	}

	public void setField(MetadataFieldRest field) {
		this.field = field;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setAuthority(AuthorityRest authority) {
		this.authority = authority;
	}
	
	public AuthorityRest getAuthority() {
		return authority;
	}
}
