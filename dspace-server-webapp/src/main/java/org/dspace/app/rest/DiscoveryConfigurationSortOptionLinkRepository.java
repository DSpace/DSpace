/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.iiif.exception.NotImplementedException;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(DiscoveryConfigurationRest.CATEGORY + "." + DiscoveryConfigurationRest.PLURAL_NAME + "."
    + DiscoveryConfigurationRest.SORT_OPTION)
public class DiscoveryConfigurationSortOptionLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {


    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public Page<DiscoveryConfigurationRest> getSortOptions(@Nullable HttpServletRequest request,
                                       UUID itemId,
                                       @Nullable Pageable optionalPageable,
                                       Projection projection) {
        throw new NotImplementedException("DiscoveryConfigurationRestRepository.getSortOptions() not implemented");
    }
}
