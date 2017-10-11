package org.dspace.app.rest.security;

import org.dspace.eperson.EPerson;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class StatelessAuthenticationFilter extends BasicAuthenticationFilter{

    private TokenAuthenticationService tokenAuthenticationService;

    public StatelessAuthenticationFilter(AuthenticationManager authenticationManager) {

        super(authenticationManager);
        tokenAuthenticationService = new TokenAuthenticationService();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        Cookie cookie = WebUtils.getCookie(req,"access_token");


        if (cookie == null ) {
            chain.doFilter(req, res);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(req, cookie.getValue());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request, String token) {

        if (token != null) {
            // parse the token.
            EPerson eperson = tokenAuthenticationService.getAuthentication(token);
            if (eperson != null) {
                return new UsernamePasswordAuthenticationToken(eperson, null, new ArrayList<>());
            }
            return null;
        }
        return null;
    }



}
