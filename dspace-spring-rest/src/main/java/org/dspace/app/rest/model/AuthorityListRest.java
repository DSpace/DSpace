/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The AuthorityList REST Resource. It represents an authority list that can be
 * used to control the values for specific metadata
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorityListRest extends BaseObjectRest<String> {
	public static final String NAME = "authority";

	private String name;

	private boolean hasVariants;
	
	private List<MetadataFieldRest> linkedMetadata;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isHasVariants() {
		return hasVariants;
	}

	public void setHasVariants(boolean hasVariants) {
		this.hasVariants = hasVariants;
	}

	public List<MetadataFieldRest> getLinkedMetadata() {
		return linkedMetadata;
	}

	public void setLinkedMetadata(List<MetadataFieldRest> linkedMetadata) {
		this.linkedMetadata = linkedMetadata;
	}

	@Override
	public String getType() {
		return NAME;
	}

	@Override
	@JsonIgnore
	public Class getController() {
		return RestResourceController.class;
	}
}