package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 18-aug-2010
 * Time: 10:35:03
 *
 * An action that sends the submission to the review stage
 */
public class DryadReviewAction extends ProcessingAction {

    private static Logger log = Logger.getLogger(DryadReviewAction.class);

    @Override
    public void activate(Context c, WorkflowItem wf) throws SQLException, IOException, WorkflowException {
        //When we activate this step we need to add a special key to the metadata
        UUID uuid = UUID.randomUUID();
        //Next add our unique key to our workflowitem
        wf.getItem().addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "reviewerKey", null, uuid.toString());

        try {
            Role role = WorkflowUtils.getCollectionRoles(wf.getCollection()).get("curator");
            List<String> mailsSent = new ArrayList<String>();
            //Retrieve the reviewers
            Group reviewersGroup = WorkflowUtils.getRoleGroup(c, wf.getCollection().getID(), role);
            if(reviewersGroup != null){
                //Loop over all the members & send a mail
                EPerson[] reviewers = Group.allMembers(c, reviewersGroup);
                for (EPerson reviewer : reviewers) {
                    if(!mailsSent.contains(reviewer.getEmail())){
                        sendReviewerEmail(c, reviewer.getEmail(), wf, uuid.toString());
                        mailsSent.add(reviewer.getEmail());
                    }
                }
            }
            DCValue[] journalReviewers = wf.getItem().getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", Item.ANY);
            for (DCValue journalReviewer : journalReviewers) {
                if(!mailsSent.contains(journalReviewer.value)){
                    sendReviewerEmail(c, journalReviewer.value, wf, uuid.toString());
                    mailsSent.add(journalReviewer.value);
                }
            }
            if(!mailsSent.contains(wf.getItem().getSubmitter().getEmail())){
                sendReviewerEmail(c, wf.getItem().getSubmitter().getEmail(), wf, uuid.toString());
            }

        } catch (WorkflowConfigurationException e) {
            log.error(LogManager.getHeader(c, "Error while activating dryad review action", "Workflowitemid: " + wf.getID()), e);
            throw new WorkflowException("Error while activating dryad review action");
        }

    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        DCValue[] approvedVals = wfi.getItem().getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", Item.ANY);
        if(approvedVals.length != 0){
            try{
                boolean approved = Boolean.valueOf(approvedVals[0].value);

                if(approved){
                    sendReviewApprovedEmail(c, wfi.getSubmitter().getEmail(), wfi);


                    return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
                }
                else
                    //Send us to the pending deletion
                    return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);


            } catch (Exception e){
                return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            }
        }else         
        if(request.getParameter("submit_leave") != null){
            //Return to the submission
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }else
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
    }

    private void sendReviewApprovedEmail(Context c, String emailAddress, WorkflowItem wfi) throws IOException, SQLException {
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "submit_datapackage_confirm"));

        email.addRecipient(emailAddress);

        email.addArgument(wfi.getItem().getName());

        //Add the doi of our data package
        String doi = DOIIdentifierProvider.getDoiValue(wfi.getItem());
        email.addArgument(doi == null ? "" : doi);

        //Get all the data files
        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
        String dataFileNames = "";
        String dataFileDois = "";
        for (Item dataFile : dataFiles){
            dataFileNames += dataFile.getName() + "\n";
            doi = DOIIdentifierProvider.getDoiValue(dataFile);
            dataFileDois += (doi == null ? "" : doi) + "\n";
        }

        email.addArgument(dataFileNames);
        email.addArgument(dataFileDois);

        try {
            email.send();
        } catch (MessagingException e) {
            log.error(LogManager.getHeader(c, "Error while email submitter about approved submission", "WorkflowItemId: " + wfi.getID()), e);
        }
    }

    private void sendReviewerEmail(Context c, String emailAddress, WorkflowItem wf, String key) throws IOException, SQLException {
        String template;
        boolean isDataPackage = DryadWorkflowUtils.isDataPackage(wf);
        if(isDataPackage)
            template = "submit_datapackage_review";
        else
            template = "submit_datafile_review";

        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), template));

        email.addRecipient(emailAddress);
        //Add the title
        email.addArgument(wf.getItem().getName());
        String doi = DOIIdentifierProvider.getDoiValue(wf.getItem());
        email.addArgument(doi == null ? "" : doi);

        //Add the parent data
        if(isDataPackage){
            //Get all the data files
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wf.getItem());
            String dataFileNames = "";
            for (Item dataFile : dataFiles)
                dataFileNames += dataFile.getName() + "\n";

            email.addArgument(dataFileNames);
        }else{
            //Get the data package
            Item dataPackage = DryadWorkflowUtils.getDataPackage(c, wf.getItem());
            email.addArgument(dataPackage.getName());
            //TODO: DECENT URL !
            email.addArgument(HandleManager.resolveToURL(c, dataPackage.getHandle()));
        }

        //add the submitter
        email.addArgument(wf.getSubmitter().getFullName() + " ("  + wf.getSubmitter().getEmail() + ")");

        // June 2011 - update URL from: submission-review to review
        //email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/submission-review?wfID=" + wf.getID() + "&token=" + key);
        email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/review?wfID=" + wf.getID() + "&token=" + key);

        try {
            email.send();
        } catch (MessagingException e) {
            log.error(LogManager.getHeader(c, "Error while email reviewer", "WorkflowItemId: " + wf.getID()), e);
        }
    }

    /*
    TODO/ finish this method once the submit_article_approve is up to date
    private void sendApprovedEmail(Context context, EPerson e, WorkflowItem wf) throws IOException {
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "submit_article_approve"));

        email.addRecipient(e.getEmail());

        email.addArgument(wf.getItem().getName());


    }
    */
}
