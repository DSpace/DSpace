package org.dspace.app.rest.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class StatelessLoginFilter extends AbstractAuthenticationProcessingFilter {

    private AuthenticationManager authenticationManager;
    private TokenAuthenticationService tokenAuthenticationService;

    public StatelessLoginFilter(String url, AuthenticationManager authenticationManager) {
        super(new AntPathRequestMatcher(url));
        this.authenticationManager = authenticationManager;
        this.tokenAuthenticationService = new TokenAuthenticationService();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
            String email = req.getParameter("email");
            String password = req.getParameter("password");

            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            password,
                            new ArrayList<>())
            );
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        tokenAuthenticationService.addAuthentication(req, res, auth.getName());

    }
}
