package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.BulkEditMetadataValue;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.util.RelationshipUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Entity;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkEditImportServiceImpl implements BulkEditImportService {
    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected BulkEditCacheService bulkEditCacheService;

    @Autowired
    protected EntityService entityService;

    @Autowired
    protected WorkspaceItemService workspaceItemService;

    @Autowired
    protected InstallItemService installItemService;

    @Override
    public void importBulkEditChange(Context c, BulkEditChange bechange, boolean useCollectionTemplate,
                                     boolean useWorkflow, boolean workflowNotify)
        throws SQLException, AuthorizeException, IOException, MetadataImportException, WorkflowException {
        if (bechange.isNewItem()) {
            createNewItem(c, bechange, useCollectionTemplate, useWorkflow, workflowNotify);
        } else {
            boolean deleted = performActions(c, bechange);
            if (deleted) {
                return;
            }
            updateCollections(c, bechange);
            updateMetadata(c, bechange);
        }
    }

    protected void createNewItem(Context c, BulkEditChange bechange, boolean useCollectionTemplate,
                                 boolean useWorkflow, boolean workflowNotify)
        throws SQLException, AuthorizeException, MetadataImportException, WorkflowException, IOException {
        // Create the item
        Collection collection = bechange.getNewOwningCollection();
        WorkspaceItem wsItem = workspaceItemService.create(c, collection, useCollectionTemplate);
        Item item = wsItem.getItem();

        // Add the metadata to the item
        for (BulkEditMetadataValue dcv : bechange.getAdds()) {
            if (!StringUtils.equals(dcv.getSchema(), MetadataSchemaEnum.RELATION.getName())) {
                itemService.addMetadata(c, item, dcv.getSchema(),
                    dcv.getElement(),
                    dcv.getQualifier(),
                    dcv.getLanguage(),
                    dcv.getValue(),
                    dcv.getAuthority(),
                    dcv.getConfidence());
            }
        }
        //Add relations after all metadata has been processed
        for (BulkEditMetadataValue dcv : bechange.getAdds()) {
            if (StringUtils.equals(dcv.getSchema(), MetadataSchemaEnum.RELATION.getName())) {
                addRelationship(c, item, dcv.getElement(), dcv.getValue());
            }
        }


        // Should the workflow be used?
        if (useWorkflow) {
            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
            if (workflowNotify) {
                workflowService.start(c, wsItem);
            } else {
                workflowService.startWithoutNotify(c, wsItem);
            }
        } else {
            // Install the item
            installItemService.installItem(c, wsItem);
        }

        // Add to extra collections
        if (CollectionUtils.isNotEmpty(bechange.getNewMappedCollections())) {
            for (Collection extra : bechange.getNewMappedCollections()) {
                collectionService.addItem(c, extra, item);
            }
        }

        bechange.setItem(item);
    }

    protected boolean performActions(Context c, BulkEditChange bechange)
        throws SQLException, AuthorizeException, IOException {
        Item item = bechange.getItem();

        if (bechange.isDeleted()) {
            itemService.delete(c, item);
            return true;
        }

        if (bechange.isWithdrawn()) {
            if (!item.isWithdrawn()) {
                itemService.withdraw(c, item);
            }
            return false;
        }

        if (bechange.isReinstated()) {
            if (item.isWithdrawn()) {
                itemService.reinstate(c, item);
            }
            return false;
        }

        return false;
    }

    protected void updateCollections(Context c, BulkEditChange bechange)
        throws SQLException, AuthorizeException, IOException {
        Item item = bechange.getItem();

        // Remove old mapped collections
        for (Collection collection : bechange.getOldMappedCollections()) {
            collectionService.removeItem(c, collection, item);
        }

        // Add to new owned collection
        if (bechange.getNewOwningCollection() != null) {
            collectionService.addItem(c, bechange.getNewOwningCollection(), item);
            item.setOwningCollection(bechange.getNewOwningCollection());
            itemService.update(c, item);
        }

        // Remove from old owned collection (if still a member)
        if (bechange.getOldOwningCollection() != null) {
            boolean found = false;
            for (Collection collection : item.getCollections()) {
                if (collection.getID().equals(bechange.getOldOwningCollection().getID())) {
                    found = true;
                }
            }

            if (found) {
                collectionService.removeItem(c, bechange.getOldOwningCollection(), item);
            }
        }

        // Add to new mapped collections
        for (Collection collection : bechange.getNewMappedCollections()) {
            collectionService.addItem(c, collection, item);
        }
    }

    protected void updateMetadata(Context c, BulkEditChange bechange)
        throws SQLException, AuthorizeException, MetadataImportException {
        Item item = bechange.getItem();

        // Update the item if it has changed
        if ((!bechange.getAdds().isEmpty()) || (!bechange.getRemoves().isEmpty())) {
            // Get the complete list of what values should now be in that element
            Map<String, List<BulkEditMetadataValue>> metadataValues = getMetadataByField(bechange.getComplete());
            for (String key : metadataValues.keySet()) {
                List<BulkEditMetadataValue> list = metadataValues.get(key);
                if (!list.isEmpty()) {
                    String schema = list.get(0).getSchema();
                    String element = list.get(0).getElement();
                    String qualifier = list.get(0).getQualifier();
                    String language = list.get(0).getLanguage();
                    List<String> values = list.stream()
                        .map(BulkEditMetadataValue::getValue).collect(Collectors.toList());
                    List<String> authorities = list.stream()
                        .map(BulkEditMetadataValue::getAuthority).collect(Collectors.toList());
                    List<Integer> confidences = list.stream()
                        .map(BulkEditMetadataValue::getConfidence).collect(Collectors.toList());

                    if (StringUtils.equals(schema, MetadataSchemaEnum.RELATION.getName())) {
                        List<RelationshipType> relationshipTypeList = relationshipTypeService
                            .findByLeftwardOrRightwardTypeName(c, element);
                        for (RelationshipType relationshipType : relationshipTypeList) {
                            for (Relationship relationship : relationshipService
                                .findByItemAndRelationshipType(c, item, relationshipType)) {
                                relationshipService.delete(c, relationship);
                                relationshipService.update(c, relationship);
                            }
                        }
                        addRelationships(c, item, element, values);
                    } else {
                        itemService.clearMetadata(c, item, schema, element, qualifier, language);
                        itemService.addMetadata(c, item, schema, element, qualifier,
                            language, values, authorities, confidences);
                    }
                }
            }

            itemService.update(c, item);
        }
    }

    private Map<String, List<BulkEditMetadataValue>> getMetadataByField(List<BulkEditMetadataValue> allMetadata) {
        Map<String, List<BulkEditMetadataValue>> map = new HashMap<>();
        for (BulkEditMetadataValue value : allMetadata) {
            String mdField = value.getSchema() + "." + value.getElement() + "." + value.getQualifier() + "." +
                value.getLanguage();
            if (!map.containsKey(mdField)) {
                map.put(mdField, new ArrayList<>());
            }
            map.get(mdField).add(value);
        }
        return map;
    }

    /**
     *
     * Adds multiple relationships with a matching typeName to an item.
     *
     * @param c             The relevant DSpace context
     * @param item          The item to which this metadatavalue belongs to
     * @param typeName       The element for the metadatavalue
     * @param values to iterate over
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void addRelationships(Context c, Item item, String typeName, List<String> values)
        throws SQLException, AuthorizeException,
        MetadataImportException {
        for (String value : values) {
            addRelationship(c, item, typeName, value);
        }
    }

    /**
     *
     * Creates a relationship for the given item
     *
     * @param c         The relevant DSpace context
     * @param item      The item that the relationships will be made for
     * @param typeName     The relationship typeName
     * @param value    The value for the relationship
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    private void addRelationship(Context c, Item item, String typeName, String value)
        throws SQLException, AuthorizeException, MetadataImportException {
        if (value.isEmpty()) {
            return;
        }
        boolean left = false;

        // Get entity from target reference
        Entity relationEntity = getEntity(c, value);
        // Get relationship type of entity and item
        String relationEntityRelationshipType = itemService.getMetadata(relationEntity.getItem(),
            "dspace", "entity",
            "type", Item.ANY).get(0).getValue();
        String itemRelationshipType = itemService.getMetadata(item, "dspace", "entity",
            "type", Item.ANY).get(0).getValue();

        // Get the correct RelationshipType based on typeName
        List<RelationshipType> relType = relationshipTypeService.findByLeftwardOrRightwardTypeName(c, typeName);
        RelationshipType foundRelationshipType = RelationshipUtils.matchRelationshipType(relType,
            relationEntityRelationshipType,
            itemRelationshipType, typeName);

        if (foundRelationshipType == null) {
            throw new MetadataImportException("Error during bulk edit import:" + "\n" +
                "No Relationship type found for:\n" +
                "Target type: " + relationEntityRelationshipType + "\n" +
                "Origin referer type: " + itemRelationshipType + "\n" +
                "with typeName: " + typeName);
        }

        if (foundRelationshipType.getLeftwardType().equalsIgnoreCase(typeName)) {
            left = true;
        }

        // Placeholder items for relation placing
        Item leftItem = null;
        Item rightItem = null;
        if (left) {
            leftItem = item;
            rightItem = relationEntity.getItem();
        } else {
            leftItem = relationEntity.getItem();
            rightItem = item;
        }

        // Create the relationship, appending to the end
        Relationship persistedRelationship = relationshipService.create(
            c, leftItem, rightItem, foundRelationshipType, -1, -1
        );
        relationshipService.update(c, persistedRelationship);
    }

    /**
     * Gets an existing entity from a target reference.
     *
     * @param context the context to use.
     * @param targetReference the target reference which may be a UUID, metadata reference, or rowName reference.
     * @return the entity, which is guaranteed to exist.
     * @throws MetadataImportException if the target reference is badly formed or refers to a non-existing item.
     */
    private Entity getEntity(Context context, String targetReference) throws MetadataImportException {
        Entity entity = null;
        UUID uuid = bulkEditCacheService.resolveEntityRef(context, targetReference);
        // At this point, we have a uuid, so we can get an entity
        try {
            entity = entityService.findByItemId(context, uuid);
            if (entity.getItem() == null) {
                throw new IllegalArgumentException("No item found in repository with uuid: " + uuid);
            }
            return entity;
        } catch (SQLException sqle) {
            throw new MetadataImportException("Unable to find entity using reference: " + targetReference, sqle);
        }
    }
}
