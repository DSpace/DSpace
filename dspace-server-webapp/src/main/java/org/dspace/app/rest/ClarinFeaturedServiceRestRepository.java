/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.model.ClarinFeaturedServiceRest;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * A REST Repository for the maintaining the Featured Service objects, but these methods do nothing.
 * This repository must be created because of parsing the Featured Service object to the response.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component(ClarinFeaturedServiceRest.CATEGORY + "." + ClarinFeaturedServiceRest.NAME)
public class ClarinFeaturedServiceRestRepository extends DSpaceRestRepository<ClarinFeaturedServiceRest, Integer> {
    @Override
    @PreAuthorize("permitAll()")
    public ClarinFeaturedServiceRest findOne(Context context, Integer integer) {
        return null;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ClarinFeaturedServiceRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    @Override
    public Class<ClarinFeaturedServiceRest> getDomainClass() {
        return ClarinFeaturedServiceRest.class;
    }
}
