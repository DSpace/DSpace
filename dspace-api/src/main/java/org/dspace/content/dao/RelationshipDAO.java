/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object Interface class for the Relationship object
 * The implementation of this class is responsible for all
 * database calls for the Relationship object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 */
public interface RelationshipDAO extends GenericDAO<Relationship> {

    /**
     * This method returns a list of Relationship objects that have the given Item object
     * as a leftItem or a rightItem
     * @param context   The relevant DSpace context
     * @param item      The item that should be either a leftItem or a rightItem of all
     *                  the Relationship objects in the returned list
     * @return          The list of Relationship objects that contain either a left or a
     *                  right item that is equal to the given item
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByItem(Context context, Item item) throws SQLException;

    /**
     * This method returns a list of Relationship objects that have the given Item object
     * as a leftItem or a rightItem
     * @param context   The relevant DSpace context
     * @param item      The item that should be either a leftItem or a rightItem of all
     *                  the Relationship objects in the returned list
     * @param limit     paging limit
     * @param offset    paging offset
     * @return          The list of Relationship objects that contain either a left or a
     *                  right item that is equal to the given item
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByItem(Context context, Item item, Integer limit, Integer offset) throws SQLException;

    /**
     * This method returns the highest leftplace integer for all the relationships where this
     * item is the leftitem so that we can set a proper leftplace attribute on the next relationship
     * @param context   The relevant DSpace context
     * @param item      The item to be matched on leftItem
     * @return          The integer for the highest leftPlace value for all the relatonship objects
     *                  that have the given item as leftItem
     * @throws SQLException If something goes wrong
     */
    int findLeftPlaceByLeftItem(Context context, Item item) throws SQLException;

    /**
     * This method returns the highest rightplace integer for all the relationships where this
     * item is the rightitem so that we can set a proper rightplace attribute on the next relationship
     * @param context   The relevant DSpace context
     * @param item      The item to be matched on rightItem
     * @return          The integer for the highest rightPlace value for all the relatonship objects
     *                  that have the given item as rightItem
     * @throws SQLException If something goes wrong
     */
    int findRightPlaceByRightItem(Context context, Item item) throws SQLException;

    /**
     * This method returns a list of Relationship objects for the given RelationshipType object.
     * It will construct a list of all Relationship objects that have the given RelationshipType object
     * as the relationshipType property
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType object to be checked on
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType) throws SQLException;

    /**
     * This method returns a list of Relationship objects for the given RelationshipType object.
     * It will construct a list of all Relationship objects that have the given RelationshipType object
     * as the relationshipType property
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType object to be checked on
     * @param limit             paging limit
     * @param offset            paging offset
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByRelationshipType(Context context, RelationshipType relationshipType,
                                              Integer limit, Integer offset) throws SQLException;

    /**
     * This method returns a list of Relationship objects for the given RelationshipType object.
     * It will construct a list of all Relationship objects that have the given RelationshipType object
     * as the relationshipType property
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType object to be checked on
     * @param limit             paging limit
     * @param offset            paging offset
     * @param item              item to filter by
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType,
                                              Integer limit, Integer offset) throws SQLException;

    /**
     * This method returns a list of Relationship objects for the given RelationshipType object.
     * It will construct a list of all Relationship objects that have the given RelationshipType object
     * as the relationshipType property
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType object to be checked on
     * @param limit             paging limit
     * @param offset            paging offset
     * @param item              item to filter by
     * @param isLeft            Is item left or right
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType,
                                                     boolean isLeft, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Count total number of relationships (rows in relationship table)
     *
     * @param context context
     * @return total count
     * @throws SQLException if database error
     */
    int countRows(Context context) throws SQLException;

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
     * Count total number of relationships (rows in relationship table) by an item and a relationship type
     *
     * @param context context
     * @param relationshipType relationship type to filter by
     * @param item item to filter by
     * @return total count
     * @throws SQLException if database error
     */
    int countByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType)
            throws SQLException;
}
