/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.model.wrapper.AuthenticationToken;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Service;

/**
 * Interface for a service that can provide authentication for the REST API
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@Service
public interface RestAuthenticationService {

    /**
     * Add authenticated user data found in the request and/or Authentication class into the response.
     * <P>
     * This method is called after authentication has succeeded, and it allows the REST API to provide
     * user data in the response (in the form of a JWT or similar, depending on implementation)
     * <P>
     * If addSingleUseCookie is set to true (and allowSingleUseAuthCookie() returns true), then user data should be
     * stored in a cookie temporarily for ONE request. Once this cookie is read again, it should be immediately replaced
     * by storing user data in Headers. Storing any authentication data in a cookie long term is not secure, as it makes
     * DSpace susceptible to CSRF (cross site request forgery) attacks.
     * @param request current request
     * @param response current response
     * @param authentication Authentication information from successful login
     * @param addSingleUseCookie true/false, whether to include authentication data in a single-use cookie
     *                           When false, DSpace only will use headers to return authentication data.
     * @throws IOException
     */
    void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response,
                                      DSpaceAuthentication authentication, boolean addSingleUseCookie)
        throws IOException;

    /**
     * Retrieve a short lived authentication token, this can be used (among other things) for file downloads
     * @param context the DSpace context
     * @param request The current client request
     * @return An AuthenticationToken that contains a string with the token
     */
    AuthenticationToken getShortLivedAuthenticationToken(Context context, HttpServletRequest request);

    /**
     * Retrieve the currently authenticated EPerson (i.e. user) based on the data in the current request
     * and/or current DSpace context.
     * <P>
     * Usually the EPerson is obtained via the current login token passed in a request header (most secure).
     * <P>
     * However, if allowSingleUseAuthCookie() is true, then the login token may be passed via a temporary cookie.
     * If such a cookie is found, then that cookie should be deleted after reading it. Storing any authentication data
     * in a cookie long term is not secure, as it makes DSpace susceptible to CSRF (cross site request forgery) attacks.
     * @param request current request
     * @param response current response
     * @param context current DSpace Context
     * @return current EPerson, or null if not found
     */
    EPerson getAuthenticatedEPerson(HttpServletRequest request, HttpServletResponse response, Context context);

    /**
     * Returns whether the current request/response has EPerson authentication data. This method simply checks
     * the headers and/or temporary cookies for a valid login token.
     * @param request current request
     * @param response current response
     * @return true if login token found, false otherwise
     */
    boolean hasAuthenticationData(HttpServletRequest request, HttpServletResponse response);

    /**
     * Invalidates any existing login tokens (if found) in request/response. This is necessary to completely
     * logout from the system. This removes/invalidates the token from the headers and/or temporary cookies.
     * @param request current request
     * @param response current response
     * @param context current DSpace context
     * @throws Exception if error occurs
     */
    void invalidateAuthenticationData(HttpServletRequest request, HttpServletResponse response, Context context)
            throws Exception;

    /**
     * Get current AuthenticationService which stores Authentication methods/plugins
     * @return
     */
    AuthenticationService getAuthenticationService();

    /**
     * Return the value that should be passed in the WWWW-Authenticate header for 4xx responses to the client.
     * <P>
     * In DSpace, we use this header to send the list of all valid authentication options to the client site, including
     * the URL/path of each authentication option.
     * @param request The current client request
     * @param response The response being build for the client
     * @return A string value that should be set in the WWWW-Authenticate header
     */
    String getWwwAuthenticateHeaderValue(HttpServletRequest request, HttpServletResponse response);

    /**
     * Whether (or not) this Authentication service allows for the creation (and reading) of single-use
     * cookies to store authentication tokens/info.
     * <P>
     * This method should be used by addAuthenticationDataForUser() to determine whether single-use
     * cookies can be created.
     * <P>
     * This method should also be used by getAuthenticatedEPerson() to determine whether single-use
     * cookies can be read from to provide trusted authentication information.
     * <P>
     * WARNING: Ideally, a RestAuthenticationService will only support single-use cookies in _very specific_ scenarios
     * (e.g. when a redirect is needed, per getOriginRedirectUrl()). Keep in mind that storing any authentication data
     * in a cookie makes DSpace potentially susceptible to CSRF (cross site request forgery) attacks. So, when in doubt,
     * this method should return 'false'.
     * @param request current request
     * @param response current response
     * @return true if single-use cookies can be trusted in current request and/or written to current response.
     * false if they cannot be trusted or used in request and/or response.
     */
    boolean allowSingleUseAuthCookie(HttpServletRequest request, HttpServletResponse response);

    /**
     * When allowSingleUseAuthCookie() is true, the most likely scenario is a *redirect* will occur during the
     * authentication of the user. Since an HTTP redirect cannot send HTTP headers, it must send the auth token via
     * a temporary cookie. One example is Shibboleth.
     * <P>
     * This method checks the current request (usually query string params) for a possible pending redirect back to
     * the origin of the authentication request, and returns the URL.
     * @param request current request
     * @return URL of origin to be redirected to (if any), or null
     */
    String getOriginRedirectUrl(HttpServletRequest request);
}
