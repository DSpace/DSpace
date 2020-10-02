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

    EPerson getAuthenticatedEPerson(HttpServletRequest request, HttpServletResponse response, Context context);

    boolean hasAuthenticationData(HttpServletRequest request, HttpServletResponse response);

    void invalidateAuthenticationData(HttpServletRequest request, HttpServletResponse response, Context context)
            throws Exception;

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

    void invalidateSingleUseCookie(HttpServletResponse res);

}
