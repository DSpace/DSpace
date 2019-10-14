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
     * leftType, rightType, leftwardType and rightwardType as given in the parameters
     * @param context           The relevant DSpace context
     * @param leftType          The leftType EntityType object to be matched in the query
     * @param rightType         The rightType EntityType object to be matched in the query
     * @param leftwardType     The leftwardType String to be matched in the query
     * @param rightwardType    The rightwardType String to be matched in the query
     * @return                  The RelationshipType object that matches all the given parameters
     * @throws SQLException If something goes wrong
     */
    RelationshipType findbyTypesAndTypeName(Context context, EntityType leftType,EntityType rightType,
                                                                              String leftwardType,
                                                                              String rightwardType)
                                                                                    throws SQLException;

    /**
     * This method will return a list of RelationshipType objects for which the given label is equal to
     * either the leftwardType or rightwardType.
     * @param context   The relevant DSpace context
     * @param type     The label that will be used to check on
     * @return A list of RelationshipType objects that have the given label as either the leftwardType
     *         or rightwardType
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String type) throws SQLException;

    /**
     * This method will return a list of RelationshipType objects for which the given label is equal to
     * either the leftLabel or rightLabel.
     * @param context   The relevant DSpace context
     * @param type     The label that will be used to check on
     * @param limit     paging limit
     * @param offset    paging offset
     * @return A list of RelationshipType objects that have the given label as either the leftLabel or rightLabel
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String type, Integer limit,
                                                             Integer offset)
            throws SQLException;

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

    /**
     * This method will return a list of RelationshipType objects for which the given EntityType object is equal
     * to the leftType or rightType
     * @param context       The relevant DSpace context
     * @param entityType    The EntityType object that will be used to check on
     * @param limit         paging limit
     * @param offset        paging offset
     * @return  The list of RelationshipType objects that have the given EntityType object
     *          as either a leftType or rightType
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByEntityType(Context context, EntityType entityType, Integer limit, Integer offset)
            throws SQLException;

    /**
     * This method will return a list of RelationshipType objects for which the given EntityType object is equal
     * to the leftType or rightType
     * @param context       The relevant DSpace context
     * @param entityType    The EntityType object that will be used to check on
     * @param isLeft        Boolean value used to filter by left_type or right_type. If true left_type results only
     *                      else right_type results.
     * @return  The list of RelationshipType objects that have the given EntityType object
     *          as either a leftType or rightType
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByEntityType(Context context,  EntityType entityType, Boolean isLeft)
            throws SQLException;


    /**
     * This method will return a list of RelationshipType objects for which the given EntityType object is equal
     * to the leftType or rightType
     * @param context       The relevant DSpace context
     * @param entityType    The EntityType object that will be used to check on
     * @param isLeft        Boolean value used to filter by left_type or right_type. If true left_type results only
     *                      else right_type results.
     * @param limit         paging limit
     * @param offset        paging offset
     * @return  The list of RelationshipType objects that have the given EntityType object
     *          as either a leftType or rightType
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByEntityType(Context context, EntityType entityType, Boolean isLeft,
                                            Integer limit, Integer offset)
            throws SQLException;
}
