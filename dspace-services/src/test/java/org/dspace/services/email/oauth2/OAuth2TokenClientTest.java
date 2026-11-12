/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Offline tests for {@link HttpOAuth2TokenClient}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class OAuth2TokenClientTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private MockWebServer server;
    private HttpOAuth2TokenClient client;
    private OAuth2Settings settings;

    /**
     * Start the token endpoint stub.
     *
     * @throws IOException when the stub cannot start
     */
    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new HttpOAuth2TokenClient(
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
            CLOCK
        );
        settings = new OAuth2Settings(
            "gmail",
            "client-id",
            "client-secret",
            "refresh-1",
            null,
            "scope:one",
            "user@example.com",
            server.url("/token").uri()
        );
    }

    /**
     * Stop the token endpoint stub.
     *
     * @throws IOException when the stub cannot stop
     */
    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    /**
     * Verify the refresh-token form and the parsed token response.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void requestsTokenAndParsesResponse() throws Exception {
        enqueue(200, "{\"access_token\":\"access-1\",\"expires_in\":3600,"
            + "\"refresh_token\":\"refresh-2\"}");

        OAuth2Token token = client.requestToken(settings, "refresh-1");

        assertEquals("access-1", token.accessToken());
        assertEquals("refresh-2", token.refreshToken());
        assertEquals(NOW.plusSeconds(3600), token.expiresAt());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/token", request.getPath());
        assertTrue(request.getHeader("Content-Type")
            .startsWith("application/x-www-form-urlencoded"));
        assertEquals("application/json", request.getHeader("Accept"));

        Map<String, String> form = parseForm(request.getBody().readUtf8());
        assertEquals("refresh_token", form.get("grant_type"));
        assertEquals("client-id", form.get("client_id"));
        assertEquals("client-secret", form.get("client_secret"));
        assertEquals("refresh-1", form.get("refresh_token"));
        assertEquals("scope:one", form.get("scope"));
    }

    /**
     * Verify the default lifetime when {@code expires_in} is missing.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void usesDefaultLifetimeWhenExpiresInIsMissing() throws Exception {
        enqueue(200, "{\"access_token\":\"access-1\"}");

        OAuth2Token token = client.requestToken(settings, "refresh-1");

        assertNull(token.refreshToken());
        assertEquals(NOW.plusSeconds(3600), token.expiresAt());
    }

    /**
     * Verify the default lifetime when {@code expires_in} is not a positive number.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void usesDefaultLifetimeWhenExpiresInIsInvalid() throws Exception {
        enqueue(200, "{\"access_token\":\"access-1\",\"expires_in\":\"not-a-number\"}");

        OAuth2Token token = client.requestToken(settings, "refresh-1");

        assertEquals(NOW.plusSeconds(3600), token.expiresAt());
    }

    /**
     * Verify that provider errors do not leak credentials.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void reportsProviderErrorWithoutLeakingSecrets() throws Exception {
        enqueue(400, "{\"error\":\"invalid_grant\",\"error_description\":\"bad refresh token\"}");

        OAuth2Exception exception = assertThrows(
            OAuth2Exception.class,
            () -> client.requestToken(settings, "refresh-1")
        );

        assertTrue(exception.getMessage().contains("HTTP 400"));
        assertTrue(exception.getMessage().contains("invalid_grant"));
        assertFalse(exception.getMessage().contains("client-secret"));
        assertFalse(exception.getMessage().contains("refresh-1"));
        assertFalse(exception.getMessage().contains("bad refresh token"));
    }

    /**
     * Verify that non-JSON error responses are reported without their body.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void reportsNonJsonErrorResponse() throws Exception {
        enqueue(500, "upstream failure with client-secret");

        OAuth2Exception exception = assertThrows(
            OAuth2Exception.class,
            () -> client.requestToken(settings, "refresh-1")
        );

        assertTrue(exception.getMessage().contains("HTTP 500"));
        assertFalse(exception.getMessage().contains("client-secret"));
    }

    /**
     * Verify that malformed success responses are rejected.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void rejectsMalformedSuccessResponse() throws Exception {
        enqueue(200, "not-json");

        OAuth2Exception exception = assertThrows(
            OAuth2Exception.class,
            () -> client.requestToken(settings, "refresh-1")
        );

        assertTrue(exception.getMessage().contains("Invalid OAuth2 token response"));
    }

    /**
     * Verify that a success response without an access token is rejected.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void rejectsSuccessResponseWithoutAccessToken() throws Exception {
        enqueue(200, "{\"expires_in\":3600}");

        OAuth2Exception exception = assertThrows(
            OAuth2Exception.class,
            () -> client.requestToken(settings, "refresh-1")
        );

        assertTrue(exception.getMessage().contains("did not contain an access token"));
    }

    /**
     * Verify that a non-string access token is rejected.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void rejectsNonStringAccessToken() throws Exception {
        enqueue(200, "{\"access_token\":{\"value\":\"access-1\"}}");

        OAuth2Exception exception = assertThrows(
            OAuth2Exception.class,
            () -> client.requestToken(settings, "refresh-1")
        );

        assertTrue(exception.getMessage().contains("did not contain an access token"));
    }

    /**
     * Verify that a blank refresh token is rejected before any HTTP call.
     */
    @Test
    public void rejectsBlankRefreshToken() {
        OAuth2Exception exception = assertThrows(
            OAuth2Exception.class,
            () -> client.requestToken(settings, " ")
        );

        assertTrue(exception.getMessage().contains("must not be blank"));
        assertEquals(0, server.getRequestCount());
    }

    /**
     * Verify that the scope form field is omitted when the provider does not set one.
     *
     * @throws Exception when the request or the assertions fail
     */
    @Test
    public void omitsScopeWhenBlank() throws Exception {
        settings = new OAuth2Settings(
            "gmail",
            "client-id",
            "client-secret",
            "refresh-1",
            null,
            null,
            "user@example.com",
            server.url("/token").uri()
        );
        enqueue(200, "{\"access_token\":\"access-1\"}");

        client.requestToken(settings, "refresh-1");

        Map<String, String> form = parseForm(server.takeRequest().getBody().readUtf8());
        assertFalse(form.containsKey("scope"));
    }

    private void enqueue(int status, String body) {
        server.enqueue(new MockResponse()
            .setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body));
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            int separator = pair.indexOf('=');
            form.put(
                URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8),
                URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8)
            );
        }
        return form;
    }
}
