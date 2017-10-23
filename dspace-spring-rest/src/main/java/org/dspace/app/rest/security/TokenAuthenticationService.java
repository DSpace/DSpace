package org.dspace.app.rest.security;

import com.nimbusds.jose.JOSEException;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

public class TokenAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationService.class);


    private JWTTokenHandler jwtTokenHandler;
    private EPersonService ePersonService;
    private AuthenticationService authenticationService;

    public TokenAuthenticationService() {
        jwtTokenHandler = new JWTTokenHandler();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    }

    public void addAuthentication(HttpServletRequest request, HttpServletResponse response, String email) {
        try {
            EPerson ePerson = ePersonService.findByEmail(ContextUtil.obtainContext(request), email);
            Context context = ContextUtil.obtainContext(request);
            List<Group> groups = authenticationService.getSpecialGroups(context, request);
            String token = jwtTokenHandler.createTokenForEPerson(context, request, ePerson, groups);
            //TODO token is saved in a cookie, but might be better to save it in http header
            Cookie cookie = new Cookie("access_token", token);
            response.addCookie(cookie);
        } catch (JOSEException e) {
            log.error("JOSE Exception", e);
        } catch (SQLException e) {
            log.error("SQL error when adding authentication", e);
        }

    }

    public EPerson getAuthentication(String token, HttpServletRequest request) {
        try {
            EPerson ePerson = jwtTokenHandler.parseEPersonFromToken(token, request);
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



}
