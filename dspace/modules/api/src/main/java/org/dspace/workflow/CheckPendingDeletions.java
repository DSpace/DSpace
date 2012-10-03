package org.dspace.workflow;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 20-aug-2010
 * Time: 15:00:11
 * A script that checks for rejected inactive data packages (in the submission) and deletes them
 */
public class CheckPendingDeletions {

    public static void main(String[] args) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        WorkspaceItem[] workspaceItems = WorkspaceItem.findAll(context);
        for (WorkspaceItem workspaceItem : workspaceItems) {
            deleteSubmission(context, workspaceItem);
        }

        WorkflowItem[] workflowItems = WorkflowItem.findAll(context);
        for(WorkflowItem workflowItem : workflowItems){
            deleteSubmission(context, workflowItem);
        }
        System.out.println("All done!");
    }

    private static void deleteSubmission(Context context, InProgressSubmission submission) throws SQLException, AuthorizeException, IOException {
        DCValue[] values = submission.getItem().getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "rejectDate", Item.ANY);
        if (values.length > 0) {
            DCDate DCrejectDate = new DCDate(values[0].value);
            Calendar rejectDate = Calendar.getInstance();
            rejectDate.setTime(DCrejectDate.toDate());
            Calendar now = Calendar.getInstance();
            if (now.after(rejectDate)){
                System.out.println("Rejecting dataset workspaceItemId: " + submission.getID());
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, submission.getItem());
                for (Item dataFile : dataFiles) {
                    try {
                        System.out.println("Rejecting datafile workflowitemid: " + dataFile.getID());
                        WorkspaceItem wsi = WorkspaceItem.findByItemId(context, dataFile.getID());
                        if(wsi == null){
                            wsi = WorkflowManager.rejectWorkflowItem(context, WorkflowItem.findByItemId(context, dataFile.getID()), null, null, null, false);
                        }

                        wsi.deleteAll();
                    } catch (Exception e) {
                        System.err.println("Error while rejecting data file: " + dataFile.getID());
                        e.printStackTrace();
                    }
                }
                WorkspaceItem workspaceItem;
                if(submission instanceof WorkspaceItem)
                    workspaceItem = (WorkspaceItem) submission;
                else
                    workspaceItem = WorkflowManager.rejectWorkflowItem(context, (WorkflowItem) submission, null, null, null, false);

                workspaceItem.deleteAll();

                context.commit();
                System.out.println("Rejected");
            }
        }
    }


}