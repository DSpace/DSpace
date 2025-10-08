/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the service for processing and applying changes found in {@link BulkEditChange}
 * More documentation on the methods in {@link BulkEditService}
 *
 * Warning: This service is stateful, in that a new instance will be created every time it is requested.
 *          This is by design because the service will keep information about multiple related changes until
 *          it is done applying them all and this ensures none of the information leaks between other calls/processes.
 *          This means the service should never be Autowired and should instead be requested through the
 *          {@link BulkEditServiceFactory} wherever the call is made to parse and/or apply the changes.
 */
public class BulkEditServiceImpl implements BulkEditService {
    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected EntityService entityService;

    @Autowired
    protected WorkspaceItemService workspaceItemService;

    @Autowired
    protected InstallItemService installItemService;

    @Autowired
    protected ConfigurationService configurationService;

    protected DSpaceRunnableHandler handler;
    protected boolean useCollectionTemplate;
    protected boolean useWorkflow;
    protected boolean workflowNotify;
    protected boolean archive;

    /**
     * A map containing previously imported/updated item UUIDs, mapped to their fake UUID found in their respective
     * {@link BulkEditChange}, this way, real relationships between newly imported items can be made
     */
    protected final Map<UUID, UUID> fakeToRealUUIDMap = new HashMap<>();

    @Override
    public void applyBulkEditChanges(Context c, List<BulkEditChange> bulkEditChanges)
        throws SQLException, AuthorizeException, IOException, MetadataImportException, WorkflowException {
        c.setMode(Context.Mode.BATCH_EDIT);
        int i = 1;
        int batchSize = configurationService.getIntProperty("bulkedit.change.commit.count", 100);
        int changeCount = bulkEditChanges.size();
        int totalCommits = (int) Math.ceil((double) changeCount / batchSize);
        int commitCount = 0;
        int iAtLastCommit = 0;
        for (BulkEditChange bechange : bulkEditChanges) {
            applyBulkEditChange(c, bechange);

            if (bechange.getItem() != null) {
                c.uncacheEntity(bechange.getItem());
            }

            if (i % batchSize == 0 || i == changeCount) {
                c.commit();
                commitCount++;
                if (handler != null) {
                    handler.logInfo(String.format(
                        "Commit %d/%d: The changes in rows %s-%s have been persisted to the database",
                        commitCount,
                        totalCommits,
                        iAtLastCommit + 1,
                        i
                    ));
                }
                iAtLastCommit = i;
            }

            i++;
        }
    }

    @Override
    public void applyBulkEditChange(Context c, BulkEditChange bechange)
        throws SQLException, AuthorizeException, IOException, MetadataImportException, WorkflowException {
        if (bechange.isNewItem()) {
            createNewItem(c, bechange);
        } else {
            boolean deleted = performActions(c, bechange);
            if (deleted) {
                return;
            }
            updateCollections(c, bechange);
            updateMetadata(c, bechange);
        }
    }

    protected void createNewItem(Context c, BulkEditChange bechange)
        throws SQLException, AuthorizeException, MetadataImportException, WorkflowException, IOException {
        // Create the item
        Collection collection = bechange.getNewOwningCollection();
        WorkspaceItem wsItem = workspaceItemService.create(c, collection, useCollectionTemplate);
        Item item = wsItem.getItem();

        // Add the metadata to the item
        for (BulkEditMetadataValue dcv : getBulkEditMetadataValueSorted(bechange.getAdds())) {
            if (!isRelationship(dcv)) {
                addMetadata(c, item, dcv);
            }
        }
        //Add relations after all metadata has been processed
        for (BulkEditMetadataValue dcv : bechange.getAdds()) {
            if (isRelationship(dcv)) {
                addRelationship(c, item, dcv);
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
        } else if (archive) {
            // Add provenance info
            String provenance = installItemService.getSubmittedByProvenanceMessage(c, wsItem.getItem());
            itemService.addMetadata(c, item, MetadataSchemaEnum.DC.getName(),
                "description", "provenance", "en", provenance);
            // Install the item
            installItemService.installItem(c, wsItem);
        }

        // Add to extra collections
        if (CollectionUtils.isNotEmpty(bechange.getNewMappedCollections())) {
            for (Collection extra : bechange.getNewMappedCollections()) {
                collectionService.addItem(c, extra, item);
            }
        }

        fakeToRealUUIDMap.put(bechange.getUuid(), item.getID());
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
            Map<String, List<BulkEditMetadataValue>> metadataValuesToAddOrKeep =
                getMetadataByField(getBulkEditMetadataValueSorted(bechange.getComplete()));
            Map<String, List<BulkEditMetadataValue>> metadataValuesToRemove =
                getMetadataByField(bechange.getRemoves());

            for (String key : metadataValuesToAddOrKeep.keySet()) {
                List<BulkEditMetadataValue> list = metadataValuesToAddOrKeep.get(key);
                if (!list.isEmpty()) {
                    String schema = list.get(0).getSchema();
                    String element = list.get(0).getElement();
                    String qualifier = list.get(0).getQualifier();
                    String language = list.get(0).getLanguage();

                    clearMetadataAndRelationships(c, item, schema, element, qualifier, language);
                    for (BulkEditMetadataValue dcv : list) {
                        if (isRelationship(dcv)) {
                            addRelationship(c, item, dcv);
                        } else {
                            addMetadata(c, item, dcv);
                        }
                    }
                }
            }
            for (String key : metadataValuesToRemove.keySet()) {
                if (!metadataValuesToAddOrKeep.containsKey(key)) {
                    List<BulkEditMetadataValue> list = metadataValuesToRemove.get(key);
                    if (!list.isEmpty()) {
                        String schema = list.get(0).getSchema();
                        String element = list.get(0).getElement();
                        String qualifier = list.get(0).getQualifier();
                        String language = list.get(0).getLanguage();

                        clearMetadataAndRelationships(c, item, schema, element, qualifier, language);
                    }
                }
            }

            itemService.update(c, item);
        }
    }

    protected void clearMetadataAndRelationships(Context c, Item item, String schema, String element, String qualifier,
                                                 String language) throws SQLException, AuthorizeException {
        itemService.clearMetadata(c, item, schema, element, qualifier, language);
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
        }
    }

    protected void addMetadata(Context c, Item item, BulkEditMetadataValue dcv)
        throws SQLException, AuthorizeException, MetadataImportException {
        itemService.addMetadata(c, item, dcv.getSchema(),
            dcv.getElement(),
            dcv.getQualifier(),
            dcv.getLanguage(),
            dcv.getValue(),
            dcv.getAuthority(),
            dcv.getConfidence());
    }

    protected boolean isRelationship(BulkEditMetadataValue dcv) {
        return StringUtils.equals(dcv.getSchema(), MetadataSchemaEnum.RELATION.getName());
    }

    protected Map<String, List<BulkEditMetadataValue>> getMetadataByField(List<BulkEditMetadataValue> allMetadata) {
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
     * Creates a relationship for the given item
     *
     * @param c         The relevant DSpace context
     * @param item      The item that the relationships will be made for
     * @param dcv       Metadata value changes to create the relationship from
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    protected void addRelationship(Context c, Item item, BulkEditMetadataValue dcv)
        throws SQLException, AuthorizeException, MetadataImportException {
        addRelationship(c, item, dcv.getElement(), dcv.getValue());
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
    protected void addRelationship(Context c, Item item, String typeName, String value)
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
     * Sort a list of {@link BulkEditMetadataValue} to put essential values first
     * dspace.entity.type is essential because it might influence how other imported values should behave
     */
    protected List<BulkEditMetadataValue> getBulkEditMetadataValueSorted(
        List<BulkEditMetadataValue> bulkEditMetadataValues
    ) {
        return bulkEditMetadataValues.stream().sorted((o1, o2) -> {
            boolean isO1EntityType = isEntityTypeMetadata(o1);
            boolean isO2EntityType = isEntityTypeMetadata(o2);

            if (isO1EntityType && !isO2EntityType) {
                return -1;
            }
            if (!isO1EntityType && isO2EntityType) {
                return 1;
            }
            return 0;
        }).collect(Collectors.toList());
    }

    /**
     * Check if a {@link BulkEditMetadataValue} is an entity type value
     */
    protected boolean isEntityTypeMetadata(BulkEditMetadataValue dcv) {
        return dcv.getSchema().equals("dspace") && dcv.getElement().equals("entity") && dcv.getQualifier() != null &&
            dcv.getQualifier().equals("type");
    }

    /**
     * Gets an existing entity from a target reference.
     *
     * @param context the context to use.
     * @param value the target reference which is expected to be a UUID
     * @return the entity, which is guaranteed to exist.
     * @throws MetadataImportException if the target reference is badly formed or refers to a non-existing item.
     */
    protected Entity getEntity(Context context, String value) throws MetadataImportException {
        Entity entity;
        UUID uuid;
        try {
            uuid = UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new MetadataImportException("Relationship reference was expected to be a UUID: " + value, e);
        }
        // If the reference is a fake UUID, it refers to an item that's been created earlier in the import, get the
        // real UUID
        if (fakeToRealUUIDMap.containsKey(uuid)) {
            uuid = fakeToRealUUIDMap.get(uuid);
        }
        // Resolve the UUID to an entity
        try {
            entity = entityService.findByItemId(context, uuid);
            if (entity.getItem() == null) {
                throw new IllegalArgumentException("No item found in repository with uuid: " + uuid);
            }
            return entity;
        } catch (SQLException sqle) {
            throw new MetadataImportException("Unable to find entity using reference: " + value, sqle);
        }
    }

    public void setHandler(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

    public void setUseCollectionTemplate(boolean useCollectionTemplate) {
        this.useCollectionTemplate = useCollectionTemplate;
    }

    public void setUseWorkflow(boolean useWorkflow) {
        this.useWorkflow = useWorkflow;
    }

    public void setWorkflowNotify(boolean workflowNotify) {
        this.workflowNotify = workflowNotify;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }
}
