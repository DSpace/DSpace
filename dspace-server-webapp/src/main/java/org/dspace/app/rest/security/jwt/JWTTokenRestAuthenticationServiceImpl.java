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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

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
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
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

    @Lazy
    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response,
            DSpaceAuthentication authentication, boolean addCookie) throws IOException {
        try {
            Context context = ContextUtil.obtainContext(request);
            context.setCurrentUser(ePersonService.findByEmail(context, authentication.getName()));

            String token = loginJWTTokenHandler.createTokenForEPerson(context, request,
                                                                 authentication.getPreviousLoginDate());
            context.commit();

            // Add newly generated auth token to the response
            addTokenToResponse(request, response, token, addCookie);

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
            String token = shortLivedJWTTokenHandler.createTokenForEPerson(context, request, null);
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
    public EPerson getAuthenticatedEPerson(HttpServletRequest request, HttpServletResponse response, Context context) {
        try {
            String token = getLoginToken(request, response);
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
        String token = getLoginToken(request, response);
        loginJWTTokenHandler.invalidateToken(token, request, context);

        // Reset our CSRF token, generating a new one
        resetCSRFToken(request, response);
    }

    /**
     * Invalidate our temporary authentication cookie by overwriting it in the response.
     * @param request
     * @param response
     */
    @Override
    public void invalidateAuthenticationCookie(HttpServletRequest request, HttpServletResponse response) {
        // Re-send the same cookie (as addTokenToResponse()) with no value and a Max-Age of 0 seconds
        ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_COOKIE, "")
                                              .maxAge(0).httpOnly(true).secure(true).sameSite("None").build();

        // Write the cookie to the Set-Cookie header in order to send it
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Reset our CSRF token, generating a new one
        resetCSRFToken(request, response);
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
     * Adds the Authentication token (JWT) to the response either in a header (default) or in a temporary cookie.
     * <P>
     * If 'addCookie' is true, then the JWT is also added to a response cookie. This is primarily for support of auth
     * plugins which _require_ cookie-based auth (e.g. Shibboleth). Note that this cookie can be used cross-site
     * (i.e. SameSite=None), but cannot be used by Javascript (HttpOnly), including the Angular UI. It also will only be
     * sent via HTTPS (Secure).
     * <P>
     * If 'addCookie' is false, then the JWT is only added in the Authorization header & the auth cookie (if it exists)
     * is removed. This ensures we are primarily using the Authorization header & remove the temporary auth cookie as
     * soon as it is no longer needed.
     * <P>
     * Because this method is called for login actions, it usually resets the CSRF token, *except* when the auth cookie
     * is being created. This is because we will reset the CSRF token once the auth cookie is used & invalidated.
     * @param request current request
     * @param response current response
     * @param token the authentication token
     * @param addCookie whether to send token in a cookie & header (true) or header only (false)
     */
    private void addTokenToResponse(final HttpServletRequest request, final HttpServletResponse response,
                                    final String token, final Boolean addCookie) {
        // If addCookie=true, create a temporary authentication cookie. This is primarily used for the initial
        // Shibboleth response (which requires a number of redirects), as headers cannot be sent via a redirect. As soon
        // as the UI (or Hal Browser) obtains the Shibboleth login data, it makes a call to /login (addCookie=false)
        // which destroys this temporary auth cookie. So, the auth cookie only exists a few seconds.
        if (addCookie) {
            ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_COOKIE, token)
                                                  .httpOnly(true).secure(true).sameSite("None").build();

            // Write the cookie to the Set-Cookie header in order to send it
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            // NOTE: Because the auth cookie is meant to be temporary, we do NOT reset our CSRF token when creating it.
            // Instead, we'll reset the CSRF token when the auth cookie is *destroyed* during call to /login.
        } else if (hasAuthorizationCookie(request)) {
            // Since an auth cookie exists & is no longer needed (addCookie=false), remove/invalidate the auth cookie.
            // This also resets the CSRF token, as auth cookie is destroyed when /login is called.
            invalidateAuthenticationCookie(request, response);
        } else {
            // If we are just adding a new token to header, then reset the CSRF token.
            // This forces the token to change when login process doesn't rely on auth cookie.
            resetCSRFToken(request, response);
        }
        response.setHeader(AUTHORIZATION_HEADER, String.format("%s %s", AUTHORIZATION_TYPE, token));
    }

    /**
     * Get the Login token (JWT) in the current request. First we check the Authorization header.
     * If not found there, we check for a temporary authentication cookie and use that.
     * @param request current request
     * @return authentication token (if found), or null
     */
    private String getLoginToken(HttpServletRequest request, HttpServletResponse response) {
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

    /**
     * Get the value of the (temporary) authorization cookie, if exists.
     * @param request current request
     * @return string cookie value
     */
    private String getAuthorizationCookie(HttpServletRequest request) {
        String authCookie = "";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_COOKIE) && StringUtils.isNotEmpty(cookie.getValue())) {
                    authCookie = cookie.getValue();
                    break;
                }
            }
        }
        return authCookie;
    }

    /**
     * Check if the (temporary) authorization cookie exists and is not empty.
     * @param request current request
     * @return true if cookie is found in request. false otherwise.
     */
    private boolean hasAuthorizationCookie(HttpServletRequest request) {
        if (StringUtils.isNotEmpty(getAuthorizationCookie(request))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Force reset the CSRF Token, causing a new one to be generated.
     * This method is used internally during login/logout to ensure a new CSRF token is generated anytime authentication
     * information changes.
     * @param request current request
     * @param response current response
     */
    private void resetCSRFToken(HttpServletRequest request, HttpServletResponse response) {
        // Remove current CSRF token & generate a new one
        // We do this as we want the token to change anytime you login or logout
        csrfTokenRepository.saveToken(null, request, response);
        CsrfToken newToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(newToken, request, response);
    }

}
