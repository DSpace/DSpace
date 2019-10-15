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
    List<Relationship> findByItem(Context context,Item item) throws SQLException;

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
}
