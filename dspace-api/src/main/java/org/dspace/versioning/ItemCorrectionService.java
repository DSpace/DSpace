/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to handle submission of item correction.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemCorrectionService {

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

    @Autowired
    protected EntityTypeService entityTypeService;

    private final String correctionRelationshipName;

    public ItemCorrectionService(String correctionRelationshipName) {
        this.correctionRelationshipName = correctionRelationshipName;
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
    public WorkspaceItem createWorkspaceItemByItem(Context context, UUID itemUUID) throws Exception {
        WorkspaceItem wsi = null;
        Collection collection = null;

        Item item = itemService.find(context, itemUUID);

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
    public WorkspaceItem createWorkspaceItemAndRelationshipByItem(Context context, UUID itemUUID, String relationship)
        throws Exception {

        if (StringUtils.isBlank(relationship)) {
            throw new IllegalArgumentException("Relationship cannot be undefined");
        }

        Item item = itemService.find(context, itemUUID);
        if (item == null) {
            throw new IllegalArgumentException("Cannot create a relationship without a given item");
        }

        RelationshipType relationshipType = findRelationshipType(context, item, relationship);
        if (relationshipType == null) {
            throw new IllegalArgumentException("No relationship type found for " + relationship);
        }

        WorkspaceItem workspaceItem = createWorkspaceItemByItem(context, itemUUID);
        relationshipService.create(context, workspaceItem.getItem(), item, relationshipType, false);

        return workspaceItem;
    }

    public boolean checkIfIsCorrectionItem(Context context, Item item) throws SQLException {
        return checkIfIsCorrectionItem(context, item, getCorrectionRelationshipName());
    }

    public boolean checkIfIsCorrectionItem(Context context, Item item, String relationshipName) throws SQLException {
        RelationshipType relationshipType = findRelationshipType(context, item, relationshipName);
        if (relationshipType == null) {
            return false;
        }
        return isNotEmpty(relationshipService.findByItemAndRelationshipType(context, item, relationshipType, true));
    }

    public Relationship getCorrectionItemRelationship(Context context, Item item) throws SQLException {
        RelationshipType type = findRelationshipType(context, item, getCorrectionRelationshipName());
        if (type == null) {
            return null;
        }
        List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context, item, type, true);
        return isNotEmpty(relationships) ? relationships.get(0) : null;
    }

    public void replaceCorrectionItemWithNative(Context context, XmlWorkflowItem wfi) {

        try {
            Relationship relationship = getCorrectionItemRelationship(context, wfi.getItem());
            Item nativeItem = relationship.getRightItem();
            relationshipService.delete(context, relationship);
            correctionItemProvider.updateNativeItemWithCorrection(context, wfi, wfi.getItem(), nativeItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    private RelationshipType findRelationshipType(Context context, Item item, String relationship) throws SQLException {

        EntityType type = entityTypeService.findByItem(context, item);
        if (type == null) {
            return null;
        }

        return relationshipTypeService.findByLeftwardOrRightwardTypeName(context, relationship).stream()
            .filter(relationshipType -> type.equals(relationshipType.getLeftType())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Relationship type " + relationship + " does not exist"));
    }

    public String getCorrectionRelationshipName() {
        return correctionRelationshipName;
    }

}
