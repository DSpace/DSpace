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

    private static Boolean statusIsApproved(String status) throws ApproveRejectReviewItemException {
        Boolean approved = null;
        if (Manuscript.statusIsAccepted(status)) {
            approved = true;
        } else if (Manuscript.statusIsRejected(status)) {
            approved = false;
        } else if (Manuscript.statusIsNeedsRevision(status)) {
            approved = false;
        } else if (Manuscript.statusIsPublished(status)) {
            approved = true;
        } else {
            throw new ApproveRejectReviewItemException("Status " + status + " is neither approved nor rejected");
        }
        return approved;
    }

    public static void processWorkflowItemUsingManuscript(Context c, WorkflowItem wfi, Manuscript manuscript) throws ApproveRejectReviewItemException {
	// get a List of ClaimedTasks, using the WorkflowItem
        List<ClaimedTask> claimedTasks = null;
        try {
            if (wfi != null) {
                claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
            }
            //Check for a valid task
            // There must be a claimedTask & it must be in the review stage, else it isn't a review workflowitem
            Item item = wfi.getItem();
            DryadDataPackage dataPackage = new DryadDataPackage(item);
            StringBuilder provenance = new StringBuilder();
            c.turnOffAuthorisationSystem();
            // update duplicate submission metadata for this item.
            item.checkForDuplicateItems(c);
            if (claimedTasks == null || claimedTasks.isEmpty() || !claimedTasks.get(0).getActionID().equals("reviewAction")) {
                log.debug ("Item " + item.getID() + " not found or not in review");
            } else {
                ClaimedTask claimedTask = claimedTasks.get(0);
                associateWithManuscript(dataPackage, manuscript, provenance);
                if (statusIsApproved(manuscript.getStatus())) { // approve
                    Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
                    WorkflowActionConfig actionConfig = workflow.getStep(claimedTask.getStepID()).getActionConfig(claimedTask.getActionID());

                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", null, statusIsApproved(manuscript.getStatus()).toString());

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
                    disassociateFromManuscript(dataPackage, manuscript);
                }
            }
        } catch (Exception ex) {
            throw new ApproveRejectReviewItemException(ex);
        }
        finally {
            c.restoreAuthSystemState();
        }
    }

    /**
     * Copies manuscript metadata into a dryad data package
     * @param dataPackage
     * @param manuscript
     * @param message
     * @throws SQLException
     */
    private static void associateWithManuscript(DryadDataPackage dataPackage, Manuscript manuscript, StringBuilder message) throws SQLException {
        if (manuscript != null) {
            // set publication DOI
            if (!"".equals(manuscript.getPublicationDOI()) && !dataPackage.getItem().hasMetadataEqualTo(PUBLICATION_DOI, manuscript.getPublicationDOI())) {
                String oldValue = dataPackage.getPublicationDOI();
                dataPackage.setPublicationDOI(manuscript.getPublicationDOI());
                message.append(" " + PUBLICATION_DOI + " was updated from " + oldValue + ".");
            }
            // set Manuscript ID
            if (!"".equals(manuscript.getManuscriptId()) && !dataPackage.getItem().hasMetadataEqualTo(MANUSCRIPT, manuscript.getManuscriptId())) {
                String oldValue = dataPackage.getManuscriptNumber();
                dataPackage.setManuscriptNumber(manuscript.getManuscriptId());
                message.append(" " + MANUSCRIPT + " was updated from " + oldValue + ".");
            }
//            // union keywords
//            if (manuscript.getKeywords().size() > 0) {
//                ArrayList<String> unionKeywords = new ArrayList<String>();
//                unionKeywords.addAll(dataPackage.getKeywords());
//                for (String newKeyword : manuscript.getKeywords()) {
//                    if (!unionKeywords.contains(newKeyword)) {
//                        unionKeywords.add(newKeyword);
//                    }
//                }
//                dataPackage.setKeywords(unionKeywords);
//            }
            // set title
            if (!"".equals(manuscript.getTitle()) && !dataPackage.getItem().hasMetadataEqualTo(ARTICLE_TITLE, manuscript.getTitle())) {
                String oldValue = dataPackage.getTitle();
                dataPackage.setTitle(prefixTitle(manuscript.getTitle()));
                message.append(" " + ARTICLE_TITLE + " was updated from \"" + oldValue + "\".");
            }
            // set abstract
            if (!"".equals(manuscript.getAbstract()) && !dataPackage.getItem().hasMetadataEqualTo(ABSTRACT, manuscript.getAbstract())) {
                dataPackage.setAbstract(manuscript.getAbstract());
                message.append(" " + ABSTRACT + " was updated.");
            }
            // set publicationDate
            if (manuscript.getPublicationDate() != null) {
                SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = dateIso.format(manuscript.getPublicationDate());
                String oldValue = dataPackage.getPublicationDate();
                if (!dateString.equals(oldValue)) {
                    dataPackage.setPublicationDate(dateString);
                    message.append(" " + PUBLICATION_DATE + " was updated from " + oldValue + ".");
                }
            }
        }
    }

    private static void disassociateFromManuscript(DryadDataPackage dataPackage, Manuscript manuscript) throws SQLException {
        if (manuscript != null) {
            // clear publication DOI
            dataPackage.setPublicationDOI(null);
            // If there is a manuscript number, move it to former msid
            dataPackage.setFormerManuscriptNumber(dataPackage.getManuscriptNumber());
            // clear Manuscript ID
            dataPackage.setManuscriptNumber(null);
            // disjoin keywords
            List<String> packageKeywords = dataPackage.getKeywords();
            List<String> manuscriptKeywords = manuscript.getKeywords();
            List<String> prunedKeywords = subtractList(packageKeywords, manuscriptKeywords);

            dataPackage.setKeywords(prunedKeywords);
            // clear publicationDate
            dataPackage.setBlackoutUntilDate(null);
        }
    }

    private static List<String> subtractList(List<String> list1, List<String> list2) {
        List<String> list = new ArrayList<String>(list1);
        for(String string : list2) {
            if(list.contains(string)) {
                list.remove(string);
            }
        }
        return list;
    }


    private static String prefixTitle(String title) {
        return String.format("Data from: %s", title);
    }
}
