/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Caches an OAuth2 access token and refreshes it before it expires.
 *
 * <p>Refreshes are serialized, so concurrent mail-sending threads share a
 * single token request. The rotated refresh token returned by the provider is
 * kept in memory; it is not persisted across restarts.</p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class OAuth2TokenManager {

    private static final Logger logger = LogManager.getLogger();
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(60);

    private final OAuth2TokenClient tokenClient;
    private final OAuth2Settings settings;
    private final Clock clock;
    private final Object lock = new Object();

    private volatile OAuth2Token token;
    private String refreshToken;

    /**
     * Create a manager using the system clock.
     *
     * @param tokenClient the client used to refresh the token
     * @param settings    the provider settings
     */
    public OAuth2TokenManager(OAuth2TokenClient tokenClient, OAuth2Settings settings) {
        this(tokenClient, settings, Clock.systemUTC());
    }

    /**
     * Create a manager with an explicit clock, primarily for testing.
     *
     * @param tokenClient the client used to refresh the token
     * @param settings    the provider settings
     * @param clock       the clock used to check token expiration
     */
    OAuth2TokenManager(OAuth2TokenClient tokenClient, OAuth2Settings settings, Clock clock) {
        this.tokenClient = Objects.requireNonNull(tokenClient, "tokenClient");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.refreshToken = settings.refreshToken();
    }

    /**
     * Return a valid access token, refreshing it when necessary.
     *
     * @return the access token to use as the XOAUTH2 password
     * @throws OAuth2Exception when the provider cannot issue a new token
     */
    public String getAccessToken() {
        OAuth2Token current = token;
        if (current != null && !current.isExpired(clock, EXPIRY_SKEW)) {
            return current.accessToken();
        }

        synchronized (lock) {
            current = token;
            if (current != null && !current.isExpired(clock, EXPIRY_SKEW)) {
                return current.accessToken();
            }

            OAuth2Token refreshed = tokenClient.requestToken(settings, refreshToken);
            String rotatedRefreshToken = refreshed.refreshToken();
            if (StringUtils.isNotBlank(rotatedRefreshToken)
                && !rotatedRefreshToken.equals(refreshToken)) {
                logger.warn(
                    "OAuth2 provider '{}' returned a new refresh token. "
                        + "The new token is kept in memory only; update the configured refresh token "
                        + "if the server is restarted.",
                    settings.providerId()
                );
                refreshToken = rotatedRefreshToken;
            }

            token = refreshed;
            logger.debug(
                "Obtained a new OAuth2 access token for provider '{}' (expires at {})",
                settings.providerId(),
                refreshed.expiresAt()
            );
            return refreshed.accessToken();
        }
    }
}
