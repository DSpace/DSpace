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
 * The implementation of this class is responsible for all database calls for the EtdUnit object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author mohideen at umd.edu
 */
public interface EtdUnitDAO extends DSpaceObjectLegacySupportDAO<EtdUnit> {

    public EtdUnit findByName(Context context, String name) throws SQLException;

    public List<EtdUnit> findAllSortedByName(Context context) throws SQLException;

    public List<EtdUnit> findAllByCollection(Context context, Collection collection) throws SQLException;

    public List<EtdUnit> searchByName(Context context, String query, int offset, int limit) throws SQLException;

    public int searchByNameResultCount(Context context, String query) throws SQLException;

    int countRows(Context context) throws SQLException;
}
