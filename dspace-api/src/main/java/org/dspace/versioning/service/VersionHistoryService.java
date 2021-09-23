/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
public interface VersionHistoryService extends DSpaceCRUDService<VersionHistory> {

    public void add(Context context, VersionHistory versionHistory, Version version)
        throws SQLException;

    public VersionHistory findByItem(Context context, Item item)
        throws SQLException;

    public Version getFirstVersion(Context context, VersionHistory versionHistory)
        throws SQLException;

    public Version getLatestVersion(Context context, VersionHistory versionHistory)
        throws SQLException;

    public Version getNext(Context context, VersionHistory versionHistory, Version version)
        throws SQLException;

    public Version getPrevious(Context context, VersionHistory versionHistory, Version version)
        throws SQLException;

    public Version getVersion(Context context, VersionHistory versionHistory, Item item)
        throws SQLException;

    public boolean hasNext(Context context, VersionHistory versionHistory, Item item)
        throws SQLException;

    public boolean hasNext(Context context, VersionHistory versionHistory, Version version)
        throws SQLException;

    public boolean hasVersionHistory(Context context, Item item)
        throws SQLException;

    public boolean isFirstVersion(Context context, Item item)
        throws SQLException;

    public boolean isFirstVersion(Context context, VersionHistory versionHistory, Version version)
        throws SQLException;

    public boolean isLastVersion(Context context, Item item)
        throws SQLException;

    public boolean isLastVersion(Context context, VersionHistory versionHistory, Version version)
        throws SQLException;

    public void remove(VersionHistory versionHistory, Version version);

    /**
     * This method has a scope to verify if the logged user has permission
     * to see the attribute 'draftVersion' of the latest version.
     * 
     * @param context             DSpace context object
     * @param versionHistory      Version history object
     * @return                    return true if the logged user has permission to see
     *                            the attribute 'draftVersion' of the latest version, otherwise false
     * @throws SQLException       If database error
     */
    public boolean canSeeDraftVersion(Context context, VersionHistory versionHistory) throws SQLException;

}
