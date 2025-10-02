/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;


import org.dspace.app.iiif.exception.NotImplementedException;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(DiscoveryConfigurationRest.CATEGORY + "." + DiscoveryConfigurationRest.PLURAL_NAME)
public class DiscoveryConfigurationRestRepository extends DSpaceRestRepository<DiscoveryConfigurationRest, String> {

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;
    @Autowired
    protected IndexObjectFactoryFactory indexObjectServiceFactory;
    @Autowired
    private ScopeResolver scopeResolver;

    @Override
    @PreAuthorize("permitAll()")
    public DiscoveryConfigurationRest findOne(Context context, String value) {
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(value);
        // Fall back to the default configuration in case nothing could be found.
        if (discoveryConfiguration == null) {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration("default");
        }

        return converter.toRest(discoveryConfiguration, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    public DiscoveryConfigurationRest findOne(Context context, String value, String uuid) {

        DiscoveryConfiguration discoveryConfiguration = null;

        // Expect UUID in query-params if id = "scope"
        if (value.equals("scope")) {
            IndexableObject scopeObject = scopeResolver.resolveScope(context, uuid);


            discoveryConfiguration = searchConfigurationService
                .getDiscoveryConfigurationByNameOrIndexableObject(context,
                    "default", scopeObject);
        }

        // Fall back to the default configuration in case nothing could be found.
        if (discoveryConfiguration == null) {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration("default");
        }

        return converter.toRest(discoveryConfiguration, utils.obtainProjection());
    }

    @Override
    public Page<DiscoveryConfigurationRest> findAll(Context context, Pageable pageable) {
        throw new NotImplementedException(
            "DiscoveryConfigurationRestRepository#findAll(context, String) not implemented"
        );
    }

    @Override
    public Class<DiscoveryConfigurationRest> getDomainClass() {
        return DiscoveryConfigurationRest.class;
    }
}
