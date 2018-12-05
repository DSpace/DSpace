/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/items/" +
        "{itemUuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12" +
        "}}/owningCollection/move")
public class ItemOwningCollectionUpdateRestController {

    @Autowired
    ItemService itemService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    CollectionConverter converter;

    @RequestMapping(method = RequestMethod.PUT, value = "/{targetUuid}")
    @PreAuthorize("hasPermission(#itemUuid, 'ITEM','WRITE') && hasPermission(#targetUuid,'COLLECTION','ADD')")
    @PostAuthorize("returnObject != null")
    public CollectionRest move(@PathVariable UUID itemUuid, HttpServletResponse response,
                               HttpServletRequest request, @PathVariable UUID targetUuid)
            throws SQLException, IOException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        Collection targetCollection = performItemMove(context, itemUuid, targetUuid);

        if (targetCollection == null) {
            return null;
        }
        return converter.fromModel(targetCollection);

    }

    private Collection moveItem(final Context context, final Item item, final Collection currentCollection,
                                final Collection targetCollection)
            throws SQLException, IOException, AuthorizeException {
        itemService.move(context, item, currentCollection, targetCollection);
        context.commit();

        return context.reloadEntity(targetCollection);
    }

    private Collection performItemMove(final Context context, final UUID itemUuid, final UUID targetUuid)
            throws SQLException, IOException, AuthorizeException {

        Item item = itemService.find(context, itemUuid);

        if (item == null) {
            throw new ResourceNotFoundException("Item with id: " + itemUuid + " not found");
        }

        Collection currentCollection = item.getOwningCollection();

        if (authorizeService.authorizeActionBoolean(context, currentCollection, Constants.ADMIN)) {
            Collection targetCollection = collectionService.find(context, targetUuid);
            return moveItem(context, item, currentCollection, targetCollection);
        }

        return null;
    }

}
