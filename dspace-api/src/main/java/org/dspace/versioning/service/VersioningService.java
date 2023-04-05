/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface VersioningService {

    Version createNewVersion(Context c, Item itemId);

    Version createNewVersion(Context c, Item itemId, String summary);

    /**
     * Returns all versions of a version history.
     * To keep version numbers stable we do not delete versions, we do only set
     * the item, date, summary and eperson null. This methods returns only those
     * versions that have an item assigned.
     *
     * @param c  The relevant DSpace Context.
     * @param vh version history
     * @return All versions of a version history that have an item assigned.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<Version> getVersionsByHistory(Context c, VersionHistory vh) throws SQLException;

    /**
     * Return a paginated list of versions of a version history.
     * To keep version numbers stable we do not delete versions, we do only set
     * the item, date, summary and eperson null. This methods returns only those
     * versions that have an item assigned.
     * 
     * @param c                   The relevant DSpace Context.
     * @param vh                  Version history
     * @param offset              The position of the first result to return
     * @param limit               Paging limit
     * @throws SQLException       If database error
     */
    List<Version> getVersionsByHistoryWithItems(Context c, VersionHistory vh, int offset, int limit)
         throws SQLException;

    /**
     * Delete a Version
     *
     * @param context        context
     * @param version        version
     * @throws SQLException  if database error
     */
    public void delete(Context context, Version version) throws SQLException;

    void removeVersion(Context c, Item item) throws SQLException;

    Version getVersion(Context c, int versionID) throws SQLException;

    Version restoreVersion(Context c, Version version);

    Version restoreVersion(Context c, Version version, String summary);

    Version updateVersion(Context c, Item itemId, String summary) throws SQLException;

    Version getVersion(Context c, Item item) throws SQLException;

    Version createNewVersion(Context context, VersionHistory history, Item item, String summary, Date date,
                             int versionNumber);

    /**
     * Update the Version
     *
     * @param context        context
     * @param version        version
     * @throws SQLException  if database error
     */
    public void update(Context context, Version version) throws SQLException;

    /**
     * This method count versions of an version history that have items
     * assigned. We do not delete versions to keep version numbers stable. To
     * remove a version we set the item, date, summary and eperson null. This
     * method returns only versions that aren't soft deleted and have items
     * assigned.
     * 
     * @param context            The relevant DSpace Context.
     * @param versionHistory     Version history
     * @return                   Total versions of an version history that have items assigned.
     * @throws SQLException      If database error
     */
    public int countVersionsByHistoryWithItem(Context context, VersionHistory versionHistory) throws SQLException;

}
