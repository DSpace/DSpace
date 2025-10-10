/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * RestRepository for the {@link DiscoveryConfiguration} object.
 */
@Component(DiscoveryConfigurationRest.CATEGORY + "." + DiscoveryConfigurationRest.PLURAL_NAME)
public class DiscoveryConfigurationRestRepository extends DSpaceRestRepository<DiscoveryConfigurationRest, String> {

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private ScopeResolver scopeResolver;

    @Override
    @PreAuthorize("permitAll()")
    public DiscoveryConfigurationRest findOne(Context context, String id) {
        DiscoveryConfiguration discoveryConfiguration = null;

        if (id.equals("scope")) {
            HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
            String uuid = request.getParameter("uuid");
            IndexableObject scopeObject = scopeResolver.resolveScope(context, uuid);
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(context, scopeObject);
        } else {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(id);
        }

        // Fall back to the default configuration in case nothing could be found.
        if (discoveryConfiguration == null) {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration("default");
        }

        return converter.toRest(discoveryConfiguration, utils.obtainProjection());
    }

    @Override
    public Page<DiscoveryConfigurationRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(DiscoveryConfigurationRest.NAME, "findAll");
    }

    @Override
    public Class<DiscoveryConfigurationRest> getDomainClass() {
        return DiscoveryConfigurationRest.class;
    }
}
