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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.QATopicRest;
import org.dspace.core.Context;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository that handle QA topics.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(QATopicRest.CATEGORY + "." + QATopicRest.PLURAL_NAME)
public class QATopicRestRepository extends DSpaceRestRepository<QATopicRest, String> {

    final static String ORDER_FIELD = "topic";

    @Autowired
    private QAEventService qaEventService;

    private static final Logger log = LogManager.getLogger();

    @Override
    public Page<QATopicRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("Method not allowed!", "");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCETOPIC', 'READ')")
    public QATopicRest findOne(Context context, String id) {
        String[] topicIdSplitted = id.split(":", 3);
        if (topicIdSplitted.length < 2) {
            return null;
        }
        String sourceName = topicIdSplitted[0];
        String topicName = topicIdSplitted[1].replaceAll("!", "/");
        UUID target = topicIdSplitted.length == 3 ? UUID.fromString(topicIdSplitted[2]) : null;
        QATopic topic = qaEventService.findTopicBySourceAndNameAndTarget(context, sourceName, topicName, target);
        return (topic != null) ? converter.toRest(topic, utils.obtainProjection()) : null;
    }

    @SearchRestMethod(name = "bySource")
    @PreAuthorize("hasPermission(#source, 'QUALITYASSURANCETOPIC', 'READ')")
    public Page<QATopicRest> findBySource(@Parameter(value = "source", required = true) String source,
           Pageable pageable) {
        Context context = obtainContext();
        boolean ascending = false;
        if (pageable.getSort() != null && pageable.getSort().getOrderFor(ORDER_FIELD) != null) {
            ascending = pageable.getSort().getOrderFor(ORDER_FIELD).getDirection() == Direction.ASC;
        }
        List<QATopic> topics = qaEventService.findAllTopicsBySource(context, source,
                                              pageable.getOffset(), pageable.getPageSize(), ORDER_FIELD, ascending);
        long count = qaEventService.countTopicsBySource(context, source);
        if (topics == null) {
            return null;
        }
        return converter.toRestPage(topics, pageable, count, utils.obtainProjection());
    }

    @SearchRestMethod(name = "byTarget")
    @PreAuthorize("hasPermission(#target, 'ITEM', 'READ')")
    public Page<QATopicRest> findByTarget(@Parameter(value = "target", required = true) UUID target,
        @Parameter(value = "source", required = true) String source, Pageable pageable) {
        Context context = obtainContext();
        boolean ascending = false;
        if (pageable.getSort() != null && pageable.getSort().getOrderFor(ORDER_FIELD) != null) {
            ascending = pageable.getSort().getOrderFor(ORDER_FIELD).getDirection() == Direction.ASC;
        }
        List<QATopic> topics = qaEventService.findAllTopicsBySourceAndTarget(context, source, target,
                                              pageable.getOffset(), pageable.getPageSize(), ORDER_FIELD, ascending);
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
