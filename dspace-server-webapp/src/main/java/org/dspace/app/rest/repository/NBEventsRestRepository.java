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

import org.dspace.app.nbevent.dao.NBEventsDao;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

@Component(NBEventRest.CATEGORY + "." + NBEventRest.NAME)
public class NBEventsRestRepository extends DSpaceRestRepository<NBEventRest, String> {

    @Autowired
    private NBEventService nbEventService;

    @Autowired
    private NBEventsDao nbEventDao;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ResourcePatch<NBEvent> resourcePatch;

    private Logger log = org.slf4j.LoggerFactory.getLogger(NBEventsRestRepository.class);

    @Override
    @PreAuthorize("permitAll()")
    public NBEventRest findOne(Context context, String id) {
        NBEvent nbEvent = nbEventService.findEventByEventId(context, id);
        if (nbEvent == null) {
            return null;
        }
        return converter.toRest(nbEvent, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByTopic")
    @PreAuthorize("permitAll()")
    public Page<NBEventRest> findByTopic(Context context, @RequestParam String topic, Pageable pageable) {
        List<NBEvent> nbEvents = null;
        Long count = 0L;
        nbEvents = nbEventService.findEventsByTopicAndPage(context, topic, pageable.getOffset(),
                pageable.getPageSize());
        count = nbEventService.countEventsByTopic(context, topic);
        if (nbEvents == null) {
            return null;
        }
        return converter.toRestPage(nbEvents, pageable, count, utils.obtainProjection());
    }

    @Override
    protected void delete(Context context, String id) throws AuthorizeException {
        Item item;
        try {
            item = itemService.find(context, UUID.fromString(id));
            EPerson eperson = context.getCurrentUser();
            NBEvent nbEvent = nbEventService.deleteEventByEventId(context, id);
            if (nbEvent != null) {
                nbEventDao.storeEvent(context, nbEvent.getEventId(), eperson, item);
            }
        } catch (SQLException e) {
            log.error("SQL Exception error", e);
        }
    }

    @Override
    public Page<NBEventRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(NBEventRest.NAME, "findAll");
    }

    @Override
    @PreAuthorize("permitAll()")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model,
            String id, Patch patch) throws SQLException, AuthorizeException {
        NBEvent nbEvent = nbEventService.findEventByEventId(context, id);
        resourcePatch.patch(context, nbEvent, patch.getOperations());
    }

    @Override
    public Class<NBEventRest> getDomainClass() {
        return NBEventRest.class;
    }

}
