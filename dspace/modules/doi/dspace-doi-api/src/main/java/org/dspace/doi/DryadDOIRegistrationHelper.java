/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.doi;

import java.sql.SQLException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Convenience methods involved in registering DOIs.
 * @author dan
 */
public class DryadDOIRegistrationHelper {

    public static final String DRYAD_PENDING_PUBLICATION_STEP = "pendingPublicationStep";

    public static boolean isDataPackageInPublicationBlackout(Context context, Item dataPackage) throws SQLException {
        // Publication Blackout is determined by the taskowner table,
        // which is not a part of dspace, so it must be queried directly.
        String query = "select taskowner.step_id from taskowner, workflowitem where taskowner.workflow_item_id = workflowitem.workflow_id and workflowitem.item_id = ?";
        TableRow row = DatabaseManager.querySingleTable(context, "taskowner", query, dataPackage.getID());
        if(row != null && DRYAD_PENDING_PUBLICATION_STEP.equals(row.getStringColumn("step_id"))) {
            return true;
        } else {
            return false;
        }
    }

}
