/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.dspace.services.ConfigurationService;
import org.junit.Test;

/**
 * Unit tests for {@link GoogleAnalytics4ClientRequestBuilder}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalytics4ClientRequestBuilderTest {

    private GoogleAnalytics4ClientRequestBuilder requestBuilder;

    private ConfigurationService configurationService = mock(ConfigurationService.class);

    @Test
    public void testGetEndpointUrl() {

        when(configurationService.getProperty("google.analytics.api-secret")).thenReturn("abc123");

        requestBuilder = new GoogleAnalytics4ClientRequestBuilder("https://google-analytics/test");
        requestBuilder.setConfigurationService(configurationService);

        String endpointUrl = requestBuilder.getEndpointUrl("G-12345");
        assertThat(endpointUrl, is("https://google-analytics/test?api_secret=abc123&measurement_id=G-12345"));

    }

    @Test
    public void testGetEndpointUrlWithoutApiSecretConfigured() {

        requestBuilder = new GoogleAnalytics4ClientRequestBuilder("https://google-analytics/test");
        requestBuilder.setConfigurationService(configurationService);

        assertThrows("The API secret must be configured to sent GA4 events",
            GoogleAnalyticsClientException.class, () -> requestBuilder.getEndpointUrl("G-12345"));

    }

    @Test
    public void testComposeRequestBodies() {

    }
}
