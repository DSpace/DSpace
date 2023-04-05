/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.CommunityGroupRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.CommunityGroup;
import org.dspace.content.service.CommunityGroupService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to retrieve CommunityGroup Rest objects
 *
 * @author Mohamed Abdul Rasheed (mohideen at umd.edu)
 */
@Component(CommunityGroupRest.CATEGORY + "." + CommunityGroupRest.NAME)
public class CommunityGroupRestRepository extends DSpaceRestRepository<CommunityGroupRest, Integer> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CommunityGroupRestRepository.class);

    @Autowired
    AuthorizeService authorizeService;

    private CommunityGroupService cgs;

    public CommunityGroupRestRepository(CommunityGroupService cgService) {
        this.cgs = cgService;
    }

    @Override
    @PreAuthorize("permitAll()")
    public CommunityGroupRest findOne(Context context, Integer id) {
        CommunityGroup communityGroup = null;
        communityGroup = cgs.find(id);
        if (communityGroup == null) {
            return null;
        }
        return converter.toRest(communityGroup, utils.obtainProjection());
    }

    @Override
    public Page<CommunityGroupRest> findAll(Context context, Pageable pageable) {
        List<CommunityGroup> communityGroups = cgs.findAll();
        return converter.toRestPage(communityGroups, pageable, utils.obtainProjection());
    }

    @Override
    public Class<CommunityGroupRest> getDomainClass() {
        return CommunityGroupRest.class;
    }
}
