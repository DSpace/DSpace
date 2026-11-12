/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

/**
 * HTTP implementation of the OAuth2 refresh-token grant.
 *
 * <p>The client is provider-agnostic: it uses the token endpoint and scope
 * resolved by an {@link OAuth2Provider} and does not contain Gmail- or
 * Microsoft-specific code.</p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class HttpOAuth2TokenClient implements OAuth2TokenClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;

    private final HttpClient httpClient;
    private final Clock clock;

    /**
     * Create a client with the default HTTP transport and the system clock.
     */
    public HttpOAuth2TokenClient() {
        this(
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .build(),
            Clock.systemUTC()
        );
    }

    /**
     * Create a client with an explicit transport and clock, primarily for testing.
     *
     * @param httpClient the HTTP client to use
     * @param clock      the clock used to compute the access-token expiration
     */
    HttpOAuth2TokenClient(HttpClient httpClient, Clock clock) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public OAuth2Token requestToken(OAuth2Settings settings, String refreshToken) {
        Objects.requireNonNull(settings, "settings");
        if (StringUtils.isBlank(refreshToken)) {
            throw new OAuth2Exception("OAuth2 refresh token must not be blank");
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", settings.clientId());
        form.put("client_secret", settings.clientSecret());
        form.put("refresh_token", refreshToken);
        if (StringUtils.isNotBlank(settings.scope())) {
            form.put("scope", settings.scope());
        }

        HttpRequest request = HttpRequest.newBuilder(settings.tokenEndpoint())
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OAuth2Exception(
                    errorMessage(settings, response.statusCode(), response.body())
                );
            }
            return parseToken(settings, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuth2Exception(
                "Interrupted while requesting an OAuth2 token for provider '"
                    + settings.providerId() + "'",
                e
            );
        } catch (IOException e) {
            throw new OAuth2Exception(
                "I/O error while requesting an OAuth2 token for provider '"
                    + settings.providerId() + "'",
                e
            );
        }
    }

    private String encode(Map<String, String> form) {
        return form.entrySet().stream()
            .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    }

    private OAuth2Token parseToken(OAuth2Settings settings, String body) {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new OAuth2Exception(
                "Invalid OAuth2 token response for provider '" + settings.providerId() + "'",
                e
            );
        }

        JsonElement accessToken = json.get("access_token");
        if (accessToken == null || !accessToken.isJsonPrimitive()
            || StringUtils.isBlank(accessToken.getAsString())) {
            throw new OAuth2Exception(
                "OAuth2 token response for provider '" + settings.providerId()
                    + "' did not contain an access token"
            );
        }

        String rotatedRefreshToken = null;
        JsonElement refreshToken = json.get("refresh_token");
        if (refreshToken != null && refreshToken.isJsonPrimitive()) {
            rotatedRefreshToken = StringUtils.trimToNull(refreshToken.getAsString());
        }

        long expiresIn = DEFAULT_EXPIRES_IN_SECONDS;
        JsonElement expiresInElement = json.get("expires_in");
        if (expiresInElement != null && !expiresInElement.isJsonNull()) {
            try {
                expiresIn = expiresInElement.getAsLong();
            } catch (RuntimeException ignored) {
                // Fall back to the default lifetime below.
            }
            if (expiresIn <= 0) {
                expiresIn = DEFAULT_EXPIRES_IN_SECONDS;
            }
        }

        Instant expiresAt = clock.instant().plusSeconds(expiresIn);
        return new OAuth2Token(accessToken.getAsString(), rotatedRefreshToken, expiresAt);
    }

    private String errorMessage(OAuth2Settings settings, int status, String body) {
        String error = null;
        try {
            JsonElement errorElement = JsonParser.parseString(body).getAsJsonObject().get("error");
            if (errorElement != null && errorElement.isJsonPrimitive()) {
                error = StringUtils.trimToNull(errorElement.getAsString());
            }
        } catch (RuntimeException ignored) {
            // The response body is not required to be JSON.
        }

        StringBuilder message = new StringBuilder("OAuth2 token request failed for provider '")
            .append(settings.providerId())
            .append("': HTTP ")
            .append(status);
        if (StringUtils.isNotBlank(error)) {
            message.append(" (").append(error).append(')');
        }
        return message.toString();
    }
}
