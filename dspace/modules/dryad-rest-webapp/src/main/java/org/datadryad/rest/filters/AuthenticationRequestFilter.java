/*
 */
package org.datadryad.rest.filters;


import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.datadryad.rest.auth.AuthHelper;
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
        if(userPrincipal != null) {
            // User found, set it into the context.
            EPersonSecurityContext securityContext = new EPersonSecurityContext(userPrincipal);
            containerRequest.setSecurityContext(securityContext);
        }
        return;
    }
}
