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
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    public void afterPropertiesSet()  {
    }

    public StatelessLoginFilter(String url, AuthenticationManager authenticationManager, RestAuthenticationService restAuthenticationService) {
        super(new AntPathRequestMatcher(url));
        this.authenticationManager = authenticationManager;
        this.restAuthenticationService = restAuthenticationService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {

        String user = req.getParameter("user");
        String password = req.getParameter("password");

        try {
             return authenticationManager.authenticate(
                    new DSpaceAuthentication(
                            user,
                            password,
                            new ArrayList<>()
                    )
            );
        } catch(BadCredentialsException e) {
            AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
            Iterator<AuthenticationMethod> authenticationMethodIterator = authenticationService.authenticationMethodIterator();
            while (authenticationMethodIterator.hasNext()) {
                AuthenticationMethod authenticationMethod = authenticationMethodIterator.next();
                Context context = ContextUtil.obtainContext(req);
                String loginPageURL = authenticationMethod.loginPageURL(context, req, res);
                if (StringUtils.isNotBlank(loginPageURL)) {
                    res.addHeader("Location", loginPageURL);
                }
            }
            throw e;
        }
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication);
    }
}
