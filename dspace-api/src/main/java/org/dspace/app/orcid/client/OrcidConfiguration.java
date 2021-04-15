/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.client;

import org.dspace.services.ConfigurationService;

/**
 * A class that contains all the configurations related to ORCID.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OrcidConfiguration {

    private final ConfigurationService configurationService;

    public OrcidConfiguration(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public String getApiUrl() {
        return configurationService.getProperty("orcid-api.api-url");
    }

    public String getDomainUrl() {
        return configurationService.getProperty("orcid.domain-url");
    }

    public String getRedirectUri() {
        return configurationService.getProperty("dspace.server.url") + "/api/authn/orcid";
    }

    public String getClientId() {
        return configurationService.getProperty("orcid-api.application-client-id");
    }

    public String getClientSecret() {
        return configurationService.getProperty("orcid-api.application-client-secret");
    }

    public String getTokenEndpointUrl() {
        return configurationService.getProperty("orcid-api.token-url");
    }

    public String getAuthorizeEndpointUrl() {
        return configurationService.getProperty("orcid-api.authorize-url");
    }

    public String[] getScopes() {
        return configurationService.getArrayProperty("orcid-api.scope");
    }

}
