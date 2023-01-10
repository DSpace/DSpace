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

import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the EtdUnit object.
 * The implementation of this class is responsible for all database calls for
 * the EtdUnit object and is autowired by spring
 * This class should only be accessed from a single service and should never be
 * exposed outside of the API
 *
 * @author mohideen at umd.edu
 */
public interface EtdUnitDAO extends DSpaceObjectLegacySupportDAO<EtdUnit> {

    public EtdUnit findByName(Context context, String name) throws SQLException;

    /**
     * Find units matching the given name (fuzzy match)
     *
     * @param context  The DSpace context
     * @param unitName Part of the unit's name to search for
     * @param offset   Offset to use for pagination (-1 to disable)
     * @param limit    The maximum number of results to return (-1 to disable)
     * @return A List of Units matching the query
     * @throws SQLException if database error
     */
    List<EtdUnit> findByNameLike(Context context, String unitName, int offset, int limit) throws SQLException;

    /**
     * Count the number of units that have a name that contains the given string
     *
     * @param context  The DSpace context
     * @param unitName Part of the unit's name to search for
     * @return The number of matching units
     * @throws SQLException if database error
     */
    int countByNameLike(Context context, String unitName) throws SQLException;

    public List<EtdUnit> findAll(Context context, int pageSize, int offset) throws SQLException;

    public List<EtdUnit> findByCollection(Context context, Collection collection) throws SQLException;

    int countRows(Context context) throws SQLException;
}
