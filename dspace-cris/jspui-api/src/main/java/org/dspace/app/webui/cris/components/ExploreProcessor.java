/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.discovery.configuration.DiscoveryConfiguration;

public interface ExploreProcessor {
	public Map<String, Object> process(String configurationName, DiscoveryConfiguration discoveryConfiguration,
			HttpServletRequest request, HttpServletResponse response) throws Exception;
}
