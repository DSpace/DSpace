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


    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        Cookie cookie = WebUtils.getCookie(httpServletRequest,"access_token");
        EPerson ePerson = tokenAuthenticationService.getAuthentication(cookie.getValue(), httpServletRequest);
        Context context = null;
        try {
            context = ContextUtil.obtainContext(httpServletRequest);
        } catch (SQLException e) {
            log.error("Unable to obtain context", e);
        }
        ePerson.setSessionSalt("");
        try {
            context.commit();
        } catch (SQLException e) {
            log.error("Unable to commit change to session salt", e);
        }
    }


}
