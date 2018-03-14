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
import org.dspace.JournalUtils;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

// DF
import java.util.*;
/**
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.actions.Action;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 */

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.datadryad.api.DryadJournalConcept;

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
                        WorkflowEmailManager.sendReviewerEmail(c, reviewer.getEmail(), wf, uuid.toString());
                        mailsSent.add(reviewer.getEmail());
                    }
                }
            }

        // Add note to item's metadata as a DC field - DF
        // Item myitem = wf.getItem();
        String provDescription = "";
        // String now = DCDate.getCurrent().toString();
        provDescription = "Item placed in review" + " on ";
        // provDescription = "Item placed in review" + " on " + now + " (GMT) ";
        // wf.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        // wf.getItem().update();
        // end DF

            sendEmailToJournalNotifyOnReview(c, wf, mailsSent, uuid);

            if(!mailsSent.contains(wf.getItem().getSubmitter().getEmail())){
                WorkflowEmailManager.sendReviewerEmail(c, wf.getItem().getSubmitter().getEmail(), wf, uuid.toString());
            }

        } catch (WorkflowConfigurationException e) {
            log.error(LogManager.getHeader(c, "Error while activating dryad review action", "Workflowitemid: " + wf.getID()), e);
            throw new WorkflowException("Error while activating dryad review action");
        }

    }

    private void sendEmailToJournalNotifyOnReview(Context c, WorkflowItem wf, List<String> mailsSent, UUID uuid) throws SQLException, IOException, WorkflowException {
        DCValue[] values=wf.getItem().getMetadata("prism.publicationName");
        if (values!=null && values.length> 0) {
            String journal = values[0].value;
            if (journal!=null) {
                DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalName(journal);
                if (journalConcept != null) {
                    ArrayList<String> emails = journalConcept.getEmailsToNotifyOnReview();
                    for (String email : emails) {
                        if(!mailsSent.contains(email)) {
                            WorkflowEmailManager.sendReviewerEmail(c, email, wf, uuid.toString());
                            mailsSent.add(email);
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
                    WorkflowEmailManager.sendReviewApprovedEmail(c, wfi.getSubmitter().getEmail(), wfi);


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
}
