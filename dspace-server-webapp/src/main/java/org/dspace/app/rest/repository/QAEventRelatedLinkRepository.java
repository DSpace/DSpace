/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "related" subresource of a qa event.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(QAEventRest.CATEGORY + "." + QAEventRest.NAME + "." + QAEventRest.RELATED)
public class QAEventRelatedLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private QAEventService qaEventService;

    @Autowired
    private ItemService itemService;

    /**
     * Returns the item related to the qa event with the given id. This is another
     * item that should be linked to the target item as part of the correction
     *
     * @param request    the http servlet request
     * @param id         the qa event id
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return the item rest representation of the secondary item related to qa event
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public ItemRest getRelated(@Nullable HttpServletRequest request, String id, @Nullable Pageable pageable,
            Projection projection) {
        Context context = obtainContext();
        QAEvent qaEvent = qaEventService.findEventByEventId(id);
        if (qaEvent == null) {
            throw new ResourceNotFoundException("No qa event with ID: " + id);
        }
        if (qaEvent.getRelated() == null) {
            return null;
        }
        UUID itemUuid = UUID.fromString(qaEvent.getRelated());
        Item item;
        try {
            item = itemService.find(context, itemUuid);
            if (item == null) {
                throw new ResourceNotFoundException("No related item found with id : " + id);
            }
            return converter.toRest(item, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
