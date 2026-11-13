/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import org.dspace.services.ConfigurationService;

/**
 * Provider-specific OAuth2 configuration loader.
 *
 * <p>Implementations are responsible for reading their own settings namespace
 * and for validating the parameters required by their token endpoint.</p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface OAuth2Provider {

    /**
     * Return the provider identifier used in {@code mail.oauth2.provider}.
     *
     * @return the provider identifier
     */
    String getId();

    /**
     * Load and validate the provider settings from DSpace configuration.
     *
     * @param configurationService the DSpace configuration service
     * @return the resolved settings
     * @throws OAuth2ConfigurationException when the configuration is missing or invalid
     */
    OAuth2Settings loadSettings(ConfigurationService configurationService);
}
