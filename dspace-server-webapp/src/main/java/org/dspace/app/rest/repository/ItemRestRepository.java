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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(ItemRest.CATEGORY + "." + ItemRest.NAME)
public class ItemRestRepository extends DSpaceObjectRestRepository<Item, ItemRest> {

    private static final Logger log = Logger.getLogger(ItemRestRepository.class);

    private static final String[] COPYVIRTUAL_ALL = {"all"};
    private static final String[] COPYVIRTUAL_CONFIGURED = {"configured"};
    private static final String REQUESTPARAMETER_COPYVIRTUALMETADATA = "copyVirtualMetadata";

    @Autowired
    MetadataConverter metadataConverter;

    @Autowired
    BundleService bundleService;

    @Autowired
    WorkspaceItemService workspaceItemService;

    @Autowired
    ItemService itemService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    InstallItemService installItemService;

    @Autowired
    RelationshipService relationshipService;

    @Autowired
    RelationshipTypeService relationshipTypeService;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    public ItemRestRepository(ItemService dsoService) {
        super(dsoService);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public ItemRest findOne(Context context, UUID id) {
        Item item = null;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            return null;
        }
        if (item.getTemplateItemOf() != null) {
            throw new DSpaceBadRequestException("Item with id: " + id + " is a template item.");
        }
        return converter.toRest(item, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ItemRest> findAll(Context context, Pageable pageable) {
        try {
            long total = itemService.countTotal(context);
            Iterator<Item> it = itemService.findAll(context, pageable.getPageSize(),
                Math.toIntExact(pageable.getOffset()));
            List<Item> items = new ArrayList<>();
            while (it.hasNext()) {
                items.add(it.next());
            }
            return converter.toRestPage(items, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ITEM', #patch)")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }

    @Override
    public Class<ItemRest> getDomainClass() {
        return ItemRest.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID id) throws AuthorizeException {
        String[] copyVirtual =
            requestService.getCurrentRequest().getServletRequest()
                .getParameterValues(REQUESTPARAMETER_COPYVIRTUALMETADATA);

        Item item = null;
        try {
            item = itemService.find(context, id);
            if (item == null) {
                throw new ResourceNotFoundException(ItemRest.CATEGORY + "." + ItemRest.NAME +
                    " with id: " + id + " not found");
            }
            if (itemService.isInProgressSubmission(context, item)) {
                throw new UnprocessableEntityException("The item cannot be deleted. "
                    + "It's part of a in-progress submission.");
            }
            if (item.getTemplateItemOf() != null) {
                throw new UnprocessableEntityException("The item cannot be deleted. "
                    + "It's a template for a collection");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            deleteMultipleRelationshipsCopyVirtualMetadata(context, copyVirtual, item);
            itemService.delete(context, item);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Deletes relationships of an item which need virtual metadata to be copied to actual metadata
     * This ensures a delete call is used which can copy the metadata prior to deleting the item
     *
     * @param context     The relevant DSpace context
     * @param copyVirtual The value(s) of the copyVirtualMetadata parameter
     * @param item        The item to be deleted
     */
    private void deleteMultipleRelationshipsCopyVirtualMetadata(Context context, String[] copyVirtual, Item item)
        throws SQLException, AuthorizeException {

        if (copyVirtual == null || copyVirtual.length == 0) {
            // Don't delete nor copy any metadata here if the "copyVirtualMetadata" parameter wasn't passed. The
            // relationships not deleted in this method will be deleted implicitly by the this.delete() method
            // without copying the metadata anyway.
            return;
        }
        if (Objects.deepEquals(copyVirtual, COPYVIRTUAL_ALL)) {
            // Option 1: Copy all virtual metadata of this item to its related items. Iterate over all of the item's
            //           relationships and copy their data.
            for (Relationship relationship : relationshipService.findByItem(context, item)) {
                deleteRelationshipCopyVirtualMetadata(item, relationship);
            }
        } else if (Objects.deepEquals(copyVirtual, COPYVIRTUAL_CONFIGURED)) {
            // Option 2: Use a configuration value to determine if virtual metadata needs to be copied. Iterate over all
            //           of the item's relationships and copy their data depending on the
            //           configuration.
            for (Relationship relationship : relationshipService.findByItem(context, item)) {
                relationshipService.delete(obtainContext(), relationship);
            }
        } else {
            // Option 3: Copy the virtual metadata of selected types of this item to its related items. The copyVirtual
            //           array should only contain numeric values at this point. These values are used to select the
            //           types. Iterate over all selected types and copy the corresponding values to this item's
            //           relatives.
            List<Integer> relationshipIds = parseVirtualMetadataTypes(copyVirtual);
            for (Integer relationshipId : relationshipIds) {
                RelationshipType relationshipType = relationshipTypeService.find(context, relationshipId);
                for (Relationship relationship : relationshipService
                    .findByItemAndRelationshipType(context, item, relationshipType)) {

                    deleteRelationshipCopyVirtualMetadata(item, relationship);
                }
            }
        }
    }

    private List<Integer> parseVirtualMetadataTypes(String[] copyVirtual) {
        List<Integer> types = new ArrayList<>();
        for (String typeString : copyVirtual) {
            if (!StringUtils.isNumeric(typeString)) {
                throw new DSpaceBadRequestException("parameter " + REQUESTPARAMETER_COPYVIRTUALMETADATA
                    + " should only contain a single value '" + COPYVIRTUAL_ALL[0] + "', '" + COPYVIRTUAL_CONFIGURED[0]
                    + "' or a list of numbers.");
            }
            types.add(Integer.parseInt(typeString));
        }
        return types;
    }

    /**
     * Deletes the relationship while copying the virtual metadata to the item which is **NOT** deleted
     *
     * @param itemToDelete         The item to be deleted
     * @param relationshipToDelete The relationship to be deleted
     */
    private void deleteRelationshipCopyVirtualMetadata(Item itemToDelete, Relationship relationshipToDelete)
        throws SQLException, AuthorizeException {

        boolean copyToLeft = relationshipToDelete.getRightItem().equals(itemToDelete);
        boolean copyToRight = relationshipToDelete.getLeftItem().equals(itemToDelete);

        if (copyToLeft && copyToRight) {
            //The item has a relationship with itself. Copying metadata is useless since the item will be deleted
            copyToLeft = false;
            copyToRight = false;
        }

        relationshipService.delete(obtainContext(), relationshipToDelete, copyToLeft, copyToRight);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected ItemRest createAndReturn(Context context) throws AuthorizeException, SQLException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        String owningCollectionUuidString = req.getParameter("owningCollection");
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            itemRest = mapper.readValue(input, ItemRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        if (itemRest.getInArchive() == false) {
            throw new DSpaceBadRequestException("InArchive attribute should not be set to false for the create");
        }
        UUID owningCollectionUuid = UUIDUtils.fromString(owningCollectionUuidString);
        Collection collection = collectionService.find(context, owningCollectionUuid);
        if (collection == null) {
            throw new DSpaceBadRequestException("The given owningCollection parameter is invalid: "
                + owningCollectionUuid);
        }
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = workspaceItem.getItem();
        item.setArchived(true);
        item.setOwningCollection(collection);
        item.setDiscoverable(itemRest.getDiscoverable());
        item.setLastModified(itemRest.getLastModified());
        metadataConverter.setMetadata(context, item, itemRest.getMetadata());

        Item itemToReturn = installItemService.installItem(context, workspaceItem);

        return converter.toRest(itemToReturn, Projection.DEFAULT);
    }

    @Override
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'WRITE')")
    protected ItemRest put(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid,
                           JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = null;
        try {
            itemRest = mapper.readValue(jsonNode.toString(), ItemRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        Item item = itemService.find(context, uuid);
        if (item == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + uuid + " not found");
        }

        if (StringUtils.equals(uuid.toString(), itemRest.getId())) {
            metadataConverter.setMetadata(context, item, itemRest.getMetadata());
        } else {
            throw new IllegalArgumentException("The UUID in the Json and the UUID in the url do not match: "
                + uuid + ", "
                + itemRest.getId());
        }
        return converter.toRest(item, Projection.DEFAULT);
    }

    /**
     * Method to add a bundle to an item
     *
     * @param context    The context
     * @param item       The item to which the bundle has to be added
     * @param bundleRest The bundleRest that needs to be added to the item
     * @return The added bundle
     */
    public Bundle addBundleToItem(Context context, Item item, BundleRest bundleRest)
        throws SQLException, AuthorizeException {
        if (item.getBundles(bundleRest.getName()).size() > 0) {
            throw new DSpaceBadRequestException("The bundle name already exists in the item");
        }

        Bundle bundle = bundleService.create(context, item, bundleRest.getName());

        metadataConverter.setMetadata(context, bundle, bundleRest.getMetadata());
        bundle.setName(context, bundleRest.getName());

        context.commit();

        return bundle;
    }

    @Override
    protected ItemRest createAndReturn(Context context, List<String> stringList)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        Item item = uriListHandlerService.handle(context, req, stringList, Item.class);
        return converter.toRest(item, Projection.DEFAULT);
    }
}
