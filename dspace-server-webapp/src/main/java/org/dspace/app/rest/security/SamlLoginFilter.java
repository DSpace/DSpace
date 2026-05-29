/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.SamlAuthentication;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * A filter that examines requests to see if the user has been authenticated via SAML.
 * <p>
 * The overall SAML login process is as follows:
 * </p>
 * <ol>
 *   <li>When SAML authentication is enabled, the client/UI receives the URL to the active SAML
 *       relying party's authentication endpoint in the WWW-Authenticate header.
 *       See {@link org.dspace.authenticate.SamlAuthentication#loginPageURL(org.dspace.core.Context, HttpServletRequest, HttpServletResponse)}.</li>
 *   <li>The client sends the user to that URL when they select SAML authentication.</li>
 *   <li>The active SAML relying party sends the client to the login page at the asserting party
 *       (aka identity provider, or IdP).</li>
 *   <li>The user logs in to the asserting party.</li>
 *   <li>If successful, the asserting party sends the client back to the relying party's assertion
 *       consumer endpoint, along with the SAML assertion.</li>
 *   <li>The relying party receives the SAML assertion, extracts attributes from the assertion,
 *       maps them into request attributes, and forwards the request to the path where this filter
 *       is listening.</li>
 *   <li>This filter intercepts the request in order to check for a valid SAML login (see
 *       {@link org.dspace.authenticate.SamlAuthentication#authenticate(org.dspace.core.Context, String, String, String, HttpServletRequest)})
 *       and stores that user info in a JWT. It also saves that JWT in a <em>temporary</em>
 *       authentication cookie.</li>
 *   <li>This filter redirects the user back to the UI (after verifying it's at a trusted URL).</li>
 *   <li>The client reads the JWT from the cookie, and sends it back in a request to
 *       /api/authn/login, which triggers the server-side to destroy the cookie and move the JWT
 *       into a header.</li>
 * </ol>
 *
 * @author Ray Lee
 */
public class SamlLoginFilter extends StatelessLoginFilter {
    private static final Logger logger = LogManager.getLogger(SamlLoginFilter.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public SamlLoginFilter(String url, String httpMethod, AuthenticationManager authenticationManager,
            RestAuthenticationService restAuthenticationService) {
        super(url, httpMethod, authenticationManager, restAuthenticationService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {

        if (!SamlAuthentication.isEnabled()) {
            throw new ProviderNotFoundException("SAML is disabled.");
        }

        // Because this authentication is implicit, we pass in an empty DSpaceAuthentication.
        return authenticationManager.authenticate(new DSpaceAuthentication());
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
        Authentication auth) throws IOException, ServletException {

        restAuthenticationService.addAuthenticationDataForUser(request, response, (DSpaceAuthentication) auth, true);

        redirectAfterSuccess(request, response);
    }

    /**
     * After successful login, redirect to the configured UI URL. If that URL is not allowed for
     * this DSpace site, return a 400 error.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    private void redirectAfterSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectUrl = configurationService.getProperty("dspace.ui.url");
        String redirectHostName = Utils.getHostName(redirectUrl);
        String serverUrl = configurationService.getProperty("dspace.server.url");

        boolean isRedirectAllowed = Stream.concat(
                Stream.of(serverUrl),
                Arrays.stream(configurationService.getArrayProperty("rest.cors.allowed-origins")))
            .map(url -> Utils.getHostName(url))
            .anyMatch(hostName -> hostName.equalsIgnoreCase(redirectHostName));

        if (isRedirectAllowed) {
            logger.debug("SAML redirecting to " + redirectUrl);

            response.sendRedirect(redirectUrl);
        } else {
            logger.error("SAML redirect URL {} is not allowed" + redirectUrl);

            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"SAML redirect URL not allowed");
        }
    }
}
