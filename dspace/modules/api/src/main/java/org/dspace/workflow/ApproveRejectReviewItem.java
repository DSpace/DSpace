package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.actions.WorkflowActionConfig;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.dspace.core.Constants;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;

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

    public static void reviewItemDOI(Boolean approved, String dataPackageDOI) throws ApproveRejectReviewItemException  {
        WorkflowItem wfi = null;
        Context c = null;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();
            IdentifierService identifierService = getIdentifierService();
            DSpaceObject object = identifierService.resolve(c, dataPackageDOI);
            if(object == null) {
                throw new ApproveRejectReviewItemException("DOI " + dataPackageDOI + " resolved to null item");
            }
            if(object.getType() != Constants.ITEM) {
                throw new ApproveRejectReviewItemException("DOI " + dataPackageDOI + " resolved to a non item DSpace Object");
            }
            wfi = WorkflowItem.findByItemId(c, object.getID());
            reviewItem(c, approved, wfi);
        } catch (SQLException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (IdentifierNotFoundException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (IdentifierNotResolvableException ex) {
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

    public static void reviewItem(Boolean approved, String manuscriptNumber) throws ApproveRejectReviewItemException  {
        WorkflowItem wfi = null;
        Context c = null;
        try {
            c = new Context();
            c.turnOffAuthorisationSystem();
            List<DSpaceObject> manuscriptItems =
                    getSearchService().search(c, "dc.identifier.manuscriptNumber: " + manuscriptNumber, 0, 2, false);
            if(manuscriptItems.size() > 0){
                wfi = WorkflowItem.findByItemId(c, manuscriptItems.get(0).getID());
                reviewItem(c, approved, wfi);
            } else {
                throw new ApproveRejectReviewItemException("No item found with manuscript number: " + manuscriptNumber);
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

    public static void reviewItem(Boolean approved, Integer workflowItemId) throws ApproveRejectReviewItemException {
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
        } catch (WorkflowConfigurationException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (IOException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (MessagingException ex) {
            throw new ApproveRejectReviewItemException(ex);
        } catch (WorkflowException ex) {
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

    private static void reviewItem(Context c, Boolean approved, WorkflowItem wfi) throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException, MessagingException, WorkflowException, ApproveRejectReviewItemException {
	// get a List of ClaimedTasks, using the WorkflowItem
        List<ClaimedTask> claimedTasks = null;

        if(wfi != null) {
            claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
	}

        //Check for a valid task
        // There must be a claimed actions & it must be in the review stage, else it isn't a valid workflowitem
        if(claimedTasks == null || claimedTasks.isEmpty() || !claimedTasks.get(0).getActionID().equals("reviewAction")){
            throw new ApproveRejectReviewItemException("Item not found or not in review");
        } else {
            ClaimedTask claimedTask = claimedTasks.get(0);
            Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
            WorkflowActionConfig actionConfig = workflow.getStep(claimedTask.getStepID()).getActionConfig(claimedTask.getActionID());

            wfi.getItem().addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", null, approved.toString());

            WorkflowManager.doState(c, c.getCurrentUser(), null, claimedTask.getWorkflowItemID(), workflow, actionConfig);

        }
    }

    private static IdentifierService getIdentifierService() {
        DSpace dspace = new DSpace();
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
        return manager.getServiceByName(IdentifierService.class.getName(), IdentifierService.class);
    }

    private static SearchService getSearchService() {
        DSpace dspace = new DSpace();
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

}
