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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * This is a Filter class that'll fetch special groups from the {@link AuthenticationService} and set these in the
 * current DSpace Context. It'll do extra processing on anonymous requests to see which authorizations they
 * can implicitly have and adds those
 * This will allow us to for example set a specific Group to a specific IP so that any request from that
 * IP is always treated as being a part of the configured group.
 * The configuration for the authentication through ip can be found in authentication-ip.cfg
 * This can be enabled by uncommenting the IPAuhentication plugin in authentication.cfg
 */
public class AnonymousAdditionalAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger log = LogManager.getLogger();

    private final AuthenticationService authenticationService;

    /**
     * Constructor for the class
     * @param authenticationManager The relevant AuthenticationManager
     * @param authenticationService The autowired AuthenticationService
     */
    public AnonymousAdditionalAuthorizationFilter(AuthenticationManager authenticationManager,
                                                  AuthenticationService authenticationService) {
        super(authenticationManager);
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        Context context = ContextUtil.obtainContext(req);
        try {
            List<Group> groups = authenticationService.getSpecialGroups(context, req);
            for (Group group : groups) {
                context.setSpecialGroup(group.getID());
            }
        } catch (SQLException e) {
            log.error("Something went wrong trying to fetch groups in IPAuthenticationFilter", e);
        }
        chain.doFilter(req, res);
    }

}
