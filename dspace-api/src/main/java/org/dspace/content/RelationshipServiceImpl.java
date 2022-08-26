/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Relationship.LatestVersionStatus;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.dao.pojo.ItemUuidAndRelationshipId;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.virtual.VirtualMetadataConfiguration;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.utils.RelationshipVersioningUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipServiceImpl implements RelationshipService {

    private static final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    protected RelationshipDAO relationshipDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private RelationshipMetadataService relationshipMetadataService;

    @Autowired
    private RelationshipVersioningUtils relationshipVersioningUtils;

    @Autowired
    private VirtualMetadataPopulator virtualMetadataPopulator;

    @Override
    public Relationship create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify relationship");
        }
        return relationshipDAO.create(context, new Relationship());
    }

    @Override
    public Relationship create(Context c, Item leftItem, Item rightItem, RelationshipType relationshipType,
                               int leftPlace, int rightPlace) throws AuthorizeException, SQLException {
        return create(c, leftItem, rightItem, relationshipType, leftPlace, rightPlace, null, null);
    }



    @Override
    public Relationship create(
        Context c, Item leftItem, Item rightItem, RelationshipType relationshipType, int leftPlace, int rightPlace,
        String leftwardValue, String rightwardValue, LatestVersionStatus latestVersionStatus
    ) throws AuthorizeException, SQLException {
        Relationship relationship = new Relationship();
        relationship.setLeftItem(leftItem);
        relationship.setRightItem(rightItem);
        relationship.setRelationshipType(relationshipType);
        relationship.setLeftPlace(leftPlace);
        relationship.setRightPlace(rightPlace);
        relationship.setLeftwardValue(leftwardValue);
        relationship.setRightwardValue(rightwardValue);
        relationship.setLatestVersionStatus(latestVersionStatus);
        return create(c, relationship);
    }

    @Override
    public Relationship create(
        Context c, Item leftItem, Item rightItem, RelationshipType relationshipType, int leftPlace, int rightPlace,
        String leftwardValue, String rightwardValue
    ) throws AuthorizeException, SQLException {
        return create(
            c, leftItem, rightItem, relationshipType, leftPlace, rightPlace, leftwardValue, rightwardValue,
            LatestVersionStatus.BOTH
        );
    }

    @Override
    public Relationship create(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        if (isRelationshipValidToCreate(context, relationship)) {
            if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
                authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                // This order of execution should be handled in the creation (create, updateplace, update relationship)
                // for a proper place allocation
                Relationship relationshipToReturn = relationshipDAO.create(context, relationship);
                updatePlaceInRelationship(context, relationshipToReturn, null, null, true, true);
                update(context, relationshipToReturn);
                updateItemsInRelationship(context, relationship);
                return relationshipToReturn;
            } else {
                throw new AuthorizeException(
                    "You do not have write rights on this relationship's items");
            }

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    @Override
    public Relationship move(
        Context context, Relationship relationship, Integer newLeftPlace, Integer newRightPlace
    ) throws SQLException, AuthorizeException {
        if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {

            // Don't do anything if neither the leftPlace nor rightPlace was updated
            if (newLeftPlace != null || newRightPlace != null) {
                // This order of execution should be handled in the creation (create, updateplace, update relationship)
                // for a proper place allocation
                updatePlaceInRelationship(context, relationship, newLeftPlace, newRightPlace, false, false);
                update(context, relationship);
                updateItemsInRelationship(context, relationship);
            }

            return relationship;
        } else {
            throw new AuthorizeException(
                "You do not have write rights on this relationship's items");
        }
    }

    @Override
    public Relationship move(
        Context context, Relationship relationship, Item newLeftItem, Item newRightItem
    ) throws SQLException, AuthorizeException {
        // If the new Item is the same as the current Item, don't move
        newLeftItem = newLeftItem != relationship.getLeftItem() ? newLeftItem : null;
        newRightItem = newRightItem != relationship.getRightItem() ? newRightItem : null;

        // Don't do anything if neither the leftItem nor rightItem was updated
        if (newLeftItem != null || newRightItem != null) {
            // First move the Relationship to the back within the current Item's lists
            // This ensures that we won't have any gaps once we move the Relationship to a different Item
            move(
                context, relationship,
                newLeftItem != null ? -1 : null,
                newRightItem != null ? -1 : null
            );

            boolean insertLeft = false;
            boolean insertRight = false;

            // If Item has been changed, mark the previous Item as modified to make sure we discard the old relation.*
            // metadata on the next update.
            // Set the Relationship's Items to the new ones, appending to the end
            if (newLeftItem != null) {
                relationship.getLeftItem().setMetadataModified();
                relationship.setLeftItem(newLeftItem);
                relationship.setLeftPlace(-1);
                insertLeft = true;
            }
            if (newRightItem != null) {
                relationship.getRightItem().setMetadataModified();
                relationship.setRightItem(newRightItem);
                relationship.setRightPlace(-1);
                insertRight = true;
            }

            // This order of execution should be handled in the creation (create, updateplace, update relationship)
            // for a proper place allocation
            updatePlaceInRelationship(context, relationship, null, null, insertLeft, insertRight);
            update(context, relationship);
            updateItemsInRelationship(context, relationship);
        }
        return relationship;
    }

    /**
     * This method will update the place for the Relationship and all other relationships found by the items and
     * relationship type of the given Relationship.
     *
     * @param context           The relevant DSpace context
     * @param relationship      The Relationship object that will have its place updated and that will be used
     *                          to retrieve the other relationships whose place might need to be updated.
     * @param newLeftPlace      If the Relationship in question is to be moved, the leftPlace it is to be moved to.
     *                          Set this to null if the Relationship has not been moved, i.e. it has just been created,
     *                          deleted or when its Items have been modified.
     * @param newRightPlace     If the Relationship in question is to be moved, the rightPlace it is to be moved to.
     *                          Set this to null if the Relationship has not been moved, i.e. it has just been created,
     *                          deleted or when its Items have been modified.
     * @param insertLeft        Whether the Relationship in question should be inserted into the left Item.
     *                          Should be set to true when creating or moving to a different Item.
     * @param insertRight       Whether the Relationship in question should be inserted into the right Item.
     *                          Should be set to true when creating or moving to a different Item.
     * @throws SQLException     If something goes wrong
     * @throws AuthorizeException
     *                          If the user is not authorized to update the Relationship or its Items
     */
    private void updatePlaceInRelationship(
        Context context, Relationship relationship,
        Integer newLeftPlace, Integer newRightPlace, boolean insertLeft, boolean insertRight
    ) throws SQLException, AuthorizeException {
        Item leftItem = relationship.getLeftItem();
        Item rightItem = relationship.getRightItem();

        // These list also include the non-latest. This is relevant to determine whether it's deleted.
        // This can also imply there may be overlapping places, and/or the given relationship will overlap
        // But the shift will allow this, and only happen when needed based on the latest status
        List<Relationship> leftRelationships = findByItemAndRelationshipType(
            context, leftItem, relationship.getRelationshipType(), true, -1, -1, false
        );
        List<Relationship> rightRelationships = findByItemAndRelationshipType(
            context, rightItem, relationship.getRelationshipType(), false, -1, -1, false
        );

        // These relationships are only deleted from the temporary lists in case they're present in them so that we can
        // properly perform our place calculation later down the line in this method.
        boolean deletedFromLeft = !leftRelationships.contains(relationship);
        boolean deletedFromRight = !rightRelationships.contains(relationship);
        leftRelationships.remove(relationship);
        rightRelationships.remove(relationship);

        List<MetadataValue> leftMetadata = getSiblingMetadata(leftItem, relationship, true);
        List<MetadataValue> rightMetadata = getSiblingMetadata(rightItem, relationship, false);

        // For new relationships added to the end, this will be -1.
        // For new relationships added at a specific position, this will contain that position.
        // For existing relationships, this will contain the place before it was moved.
        // For deleted relationships, this will contain the place before it was deleted.
        int oldLeftPlace = relationship.getLeftPlace();
        int oldRightPlace = relationship.getRightPlace();


        boolean movedUpLeft = resolveRelationshipPlace(
            relationship, true, leftRelationships, leftMetadata, oldLeftPlace, newLeftPlace
        );
        boolean movedUpRight = resolveRelationshipPlace(
            relationship, false, rightRelationships, rightMetadata, oldRightPlace, newRightPlace
        );

        context.turnOffAuthorisationSystem();

        //only shift if the place is relevant for the latest relationships
        if (relationshipVersioningUtils.otherSideIsLatest(true, relationship.getLatestVersionStatus())) {
            shiftSiblings(
                relationship, true, oldLeftPlace, movedUpLeft, insertLeft, deletedFromLeft,
                leftRelationships, leftMetadata
            );
        }
        if (relationshipVersioningUtils.otherSideIsLatest(false, relationship.getLatestVersionStatus())) {
            shiftSiblings(
                relationship, false, oldRightPlace, movedUpRight, insertRight, deletedFromRight,
                rightRelationships, rightMetadata
            );
        }

        updateItem(context, leftItem);
        updateItem(context, rightItem);

        context.restoreAuthSystemState();
    }

    /**
     * Return the MDVs in the Item's MDF corresponding to the given Relationship.
     * Return an empty list if the Relationship isn't mapped to any MDF
     * or if the mapping is configured with useForPlace=false.
     *
     * This returns actual metadata (not virtual) which in the same metadata field as the useForPlace.
     * For a publication with 2 author relationships and 3 plain text dc.contributor.author values,
     * it would return the 3 plain text dc.contributor.author values.
     * For a person related to publications, it would return an empty list.
     */
    private List<MetadataValue> getSiblingMetadata(
        Item item, Relationship relationship, boolean isLeft
    ) {
        List<MetadataValue> metadata = new ArrayList<>();
        if (virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), isLeft)) {
            HashMap<String, VirtualMetadataConfiguration> mapping;
            if (isLeft) {
                mapping = virtualMetadataPopulator.getMap().get(relationship.getRelationshipType().getLeftwardType());
            } else {
                mapping = virtualMetadataPopulator.getMap().get(relationship.getRelationshipType().getRightwardType());
            }
            if (mapping != null) {
                for (String mdf : mapping.keySet()) {
                    metadata.addAll(
                        // Make sure we're only looking at database MDVs; if the relationship currently overlaps
                        // one of these, its virtual MDV will overwrite the database MDV in itemService.getMetadata()
                        // The relationship pass should be sufficient to move any sibling virtual MDVs.
                        item.getMetadata()
                            .stream()
                            .filter(mdv -> mdv.getMetadataField().toString().equals(mdf.replace(".", "_")))
                            .collect(Collectors.toList())
                    );
                }
            }
        }
        return metadata;
    }

    /**
     * Set the left/right place of a Relationship
     *   - To a new place in case it's being moved
     *   - Resolve -1 to the actual last place based on the places of its sibling Relationships and/or MDVs
     * and determine if it has been moved up in the list.
     *
     * Examples:
     *   - Insert a Relationship at place 3
     *     newPlace starts out as null and is not updated. Return movedUp=false
     *   - Insert a Relationship at place -1
     *     newPlace starts out as null and is resolved to e.g. 6. Update the Relationship and return movedUp=false
     *   - Move a Relationship from place 4 to 2
     *     Update the Relationship and return movedUp=false.
     *   - Move a Relationship from place 2 to -1
     *     newPlace starts out as -1 and is resolved to e.g. 5. Update the relationship and return movedUp=true.
     *   - Remove a relationship from place 1
     *     Return movedUp=false
     *
     * @param relationship      the Relationship that's being updated
     * @param isLeft            whether to consider the left side of the Relationship.
     *                          This method should be called twice, once with isLeft=true and once with isLeft=false.
     *                          Make sure this matches the provided relationships/metadata/oldPlace/newPlace.
     * @param relationships     the list of sibling Relationships
     * @param metadata          the list of sibling MDVs
     * @param oldPlace          the previous place for this Relationship, in case it has been moved.
     *                          Otherwise, the current place of a deleted Relationship
     *                          or the place a Relationship has been inserted.
     * @param newPlace          The new place for this Relationship. Will be null on insert/delete.
     * @return  true if the Relationship was moved and newPlace > oldPlace
     */
    private boolean resolveRelationshipPlace(
        Relationship relationship, boolean isLeft,
        List<Relationship> relationships, List<MetadataValue> metadata,
        int oldPlace, Integer newPlace
    ) {
        boolean movedUp = false;

        if (newPlace != null) {
            // We're moving an existing Relationship...
            if (newPlace == -1) {
                // ...to the end of the list
                int nextPlace = getNextPlace(relationships, metadata, isLeft);
                if (nextPlace == oldPlace) {
                    // If this Relationship is already at the end, do nothing.
                    newPlace = oldPlace;
                } else {
                    // Subtract 1 from the next place since we're moving, not inserting and
                    // the total number of Relationships stays the same.
                    newPlace = nextPlace - 1;
                }
            }
            if (newPlace > oldPlace) {
                // ...up the list. We have to keep track of this in order to shift correctly later on
                movedUp = true;
            }
        } else if (oldPlace == -1) {
            // We're _not_ moving an existing Relationship. The newPlace is already set in the Relationship object.
            // We only need to resolve it to the end of the list if it's set to -1, otherwise we can just keep it as is.
            newPlace = getNextPlace(relationships, metadata, isLeft);
        }

        if (newPlace != null) {
            setPlace(relationship, isLeft, newPlace);
        }

        return movedUp;
    }

    /**
     * Return the index of the next place in a list of Relationships and Metadata.
     * By not relying on the size of both lists we can support one-to-many virtual MDV mappings.
     * @param isLeft  whether to take the left or right place of each Relationship
     */
    private int getNextPlace(List<Relationship> relationships, List<MetadataValue> metadata, boolean isLeft) {
        return Stream.concat(
                         metadata.stream().map(MetadataValue::getPlace),
                         relationships.stream().map(r -> getPlace(r, isLeft))
                     ).max(Integer::compare)
                     .map(integer -> integer + 1)
                     .orElse(0);
    }

    /**
     * Adjust the left/right place of sibling Relationships and MDVs
     *
     * Examples: with sibling Relationships R,S,T and metadata a,b,c
     *   - Insert T at place 1                                              aRbSc     ->  a T RbSc
     *     Shift all siblings with place >= 1 one place to the right
     *   - Delete R from place 2                                            aT R bSc  ->  aTbSc
     *     Shift all siblings with place > 2 one place to the left
     *   - Move S from place 3 to place 2 (movedUp=false)                   aTb S c   ->  aT S bc
     *     Shift all siblings with 2 < place <= 3 one place to the right
     *   - Move T from place 1 to place 3 (movedUp=true)                    a T Sbc   ->  aSb T c
     *     Shift all siblings with 1 < place <= 3 one place to the left
     *
     * @param relationship      the Relationship that's being updated
     * @param isLeft            whether to consider the left side of the Relationship.
     *                          This method should be called twice, once with isLeft=true and once with isLeft=false.
     *                          Make sure this matches the provided relationships/metadata/oldPlace/newPlace.
     * @param oldPlace          the previous place for this Relationship, in case it has been moved.
     *                          Otherwise, the current place of a deleted Relationship
     *                          or the place a Relationship has been inserted.
     * @param movedUp           if this Relationship has been moved up the list, e.g. from place 2 to place 4
     * @param deleted           whether this Relationship has been deleted
     * @param relationships     the list of sibling Relationships
     * @param metadata          the list of sibling MDVs
     */
    private void shiftSiblings(
        Relationship relationship, boolean isLeft, int oldPlace, boolean movedUp, boolean inserted, boolean deleted,
        List<Relationship> relationships, List<MetadataValue> metadata
    ) {
        int newPlace = getPlace(relationship, isLeft);

        for (Relationship sibling : relationships) {
            // NOTE: If and only if the other side of the relationship has "latest" status, the relationship will appear
            //       as a metadata value on the item at the current side (indicated by isLeft) of the relationship.
            //
            //       Example: volume <----> issue (LEFT_ONLY)
            //       => LEFT_ONLY means that the volume has "latest" status, but the issue does NOT have "latest" status
            //       => the volume will appear in the metadata of the issue,
            //          but the issue will NOT appear in the metadata of the volume
            //
            //       This means that the other side of the relationship has to have "latest" status, otherwise this
            //       relationship is NOT relevant for place calculation.
            if (relationshipVersioningUtils.otherSideIsLatest(isLeft, sibling.getLatestVersionStatus())) {
                int siblingPlace = getPlace(sibling, isLeft);
                if (
                    (deleted && siblingPlace > newPlace)
                    // If the relationship was deleted, all relationships after it should shift left
                    // We must make the distinction between deletes and moves because for inserts oldPlace == newPlace
                        || (movedUp && siblingPlace <= newPlace && siblingPlace > oldPlace)
                            // If the relationship was moved up e.g. from place 2 to 5, all relationships
                            // with place > 2 (the old place) and <= to 5 should shift left
                ) {
                    setPlace(sibling, isLeft, siblingPlace - 1);
                } else if (
                    (inserted && siblingPlace >= newPlace)
                    // If the relationship was inserted, all relationships starting from that place should shift right
                    // We must make the distinction between inserts and moves because for inserts oldPlace == newPlace
                        || (!movedUp && siblingPlace >= newPlace && siblingPlace < oldPlace)
                            // If the relationship was moved down e.g. from place 5 to 2, all relationships
                            // with place >= 2 and < 5 (the old place) should shift right
                ) {
                    setPlace(sibling, isLeft, siblingPlace + 1);
                }
            }
        }
        for (MetadataValue mdv : metadata) {
            // NOTE: Plain text metadata values should ALWAYS be included in the place calculation,
            //       because they are by definition only visible/relevant to the side of the relationship
            //       that we are currently processing.
            int mdvPlace = mdv.getPlace();
            if (
                (deleted && mdvPlace > newPlace)
                // If the relationship was deleted, all metadata after it should shift left
                // We must make the distinction between deletes and moves because for inserts oldPlace == newPlace
                // If the reltionship was copied to metadata on deletion:
                //   - the plain text can be after the relationship (in which case it's moved forward again)
                //   - or before the relationship (in which case it remains in place)
                    || (movedUp && mdvPlace <= newPlace && mdvPlace > oldPlace)
                        // If the relationship was moved up e.g. from place 2 to 5, all metadata
                        // with place > 2 (the old place) and <= to 5 should shift left
            ) {
                mdv.setPlace(mdvPlace - 1);
            } else if (
                (inserted && mdvPlace >= newPlace)
                // If the relationship was inserted, all relationships starting from that place should shift right
                // We must make the distinction between inserts and moves because for inserts oldPlace == newPlace
                    || (!movedUp && mdvPlace >= newPlace && mdvPlace < oldPlace)
                        // If the relationship was moved down e.g. from place 5 to 2, all relationships
                        // with place >= 2 and < 5 (the old place) should shift right
            ) {
                mdv.setPlace(mdvPlace + 1);
            }
        }
    }

    private int getPlace(Relationship relationship, boolean isLeft) {
        if (isLeft) {
            return relationship.getLeftPlace();
        } else {
            return relationship.getRightPlace();
        }
    }

    private void setPlace(Relationship relationship, boolean isLeft, int place) {
        if (isLeft) {
            relationship.setLeftPlace(place);
        } else {
            relationship.setRightPlace(place);
        }
    }

    @Override
    public void updateItem(Context context, Item relatedItem)
        throws SQLException, AuthorizeException {
        relatedItem.setMetadataModified();
        itemService.update(context, relatedItem);
    }

    private boolean isRelationshipValidToCreate(Context context, Relationship relationship) throws SQLException {
        RelationshipType relationshipType = relationship.getRelationshipType();

        if (!verifyEntityTypes(relationship.getLeftItem(), relationshipType.getLeftType())) {
            log.warn("The relationship has been deemed invalid since the leftItem" +
                         " and leftType do no match on entityType");
            logRelationshipTypeDetailsForError(relationshipType);
            return false;
        }
        if (!verifyEntityTypes(relationship.getRightItem(), relationshipType.getRightType())) {
            log.warn("The relationship has been deemed invalid since the rightItem" +
                         " and rightType do no match on entityType");
            logRelationshipTypeDetailsForError(relationshipType);
            return false;
        }
        if (!relationship.getLatestVersionStatus().equals(LatestVersionStatus.LEFT_ONLY)
            && !verifyMaxCardinality(context, relationship.getLeftItem(),
                                  relationshipType.getLeftMaxCardinality(), relationshipType, true)) {
            //If RIGHT_ONLY => it's a copied relationship, and the count can be ignored
            log.warn("The relationship has been deemed invalid since the left item has more" +
                         " relationships than the left max cardinality allows after we'd store this relationship");
            logRelationshipTypeDetailsForError(relationshipType);
            return false;
        }
        if (!relationship.getLatestVersionStatus().equals(LatestVersionStatus.RIGHT_ONLY)
                && !verifyMaxCardinality(context, relationship.getRightItem(),
                                  relationshipType.getRightMaxCardinality(), relationshipType, false)) {
            //If LEFT_ONLY => it's a copied relationship, and the count can be ignored
            log.warn("The relationship has been deemed invalid since the right item has more" +
                         " relationships than the right max cardinality allows after we'd store this relationship");
            logRelationshipTypeDetailsForError(relationshipType);
            return false;
        }
        return true;
    }

    private void logRelationshipTypeDetailsForError(RelationshipType relationshipType) {
        log.warn("The relationshipType's ID is: " + relationshipType.getID());
        log.warn("The relationshipType's leftward type is: " + relationshipType.getLeftwardType());
        log.warn("The relationshipType's rightward type is: " + relationshipType.getRightwardType());
        log.warn("The relationshipType's left entityType label is: " + relationshipType.getLeftType().getLabel());
        log.warn("The relationshipType's right entityType label is: " + relationshipType.getRightType().getLabel());
        log.warn("The relationshipType's left min cardinality is: " + relationshipType.getLeftMinCardinality());
        log.warn("The relationshipType's left max cardinality is: " + relationshipType.getLeftMaxCardinality());
        log.warn("The relationshipType's right min cardinality is: " + relationshipType.getRightMinCardinality());
        log.warn("The relationshipType's right max cardinality is: " + relationshipType.getRightMaxCardinality());
    }

    private boolean verifyMaxCardinality(Context context, Item itemToProcess,
                                         Integer maxCardinality,
                                         RelationshipType relationshipType,
                                         boolean isLeft) throws SQLException {
        if (maxCardinality == null) {
            //no need to check the relationships
            return true;
        }
        List<Relationship> rightRelationships = findByItemAndRelationshipType(context, itemToProcess, relationshipType,
                                                                              isLeft);
        if (rightRelationships.size() >= maxCardinality) {
            return false;
        }
        return true;
    }

    private boolean verifyEntityTypes(Item itemToProcess, EntityType entityTypeToProcess) {
        List<MetadataValue> list = itemService.getMetadata(itemToProcess, "dspace", "entity",
                "type", Item.ANY, false);
        if (list.isEmpty()) {
            return false;
        }
        String leftEntityType = list.get(0).getValue();
        return StringUtils.equals(leftEntityType, entityTypeToProcess.getLabel());
    }

    @Override
    public Relationship find(Context context, int id) throws SQLException {
        Relationship relationship = relationshipDAO.findByID(context, Relationship.class, id);
        return relationship;
    }

    @Override
    public List<Relationship> findByItem(Context context, Item item) throws SQLException {
        return findByItem(context, item, -1, -1, false);
    }

    @Override
    public List<Relationship> findByItem(
        Context context, Item item, Integer limit, Integer offset, boolean excludeTilted
    ) throws SQLException {
        return findByItem(context, item, limit, offset, excludeTilted, true);
    }

    @Override
    public List<Relationship> findByItem(
        Context context, Item item, Integer limit, Integer offset, boolean excludeTilted, boolean excludeNonLatest
    ) throws SQLException {
        List<Relationship> list =
            relationshipDAO.findByItem(context, item, limit, offset, excludeTilted, excludeNonLatest);

        list.sort((o1, o2) -> {
            int relationshipType = o1.getRelationshipType().getLeftwardType()
                .compareTo(o2.getRelationshipType().getLeftwardType());
            if (relationshipType != 0) {
                return relationshipType;
            } else {
                if (o1.getLeftItem() == item) {
                    return o1.getLeftPlace() - o2.getLeftPlace();
                } else {
                    return o1.getRightPlace() - o2.getRightPlace();
                }
            }
        });
        return list;
    }

    @Override
    public List<Relationship> findAll(Context context) throws SQLException {
        return findAll(context, -1, -1);
    }

    @Override
    public List<Relationship> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        return relationshipDAO.findAll(context, Relationship.class, limit, offset);
    }

    @Override
    public void update(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(relationship));

    }

    @Override
    public void update(Context context, List<Relationship> relationships) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(relationships)) {
            for (Relationship relationship : relationships) {
                if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
                    authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                    if (isRelationshipValidToCreate(context, relationship)) {
                        relationshipDAO.save(context, relationship);
                    }
                } else {
                    throw new AuthorizeException("You do not have write rights on this relationship's items");
                }
            }
        }
    }

    @Override
    public void delete(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        delete(context, relationship, relationship.getRelationshipType().isCopyToLeft(),
               relationship.getRelationshipType().isCopyToRight());
    }

    @Override
    public void delete(Context context, Relationship relationship, boolean copyToLeftItem, boolean copyToRightItem)
        throws SQLException, AuthorizeException {
        log.info(org.dspace.core.LogHelper.getHeader(context, "delete_relationship",
                                                      "relationship_id=" + relationship.getID() + "&" +
                                                          "copyMetadataValuesToLeftItem=" + copyToLeftItem + "&" +
                                                          "copyMetadataValuesToRightItem=" + copyToRightItem));
        if (isRelationshipValidToDelete(context, relationship) &&
            copyToItemPermissionCheck(context, relationship, copyToLeftItem, copyToRightItem)) {
            // To delete a relationship, a user must have WRITE permissions on one of the related Items
            deleteRelationshipAndCopyToItem(context, relationship, copyToLeftItem, copyToRightItem);

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    @Override
    public void forceDelete(Context context, Relationship relationship, boolean copyToLeftItem, boolean copyToRightItem)
        throws SQLException, AuthorizeException {
        log.info(org.dspace.core.LogHelper.getHeader(context, "delete_relationship",
                                                      "relationship_id=" + relationship.getID() + "&" +
                                                          "copyMetadataValuesToLeftItem=" + copyToLeftItem + "&" +
                                                          "copyMetadataValuesToRightItem=" + copyToRightItem));
        if (copyToItemPermissionCheck(context, relationship, copyToLeftItem, copyToRightItem)) {
            // To delete a relationship, a user must have WRITE permissions on one of the related Items
            deleteRelationshipAndCopyToItem(context, relationship, copyToLeftItem, copyToRightItem);

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    private void deleteRelationshipAndCopyToItem(Context context, Relationship relationship, boolean copyToLeftItem,
                                                 boolean copyToRightItem) throws SQLException, AuthorizeException {
        copyMetadataValues(context, relationship, copyToLeftItem, copyToRightItem);
        if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
            relationshipDAO.delete(context, relationship);
            updatePlaceInRelationship(context, relationship, null, null, false, false);
            updateItemsInRelationship(context, relationship);
        } else {
            throw new AuthorizeException(
                "You do not have write rights on this relationship's items");
        }
    }



    /**
     * Utility method to ensure discovery is updated for the 2 items
     * This method is used when creating, modifying or deleting a relationship
     * The virtual metadata of the 2 items may need to be updated, so they should be re-indexed
     *
     * @param context           The relevant DSpace context
     * @param relationship      The relationship which has been created, updated or deleted
     * @throws SQLException     If something goes wrong
     */
    private void updateItemsInRelationship(Context context, Relationship relationship) throws SQLException {
        // Since this call is performed after creating, updating or deleting the relationships, the permissions have
        // already been verified. The following updateItem calls can however call the
        // ItemService.update() functions which would fail if the user doesn't have permission on both items.
        // Since we allow this edits to happen under these circumstances, we need to turn off the
        // authorization system here so that this failure doesn't happen when the items need to be update
        context.turnOffAuthorisationSystem();
        try {
            // Set a limit on the total amount of items to update at once during a relationship change
            int max = configurationService.getIntProperty("relationship.update.relateditems.max", 20);
            // Set a limit on the total depth of relationships to traverse during a relationship change
            int maxDepth = configurationService.getIntProperty("relationship.update.relateditems.maxdepth", 5);
            // This is the list containing all items which will have changes to their virtual metadata
            List<Item> itemsToUpdate = new ArrayList<>();
            itemsToUpdate.add(relationship.getLeftItem());
            itemsToUpdate.add(relationship.getRightItem());

            if (containsVirtualMetadata(relationship.getRelationshipType().getLeftwardType())) {
                findModifiedDiscoveryItemsForCurrentItem(context, relationship.getLeftItem(),
                                           itemsToUpdate, max, 0, maxDepth);
            }
            if (containsVirtualMetadata(relationship.getRelationshipType().getRightwardType())) {
                findModifiedDiscoveryItemsForCurrentItem(context, relationship.getRightItem(),
                                            itemsToUpdate, max, 0, maxDepth);
            }

            for (Item item : itemsToUpdate) {
                updateItem(context, item);
            }
        } catch (AuthorizeException e) {
            log.error("Authorization Exception while authorization has been disabled", e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Search for items whose metadata should be updated in discovery and adds them to itemsToUpdate
     * It starts from the given item, excludes items already in itemsToUpdate (they're already handled),
     * and can be limited in amount of items or depth to update
     */
    private void findModifiedDiscoveryItemsForCurrentItem(Context context, Item item, List<Item> itemsToUpdate,
                                                          int max, int currentDepth, int maxDepth)
        throws SQLException {
        if (itemsToUpdate.size() >= max) {
            log.debug("skipping findModifiedDiscoveryItemsForCurrentItem for item "
                    + item.getID() + " due to " + itemsToUpdate.size() + " items to be updated");
            return;
        }
        if (currentDepth == maxDepth) {
            log.debug("skipping findModifiedDiscoveryItemsForCurrentItem for item "
                    + item.getID() + " due to " + currentDepth + " depth");
            return;
        }
        String entityTypeStringFromMetadata = itemService.getEntityTypeLabel(item);
        EntityType actualEntityType = entityTypeService.findByEntityType(context, entityTypeStringFromMetadata);
        // Get all types of relations for the current item
        List<RelationshipType> relationshipTypes = relationshipTypeService.findByEntityType(context, actualEntityType);
        for (RelationshipType relationshipType : relationshipTypes) {
            //are we searching for items where the current item is on the left
            boolean isLeft = relationshipType.getLeftType().equals(actualEntityType);

            // Verify whether there's virtual metadata configured for this type of relation
            // If it's not present, we don't need to update the virtual metadata in discovery
            String typeToSearchInVirtualMetadata;
            if (isLeft) {
                typeToSearchInVirtualMetadata = relationshipType.getRightwardType();
            } else {
                typeToSearchInVirtualMetadata = relationshipType.getLeftwardType();
            }
            if (containsVirtualMetadata(typeToSearchInVirtualMetadata)) {
                // we have a relationship type where the items attached to the current item will inherit
                // virtual metadata from the current item
                // retrieving the actual relationships so the related items can be updated
                List<Relationship> list = findByItemAndRelationshipType(context, item, relationshipType, isLeft);
                for (Relationship foundRelationship : list) {
                    Item nextItem;
                    if (isLeft) {
                        // current item on the left, next item is on the right
                        nextItem = foundRelationship.getRightItem();
                    } else {
                        nextItem = foundRelationship.getLeftItem();
                    }

                    // verify it hasn't been processed yet
                    if (!itemsToUpdate.contains(nextItem)) {
                        itemsToUpdate.add(nextItem);
                        // continue the process for the next item, it may also inherit item from the current item
                        findModifiedDiscoveryItemsForCurrentItem(context, nextItem,
                                itemsToUpdate, max, currentDepth + 1, maxDepth);
                    }
                }
            } else {
                log.debug("skipping " + relationshipType.getID()
                        + " in findModifiedDiscoveryItemsForCurrentItem for item "
                        + item.getID() + " because no relevant virtual metadata was found");
            }
        }
    }

    /**
     * Verifies whether there is virtual metadata generated for the given relationship
     * If no such virtual metadata exists, there's no need to update the items in discovery
     * @param typeToSearchInVirtualMetadata     a leftWardType or rightWardType of a relationship type
     *                                          This can be e.g. isAuthorOfPublication
     * @return                                  true if there is virtual metadata for this relationship
     */
    private boolean containsVirtualMetadata(String typeToSearchInVirtualMetadata) {
        return virtualMetadataPopulator.getMap().containsKey(typeToSearchInVirtualMetadata)
                && virtualMetadataPopulator.getMap().get(typeToSearchInVirtualMetadata).size() > 0;
    }

    /**
     * Converts virtual metadata from RelationshipMetadataValue objects to actual item metadata.
     * The resulting MDVs are added in front or behind the Relationship's virtual MDVs.
     * The Relationship's virtual MDVs may be shifted right, and all subsequent metadata will be shifted right.
     * So this method ensures the places are still valid.
     *
     * @param context           The relevant DSpace context
     * @param relationship      The relationship containing the left and right items
     * @param copyToLeftItem    The boolean indicating whether we want to write to left item or not
     * @param copyToRightItem   The boolean indicating whether we want to write to right item or not
     */
    private void copyMetadataValues(Context context, Relationship relationship, boolean copyToLeftItem,
                                    boolean copyToRightItem)
        throws SQLException, AuthorizeException {
        if (copyToLeftItem) {
            String entityTypeString = itemService.getEntityTypeLabel(relationship.getLeftItem());
            List<RelationshipMetadataValue> relationshipMetadataValues =
                relationshipMetadataService.findRelationshipMetadataValueForItemRelationship(context,
                    relationship.getLeftItem(), entityTypeString, relationship, true);
            for (RelationshipMetadataValue relationshipMetadataValue : relationshipMetadataValues) {
                // This adds the plain text metadata values on the same spot as the virtual values.
                // This will be overruled in org.dspace.content.DSpaceObjectServiceImpl.update
                //   in the line below but it's not important whether the plain text or virtual values end up on top.
                // The virtual values will eventually be deleted, and the others shifted
                // This is required because addAndShiftRightMetadata has issues on metadata fields containing
                //   relationship values which are not useForPlace, while the relationhip type has useForPlace
                // E.g. when using addAndShiftRightMetadata on relation.isAuthorOfPublication, it will break the order
                // from dc.contributor.author
                itemService.addMetadata(context, relationship.getLeftItem(),
                                                     relationshipMetadataValue.getMetadataField().
                                                         getMetadataSchema().getName(),
                                                     relationshipMetadataValue.getMetadataField().getElement(),
                                                     relationshipMetadataValue.getMetadataField().getQualifier(),
                                                     relationshipMetadataValue.getLanguage(),
                                                     relationshipMetadataValue.getValue(), null, -1,
                                                     relationshipMetadataValue.getPlace());
            }
            //This will ensure the new values no longer overlap, but won't break the order
            itemService.update(context, relationship.getLeftItem());
        }
        if (copyToRightItem) {
            String entityTypeString = itemService.getEntityTypeLabel(relationship.getRightItem());
            List<RelationshipMetadataValue> relationshipMetadataValues =
                relationshipMetadataService.findRelationshipMetadataValueForItemRelationship(context,
                    relationship.getRightItem(), entityTypeString, relationship, true);
            for (RelationshipMetadataValue relationshipMetadataValue : relationshipMetadataValues) {
                itemService.addMetadata(context, relationship.getRightItem(),
                                                     relationshipMetadataValue.getMetadataField().
                                                         getMetadataSchema().getName(),
                                                     relationshipMetadataValue.getMetadataField().getElement(),
                                                     relationshipMetadataValue.getMetadataField().getQualifier(),
                                                     relationshipMetadataValue.getLanguage(),
                                                     relationshipMetadataValue.getValue(), null, -1,
                                                     relationshipMetadataValue.getPlace());
            }
            itemService.update(context, relationship.getRightItem());
        }
    }

    /**
     * This method will check if the current user has sufficient rights to write to the respective items if requested
     * @param context           The relevant DSpace context
     * @param relationship      The relationship containing the left and right items
     * @param copyToLeftItem    The boolean indicating whether we want to write to left item or not
     * @param copyToRightItem   The boolean indicating whether we want to write to right item or not
     * @return                  A boolean indicating whether the permissions are okay for this request
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    private boolean copyToItemPermissionCheck(Context context, Relationship relationship,
                                              boolean copyToLeftItem, boolean copyToRightItem) throws SQLException {
        boolean isPermissionCorrect = true;
        if (copyToLeftItem) {
            if (!authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE)) {
                isPermissionCorrect = false;
            }
        }
        if (copyToRightItem) {
            if (!authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                isPermissionCorrect = false;
            }
        }
        return isPermissionCorrect;
    }

    private boolean isRelationshipValidToDelete(Context context, Relationship relationship) throws SQLException {
        if (relationship == null) {
            log.warn("The relationship has been deemed invalid since the relation was null");
            return false;
        }
        if (relationship.getID() == null) {
            log.warn("The relationship has been deemed invalid since the ID" +
                         " off the given relationship was null");
            return false;
        }
        if (this.find(context, relationship.getID()) == null) {
            log.warn("The relationship has been deemed invalid since the relationship" +
                         " is not present in the DB with the current ID");
            logRelationshipTypeDetailsForError(relationship.getRelationshipType());
            return false;
        }
        if (!checkMinCardinality(context, relationship.getLeftItem(),
                                 relationship, relationship.getRelationshipType().getLeftMinCardinality(), true)) {
            log.warn("The relationship has been deemed invalid since the leftMinCardinality" +
                         " constraint would be violated upon deletion");
            logRelationshipTypeDetailsForError(relationship.getRelationshipType());
            return false;
        }

        if (!checkMinCardinality(context, relationship.getRightItem(),
                                 relationship, relationship.getRelationshipType().getRightMinCardinality(), false)) {
            log.warn("The relationship has been deemed invalid since the rightMinCardinality" +
                         " constraint would be violated upon deletion");
            logRelationshipTypeDetailsForError(relationship.getRelationshipType());
            return false;
        }
        return true;
    }

    private boolean checkMinCardinality(Context context, Item item,
                                        Relationship relationship,
                                        Integer minCardinality, boolean isLeft) throws SQLException {
        List<Relationship> list = this.findByItemAndRelationshipType(context, item, relationship.getRelationshipType(),
                                                                     isLeft, -1, -1);
        if (minCardinality != null && !(list.size() > minCardinality)) {
            return false;
        }
        return true;
    }

    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, boolean isLeft)
        throws SQLException {
        return this.findByItemAndRelationshipType(context, item, relationshipType, isLeft, -1, -1);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType)
        throws SQLException {
        return findByItemAndRelationshipType(context, item, relationshipType, -1, -1, true);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, int limit, int offset)
            throws SQLException {
        return findByItemAndRelationshipType(context, item, relationshipType, limit, offset, true);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(
        Context context, Item item, RelationshipType relationshipType, int limit, int offset, boolean excludeNonLatest
    ) throws SQLException {
        return relationshipDAO
            .findByItemAndRelationshipType(context, item, relationshipType, limit, offset, excludeNonLatest);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(
        Context context, Item item, RelationshipType relationshipType, boolean isLeft, int limit, int offset
    ) throws SQLException {
        return findByItemAndRelationshipType(context, item, relationshipType, isLeft, limit, offset, true);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(
        Context context, Item item, RelationshipType relationshipType, boolean isLeft, int limit, int offset,
        boolean excludeNonLatest
    ) throws SQLException {
        return relationshipDAO
            .findByItemAndRelationshipType(context, item, relationshipType, isLeft, limit, offset, excludeNonLatest);
    }

    @Override
    public List<ItemUuidAndRelationshipId> findByLatestItemAndRelationshipType(
        Context context, Item latestItem, RelationshipType relationshipType, boolean isLeft
    ) throws SQLException {
        return relationshipDAO
            .findByLatestItemAndRelationshipType(context, latestItem, relationshipType, isLeft);
    }

    @Override
    public List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType)
        throws SQLException {

        return findByRelationshipType(context, relationshipType, -1, -1);
    }

    @Override
    public List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType, Integer limit,
                                                     Integer offset)
        throws SQLException {
        return relationshipDAO.findByRelationshipType(context, relationshipType, limit, offset);
    }

    @Override
    public List<Relationship> findByTypeName(Context context, String typeName)
            throws SQLException {
        return this.findByTypeName(context, typeName, -1, -1);
    }

    @Override
    public List<Relationship> findByTypeName(Context context, String typeName, Integer limit, Integer offset)
            throws SQLException {
        return relationshipDAO.findByTypeName(context, typeName, limit, offset);
    }


    @Override
    public int countTotal(Context context) throws SQLException {
        return relationshipDAO.countRows(context);
    }

    @Override
    public int countByItem(Context context, Item item) throws SQLException {
        return countByItem(context, item, false, true);
    }

    @Override
    public int countByItem(
        Context context, Item item, boolean excludeTilted, boolean excludeNonLatest
    ) throws SQLException {
        return relationshipDAO.countByItem(context, item, excludeTilted, excludeNonLatest);
    }

    @Override
    public int countByRelationshipType(Context context, RelationshipType relationshipType) throws SQLException {
        return relationshipDAO.countByRelationshipType(context, relationshipType);
    }

    @Override
    public int countByItemAndRelationshipType(
        Context context, Item item, RelationshipType relationshipType, boolean isLeft
    ) throws SQLException {
        return countByItemAndRelationshipType(context, item, relationshipType, isLeft, true);
    }

    @Override
    public int countByItemAndRelationshipType(
        Context context, Item item, RelationshipType relationshipType, boolean isLeft, boolean excludeNonLatest
    ) throws SQLException {
        return relationshipDAO
            .countByItemAndRelationshipType(context, item, relationshipType, isLeft, excludeNonLatest);
    }

    @Override
    public int countByTypeName(Context context, String typeName)
            throws SQLException {
        return relationshipDAO.countByTypeName(context, typeName);
    }

    @Override
    public List<Relationship> findByItemRelationshipTypeAndRelatedList(Context context, UUID focusUUID,
            RelationshipType relationshipType, List<UUID> items, boolean isLeft,
            int offset, int limit) throws SQLException {
        return relationshipDAO
               .findByItemAndRelationshipTypeAndList(context, focusUUID, relationshipType, items, isLeft, offset,limit);
    }

    @Override
    public int countByItemRelationshipTypeAndRelatedList(Context context, UUID focusUUID,
           RelationshipType relationshipType, List<UUID> items, boolean isLeft) throws SQLException {
        return relationshipDAO
               .countByItemAndRelationshipTypeAndList(context, focusUUID, relationshipType, items, isLeft);
    }
}
