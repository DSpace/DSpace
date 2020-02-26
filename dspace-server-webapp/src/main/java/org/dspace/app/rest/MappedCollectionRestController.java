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

import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.MethodNotAllowedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController takes care of the creation and deletion of MappedCollections.
 * This class will typically receive a UUID that resolves to an Item and it'll perform logic on its collections
 */
@RestController
@RequestMapping("/api/core/items" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/mappedCollections")
public class MappedCollectionRestController {

    private static final Logger log = Logger.getLogger(MappedCollectionRestController.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    Utils utils;

    /**
     * This method will add an Item to a Collection. The Collection object is encapsulated in the request due to the
     * text/uri-list consumer and the Item UUID comes from the path in the URL
     *
     * curl -X POST http://<dspace.server.url>/api/core/item/{uuid}/mappedCollections
     *  -H "Content-Type:text/uri-list"
     *  --data $'https://{url}/rest/api/core/collections/{uuid}'
     *
     * Example:
     * <pre>
     * {@code
     * curl -X POST http://<dspace.server.url>/api/core/item/{uuid}/mappedCollections
     *  -H "Content-Type:text/uri-list"
     *  --data $'https://{url}/rest/api/core/collections/506a7e54-8d7c-4d5b-8636-d5f6411483de'
     * }
     * </pre>
     * @param uuid      The UUID of the Item that'll be added to a collection
     * @param response  The HttpServletResponse
     * @param request   The HttpServletRequest that will contain the UUID of the Collection in its body
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, consumes = {"text/uri-list"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createCollectionToItemRelation(@PathVariable UUID uuid,
                                               HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);

        List<DSpaceObject> listDsoFoundInRequest
                = utils.constructDSpaceObjectList(context, utils.getStringListFromRequest(request));

        if (listDsoFoundInRequest.size() < 1) {
            throw new UnprocessableEntityException("Not a valid collection uuid.");
        }

        for (DSpaceObject dso : listDsoFoundInRequest) {

            Item item = itemService.find(context, uuid);
            if (dso != null && dso.getType() == COLLECTION && item != null) {
                this.checkIfItemIsTemplate(item);
                Collection collectionToMapTo = (Collection) dso;
                this.checkIfOwningCollection(item, collectionToMapTo.getID());

                collectionService.addItem(context, collectionToMapTo, item);
                collectionService.update(context, collectionToMapTo);
                itemService.update(context, item);
            } else {
                throw new UnprocessableEntityException("Not a valid collection or item uuid.");
            }
        }

        context.commit();
    }

    /**
     * This method will delete a Collection to Item relation. It will remove an Item with UUID given in the request
     * URL from the Collection with UUID given in the request URL.
     *
     * curl -X DELETE http://<dspace.server.url>/api/core/item/{uuid}/mappedCollections/{collectionUuid}
     *
     * Example:
     * <pre>
     * {@code
     * curl -X DELETE http://<dspace.server.url>/api/core/item/{uuid}/mappedCollections/{collectionUuid}
     * }
     * </pre>
     *
     * @param uuid              The UUID of the Item
     * @param collectionUuid    The UUID of the Collection
     * @param response          The HttpServletReponse
     * @param request           The HttpServletRequest
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException          If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{collectionUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollectionToItemRelation(@PathVariable UUID uuid, @PathVariable UUID collectionUuid,
                                               HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);
        Item item = itemService.find(context, uuid);
        if (collection != null && item != null) {
            this.checkIfItemIsTemplate(item);
            UUID owningCollectionUuid = item.getOwningCollection().getID();
            this.checkIfOwningCollection(item, collectionUuid);
            if (collection.getID() != owningCollectionUuid && item.getCollections().contains(collection)) {
                collectionService.removeItem(context, collection, item);
                collectionService.update(context, collection);
                itemService.update(context, item);
                context.commit();
            }
        } else {
            throw new UnprocessableEntityException("Not a valid collection or item uuid.");
        }

    }

    private void checkIfItemIsTemplate(Item item) {
        if (item.getTemplateItemOf() != null) {
            throw new MethodNotAllowedException("Given item is a template item.");
        }
    }

    private void checkIfOwningCollection(Item item, UUID collectionID) {
        if (item.getOwningCollection().getID().equals(collectionID)) {
            throw new UnprocessableEntityException("Collection given same as owningCollection of item.");
        }
    }
}
