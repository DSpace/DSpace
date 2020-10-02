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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JOSEException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.wrapper.AuthenticationToken;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Rest Authentication implementation for JSON Web Tokens
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
@Component
public class JWTTokenRestAuthenticationServiceImpl implements RestAuthenticationService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationService.class);
    private static final String AUTHORIZATION_COOKIE = "Authorization-cookie";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_TYPE = "Bearer";
    private static final String AUTHORIZATION_TOKEN_PARAMETER = "authentication-token";

    @Autowired
    private LoginJWTTokenHandler loginJWTTokenHandler;

    @Autowired
    private ShortLivedJWTTokenHandler shortLivedJWTTokenHandler;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response,
            DSpaceAuthentication authentication, boolean addCookie) throws IOException {
        try {
            Context context = ContextUtil.obtainContext(request);
            context.setCurrentUser(ePersonService.findByEmail(context, authentication.getName()));

            List<Group> groups = authenticationService.getSpecialGroups(context, request);

            String token = loginJWTTokenHandler.createTokenForEPerson(context, request,
                                                                 authentication.getPreviousLoginDate(), groups);

            addTokenToResponse(response, token, addCookie);
            context.commit();

        } catch (JOSEException e) {
            log.error("JOSE Exception", e);
        } catch (SQLException e) {
            log.error("SQL error when adding authentication", e);
        }
    }

    /**
     * Create a short-lived token for bitstream downloads among other things
     * @param context   The context for which to create the token
     * @param request   The request for which to create the token
     * @return The token with a short lifespan
     */
    @Override
    public AuthenticationToken getShortLivedAuthenticationToken(Context context, HttpServletRequest request) {
        try {
            String token;
            List<Group> groups = authenticationService.getSpecialGroups(context, request);
            token = shortLivedJWTTokenHandler.createTokenForEPerson(context, request, null, groups);
            context.commit();
            return new AuthenticationToken(token);
        } catch (JOSEException e) {
            log.error("JOSE Exception", e);
        } catch (SQLException e) {
            log.error("SQL error when adding authentication", e);
        }

        return null;
    }

    @Override
    public EPerson getAuthenticatedEPerson(HttpServletRequest request, Context context) {
        try {
            String token = getLoginToken(request);
            EPerson ePerson = null;
            if (token == null) {
                token = getShortLivedToken(request);
                ePerson = shortLivedJWTTokenHandler.parseEPersonFromToken(token, request, context);
            } else {
                ePerson = loginJWTTokenHandler.parseEPersonFromToken(token, request, context);
            }
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
        return StringUtils.isNotBlank(request.getHeader(AUTHORIZATION_HEADER))
                || StringUtils.isNotBlank(getAuthorizationCookie(request))
                || StringUtils.isNotBlank(request.getParameter(AUTHORIZATION_TOKEN_PARAMETER));
    }

    @Override
    public void invalidateAuthenticationData(HttpServletRequest request, HttpServletResponse response,
                                             Context context) throws Exception {
        String token = getLoginToken(request);
        invalidateAuthenticationCookie(response);
        loginJWTTokenHandler.invalidateToken(token, request, context);
    }

    @Override
    public void invalidateAuthenticationCookie(HttpServletResponse response) {
        // Re-send the same cookie (as addTokenToResponse()) with no value and a Max-Age of 0 seconds
        ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_COOKIE, "")
                                              .maxAge(0).httpOnly(true).secure(true).sameSite("None").build();

        // Write the cookie to the Set-Cookie header in order to send it
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    /**
     * Return a comma-separated list of all currently enabled authentication options (based on DSpace configuration).
     * This list is sent to the client in the WWW-Authenticate header in order to inform it of all the enabled
     * authentication plugins *and* (optionally) to provide it with the "location" of the login page, if
     * the authentication plugin requires an external login page (e.g. Shibboleth).
     * <P>
     * Example output looks like:
     *    shibboleth realm="DSpace REST API" location=[shibboleth-url], password realm="DSpace REST API"
     * @param request The current client request
     * @param response The response being build for the client
     * @return comma separated list of authentication options
     */
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

            // If authentication method requires a custom login page, add that as the "location". The client is
            // expected to read this "location" and send users to that URL when this authentication option is selected
            // We cannot reply with a 303 code because many browsers handle 3xx response codes transparently. This
            // means that the JavaScript client code is not aware of the 303 status and fails to react accordingly.
            String loginPageURL = authenticationMethod.loginPageURL(context, request, response);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(loginPageURL)) {
                wwwAuthenticate.append(", location=\"").append(loginPageURL).append("\"");
            }
        }

        return wwwAuthenticate.toString();
    }

    /**
     * Add login token to the current response in the Authorization header.
     * <P>
     * If requested, the JWT is also returned in a cookie. This is primarily for support of auth
     * plugins which _require_ cookie-based auth (e.g. Shibboleth). Note that this cookie can be used cross-site
     * (i.e. SameSite=None), but cannot be used by Javascript (HttpOnly) including the Angular UI. It also will only be
     * sent via HTTPS (Secure).
     *
     * @param response current response
     * @param token auth token to add
     * @throws IOException
     */
    private void addTokenToResponse(final HttpServletResponse response, final String token, final Boolean addCookie)
            throws IOException {
        // Add an authentication cookie to the response *if* requested
        // NOTE: authentication cookies are only needed by specific auth plugins (e.g. Shibboleth, which cannot use
        // authentication headers as headers cannot be sent via redirects).
        if (addCookie) {
            ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_COOKIE, token)
                                                  .httpOnly(true).secure(true).sameSite("None").build();

            // Write the cookie to the Set-Cookie header in order to send it
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        response.setHeader(AUTHORIZATION_HEADER, String.format("%s %s", AUTHORIZATION_TYPE, token));
    }

    private String getLoginToken(HttpServletRequest request) {
        String tokenValue = null;
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String authCookie = getAuthorizationCookie(request);
        if (StringUtils.isNotBlank(authHeader)) {
            tokenValue = authHeader.replace(AUTHORIZATION_TYPE, "").trim();
        } else if (StringUtils.isNotBlank(authCookie)) {
            tokenValue = authCookie;
        }

        return tokenValue;
    }

    private String getShortLivedToken(HttpServletRequest request) {
        String tokenValue = null;
        if (StringUtils.isNotBlank(request.getParameter(AUTHORIZATION_TOKEN_PARAMETER))) {
            tokenValue = request.getParameter(AUTHORIZATION_TOKEN_PARAMETER);
        }

        return tokenValue;
    }

    private String getAuthorizationCookie(HttpServletRequest request) {
        String authCookie = "";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_COOKIE) && StringUtils.isNotEmpty(cookie.getValue())) {
                    authCookie = cookie.getValue();
                }
            }
        }
        return authCookie;
    }

}
