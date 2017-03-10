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

/**
 * A Metadata Schema wrapper to use to embedd metadata in a REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataSchemaEmbeddedRest implements EmbeddedRestModel {
	@JsonIgnore
	private MetadataSchemaRest metadataSchema;

	private Map<String, MetadataElementRest> elements = new HashMap<String, MetadataElementRest>();

	public MetadataSchemaRest getMetadataSchema() {
		return metadataSchema;
	}

	public void setMetadataSchema(MetadataSchemaRest metadataSchema) {
		this.metadataSchema = metadataSchema;
	}

	@JsonAnyGetter
	public Map<String, MetadataElementRest> getElements() {
		return elements;
	}

	public void setElements(Map<String, MetadataElementRest> elements) {
		this.elements = elements;
	}
}