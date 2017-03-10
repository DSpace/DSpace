/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A Metadata Qualifier to use in the embedded Metadata REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataQualifierRest implements EmbeddedRestModel {
	@JsonIgnore
	private MetadataFieldRest metadataField;
	
	private List<MetadataValueRest> values = new ArrayList<MetadataValueRest>();

	public MetadataFieldRest getMetadataField() {
		return metadataField;
	}

	public void setMetadataField(MetadataFieldRest metadataField) {
		this.metadataField = metadataField;
	}

	public List<MetadataValueRest> getValues() {
		return values;
	}

	public void setValues(List<MetadataValueRest> values) {
		this.values = values;
	}
}