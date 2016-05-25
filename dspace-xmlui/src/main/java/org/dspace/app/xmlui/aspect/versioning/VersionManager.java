/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionManager {

    private static final Message T_version_created = new Message("default", "The new version has been created.");
    private static final Message T_version_delete = new Message("default", "The selected version(s) have been deleted.");
    private static final Message T_version_updated = new Message("default", "The version has been updated.");
    private static final Message T_version_restored = new Message("default", "The version has been restored.");

    protected static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static final VersionHistoryService versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
    protected static final VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();
    protected static final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();


    /**
     * Create a new version of the specified item
     *
     * @param context The DSpace context
     * @param itemID  The id of the to-be-versioned item
     * @param summary summary.
     * @return A result object
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    public static FlowResult processCreateNewVersion(Context context, UUID itemID, String summary)
            throws SQLException, AuthorizeException, IOException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = itemService.find(context, itemID);

            if (authorizeService.isAdmin(context, item) || itemService.canEdit(context, item)) {
                Version version = versioningService.createNewVersion(context, item, summary);
                WorkspaceItem wsi = workspaceItemService.findByItem(context, version.getItem());

                result.setParameter("wsid", wsi.getID());
                result.setOutcome(true);
                result.setContinue(true);
                result.setMessage(T_version_created);
                result.setParameter("summary", summary);
            }
        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }

    /**
     * Modify latest version
     *
     * @param context The DSpace context
     * @param itemID  The id of the to-be-versioned item
     * @param summary summary.
     * @return A result object
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    public static FlowResult processUpdateVersion(Context context, UUID itemID, String summary) throws SQLException, AuthorizeException, IOException {

        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = itemService.find(context, itemID);

            if (authorizeService.isAdmin(context, item)) {
                versioningService.updateVersion(context, item, summary);

                result.setOutcome(true);
                result.setContinue(true);
                result.setMessage(T_version_updated);
                result.setParameter("summary", summary);
            }
        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }


    /**
     * Restore a version
     *
     * @param versionID id of the version to restore
     * @param context   The DSpace context
     * @param summary summary.
     * @return A result object
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    public static FlowResult processRestoreVersion(Context context, int versionID, String summary) throws SQLException, AuthorizeException, IOException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            versioningService.restoreVersion(context, versioningService.getVersion(context, versionID), summary);

            result.setOutcome(true);
            result.setContinue(true);
            result.setMessage(T_version_restored);
        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }


    /**
     * Delete version(s)
     *
     * @param context    The DSpace context
     * @param itemId the item to be reduced.
     * @param versionIDs list of versionIDs to delete
     * @return A result object
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     */
    public static FlowResult processDeleteVersions(Context context, UUID itemId, String[] versionIDs)
            throws SQLException, AuthorizeException, IOException, UIException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = itemService.find(context, itemId);

            VersionHistory versionHistory = versionHistoryService.findByItem(context, item);

            for (String id : versionIDs) {
                versioningService.removeVersion(context, versioningService.getVersion(context, Integer.parseInt(id)));
            }

            //Retrieve the latest version of our history (IF any is even present)
            Version latestVersion = versionHistoryService.getLatestVersion(context, versionHistory);
            if(latestVersion == null){
                result.setParameter("itemID", null);
            }else{
                result.setParameter("itemID", latestVersion.getItem().getID());
            }
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_version_delete);

        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }
}
