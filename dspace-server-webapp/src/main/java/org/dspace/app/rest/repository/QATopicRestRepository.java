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
import org.dspace.app.rest.model.QATopicRest;
import org.dspace.core.Context;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository that handle QA topics.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(QATopicRest.CATEGORY + "." + QATopicRest.NAME)
public class QATopicRestRepository extends DSpaceRestRepository<QATopicRest, String> {

    @Autowired
    private QAEventService qaEventService;

    @Override
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCETOPIC', 'READ')")
    public QATopicRest findOne(Context context, String id) {
        QATopic topic = qaEventService.findTopicByTopicId(id);
        if (topic == null) {
            return null;
        }
        return converter.toRest(topic, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<QATopicRest> findAll(Context context, Pageable pageable) {
        List<QATopic> topics = qaEventService.findAllTopics(context, pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countTopics();
        if (topics == null) {
            return null;
        }
        return converter.toRestPage(topics, pageable, count, utils.obtainProjection());
    }

    @SearchRestMethod(name = "bySource")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<QATopicRest> findBySource(Context context, @Parameter(value = "source", required = true) String source,
           Pageable pageable) {
        List<QATopic> topics = qaEventService.findAllTopicsBySource(context, source,
                                              pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countTopicsBySource(context, source);
        if (topics == null) {
            return null;
        }
        return converter.toRestPage(topics, pageable, count, utils.obtainProjection());
    }

    @SearchRestMethod(name = "byTarget")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<QATopicRest> findByTarget(@Parameter(value = "target", required = true) UUID target,
        @Parameter(value = "source", required = true) String source,
        Pageable pageable) {
        Context context = obtainContext();
        List<QATopic> topics = qaEventService.findAllTopicsBySourceAndTarget(context, source, target,
                                              pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countTopicsBySourceAndTarget(context, source, target);
        if (topics == null) {
            return null;
        }
        return converter.toRestPage(topics, pageable, count, utils.obtainProjection());
    }

    @Override
    public Class<QATopicRest> getDomainClass() {
        return QATopicRest.class;
    }

}
