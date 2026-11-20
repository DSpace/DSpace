/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;

/**
 * Offline tests for the OAuth2 providers and their configuration resolution.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class OAuth2ProviderTest {

    private ConfigurationService configurationService;

    /**
     * Create a configuration mock with a mailbox username.
     */
    @Before
    public void setUp() {
        configurationService = mock(ConfigurationService.class);
        when(configurationService.getProperty("mail.server.username"))
            .thenReturn("user@example.com");
    }

    /**
     * Verify the Gmail defaults.
     */
    @Test
    public void gmailUsesProviderDefaults() {
        set("mail.oauth2.gmail.clientId", "client-id");
        set("mail.oauth2.gmail.clientSecret", "client-secret");
        set("mail.oauth2.gmail.refreshToken", "refresh-token");

        OAuth2Settings settings = new GmailOAuth2Provider().loadSettings(configurationService);

        assertEquals("gmail", settings.providerId());
        assertEquals("https://oauth2.googleapis.com/token", settings.tokenEndpoint().toString());
        assertEquals("https://mail.google.com/", settings.scope());
        assertEquals("user@example.com", settings.username());
        assertNull(settings.tenantId());
    }

    /**
     * Verify that provider-specific keys override the generic keys.
     */
    @Test
    public void providerSpecificSettingsOverrideGenericSettings() {
        set("mail.oauth2.clientId", "generic-id");
        set("mail.oauth2.clientSecret", "generic-secret");
        set("mail.oauth2.refreshToken", "generic-refresh");
        set("mail.oauth2.gmail.clientId", "gmail-id");
        set("mail.oauth2.gmail.clientSecret", "gmail-secret");
        set("mail.oauth2.gmail.refreshToken", "gmail-refresh");

        OAuth2Settings settings = new GmailOAuth2Provider().loadSettings(configurationService);

        assertEquals("gmail-id", settings.clientId());
        assertEquals("gmail-secret", settings.clientSecret());
        assertEquals("gmail-refresh", settings.refreshToken());
    }

    /**
     * Verify that the generic keys remain a fallback for existing configurations.
     */
    @Test
    public void genericSettingsRemainSupported() {
        set("mail.oauth2.clientId", "generic-id");
        set("mail.oauth2.clientSecret", "generic-secret");
        set("mail.oauth2.refreshToken", "generic-refresh");

        OAuth2Settings settings = new GmailOAuth2Provider().loadSettings(configurationService);

        assertEquals("generic-id", settings.clientId());
        assertEquals("generic-secret", settings.clientSecret());
        assertEquals("generic-refresh", settings.refreshToken());
    }

    /**
     * Verify that a missing client secret is reported with the expected key.
     */
    @Test
    public void missingClientSecretIsReported() {
        set("mail.oauth2.gmail.clientId", "client-id");
        set("mail.oauth2.gmail.refreshToken", "refresh-token");

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> new GmailOAuth2Provider().loadSettings(configurationService)
        );

        assertTrue(exception.getMessage().contains("mail.oauth2.gmail.clientSecret"));
    }

    /**
     * Verify that the XOAUTH2 mailbox username is required.
     */
    @Test
    public void missingUsernameIsReported() {
        when(configurationService.getProperty("mail.server.username")).thenReturn(null);
        set("mail.oauth2.gmail.clientId", "client-id");
        set("mail.oauth2.gmail.clientSecret", "client-secret");
        set("mail.oauth2.gmail.refreshToken", "refresh-token");

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> new GmailOAuth2Provider().loadSettings(configurationService)
        );

        assertTrue(exception.getMessage().contains("mail.server.username"));
    }

    /**
     * Verify the Microsoft 365 tenant-specific endpoint and default scope.
     */
    @Test
    public void microsoftUsesTenantEndpointAndScope() {
        set("mail.oauth2.microsoft.clientId", "client-id");
        set("mail.oauth2.microsoft.clientSecret", "client-secret");
        set("mail.oauth2.microsoft.refreshToken", "refresh-token");
        set("mail.oauth2.microsoft.tenantId", "contoso.onmicrosoft.com");

        OAuth2Settings settings = new MicrosoftOAuth2Provider().loadSettings(configurationService);

        assertEquals("microsoft", settings.providerId());
        assertEquals(
            "https://login.microsoftonline.com/contoso.onmicrosoft.com/oauth2/v2.0/token",
            settings.tokenEndpoint().toString()
        );
        assertEquals(
            "https://outlook.office.com/SMTP.Send offline_access",
            settings.scope()
        );
        assertEquals("contoso.onmicrosoft.com", settings.tenantId());
    }

    /**
     * Verify that the Microsoft tenant can come from the generic key.
     */
    @Test
    public void microsoftTenantFallsBackToGenericKey() {
        set("mail.oauth2.microsoft.clientId", "client-id");
        set("mail.oauth2.microsoft.clientSecret", "client-secret");
        set("mail.oauth2.microsoft.refreshToken", "refresh-token");
        set("mail.oauth2.tenantId", "contoso.onmicrosoft.com");

        OAuth2Settings settings = new MicrosoftOAuth2Provider().loadSettings(configurationService);

        assertEquals("contoso.onmicrosoft.com", settings.tenantId());
    }

    /**
     * Verify that the Microsoft tenant is mandatory.
     */
    @Test
    public void microsoftTenantIsRequired() {
        set("mail.oauth2.microsoft.clientId", "client-id");
        set("mail.oauth2.microsoft.clientSecret", "client-secret");
        set("mail.oauth2.microsoft.refreshToken", "refresh-token");

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> new MicrosoftOAuth2Provider().loadSettings(configurationService)
        );

        assertTrue(exception.getMessage().contains("mail.oauth2.microsoft.tenantId"));
    }

    /**
     * Verify that unsafe tenant values are rejected.
     */
    @Test
    public void microsoftTenantIsValidated() {
        set("mail.oauth2.microsoft.clientId", "client-id");
        set("mail.oauth2.microsoft.clientSecret", "client-secret");
        set("mail.oauth2.microsoft.refreshToken", "refresh-token");
        set("mail.oauth2.microsoft.tenantId", "contoso/evil");

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> new MicrosoftOAuth2Provider().loadSettings(configurationService)
        );

        assertTrue(exception.getMessage().contains("Invalid OAuth2 tenant id"));
    }

    /**
     * Verify that the token endpoint can be overridden, including for tests and sovereign clouds.
     */
    @Test
    public void tokenEndpointCanBeOverridden() {
        set("mail.oauth2.gmail.clientId", "client-id");
        set("mail.oauth2.gmail.clientSecret", "client-secret");
        set("mail.oauth2.gmail.refreshToken", "refresh-token");
        set("mail.oauth2.gmail.tokenEndpoint", "http://localhost:8080/token");

        OAuth2Settings settings = new GmailOAuth2Provider().loadSettings(configurationService);

        assertEquals("http://localhost:8080/token", settings.tokenEndpoint().toString());
    }

    /**
     * Verify that an invalid token endpoint is reported as a configuration error.
     */
    @Test
    public void invalidTokenEndpointIsReported() {
        set("mail.oauth2.gmail.clientId", "client-id");
        set("mail.oauth2.gmail.clientSecret", "client-secret");
        set("mail.oauth2.gmail.refreshToken", "refresh-token");
        set("mail.oauth2.gmail.tokenEndpoint", "not a uri");

        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> new GmailOAuth2Provider().loadSettings(configurationService)
        );

        assertTrue(exception.getMessage().contains("Invalid OAuth2 configuration"));
    }

    /**
     * Verify that providers are resolved case-insensitively.
     */
    @Test
    public void registryResolvesProvidersCaseInsensitively() {
        assertTrue(OAuth2ProviderRegistry.getProvider("GMAIL") instanceof GmailOAuth2Provider);
        assertTrue(
            OAuth2ProviderRegistry.getProvider("Microsoft") instanceof MicrosoftOAuth2Provider
        );
    }

    /**
     * Verify that an unsupported provider is rejected with the supported list.
     */
    @Test
    public void registryRejectsUnknownProvider() {
        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> OAuth2ProviderRegistry.getProvider("unknown")
        );

        assertTrue(exception.getMessage().contains("Supported providers: gmail, microsoft"));
    }

    /**
     * Verify that a blank provider is rejected.
     */
    @Test
    public void registryRejectsBlankProvider() {
        OAuth2ConfigurationException exception = assertThrows(
            OAuth2ConfigurationException.class,
            () -> OAuth2ProviderRegistry.getProvider(" ")
        );

        assertTrue(exception.getMessage().contains("mail.oauth2.provider"));
    }

    /**
     * Verify the configured/not-configured switch.
     */
    @Test
    public void registryDetectsConfiguration() {
        when(configurationService.getProperty("mail.oauth2.provider")).thenReturn("gmail");
        assertTrue(OAuth2ProviderRegistry.isConfigured(configurationService));

        when(configurationService.getProperty("mail.oauth2.provider")).thenReturn(" ");
        assertTrue(!OAuth2ProviderRegistry.isConfigured(configurationService));
    }

    private void set(String key, String value) {
        when(configurationService.getProperty(key)).thenReturn(value);
    }
}
