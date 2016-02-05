/*
 */
package org.datadryad.rest.auth;

import com.sun.istack.logging.Logger;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthenticationRequestFilter implements ContainerRequestFilter {
    private static final Logger log = Logger.getLogger(AuthenticationRequestFilter.class);
    @Context private HttpServletRequest servletRequest;
    @Context private AuthHelper authHelper;

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        log.info("Filtering request for authentication");
        // TODO: Make sure HTTPs if we can
        try {
            // This allows us to get tokens out of query parameter or header
            OAuthAccessResourceRequest oAuthRequest = new OAuthAccessResourceRequest(servletRequest, ParameterStyle.QUERY, ParameterStyle.HEADER);
            String accessToken = oAuthRequest.getAccessToken();
            EPersonUserPrincipal userPrincipal = authHelper.getPrincipalFromToken(accessToken);
            if(userPrincipal == null) {
                AuthHelper.throwExceptionResponse(null, Status.UNAUTHORIZED, OAuthError.ResourceResponse.INVALID_TOKEN);
            } else {
                // User found, set it into the context.
                EPersonSecurityContext securityContext = new EPersonSecurityContext(userPrincipal);
                containerRequest.setSecurityContext(securityContext);
                return containerRequest;
            }
        } catch (OAuthProblemException ex) {
            AuthHelper.throwExceptionResponse(ex, Status.INTERNAL_SERVER_ERROR, "OAuth Problem");
        } catch (OAuthSystemException ex) {
            AuthHelper.throwExceptionResponse(ex, Status.INTERNAL_SERVER_ERROR, "OAuth System Exception");
        }
        return containerRequest;
    }
}
