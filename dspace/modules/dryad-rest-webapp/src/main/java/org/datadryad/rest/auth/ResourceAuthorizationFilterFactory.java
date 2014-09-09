/*
 */
package org.datadryad.rest.auth;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ResourceAuthorizationFilterFactory implements ResourceFilterFactory {

    @Override
    public List<ResourceFilter> create(AbstractMethod am) {
        List<ResourceFilter> filters = new ArrayList<ResourceFilter>();
        filters.add(new ResourceAuthorizationFilter(am));
        return filters;
    }

}
