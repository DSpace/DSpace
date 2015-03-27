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
