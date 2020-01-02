/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.dspace.core.Constants.COLLECTION;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
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

/**
 * This controller will handle all the incoming calls on the api/code/items/{uuid}/owningCollection endpoint
 * where the uuid corresponds to the item of which you want to edit the owning collection.
 */
@RestController
@RequestMapping("/api/core/items" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/owningCollection")
public class ItemOwningCollectionUpdateRestController {

    @Autowired
    ItemService itemService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ConverterService converter;

    @Autowired
    Utils utils;

    /**
     * This method will update the owning collection of the item that correspond to the provided item uuid, effectively
     * moving the item to the new collection.
     *
     * @param uuid The UUID of the item that will be moved
     * @param response The response object
     * @param request  The request object
     * @return The wrapped resource containing the new owning collection or null when the item was not moved
     * @throws SQLException       If something goes wrong
     * @throws IOException        If something goes wrong
     * @throws AuthorizeException If the user is not authorized to perform the move action
     */
    @RequestMapping(method = RequestMethod.PUT, consumes = {"text/uri-list"})
    @PreAuthorize("hasPermission(#uuid, 'ITEM','WRITE')")
    @PostAuthorize("returnObject != null")
    public CollectionRest move(@PathVariable UUID uuid, HttpServletResponse response,
                               HttpServletRequest request)
            throws SQLException, IOException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        List<DSpaceObject> dsoList = utils.constructDSpaceObjectList(context, utils.getStringListFromRequest(request));

        if (dsoList.size() != 1 || dsoList.get(0).getType() != COLLECTION) {
            throw new UnprocessableEntityException("The collection doesn't exist " +
                                                           "or the data cannot be resolved to a collection.");
        }

        Collection targetCollection = performItemMove(context, uuid, (Collection) dsoList.get(0));

        if (targetCollection == null) {
            return null;
        }
        return converter.toRest(targetCollection, Projection.DEFAULT);

    }

    /**
     * This method will move the item from the current collection to the target collection
     *
     * @param context           The context object
     * @param item              The item to be moved
     * @param currentCollection The current owning collection of the item
     * @param targetCollection  The target collection of the item
     * @return The target collection
     * @throws SQLException       If something goes wrong
     * @throws IOException        If something goes wrong
     * @throws AuthorizeException If the user is not authorized to perform the move action
     */
    private Collection moveItem(final Context context, final Item item, final Collection currentCollection,
                                final Collection targetCollection)
            throws SQLException, IOException, AuthorizeException {
        itemService.move(context, item, currentCollection, targetCollection);
        //Necessary because Controller does not pass through general RestResourceController, and as such does not do its
        //  commit in DSpaceRestRepository.createAndReturn() or similar
        context.commit();

        return context.reloadEntity(targetCollection);
    }

    /**
     * This method will perform the item move based on the provided item uuid and the target collection
     *
     * @param context          The context Object
     * @param itemUuid         The uuid of the item to be moved
     * @param targetCollection The target collection
     * @return The new owning collection of the item when authorized or null when not authorized
     * @throws SQLException       If something goes wrong
     * @throws IOException        If something goes wrong
     * @throws AuthorizeException If the user is not authorized to perform the move action
     */
    private Collection performItemMove(final Context context, final UUID itemUuid, final Collection targetCollection)
            throws SQLException, IOException, AuthorizeException {

        Item item = itemService.find(context, itemUuid);

        if (item == null) {
            throw new ResourceNotFoundException("Item with id: " + itemUuid + " not found");
        }
        if (!(item.isArchived() || item.isWithdrawn())) {
            throw new DSpaceBadRequestException("Only archived or withdrawn items can be moved between collections");
        }
        if (targetCollection.equals(item.getOwningCollection())) {
            throw new DSpaceBadRequestException("The provided collection is already the owning collection");
        }

        Collection currentCollection = item.getOwningCollection();

        if (authorizeService.authorizeActionBoolean(context, currentCollection, Constants.ADMIN)) {

            return moveItem(context, item, currentCollection, targetCollection);
        }

        return null;
    }

}
