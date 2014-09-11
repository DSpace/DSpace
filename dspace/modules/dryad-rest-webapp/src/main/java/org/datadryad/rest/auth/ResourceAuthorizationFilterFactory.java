/*
 */
package org.datadryad.rest.auth;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ResourceAuthorizationFilterFactory implements ResourceFilterFactory {
    private final HttpContext httpContext;
    private final AuthHelper authHelper;
    public ResourceAuthorizationFilterFactory(@Context HttpContext httpContext, @Context AuthHelper authHelper) {
        this.httpContext = httpContext;
        this.authHelper = authHelper;
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod abstractMethod) {
        List<ResourceFilter> filters = new ArrayList<ResourceFilter>();
        filters.add(new ResourceAuthorizationFilter(httpContext, authHelper, abstractMethod));
        return filters;
    }

}
