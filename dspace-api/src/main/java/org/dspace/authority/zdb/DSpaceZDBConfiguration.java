/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.zdb;

import org.dspace.services.ConfigurationService;

public class DSpaceZDBConfiguration {

	private ConfigurationService configurationService;

	public String getSearchURL()
	{
		return configurationService.getProperty("cris.zdb.search.url");
	}
	
	public String getDetailsURL()
	{
		return configurationService.getProperty("cris.zdb.detail.url");
	}
	
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
}
