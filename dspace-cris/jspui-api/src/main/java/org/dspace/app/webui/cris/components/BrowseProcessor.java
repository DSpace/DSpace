/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.discovery.configuration.DiscoveryConfiguration;

public class BrowseProcessor implements ExploreProcessor {
	private List<String> browseNames;
	
	public void setBrowseNames(List<String> browseNames) {
		this.browseNames = browseNames;
	}

	public List<String> getBrowseNames() {
		return browseNames;
	}
	
	@Override
	public Map<String, Object> process(String configurationName, DiscoveryConfiguration discoveryConfiguration,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("browseNames", browseNames);
		return model;
	}

}
