/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.util.Map;

/**
 * The Authority Entry REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorityEntryRest implements Serializable {
	public static final String NAME = "authorityEntry";
	private String id;
	private String display;
	private String value;
	private long count;
	private Map<String, String> otherInformation;
	private AuthorityEntryRest parent;

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getDisplay() {
		return display;
	}
	public void setDisplay(String value) {
		this.display = value;
	}
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}
	public Map<String, String> getOtherInformation() {
		return otherInformation;
	}
	public void setOtherInformation(Map<String, String> otherInformation) {
		this.otherInformation = otherInformation;
	}
	public AuthorityEntryRest getParent() {
		return parent;
	}
	public void setParent(AuthorityEntryRest parent) {
		this.parent = parent;
	}
	public static String getName() {
		return NAME;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
