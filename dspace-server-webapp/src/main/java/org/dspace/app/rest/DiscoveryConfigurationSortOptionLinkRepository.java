/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.model.SortOptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * LinkRepository for the "sortoptions" subresource of an individual DiscoveryConfiguration.
 */
@Component(DiscoveryConfigurationRest.CATEGORY + "." + DiscoveryConfigurationRest.PLURAL_NAME + "."
    + DiscoveryConfigurationRest.SORT_OPTIONS)
public class DiscoveryConfigurationSortOptionLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @PreAuthorize("permitAll()")
    public Page<SortOptionRest> getSortOptions(@Nullable HttpServletRequest request, String name,
                                               @Nullable Pageable optionalPageable, Projection projection) {
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(name);
        if (discoveryConfiguration == null) {
            throw new ResourceNotFoundException("No such DiscoveryConfiguration: " + name);
        }

        Pageable pageable = optionalPageable != null ? optionalPageable : PageRequest.of(0, 20);
        return converter.toRestPage(discoveryConfiguration.getSearchSortConfiguration().getSortFields(),
            pageable, projection);
    }
}
