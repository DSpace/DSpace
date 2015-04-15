/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import org.dspace.services.ConfigurationService;

public class DSpaceORCIDConfiguration {
	
	private ConfigurationService configurationService;

	public String getORCIDApi()
	{
		return configurationService.getProperty("authentication-oauth.orcid-api-url");
	}
	
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
}
