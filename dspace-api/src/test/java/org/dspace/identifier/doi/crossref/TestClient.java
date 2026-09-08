/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi.crossref;

import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.mockito.Mockito;

class TestClient {

    private TestClient() { }

    /**
     * provides a BaseHttpClient without the need of fully initializing DSpaceHttpClientFactory.
     */
    @SuppressWarnings("Regexp")
    static BaseHttpClient baseHttpClientForTest() {
        var factory = Mockito.mock(DSpaceHttpClientFactory.class);

        // here we mock the 2 configurations that are used by BaseHttpClient
        // in case BaseHttpClient changes this mocking must be updated too!
        Mockito.doReturn(HttpClientBuilder.create()
                .disableRedirectHandling().build()).when(factory).build(Mockito.any());

        Mockito.doReturn(HttpClientBuilder.create().build()).when(factory).build();

        return new BaseHttpClient(factory);
    }
}
