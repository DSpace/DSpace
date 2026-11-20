/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable OAuth2 settings resolved for a specific provider.
 *
 * @param providerId    the provider identifier (for example {@code gmail} or {@code microsoft})
 * @param clientId      the OAuth2 client identifier
 * @param clientSecret  the OAuth2 client secret
 * @param refreshToken  the refresh token used to obtain access tokens
 * @param tenantId      the Microsoft 365 tenant identifier, or {@code null} for providers that do not use it
 * @param scope         the OAuth2 scope requested when refreshing the token
 * @param username      the mailbox address used as the XOAUTH2 username
 * @param tokenEndpoint the absolute token endpoint URI
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public record OAuth2Settings(
    String providerId,
    String clientId,
    String clientSecret,
    String refreshToken,
    String tenantId,
    String scope,
    String username,
    URI tokenEndpoint
) {

    /**
     * Create settings and validate the mandatory connection parameters.
     *
     * @param providerId    the provider identifier
     * @param clientId      the OAuth2 client identifier
     * @param clientSecret  the OAuth2 client secret
     * @param refreshToken  the refresh token used to obtain access tokens
     * @param tenantId      the Microsoft 365 tenant identifier, or {@code null}
     * @param scope         the OAuth2 scope requested when refreshing the token
     * @param username      the mailbox address used as the XOAUTH2 username
     * @param tokenEndpoint the absolute token endpoint URI
     */
    public OAuth2Settings {
        requireText(providerId, "OAuth2 provider id");
        requireText(clientId, "OAuth2 client id");
        requireText(clientSecret, "OAuth2 client secret");
        requireText(refreshToken, "OAuth2 refresh token");
        requireText(username, "OAuth2 username (mail.server.username)");
        Objects.requireNonNull(tokenEndpoint, "OAuth2 token endpoint must not be null");
        if (!tokenEndpoint.isAbsolute() || tokenEndpoint.getHost() == null) {
            throw new OAuth2ConfigurationException(
                "OAuth2 token endpoint must be an absolute URI with a host: " + tokenEndpoint
            );
        }
        String scheme = tokenEndpoint.getScheme();
        if (scheme == null
            || (!"http".equals(scheme.toLowerCase(Locale.ROOT))
                && !"https".equals(scheme.toLowerCase(Locale.ROOT)))) {
            throw new OAuth2ConfigurationException(
                "OAuth2 token endpoint must use http or https: " + tokenEndpoint
            );
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new OAuth2ConfigurationException(label + " must not be blank");
        }
    }

    @Override
    public String toString() {
        return "OAuth2Settings[providerId=" + providerId
            + ", clientId=***, clientSecret=***, refreshToken=***"
            + ", tenantId=" + tenantId
            + ", scope=" + scope
            + ", username=" + username
            + ", tokenEndpoint=" + tokenEndpoint + ']';
    }
}
