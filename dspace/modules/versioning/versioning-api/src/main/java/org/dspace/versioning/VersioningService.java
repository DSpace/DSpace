package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Jun 17, 2011
 * Time: 8:24:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface VersioningService {
    Version createNewVersion(Context c, int itemId);

    Version createNewVersion(Context c, int itemId, String summary);

    VersionHistory startNewVersionHistoryAt(Context c, Item item, String summary, int versionNumber);

    void removeVersion(Context c, int versionID);

    void removeVersion(Context c, Item item);

    Version getVersion(Context c, int versionID);

    Version restoreVersion(Context c, int versionID);

    Version restoreVersion(Context c, int versionID, String summary);

    VersionHistory findVersionHistory(Context c, int itemId);

    Version updateVersion(Context c, int itemId, String summary);

    Version getVersion(Context c, Item item);
}
