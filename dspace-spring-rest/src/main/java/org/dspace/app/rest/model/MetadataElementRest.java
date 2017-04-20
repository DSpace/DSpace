/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A Metadata Element to use to embedd metadata in a REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataElementRest implements EmbeddedRestModel {
	
	@JsonIgnore
	private Map<String, MetadataQualifierRest> qualifiers = new HashMap<String, MetadataQualifierRest>();
	
	@JsonUnwrapped
	private MetadataQualifierRest nullQualifier;

	@JsonAnyGetter
	public Map<String, MetadataQualifierRest> getQualifiers() {
		return qualifiers;
	}

	public void setQualifiers(Map<String, MetadataQualifierRest> qualifiers) {
		this.qualifiers = qualifiers;
	}

	public MetadataQualifierRest getNullQualifier() {
		return nullQualifier;
	}

	public void setNullQualifier(MetadataQualifierRest nullQualifier) {
		this.nullQualifier = nullQualifier;
	}
}