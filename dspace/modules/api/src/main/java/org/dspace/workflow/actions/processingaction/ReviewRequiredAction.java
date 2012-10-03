package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.dspace.workflow.actions.ActionResult;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 20-aug-2010
 * Time: 14:36:08
 *
 * An action in the dryad review process which indicates wether or not a submission is to go to the review stage
 */
public class ReviewRequiredAction extends ProcessingAction{

    private static int REVIEW_REQUIRED = 0;
    private static int REVIEW_NOT_REQUIRED = 1;

    private static Logger log = Logger.getLogger(ReviewRequiredAction.class);

    @Override
    public void activate(Context c, WorkflowItem wfItem) {

    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        Item item = wfi.getItem();

        DCValue[] skipVals = item.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY);
        item.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY);
        item.update();
        if(0 == skipVals.length || Boolean.valueOf(skipVals[0].value)){
            // if review not required send an email to the submitter.
            sendReviewApprovedEmail(c, wfi.getSubmitter().getEmail(), wfi);
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, REVIEW_NOT_REQUIRED);
        }else{
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, REVIEW_REQUIRED);
        }
    }



    private void sendReviewApprovedEmail(Context c, String emailAddress, WorkflowItem wfi) throws IOException, SQLException {
        try {
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


}
