/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.ArrayList;
import java.util.List;

import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * @author kstamatis
 *
 */
public class TagCloudFacetConfiguration {

	TagCloudConfiguration tagCloudConfiguration;
	
	/** The configuration for the tagcloud facets **/
    private List<DiscoverySearchFilterFacet> tagCloudFacets = new ArrayList<DiscoverySearchFilterFacet>();
	
	/**
	 * 
	 */
	public TagCloudFacetConfiguration() {
		// TODO Auto-generated constructor stub
	}

	public TagCloudConfiguration getTagCloudConfiguration() {
		return tagCloudConfiguration;
	}

	public void setTagCloudConfiguration(
			TagCloudConfiguration tagCloudConfiguration) {
		this.tagCloudConfiguration = tagCloudConfiguration;
	}

	public List<DiscoverySearchFilterFacet> getTagCloudFacets() {
		return tagCloudFacets;
	}

	public void setTagCloudFacets(List<DiscoverySearchFilterFacet> tagCloudFacets) {
		this.tagCloudFacets = tagCloudFacets;
	}
}
