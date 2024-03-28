/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.authenticate.OidcAuthenticationBean.OIDC_AUTH_ATTRIBUTE;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * This class will filter OpenID Connect (OIDC) requests and try and authenticate them.
 * In this case, the actual authentication is performed by OIDC. After authentication succeeds, OIDC will send
 * the authentication data to this filter in order for it to be processed by DSpace.
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class OidcLoginFilter extends StatelessLoginFilter {

    public OidcLoginFilter(String url, String httpMethod, AuthenticationManager authenticationManager,
            RestAuthenticationService restAuthenticationService) {
        super(url, httpMethod, authenticationManager, restAuthenticationService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
        throws AuthenticationException {
        req.setAttribute(OIDC_AUTH_ATTRIBUTE, OIDC_AUTH_ATTRIBUTE);
        // NOTE: because this authentication is implicit, we pass in an empty DSpaceAuthentication
        return authenticationManager.authenticate(new DSpaceAuthentication());
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
        Authentication auth) throws IOException, ServletException {
        restAuthenticationService.addAuthenticationDataForUser(req, res, (DSpaceAuthentication) auth, true);
        chain.doFilter(req, res);
    }

}
