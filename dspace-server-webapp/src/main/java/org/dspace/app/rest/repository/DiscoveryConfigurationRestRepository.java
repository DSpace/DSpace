/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.discovery.SearchUtils.RESOURCE_ID_FIELD;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.iiif.exception.NotImplementedException;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
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

    @PreAuthorize("permitAll()")
    public DiscoveryConfigurationRest findOne(Context context, String s) {

        DiscoveryConfiguration discoveryConfiguration = null;
        try {
            // check if the param is an UUID -> if not IllegalArgumentException is thrown
            UUID.fromString(s);

            Optional indexableObject = indexObjectServiceFactory
                .getIndexableObjectFactory(RESOURCE_ID_FIELD).findIndexableObject(context, s);

            if (indexableObject.isPresent()) {
                discoveryConfiguration = searchConfigurationService
                    .getDiscoveryConfigurationByNameOrIndexableObject(context,
                        "default", (IndexableObject) indexableObject.get());
            }

        } catch (IllegalArgumentException e) {
            // If the param is not an UUID -> it must be the name
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(s);
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
