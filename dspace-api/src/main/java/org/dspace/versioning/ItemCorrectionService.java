/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Request;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service to handle submission of item correction.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
@Component
public class ItemCorrectionService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemCorrectionService.class);

    private String correctionRelationshipName;

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected WorkspaceItemService workspaceItemService;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    protected ItemCorrectionProvider correctionItemProvider;

    /**
     * Create a workspaceitem by an existing Item
     * 
     * @param context
     *            the dspace context
     * @param request
     *            the request containing the details about the workspace to create
     * @param itemUUID
     *            the item UUID to use for creating the workspaceitem
     * @return    the created workspaceitem
     * @throws Exception
     */
    public WorkspaceItem createWorkspaceItemByItem(Context context, Request request, UUID itemUUID)
            throws Exception {
        WorkspaceItem wsi = null;
        Collection collection = null;
        Item item = null;

        try {
            item = itemService.find(context, itemUUID);
        } catch (SQLException e) {
            throw new SQLException("Item " + itemUUID + " is not found");
        }

        if (item != null) {
            try {
                final List<Collection> findAuthorizedOptimized = collectionService.findAuthorizedOptimized(context,
                        Constants.ADD);
                for (Collection itemCollection : item.getCollections()) {
                    if (findAuthorizedOptimized.contains(itemCollection)) {
                        collection = itemCollection;
                        break;
                    }
                }

                if (collection == null) {
                    throw new AuthorizeException("No collection suitable for submission for the current user");
                }

                wsi = correctionItemProvider.createNewItemAndAddItInWorkspace(context, collection, item);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new Exception("Item " + itemUUID + " is not found");
        }

        return wsi;
    }

    /**
     * Create a workspaceitem by an existing Item
     * 
     * @param context
     *            the dspace context
     * @param request
     *            the request containing the details about the workspace to create
     * @param itemUUID
     *            the item UUID to use for creating the workspaceitem
     * @return    the created workspaceitem
     * @throws Exception
     */
    public WorkspaceItem createWorkspaceItemAndRelationshipByItem(Context context, Request request, UUID itemUUID,
            String relationship) throws Exception {

        Item item;
        RelationshipType relationshipType;
        WorkspaceItem wsi = null;

        try {
            item = itemService.find(context, itemUUID);
        } catch (SQLException e) {
            throw new Exception("Cannot create a relationship without a given item");
        }

        if (item != null) {
            if (StringUtils.isNotBlank(relationship)) {
                List<RelationshipType> types;

                types = relationshipTypeService
                        .findByLeftwardOrRightwardTypeName(context, relationship);

                if (types != null && !types.isEmpty() && types.get(0) != null) {
                    relationshipType = types.get(0);
                    if (isRelationshipExisting(context, item, relationshipType)) {
                        throw new AuthorizeException("Relationship " + relationship +
                                " already exists for item " + itemUUID);
                    }
                    wsi = createWorkspaceItemByItem(context, request, itemUUID);
                    createRelationship(context, relationshipType, wsi.getItem(), item);
                } else {
                    throw new Exception("Relationship of type "
                            + relationship + " does not exist");
                }
            } else {
                throw new Exception("Relationship cannot be undefined");
            }

        } else {
            throw new Exception("Cannot create a relationship without a given item");
        }

        return wsi;
    }

    public boolean checkIfIsCorrectionItem(Context context, Item item) throws SQLException {
        List<RelationshipType> types = relationshipTypeService
                .findByLeftwardOrRightwardTypeName(context, getCorrectionRelationshipName());
        List<Relationship> itemRelationshiplist = relationshipService
                .findByItemAndRelationshipType(context, item, types.get(0), true);

        return itemRelationshiplist != null && itemRelationshiplist.size() > 0;
    }

    public Relationship getCorrectionItemRelationship(Context context, Item item) throws SQLException {
        List<RelationshipType> types = relationshipTypeService
                .findByLeftwardOrRightwardTypeName(context, getCorrectionRelationshipName());
        List<Relationship> itemRelationshiplist = relationshipService
                .findByItemAndRelationshipType(context, item, types.get(0), true);

        if (itemRelationshiplist != null && itemRelationshiplist.size() > 0) {
            return itemRelationshiplist.get(0);
        } else {
            return null;
        }
    }

    public void replaceCorrectionItemWithNative(Context context, XmlWorkflowItem wfi) {

        try {
            Relationship relationship = getCorrectionItemRelationship(context, wfi.getItem());
            Item nativeItem = relationship.getRightItem();
            relationshipService.delete(context, relationship);
            correctionItemProvider.updateNativeItemWithCorrection(context, wfi, wfi.getItem(), nativeItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    private boolean isRelationshipExisting(Context context, Item item, RelationshipType relationshipType) {

        if (relationshipType != null && item != null) {
            List<Relationship> relationshipList;
            try {
                relationshipList = relationshipService
                        .findByItemAndRelationshipType(context, item, relationshipType);
            } catch (SQLException e) {
                relationshipList = null;
            }
            return (relationshipList != null && !relationshipList.isEmpty());
        } else {
            throw new RuntimeException("Invalid null argument");
        }

    }

    private void createRelationship(Context context, RelationshipType relationshipType, Item leftItem, Item rightItem)
            throws AuthorizeException {
        if (relationshipType != null && leftItem != null && rightItem != null) {
            try {
                relationshipService.create(context, leftItem, rightItem, relationshipType, true);

            } catch (AuthorizeException e) {
                throw new AuthorizeException(e.getMessage());
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            throw new RuntimeException("Invalid null argument");
        }
    }

    public String getCorrectionRelationshipName() {
        return correctionRelationshipName;
    }

    public void setCorrectionRelationshipName(String correctionRelationshipName) {
        this.correctionRelationshipName = correctionRelationshipName;
    }
}
