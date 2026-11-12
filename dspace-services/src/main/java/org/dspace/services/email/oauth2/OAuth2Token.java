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
import java.time.Instant;
import java.util.Objects;

/**
 * An OAuth2 access token together with the optional rotated refresh token and
 * its expiration instant.
 *
 * @param accessToken  the access token used as the XOAUTH2 password
 * @param refreshToken the refresh token returned by the provider, or {@code null} if unchanged
 * @param expiresAt    the instant at which the access token expires
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public record OAuth2Token(String accessToken, String refreshToken, Instant expiresAt) {

    /**
     * Create a token and validate its mandatory fields.
     *
     * @param accessToken  the access token used as the XOAUTH2 password
     * @param refreshToken the refresh token returned by the provider, or {@code null} if unchanged
     * @param expiresAt    the instant at which the access token expires
     */
    public OAuth2Token {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("OAuth2 access token must not be blank");
        }
        Objects.requireNonNull(expiresAt, "OAuth2 token expiration instant must not be null");
    }

    /**
     * Check whether the token is expired or will expire within the given skew.
     *
     * @param clock the clock used to read the current instant
     * @param skew  the safety margin before the actual expiration
     * @return {@code true} when the token should be refreshed
     */
    public boolean isExpired(Clock clock, Duration skew) {
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(skew, "skew");
        return !clock.instant().plus(skew).isBefore(expiresAt);
    }

    @Override
    public String toString() {
        return "OAuth2Token[accessToken=***, refreshToken="
            + (refreshToken == null ? "null" : "***")
            + ", expiresAt=" + expiresAt + ']';
    }
}
