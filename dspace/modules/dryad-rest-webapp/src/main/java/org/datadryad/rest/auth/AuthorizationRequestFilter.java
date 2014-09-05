/*
 */
package org.datadryad.rest.auth;

import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.dspace.eperson.EPerson;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthorizationRequestFilter implements ContainerRequestFilter {
    @Context private HttpServletRequest servletRequest;
    static Boolean isValidToken(String accessToken) {
        System.out.println("Access token: " + accessToken);
        return Boolean.TRUE;
    }

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        // TODO: Make sure HTTPs if we can
        try {
            // This allows us to get tokens out of query parameter or header
            OAuthAccessResourceRequest oAuthRequest = new OAuthAccessResourceRequest(servletRequest, ParameterStyle.QUERY, ParameterStyle.HEADER);
            String accessToken = oAuthRequest.getAccessToken();
            EPersonUserPrincipal userPrincipal = AuthorizationHelper.getPrincipalFromToken(accessToken);
            if(userPrincipal == null) {
                throwExceptionResponse(null, Status.UNAUTHORIZED, OAuthError.ResourceResponse.INVALID_TOKEN);
            } else {
                // User found, set it into the context.
                EPersonSecurityContext securityContext = new EPersonSecurityContext(userPrincipal);
                containerRequest.setSecurityContext(securityContext);
                return containerRequest;
            }
        } catch (OAuthProblemException ex) {
            throwExceptionResponse(ex, Status.INTERNAL_SERVER_ERROR, "OAuth Problem");
        } catch (OAuthSystemException ex) {
            throwExceptionResponse(ex, Status.INTERNAL_SERVER_ERROR, "OAuth System Exception");
        }
        return containerRequest;
    }

    private static void throwExceptionResponse(Throwable throwable, Status status, String responseString) throws WebApplicationException {
        ResponseBuilder builder;
        builder = Response.status(status).entity(responseString);
        if(throwable == null) {
            throw new WebApplicationException(builder.build());
        } else {
            throw new WebApplicationException(throwable, builder.build());
        }
    }

}
