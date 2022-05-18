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
public interface UnitDAO extends DSpaceObjectLegacySupportDAO<Unit> {

    public Unit findByName(Context context, String name) throws SQLException;

    public List<Unit> findAllSortedByName(Context context) throws SQLException;

    public List<Unit> findAllByGroup(Context context, Group group) throws SQLException;

    public List<Unit> searchByName(Context context, String query, int offset, int limit) throws SQLException;

    public int searchByNameResultCount(Context context, String query) throws SQLException;

    int countRows(Context context) throws SQLException;
}
