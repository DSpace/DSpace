/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
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
@Component(QASourceRest.CATEGORY + "." + QASourceRest.PLURAL_NAME)
public class QASourceRestRepository extends DSpaceRestRepository<QASourceRest, String> {

    @Autowired
    private QAEventService qaEventService;

    @Override
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCESOURCE', 'READ')")
    public QASourceRest findOne(Context context, String id) {
        QASource qaSource = qaEventService.findSource(context, id);
        if (qaSource == null) {
            return null;
        }
        return converter.toRest(qaSource, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<QASourceRest> findAll(Context context, Pageable pageable) {
        List<QASource> qaSources = qaEventService.findAllSources(context, pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countSources(context);
        return converter.toRestPage(qaSources, pageable, count, utils.obtainProjection());
    }

    @SearchRestMethod(name = "byTarget")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<QASourceRest> findByTarget(@Parameter(value = "target", required = true) UUID target,
           Pageable pageable) {
        Context context = obtainContext();
        List<QASource> topics = qaEventService.findAllSourcesByTarget(context, target,
                                        pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countSourcesByTarget(context, target);
        if (topics == null) {
            return null;
        }
        return converter.toRestPage(topics, pageable, count, utils.obtainProjection());
    }

    @Override
    public Class<QASourceRest> getDomainClass() {
        return QASourceRest.class;
    }

}
