/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.service;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface VersionHistoryService extends DSpaceCRUDService<VersionHistory> {

    public Version getLatestVersion(VersionHistory versionHistory);

    public Version getFirstVersion(VersionHistory versionHistory);

    public Version getPrevious(VersionHistory versionHistory, Version version);

    public Version getNext(VersionHistory versionHistory, Version version);

    public boolean hasNext(VersionHistory versionHistory, Version version);

    public void add(VersionHistory versionHistory, Version version);

    public Version getVersion(VersionHistory versionHistory, Item item);

    public boolean hasNext(VersionHistory versionHistory, Item item);

    public boolean isFirstVersion(VersionHistory versionHistory, Version version);

    public boolean isLastVersion(VersionHistory versionHistory, Version version);

    public void remove(VersionHistory versionHistory, Version version);

    public VersionHistory findByItem(Context context, Item item) throws SQLException;

}
