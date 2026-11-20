/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;

/**
 * Base class for providers that share the {@code mail.oauth2.<provider>.*}
 * configuration namespace and its generic {@code mail.oauth2.*} fallback.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public abstract class AbstractOAuth2Provider implements OAuth2Provider {

    private final String id;
    private final String defaultScope;

    /**
     * Create a provider.
     *
     * @param id           the provider identifier
     * @param defaultScope the scope used when the configuration does not override it
     */
    protected AbstractOAuth2Provider(String id, String defaultScope) {
        this.id = Objects.requireNonNull(id, "id");
        this.defaultScope = defaultScope;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public OAuth2Settings loadSettings(ConfigurationService configurationService) {
        Objects.requireNonNull(configurationService, "configurationService");

        String clientId = getSetting(configurationService, "clientId", null);
        String clientSecret = getSetting(configurationService, "clientSecret", null);
        String refreshToken = getSetting(configurationService, "refreshToken", null);
        String tenantId = getSetting(configurationService, "tenantId", null);
        String scope = getSetting(configurationService, "scope", defaultScope);
        String username = StringUtils.trimToNull(configurationService.getProperty("mail.server.username"));

        requireSetting(clientId, "clientId");
        requireSetting(clientSecret, "clientSecret");
        requireSetting(refreshToken, "refreshToken");
        if (StringUtils.isBlank(username)) {
            throw new OAuth2ConfigurationException(
                "Missing required OAuth2 setting: mail.server.username"
            );
        }

        validateTenant(tenantId);

        String tokenEndpointValue = getSetting(configurationService, "tokenEndpoint", null);
        String endpoint = StringUtils.isNotBlank(tokenEndpointValue)
            ? tokenEndpointValue
            : resolveTokenEndpoint(tenantId);

        try {
            return new OAuth2Settings(
                id,
                clientId,
                clientSecret,
                refreshToken,
                tenantId,
                scope,
                username,
                URI.create(endpoint)
            );
        } catch (IllegalArgumentException | OAuth2ConfigurationException e) {
            throw new OAuth2ConfigurationException(
                "Invalid OAuth2 configuration for provider '" + id + "': " + e.getMessage(), e
            );
        }
    }

    /**
     * Validate provider-specific tenant settings.
     *
     * @param tenantId the configured tenant identifier, possibly {@code null}
     * @throws OAuth2ConfigurationException when the tenant is required but invalid
     */
    protected void validateTenant(String tenantId) {
        // Providers that do not use a tenant identifier accept a null value.
    }

    /**
     * Resolve the default token endpoint for the provider.
     *
     * @param tenantId the configured tenant identifier, possibly {@code null}
     * @return the token endpoint URI as a string
     */
    protected abstract String resolveTokenEndpoint(String tenantId);

    private void requireSetting(String value, String key) {
        if (StringUtils.isBlank(value)) {
            throw new OAuth2ConfigurationException(
                "Missing required OAuth2 setting: " + providerProperty(key)
                    + " (or mail.oauth2." + key + ")"
            );
        }
    }

    private String providerProperty(String key) {
        return "mail.oauth2." + id + "." + key;
    }

    private String getSetting(ConfigurationService configurationService, String key, String defaultValue) {
        String value = configurationService.getProperty(providerProperty(key));
        if (StringUtils.isBlank(value)) {
            value = configurationService.getProperty("mail.oauth2." + key);
        }
        if (StringUtils.isBlank(value)) {
            value = getEnvironmentValue(key);
        }
        if (StringUtils.isBlank(value)) {
            value = defaultValue;
        }
        return StringUtils.trimToNull(value);
    }

    private String getEnvironmentValue(String key) {
        switch (key) {
            case "clientId":
                return System.getenv("OAUTH2_CLIENT_ID");
            case "clientSecret":
                return System.getenv("OAUTH2_CLIENT_SECRET");
            case "refreshToken":
                return StringUtils.defaultIfBlank(
                    System.getenv("OAUTH2_REFRESH_TOKEN"),
                    System.getenv("OAUTH2_TOKEN")
                );
            case "tenantId":
                return System.getenv("OAUTH2_TENANT_ID");
            case "scope":
                return System.getenv("OAUTH2_SCOPE");
            case "tokenEndpoint":
                return System.getenv("OAUTH2_TOKEN_ENDPOINT");
            default:
                return null;
        }
    }
}
