/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JOSEException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.security.DSpaceAuthentication;
import org.dspace.app.rest.security.RestAuthenticationService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Rest Authentication implementation for JSON Web Tokens
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@Component
public class JWTTokenRestAuthenticationServiceImpl implements RestAuthenticationService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationService.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_TYPE = "Bearer";

    @Autowired
    private JWTTokenHandler jwtTokenHandler;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response,
                                             DSpaceAuthentication authentication) throws IOException {
        try {
            Context context = ContextUtil.obtainContext(request);
            context.setCurrentUser(ePersonService.findByEmail(context, authentication.getName()));

            List<Group> groups = authenticationService.getSpecialGroups(context, request);

            String token = jwtTokenHandler.createTokenForEPerson(context, request,
                                                                 authentication.getPreviousLoginDate(), groups);

            addTokenToResponse(response, token);
            context.commit();

        } catch (JOSEException e) {
            log.error("JOSE Exception", e);
        } catch (SQLException e) {
            log.error("SQL error when adding authentication", e);
        }
    }

    @Override
    public EPerson getAuthenticatedEPerson(HttpServletRequest request, Context context) {
        String token = getToken(request);
        try {
            EPerson ePerson = jwtTokenHandler.parseEPersonFromToken(token, request, context);
            return ePerson;
        } catch (JOSEException e) {
            log.error("Jose error", e);
        } catch (ParseException e) {
            log.error("Error parsing EPerson from token", e);
        } catch (SQLException e) {
            log.error("SQL error while retrieving EPerson from token", e);
        }
        return null;
    }

    @Override
    public boolean hasAuthenticationData(HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getHeader(AUTHORIZATION_HEADER));
    }

    @Override
    public void invalidateAuthenticationData(HttpServletRequest request, Context context) throws Exception {
        String token = getToken(request);
        jwtTokenHandler.invalidateToken(token, request, context);
    }

    @Override
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    @Override
    public String getWwwAuthenticateHeaderValue(final HttpServletRequest request, final HttpServletResponse response) {
        Iterator<AuthenticationMethod> authenticationMethodIterator
                = authenticationService.authenticationMethodIterator();
        Context context = ContextUtil.obtainContext(request);

        StringBuilder wwwAuthenticate = new StringBuilder();
        while (authenticationMethodIterator.hasNext()) {
            AuthenticationMethod authenticationMethod = authenticationMethodIterator.next();

            if (wwwAuthenticate.length() > 0) {
                wwwAuthenticate.append(", ");
            }

            wwwAuthenticate.append(authenticationMethod.getName()).append(" realm=\"DSpace REST API\"");

            String loginPageURL = authenticationMethod.loginPageURL(context, request, response);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(loginPageURL)) {
                // We cannot reply with a 303 code because may browsers handle 3xx response codes transparently. This
                // means that the JavaScript client code is not aware of the 303 status and fails to react accordingly.
                wwwAuthenticate.append(", location=\"").append(loginPageURL).append("\"");
            }
        }

        return wwwAuthenticate.toString();
    }

    private void addTokenToResponse(final HttpServletResponse response, final String token) throws IOException {
        response.setHeader(AUTHORIZATION_HEADER, String.format("%s %s", AUTHORIZATION_TYPE, token));
    }

    private String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(authHeader)) {
            String tokenValue = authHeader.replace(AUTHORIZATION_TYPE, "").trim();
            return tokenValue;
        } else {
            return null;
        }
    }

}
