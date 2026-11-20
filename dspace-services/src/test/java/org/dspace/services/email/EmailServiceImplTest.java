/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.services.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EmailService;
import org.dspace.services.email.oauth2.OAuth2ConfigurationException;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link EmailServiceImpl}, including its basic-auth and OAuth2 paths.
 *
 * @author mwood
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class EmailServiceImplTest
    extends DSpaceAbstractKernelTest {

    private static final String USERNAME = "auser";
    private static final String PASSWORD = "apassword";

    private static final String CFG_USERNAME = "mail.server.username";
    private static final String CFG_PASSWORD = "mail.server.password";

    private static final String[] OAUTH2_KEYS = {
        "mail.oauth2.provider",
        "mail.oauth2.clientId",
        "mail.oauth2.clientSecret",
        "mail.oauth2.refreshToken",
        "mail.oauth2.tenantId",
        "mail.oauth2.scope",
        "mail.oauth2.tokenEndpoint",
        "mail.oauth2.gmail.clientId",
        "mail.oauth2.gmail.clientSecret",
        "mail.oauth2.gmail.refreshToken",
        "mail.oauth2.gmail.scope",
        "mail.oauth2.gmail.tokenEndpoint",
        "mail.oauth2.microsoft.clientId",
        "mail.oauth2.microsoft.clientSecret",
        "mail.oauth2.microsoft.refreshToken",
        "mail.oauth2.microsoft.tenantId",
        "mail.oauth2.microsoft.scope",
        "mail.oauth2.microsoft.tokenEndpoint"
    };

    private ConfigurationService cfg;
    private String oldUsername;
    private String oldPassword;

    /**
     * Save the mail configuration and start each test from a clean OAuth2 state.
     */
    @Before
    public void saveMailConfiguration() {
        cfg = getKernel().getConfigurationService();
        oldUsername = cfg.getProperty(CFG_USERNAME);
        oldPassword = cfg.getProperty(CFG_PASSWORD);
        for (String key : OAUTH2_KEYS) {
            cfg.setProperty(key, null);
        }
        emailService().reset();
    }

    /**
     * Restore the mail configuration after each test.
     */
    @After
    public void restoreMailConfiguration() {
        if (cfg == null) {
            return;
        }
        for (String key : OAUTH2_KEYS) {
            cfg.setProperty(key, null);
        }
        cfg.setProperty(CFG_USERNAME, oldUsername);
        cfg.setProperty(CFG_PASSWORD, oldPassword);
        emailService().reset();
    }

    /**
     * Test of getSession method, of class EmailService.
     *
     * @throws MessagingException when the session cannot be read
     */
    @Test
    public void testGetSession()
        throws MessagingException {
        System.out.println("getSession");
        Session session;
        EmailService instance = getService(EmailServiceImpl.class);

        // Try to get a Session
        session = instance.getSession();
        assertNotNull(" getSession returned null", session);
        assertNull(" getSession returned authenticated session",
            session.getProperties().getProperty("mail.smtp.auth"));
    }

    /**
     * Test of testGetSession method, of class EmailServiceImpl when an smtp
     * username is provided.
     */
    @Test
    public void testGetAuthenticatedInstance() {
        System.out.println("getSession");

        // Save existing values.
        String oldUsername = cfg.getProperty(CFG_USERNAME);
        String oldPassword = cfg.getProperty(CFG_PASSWORD);

        // Set known values.
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);

        EmailServiceImpl instance = emailService();
        instance.reset();
        assertNotNull(" getSession returned null", instance);
        assertEquals(" authenticated session ", "true",
            instance.getSession().getProperties().getProperty("mail.smtp.auth"));

        // Restore old values, if any.
        cfg.setProperty(CFG_USERNAME, oldUsername);
        cfg.setProperty(CFG_PASSWORD, oldPassword);
        instance.reset();
    }

    /**
     * Test of getPasswordAuthentication method, of class EmailServiceImpl.
     */
    @Test
    public void testGetPasswordAuthentication() {
        System.out.println("getPasswordAuthentication");

        // Save existing values.
        String oldUsername = cfg.getProperty(CFG_USERNAME);
        String oldPassword = cfg.getProperty(CFG_PASSWORD);

        // Set known values.
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);

        EmailServiceImpl instance = emailService();

        PasswordAuthentication result = instance.getPasswordAuthentication();
        assertNotNull(" null returned", result);
        assertEquals(" username does not match configuration", result.getUserName(), USERNAME);
        assertEquals(" password does not match configuration", result.getPassword(), PASSWORD);

        // Restore old values, if any.
        cfg.setProperty(CFG_USERNAME, oldUsername);
        cfg.setProperty(CFG_PASSWORD, oldPassword);
    }

    /**
     * Verify that an OAuth2 session uses XOAUTH2 and does not fetch a token at startup.
     *
     * @throws IOException when the token endpoint stub cannot start
     */
    @Test
    public void testOAuth2SessionProperties() throws IOException {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            configureGmail(server);

            EmailServiceImpl instance = emailService();
            instance.reset();

            assertXoauth2(instance.getSession());
            assertNull(
                "OAuth2 must not write the access token into configuration",
                cfg.getProperty("mail.server.oauth2.token")
            );
            assertEquals("access token must be fetched lazily", 0, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }

    /**
     * Verify that the access token is used as the XOAUTH2 password.
     *
     * @throws Exception when the token endpoint stub or the assertions fail
     */
    @Test
    public void testOAuth2PasswordAuthenticationUsesAccessToken() throws Exception {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            configureGmail(server);
            enqueueToken(server, "access-1", 3600, null);

            EmailServiceImpl instance = emailService();
            instance.reset();

            PasswordAuthentication authentication = instance.getPasswordAuthentication();
            assertEquals(USERNAME, authentication.getUserName());
            assertEquals("access-1", authentication.getPassword());

            RecordedRequest request = server.takeRequest();
            assertEquals("POST", request.getMethod());
            assertEquals("/token", request.getPath());
            assertTrue(request.getHeader("Content-Type")
                .startsWith("application/x-www-form-urlencoded"));
            Map<String, String> form = parseForm(request.getBody().readUtf8());
            assertEquals("refresh_token", form.get("grant_type"));
            assertEquals("client-id", form.get("client_id"));
            assertEquals("client-secret", form.get("client_secret"));
            assertEquals("refresh-1", form.get("refresh_token"));
        } finally {
            server.shutdown();
        }
    }

    /**
     * Verify that an almost-expired token is refreshed and the rotated refresh token is used.
     *
     * @throws Exception when the token endpoint stub or the assertions fail
     */
    @Test
    public void testOAuth2RefreshesBeforeExpiryAndUsesRotatedToken() throws Exception {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            configureGmail(server);
            enqueueToken(server, "access-1", 30, "refresh-2");
            enqueueToken(server, "access-2", 3600, null);

            EmailServiceImpl instance = emailService();
            instance.reset();

            assertEquals("access-1", instance.getPasswordAuthentication().getPassword());
            assertEquals("access-2", instance.getPasswordAuthentication().getPassword());
            assertEquals(2, server.getRequestCount());

            Map<String, String> first = parseForm(server.takeRequest().getBody().readUtf8());
            Map<String, String> second = parseForm(server.takeRequest().getBody().readUtf8());
            assertEquals("refresh-1", first.get("refresh_token"));
            assertEquals("refresh-2", second.get("refresh_token"));
        } finally {
            server.shutdown();
        }
    }

    /**
     * Verify that the legacy basic-auth path is unchanged when no provider is configured,
     * even if OAuth2 keys are present.
     */
    @Test
    public void testLegacyBasicAuthWhenProviderNotConfigured() {
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);
        cfg.setProperty("mail.oauth2.gmail.clientId", "client-id");
        cfg.setProperty("mail.oauth2.gmail.clientSecret", "client-secret");
        cfg.setProperty("mail.oauth2.gmail.refreshToken", "refresh-1");

        EmailServiceImpl instance = emailService();
        instance.reset();

        assertEquals(
            "true",
            instance.getSession().getProperties().getProperty("mail.smtp.auth")
        );
        PasswordAuthentication authentication = instance.getPasswordAuthentication();
        assertEquals(USERNAME, authentication.getUserName());
        assertEquals(PASSWORD, authentication.getPassword());
    }

    /**
     * Verify that an invalid OAuth2 configuration does not fall back to basic auth.
     */
    @Test
    public void testInvalidOAuth2ConfigurationDoesNotFallBack() {
        cfg.setProperty("mail.oauth2.provider", "gmail");
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);
        cfg.setProperty("mail.oauth2.gmail.clientId", "client-id");
        cfg.setProperty("mail.oauth2.gmail.refreshToken", "refresh-1");

        EmailServiceImpl instance = emailService();
        instance.reset();

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            instance::getPasswordAuthentication
        );
        assertTrue(exception.getMessage().contains("clientSecret"));
        assertXoauth2(instance.getSession());
    }

    /**
     * Verify that an unsupported provider does not fall back to basic auth.
     */
    @Test
    public void testUnknownProviderDoesNotFallBack() {
        cfg.setProperty("mail.oauth2.provider", "unknown");
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);

        EmailServiceImpl instance = emailService();
        instance.reset();

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            instance::getPasswordAuthentication
        );
        assertTrue(exception.getMessage().contains("Unsupported OAuth2 provider"));
        assertXoauth2(instance.getSession());
    }

    private EmailServiceImpl emailService() {
        return (EmailServiceImpl) getService(EmailServiceImpl.class);
    }

    private void configureGmail(MockWebServer server) {
        cfg.setProperty("mail.oauth2.provider", "gmail");
        cfg.setProperty(CFG_USERNAME, USERNAME);
        cfg.setProperty(CFG_PASSWORD, PASSWORD);
        cfg.setProperty("mail.oauth2.gmail.clientId", "client-id");
        cfg.setProperty("mail.oauth2.gmail.clientSecret", "client-secret");
        cfg.setProperty("mail.oauth2.gmail.refreshToken", "refresh-1");
        cfg.setProperty("mail.oauth2.gmail.tokenEndpoint", server.url("/token").toString());
    }

    private void enqueueToken(MockWebServer server, String accessToken, long expiresIn,
                              String refreshToken) {
        StringBuilder body = new StringBuilder("{\"access_token\":\"")
            .append(accessToken)
            .append("\",\"expires_in\":")
            .append(expiresIn);
        if (refreshToken != null) {
            body.append(",\"refresh_token\":\"").append(refreshToken).append('"');
        }
        body.append('}');
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body.toString()));
    }

    private void assertXoauth2(Session session) {
        assertEquals("true", session.getProperties().getProperty("mail.smtp.auth"));
        assertEquals("XOAUTH2", session.getProperties().getProperty("mail.smtp.auth.mechanisms"));
        assertEquals("true", session.getProperties().getProperty("mail.smtp.auth.login.disable"));
        assertEquals("true", session.getProperties().getProperty("mail.smtp.auth.plain.disable"));
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
