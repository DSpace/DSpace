/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class StatelessAuthenticationFilter extends BasicAuthenticationFilter{

    private static final Logger log = LoggerFactory.getLogger(StatelessAuthenticationFilter.class);

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    @Autowired
    private EPersonRestAuthenticationProvider authenticationProvider;

    public StatelessAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void initFilterBean() throws ServletException {
        //Initialise the auto-wire dependencies
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this,
                getFilterConfig().getServletContext());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        Authentication authentication = getAuthentication(req);
        if (authentication != null ) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(req, res);
    }

    private Authentication getAuthentication(HttpServletRequest request) {

        if (restAuthenticationService.hasAuthenticationData(request)) {
            // parse the token.
            Context context = null;
            try {
                context = ContextUtil.obtainContext(request);
            } catch (SQLException e) {
                log.error("Unable to obtain context from request", e);
                //TODO FREDERIC throw exception to fail fast
            }

            EPerson eperson = restAuthenticationService.getAuthenticatedEPerson(request, context);
            if (eperson != null) {
                List<GrantedAuthority> authorities = authenticationProvider.getGrantedAuthorities(context, eperson);
                return new DSpaceAuthentication(eperson.getEmail(), authorities);
            } else {
                return null;
            }
        }

        return null;
    }

}
