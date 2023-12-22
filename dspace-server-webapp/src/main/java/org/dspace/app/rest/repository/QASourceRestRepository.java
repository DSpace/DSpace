/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.rest.model.QASourceRest;
import org.dspace.core.Context;
import org.dspace.qaevent.QASource;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository that handle QA sources.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(QASourceRest.CATEGORY + "." + QASourceRest.NAME)
public class QASourceRestRepository extends DSpaceRestRepository<QASourceRest, String> {

    @Autowired
    private QAEventService qaEventService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public QASourceRest findOne(Context context, String id) {
        QASource qaSource = qaEventService.findSource(id);
        if (qaSource == null) {
            return null;
        }
        return converter.toRest(qaSource, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<QASourceRest> findAll(Context context, Pageable pageable) {
        List<QASource> qaSources = qaEventService.findAllSources(pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countSources();
        return converter.toRestPage(qaSources, pageable, count, utils.obtainProjection());
    }


    @Override
    public Class<QASourceRest> getDomainClass() {
        return QASourceRest.class;
    }

}
