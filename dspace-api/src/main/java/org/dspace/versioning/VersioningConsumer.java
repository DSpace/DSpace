/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.Relationship.LatestVersionStatus;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;

/**
 * When a new version of an item is published, unarchive the previous version and
 * update {@link Relationship#latestVersionStatus} of the relevant relationships.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(VersioningConsumer.class);

    private Set<Item> itemsToProcess;

    private VersionHistoryService versionHistoryService;
    private ItemService itemService;
    private EntityTypeService entityTypeService;
    private RelationshipTypeService relationshipTypeService;
    private RelationshipService relationshipService;

    @Override
    public void initialize() throws Exception {
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    }

    @Override
    public void finish(Context ctx) throws Exception {
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (itemsToProcess == null) {
            itemsToProcess = new HashSet<>();
        }

        // only items
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }

        // only install events
        if (event.getEventType() != Event.INSTALL) {
            return;
        }

        // get the item (should be archived)
        Item item = (Item) event.getSubject(ctx);
        if (item == null || !item.isArchived()) {
            return;
        }

        // get version history
        VersionHistory history = versionHistoryService.findByItem(ctx, item);
        if (history == null) {
            return;
        }

        // get latest version
        Version latestVersion = versionHistoryService.getLatestVersion(ctx, history);
        if (latestVersion == null) {
            return;
        }

        // get previous version
        Version previousVersion = versionHistoryService.getPrevious(ctx, history, latestVersion);
        if (previousVersion == null) {
            return;
        }

        // get latest item
        Item latestItem = latestVersion.getItem();
        if (latestItem == null) {
            String msg = String.format(
                "Illegal state: Obtained version history of item with uuid %s, handle %s, but the latest item is null",
                item.getID(), item.getHandle()
            );
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // get previous item
        Item previousItem = previousVersion.getItem();
        if (previousItem == null) {
            return;
        }

        // unarchive previous item
        unarchiveItem(ctx, previousItem);

        // update relationships
        updateRelationships(ctx, latestItem, previousItem);
    }

    protected void unarchiveItem(Context ctx, Item item) {
        item.setArchived(false);
        itemsToProcess.add(item);
        //Fire a new modify event for our previous item
        //Due to the need to reindex the item in the search
        //and browse index we need to fire a new event
        ctx.addEvent(new Event(
            Event.MODIFY, item.getType(), item.getID(), null, itemService.getIdentifiers(ctx, item)
        ));
    }

    protected void updateRelationships(Context ctx, Item latestItem, Item previousItem) {
        // check that the entity types of both items match
        if (!doEntityTypesMatch(latestItem, previousItem)) {
            return;
        }

        // get the entity type (same for both items)
        EntityType entityType = getEntityType(ctx, latestItem);
        if (entityType == null) {
            return;
        }

        // get all relationship types that are linked to the given entity type
        List<RelationshipType> relationshipTypes = getRelationshipTypes(ctx, entityType);
        if (CollectionUtils.isEmpty(relationshipTypes)) {
            return;
        }

        for (RelationshipType relationshipType : relationshipTypes) {
            List<Relationship> latestItemRelationships = getAllRelationships(ctx, latestItem, relationshipType);
            if (latestItemRelationships == null) {
                continue;
            }

            List<Relationship> previousItemRelationships = getAllRelationships(ctx, previousItem, relationshipType);
            if (previousItemRelationships == null) {
                continue;
            }

            for (Relationship previousItemRelationship : previousItemRelationships) {
                // determine on which side of the relationship the latest and previous item should be
                boolean isLeft = previousItem.equals(previousItemRelationship.getLeftItem());
                boolean isRight = previousItem.equals(previousItemRelationship.getRightItem());
                if (isLeft == isRight) {
                    Item leftItem = previousItemRelationship.getLeftItem();
                    Item rightItem = previousItemRelationship.getRightItem();
                    String msg = String.format(
                        "Illegal state: could not determine side of item with uuid %s, handle %s in " +
                        "relationship with id %s, rightward name %s between " +
                        "left item with uuid %s, handle %s and right item with uuid %s, handle %s",
                        previousItem.getID(), previousItem.getHandle(), previousItemRelationship.getID(),
                        previousItemRelationship.getRelationshipType().getRightwardType(),
                        leftItem.getID(), leftItem.getHandle(), rightItem.getID(), rightItem.getHandle()
                    );
                    log.error(msg);
                    throw new IllegalStateException(msg);
                }

                // get the matching relationship on the latest item
                Relationship latestItemRelationship =
                    getMatchingRelationship(latestItem, isLeft, previousItemRelationship, latestItemRelationships);

                // for sure set the previous item to non-latest
                // NOTE: if no matching relationship exists, this relationship will be considered deleted
                //       when viewed from the other item
                updateLatestVersionStatus(previousItemRelationship, isLeft, false);

                // set the new item to latest if the relevant relationship exists
                if (latestItemRelationship != null) {
                    updateLatestVersionStatus(latestItemRelationship, isLeft, true);
                }
            }
        }
    }

    /**
     * Given two items, check if their entity types match.
     * If one or both items don't have an entity type, comparing is pointless and this method will return false.
     * @param latestItem the item that represents the most recent version.
     * @param previousItem the item that represents the second-most recent version.
     * @return true if the entity types of both items are non-null and equal, false otherwise.
     */
    protected boolean doEntityTypesMatch(Item latestItem, Item previousItem) {
        String latestItemEntityType = getEntityType(latestItem);
        String previousItemEntityType = getEntityType(previousItem);

        // check if both items have an entity type
        if (latestItemEntityType == null || previousItemEntityType == null) {
            if (previousItemEntityType != null) {
                log.warn(String.format(
                    "Inconsistency: Item with uuid %s, handle %s has NO entity type, " +
                    "but the previous version of that item with uuid %s, handle %s has entity type %s",
                    latestItem.getID(), latestItem.getHandle(),
                    previousItem.getID(), previousItem.getHandle(), previousItemEntityType
                ));
            }

            // one or both items do not have an entity type, so comparing is pointless
            return false;
        }

        // check if the entity types are equal
        if (!StringUtils.equals(latestItemEntityType, previousItemEntityType)) {
            log.warn(String.format(
                "Inconsistency: Item with uuid %s, handle %s has entity type %s, " +
                "but the previous version of that item with uuid %s, handle %s has entity type %s",
                latestItem.getID(), latestItem.getHandle(), latestItemEntityType,
                previousItem.getID(), previousItem.getHandle(), previousItemEntityType
            ));
            return false;
        }

        // success - the entity types of both items are non-null and equal
        log.info(String.format(
            "Item with uuid %s, handle %s and the previous version of that item with uuid %s, handle %s " +
            "have the same entity type: %s",
            latestItem.getID(), latestItem.getHandle(), previousItem.getID(), previousItem.getHandle(),
            latestItemEntityType
        ));
        return true;
    }

    /**
     * Get the entity type (stored in metadata field dspace.entity.type) of any item.
     * @param item the item.
     * @return the label of the entity type.
     */
    protected String getEntityType(Item item) {
        List<MetadataValue> mdvs = itemService.getMetadata(item, "dspace", "entity", "type", Item.ANY, false);
        if (mdvs.isEmpty()) {
            return null;
        }
        if (mdvs.size() > 1) {
            log.warn(String.format(
                "Item with uuid %s, handle %s has %s entity types (%s), expected 1 entity type",
                item.getID(), item.getHandle(), mdvs.size(),
                mdvs.stream().map(MetadataValue::getValue).collect(Collectors.toUnmodifiableList())
            ));
        }

        String entityType = mdvs.get(0).getValue();
        if (StringUtils.isBlank(entityType)) {
            return null;
        }

        return entityType;
    }

    /**
     * Get the entity type (stored in metadata field dspace.entity.type) of any item.
     * @param item the item.
     * @return the entity type.
     */
    protected EntityType getEntityType(Context ctx, Item item) {
        String entityTypeStr = getEntityType(item);
        if (entityTypeStr == null) {
            return null;
        }

        try {
            return entityTypeService.findByEntityType(ctx, entityTypeStr);
        } catch (SQLException e) {
            log.error(String.format(
                "Exception occurred when trying to obtain entity type with label %s of item with uuid %s, handle %s",
                entityTypeStr, item.getID(), item.getHandle()
            ), e);
            return null;
        }
    }

    /**
     * Get all relationship types that have the given entity type on their left and/or right side.
     * @param ctx the DSpace context.
     * @param entityType the entity type for which all relationship types should be found.
     * @return a list of relationship types (possibly empty), or null in case of error.
     */
    protected List<RelationshipType> getRelationshipTypes(Context ctx, EntityType entityType) {
        try {
            return relationshipTypeService.findByEntityType(ctx, entityType);
        } catch (SQLException e) {
            log.error(String.format(
                "Exception occurred when trying to obtain relationship types via entity type with id %s, label %s",
                entityType.getID(), entityType.getLabel()
            ), e);
            return null;
        }
    }

    /**
     * Get all relationships of the given type linked to the given item.
     * @param ctx the DSpace context.
     * @param item the item.
     * @param relationshipType the relationship type.
     * @return a list of relationships (possibly empty), or null in case of error.
     */
    protected List<Relationship> getAllRelationships(Context ctx, Item item, RelationshipType relationshipType) {
        try {
            return relationshipService.findByItemAndRelationshipType(ctx, item, relationshipType, -1, -1, false);
        } catch (SQLException e) {
            log.error(String.format(
                "Exception occurred when trying to obtain relationships of type with id %s, rightward name %s " +
                "for item with uuid %s, handle %s",
                relationshipType.getID(), relationshipType.getRightwardType(), item.getID(), item.getHandle()
            ), e);
            return null;
        }
    }

    /**
     * From a list of relationships, find the relationship with the correct relationship type and items.
     * If isLeft is true, the provided item should be on the left side of the relationship.
     * If isLeft is false, the provided item should be on the right side of the relationship.
     * In both cases, the other item is taken from the given relationship.
     * @param latestItem the item that should either be on the left or right side of the returned relationship (if any).
     * @param isLeft decide on which side of the relationship the provided item should be.
     * @param previousItemRelationship the relationship from which the type and the other item are read.
     * @param relationships the list of relationships that we'll search through.
     * @return the relationship that satisfies the requirements (can only be one or zero).
     */
    protected Relationship getMatchingRelationship(
        Item latestItem, boolean isLeft, Relationship previousItemRelationship, List<Relationship> relationships
    ) {
        Item leftItem = previousItemRelationship.getLeftItem();
        RelationshipType relationshipType = previousItemRelationship.getRelationshipType();
        Item rightItem = previousItemRelationship.getRightItem();

        if (isLeft) {
            return getMatchingRelationship(latestItem, relationshipType, rightItem, relationships);
        } else {
            return getMatchingRelationship(leftItem, relationshipType, latestItem, relationships);
        }
    }


    /**
     * Find the relationship with the given left item, relation type and right item, from a list of relationships.
     * @param expectedLeftItem the relationship that we're looking for has this item on the left side.
     * @param expectedRelationshipType the relationship that we're looking for has this relationship type.
     * @param expectedRightItem the relationship that we're looking for has this item on the right side.
     * @param relationships the list of relationships that we'll search through.
     * @return the relationship that satisfies the requirements (can only be one or zero).
     */
    protected Relationship getMatchingRelationship(
        Item expectedLeftItem, RelationshipType expectedRelationshipType, Item expectedRightItem,
        List<Relationship> relationships
    ) {
        Integer expectedRelationshipTypeId = expectedRelationshipType.getID();

        List<Relationship> matchingRelationships = relationships.stream()
            .filter(relationship -> {
                int relationshipTypeId = relationship.getID();

                boolean leftItemMatches = expectedLeftItem.equals(relationship.getLeftItem());
                boolean relationshipTypeMatches = expectedRelationshipTypeId == relationshipTypeId;
                boolean rightItemMatches = expectedRightItem.equals(relationship.getRightItem());

                return leftItemMatches && relationshipTypeMatches && rightItemMatches;
            })
            .distinct()
            .collect(Collectors.toUnmodifiableList());

        if (matchingRelationships.isEmpty()) {
            return null;
        }

        // NOTE: this situation should never occur because the relationship table has a unique constraint
        //       over the "left_id", "type_id" and "right_id" columns
        if (matchingRelationships.size() > 1) {
            String msg = String.format(
                "Illegal state: expected 0 or 1 relationship, but found %s relationships (ids: %s) " +
                "of type with id %s, rightward name %s " +
                "between left item with uuid %s, handle %s and right item with uuid %s, handle %s",
                matchingRelationships.size(),
                matchingRelationships.stream().map(Relationship::getID).collect(Collectors.toUnmodifiableList()),
                expectedRelationshipTypeId, expectedRelationshipType.getRightwardType(),
                expectedLeftItem.getID(), expectedLeftItem.getHandle(),
                expectedRightItem.getID(), expectedRightItem.getHandle()
            );
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        return matchingRelationships.get(0);
    }

    /**
     * Update {@link Relationship#latestVersionStatus} of the given relationship.
     * @param relationship the relationship.
     * @param updateLeftSide whether the status of the left item or the right item should be updated.
     * @param isLatest to what the status should be set.
     * @throws IllegalStateException if the operation would result in both the left side and the right side
     *                               being set to non-latest.
     */
    protected void updateLatestVersionStatus(
        Relationship relationship, boolean updateLeftSide, boolean isLatest
    ) throws IllegalStateException {
        LatestVersionStatus lvs = relationship.getLatestVersionStatus();

        boolean leftSideIsLatest = lvs == LatestVersionStatus.BOTH || lvs == LatestVersionStatus.LEFT_ONLY;
        boolean rightSideIsLatest = lvs == LatestVersionStatus.BOTH || lvs == LatestVersionStatus.RIGHT_ONLY;

        if (updateLeftSide) {
            if (leftSideIsLatest == isLatest) {
                return; // no change needed
            }
            leftSideIsLatest = isLatest;
        } else {
            if (rightSideIsLatest == isLatest) {
                return; // no change needed
            }
            rightSideIsLatest = isLatest;
        }

        LatestVersionStatus newVersionStatus;
        if (leftSideIsLatest && rightSideIsLatest) {
            newVersionStatus = LatestVersionStatus.BOTH;
        } else if (leftSideIsLatest) {
            newVersionStatus = LatestVersionStatus.LEFT_ONLY;
        } else if (rightSideIsLatest) {
            newVersionStatus = LatestVersionStatus.RIGHT_ONLY;
        } else {
            String msg = String.format(
                "Illegal state: cannot set %s item to latest = false, because relationship with id %s, " +
                "rightward name %s between left item with uuid %s, handle %s and right item with uuid %s, handle %s " +
                "has latest version status set to %s",
                updateLeftSide ? "left" : "right", relationship.getID(),
                relationship.getRelationshipType().getRightwardType(),
                relationship.getLeftItem().getID(), relationship.getLeftItem().getHandle(),
                relationship.getRightItem().getID(), relationship.getRightItem().getHandle(), lvs
            );
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info(String.format(
            "set latest version status from %s to %s for relationship with id %s, rightward name %s " +
            "between left item with uuid %s, handle %s and right item with uuid %s, handle %s",
            lvs, newVersionStatus, relationship.getID(), relationship.getRelationshipType().getRightwardType(),
            relationship.getLeftItem().getID(), relationship.getLeftItem().getHandle(),
            relationship.getRightItem().getID(), relationship.getRightItem().getHandle()
        ));
        relationship.setLatestVersionStatus(newVersionStatus);
    }

    @Override
    public void end(Context ctx) throws Exception {
        if (itemsToProcess != null) {
            for (Item item : itemsToProcess) {
                ctx.turnOffAuthorisationSystem();
                try {
                    itemService.update(ctx, item);
                } finally {
                    ctx.restoreAuthSystemState();
                }
            }
        }

        itemsToProcess = null;
    }

}
