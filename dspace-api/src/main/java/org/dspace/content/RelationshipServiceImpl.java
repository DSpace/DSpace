/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
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
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
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
        Relationship relationship = createRelationshipObject(c, leftItem, rightItem, relationshipType,
                leftPlace, rightPlace, leftwardValue, rightwardValue);
        return create(c, relationship);
    }

    @Override
    public Relationship create(Context c, Item leftItem, Item rightItem, RelationshipType relationshipType,
                               boolean bypassValidation)
            throws AuthorizeException, SQLException {
        Relationship relationship = createRelationshipObject(c, leftItem, rightItem, relationshipType,
                0, 0, null, null);
        return create(c, relationship, bypassValidation);
    }

    private Relationship createRelationshipObject(Context c, Item leftItem, Item rightItem,
            RelationshipType relationshipType, int leftPlace, int rightPlace, String leftwardValue,
            String rightwardValue)
        throws AuthorizeException, SQLException {
        Relationship relationship = new Relationship();
        relationship.setLeftItem(leftItem);
        relationship.setRightItem(rightItem);
        relationship.setRelationshipType(relationshipType);
        relationship.setLeftPlace(leftPlace);
        relationship.setRightPlace(rightPlace);
        relationship.setLeftwardValue(leftwardValue);
        relationship.setRightwardValue(rightwardValue);
        return relationship;
    }

    @Override
    public Relationship create(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        if (isRelationshipValidToCreate(context, relationship)) {
            return createRelationshipDAO(context, relationship);
        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    @Override
    public Relationship create(Context context, Relationship relationship, boolean bypassValidation)
            throws SQLException, AuthorizeException {
        if (isRelationshipValidToCreate(context, relationship) || bypassValidation) {
            return createRelationshipDAO(context, relationship);
        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    private Relationship createRelationshipDAO(Context context, Relationship relationship)
        throws AuthorizeException, SQLException {
        if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
            // This order of execution should be handled in the creation (create, updateplace, update relationship)
            // for a proper place allocation
            Relationship relationshipToReturn = relationshipDAO.create(context, relationship);
            updatePlaceInRelationship(context, relationshipToReturn);
            update(context, relationshipToReturn);
            return relationshipToReturn;
        } else {
            throw new AuthorizeException(
                "You do not have write rights on this relationship's items");
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
                                  relationshipType.getLeftMaxCardinality(), relationshipType)) {
            log.warn("The relationship has been deemed invalid since the left item has more" +
                         " relationships than the left max cardinality allows after we'd store this relationship");
            logRelationshipTypeDetailsForError(relationshipType);
            return false;
        }
        if (!verifyMaxCardinality(context, relationship.getRightItem(),
                                  relationshipType.getRightMaxCardinality(), relationshipType)) {
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
                                         RelationshipType relationshipType) throws SQLException {
        List<Relationship> rightRelationships = findByItemAndRelationshipType(context, itemToProcess, relationshipType,
                                                                              false);
        if (maxCardinality != null && rightRelationships.size() >= maxCardinality) {
            return false;
        }
        return true;
    }

    private boolean verifyEntityTypes(Item itemToProcess, EntityType entityTypeToProcess) {
        List<MetadataValue> list = itemService.getMetadata(itemToProcess, "relationship", "type", null, Item.ANY);
        if (list.isEmpty()) {
            return false;
        }
        String leftEntityType = list.get(0).getValue();
        return StringUtils.equals(leftEntityType, entityTypeToProcess.getLabel());
    }

    public Relationship find(Context context, int id) throws SQLException {
        Relationship relationship = relationshipDAO.findByID(context, Relationship.class, id);
        return relationship;
    }

    @Override
    public List<Relationship> findByItem(Context context, Item item) throws SQLException {

        return findByItem(context, item, -1, -1);
    }

    @Override
    public List<Relationship> findByItem(Context context, Item item, Integer limit, Integer offset)
            throws SQLException {

        List<Relationship> list = relationshipDAO.findByItem(context, item, limit, offset);

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
    public void delete(Context context, Relationship relationship, boolean bypassValidation)
            throws SQLException, AuthorizeException {
        //TODO: retrieve default settings from configuration
        delete(context, relationship, false, false, bypassValidation);
    }

    @Override
    public void delete(Context context, Relationship relationship, boolean copyToLeftItem, boolean copyToRightItem)
        throws SQLException, AuthorizeException {
        log.info(org.dspace.core.LogManager.getHeader(context, "delete_relationship",
                                                      "relationship_id=" + relationship.getID() + "&" +
                                                          "copyMetadataValuesToLeftItem=" + copyToLeftItem + "&" +
                                                          "copyMetadataValuesToRightItem=" + copyToRightItem));
        if (isRelationshipValidToDelete(context, relationship) &&
            copyToItemPermissionCheck(context, relationship, copyToLeftItem, copyToRightItem)) {
            deleteRelationship(context, relationship, copyToLeftItem, copyToRightItem);

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    @Override
    public void delete(Context context, Relationship relationship, boolean copyToLeftItem, boolean copyToRightItem,
            boolean bypassValidation) throws SQLException, AuthorizeException {
        log.info(org.dspace.core.LogManager.getHeader(context, "delete_relationship",
                                                      "relationship_id=" + relationship.getID() + "&" +
                                                          "copyMetadataValuesToLeftItem=" + copyToLeftItem + "&" +
                                                          "copyMetadataValuesToRightItem=" + copyToRightItem));
        if ((isRelationshipValidToDelete(context, relationship) &&
            copyToItemPermissionCheck(context, relationship, copyToLeftItem, copyToRightItem)) || bypassValidation) {
            deleteRelationship(context, relationship, copyToLeftItem, copyToRightItem);

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    private void deleteRelationship(Context context, Relationship relationship, boolean copyToLeftItem,
            boolean copyToRightItem) throws SQLException, AuthorizeException {

        // To delete a relationship, a user must have WRITE permissions on one of the related Items
        copyMetadataValues(context, relationship, copyToLeftItem, copyToRightItem);
        if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
            relationshipDAO.delete(context, relationship);
            updatePlaceInRelationship(context, relationship);
        } else {
            throw new AuthorizeException(
                "You do not have write rights on this relationship's items");
        }
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
    public int countByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType)
            throws SQLException {
        return relationshipDAO.countByItemAndRelationshipType(context, item, relationshipType);
    }

    @Override
    public int countByTypeName(Context context, String typeName)
            throws SQLException {
        return relationshipDAO.countByTypeName(context, typeName);
    }
}
