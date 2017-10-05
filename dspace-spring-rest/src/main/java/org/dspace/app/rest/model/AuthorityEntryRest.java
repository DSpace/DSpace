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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Authority Entry REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorityEntryRest implements Serializable {
	public static final String NAME = "authorityEntry";
	private String id;
	private String value;
	private long count;
	private Map<String, Serializable> extraInformation;
	private AuthorityEntryRest parent;

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}
	public Map<String, Serializable> getExtraInformation() {
		return extraInformation;
	}
	public void setExtraInformation(Map<String, Serializable> extraInformation) {
		this.extraInformation = extraInformation;
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

}
