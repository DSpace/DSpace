/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;
import org.dspace.content.dao.DSpaceObjectDAO;

import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the Unit object.
 * The implementation of this class is responsible for all database calls for the Unit object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author mohideen at umd.edu
 */
public interface UnitDAO extends DSpaceObjectDAO<Unit>, DSpaceObjectLegacySupportDAO<Unit> {

    /**
     * Find a unit by its name (exact match)
     *
     * @param context The DSpace context
     * @param name    The name of the unit to look for
     * @return The unit with the specified name
     * @throws SQLException if database error
     */
    Unit findByName(Context context, String name) throws SQLException;

    /**
     * Find units matching the given name (fuzzy match)
     *
     * @param context   The DSpace context
     * @param unitName  Part of the unit's name to search for
     * @param offset    Offset to use for pagination (-1 to disable)
     * @param limit     The maximum number of results to return (-1 to disable)
     * @return A List of Units matching the query
     * @throws SQLException if database error
     */
    List<Unit> findByNameLike(Context context, String unitName, int offset, int limit) throws SQLException;

    /**
     * Count the number of units that have a name that contains the given string
     *
     * @param context   The DSpace context
     * @param unitName Part of the unit's name to search for
     * @return The number of matching units
     * @throws SQLException if database error
     */
    int countByNameLike(Context context, String unitName) throws SQLException;


    /**
     * Find all units ordered by name ascending
     *
     * @param context  The DSpace context
     * @param pageSize how many results return
     * @param offset   the position of the first result to return
     * @return A list of all units, ordered by name
     * @throws SQLException if database error
     */
    List<Unit> findAll(Context context, int pageSize, int offset) throws SQLException;

    /**
     * Find all units that the given Group belongs to
     *
     * @param context The DSpace context
     * @param group The Group to match
     * @return A list of all units to which the given Group belongs
     * @throws SQLException if database error
     */
    List<Unit> findByGroup(Context context, Group group) throws SQLException;


    /**
     * Count the number of units in DSpace
     *
     * @param context The DSpace context
     * @return The number of units
     * @throws SQLException if database error
     */
    int countRows(Context context) throws SQLException;
}
