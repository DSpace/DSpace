package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.*;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        try{
            wf.getItem().update();
        } catch (AuthorizeException e)
        {
            log.error("cant update reviewerKey for item:"+wf.getItem().getID());
        }

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

            sendEmailToJournalNotifyOnReview(c, wf, mailsSent, uuid);

            if(!mailsSent.contains(wf.getItem().getSubmitter().getEmail())){
                sendReviewerEmail(c, wf.getItem().getSubmitter().getEmail(), wf, uuid.toString());
            }

        } catch (WorkflowConfigurationException e) {
            log.error(LogManager.getHeader(c, "Error while activating dryad review action", "Workflowitemid: " + wf.getID()), e);
            throw new WorkflowException("Error while activating dryad review action");
        }

    }

    private void sendEmailToJournalNotifyOnReview(Context c, WorkflowItem wf, List<String> mailsSent, UUID uuid) throws SQLException, IOException, WorkflowException {
        DCValue[] values=wf.getItem().getMetadata("prism.publicationName");
        if(values!=null && values.length> 0){
            String journal = values[0].value;
            if(journal!=null){
                Map<String, String> properties = DryadJournalSubmissionUtils.getPropertiesByJournal(journal);
                if(properties!=null){
                    String emails = properties.get(DryadJournalSubmissionUtils.NOTIFY_ON_REVIEW);
                    log.debug("reviewers for journal " + journal + " are " + emails);
                    if(emails != null) {
                        String[] emails_=emails.split(",");
                        for(String email : emails_){
                            if(!mailsSent.contains(email)){
                                sendReviewerEmail(c, email, wf, uuid.toString());
                                mailsSent.add(email);
                            }
                        }
                    }
                }
            }
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
	    // Send the email -- Unless the journal is Evolution
	    // TODO: make this configurable for each journal
	    DCValue journals[] = wfi.getItem().getMetadata("prism", "publicationName", null, Item.ANY);
	    String journalName =  (journals.length >= 1) ? journals[0].value : null;
	    if(journalName !=null && !journalName.equals("Evolution") && !journalName.equals("Evolution*")) {
		log.debug("sending submit_datapackage_confirm");
		email.send();
	    } else {
		log.debug("skipping submit_datapackage_confirm; journal is " + journalName);
	    }
	} catch (MessagingException e) {
            log.error(LogManager.getHeader(c, "Error while email submitter about approved submission", "WorkflowItemId: " + wfi.getID()), e);
        }
    }

    private void sendReviewerEmail(Context c, String emailAddress, WorkflowItem wf, String key) throws IOException, SQLException {
	log.debug("sending review email for workflow item " + wf.getID() + " to " + emailAddress);
        try {
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
            if(dataPackage!=null){
                email.addArgument(dataPackage.getName());
            }
            //TODO: DECENT URL !
            email.addArgument(HandleManager.resolveToURL(c, dataPackage.getHandle()));
        }

        //add the submitter
        email.addArgument(wf.getSubmitter().getFullName() + " ("  + wf.getSubmitter().getEmail() + ")");

	// add the review URL (using provisional DOI as key)
        email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/review?doi=" + doi);

	// add journal's manuscript number
	String manuScriptIdentifier = "none available";
	DCValue[] manuScripIdentifiers = wf.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "identifier", "manuscriptNumber", Item.ANY);
	if(0 < manuScripIdentifiers.length){
	    manuScriptIdentifier = manuScripIdentifiers[0].value;
	}
	
	email.addArgument(manuScriptIdentifier);

	// Add journal name
	String journalName = "not available";
	DCValue[] journalNames = wf.getItem().getMetadata("prism", "publicationName", null, Item.ANY);
	if(0 < journalNames.length){
	    journalName = journalNames[0].value;
	}
        
        email.addArgument(journalName);

            email.send();
        } catch (Exception e) {
            log.error(LogManager.getHeader(c, "Error while email reviewer", "WorkflowItemId: " + wf.getID()), e);
        }
    }

}
