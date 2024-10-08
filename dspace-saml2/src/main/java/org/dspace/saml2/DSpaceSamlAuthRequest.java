/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saml2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.HttpMethod;

/**
 * A request wrapper for SAML authentication requests that are forwarded to the DSpace
 * authentication endpoint from the assertion consumer endpoint of the SAML relying party.
 * <p>
 * The assertion consumer may receive an assertion from the SAML asserting party via
 * either GET or POST, depending on how the binding has been configured. This normalizes
 * the method to GET when forwarding to the DSpace authentication endpoint, which has the
 * advantage of bypassing (unnecessary) CORS protection on the DSpace authentication endpoint.
 * </p>
 *
 * @author Ray Lee
 */
public class DSpaceSamlAuthRequest extends HttpServletRequestWrapper {
    public DSpaceSamlAuthRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * Returns GET, regardless of the method of the wrapped requeset.
     *
     * @return "GET"
     */
    @Override
    public String getMethod() {
        return HttpMethod.GET.name();
    }
}
