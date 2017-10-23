/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StatelessAuthenticationFilter extends BasicAuthenticationFilter{

    private static final Logger log = LoggerFactory.getLogger(StatelessAuthenticationFilter.class);

    private TokenAuthenticationService tokenAuthenticationService;

    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

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

        Authentication authentication = getAuthentication(req, cookie.getValue());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
    }

    private Authentication getAuthentication(HttpServletRequest request, String token) {

        if (token != null) {
            // parse the token.
            Context context = null;
            try {
                context = ContextUtil.obtainContext(request);
            } catch (SQLException e) {
                log.error("Unable to obtain context from request", e);
            }
            EPerson eperson = tokenAuthenticationService.getAuthentication(token, request, context);
            boolean isAdmin = false;
            try {
                isAdmin = authorizeService.isAdmin(context, eperson);
            } catch (SQLException e) {
                log.error("SQL error while checking for admin rights", e);
            }
            if (eperson != null) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (isAdmin) {
                    authorities.add(new SimpleGrantedAuthority("ADMIN"));
                } else {
                    authorities.add(new SimpleGrantedAuthority("EPERSON"));
                }
                return new DSpaceAuthentication(eperson.getEmail(), null, authorities);
            }
            return null;
        }
        return null;
    }



}
