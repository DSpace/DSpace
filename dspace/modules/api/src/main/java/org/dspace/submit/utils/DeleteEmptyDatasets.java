package org.dspace.submit.utils;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 24-sep-2010
 * Time: 10:33:02
 *
 * A class that removes any left over workspace data files
 * This is the case when there is some faulty linking between a data package and its da files
 */
public class DeleteEmptyDatasets {

    public static void main(String[] args) throws SQLException, AuthorizeException, IOException {
        Context context = new Context();

        context.turnOffAuthorisationSystem();

        WorkspaceItem[] workspaceItems = WorkspaceItem.findByCollection(context, (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.dataset.collection")));

        for (WorkspaceItem workspaceItem : workspaceItems) {
            int parent = workspaceItem.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", Item.ANY).length;
            String submitterMail;
            if(workspaceItem.getSubmitter() != null)
                submitterMail = workspaceItem.getSubmitter().getEmail();
            else
                submitterMail = "Unknown";

            if(parent == 0 && workspaceItem.getItem().getHandle() == null){
                //Delete this workspaceItem
                System.out.println("Removed item with workspace id: " + workspaceItem.getID() + " submitter: " + submitterMail);
                workspaceItem.deleteAll();
            }
        }

        context.complete();
    }
}
