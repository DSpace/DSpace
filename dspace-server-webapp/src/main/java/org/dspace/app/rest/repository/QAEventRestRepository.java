/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.qaevent.dao.QAEventsDao;
import org.dspace.qaevent.service.QAEventService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository that handle QA events.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(QAEventRest.CATEGORY + "." + QAEventRest.NAME)
public class QAEventRestRepository extends DSpaceRestRepository<QAEventRest, String> {

    final static String ORDER_FIELD = "trust";

    @Autowired
    private QAEventService qaEventService;

    @Autowired
    private QAEventsDao qaEventDao;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ResourcePatch<QAEvent> resourcePatch;

    private Logger log = org.slf4j.LoggerFactory.getLogger(QAEventRestRepository.class);

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public QAEventRest findOne(Context context, String id) {
        QAEvent qaEvent = qaEventService.findEventByEventId(id);
        if (qaEvent == null) {
            // HACK check if this request is part of a patch flow
            qaEvent = (QAEvent) requestService.getCurrentRequest().getAttribute("patchedNotificationEvent");
            if (qaEvent != null && qaEvent.getEventId().contentEquals(id)) {
                return converter.toRest(qaEvent, utils.obtainProjection());
            } else {
                return null;
            }
        }
        return converter.toRest(qaEvent, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByTopic")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<QAEventRest> findByTopic(Context context, @Parameter(value = "topic", required = true) String topic,
            Pageable pageable) {
        List<QAEvent> qaEvents = null;
        Long count = 0L;
        boolean ascending = false;
        if (pageable.getSort() != null && pageable.getSort().getOrderFor(ORDER_FIELD) != null) {
            ascending = pageable.getSort().getOrderFor(ORDER_FIELD).getDirection() == Direction.ASC;
        }
        qaEvents = qaEventService.findEventsByTopicAndPage(topic,
            pageable.getOffset(), pageable.getPageSize(), ORDER_FIELD, ascending);
        count = qaEventService.countEventsByTopic(topic);
        if (qaEvents == null) {
            return null;
        }
        return converter.toRestPage(qaEvents, pageable, count, utils.obtainProjection());
    }

    @Override
    protected void delete(Context context, String id) throws AuthorizeException {
        Item item;
        try {
            item = itemService.find(context, UUID.fromString(id));
            EPerson eperson = context.getCurrentUser();
            qaEventService.deleteEventByEventId(id);
            qaEventDao.storeEvent(context, id, eperson, item);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete QAEvent " + id, e);
        }
    }

    @Override
    public Page<QAEventRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(QAEventRest.NAME, "findAll");
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model,
            String id, Patch patch) throws SQLException, AuthorizeException {
        QAEvent qaEvent = qaEventService.findEventByEventId(id);
        resourcePatch.patch(context, qaEvent, patch.getOperations());
    }

    @Override
    public Class<QAEventRest> getDomainClass() {
        return QAEventRest.class;
    }

}
