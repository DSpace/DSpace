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
}
