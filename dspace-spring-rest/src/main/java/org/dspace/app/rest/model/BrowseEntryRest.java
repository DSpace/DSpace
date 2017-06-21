/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Browse Entry REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class BrowseEntryRest implements Serializable {
	private static final long serialVersionUID = -3415049466402327251L;
	public static final String NAME = "browseEntry";
	private String authority;
	private String value;
	private String valueLang;
	private long count;

	@JsonIgnore
	private BrowseIndexRest browseIndex;
	
	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValueLang() {
		return valueLang;
	}

	public void setValueLang(String valueLang) {
		this.valueLang = valueLang;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}
	
	public BrowseIndexRest getBrowseIndex() {
		return browseIndex;
	}
	
	public void setBrowseIndex(BrowseIndexRest browseIndex) {
		this.browseIndex = browseIndex;
	}
}
