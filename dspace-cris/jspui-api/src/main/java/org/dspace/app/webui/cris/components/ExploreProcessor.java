package org.dspace.app.webui.cris.components;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.discovery.configuration.DiscoveryConfiguration;

public interface ExploreProcessor {
	public Map<String, Object> process(String configurationName, DiscoveryConfiguration discoveryConfiguration,
			HttpServletRequest request, HttpServletResponse response) throws Exception;
}
