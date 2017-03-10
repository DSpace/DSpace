/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;
import java.util.Map;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A Metadata Value to embedd in a REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataValueRest {
	
	private String value;
	
	private int confidence;
	
	private AuthorityValueRest authorityValue;
	
	private String lang;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getConfidence() {
		return confidence;
	}

	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public AuthorityValueRest getAuthorityValue() {
		return authorityValue;
	}

	public void setAuthorityValue(AuthorityValueRest authorityValue) {
		this.authorityValue = authorityValue;
	}
	
	public String getLang() {
		return lang;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
}