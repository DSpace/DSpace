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

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Service;

/**
 * Interface for a service that can provide authentication for the REST API
 *
 * @author Atmire NV (info at atmire dot com)
 */
@Service
public interface RestAuthenticationService {

    void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response, DSpaceAuthentication authentication) throws IOException;

    EPerson getAuthenticatedEPerson(HttpServletRequest request, Context context);

    boolean hasAuthenticationData(HttpServletRequest request);

    void invalidateAuthenticationData(HttpServletRequest request, Context context) throws Exception;

}
