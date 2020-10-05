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
     * If addSingleUseCookie is set to true, then user data should be stored in a cookie temporarily for ONE request.
     * Once this cookie is read again, it should be immediately replaced by storing user data in Headers. Storing any
     * authentication info in a cookie long term is not secure, as it makes DSpace susceptible to CSRF
     * (cross site request forgery) attacks.
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
     * and/or current DSpace context. Usually the EPerson is obtained via the current login token passed
     * in a request header (or temporary cookie)
     * @param request current request
     * @param response current response
     * @param context current DSpace Context
     * @return current EPerson, or null if not found
     */
    EPerson getAuthenticatedEPerson(HttpServletRequest request, HttpServletResponse response, Context context);

    /**
     * Returns whether the current request/response has EPerson authentication data. This method simply checks
     * the headers and/or temporary cookies for a valid login token (if found). If its found in a single-use
     * cookie then the cookie should be deleted & the login token moved to a header in the response.
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
}
