/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;
import org.dspace.correctiontype.service.CorrectionTypeService;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.dao.QAEventsDAO;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.qaevent.service.dto.CorrectionTypeMessageDTO;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository that handle QA events.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(QAEventRest.CATEGORY + "." + QAEventRest.PLURAL_NAME)
public class QAEventRestRepository extends DSpaceRestRepository<QAEventRest, String> {

    final static String ORDER_FIELD = "trust";

    @Autowired
    private QAEventService qaEventService;

    @Autowired
    private QAEventsDAO qaEventDao;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ResourcePatch<QAEvent> resourcePatch;

    @Autowired
    private CorrectionTypeService correctionTypeService;

    @Override
    public Page<QAEventRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(QAEventRest.NAME, "findAll");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCEEVENT', 'READ')")
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

    @SearchRestMethod(name = "findByTopic")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<QAEventRest> findByTopic(@Parameter(value = "topic", required = true) String topic,
        Pageable pageable) {
        Context context = obtainContext();
        String[] topicIdSplitted = topic.split(":", 3);
        if (topicIdSplitted.length < 2) {
            return null;
        }
        String sourceName = topicIdSplitted[0];
        String topicName = topicIdSplitted[1].replaceAll("!", "/");
        UUID target = topicIdSplitted.length == 3 ? UUID.fromString(topicIdSplitted[2]) : null;
        List<QAEvent> qaEvents = qaEventService.findEventsByTopicAndTarget(context, sourceName, topicName, target,
            pageable.getOffset(), pageable.getPageSize());
        long count = qaEventService.countEventsByTopicAndTarget(context, sourceName, topicName, target);
        if (qaEvents == null) {
            return null;
        }
        return converter.toRestPage(qaEvents, pageable, count, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCEEVENT', 'DELETE')")
    protected void delete(Context context, String id) throws AuthorizeException {
        Item item = findTargetItem(context, id);
        EPerson eperson = context.getCurrentUser();
        qaEventService.deleteEventByEventId(id);
        qaEventDao.storeEvent(context, id, eperson, item);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'QUALITYASSURANCEEVENT', 'WRITE')")
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
    protected QAEventRest createAndReturn(Context context) throws SQLException, AuthorizeException {
        ServletRequest request = getRequestService().getCurrentRequest().getServletRequest();

        String itemUUID = request.getParameter("target");
        String relatedItemUUID = request.getParameter("related");
        String correctionTypeStr = request.getParameter("correctionType");


        if (StringUtils.isBlank(correctionTypeStr) || StringUtils.isBlank(itemUUID)) {
            throw new UnprocessableEntityException("The target item and correctionType must be provided!");
        }

        Item targetItem = null;
        Item relatedItem = null;
        try {
            targetItem = itemService.find(context, UUID.fromString(itemUUID));
            relatedItem =  StringUtils.isNotBlank(relatedItemUUID) ?
                                       itemService.find(context, UUID.fromString(relatedItemUUID)) : null;
        } catch (Exception e) {
            throw new UnprocessableEntityException(e.getMessage(), e);
        }

        if (Objects.isNull(targetItem)) {
            throw new UnprocessableEntityException("The target item UUID is not valid!");
        }

        CorrectionType correctionType = correctionTypeService.findOne(correctionTypeStr);
        if (Objects.isNull(correctionType)) {
            throw new UnprocessableEntityException("The given correction type in the request is not valid!");
        }

        if (correctionType.isRequiredRelatedItem() && Objects.isNull(relatedItem)) {
            throw new UnprocessableEntityException("The given correction type in the request is not valid!");
        }

        if (!correctionType.isAllowed(context, targetItem)) {
            throw new UnprocessableEntityException("This item cannot be processed by this correction type!");
        }

        ObjectMapper mapper = new ObjectMapper();
        CorrectionTypeMessageDTO reason = null;
        try {
            reason = mapper.readValue(request.getInputStream(), CorrectionTypeMessageDTO.class);
        } catch (IOException exIO) {
            throw new UnprocessableEntityException("error parsing the body " + exIO.getMessage(), exIO);
        }

        QAEvent qaEvent;
        if (correctionType.isRequiredRelatedItem()) {
            qaEvent = correctionType.createCorrection(context, targetItem, relatedItem, reason);
        } else {
            qaEvent = correctionType.createCorrection(context, targetItem, reason);
        }
        return converter.toRest(qaEvent, utils.obtainProjection());
    }

    @Override
    public Class<QAEventRest> getDomainClass() {
        return QAEventRest.class;
    }

}
