/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.nbevent.NBTopic;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.rest.model.NBTopicRest;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(NBTopicRest.CATEGORY + "." + NBTopicRest.NAME)
public class NBTopicRestRepository extends DSpaceRestRepository<NBTopicRest, String> {

    @Autowired
    private NBEventService nbEventService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public NBTopicRest findOne(Context context, String id) {
        NBTopic nbTopic = nbEventService.findTopicByTopicId(id);
        if (nbTopic == null) {
            return null;
        }
        return converter.toRest(nbTopic, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<NBTopicRest> findAll(Context context, Pageable pageable) {
        List<NBTopic> nbTopics = nbEventService.findAllTopics(context, pageable.getOffset(), pageable.getPageSize());
        long count = nbEventService.countTopics(context);
        if (nbTopics == null) {
            return null;
        }
        return converter.toRestPage(nbTopics, pageable, count, utils.obtainProjection());
    }

    @Override
    public Class<NBTopicRest> getDomainClass() {
        return NBTopicRest.class;
    }

}
