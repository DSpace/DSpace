package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.ShibAuthentication;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        String redirectPageURL = getRedirectPageURL(request, response);
        if (StringUtils.isNotBlank(redirectPageURL)) {
            // we have a logout page to redirect to
            response.setHeader("Location", redirectPageURL);
            response.setStatus(HttpStatus.FOUND.value());
        } else {
            // the default logout will return no content
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }

    /**
     * This method will test if the request has the shibboleth parameters (action
     * and return) and in case of a shibboleth logout action, then retrieve the
     * return URL, if that is not the case it will test if the authenticated methods
     * have any logout page and provide it if it occurs
     * 
     * @param request
     * @param response
     * @return fully-qualified URL or an empty string
     */
    private String getRedirectPageURL(HttpServletRequest request, HttpServletResponse response) {
        String returnURL;
        // verify if we have shibboleth parameters (action and return)
        String action = request.getParameter("action");
        String shibLogoutURL = request.getParameter("return");

        // is shibboleth action for logout?
        if (action.equals(ShibAuthentication.SHIBBOLETH_LOGOUT_ACTION) && StringUtils.isNotBlank(shibLogoutURL)) {
            returnURL = shibLogoutURL;
        } else {
            returnURL = getRedirectLogoutURLFromAuthMethod(request, response);
        }

        return returnURL;
    }

    /**
     * This method will return a string with a url for a logout page if any is found
     * 
     * @param request
     * @param response
     * @return fully-qualified URL or an empty string
     */
    private String getRedirectLogoutURLFromAuthMethod(HttpServletRequest request, HttpServletResponse response) {
        String logoutPageURL = "";
        AuthenticationService authService = restAuthenticationService.getAuthenticationService();
        Iterator<AuthenticationMethod> authenticationMethodIterator = authService.authenticationMethodIterator();

        Context context = ContextUtil.obtainContext(request);

        // We don't know which authentication method has a logout page so we have to
        // go through all the available ones and set the first one available, if any
        while (authenticationMethodIterator.hasNext()) {
            AuthenticationMethod authenticationMethod = authenticationMethodIterator.next();

            // Only return if we find any value for logout url
            logoutPageURL = authenticationMethod.logoutPageURL(context, request, response);
            if (StringUtils.isNotBlank(logoutPageURL)) {
                return logoutPageURL;
            }
        }
        return logoutPageURL;
    }

}
