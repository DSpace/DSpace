/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCValue;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.WorkflowItem;

/**
 * Item level versioning feature utility method
 * 
 * @author Luigi Andrea Pascarelli
 * 
 */
public class VersionUtil
{

    /**
     * Create a new version of the specified item, otherwise return null
     * 
     * @param context
     *            The DSpace context
     * @param itemID
     *            The id of the to-be-versioned item
     * @param summary
     *            The motif of the versioning
     * @return Integer
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static Integer processCreateNewVersion(Context context, int itemID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {

        try
        {

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.authorizeActionBoolean(context, item,
                    Constants.WRITE) || item.canEdit())
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
     * @param summary
     *            The motif of the versioning
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static void processUpdateVersion(Context context, int itemID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {

        try
        {

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.authorizeActionBoolean(context, item,
                    Constants.WRITE))
            {
                VersioningService versioningService = new DSpace()
                        .getSingletonService(VersioningService.class);
                versioningService.updateVersion(context, itemID, summary);

                context.commit();
            }
        }
        catch (Exception ex)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
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
     * @param summary
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
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
            if (context != null && context.isValid())
            {
                context.abort();
            }
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
     * @param itemId
     * 
     * @return latest version item id or null if all versions has been removed
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
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
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw new RuntimeException(ex);
        }

    }

    /**
     * Check if the item is the last version builded
     * 
     * @param context
     * @param item
     * @return true or false
     */
    public static boolean isLatest(Context context, Item item)
    {
        VersionHistory history = retrieveVersionHistory(context, item);
        return (history == null || history.getLatestVersion().getItem().getID() == item
                .getID());
    }

    /**
     * Check if the item have a version history
     * 
     * @param context
     * @param item
     * @return true or false
     */
    public static boolean hasVersionHistory(Context context, Item item)
    {
        VersionHistory history = retrieveVersionHistory(context, item);
        return (history != null);
    }

    /**
     * Return the latest version, if there isn't or the user not have permission
     * then return null.
     * 
     * @param context
     * @param item
     * @return the latest version of the item
     * @throws SQLException
     */
    public static Version checkLatestVersion(Context context, Item item)
            throws SQLException
    {

        VersionHistory history = retrieveVersionHistory(context, item);

        if (history != null)
        {
            List<Version> allVersions = history.getVersions();
            for (Version version : allVersions)
            {
                if (version.getItem().isArchived()
                        || AuthorizeManager.isAdmin(context,
                                item.getOwningCollection()))
                {
                    // We have a newer version
                    return version;
                }
            }
        }

        return null;
    }

    /**
     * Retrieve the version history of the item
     * 
     * @param context
     * @param item
     * @return history
     */
    public static VersionHistory retrieveVersionHistory(Context context,
            Item item)
    {
        VersioningService versioningService = new DSpace()
                .getSingletonService(VersioningService.class);
        return versioningService.findVersionHistory(context, item.getID());
    }

    /**
     * Check item if it is in workspace or workflow
     * 
     * @param context
     * @param item
     * @return true if item is in workflow or workspace
     * @throws SQLException
     */
    public static boolean isItemInSubmission(Context context, Item item)
            throws SQLException
    {
        WorkspaceItem workspaceItem = WorkspaceItem.findByItem(context, item);
        InProgressSubmission workflowItem = WorkflowItem.findByItem(context,
                item);

        return workspaceItem != null || workflowItem != null;
    }

    /**
     * Retrieve an array of string where in first position there is the path
     * builded from the dc.identifier (e.g. //authority/path where path is
     * /handle/123456789/1), in second position founded the value of
     * dc.identifier
     * 
     * @param item
     * @param version
     * @return array of string
     */
    public static String[] addItemIdentifier(Item item, Version version)
    {
        String[] result = null;
        String itemHandle = version.getItem().getHandle();

        DCValue[] identifiers = version.getItem().getMetadata(
                MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
        String itemIdentifier = null;
        if (identifiers != null && identifiers.length > 0)
        {
            itemIdentifier = identifiers[0].value;
        }

        if (itemIdentifier != null)
        {
            result = new String[] { "/resource/" + itemIdentifier,
                    itemIdentifier };
        }
        else
        {
            result = new String[] { "/handle/" + itemHandle, itemHandle };
        }
        return result;
    }

    /**
     * Retrieve the summary for the version
     * 
     * @param context
     * @param stringVersionID
     * @return
     */
    public static String getSummary(Context context, String stringVersionID)
    {
        String result = "";

        try
        {
            Integer versionID = Integer.parseInt(stringVersionID);
            VersioningService versioningService = new DSpace()
                    .getSingletonService(VersioningService.class);
            Version version = versioningService.getVersion(context, versionID);
            if (version != null)
            {
                result = version.getSummary();
            }

        }
        catch (Exception ex)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw new RuntimeException(ex);
        }

        return result;
    }
}
