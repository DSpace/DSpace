/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.security.jwt.JWTTokenHandler;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Custom logout handler to support stateless sessions
 *
 * @author Atmire NV (info at atmire dot com)
 */
@Component
public class CustomLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    /**
     * This method removes the session salt from an eperson, this way the token won't be verified anymore
     * @param httpServletRequest
     * @param httpServletResponse
     * @param authentication
     */
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        try {
            Context context = ContextUtil.obtainContext(httpServletRequest);
            restAuthenticationService.invalidateAuthenticationData(httpServletRequest, context);
            context.commit();

        } catch (Exception e) {
            log.error("Unable to logout", e);
        }
    }
}
