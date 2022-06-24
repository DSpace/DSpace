/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.client;

import org.apache.commons.lang3.StringUtils;

/**
 * A class that contains all the configurations related to ORCID.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OrcidConfiguration {

    private String apiUrl;

    private String publicUrl;

    private String domainUrl;

    private String redirectUrl;

    private String clientId;

    private String clientSecret;

    private String tokenEndpointUrl;

    private String authorizeEndpointUrl;

    private String scopes;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getDomainUrl() {
        return domainUrl;
    }

    public void setDomainUrl(String domainUrl) {
        this.domainUrl = domainUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    public void setTokenEndpointUrl(String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
    }

    public String getAuthorizeEndpointUrl() {
        return authorizeEndpointUrl;
    }

    public void setAuthorizeEndpointUrl(String authorizeEndpointUrl) {
        this.authorizeEndpointUrl = authorizeEndpointUrl;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String[] getScopes() {
        return StringUtils.isNotBlank(scopes) ? StringUtils.split(scopes, ",") : new String[] {};
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public boolean isApiConfigured() {
        return !StringUtils.isAnyBlank(clientId, clientSecret);
    }

}
