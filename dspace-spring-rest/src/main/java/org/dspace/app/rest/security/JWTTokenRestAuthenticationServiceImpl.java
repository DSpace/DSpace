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
import java.text.ParseException;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JOSEException;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class JWTTokenRestAuthenticationServiceImpl implements RestAuthenticationService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationService.class);
    private static final String ACCESS_TOKEN = "access_token";

    @Autowired
    private JWTTokenHandler jwtTokenHandler;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response, String email) {
        try {
            Context context = ContextUtil.obtainContext(request);
            EPerson ePerson = ePersonService.findByEmail(context, email);
            List<Group> groups = authenticationService.getSpecialGroups(context, request);
            String token = jwtTokenHandler.createTokenForEPerson(context, request, ePerson, groups);

            response.getWriter().write(wrapTokenInJsonFormat(token));

        } catch (JOSEException e) {
            log.error("JOSE Exception", e);
        } catch (SQLException e) {
            log.error("SQL error when adding authentication", e);
        } catch (IOException e) {
            log.error("Error writing to response", e);
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
        return request.getHeader("Authorization") != null;
    }

    @Override
    public void invalidateAuthenticationData(HttpServletRequest request, Context context) {
        String token = getToken(request);
        jwtTokenHandler.invalidateToken(token, request, context);
    }

    private String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String tokenValue = authHeader.replace("Bearer", "").trim();
            return tokenValue;
        }
        return null;
    }

    //Put the token in a json-string
    private String wrapTokenInJsonFormat(String token) {
        return "{ \""+ ACCESS_TOKEN +"\" : \"" + token + "\" }";
    }


}
