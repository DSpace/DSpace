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
    RelationshipType findbyTypesAndLabels(Context context,
                                          EntityType leftType,EntityType rightType,String leftLabel,String rightLabel)
                                                throws SQLException;
    List<RelationshipType> findByLeftOrRightLabel(Context context, String label) throws SQLException;

}
