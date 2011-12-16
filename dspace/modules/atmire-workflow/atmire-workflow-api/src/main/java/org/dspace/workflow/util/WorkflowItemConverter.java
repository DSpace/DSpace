package org.dspace.workflow.util;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowItem;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 31-aug-2010
 * Time: 9:12:01
 */
public class WorkflowItemConverter {

    public static void main(String[] args) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();
        System.out.println("All workflowitems will be sent back to the workspace.");


        System.out.println("Removing pooled tasks");
        //First of all find all the pooled tasks & delete them
        DatabaseManager.updateQuery(context, "DELETE FROM TaskListItem");

        System.out.println("Sending all workflow items back to the workspace");

        WorkflowItem[] workflowItems = WorkflowItem.findAll(context);

        for (int i = 0; i < workflowItems.length; i++) {
            System.out.println("Processing workflow item " + i + " of " + workflowItems.length);
            WorkflowItem workflowItem = workflowItems[i];
            Item myItem = workflowItem.getItem();
            EPerson submitter = workflowItem.getSubmitter();
            //Remove all the authorizations & give the submitter the right rights
            AuthorizeManager.removeAllPolicies(context, myItem);
            AuthorizeManager.addPolicy(context, myItem, Constants.READ, submitter);
            AuthorizeManager.addPolicy(context, myItem, Constants.WRITE, submitter);
            AuthorizeManager.addPolicy(context, myItem, Constants.ADD, submitter);
            AuthorizeManager.addPolicy(context, myItem, Constants.REMOVE, submitter);


            TableRow row = DatabaseManager.create(context, "workspaceitem");
            row.setColumn("item_id", myItem.getID());
            row.setColumn("collection_id", workflowItem.getCollection().getID());
            DatabaseManager.update(context, row);

            int wsi_id = row.getIntColumn("workspace_item_id");
            WorkspaceItem wi = WorkspaceItem.find(context, wsi_id);
            wi.setMultipleFiles(workflowItem.hasMultipleFiles());
            wi.setMultipleTitles(workflowItem.hasMultipleTitles());
            wi.setPublishedBefore(workflowItem.isPublishedBefore());
            wi.update();

            context.removeCached(myItem, myItem.getID());
        }
        System.out.println("All workflow items sent back to workspace, delete all the workflowitems");
        DatabaseManager.updateQuery(context,"DELETE FROM WorkflowItem");

        System.out.println("All done, committing context");
        context.commit();
        context.complete();

    }
}
