/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import static org.dspace.versioning.utils.RelationshipVersioningUtils.LatestVersionStatusChangelog.NO_CHANGES;

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
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexEventConsumer;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.utils.RelationshipVersioningUtils;
import org.dspace.versioning.utils.RelationshipVersioningUtils.LatestVersionStatusChangelog;

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
    private RelationshipVersioningUtils relationshipVersioningUtils;

    @Override
    public void initialize() throws Exception {
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        relationshipVersioningUtils = VersionServiceFactory.getInstance().getRelationshipVersioningUtils();
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

    /**
     * Update {@link Relationship#latestVersionStatus} of the relationships of both the old version and the new version
     * of the item.
     *
     * This method will first locate all relationships that are eligible for an update,
     * then it will try to match each of those relationships on the old version of given item
     * with a relationship on the new version.
     *
     * One of the following scenarios will happen:
     * - if a match is found, then the "latest" status on the side of given item is transferred from
     *   the old relationship to the new relationship. This implies that on the page of the third-party item,
     *   the old version of given item will NOT be shown anymore and the new version of given item will appear.
     *   Both versions of the given item still show the third-party item on their pages.
     * - if a relationship only exists on the new version of given item, then this method does nothing.
     *   The status of those relationships should already have been set to "latest" on both sides during relationship
     *   creation.
     * - if a relationship only exists on the old version of given item, then we assume that the relationship is no
     *   longer relevant to / has been removed from the new version of the item. The "latest" status is removed from
     *   the side of the given item. This implies that on the page of the third-party item,
     *   the relationship with given item will no longer be listed. The old version of given item still lists
     *   the third-party item and the new version doesn't.
     * @param ctx the DSpace context.
     * @param latestItem the new version of the item.
     * @param previousItem the old version of the item.
     */
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

            // NOTE: no need to loop through latestItemRelationships, because if no match can be found
            //       (meaning a relationship is only present on the new version of the item), then it's
            //       a newly added relationship and its status should have been set to BOTH during creation.
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

                // the other side of the relationship should be "latest", otherwise the relationship could not have been
                // copied to the new item in the first place (by DefaultVersionProvider#copyRelationships)
                if (relationshipVersioningUtils.otherSideIsLatest(
                    isLeft, previousItemRelationship.getLatestVersionStatus()
                )) {
                    // Set the previous version of the item to non-latest. This implies that the previous version
                    // of the item will not be shown anymore on the page of the third-party item. That makes sense,
                    // because either the relationship has been deleted from the new version of the item (no match),
                    // or the matching relationship (linked to new version) will receive "latest" status in
                    // the next step.
                    LatestVersionStatusChangelog changelog =
                        relationshipVersioningUtils.updateLatestVersionStatus(previousItemRelationship, isLeft, false);
                    reindexRelationship(ctx, changelog, previousItemRelationship);
                }

                if (latestItemRelationship != null) {
                    // Set the new version of the item to latest if the relevant relationship exists (match found).
                    // This implies that the new version of the item will appear on the page of the third-party item.
                    // The old version of the item will not appear anymore on the page of the third-party item,
                    // see previous step.
                    LatestVersionStatusChangelog changelog =
                        relationshipVersioningUtils.updateLatestVersionStatus(latestItemRelationship, isLeft, true);
                    reindexRelationship(ctx, changelog, latestItemRelationship);
                }
            }
        }
    }

    /**
     * If the {@link Relationship#latestVersionStatus} of the relationship has changed,
     * an "item modified" event should be fired for both the left and right item of the relationship.
     * On one item the relation.* fields will change. On the other item the relation.*.latestForDiscovery will change.
     * The event will cause the items to be re-indexed by the {@link IndexEventConsumer}.
     * @param ctx the DSpace context.
     * @param changelog indicates which side of the relationship has changed.
     * @param relationship the relationship.
     */
    protected void reindexRelationship(
        Context ctx, LatestVersionStatusChangelog changelog, Relationship relationship
    ) {
        if (changelog == NO_CHANGES) {
            return;
        }

        // on one item, relation.* fields will change
        // on the other item, relation.*.latestForDiscovery will change

        // reindex left item
        Item leftItem = relationship.getLeftItem();
        itemsToProcess.add(leftItem);
        ctx.addEvent(new Event(
            Event.MODIFY, leftItem.getType(), leftItem.getID(), null, itemService.getIdentifiers(ctx, leftItem)
        ));

        // reindex right item
        Item rightItem = relationship.getRightItem();
        itemsToProcess.add(rightItem);
        ctx.addEvent(new Event(
            Event.MODIFY, rightItem.getType(), rightItem.getID(), null, itemService.getIdentifiers(ctx, rightItem)
        ));
    }

    /**
     * Given two items, check if their entity types match.
     * If one or both items don't have an entity type, comparing is pointless and this method will return false.
     * @param latestItem the item that represents the most recent version.
     * @param previousItem the item that represents the second-most recent version.
     * @return true if the entity types of both items are non-null and equal, false otherwise.
     */
    protected boolean doEntityTypesMatch(Item latestItem, Item previousItem) {
        String latestItemEntityType = itemService.getEntityTypeLabel(latestItem);
        String previousItemEntityType = itemService.getEntityTypeLabel(previousItem);

        // check if both items have an entity type
        if (latestItemEntityType == null || previousItemEntityType == null) {
            if (previousItemEntityType != null) {
                log.warn(
                    "Inconsistency: Item with uuid {}, handle {} has NO entity type, " +
                    "but the previous version of that item with uuid {}, handle {} has entity type {}",
                    latestItem.getID(), latestItem.getHandle(),
                    previousItem.getID(), previousItem.getHandle(), previousItemEntityType
                );
            }

            // one or both items do not have an entity type, so comparing is pointless
            return false;
        }

        // check if the entity types are equal
        if (!StringUtils.equals(latestItemEntityType, previousItemEntityType)) {
            log.warn(
                "Inconsistency: Item with uuid {}, handle {} has entity type {}, " +
                "but the previous version of that item with uuid {}, handle {} has entity type {}",
                latestItem.getID(), latestItem.getHandle(), latestItemEntityType,
                previousItem.getID(), previousItem.getHandle(), previousItemEntityType
            );
            return false;
        }

        // success - the entity types of both items are non-null and equal
        log.info(
            "Item with uuid {}, handle {} and the previous version of that item with uuid {}, handle {} " +
            "have the same entity type: {}",
            latestItem.getID(), latestItem.getHandle(), previousItem.getID(), previousItem.getHandle(),
            latestItemEntityType
        );
        return true;
    }

    /**
     * Get the entity type (stored in metadata field dspace.entity.type) of any item.
     * @param item the item.
     * @return the entity type.
     */
    protected EntityType getEntityType(Context ctx, Item item) {
        try {
            return itemService.getEntityType(ctx, item);
        } catch (SQLException e) {
            log.error(
                "Exception occurred when trying to obtain entity type with label {} of item with uuid {}, handle {}",
                itemService.getEntityTypeLabel(item), item.getID(), item.getHandle(), e
            );
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
            log.error(
                "Exception occurred when trying to obtain relationship types via entity type with id {}, label {}",
                entityType.getID(), entityType.getLabel(), e
            );
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
            log.error(
                "Exception occurred when trying to obtain relationships of type with id {}, rightward name {} " +
                "for item with uuid {}, handle {}",
                relationshipType.getID(), relationshipType.getRightwardType(), item.getID(), item.getHandle(), e
            );
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
                int relationshipTypeId = relationship.getRelationshipType().getID();

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
