package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;
import org.dspace.workflow.actions.WorkflowActionConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 19-aug-2010
 * Time: 14:25:02
 *
 * An executable class which determines if an item is to be rejected or accepted,
 * when accepted the submission is sent to the next step
 *
 * Updated by dcl9 (dan.leehr@nescent.org) 04-Sep-2014 - exposing reviewItem as static methods
 */
public class ApproveRejectReviewItem {
    private static final String PUBLICATION_DOI = "dc.relation.isreferencedby";
    private static final String MANUSCRIPT = "dc.identifier.manuscriptNumber";
    private static final String ARTICLE_TITLE = "dc.title";
    private static final String ABSTRACT = "dc.description";
    private static final String PUBLICATION_DATE = "dc.date.issued";

    private static final Logger log = Logger.getLogger(ApproveRejectReviewItem.class);
    public static void main(String[] args) throws ApproveRejectReviewItemException, ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("m", "manuscriptnumber", true, "The manuscript number");
        options.addOption("i", "wfitemid", true, "The workflow item id");
        options.addOption("j", "journalcode", true, "The journal code");
        options.addOption("a", "approved", true, "Whether or not the the application will be approved, can either be true or false");
        options.addOption("h", "help", false, "help");


        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("dspace review-item\n", options);
            System.exit(1);
        }
        Boolean approved;

        if(line.hasOption('a')){
            approved = Boolean.valueOf(line.getOptionValue('a'));
        }else{
            System.out.println("No result (approved true or false) was given");
            System.exit(1);
            return;
        }

        if(line.hasOption('m')){
            // get a WorkflowItem using a manuscript number
            String manuscriptNumber = line.getOptionValue('m');
            String journalCode = line.getOptionValue('j');
            if (journalCode == null) {
                System.out.println("No journal code was given. This is needed to match the manuscript number with an item in review.");
                System.exit(1);
            }
            DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(journalCode);
            String status = approved ? Manuscript.STATUS_ACCEPTED : Manuscript.STATUS_REJECTED;
            Manuscript manuscript = new Manuscript(journalConcept);
            manuscript.setManuscriptId(manuscriptNumber);
            manuscript.setStatus(status);
            processWorkflowItemsUsingManuscript(manuscript);
        } else if(line.hasOption('i')) {
            // get a WorkflowItem using a workflow ID
            Integer wfItemId = Integer.parseInt(line.getOptionValue('i'));
            Context c = null;
            try {
                c = new Context();
                c.turnOffAuthorisationSystem();
                WorkflowItem wfi = WorkflowItem.find(c, wfItemId);
                Manuscript manuscript = new Manuscript();
                manuscript.setStatus(approved ? Manuscript.STATUS_ACCEPTED : Manuscript.STATUS_REJECTED);

                processWorkflowItemUsingManuscript(c, wfi, manuscript);
                c.restoreAuthSystemState();
            } catch (SQLException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } catch (AuthorizeException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } catch (IOException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } finally {
                if(c != null) {
                    try {
                        c.complete();
                    } catch (SQLException ex) {
                        // Swallow it
                    } finally {
                        c = null;
                    }
                }
            }
        } else {
            System.out.println("No manuscript number or workflow ID was given. One of these must be provided to identify the correct item in the review stage.");
            System.exit(1);
        }
    }

    public static void processWorkflowItemsUsingManuscript(Manuscript manuscript) throws ApproveRejectReviewItemException {
        Context c = null;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();

            ArrayList<WorkflowItem> workflowItems = new ArrayList<WorkflowItem>();

            if (manuscript.getDryadDataDOI() != null) {
                try {
                    WorkflowItem wfi = WorkflowItem.findByDOI(c, manuscript.getDryadDataDOI());
                    if (wfi != null) {
                        workflowItems.add(wfi);
                    }
                } catch (ApproveRejectReviewItemException e) {
    //                LOGGER.debug ("no workflow items matched DOI " + manuscript.dryadDataDOI);
                }
            }

            workflowItems.addAll(WorkflowItem.findAllByManuscript(c, manuscript));
            for (WorkflowItem wfi : workflowItems) {
                try {
                    processWorkflowItemUsingManuscript(c, wfi, manuscript);
                } catch (ApproveRejectReviewItemException e) {
                    throw new ApproveRejectReviewItemException("Exception caught while reviewing item " + wfi.getItem().getID() + ": " + e.getMessage(), e);
                }
            }
        } catch (SQLException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } finally {
            if(c != null) {
                try {
                    c.complete();
                } catch (SQLException ex) {
                    // Swallow it
                }
            }
        }
    }

    public static void processWorkflowItemUsingManuscript(Context c, WorkflowItem wfi, Manuscript manuscript) throws ApproveRejectReviewItemException {
        try {
            Item item = wfi.getItem();
            DryadDataPackage dataPackage = new DryadDataPackage(item);
            StringBuilder provenance = new StringBuilder();
            c.turnOffAuthorisationSystem();
            // update duplicate submission metadata for this item.
            item.checkForDuplicateItems(c);
            if (DryadWorkflowUtils.isItemInReview(c, wfi)) {
                List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
                ClaimedTask claimedTask = claimedTasks.get(0);
                dataPackage.associateWithManuscript(manuscript, provenance);
                if (Manuscript.statusIsApproved(manuscript.getStatus())) { // approve
                    Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
                    WorkflowActionConfig actionConfig = workflow.getStep(claimedTask.getStepID()).getActionConfig(claimedTask.getActionID());

                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", null, Manuscript.statusIsApproved(manuscript.getStatus()).toString());

                    WorkflowManager.doState(c, c.getCurrentUser(), null, claimedTask.getWorkflowItemID(), workflow, actionConfig);
                    // Add provenance to item
                    String manuscriptNumber = "<null>";
                    if (manuscript != null) {
                        manuscriptNumber = manuscript.getManuscriptId();
                    }
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", "Approved by ApproveRejectReviewItem based on metadata for " + manuscriptNumber + " on " + DCDate.getCurrent().toString() + " (GMT)" + provenance.toString());
                    item.update();
                } else { // reject
                    String reason = "The journal with which your data submission is associated has notified us that your manuscript is no longer being considered for publication. If you feel this has happened in error or wish to re-submit your data associated with a different journal, please contact us at help@datadryad.org.";
                    EPerson ePerson = EPerson.findByEmail(c, ConfigurationManager.getProperty("system.curator.account"));
                    //Also reject all the data files
                    Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
                    for (Item dataFile : dataFiles) {
                        try {
                            WorkflowManager.rejectWorkflowItem(c, WorkflowItem.findByItemId(c, dataFile.getID()), ePerson, null, reason, false);
                        } catch (Exception e) {
                            throw new IOException(e);
                        }
                    }
                    WorkspaceItem wsi = WorkflowManager.rejectWorkflowItem(c, wfi, ePerson, null, reason, true);
                    dataPackage.disassociateFromManuscript(manuscript);
                }
            }
        } catch (Exception ex) {
            throw new ApproveRejectReviewItemException(ex);
        }
        finally {
            c.restoreAuthSystemState();
        }
    }
}
