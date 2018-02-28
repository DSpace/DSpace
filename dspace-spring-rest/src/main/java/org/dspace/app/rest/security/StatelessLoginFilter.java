/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * This class will filter login requests to try and authenticate them
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class StatelessLoginFilter extends AbstractAuthenticationProcessingFilter {

    private AuthenticationManager authenticationManager;

    private RestAuthenticationService restAuthenticationService;

    @Override
    public void afterPropertiesSet() {
    }

    public StatelessLoginFilter(String url, AuthenticationManager authenticationManager,
                                RestAuthenticationService restAuthenticationService) {
        super(new AntPathRequestMatcher(url));
        this.authenticationManager = authenticationManager;
        this.restAuthenticationService = restAuthenticationService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {

        String user = req.getParameter("user");
        String password = req.getParameter("password");

        return authenticationManager.authenticate(
                new DSpaceAuthentication(user, password, new ArrayList<>())
        );
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {

        AuthenticationService authenticationService = restAuthenticationService.getAuthenticationService();

        Iterator<AuthenticationMethod> authenticationMethodIterator
                = authenticationService.authenticationMethodIterator();
        Context context = ContextUtil.obtainContext(request);
        String redirectUrl = null;

        while (authenticationMethodIterator.hasNext()) {
            AuthenticationMethod authenticationMethod = authenticationMethodIterator.next();

            String loginPageURL = authenticationMethod.loginPageURL(context, request, response);
            if (StringUtils.isNotBlank(loginPageURL)) {
                redirectUrl = loginPageURL;
            }
        }

        String wwwAuthenticate = "Bearer realm=\"DSpace REST API\"";
        if (redirectUrl != null) {
            //We cannot reply with a 303 code because may browsers handle 3xx response codes transparently.
            //This means that the JavaScript client code is not aware of the 303 status and fails to react accordingly.
            wwwAuthenticate = wwwAuthenticate + ", location=\"" + redirectUrl + "\"";
        }

        response.setHeader("WWW-Authenticate", wwwAuthenticate);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, failed.getMessage());

    }
}
