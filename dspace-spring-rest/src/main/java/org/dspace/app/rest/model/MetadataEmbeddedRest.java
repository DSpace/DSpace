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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * A class to embedd metadata in the object resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataEmbeddedRest implements EmbeddedRestModel {
	@JsonProperty(access=Access.WRITE_ONLY)
	private Map<String, MetadataSchemaEmbeddedRest> metadata = new HashMap<String, MetadataSchemaEmbeddedRest>();

	public Map<String, MetadataSchemaEmbeddedRest> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, MetadataSchemaEmbeddedRest> metadata) {
		this.metadata = metadata;
	}
}