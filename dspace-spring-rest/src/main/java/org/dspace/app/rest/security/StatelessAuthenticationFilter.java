/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Custom Spring authentication filter for Stateless authentication, intercepts requests to check for valid
 * authentication
 *
 * @author Atmire NV (info at atmire dot com)
 */
public class StatelessAuthenticationFilter extends BasicAuthenticationFilter{

    private static final Logger log = LoggerFactory.getLogger(StatelessAuthenticationFilter.class);

    private RestAuthenticationService restAuthenticationService;

    private EPersonRestAuthenticationProvider authenticationProvider;

    private RequestService requestService;

    public StatelessAuthenticationFilter(AuthenticationManager authenticationManager,
                                         RestAuthenticationService restAuthenticationService,
                                         EPersonRestAuthenticationProvider authenticationProvider,
                                         RequestService requestService) {
        super(authenticationManager);
        this.requestService = requestService;
        this.restAuthenticationService = restAuthenticationService;
        this.authenticationProvider = authenticationProvider;
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

            Context context = ContextUtil.obtainContext(request);

            EPerson eperson = restAuthenticationService.getAuthenticatedEPerson(request, context);
            if (eperson != null) {
                //Pass the eperson ID to the request service
                requestService.setCurrentUserId(eperson.getID());

                //Get the Spring authorities for this eperson
                List<GrantedAuthority> authorities = authenticationProvider.getGrantedAuthorities(context, eperson);

                //Return the Spring authentication object
                return new DSpaceAuthentication(eperson.getEmail(), authorities);
            } else {
                return null;
            }
        }

        return null;
    }

}
