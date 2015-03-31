/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.List;

public class DiscoveryViewConfiguration {

	private String thumbnail;
	
	private List<DiscoveryViewFieldConfiguration> metadataHeadingFields;
	private List<DiscoveryViewFieldConfiguration> metadataDescriptionFields;
	
	public List<DiscoveryViewFieldConfiguration> getMetadataDescriptionFields() {
		return metadataDescriptionFields;
	}
	
	public void setMetadataDescriptionFields(
			List<DiscoveryViewFieldConfiguration> metadataDescriptionFields) {
		this.metadataDescriptionFields = metadataDescriptionFields;
	}
	
	public List<DiscoveryViewFieldConfiguration> getMetadataHeadingFields() {
		return metadataHeadingFields;
	}

	public void setMetadataHeadingFields(List<DiscoveryViewFieldConfiguration> metadataFields) {
		this.metadataHeadingFields = metadataFields;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
}
