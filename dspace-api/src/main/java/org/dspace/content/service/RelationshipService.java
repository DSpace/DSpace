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
    public List<Relationship> findByItem(Context context, Item item) throws SQLException;

    /**
     * Retrieves the list of Relationships currently in the system for which the given Item is either
     * a leftItem or a rightItem object
     * @param context   The relevant DSpace context
     * @param item      The Item that has to be the left or right item for the relationship to be included in the list
     * @param limit     paging limit
     * @param offset    paging offset
     * @return          The list of relationships for which each relationship adheres to the above listed constraint
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByItem(Context context, Item item, Integer limit, Integer offset) throws SQLException;

    /**
     * Retrieves the full list of relationships currently in the system
     * @param context   The relevant DSpace context
     * @return  The list of all relationships currently in the system
     * @throws SQLException If something goes wrong
     */
    public List<Relationship> findAll(Context context) throws SQLException;

    /**
     * Retrieves the full list of relationships currently in the system
     * @param context   The relevant DSpace context
     * @param limit     paging limit
     * @param offset    paging offset
     * @return  The list of all relationships currently in the system
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findAll(Context context, Integer limit, Integer offset) throws SQLException;

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
     * This method creates a relationship object in the database equal to the given relationship param
     * if this is a valid relationship
     * @param context               The relevant DSpace context
     * @param relationship          The relationship that will be created in the database if it is valid
     * @param forceBypassValidation A boolean indicating whether we should force by-pass validation
     * @return                      The created relationship with updated place variables
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong with authorizations
     */
    public Relationship create(Context context, Relationship relationship, boolean forceBypassValidation)
            throws SQLException, AuthorizeException;

    /**
     * This method returns the next leftplace integer to use for a relationship with this item as the leftItem
     *
     * @param context   The relevant DSpace context
     * @param item      The item that has to be the leftItem of a relationship for it to qualify
     * @return          The next integer to be used for the leftplace of a relationship with the given item
     *                  as a left item
     * @throws SQLException If something goes wrong
     */
    int findNextLeftPlaceByLeftItem(Context context, Item item) throws SQLException;

    /**
     * This method returns the next rightplace integer to use for a relationship with this item as the rightItem
     *
     * @param context   The relevant DSpace context
     * @param item      The item that has to be the rightitem of a relationship for it to qualify
     * @return          The next integer to be used for the rightplace of a relationship with the given item
     *                  as a right item
     * @throws SQLException If something goes wrong
     */
    int findNextRightPlaceByRightItem(Context context, Item item) throws SQLException;

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
     * This method returns a list of Relationships for which the leftItem or rightItem is equal to the given
     * Item object and for which the RelationshipType object is equal to the relationshipType property
     * @param context           The relevant DSpace context
     * @param item              The Item object to be matched on the leftItem or rightItem for the relationship
     * @param relationshipType  The RelationshipType object that will be used to check the Relationship on
     * @param isLeft             Is the item left or right
     * @return  The list of Relationship objects that have the given Item object as leftItem or rightItem and
     *          for which the relationshipType property is equal to the given RelationshipType
     * @throws SQLException If something goes wrong
     */
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, boolean isLeft)
            throws SQLException;
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
                                                            RelationshipType relationshipType, int limit, int offset)
            throws SQLException;

    /**
     * This method returns a list of Relationships for which the leftItem or rightItem is equal to the given
     * Item object and for which the RelationshipType object is equal to the relationshipType property
     * @param context           The relevant DSpace context
     * @param item              The Item object to be matched on the leftItem or rightItem for the relationship
     * @param relationshipType  The RelationshipType object that will be used to check the Relationship on
     * @param isLeft             Is the item left or right
     * @return  The list of Relationship objects that have the given Item object as leftItem or rightItem and
     *          for which the relationshipType property is equal to the given RelationshipType
     * @throws SQLException If something goes wrong
     */
    public List<Relationship> findByItemAndRelationshipType(Context context, Item item,
                                                            RelationshipType relationshipType, boolean isLeft,
                                                            int limit, int offset)
            throws SQLException;

    /**
     * This method will update the place for the Relationship and all other relationships found by the items and
     * relationship type of the given Relationship. It will give this Relationship the last place in both the
     * left and right place determined by querying for the list of leftRelationships and rightRelationships
     * by the leftItem, rightItem and relationshipType of the given Relationship.
     * @param context           The relevant DSpace context
     * @param relationship      The Relationship object that will have it's place updated and that will be used
     *                          to retrieve the other relationships whose place might need to be updated
     * @throws SQLException     If something goes wrong
     */
    public void updatePlaceInRelationship(Context context, Relationship relationship)
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
     * This method returns a list of Relationship objets for which the relationshipType property is equal to the given
     * RelationshipType object
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType object that will be used to check the Relationship on
     * @param limit             paging limit
     * @param offset            paging offset
     * @return  The list of Relationship objects for which the given RelationshipType object is equal
     *          to the relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType, Integer limit,
                                              Integer offset) throws SQLException;

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

    /**
     * This method is used to construct a Relationship object by-passing validation
     * @param c                     The relevant DSpace context
     * @param leftItem              The leftItem Item object for the relationship
     * @param rightItem             The rightItem Item object for the relationship
     * @param relationshipType      The RelationshipType object for the relationship
     * @param forceBypassValidation A boolean indicating whether we should force by-pass validation
     * @return                      The created Relationship object with the given properties
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    Relationship create(Context c, Item leftItem, Item rightItem, RelationshipType relationshipType,
            boolean forceBypassValidation) throws AuthorizeException, SQLException;

    /**
     * This method returns a list of Relationship objects for the given typeName
     * @param context           The relevant DSpace context
     * @param typeName          The leftward or rightward typeName of the relationship type
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByTypeName(Context context, String typeName) throws SQLException;

    /**
     * This method returns a list of Relationship objects for the given typeName
     * @param context           The relevant DSpace context
     * @param typeName          The leftward or rightward typeName of the relationship type
     * @param limit             paging limit
     * @param offset            paging offset
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByTypeName(Context context, String typeName, Integer limit, Integer offset)
            throws SQLException;


    /**
     * counts all relationships
     *
     * @param context DSpace context object
     * @return total relationships
     * @throws SQLException if database error
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Count total number of relationships (rows in relationship table) by a relationship type
     *
     * @param context context
     * @param relationshipType relationship type to filter by
     * @return total count
     * @throws SQLException if database error
     */
    int countByRelationshipType(Context context, RelationshipType relationshipType) throws SQLException;

    /**
     * This method returns a count of Relationship objects that have the given Item object
     * as a leftItem or a rightItem
     * @param context   The relevant DSpace context
     * @param item      The item that should be either a leftItem or a rightItem of all
     *                  the Relationship objects in the returned list
     * @return          The list of Relationship objects that contain either a left or a
     *                  right item that is equal to the given item
     * @throws SQLException If something goes wrong
     */
    int countByItem(Context context, Item item) throws SQLException;

    /**
     * Count total number of relationships (rows in relationship table) by a relationship type
     *
     * @param context context
     * @param relationshipType relationship type to filter by
     * @return total count
     * @throws SQLException if database error
     */
    int countByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType)
            throws SQLException;

    /**
     * Count total number of relationships (rows in relationship table)
     * by a relationship leftward or rightward typeName
     *
     * @param context context
     * @param typeName typeName of relationship
     * @return total count
     * @throws SQLException if database error
     */
    int countByTypeName(Context context, String typeName)
            throws SQLException;

    /**
     * This method is used to delete a Relationship whilst given the possibility to copy the Virtual Metadata created
     * by this relationship to the left and/or right item
     * @param context               The relevant DSpace context
     * @param relationship          The relationship to be deleted
     * @param forceBypassValidation A boolean indicating whether we should force by-pass validation
     */
    void delete(Context context, Relationship relationship, boolean bypassValidation)
            throws SQLException, AuthorizeException;

    /**
     * This method is used to delete a Relationship whilst given the possibility to copy the Virtual Metadata created
     * by this relationship to the left and/or right item
     * @param context           The relevant DSpace context
     * @param relationship      The relationship to be deleted
     * @param copyToLeftItem    A boolean indicating whether we should copy metadata to the left item or not
     * @param copyToRightItem   A boolean indicating whether we should copy metadata to the right item or not
     */
    void delete(Context context, Relationship relationship, boolean copyToLeftItem, boolean copyToRightItem)
        throws SQLException, AuthorizeException;

    /**
     * This method is used to delete a Relationship whilst given the possibility to copy the Virtual Metadata created
     * by this relationship to the left and/or right item
     * @param context               The relevant DSpace context
     * @param relationship          The relationship to be deleted
     * @param copyToLeftItem        A boolean indicating whether we should copy metadata to the left item or not
     * @param copyToRightItem       A boolean indicating whether we should copy metadata to the right item or not
     * @param forceBypassValidation A boolean indicating whether we should force by-pass validation
     */
    void delete(Context context, Relationship relationship, boolean copyToLeftItem, boolean copyToRightItem,
            boolean bypassValidation) throws SQLException, AuthorizeException;

}