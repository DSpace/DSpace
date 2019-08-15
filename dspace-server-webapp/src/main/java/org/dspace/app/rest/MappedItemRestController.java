/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MappedItemRestWrapper;
import org.dspace.app.rest.model.hateoas.MappedItemResourceWrapper;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
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

/**
 * This Controller will deal with request about the items that are mapped to the collection given in the request url
 * Currently this only includes a GET method for all the items that are mapped to the given collection that do not
 * have the given collection as their owning collection
 */
@RestController
@RequestMapping("/api/core/collections/" +
    "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/mappedItems")
public class MappedItemRestController {

    private static final Logger log = Logger.getLogger(MappedItemRestController.class);

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    Utils utils;

    @Autowired
    private HalLinkService halLinkService;

    /**
     * This method will retrieve a List of Item objects that are mapped to the Collection given in the URL.
     * These Item objects will be filtered out of their owning collection is the given collection, resulting in
     * returning only items that belong to a different collection but are mapped to the given one.
     * These Items are then encapsulated in a MappedItemResourceWrapper and returned
     *
     * curl -X GET http://<dspace.restUrl>/api/core/collections/{uuid}/mappedItems
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X GET http://<dspace.restUrl>/api/core/collections/8b632938-77c2-487c-81f0-e804f63e68e6/mappedItems
     * }
     * </pre>
     *
     * @param uuid      The UUID of the collection
     * @param response  The HttpServletResponse
     * @param request   The HttpServletRequest
     * @param pageable  The pagination object
     * @return
     * @throws Exception
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    public MappedItemResourceWrapper retrieve(@PathVariable UUID uuid, HttpServletResponse response,
                                               HttpServletRequest request, Pageable pageable) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        Iterator<Item> itemIterator = itemService.findByCollectionMapping(context, collection, pageable.getPageSize(),
                                                                          pageable.getOffset());
        int totalElements = itemService.countByCollectionMapping(context, collection);
        List<ItemRest> mappedItemRestList = new LinkedList<>();
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            if (item.getOwningCollection().getID() != uuid) {
                mappedItemRestList.add(itemConverter.fromModel(item));
            }
        }

        MappedItemRestWrapper mappedItemRestWrapper = new MappedItemRestWrapper();
        mappedItemRestWrapper.setMappedItemRestList(mappedItemRestList);
        mappedItemRestWrapper.setCollectionUuid(uuid);
        MappedItemResourceWrapper mappedItemResourceWrapper =
            new MappedItemResourceWrapper(mappedItemRestWrapper, utils, totalElements);

        halLinkService.addLinks(mappedItemResourceWrapper, pageable);
        return mappedItemResourceWrapper;
    }
}
