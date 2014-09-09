/*
 */
package org.datadryad.rest.auth;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import java.security.Principal;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ResourceAuthorizationFilter implements ResourceFilter, ContainerRequestFilter {
    private static final Logger log = Logger.getLogger(ResourceAuthorizationFilter.class);
    private static final String NO_ACCESS = "You do not have access to the requested resource";
    // The abstractMethod tells us what resource is being accessed, e.g.
    // GET /organizations/test/manuscripts/abc
    private final AbstractMethod abstractMethod;
    public ResourceAuthorizationFilter(AbstractMethod abstractMethod) {
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
        // Filter needs to check if user is authorized to access resource
        log.info("Filtering request for authorization");
        SecurityContext securityContext = containerRequest.getSecurityContext();
        Principal principal = securityContext.getUserPrincipal();
        if(principal != null) {
            log.info("Authenticated user is " + principal.getName());
            log.info("Abstract Method is " + abstractMethod.toString());
            // TODO: check access to resource
            return containerRequest;
        } else {
            // No authenticated user
            AuthHelper.throwExceptionResponse(null, Status.UNAUTHORIZED, NO_ACCESS);
            return null;
        }
    }

}
