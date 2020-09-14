package org.dspace.content.authority;

import org.dspace.services.ConfigurationService;

public class DSpaceZDBConfiguration {

    private ConfigurationService configurationService;

    public String getSearchURL() {
        return configurationService.getProperty("cris.zdb.search.url");
    }

    public String getDetailsURL() {
        return configurationService.getProperty("cris.zdb.detail.url");
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

}
