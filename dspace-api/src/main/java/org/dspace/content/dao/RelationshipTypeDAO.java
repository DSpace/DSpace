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

import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object Interface class for the RelationshipType object
 * The implementation of this class is responsible for all
 * database calls for the RelationshipType object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 */
public interface RelationshipTypeDAO extends GenericDAO<RelationshipType> {

    /**
     * This method is used to retrieve the RelationshipType object that has the same
     * leftType, rightType, leftLabel and rightLabel as given in the parameters
     * @param context       The relevant DSpace context
     * @param leftType      The leftType EntityType object to be matched in the query
     * @param rightType     The rightType EntityType object to be matched in the query
     * @param leftLabel     The leftLabel String to be matched in the query
     * @param rightLabel    The rightLabel String to be matched in the query
     * @return              The RelationshipType object that matches all the given parameters
     * @throws SQLException If something goes wrong
     */
    RelationshipType findByTypesAndLabels(Context context,
                                          EntityType leftType,EntityType rightType,String leftLabel,String rightLabel)
                                                throws SQLException;

    /**
     * This method will return a list of RelationshipType objects for which the given label is equal to
     * either the leftLabel or rightLabel.
     * @param context   The relevant DSpace context
     * @param label     The label that will be used to check on
     * @return A list of RelationshipType objects that have the given label as either the leftLabel or rightLabel
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByLeftOrRightLabel(Context context, String label) throws SQLException;

    /**
     * This method will return a list of RelationshipType objects for which the given EntityType object is equal
     * to the leftType or rightType
     * @param context       The relevant DSpace context
     * @param entityType    The EntityType object that will be used to check on
     * @return  The list of RelationshipType objects that have the given EntityType object
     *          as either a leftType or rightType
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByEntityType(Context context, EntityType entityType) throws SQLException;
}
