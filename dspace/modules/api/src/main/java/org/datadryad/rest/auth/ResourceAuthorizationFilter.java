/*
 */
package org.datadryad.rest.auth;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.api.model.AbstractSubResourceLocator;
import com.sun.jersey.api.model.AbstractSubResourceMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;


/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ResourceAuthorizationFilter implements ResourceFilter, ContainerRequestFilter {
    private static final Logger log = Logger.getLogger(ResourceAuthorizationFilter.class);
    private final HttpContext httpContext;
    private final AuthHelper authHelper;
    private static final String NO_ACCESS = "You do not have access to the requested resource";
    private final AbstractMethod abstractMethod;
    public ResourceAuthorizationFilter(HttpContext httpContext, AuthHelper authHelper, AbstractMethod abstractMethod) {
        this.httpContext = httpContext;
        this.authHelper = authHelper;
        this.abstractMethod = abstractMethod;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        log.info("Filtering request for resource authorization");
        // Filter needs to check if user is authorized to access resource
        SecurityContext securityContext = containerRequest.getSecurityContext();
        AuthorizationTuple tuple = getTupleFromSecurityContext(securityContext);
        if(!authHelper.isAuthorized(tuple)) {
            AuthHelper.throwExceptionResponse(null, Status.UNAUTHORIZED, NO_ACCESS);
            return null;
        }
        return containerRequest;
    }

    private AuthorizationTuple getTupleFromSecurityContext(SecurityContext securityContext) {
        // Three things to extract: person, verb, and path
        EPersonUserPrincipal principal = null;
        Integer ePersonId = null;
        if(securityContext.getUserPrincipal() instanceof EPersonUserPrincipal) {
            principal = (EPersonUserPrincipal) securityContext.getUserPrincipal();
        }
        if(principal != null) {
            ePersonId = principal.getID();
        } else {
            ePersonId = -1;
        }
        String httpMethod = null;
        String path = httpContext.getUriInfo().getPath();
        if(abstractMethod instanceof AbstractResourceMethod) {
            AbstractResourceMethod resourceMethod = (AbstractResourceMethod) abstractMethod;
            httpMethod = resourceMethod.getHttpMethod();
        } else if(abstractMethod instanceof AbstractSubResourceMethod) {
            AbstractSubResourceMethod subResourceMethod = (AbstractSubResourceMethod) abstractMethod;
            httpMethod = subResourceMethod.getHttpMethod();
        } else if(abstractMethod instanceof AbstractSubResourceLocator) {
            AbstractSubResourceLocator subResourceLocator = (AbstractSubResourceLocator) abstractMethod;
        }
        AuthorizationTuple tuple = new AuthorizationTuple(ePersonId, httpMethod, path);
        return tuple;
    }
}
