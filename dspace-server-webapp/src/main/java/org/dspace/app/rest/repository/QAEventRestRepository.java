/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.core.Constants.ITEM;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.dao.QAEventsDao;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.util.UUIDUtils;
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
    private ResourcePatch<QAEvent> resourcePatch;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public QAEventRest findOne(Context context, String id) {
        QAEvent qaEvent = qaEventService.findEventByEventId(id);
        if (qaEvent == null) {
            // check if this request is part of a patch flow
            qaEvent = (QAEvent) requestService.getCurrentRequest().getAttribute("patchedNotificationEvent");
            if (qaEvent != null && qaEvent.getEventId().contentEquals(id)) {
                return converter.toRest(qaEvent, utils.obtainProjection());
            } else {
                return null;
            }
        }
        return converter.toRest(qaEvent, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "findByTopic")
    public Page<QAEventRest> findByTopic(Context context, @Parameter(value = "topic", required = true) String topic,
        Pageable pageable) {
        List<QAEvent> qaEvents = null;
        long count = 0L;
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
    protected void delete(Context context, String eventId) throws AuthorizeException {
        Item item = findTargetItem(context, eventId);
        EPerson eperson = context.getCurrentUser();
        qaEventService.deleteEventByEventId(eventId);
        qaEventDao.storeEvent(context, eventId, eperson, item);
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

    private Item findTargetItem(Context context, String eventId) {
        QAEvent qaEvent = qaEventService.findEventByEventId(eventId);
        if (qaEvent == null) {
            return null;
        }

        try {
            return itemService.find(context, UUIDUtils.fromString(qaEvent.getTarget()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    protected QAEventRest createAndReturn(Context context, List<String> stringList)
            throws SQLException, AuthorizeException {

        if (stringList.size() < 2) {
            throw new IllegalArgumentException("the request must include at least uris for target item, " +
                "and correction type");
        }

        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        CorrectionType correctionType = uriListHandlerService.handle(context, request, List.of(stringList.get(0)),
                                                                     CorrectionType.class);
        if (Objects.isNull(correctionType)) {
            throw new UnprocessableEntityException("The given correction type in the request is not valid!");
        }

        List<DSpaceObject> list = utils.constructDSpaceObjectList(context, stringList);

        QAEvent qaEvent;
        List<Item> items = getItems(list, correctionType);
        if (correctionType.isRequiredRelatedItem()) {
            qaEvent = correctionType.createCorrection(context, items.get(0), items.get(1));
        } else {
            qaEvent = correctionType.createCorrection(context, items.get(0));
        }
        return converter.toRest(qaEvent, utils.obtainProjection());
    }

    private List<Item> getItems(List<DSpaceObject> list, CorrectionType correctionType) {
        if (correctionType.isRequiredRelatedItem()) {
            if (list.size() == 2 && list.get(0).getType() == ITEM && list.get(1).getType() == ITEM) {
                return List.of((Item) list.get(0), (Item) list.get(1));
            } else {
                throw new UnprocessableEntityException("The given items in the request were not valid!");
            }
        } else if (list.size() != 1) {
            throw new UnprocessableEntityException("The given item in the request were not valid!");
        } else {
            return List.of((Item) list.get(0));
        }
    }

    @Override
    public Class<QAEventRest> getDomainClass() {
        return QAEventRest.class;
    }

}
