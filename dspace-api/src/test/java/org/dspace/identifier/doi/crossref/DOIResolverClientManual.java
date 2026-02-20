/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.crossref;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

/**
 * Manual test that connects to the actual DOI resolver service.
 */
public class DOIResolverClientManual {

    private DOIResolverClient client;

    @Before
    public void setUp() throws Exception {
        var baseClient = TestClient.baseHttpClientForTest();


        client = new DOIResolverClient("https", "dx.doi.org", null, baseClient);
    }

    // example data is taken from
    // https://www.crossref.org/documentation/register-maintain-records/verify-your-registration/

    @Test
    public void canResolveKnownDoi() throws Exception {
        var response = client.sendDOIGetRequest("doi:10.13003/5jchdy");

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.url()).isEqualTo("https://www.crossref.org/display-guidelines/");
    }

    @Test
    public void canResolveUnknownDoi() throws Exception {
        var response = client.sendDOIGetRequest("doi:10.13003/unregisteredDOI");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.url()).isNull();
    }
}