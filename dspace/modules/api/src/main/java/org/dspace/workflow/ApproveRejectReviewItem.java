package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.MetadataSchema;
import org.dspace.content.DCDate;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dspace.core.Constants;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.eperson.EPerson;
import org.datadryad.rest.models.Manuscript;
import org.dspace.workflow.WorkflowItem;

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
    private static final Logger log = Logger.getLogger(ApproveRejectReviewItem.class);
    public static void main(String[] args) throws ApproveRejectReviewItemException, ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("m", "manuscriptnumber", true, "The manuscript number");
        options.addOption("i", "wfitemid", true, "The workflow item id");
        options.addOption("a", "approved", true, "Whether or not the the application will be approved, can either be true or false");
        options.addOption("h", "help", false, "help");


        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemImport\n", options);
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
            reviewItem(approved, manuscriptNumber);
        } else if(line.hasOption('i')) {
            // get a WorkflowItem using a workflow ID
            Integer wfItemId = Integer.parseInt(line.getOptionValue('i'));
            reviewItem(approved, wfItemId);
        } else {
            System.out.println("No manuscript number or workflow ID was given. One of these must be provided to identify the correct item in the review stage.");
            System.exit(1);
            return;
        }
    }

    private static void reviewItem(Boolean approved, String manuscriptNumber) throws ApproveRejectReviewItemException  {
        WorkflowItem wfi = null;
        ArrayList workflowItems = new ArrayList<WorkflowItem>();
        Context c = null;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();
            ItemIterator manuscriptItems = Item.findByMetadataField(c, "dc", "identifier", "manuscriptNumber", manuscriptNumber, false);
            if (manuscriptItems.hasNext()) {
                while (manuscriptItems.hasNext()) {
                    Item ms = manuscriptItems.next();
                    wfi = WorkflowItem.findByItemId(c, ms.getID());
                    if (wfi != null) {
                        workflowItems.add(wfi);
                    }
                }
            } else {
                throw new ApproveRejectReviewItemException("No item found with manuscript number: " + manuscriptNumber);
            }
            reviewItems(c, approved, workflowItems);
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
    }

    private static void reviewItem(Boolean approved, Integer workflowItemId) throws ApproveRejectReviewItemException {
        Context c = null;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();
            WorkflowItem wfi = WorkflowItem.find(c, workflowItemId);
            reviewItem(c, approved, wfi);
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
    }

    public static void reviewItems(Context c, Boolean approved, List<WorkflowItem> workflowItems) throws ApproveRejectReviewItemException {
        for (WorkflowItem wfi : workflowItems) {
            try {
                reviewItem(c, approved, wfi);
            } catch (ApproveRejectReviewItemException e) {
                throw new ApproveRejectReviewItemException("Exception caught while reviewing item " + wfi.getItem().getID() + ": " + e.getMessage(), e);
            }
        }
    }

    public static void reviewManuscript(Manuscript manuscript) throws ApproveRejectReviewItemException {
        Context c = null;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();
            Boolean approved = statusIsApproved(manuscript.getStatus());

            ArrayList<WorkflowItem> workflowItems = new ArrayList<WorkflowItem>();

            if (manuscript.dryadDataDOI != null) {
                try {
                    WorkflowItem wfi = WorkflowItem.findByDOI(c, manuscript.dryadDataDOI);
                    if (wfi != null) {
                        workflowItems.add(wfi);
                    }
                } catch (ApproveRejectReviewItemException e) {
    //                LOGGER.debug ("no workflow items matched DOI " + manuscript.dryadDataDOI);
                }
            }

            workflowItems.addAll(WorkflowItem.findAllByManuscript(c, manuscript));
            ApproveRejectReviewItem.reviewItems(c, approved, workflowItems);
        } catch (SQLException ex) {
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
    }

    private static boolean statusIsApproved(String status) throws ApproveRejectReviewItemException {
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

    private static void reviewItem(Context c, Boolean approved, WorkflowItem wfi) throws ApproveRejectReviewItemException {
	// get a List of ClaimedTasks, using the WorkflowItem
        List<ClaimedTask> claimedTasks = null;
        try {
            if (wfi != null) {
                claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
            }
            //Check for a valid task
            // There must be a claimed actions & it must be in the review stage, else it isn't a valid workflowitem
            Item item = wfi.getItem();
            if (claimedTasks == null || claimedTasks.isEmpty() || !claimedTasks.get(0).getActionID().equals("reviewAction")) {
                log.debug ("Item " + item.getID() + " not found or not in review");
            } else {
                ClaimedTask claimedTask = claimedTasks.get(0);
                if (approved) { // approve
                    Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
                    WorkflowActionConfig actionConfig = workflow.getStep(claimedTask.getStepID()).getActionConfig(claimedTask.getActionID());

                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", null, approved.toString());

                    WorkflowManager.doState(c, c.getCurrentUser(), null, claimedTask.getWorkflowItemID(), workflow, actionConfig);

                    // Add provenance to item
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", "Approved by ApproveRejectReviewItem on " + DCDate.getCurrent().toString() + " (GMT)");
                    item.update();
                } else { // reject
                    c.turnOffAuthorisationSystem();
                    String reason = "The journal with which your data submission is associated has notified us that your manuscript is no longer being considered for publication. If you feel this has happened in error, please contact us at help@datadryad.org.";
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
                    c.restoreAuthSystemState();
                }
            }
        } catch (SQLException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (AuthorizeException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (WorkflowConfigurationException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (IOException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (MessagingException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (WorkflowException ex) {
            throw new ApproveRejectReviewItemException(ex);
        }
    }

    private static SearchService getSearchService() {
        DSpace dspace = new DSpace();
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

}
