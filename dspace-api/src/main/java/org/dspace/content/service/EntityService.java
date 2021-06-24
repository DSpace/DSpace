/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;

/**
 * This Service provides us with a few methods to return objects based on the Entity object.
 * Since the Entity object isn't a database object, this method mostly outsources to getters for other services
 * to return the wanted objects to then check for properties on either the list of relationships or the item included
 * in the Entity.
 */
public interface EntityService {

    /**
     * This will construct an Entity object that will be returned with the Item that matches the ItemID that was
     * passed along
     * as well as a list of relationships for that Item.
     * @param context   The relevant DSpace context
     * @param itemId    The ItemID for the Item that is to be used in the Entity object
     * @return The constructed Entity object with the Item and the list of relationships
     * @throws SQLException If something goes wrong
     */
    Entity findByItemId(Context context, UUID itemId) throws SQLException;

    /**
     * This will construct an Entity object that will be returned with the Item that matches the ItemID that was
     * passed along
     * as well as a list of relationships for that Item.
     * @param context   The relevant DSpace context
     * @param itemId    The ItemID for the Item that is to be used in the Entity object
     * @param limit     paging limit
     * @param offset    paging offset
     * @return The constructed Entity object with the Item and the list of relationships
     * @throws SQLException If something goes wrong
     */
    Entity findByItemId(Context context, UUID itemId, Integer limit, Integer offset) throws SQLException;

    /**
     * Returns the EntityType for the Item that is attached to the Entity that is passed along to this method.
     * The EntityType String logic is in the Metadata for that Item and will be searched on in the EntityTypeService
     * to retrieve the actual EntityType object
     * @param context   The relevant DSpace context
     * @param entity    The Entity object which contains the Item
     * @return The EntityType that belongs to this Item
     * @throws SQLException If something goes wrong
     */
    EntityType getType(Context context, Entity entity) throws SQLException;

    /**
     * Retrieves the list of relationships, which are attached to the Entity object that is passed along, where the
     * left item object of each relationship is equal to the Item object of the Entity object that is passed along
     * @param context   The relevant DSpace context
     * @param entity    The Entity object to be returned
     * @return The list of relationships that have the Item in the Entity object as their left item
     */
    List<Relationship> getLeftRelations(Context context, Entity entity);

    /**
     * Retrieves the list of relationships, which are attached to the Entity object that is passed along, where the
     * right item object of each relationship is equal to the Item object of the Entity object that is passed along
     * @param context   The relevant DSpace context
     * @param entity    The Entity object to be returned
     * @return The list of relationships that have the Item in the Entity object as their right item
     */
    List<Relationship> getRightRelations(Context context, Entity entity);

    /**
     * Retrieves the list of relationships for which their relationshiptype has a left or right label that is
     * equal to the passed along label String
     * @param context   The relevant DSpace context
     * @param typeName     The label that needs to be in the relationshiptype of the relationship
     * @return The list of relationships that have a relationshiptype with a left or right label
     *                  that is equal to the label param
     * @throws SQLException If something goes wrong
     */
    List<Relationship> getRelationsByTypeName(Context context, String typeName) throws SQLException;

    /**
     * Retrieves the list of relationships for which their relationshiptype has a left or right label that is
     * equal to the passed along label String
     * @param context   The relevant DSpace context
     * @param typeName     The label that needs to be in the relationshiptype of the relationship
     * @param limit     paging limit
     * @param offset    paging offset
     * @return The list of relationships that have a relationshiptype with a left or right label
     *                  that is equal to the label param
     * @throws SQLException If something goes wrong
     */
    List<Relationship> getRelationsByTypeName(Context context, String typeName, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Retrieves the list of relationships that have a relationshiptype that contains the EntityType for the given
     * Entity
     * in either the leftEntityType or the rightEntityType variables
     * @param context   The relevant DSpace context
     * @param entity    The Entity for which the EntityType should be checked for relationships
     * @return The list of relationships that each contain a relationshiptype in which there is a right or left
     * entity type that
     *                  is equal to the entity type for the given entity
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getAllRelationshipTypes(Context context, Entity entity) throws SQLException;

    /**
     * Retrieves the list of relationships that have a relationshiptype that contains the EntityType for the given
     * Entity
     * in either the leftEntityType or the rightEntityType variables
     * @param context   The relevant DSpace context
     * @param entity    The Entity for which the EntityType should be checked for relationships
     * @param limit     paging limit
     * @param offset    paging offset
     * @return The list of relationships that each contain a relationshiptype in which there is a right or left
     * entity type that
     *                  is equal to the entity type for the given entity
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getAllRelationshipTypes(Context context, Entity entity, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Retrieves the list of relationships that have a relationshiptype that contains the EntityType for the given
     * Entity
     * in the leftEntityType
     * @param context   The relevant DSpace context
     * @param entity    The Entity for which the EntityType should be checked for relationships
     * @return The list of relationships that each contain a relationshiptype in which there is a left entity type that
     *                  is equal to the entity type for the given entity
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getLeftRelationshipTypes(Context context, Entity entity) throws SQLException;

    /**
     * Retrieves the list of relationships that have a relationshiptype that contains the EntityType for the given
     * Entity
     * in the leftEntityType
     * @param context   The relevant DSpace context
     * @param entity    The Entity for which the EntityType should be checked for relationships
     * @param isLeft    Boolean value used to filter by left_type or right_type. If true left_type results only
     *                  else right_type results.
     * @param limit     paging limit
     * @param offset    paging offset
     * @return The list of relationships that each contain a relationshiptype in which there is a left entity type that
     *                  is equal to the entity type for the given entity
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getLeftRelationshipTypes(Context context, Entity entity, boolean isLeft,
                                                    Integer limit, Integer offset) throws SQLException;

    /**
     * Retrieves the list of relationships that have a relationshiptype that contains the EntityType for the given
     * Entity
     * in the rightEntityType
     * @param context   The relevant DSpace context
     * @param entity    The Entity for which the EntityType should be checked for relationships
     * @return The list of relationships that each contain a relationshiptype in which there is a right entity type that
     *                  is equal to the entity type for the given entity
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getRightRelationshipTypes(Context context, Entity entity) throws SQLException;

    /**
     * Retrieves the list of relationships that have a relationshiptype that contains the EntityType for the given
     * Entity
     * in the rightEntityType
     * @param context   The relevant DSpace context
     * @param entity    The Entity for which the EntityType should be checked for relationships
     * @param isLeft    Boolean value used to filter by left_type or right_type. If true left_type results only
     *                  else right_type results.
     * @param limit     paging limit
     * @param offset    paging offset
     * @return The list of relationships that each contain a relationshiptype in which there is a right entity type that
     *                  is equal to the entity type for the given entity
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getRightRelationshipTypes(Context context, Entity entity, boolean isLeft,
                                                     Integer limit, Integer offset) throws SQLException;

    /**
     * Retrieves a list of RelationshipType objects for which either their left or right label is equal to the
     * label parameter that's being passed along
     * @param context   The relevant DSpace context
     * @param typeName     The typeName for which the relationshiptype's labels must be checked
     * @return The list of relationshiptypes that each contain a left or right label that is equal
     *                  to the given label parameter
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getRelationshipTypesByTypeName(Context context, String typeName) throws SQLException;

    /**
     * Retrieves a list of RelationshipType objects for which either their left or right label is equal to the
     * label parameter that's being passed along
     * @param context   The relevant DSpace context
     * @param type     The label for which the relationshiptype's labels must be checked
     * @param limit     paging limit
     * @param offset    paging offset
     * @return The list of relationshiptypes that each contain a left or right label that is equal
     *                  to the given label parameter
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> getRelationshipTypesByTypeName(Context context, String type,
                                                       Integer limit, Integer offset) throws SQLException;

}
