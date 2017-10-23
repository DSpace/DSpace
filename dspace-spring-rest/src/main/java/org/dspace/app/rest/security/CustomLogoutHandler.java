/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;

@Component
public class CustomLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    private TokenAuthenticationService tokenAuthenticationService = new TokenAuthenticationService();

    /**
     * This method removes the session salt from an eperson, this way the token won't be verified anymore
     * @param httpServletRequest
     * @param httpServletResponse
     * @param authentication
     */
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        Cookie cookie = WebUtils.getCookie(httpServletRequest,"access_token");
        try {
            Context context = ContextUtil.obtainContext(httpServletRequest);
            EPerson ePerson = tokenAuthenticationService.getAuthentication(cookie.getValue(), httpServletRequest, context);
            ePerson.setSessionSalt("");
            context.commit();
        } catch (SQLException e) {
            log.error("Unable to obtain context", e);
        }

    }


}
