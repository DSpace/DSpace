/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.nbevent.NBTopic;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.model.NBTopicRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "topic" subresource of a nb event.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(NBEventRest.CATEGORY + "." + NBEventRest.NAME + "." + NBEventRest.TOPIC)
public class NBEventTopicLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private NBEventService nbEventService;

    /**
     * Returns the topic of the nb event with the given id.
     *
     * @param request    the http servlet request
     * @param id         the nb event id
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return the nb topic rest representation
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public NBTopicRest getTopic(@Nullable HttpServletRequest request, String id, @Nullable Pageable pageable,
            Projection projection) {
        Context context = obtainContext();
        NBEvent nbEvent = nbEventService.findEventByEventId(context, id);
        if (nbEvent == null) {
            throw new ResourceNotFoundException("No nb event with ID: " + id);
        }
        NBTopic topic = nbEventService.findTopicByTopicId(nbEvent.getTopic().replace("/", "!"));
        if (topic == null) {
            throw new ResourceNotFoundException("No topic found with id : " + id);
        }
        return converter.toRest(topic, projection);
    }
}
