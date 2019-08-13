/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.HarvesterMetadataRest;
import org.dspace.app.rest.model.hateoas.HarvesterMetadataResource;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for managing the HarvesterMetadata Rest object
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@Component(HarvesterMetadataRest.CATEGORY + "." + HarvesterMetadataRest.NAME)
public class HarvesterMetadataRestRepository extends DSpaceRestRepository<HarvesterMetadataRest, String> {

    public HarvesterMetadataRest findOne(Context context, String s) {
        return null;
    }

    public Page<HarvesterMetadataRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    public Class<HarvesterMetadataRest> getDomainClass() {
        return null;
    }

    public HarvesterMetadataResource wrapResource(HarvesterMetadataRest item, String... rels) {
        return new HarvesterMetadataResource(item, utils, rels);
    }


}
