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

	public String getORCIDClientID()
	{
		return configurationService.getProperty("authentication-oauth.application-client-id");
	}
	
	
	public String getORCIDClientSecretKey()
	{
		return configurationService.getProperty("authentication-oauth.application-client-secret");
	}
	
	
	public String getORCIDTokenURL()
	{
		return configurationService.getProperty("authentication-oauth.application-token-url");
	}

	public String getORCIDBaseURL()
	{
		return configurationService.getProperty("cris.external.domainname.authority.service.orcid");
	}
	
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
}
