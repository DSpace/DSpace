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
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Item level versioning feature utility method
 * 
 * @author Luigi Andrea Pascarelli
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 * 
 */
public class VersionUtil
{
	private static boolean initialezed = false;
	
	private static ItemService itemService;
	
	private static AuthorizeService authorizeService;
	
	private static VersioningService versioningService; 
	
	private static VersionHistoryService versionHistoryService; 
	
	private static WorkspaceItemService workspaceItemService;
	
	private static WorkflowItemService workflowItemService;
        
	private synchronized static void initialize() {
		initialezed = true;
		itemService = ContentServiceFactory.getInstance().getItemService();
		authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
		versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
		versioningService = VersionServiceFactory.getInstance().getVersionService();
		workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
		workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService(); 
	}

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
    public static Integer processCreateNewVersion(Context context, UUID itemID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {
    	initialize();
        try
        {

            Item item = itemService.find(context, itemID);

            if (authorizeService.authorizeActionBoolean(context, item,
                    Constants.WRITE) || itemService.canEdit(context, item) 
                                     || itemService.canCreateNewVersion(context, item))
            {
                VersioningService versioningService = new DSpace()
                        .getSingletonService(VersioningService.class);
                Version version = versioningService.createNewVersion(context,
                        item, summary);
                WorkspaceItem wsi = workspaceItemService.findByItem(context,
                        version.getItem());

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
    public static void processUpdateVersion(Context context, UUID itemID,
            String summary) throws SQLException, AuthorizeException,
            IOException
    {
    	initialize();
        try
        {

            Item item = itemService.find(context, itemID);

            if (authorizeService.authorizeActionBoolean(context, item,
                    Constants.WRITE))
            {
                versioningService.updateVersion(context, item, summary);
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
    	initialize();
        VersioningService versioningService = new DSpace()
                .getSingletonService(VersioningService.class);
        Version version = versioningService.getVersion(context, versionID);
        versioningService.restoreVersion(context, version, summary);
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
    public static Item processDeleteVersions(Context context, UUID itemId,
            String[] versionIDs) throws SQLException, AuthorizeException,
            IOException
    {
    	initialize();
        try
        {
            Item item = itemService.find(context, itemId);
            VersionHistory versionHistory = versionHistoryService.findByItem(context, item);
            
            for (String versionID : versionIDs)
            {
            	Version version = versioningService.getVersion(context, Integer.parseInt(versionID));
                versioningService.removeVersion(context, version);
            }

            // Retrieve the latest version of our history (IF any is even
            // present)
            Version latestVersion = versionHistoryService.getLatestVersion(context, versionHistory);
            if (latestVersion == null)
            {
                return null;
            }
            else
            {
                return latestVersion.getItem();
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
    	initialize();
        VersionHistory history = versionHistoryService.findByItem(context, item);

        if (history != null)
        {
            List<Version> allVersions = versioningService.getVersionsByHistory(context, history);
            for (Version version : allVersions)
            {
                if (version.getItem().isArchived()
                        || authorizeService.isAdmin(context,
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
    	initialize();
        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, item);
        InProgressSubmission workflowItem = workflowItemService.findByItem(context,
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
     * @deprecated Use {@link UIUtil#getItemIdentifier(org.dspace.core.Context, org.dspace.content.Item)} instead.
     */
    @Deprecated
    public static String[] addItemIdentifier(Item item, Version version)
    {
    	initialize();
        String[] result = null;
        String itemHandle = version.getItem().getHandle();

        List<MetadataValue> identifiers = itemService.getMetadata(item,
                MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
        String itemIdentifier = null;
        if (identifiers != null && identifiers.size() > 0)
        {
            itemIdentifier = identifiers.get(0).getValue();
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
     * @return version summary string
     */
    public static String getSummary(Context context, String stringVersionID)
    {
    	initialize();
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
