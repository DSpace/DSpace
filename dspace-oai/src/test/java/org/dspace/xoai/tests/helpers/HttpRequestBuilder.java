/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers;

import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;

public class HttpRequestBuilder {
    private MockHttpServletRequest request = new MockHttpServletRequest();

    public HttpRequestBuilder withUrl(String url) {
        try {
            URI uri = new URI(url);
        } catch (URISyntaxException e) {
            // ASD
        }
        return this;
    }

    public HttpRequestBuilder usingGetMethod() {
        request.setMethod("GET");
        return this;
    }

    public HttpServletRequest build() {

        return request;
    }
}
