package org.dspace.app.webui.util;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;

public class VersionUtil
{

    /**
     * Create a new version of the specified item
     * 
     * @param context
     *            The DSpace context
     * @param itemID
     *            The id of the to-be-versioned item
     * @return A result object
     */
    // Versioning
    public static Integer processCreateNewVersion(Context context, int itemID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {

        try
        {

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.isAdmin(context, item) || item.canEdit())
            {
                VersioningService versioningService = new DSpace()
                        .getSingletonService(VersioningService.class);
                Version version = versioningService.createNewVersion(context,
                        itemID, summary);
                WorkspaceItem wsi = WorkspaceItem.findByItem(context,
                        version.getItem());

                context.commit();

                return wsi.getID();

            }
        }
        catch (Exception ex)
        {
            context.abort();
            throw new RuntimeException(ex);
        }
        return null;
    }

    /**
     * Modify latest version
     * 
     * @param context
     *            The DSpace context
     * @param itemID
     *            The id of the to-be-versioned item
     * @return A result object
     */
    // Versioning
    public static void processUpdateVersion(Context context, int itemID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {

        try
        {

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.isAdmin(context, item))
            {
                VersioningService versioningService = new DSpace()
                        .getSingletonService(VersioningService.class);
                versioningService.updateVersion(context, itemID, summary);

                context.commit();
            }
        }
        catch (Exception ex)
        {
            context.abort();
            throw new RuntimeException(ex);
        }

    }

    /**
     * Restore a version
     * 
     * @param versionID
     *            id of the version to restore
     * @param context
     *            The DSpace context
     * @return A result object
     */
    // Versioning
    public static void processRestoreVersion(Context context, int versionID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {

        try
        {

            VersioningService versioningService = new DSpace()
                    .getSingletonService(VersioningService.class);
            versioningService.restoreVersion(context, versionID, summary);

            context.commit();

        }
        catch (Exception ex)
        {
            context.abort();
            throw new RuntimeException(ex);
        }

    }

    /**
     * Delete version(s)
     * 
     * @param context
     *            The DSpace context
     * @param versionIDs
     *            list of versionIDs to delete
     * @return A result object
     */
    // Versioning
    public static Integer processDeleteVersions(Context context, int itemId,
            String[] versionIDs) throws SQLException, AuthorizeException,
            IOException
    {

        try
        {

            VersioningService versioningService = new DSpace()
                    .getSingletonService(VersioningService.class);
            VersionHistory versionHistory = versioningService
                    .findVersionHistory(context, itemId);

            for (String id : versionIDs)
            {
                versioningService.removeVersion(context, Integer.parseInt(id));
            }
            context.commit();

            // Retrieve the latest version of our history (IF any is even
            // present)
            Version latestVersion = versionHistory.getLatestVersion();
            if (latestVersion == null)
            {
                return null;
            }
            else
            {
                return latestVersion.getItemID();
            }

        }
        catch (Exception ex)
        {
            context.abort();
            throw new RuntimeException(ex);
        }

    }
}
