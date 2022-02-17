/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.nbevent.NBSource;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.rest.model.NBSourceRest;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository that handle NB soufces.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(NBSourceRest.CATEGORY + "." + NBSourceRest.NAME)
public class NBSourceRestRepository extends DSpaceRestRepository<NBSourceRest, String> {

    @Autowired
    private NBEventService nbEventService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public NBSourceRest findOne(Context context, String id) {
        NBSource nbSource = nbEventService.findSource(id);
        return converter.toRest(nbSource, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<NBSourceRest> findAll(Context context, Pageable pageable) {
        List<NBSource> nbSources = nbEventService.findAllSources(context, pageable.getOffset(), pageable.getPageSize());
        long count = nbEventService.countTopics(context);
        if (nbSources == null) {
            return null;
        }
        return converter.toRestPage(nbSources, pageable, count, utils.obtainProjection());
    }


    @Override
    public Class<NBSourceRest> getDomainClass() {
        return NBSourceRest.class;
    }

}
