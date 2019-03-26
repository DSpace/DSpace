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
    RelationshipType create(Context context,RelationshipType relationshipType) throws SQLException, AuthorizeException;

    /**
     * Retrieves a RelationshipType for which the given parameters all match the one in the returned RelationshipType
     * @param context       The relevant DSpace context
     * @param leftType      The rightType EntityType that needs to match for the returned RelationshipType
     * @param rightType     The rightType EntityType that needs to match for the returned RelationshipType
     * @param leftLabel     The leftLabel String that needs to match for the returned RelationshipType
     * @param rightLabel    The rightLabel String that needs to match for the returned RelationshipType
     * @return
     * @throws SQLException If something goes wrong
     */
    RelationshipType findbyTypesAndLabels(Context context,EntityType leftType,EntityType rightType,
                                          String leftLabel,String rightLabel)
                                            throws SQLException;

    /**
     * Retrieves all RelationshipType objects currently in the system
     * @param context   The relevant DSpace context
     * @return          The list of all RelationshipType objects currently in the system
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findAll(Context context) throws SQLException;

    /**
     * Retrieves all RelationshipType objects that have a left or right label that is
     * equal to the given String
     * @param context   The relevant DSpace context
     * @param label     The label that has to match
     * @return          The list of all RelationshipType objects that have a left or right label
     *                  that is equal to the given label param
     * @throws SQLException If something goes wrong
     */
    List<RelationshipType> findByLeftOrRightLabel(Context context, String label) throws SQLException;

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

    /**
     * This method will support the creation of a RelationshipType object with the given parameters
     * @param context                       The relevant DSpace context
     * @param leftEntityType                The leftEntityType EntityType object for this relationshipType
     * @param rightEntityType               The rightEntityType EntityType object for this relationshipType
     * @param leftLabel                     The leftLabel String object for this relationshipType
     * @param rightLabel                    The rightLabel String object for this relationshipType
     * @param leftCardinalityMinInteger     The leftCardinalityMinInteger Integer object for this relationshipType
     * @param leftCardinalityMaxInteger     The leftCardinalityMaxInteger Integer object for this relationshipType
     * @param rightCardinalityMinInteger    The rightCardinalityMinInteger Integer object for this relationshipType
     * @param rightCardinalityMaxInteger    The rightCardinalityMaxInteger Integer object for this relationshipType
     * @return                              The created RelationshipType object for these properties
     * @throws SQLException                 If something goes wrong
     * @throws AuthorizeException           If something goes wrong
     */
    RelationshipType create(Context context, EntityType leftEntityType, EntityType rightEntityType, String leftLabel,
                            String rightLabel, Integer leftCardinalityMinInteger, Integer leftCardinalityMaxInteger,
                            Integer rightCardinalityMinInteger, Integer rightCardinalityMaxInteger)
        throws SQLException, AuthorizeException;
}
