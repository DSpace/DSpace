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
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
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
    public Relationship create(Context c, Item leftItem, Item rightItem, RelationshipType relationshipType,
                               int leftPlace, int rightPlace, String leftwardValue, String rightwardValue)
            throws AuthorizeException, SQLException {
        Relationship relationship = new Relationship();
        relationship.setLeftItem(leftItem);
        relationship.setRightItem(rightItem);
        relationship.setRelationshipType(relationshipType);
        relationship.setLeftPlace(leftPlace);
        relationship.setRightPlace(rightPlace);
        relationship.setLeftwardValue(leftwardValue);
        relationship.setRightwardValue(rightwardValue);
        return create(c, relationship);
    }

    @Override
    public Relationship create(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        if (isRelationshipValidToCreate(context, relationship)) {
            if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
                authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                // This order of execution should be handled in the creation (create, updateplace, update relationship)
                // for a proper place allocation
                Relationship relationshipToReturn = relationshipDAO.create(context, relationship);
                updatePlaceInRelationship(context, relationshipToReturn);
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
    public void updatePlaceInRelationship(Context context, Relationship relationship)
        throws SQLException, AuthorizeException {
        Item leftItem = relationship.getLeftItem();
        // Max value is used to ensure that these will get added to the back of the list and thus receive the highest
        // (last) place as it's set to a -1 for creation
        if (relationship.getLeftPlace() == -1) {
            relationship.setLeftPlace(Integer.MAX_VALUE);
        }
        Item rightItem = relationship.getRightItem();
        if (relationship.getRightPlace() == -1) {
            relationship.setRightPlace(Integer.MAX_VALUE);
        }
        List<Relationship> leftRelationships = findByItemAndRelationshipType(context,
                                                                             leftItem,
                                                                             relationship.getRelationshipType(), true);
        List<Relationship> rightRelationships = findByItemAndRelationshipType(context,
                                                                              rightItem,
                                                                              relationship.getRelationshipType(),
                                                                              false);

        // These relationships are only deleted from the temporary lists incase they're present in them so that we can
        // properly perform our place calculation later down the line in this method.
        if (leftRelationships.contains(relationship)) {
            leftRelationships.remove(relationship);
        }
        if (rightRelationships.contains(relationship)) {
            rightRelationships.remove(relationship);
        }
        context.turnOffAuthorisationSystem();
        //If useForPlace for the leftwardType is false for the relationshipType,
        // we need to sort the relationships here based on leftplace.
        if (!virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), true)) {
            if (!leftRelationships.isEmpty()) {
                leftRelationships.sort(Comparator.comparingInt(Relationship::getLeftPlace));
                for (int i = 0; i < leftRelationships.size(); i++) {
                    leftRelationships.get(i).setLeftPlace(i);
                }
                relationship.setLeftPlace(leftRelationships.size());
            } else {
                relationship.setLeftPlace(0);
            }
        } else {
            updateItem(context, leftItem);

        }

        //If useForPlace for the rightwardType is false for the relationshipType,
        // we need to sort the relationships here based on the rightplace.
        if (!virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), false)) {
            if (!rightRelationships.isEmpty()) {
                rightRelationships.sort(Comparator.comparingInt(Relationship::getRightPlace));
                for (int i = 0; i < rightRelationships.size(); i++) {
                    rightRelationships.get(i).setRightPlace(i);
                }
                relationship.setRightPlace(rightRelationships.size());
            } else {
                relationship.setRightPlace(0);
            }

        } else {
            updateItem(context, rightItem);

        }
        context.restoreAuthSystemState();

    }

    @Override
    public void updateItem(Context context, Item relatedItem)
        throws SQLException, AuthorizeException {
        relatedItem.setMetadataModified();
        itemService.update(context, relatedItem);
    }

    @Override
    public int findNextLeftPlaceByLeftItem(Context context, Item item) throws SQLException {
        return relationshipDAO.findNextLeftPlaceByLeftItem(context, item);
    }

    @Override
    public int findNextRightPlaceByRightItem(Context context, Item item) throws SQLException {
        return relationshipDAO.findNextRightPlaceByRightItem(context, item);
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
        if (!verifyMaxCardinality(context, relationship.getLeftItem(),
                                  relationshipType.getLeftMaxCardinality(), relationshipType, true)) {
            log.warn("The relationship has been deemed invalid since the left item has more" +
                         " relationships than the left max cardinality allows after we'd store this relationship");
            logRelationshipTypeDetailsForError(relationshipType);
            return false;
        }
        if (!verifyMaxCardinality(context, relationship.getRightItem(),
                                  relationshipType.getRightMaxCardinality(), relationshipType, false)) {
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
    public List<Relationship> findByItem(Context context, Item item, Integer limit, Integer offset,
                                         boolean excludeTilted) throws SQLException {

        List<Relationship> list = relationshipDAO.findByItem(context, item, limit, offset, excludeTilted);

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
            updatePlaceInRelationship(context, relationship);
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
        String entityTypeStringFromMetadata = relationshipMetadataService.getEntityTypeStringFromMetadata(item);
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
            String entityTypeString = relationshipMetadataService
                .getEntityTypeStringFromMetadata(relationship.getLeftItem());
            List<RelationshipMetadataValue> relationshipMetadataValues =
                relationshipMetadataService.findRelationshipMetadataValueForItemRelationship(context,
                    relationship.getLeftItem(), entityTypeString, relationship, true);
            for (RelationshipMetadataValue relationshipMetadataValue : relationshipMetadataValues) {
                itemService.addAndShiftRightMetadata(context, relationship.getLeftItem(),
                                                     relationshipMetadataValue.getMetadataField().
                                                         getMetadataSchema().getName(),
                                                     relationshipMetadataValue.getMetadataField().getElement(),
                                                     relationshipMetadataValue.getMetadataField().getQualifier(),
                                                     relationshipMetadataValue.getLanguage(),
                                                     relationshipMetadataValue.getValue(), null, -1,
                                                     relationshipMetadataValue.getPlace());
            }
            itemService.update(context, relationship.getLeftItem());
        }
        if (copyToRightItem) {
            String entityTypeString = relationshipMetadataService
                .getEntityTypeStringFromMetadata(relationship.getRightItem());
            List<RelationshipMetadataValue> relationshipMetadataValues =
                relationshipMetadataService.findRelationshipMetadataValueForItemRelationship(context,
                    relationship.getRightItem(), entityTypeString, relationship, true);
            for (RelationshipMetadataValue relationshipMetadataValue : relationshipMetadataValues) {
                itemService.addAndShiftRightMetadata(context, relationship.getRightItem(),
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
        return relationshipDAO.findByItemAndRelationshipType(context, item, relationshipType, -1, -1);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, int limit, int offset)
            throws SQLException {
        return relationshipDAO.findByItemAndRelationshipType(context, item, relationshipType, limit, offset);
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, boolean isLeft,
                                                            int limit, int offset)
            throws SQLException {
        return relationshipDAO.findByItemAndRelationshipType(context, item, relationshipType, isLeft, limit, offset);
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
        return relationshipDAO.countByItem(context, item);
    }

    @Override
    public int countByRelationshipType(Context context, RelationshipType relationshipType) throws SQLException {
        return relationshipDAO.countByRelationshipType(context, relationshipType);
    }

    @Override
    public int countByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType,
                                              boolean isLeft) throws SQLException {
        return relationshipDAO.countByItemAndRelationshipType(context, item, relationshipType, isLeft);
    }

    @Override
    public int countByTypeName(Context context, String typeName)
            throws SQLException {
        return relationshipDAO.countByTypeName(context, typeName);
    }
}
