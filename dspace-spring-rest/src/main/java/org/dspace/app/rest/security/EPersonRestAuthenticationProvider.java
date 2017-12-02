/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.security.WebSecurityConfiguration.ADMIN_GRANT;
import static org.dspace.app.rest.security.WebSecurityConfiguration.EPERSON_GRANT;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * This class is reponsible for authenticating a user via REST
 *
 * @author Atmire NV (info at atmire dot com)
 */
@Component
public class EPersonRestAuthenticationProvider implements AuthenticationProvider{

    private static final Logger log = LoggerFactory.getLogger(EPersonRestAuthenticationProvider.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private HttpServletRequest request;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Context context = ContextUtil.obtainContext(request);
        if(context != null && context.getCurrentUser() != null) {
            return authenticateRefreshTokenRequest(context);
        } else {
            return authenticateNewLogin(authentication);
        }
    }

    private Authentication authenticateRefreshTokenRequest(Context context) {
        authenticationService.updateLastActiveDate(context);
        return createAuthentication(null, context);
    }

    private Authentication authenticateNewLogin(Authentication authentication) {
        Context newContext = null;
        Authentication output = null;

        if(authentication != null && authentication.getCredentials() != null) {
            try {
                newContext = new Context();
                String name = authentication.getName();
                String password = authentication.getCredentials().toString();

                int implicitStatus = authenticationService.authenticateImplicit(newContext, null, null, null, request);

                if (implicitStatus == AuthenticationMethod.SUCCESS) {
                    log.info(LogManager.getHeader(newContext, "login", "type=implicit"));
                    output = createAuthentication(password, newContext);
                } else {
                    int authenticateResult = authenticationService.authenticate(newContext, name, password, null, request);
                    if (AuthenticationMethod.SUCCESS == authenticateResult) {

                        log.info(LogManager
                                .getHeader(newContext, "login", "type=explicit"));

                        output = createAuthentication(password, newContext);
                    } else {
                        log.info(LogManager.getHeader(newContext, "failed_login", "email="
                                + name + ", result="
                                + authenticateResult));
                        throw new BadCredentialsException("Login failed");
                    }
                }
            } catch (Exception e) {
                log.error("Error while authenticating in the rest api", e);
            } finally {
                if (newContext != null && newContext.isValid()) {
                    try {
                        newContext.complete();
                    } catch (SQLException e) {
                        log.error(e.getMessage() + " occurred while trying to close", e);
                    }
                }
            }
        }

        return output;
    }

    private Authentication createAuthentication(final String password, final Context context) {
        EPerson ePerson = context.getCurrentUser();

        if(ePerson != null && StringUtils.isNotBlank(ePerson.getEmail())) {
            //Pass the eperson ID to the request service
            requestService.setCurrentUserId(ePerson.getID());

            return new DSpaceAuthentication(ePerson, getGrantedAuthorities(context, ePerson));

        } else {
            log.info(LogManager.getHeader(context, "failed_login", "No eperson with an non-blank e-mail address found"));
            throw new BadCredentialsException("Login failed");
        }
    }

    public List<GrantedAuthority> getGrantedAuthorities(Context context, EPerson eperson) {
        List<GrantedAuthority> authorities = new LinkedList<>();

        if(eperson != null) {
            boolean isAdmin = false;
            try {
                isAdmin = authorizeService.isAdmin(context, eperson);
            } catch (SQLException e) {
                log.error("SQL error while checking for admin rights", e);
            }

            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority(ADMIN_GRANT));
            }

            authorities.add(new SimpleGrantedAuthority(EPERSON_GRANT));
        }

        return authorities;
    }

    public boolean supports(Class<?> authentication) {
        return authentication.equals(DSpaceAuthentication.class);
    }
}
