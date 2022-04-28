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
import java.util.UUID;

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
     * @param context         The relevant DSpace context
     * @param item            The item that should be either a leftItem or a rightItem of all
     *                        the Relationship objects in the returned list
     * @param excludeTilted   If true, excludes tilted relationships
     * @return                The list of Relationship objects that contain either a left or a
     *                        right item that is equal to the given item
     * @throws SQLException   If something goes wrong
     */
    List<Relationship> findByItem(Context context, Item item, boolean excludeTilted) throws SQLException;

    /**
     * This method returns a list of Relationship objects that have the given Item object
     * as a leftItem or a rightItem
     * @param context         The relevant DSpace context
     * @param item            The item that should be either a leftItem or a rightItem of all
     *                        the Relationship objects in the returned list
     * @param limit           paging limit
     * @param offset          paging offset
     * @param excludeTilted   If true, excludes tilted relationships
     * @return                The list of Relationship objects that contain either a left or a
     *                        right item that is equal to the given item
     * @throws SQLException   If something goes wrong
     */
    List<Relationship> findByItem(Context context, Item item, Integer limit, Integer offset, boolean excludeTilted)
            throws SQLException;

    /**
     * This method returns the next leftplace integer to use for a relationship with this item as the leftItem
     *
     * @param context   The relevant DSpace context
     * @param item      The item to be matched on leftItem
     * @return          The next integer to be used for the leftplace of a relationship with the given item
     *                  as a left item
     * @throws SQLException If something goes wrong
     */
    int findNextLeftPlaceByLeftItem(Context context, Item item) throws SQLException;

    /**
     * This method returns the next rightplace integer to use for a relationship with this item as the rightItem
     *
     * @param context   The relevant DSpace context
     * @param item      The item to be matched on rightItem
     * @return          The next integer to be used for the rightplace of a relationship with the given item
     *                  as a right item
     * @throws SQLException If something goes wrong
     */
    int findNextRightPlaceByRightItem(Context context, Item item) throws SQLException;

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
     * This method returns a list of Relationship objects for the given typeName
     * @param context           The relevant DSpace context
     * @param typeName          The leftward or rightward typeName of the relationship type
     * @return  A list of Relationship objects that have the given RelationshipType object as the
     *          relationshipType property
     * @throws SQLException If something goes wrong
     */
    List<Relationship> findByTypeName(Context context, String typeName)
            throws SQLException;

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
     * Count total number of relationships (rows in relationship table) by an item and a relationship type and a boolean
     * indicating whether the item should be the leftItem or the rightItem
     *
     * @param context context
     * @param relationshipType relationship type to filter by
     * @param item item to filter by
     * @param isLeft Indicating whether the counted Relationships should have the given Item on the left side or not
     * @return total count
     * @throws SQLException if database error
     */
    int countByItemAndRelationshipType(Context context, Item item, RelationshipType relationshipType, boolean isLeft)
            throws SQLException;

    /**
     * Count total number of relationships (rows in relationship table) given a typeName
     *
     * @param context context
     * @param typeName the relationship typeName to filter by
     * @return total count
     * @throws SQLException if database error
     */
    int countByTypeName(Context context, String typeName)
            throws SQLException;

    /**
     * This method is used to retrieve relationships that match focusItem
     * on the one hand and matches list of related items elsewhere.
     *
     * @param context            DSpace context object
     * @param focusUUID          UUID of Item that will match left side if the param isLeft is true otherwise right side
     * @param relationshipType   Relationship type to filter by
     * @param items              List of UUID that will use to filter other side respect the focusUUID
     * @param isLeft             Indicating whether the counted Relationships should have
     *                           the given Item on the left side or not
     * @param limit              paging limit
     * @param offset             paging offset
     * @return
     * @throws SQLException      If database error
     */
    List<Relationship> findByItemAndRelationshipTypeAndList(Context context, UUID focusUUID,
            RelationshipType relationshipType, List<UUID> items, boolean isLeft,
            int offset, int limit) throws SQLException;

    /**
     * Count total number of relationships that match focusItem
     * on the one hand and matches list of related items elsewhere.
     *
     * @param context            DSpace context object
     * @param focusUUID          UUID of Item that will match left side if the param isLeft is true otherwise right side
     * @param relationshipType   Relationship type to filter by
     * @param items              List of UUID that will use to filter other side respect the focusUUID
     * @param isLeft             Indicating whether the counted Relationships should have
     *                           the given Item on the left side or not
     * @return
     * @throws SQLException      If database error
     */
    int countByItemAndRelationshipTypeAndList(Context context, UUID focusUUID, RelationshipType relationshipType,
                                               List<UUID> items, boolean isLeft) throws SQLException;
}
