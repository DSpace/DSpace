/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.saml2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ray Lee
 */
public class DSpaceSamlAuthRequestTest {
    @Test
    public void testWrapPostRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod(HttpMethod.POST.name());

        DSpaceSamlAuthRequest samlAuthRequest = new DSpaceSamlAuthRequest(request);

        assertEquals(HttpMethod.GET.name(), samlAuthRequest.getMethod());
    }

    @Test
    public void testWrapGetRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod(HttpMethod.GET.name());

        DSpaceSamlAuthRequest samlAuthRequest = new DSpaceSamlAuthRequest(request);

        assertEquals(HttpMethod.GET.name(), samlAuthRequest.getMethod());
    }
}
