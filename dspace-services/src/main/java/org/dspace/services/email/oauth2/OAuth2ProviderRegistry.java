/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;

/**
 * Registry of the OAuth2 providers supported by the SMTP email service.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public final class OAuth2ProviderRegistry {

    private static final String PROVIDER_PROPERTY = "mail.oauth2.provider";

    private static final Map<String, OAuth2Provider> PROVIDERS = createProviders();

    private OAuth2ProviderRegistry() {
        // Utility class.
    }

    /**
     * Resolve a provider by its identifier.
     *
     * @param providerId the value of {@code mail.oauth2.provider}
     * @return the matching provider
     * @throws OAuth2ConfigurationException when the identifier is blank or unsupported
     */
    public static OAuth2Provider getProvider(String providerId) {
        String normalized = StringUtils.trimToNull(providerId);
        if (normalized == null) {
            throw new OAuth2ConfigurationException(
                "Missing required OAuth2 setting: " + PROVIDER_PROPERTY
            );
        }
        OAuth2Provider provider = PROVIDERS.get(normalized.toLowerCase(Locale.ROOT));
        if (provider == null) {
            throw new OAuth2ConfigurationException(
                "Unsupported OAuth2 provider '" + providerId + "'. Supported providers: "
                    + String.join(", ", PROVIDERS.keySet())
            );
        }
        return provider;
    }

    /**
     * Check whether an OAuth2 provider is selected in the configuration.
     *
     * @param configurationService the DSpace configuration service
     * @return {@code true} when {@code mail.oauth2.provider} is set
     */
    public static boolean isConfigured(ConfigurationService configurationService) {
        return StringUtils.isNotBlank(configurationService.getProperty(PROVIDER_PROPERTY));
    }

    private static Map<String, OAuth2Provider> createProviders() {
        Map<String, OAuth2Provider> providers = new LinkedHashMap<>();
        register(providers, new GmailOAuth2Provider());
        register(providers, new MicrosoftOAuth2Provider());
        return Collections.unmodifiableMap(providers);
    }

    private static void register(Map<String, OAuth2Provider> providers, OAuth2Provider provider) {
        providers.put(provider.getId(), provider);
    }
}
