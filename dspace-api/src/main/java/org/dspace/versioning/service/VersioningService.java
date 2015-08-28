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
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

import java.sql.SQLException;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface VersioningService {

    Version createNewVersion(Context c, Item itemId);

    Version createNewVersion(Context c, Item itemId, String summary);

    void removeVersion(Context c, Version version) throws SQLException;

    void removeVersion(Context c, Item item) throws SQLException;

    Version getVersion(Context c, int versionID) throws SQLException;

    Version restoreVersion(Context c, Version version);

    Version restoreVersion(Context c, Version version, String summary);

    VersionHistory findVersionHistory(Context c, Item itemId) throws SQLException;

    Version updateVersion(Context c, Item itemId, String summary) throws SQLException;

    Version getVersion(Context c, Item item) throws SQLException;

    Version createNewVersion(Context context, VersionHistory history, Item item, String summary, Date date, int versionNumber);
}
