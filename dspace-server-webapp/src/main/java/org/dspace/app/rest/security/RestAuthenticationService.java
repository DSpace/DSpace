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

    void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response,
                                      DSpaceAuthentication authentication, boolean addCookie) throws IOException;

    EPerson getAuthenticatedEPerson(HttpServletRequest request, Context context);

    boolean hasAuthenticationData(HttpServletRequest request);

    void invalidateAuthenticationData(HttpServletRequest request, HttpServletResponse response, Context context)
            throws Exception;

    AuthenticationService getAuthenticationService();

    /**
     * Return the value that should be passed in the WWWW-Authenticate header for 4xx responses to the client
     * @param request The current client request
     * @param response The response being build for the client
     * @return A string value that should be set in the WWWW-Authenticate header
     */
    String getWwwAuthenticateHeaderValue(HttpServletRequest request, HttpServletResponse response);

    void invalidateAuthenticationCookie(HttpServletResponse res);

}
