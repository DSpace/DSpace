/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

/**
 * Database Access Object interface class for the Version object.
 * The implementation of this class is responsible for all database calls for the Version object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface VersionDAO extends GenericDAO<Version> {
    public Version findByItem(Context context, Item item) throws SQLException;

    /**
     * This method returns all versions of an version history that have items
     * assigned. We do not delete versions to keep version numbers stable. To
     * remove a version we set the item, date, summary and eperson null. This
     * method returns only versions that aren't soft deleted and have items
     * assigned.
     *
     * @param context        The relevant DSpace Context.
     * @param versionHistory version history
     * @param offset         the position of the first result to return
     * @param limit          paging limit
     * @return all versions of an version history that have items assigned.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Version> findVersionsWithItems(Context context, VersionHistory versionHistory, int offset, int limit)
        throws SQLException;

    public int getNextVersionNumber(Context c, VersionHistory vh) throws SQLException;

    /**
     * This method count versions of an version history that have items
     * assigned. We do not delete versions to keep version numbers stable. To
     * remove a version we set the item, date, summary and eperson null. This
     * method returns only versions that aren't soft deleted and have items
     * assigned.
     * 
     * @param context          The relevant DSpace Context.
     * @param versionHistory   Version history
     * @return                 Total versions of an version history that have items assigned.
     * @throws SQLException    If database error
     */
    public int countVersionsByHistoryWithItem(Context context, VersionHistory versionHistory) throws SQLException;

}
