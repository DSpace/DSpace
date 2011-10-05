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
            

            WorkspaceItem workspace = rejectWorkflowItem(c, wf, null, null, "Rejected by reviewers", true);
            workspace.getItem().addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "rejectDate", null, new DCDate(deleteDate.getTime()).toString());
            workspace.getItem().update();

            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, workspace.getItem());
            for (Item dataFile : dataFiles) {
                try {
                    log.info("Rejecting datafile workflowitemid: " + dataFile.getID());
                    WorkflowItem wfi = WorkflowItem.findByItemId(c, dataFile.getID());
                    rejectWorkflowItem(c, wfi, null, null, null, false);
                } catch (Exception e) {
                    log.error("Error while rejecting data file: " + dataFile.getID());
                    e.printStackTrace();
                }
            }
        } catch (AuthorizeException e) {
            log.error("Error while activating delete action", e);
        }
    }

    public static WorkspaceItem rejectWorkflowItem(Context c, WorkflowItem wi, EPerson e, Action action,
            String rejection_message, boolean sendMail) throws SQLException, AuthorizeException,
            IOException
    {
        // authorize a DSpaceActions.REJECT
        // stop workflow
        WorkflowManager.deleteAllTasks(c, wi);

        //Also clear all info for this step
        WorkflowRequirementsManager.clearStepMetadata(wi);

        // Remove (if any) the workflowItemroles for this item
        WorkflowItemRole[] workflowItemRoles = WorkflowItemRole.findAllForItem(c, wi.getID());
        for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
            workflowItemRole.delete();
        }

        // rejection provenance
        Item myitem = wi.getItem();

        // Get current date
        String now = DCDate.getCurrent().toString();


        // Here's what happened
        if(action != null){
            // Get user's name + email address
            String usersName = WorkflowManager.getEPersonName(e);
            String provDescription = action.getProvenanceStartId() + " Rejected by " + usersName + ", reason: "
                    + rejection_message + " on " + now + " (GMT) ";

            // Add to item as a DC field
            myitem.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        }

        myitem.update();

        // convert into personal workspace
        WorkspaceItem wsi = WorkflowManager.returnToWorkspace(c, wi);

        if(sendMail){
            // notify that it's been rejected
            notifyOfReject(c, wi, e, rejection_message);
        }

        log.info(LogManager.getHeader(c, "reject_workflow", "workflow_item_id="
                + wi.getID() + "item_id=" + wi.getItem().getID()
                + "collection_id=" + wi.getCollection().getID() + "eperson_id="
                + (e == null ? "" :  e.getID())));

        return wsi;
    }


    private static void notifyOfReject(Context c, WorkflowItem wi, EPerson e,
        String reason)
    {
        try
        {
            // Get the item title
            String title = wi.getItem().getName();
            String dataFileTitles = "";
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wi.getItem());
            for (Item dataFile : dataFiles) {
                dataFileTitles += dataFile.getName() + "\n";
            }

            // Get rejector's name
            String rejector = e == null ? "" : WorkflowManager.getEPersonName(e);
            Locale supportedLocale = I18nUtil.getEPersonLocale(e);
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale,"submit_article_reject"));

            email.addRecipient(wi.getSubmitter().getEmail());

            email.addArgument(title);
            email.addArgument(dataFileTitles);
            email.addArgument(rejector);
            email.addArgument(reason);
//            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/mydspace");

            email.send();
        }
        catch (Exception ex)
        {
	    System.out.println(ex);
	    log.error("unable to send submit_article_reject email", ex);
	    log.error("eperson object=" + e);
	    log.error("workflowItem object=" +wi);
            // log this email error
            log.error(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + e.getID()
                            + " eperson_email" + e.getEmail()
                            + " workflow_item_id" + wi.getID()));
        }
    }


    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        //Since these item are awaiting deletion just use cancel
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }
}
