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
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;

/**
 * This Service uses DAOs to access information on the database objects for the RelationshipTypes
 */
public interface RelationshipTypeService extends DSpaceCRUDService<RelationshipType> {

    /**
     * This method creates the given RelationshipType object in the database and returns it
     * @param context           The relevant DSpace context
     * @param relationshipType  The RelationshipType to be created in the database
     * @return                  The newly created RelationshipType
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong with authorizations
     */
    RelationshipType create(Context context, RelationshipType relationshipType) throws SQLException, AuthorizeException;

    /**
     * Retrieves a RelationshipType for which the given parameters all match the one in the returned RelationshipType
     * @param context       The relevant DSpace context
     * @param leftType      The rightType EntityType that needs to match for the returned RelationshipType
     * @param rightType     The rightType EntityType that needs to match for the returned RelationshipType
     * @param leftwardType     The leftwardType String that needs to match for the returned RelationshipType
     * @param rightwardType    The rightwardType String that needs to match for the returned RelationshipType
     * @return
     * @throws SQLException If something goes wrong
     */
    RelationshipType findbyTypesAndTypeName(Context context, EntityType leftType, EntityType rightType,
                                          String leftwardType, String rightwardType)
                                            throws SQLException;

    /**
     * Retrieves all RelationshipType objects currently in the system
     * @param context   The relevant DSpace context
     * @return          The list of all RelationshipType objects currently in the system
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findAll(Context context) throws SQLException;

    /**
     * Retrieves all RelationshipType objects currently in the system
     * @param context   The relevant DSpace context
     * @param limit     paging limit
     * @param offset    paging offset
     * @return          The list of all RelationshipType objects currently in the system
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * Retrieves all RelationshipType objects that have a left or right type that is
     * equal to the given String
     * @param context   The relevant DSpace context
     * @param typeName     The label that has to match
     * @return          The list of all RelationshipType objects that have a left or right label
     *                  that is equal to the given label param
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String typeName) throws SQLException;

    /**
     * Retrieves all RelationshipType objects that have a left or right label that is
     * equal to the given String
     * @param context   The relevant DSpace context
     * @param typeName  The typeName that has to match
     * @param limit     paging limit
     * @param offset    paging offset
     * @return          The list of all RelationshipType objects that have a left or right label
     *                  that is equal to the given label param
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByLeftwardOrRightwardTypeName(Context context, String typeName, Integer limit,
                                                             Integer offset)
            throws SQLException;

    /**
     * Returns a list of RelationshipType objects for which the given EntityType is equal to either the leftType
     * or the rightType
     * @param context       The relevant DSpace context
     * @param entityType    The EntityType object used to check the leftType and rightType properties
     * @return  A list of RelationshipType objects for which the leftType or rightType property are equal to the
     *          given EntityType object
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByEntityType(Context context, EntityType entityType) throws SQLException;

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
    List<RelationshipType> findByEntityType(Context context, EntityType entityType, boolean isLeft)
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
    List<RelationshipType> findByEntityType(Context context, EntityType entityType, boolean isLeft,
                                            Integer limit, Integer offset)
            throws SQLException;

    /**
     * This method will support the creation of a RelationshipType object with the given parameters
     * @param context                       The relevant DSpace context
     * @param leftEntityType                The leftEntityType EntityType object for this relationshipType
     * @param rightEntityType               The rightEntityType EntityType object for this relationshipType
     * @param leftwardType                  The leftwardType String object for this relationshipType
     * @param rightwardType                 The rightwardType String object for this relationshipType
     * @param leftCardinalityMinInteger     The leftCardinalityMinInteger Integer object for this relationshipType
     * @param leftCardinalityMaxInteger     The leftCardinalityMaxInteger Integer object for this relationshipType
     * @param rightCardinalityMinInteger    The rightCardinalityMinInteger Integer object for this relationshipType
     * @param rightCardinalityMaxInteger    The rightCardinalityMaxInteger Integer object for this relationshipType
     * @return                              The created RelationshipType object for these properties
     * @throws SQLException                 If something goes wrong
     * @throws AuthorizeException           If something goes wrong
     */
    RelationshipType create(Context context, EntityType leftEntityType, EntityType rightEntityType,
                            String leftwardType, String rightwardType, Integer leftCardinalityMinInteger,
                            Integer leftCardinalityMaxInteger, Integer rightCardinalityMinInteger,
                            Integer rightCardinalityMaxInteger)
        throws SQLException, AuthorizeException;
    /**
     * This method will support the creation of a RelationshipType object with the given parameters
     * @param context                       The relevant DSpace context
     * @param leftEntityType                The leftEntityType EntityType object for this relationshipType
     * @param rightEntityType               The rightEntityType EntityType object for this relationshipType
     * @param leftwardType                  The leftwardType String object for this relationshipType
     * @param rightwardType                 The rightwardType String object for this relationshipType
     * @param leftCardinalityMinInteger     The leftCardinalityMinInteger Integer object for this relationshipType
     * @param leftCardinalityMaxInteger     The leftCardinalityMaxInteger Integer object for this relationshipType
     * @param rightCardinalityMinInteger    The rightCardinalityMinInteger Integer object for this relationshipType
     * @param rightCardinalityMaxInteger    The rightCardinalityMaxInteger Integer object for this relationshipType
     * @return                              The created RelationshipType object for these properties
     * @throws SQLException                 If something goes wrong
     * @throws AuthorizeException           If something goes wrong
     */
    RelationshipType create(Context context, EntityType leftEntityType, EntityType rightEntityType,
                            String leftwardType, String rightwardType, Integer leftCardinalityMinInteger,
                            Integer leftCardinalityMaxInteger, Integer rightCardinalityMinInteger,
                            Integer rightCardinalityMaxInteger, Boolean copyToLeft, Boolean copyToRight)
        throws SQLException, AuthorizeException;
}
