/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.utils.DSpace;
import org.junit.Test;

/**
 * Integration tests that verifies the configured beans of
 * {@link GoogleAnalyticsClient} type.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalyticsClientIT extends AbstractIntegrationTestWithDatabase {

    @Test
    public void testConfiguredGoogleAnalyticsClients() {

        List<GoogleAnalyticsClientImpl> clients = new DSpace().getServiceManager()
            .getServicesByType(GoogleAnalyticsClientImpl.class);

        assertThat(clients, hasSize(2));

        GoogleAnalyticsClientImpl ga4Client = getClientByKeyPrefix(clients, "G-");
        assertThat(ga4Client, notNullValue());
        assertThat(ga4Client.getRequestBuilder(), instanceOf(GoogleAnalytics4ClientRequestBuilder.class));
        assertThat(ga4Client.isAnalyticsKeySupported("G-12345"), is(true));
        assertThat(ga4Client.isAnalyticsKeySupported("UA-12345"), is(false));

        GoogleAnalyticsClientImpl uaClient = getClientByKeyPrefix(clients, "UA-");
        assertThat(uaClient, notNullValue());
        assertThat(uaClient.getRequestBuilder(), instanceOf(UniversalAnalyticsClientRequestBuilder.class));
        assertThat(uaClient.isAnalyticsKeySupported("G-12345"), is(false));
        assertThat(uaClient.isAnalyticsKeySupported("UA-12345"), is(true));

    }

    private GoogleAnalyticsClientImpl getClientByKeyPrefix(List<GoogleAnalyticsClientImpl> clients, String keyPrefix) {
        return clients.stream()
            .filter(client -> keyPrefix.equals(client.getKeyPrefix()))
            .findFirst()
            .orElse(null);
    }

}
