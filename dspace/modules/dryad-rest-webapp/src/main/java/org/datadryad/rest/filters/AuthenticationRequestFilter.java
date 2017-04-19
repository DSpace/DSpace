/*
 */
package org.datadryad.rest.filters;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.datadryad.rest.auth.AuthHelper;
import org.datadryad.rest.auth.AuthorizationTuple;
import org.datadryad.rest.auth.EPersonSecurityContext;
import org.datadryad.rest.auth.EPersonUserPrincipal;
import org.datadryad.rest.storage.AuthorizationStorageInterface;
import org.datadryad.rest.storage.OAuthTokenStorageInterface;
import org.datadryad.rest.storage.rdbms.AuthorizationDatabaseStorageImpl;
import org.datadryad.rest.storage.rdbms.OAuthTokenDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthenticationRequestFilter implements ContainerRequestFilter {
    private static final Logger log = Logger.getLogger(AuthenticationRequestFilter.class);
    @Context private HttpServletRequest servletRequest;
    OAuthTokenStorageInterface tokenStorage = new OAuthTokenDatabaseStorageImpl();
    AuthorizationStorageInterface authzStorage = new AuthorizationDatabaseStorageImpl();
    private AuthHelper authHelper;
    private static final String NO_ACCESS = "You do not have access to the requested resource";

    public AuthenticationRequestFilter() {
        authHelper =  new AuthHelper(tokenStorage, authzStorage);
    }

    @Override
    public void filter(ContainerRequestContext containerRequest) {
        log.info("Filtering request for authentication");
        // TODO: Make sure HTTPs if we can
        OAuthAccessResourceRequest oAuthRequest = null;
        String accessToken = null;
        try {
            // This allows us to get tokens out of query parameter or header
            oAuthRequest = new OAuthAccessResourceRequest(servletRequest, ParameterStyle.QUERY, ParameterStyle.HEADER);
            accessToken = oAuthRequest.getAccessToken();
        } catch (OAuthProblemException ex) {
            log.error("OAuth problem: " + ex.getMessage());
        } catch (OAuthSystemException ex) {
            AuthHelper.throwExceptionResponse(ex, Status.INTERNAL_SERVER_ERROR, "OAuth System Exception");
        }
        EPersonUserPrincipal userPrincipal = authHelper.getPrincipalFromToken(accessToken);
        EPersonSecurityContext securityContext = new EPersonSecurityContext(userPrincipal);
        AuthorizationTuple tuple = getTupleFromSecurityContext(securityContext, containerRequest);
        if(!authHelper.isAuthorized(tuple)) {
            AuthHelper.throwExceptionResponse(null, Status.UNAUTHORIZED, NO_ACCESS);
        }
        containerRequest.setSecurityContext(securityContext);
    }

    private AuthorizationTuple getTupleFromSecurityContext(SecurityContext securityContext, ContainerRequestContext containerRequestContext) {
        // Three things to extract: person, verb, and path
        EPersonUserPrincipal principal = null;
        AuthorizationTuple tuple = null;
        String path = containerRequestContext.getUriInfo().getPath();
        String httpMethod = containerRequestContext.getMethod();
        if(securityContext.getUserPrincipal() instanceof EPersonUserPrincipal) {
            principal = (EPersonUserPrincipal) securityContext.getUserPrincipal();
        }
        if (principal == null) {
            tuple = new AuthorizationTuple(-1, httpMethod, path);
        } else {
            Integer ePersonId = principal.getID();
            log.info("Authenticated user is " + principal.getName());
            log.info("Eperson id is " + ePersonId);
            log.info("HTTP Method is " + httpMethod);
            tuple = new AuthorizationTuple(ePersonId, httpMethod, path);
        }

        return tuple;
    }

}
