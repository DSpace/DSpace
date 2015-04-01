/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.Map;

public class DiscoveryViewAndHighlightConfiguration {
		
	private String selector = "discovery-list-artifact";
	
	private Map<String, DiscoveryViewConfiguration> viewConfiguration;

	public Map<String, DiscoveryViewConfiguration> getViewConfiguration() {
		return viewConfiguration;
	}

	public void setViewConfiguration(Map<String, DiscoveryViewConfiguration> viewConfiguration) {
		this.viewConfiguration = viewConfiguration;
	}

	public String getSelector() {
		return selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}
	
}
