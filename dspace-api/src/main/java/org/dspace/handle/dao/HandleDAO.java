/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.handle.Handle;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the Handle object.
 * The implementation of this class is responsible for all database calls for the Handle object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HandleDAO extends GenericDAO<Handle> {

    public Long getNextHandleSuffix(Context context) throws SQLException;

    public List<Handle> getHandlesByDSpaceObject(Context context, DSpaceObject dso) throws SQLException;

    public Handle findByHandle(Context context, String handle)throws SQLException;

    public List<Handle> findByPrefix(Context context, String prefix) throws SQLException;

    public long countHandlesByPrefix(Context context, String prefix) throws SQLException;

    int updateHandlesWithNewPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException;

    int countRows(Context context) throws SQLException;
}
