/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;

import org.dspace.content.Bundle;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the Bundle object.
 * The implementation of this class is responsible for all database calls for the Bundle object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BundleDAO extends DSpaceObjectLegacySupportDAO<Bundle> {
    int countRows(Context context) throws SQLException;

    /**
     * Acquires a pessimistic write lock on the given bundle row and refreshes the entity from the
     * database.
     *
     * <p>The refresh ensures that any stale in-memory state (e.g. a lazy-loaded bitstream
     * collection that was initialised before a concurrent transaction committed its changes) is
     * discarded and replaced with the current database state.  The {@code PESSIMISTIC_WRITE} lock
     * prevents any other transaction from acquiring a conflicting lock on the same bundle row until
     * the current transaction commits, which serialises all add / remove / reorder operations on
     * the same bundle.</p>
     *
     * @param context the DSpace context (and thus the Hibernate session) to use
     * @param bundle  the bundle to lock
     * @throws SQLException if the database operation fails
     */
    void lockForWrite(Context context, Bundle bundle) throws SQLException;
}
