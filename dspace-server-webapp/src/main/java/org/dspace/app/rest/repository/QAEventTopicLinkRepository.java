/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.model.QATopicRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.qaevent.QATopic;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "topic" subresource of a qa event.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(QAEventRest.CATEGORY + "." + QAEventRest.PLURAL_NAME + "." + QAEventRest.TOPIC)
public class QAEventTopicLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private QAEventService qaEventService;

    /**
     * Returns the topic of the qa event with the given id.
     *
     * @param request    the http servlet request
     * @param id         the qa event id
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return           the qa topic rest representation
     */
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCEEVENT', 'READ')")
    public QATopicRest getTopic(@Nullable HttpServletRequest request, String id, @Nullable Pageable pageable,
            Projection projection) {
        Context context = obtainContext();
        QAEvent qaEvent = qaEventService.findEventByEventId(id);
        if (qaEvent == null) {
            throw new ResourceNotFoundException("No qa event with ID: " + id);
        }
        String source = qaEvent.getSource();
        String topicName = qaEvent.getTopic();
        QATopic topic = qaEventService
                .findTopicBySourceAndNameAndTarget(context, source, topicName, null);
        if (topic == null) {
            throw new ResourceNotFoundException("No topic found with source: " + source + " topic: " + topicName);
        }
        return converter.toRest(topic, projection);
    }
}
