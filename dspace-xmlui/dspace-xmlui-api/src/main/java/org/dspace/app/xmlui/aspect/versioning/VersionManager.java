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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;

import java.io.IOException;
import java.sql.SQLException;

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


    /**
     * Create a new version of the specified item
     *
     * @param context The DSpace context
     * @param itemID  The id of the to-be-versioned item
     * @return A result object
     */
    // Versioning
    public static FlowResult processCreateNewVersion(Context context, int itemID, String summary) throws SQLException, AuthorizeException, IOException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.isAdmin(context, item) || item.canEdit()) {
                VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
                Version version = versioningService.createNewVersion(context, itemID, summary);
                WorkspaceItem wsi = WorkspaceItem.findByItem(context, version.getItem());

                context.commit();

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
     * @return A result object
     */
    // Versioning
    public static FlowResult processUpdateVersion(Context context, int itemID, String summary) throws SQLException, AuthorizeException, IOException {

        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.isAdmin(context, item)) {
                VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
                versioningService.updateVersion(context, itemID, summary);

                context.commit();

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
     * @return A result object
     */
    // Versioning
    public static FlowResult processRestoreVersion(Context context, int versionID, String summary) throws SQLException, AuthorizeException, IOException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            versioningService.restoreVersion(context, versionID, summary);

            context.commit();

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
     * @param versionIDs list of versionIDs to delete
     * @return A result object
     */
    // Versioning
    public static FlowResult processDeleteVersions(Context context, int itemId, String[] versionIDs) throws SQLException, AuthorizeException, IOException, UIException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);

            VersionHistory versionHistory = versioningService.findVersionHistory(context, itemId);

            for (String id : versionIDs) {
                versioningService.removeVersion(context, Integer.parseInt(id));
            }
            context.commit();

            //Retrieve the latest version of our history (IF any is even present)
            Version latestVersion = versionHistory.getLatestVersion();
            if(latestVersion == null){
                result.setParameter("itemID", null);
            }else{
                result.setParameter("itemID", latestVersion.getItemID());
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
