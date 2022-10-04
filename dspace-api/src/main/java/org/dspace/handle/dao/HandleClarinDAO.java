/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.handle.Handle;

/**
 * Database Access Object interface class for the Handle object.
 * The implementation of this class is responsible for the specific database calls for the Handle object
 * and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface HandleClarinDAO {

    /**
     * Find all Handles following the sorting options
     * @param context DSpace context object
     * @param sortingColumn sorting option in the specific format e.g. `handle:123456789/111`
     * @return List of Handles
     */
    List<Handle> findAll(Context context, String sortingColumn) throws SQLException;
}
