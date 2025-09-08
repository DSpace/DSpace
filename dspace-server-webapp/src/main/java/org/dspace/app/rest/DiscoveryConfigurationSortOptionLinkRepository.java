/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.springframework.stereotype.Component;

@Component(DiscoveryConfigurationRest.CATEGORY + "." + DiscoveryConfigurationRest.PLURAL_NAME + "."
    + DiscoveryConfigurationRest.SORT_OPTION)
public class DiscoveryConfigurationSortOptionLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {
}
