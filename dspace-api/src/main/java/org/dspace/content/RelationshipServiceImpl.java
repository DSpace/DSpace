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
import java.util.LinkedList;
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
    private VirtualMetadataPopulator virtualMetadataPopulator;

    public Relationship create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can modify relationship");
        }
        return relationshipDAO.create(context, new Relationship());
    }

    public Relationship create(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        if (isRelationshipValidToCreate(context, relationship)) {
            if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
                authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                updatePlaceInRelationship(context, relationship, true);
                return relationshipDAO.create(context, relationship);
            } else {
                throw new AuthorizeException(
                    "You do not have write rights on this relationship's items");
            }

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
    }

    @Override
    public void updatePlaceInRelationship(Context context, Relationship relationship, boolean isCreation)
        throws SQLException, AuthorizeException {
        Item leftItem = relationship.getLeftItem();
        List<Relationship> leftRelationships = findByItemAndRelationshipType(context,
                                                                             leftItem,
                                                                             relationship.getRelationshipType(), true);
        Item rightItem = relationship.getRightItem();
        List<Relationship> rightRelationships = findByItemAndRelationshipType(context,
                                                                              rightItem,
                                                                              relationship.getRelationshipType(),
                                                                              false);

        context.turnOffAuthorisationSystem();
        if (!virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), true)) {
            if (!leftRelationships.isEmpty()) {
                leftRelationships.sort((o1, o2) -> o2.getLeftPlace() - o1.getLeftPlace());
                for (int i = 0; i < leftRelationships.size(); i++) {
                    leftRelationships.get(leftRelationships.size() - 1 - i).setLeftPlace(i);
                }
                relationship.setLeftPlace(leftRelationships.get(0).getLeftPlace() + 1);
            } else {
                relationship.setLeftPlace(0);
            }
        } else {
            updateItem(context, leftItem);

        }

        if (!virtualMetadataPopulator.isUseForPlaceTrueForRelationshipType(relationship.getRelationshipType(), false)) {
            if (!rightRelationships.isEmpty()) {
                rightRelationships.sort((o1, o2) -> o2.getRightPlace() - o1.getRightPlace());
                for (int i = 0; i < rightRelationships.size(); i++) {
                    rightRelationships.get(rightRelationships.size() - 1 - i).setRightPlace(i);
                }
                relationship.setRightPlace(rightRelationships.get(0).getRightPlace() + 1);
            } else {
                relationship.setRightPlace(0);
            }

        } else {
            updateItem(context, rightItem);

        }

        if (isCreation) {
            handleCreationPlaces(context, relationship);
        }
        context.restoreAuthSystemState();

    }

    public void updateItem(Context context, Item leftItem)
        throws SQLException, AuthorizeException {
        leftItem.setMetadataModified();
        itemService.update(context, leftItem);
    }


    private void handleCreationPlaces(Context context, Relationship relationship) throws SQLException {
        List<Relationship> leftRelationships;
        List<Relationship> rightRelationships;
        leftRelationships = findByItemAndRelationshipType(context,
                                                          relationship.getLeftItem(),
                                                          relationship.getRelationshipType(), true);
        rightRelationships = findByItemAndRelationshipType(context,
                                                           relationship.getRightItem(),
                                                           relationship.getRelationshipType(),
                                                           false);
        leftRelationships.sort((o1, o2) -> o2.getLeftPlace() - o1.getLeftPlace());
        rightRelationships.sort((o1, o2) -> o2.getRightPlace() - o1.getRightPlace());

        if (!leftRelationships.isEmpty()) {
            relationship.setLeftPlace(leftRelationships.get(0).getLeftPlace() + 1);
        } else {
            relationship.setLeftPlace(0);
        }

        if (!rightRelationships.isEmpty()) {
            relationship.setRightPlace(rightRelationships.get(0).getRightPlace() + 1);
        } else {
            relationship.setRightPlace(0);
        }
    }

    public int findLeftPlaceByLeftItem(Context context, Item item) throws SQLException {
        return relationshipDAO.findLeftPlaceByLeftItem(context, item);
    }

    public int findRightPlaceByRightItem(Context context, Item item) throws SQLException {
        return relationshipDAO.findRightPlaceByRightItem(context, item);
    }

    private boolean isRelationshipValidToCreate(Context context, Relationship relationship) throws SQLException {
        RelationshipType relationshipType = relationship.getRelationshipType();

        if (!verifyEntityTypes(relationship.getLeftItem(), relationshipType.getLeftType())) {
            log.warn("The relationship has been deemed invalid since the leftItem" +
                         " and leftType do no match on entityType");
            logRelationshipTypeDetails(relationshipType);
            return false;
        }
        if (!verifyEntityTypes(relationship.getRightItem(), relationshipType.getRightType())) {
            log.warn("The relationship has been deemed invalid since the rightItem" +
                         " and rightType do no match on entityType");
            logRelationshipTypeDetails(relationshipType);
            return false;
        }
        if (!verifyMaxCardinality(context, relationship.getLeftItem(),
                                  relationshipType.getLeftMaxCardinality(), relationshipType)) {
            log.warn("The relationship has been deemed invalid since the left item has more" +
                         " relationships than the left max cardinality allows after we'd store this relationship");
            logRelationshipTypeDetails(relationshipType);
            return false;
        }
        if (!verifyMaxCardinality(context, relationship.getRightItem(),
                                  relationshipType.getRightMaxCardinality(), relationshipType)) {
            log.warn("The relationship has been deemed invalid since the right item has more" +
                         " relationships than the right max cardinality allows after we'd store this relationship");
            logRelationshipTypeDetails(relationshipType);
            return false;
        }
        return true;
    }

    private void logRelationshipTypeDetails(RelationshipType relationshipType) {
        log.warn("The relationshipType's ID is: " + relationshipType.getID());
        log.warn("The relationshipType's left label is: " + relationshipType.getLeftLabel());
        log.warn("The relationshipType's right label is: " + relationshipType.getRightLabel());
        log.warn("The relationshipType's left entityType label is: " + relationshipType.getLeftType().getLabel());
        log.warn("The relationshipType's right entityType label is: " + relationshipType.getRightType().getLabel());
        log.warn("The relationshipType's left min cardinality is: " + relationshipType.getLeftMinCardinality());
        log.warn("The relationshipType's left max cardinality is: " + relationshipType.getLeftMaxCardinality());
        log.warn("The relationshipType's right min cardinality is: " + relationshipType.getRightMinCardinality());
        log.warn("The relationshipType's right max cardinality is: " + relationshipType.getRightMaxCardinality());
    }

    private boolean verifyMaxCardinality(Context context, Item itemToProcess,
                                         int maxCardinality, RelationshipType relationshipType) throws SQLException {
        List<Relationship> rightRelationships = findByItemAndRelationshipType(context, itemToProcess, relationshipType,
                                                                              false);
        if (rightRelationships.size() >= maxCardinality && maxCardinality != 0) {
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
        if (!StringUtils.equals(leftEntityType, entityTypeToProcess.getLabel())) {
            return false;
        }
        return true;
    }

    public Relationship find(Context context, int id) throws SQLException {
        Relationship relationship = relationshipDAO.findByID(context, Relationship.class, id);
        return relationship;
    }

    public List<Relationship> findByItem(Context context, Item item) throws SQLException {

        List<Relationship> list = relationshipDAO.findByItem(context, item);

        list.sort((o1, o2) -> {
            int relationshipType = o1.getRelationshipType().getLeftLabel()
                                     .compareTo(o2.getRelationshipType().getLeftLabel());
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

    public List<Relationship> findAll(Context context) throws SQLException {
        return relationshipDAO.findAll(context, Relationship.class);
    }

    public void update(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(relationship));

    }

    public void update(Context context, List<Relationship> relationships) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(relationships)) {
            // Check authorisation - only administrators can change formats
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException(
                    "Only administrators can modify relationship");
            }

            for (Relationship relationship : relationships) {
                if (isRelationshipValidToCreate(context, relationship)) {
                    relationshipDAO.save(context, relationship);
                }
            }
        }
    }

    public void delete(Context context, Relationship relationship) throws SQLException, AuthorizeException {
        if (isRelationshipValidToDelete(context, relationship)) {
            if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
                authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                relationshipDAO.delete(context, relationship);
                updatePlaceInRelationship(context, relationship, false);
            } else {
                throw new AuthorizeException(
                    "You do not have write rights on this relationship's items");
            }

        } else {
            throw new IllegalArgumentException("The relationship given was not valid");
        }
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
            logRelationshipTypeDetails(relationship.getRelationshipType());
            return false;
        }
        if (!checkMinCardinality(context, relationship.getLeftItem(),
                                 relationship, relationship.getRelationshipType().getLeftMinCardinality(), true)) {
            log.warn("The relationship has been deemed invalid since the leftMinCardinality" +
                         " constraint would be violated upon deletion");
            logRelationshipTypeDetails(relationship.getRelationshipType());
            return false;
        }

        if (!checkMinCardinality(context, relationship.getRightItem(),
                                 relationship, relationship.getRelationshipType().getRightMinCardinality(), false)) {
            log.warn("The relationship has been deemed invalid since the rightMinCardinality" +
                         " constraint would be violated upon deletion");
            logRelationshipTypeDetails(relationship.getRelationshipType());
            return false;
        }
        return true;
    }

    private boolean checkMinCardinality(Context context, Item item,
                                        Relationship relationship,
                                        int minCardinality, boolean isLeft) throws SQLException {
        List<Relationship> list = this
            .findByItemAndRelationshipType(context, item, relationship.getRelationshipType(), isLeft);
        if (!(list.size() > minCardinality)) {
            return false;
        }
        return true;
    }

    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, boolean isLeft)

        throws SQLException {
        List<Relationship> list = this.findByItem(context, item);
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : list) {
            if (isLeft) {
                if (StringUtils
                    .equals(relationship.getRelationshipType().getLeftLabel(), relationshipType.getLeftLabel())) {
                    listToReturn.add(relationship);
                }
            } else {
                if (StringUtils
                    .equals(relationship.getRelationshipType().getRightLabel(), relationshipType.getRightLabel())) {
                    listToReturn.add(relationship);
                }
            }
        }
        return listToReturn;
    }

    @Override
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType)
        throws SQLException {
        List<Relationship> list = this.findByItem(context, item);
        List<Relationship> listToReturn = new LinkedList<>();
        for (Relationship relationship : list) {
            if (relationship.getRelationshipType().equals(relationshipType)) {
                listToReturn.add(relationship);
            }
        }
        return listToReturn;
    }
}
