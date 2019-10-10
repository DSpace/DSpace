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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;

/**
 * This Service will use the DAO classes to access the information about Relationships from the database
 */
public interface RelationshipService extends DSpaceCRUDService<Relationship> {

    /**
     * Retrieves the list of Relationships currently in the system for which the given Item is either
     * a leftItem or a rightItem object
     * @param context   The relevant DSpace context
     * @param item      The Item that has to be the left or right item for the relationship to be included in the list
     * @return          The list of relationships for which each relationship adheres to the above listed constraint
     * @throws SQLException If something goes wrong
     */
    public List<Relationship> findByItem(Context context,Item item) throws SQLException;

    /**
     * Retrieves the full list of relationships currently in the system
     * @param context   The relevant DSpace context
     * @return  The list of all relationships currently in the system
     * @throws SQLException If something goes wrong
     */
    public List<Relationship> findAll(Context context) throws SQLException;

    /**
     * This method creates a relationship object in the database equal to the given relationship param
     * if this is a valid relationship
     * @param context       The relevant DSpace context
     * @param relationship  The relationship that will be created in the database if it is valid
     * @return              The created relationship with updated place variables
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong with authorizations
     */
    public Relationship create(Context context, Relationship relationship) throws SQLException, AuthorizeException;

    /**
     * Retrieves the highest integer value for the leftplace property of a Relationship for all relationships
     * that have the given item as a left item
     * @param context   The relevant DSpace context
     * @param item      The item that has to be the leftItem of a relationship for it to qualify
     * @return          The integer value of the highest left place property of all relationships
     *                  that have the given item as a leftitem property
     * @throws SQLException If something goes wrong
     */
    int findLeftPlaceByLeftItem(Context context, Item item) throws SQLException;

    /**
     * Retrieves the highest integer value for the rightplace property of a Relationship for all relationships
     * that have the given item as a right item
     * @param context   The relevant DSpace context
     * @param item      The item that has to be the rightitem of a relationship for it to qualify
     * @return          The integer value of the highest right place property of all relationships
     *                  that have the given item as a rightitem property
     * @throws SQLException If something goes wrong
     */
    int findRightPlaceByRightItem(Context context, Item item) throws SQLException;

    /**
     * This method returns a list of Relationships for which the leftItem or rightItem is equal to the given
     * Item object and for which the RelationshipType object is equal to the relationshipType property
     * @param context           The relevant DSpace context
     * @param item              The Item object to be matched on the leftItem or rightItem for the relationship
     * @param relationshipType  The RelationshipType object that will be used to check the Relationship on
     * @return  The list of Relationship objects that have the given Item object as leftItem or rightItem and
     *          for which the relationshipType property is equal to the given RelationshipType
     * @throws SQLException If something goes wrong
     */
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType)
        throws SQLException;

    /**
     * This method will update the place for the Relationship and all other relationships found by the items and
     * relationship type of the given Relationship. It will give this Relationship the last place in both the
     * left and right place determined by querying for the list of leftRelationships and rightRelationships
     * by the leftItem, rightItem and relationshipType of the given Relationship.
     * @param context           The relevant DSpace context
     * @param relationship      The Relationship object that will have it's place updated and that will be used
     *                          to retrieve the other relationships whose place might need to be updated
     * @param isCreation        Is the relationship new or did it already exist
     * @throws SQLException     If something goes wrong
     */
    public void updatePlaceInRelationship(Context context, Relationship relationship, boolean isCreation)
            throws SQLException, AuthorizeException;

    /**
     * This method will update the given item's metadata order.
     * If the relationships for the item have been modified and will calculate the place based on a
     * metadata field, this function will ensure the place is calculated.
     * @param context           The relevant DSpace context
     * @param relatedItem       The Item for which the list of Relationship location is calculated
     *                          based on a metadata field
     * @throws SQLException     If something goes wrong
     * @throws AuthorizeException
     *                          If the user is not authorized to update the item
     */
    public void updateItem(Context context, Item relatedItem) throws SQLException, AuthorizeException;


    /**
     * This method returns a list of Relationship objects for which the relationshipType property is equal to the given
     * RelationshipType object
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType object that will be used to check the Relationship on
     * @return  The list of Relationship objects for which the given RelationshipType object is equal
     *          to the relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType) throws SQLException;

    /**
     * This method is used to construct a Relationship object with all it's variables
     * @param c                 The relevant DSpace context
     * @param leftItem          The leftItem Item object for the relationship
     * @param rightItem         The rightItem Item object for the relationship
     * @param relationshipType  The RelationshipType object for the relationship
     * @param leftPlace         The leftPlace integer for the relationship
     * @param rightPlace        The rightPlace integer for the relationship
     * @param leftwardValue     The leftwardValue string for the relationship
     * @param rightwardValue    The rightwardValue string for the relationship
     * @return                  The created Relationship object with the given properties
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    Relationship create(Context c, Item leftItem, Item rightItem, RelationshipType relationshipType,
                        int leftPlace, int rightPlace, String leftwardValue, String rightwardValue)
        throws AuthorizeException, SQLException;


    /**
     * This method is used to construct a Relationship object with all it's variables,
     * except the leftward and rightward labels
     * @param c                 The relevant DSpace context
     * @param leftItem          The leftItem Item object for the relationship
     * @param rightItem         The rightItem Item object for the relationship
     * @param relationshipType  The RelationshipType object for the relationship
     * @param leftPlace         The leftPlace integer for the relationship
     * @param rightPlace        The rightPlace integer for the relationship
     * @return                  The created Relationship object with the given properties
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    Relationship create(Context c, Item leftItem, Item rightItem, RelationshipType relationshipType,
                        int leftPlace, int rightPlace)
            throws AuthorizeException, SQLException;
}