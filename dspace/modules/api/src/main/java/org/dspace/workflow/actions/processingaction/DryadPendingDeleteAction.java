package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.Action;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Locale;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 19-aug-2010
 * Time: 14:11:47
 *
 * An action in the dryad workflow where the submission will be sent back to the submitter
 */
public class DryadPendingDeleteAction extends ProcessingAction {

    private static final Logger log = Logger.getLogger(DryadPendingDeleteAction.class);

    @Override
    public void activate(Context c, WorkflowItem wf) throws SQLException, IOException {
        //Add the rejected date
        Calendar deleteDate = Calendar.getInstance();
        deleteDate.add(Calendar.MONTH, 1);

        //Create a task for a dummy eperson, so we can retrieve it
        try {
            WorkflowManager.createOwnedTask(c, wf, getParent().getStep(), getParent(), null);

            //Grant the curator read rights
            DryadWorkflowUtils.grantCuratorReadRightsOnItem(c, wf, this);
            

            WorkspaceItem workspace = WorkflowManager.rejectWorkflowItem(c, wf, null, null, "Rejected by reviewers", true);
            workspace.getItem().addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "rejectDate", null, new DCDate(deleteDate.getTime()).toString());
            workspace.getItem().update();

            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, workspace.getItem());
            for (Item dataFile : dataFiles) {
                try {
                    log.info("Rejecting datafile workflowitemid: " + dataFile.getID());
                    WorkflowItem wfi = WorkflowItem.findByItemId(c, dataFile.getID());
                    WorkflowManager.rejectWorkflowItem(c, wfi, null, null, null, false);
                } catch (Exception e) {
                    log.error("Error while rejecting data file: " + dataFile.getID());
                    e.printStackTrace();
                }
            }
        } catch (AuthorizeException e) {
            log.error("Error while activating delete action", e);
        }
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        //Since these item are awaiting deletion just use cancel
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }
}
