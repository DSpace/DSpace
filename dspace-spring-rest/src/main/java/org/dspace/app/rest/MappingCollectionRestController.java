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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.MappingCollectionRestWrapper;
import org.dspace.app.rest.model.hateoas.MappingCollectionResourceWrapper;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/items/" +
    "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/mappingCollections")
public class MappingCollectionRestController {

    private static final Logger log = Logger.getLogger(MappingCollectionRestController.class);


    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    Utils utils;

    @Autowired
    private HalLinkService halLinkService;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    public MappingCollectionResourceWrapper retrieve(@PathVariable UUID uuid, HttpServletResponse response,
                                                     HttpServletRequest request, Pageable pageable)
        throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Item item = itemService.find(context, uuid);
        List<Collection> collections = item.getCollections();
        UUID owningCollectionUuid = item.getOwningCollection().getID();
        List<CollectionRest> mappingCollectionRest = new LinkedList<>();
        for (Collection collection : collections) {
            if (collection.getID() != owningCollectionUuid) {
                mappingCollectionRest.add(collectionConverter.fromModel(collection));
            }
        }

        MappingCollectionRestWrapper mappingCollectionRestWrapper = new MappingCollectionRestWrapper();
        mappingCollectionRestWrapper.setMappingCollectionRestList(mappingCollectionRest);
        mappingCollectionRestWrapper.setItem(item);
        MappingCollectionResourceWrapper mappingCollectionResourceWrapper = new MappingCollectionResourceWrapper(
            mappingCollectionRestWrapper, utils, pageable);


        halLinkService.addLinks(mappingCollectionResourceWrapper);

        return mappingCollectionResourceWrapper;

    }

    @RequestMapping(method = RequestMethod.POST, value = "/{collectionUuid}")
    public void createCollectionToItemRelation(@PathVariable UUID uuid, @PathVariable UUID collectionUuid,
                                               HttpServletResponse response, HttpServletRequest request)
        throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);
        Item item = itemService.find(context, uuid);
        if (collection != null && item != null) {
            collectionService.addItem(context, collection, item);
            collectionService.update(context, collection);
            itemService.update(context, item);
            context.commit();
        }

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{collectionUuid}")
    public void deleteCollectionToItemRelation(@PathVariable UUID uuid, @PathVariable UUID collectionUuid,
                                               HttpServletResponse response, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);
        Item item = itemService.find(context, uuid);
        if (collection != null && item != null) {
            UUID owningCollectionUuid = item.getOwningCollection().getID();
            if (collection.getID() != owningCollectionUuid && item.getCollections().contains(collection)) {
                collectionService.removeItem(context, collection, item);
                collectionService.update(context, collection);
                itemService.update(context, item);
                context.commit();
            }
        }

    }
}
