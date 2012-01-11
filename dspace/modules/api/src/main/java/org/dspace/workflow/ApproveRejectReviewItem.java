package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.xml.sax.SAXException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 19-aug-2010
 * Time: 14:25:02
 *
 * An executable class which determines if an item is to be rejected or accepted,
 * when accepted the submission is sent to the next step
 */
public class ApproveRejectReviewItem {

    public static void main(String[] args) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException, TransformerException, SAXException, ParserConfigurationException, WorkflowException, MessagingException, ParseException {
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

        Context c = new Context();
        c.turnOffAuthorisationSystem();

	WorkflowItem wfi = null;
        if(line.hasOption('m')){
	    // get a WorkflowItem using a manuscript number
	    String manuScriptNumber;
	    manuScriptNumber = line.getOptionValue('m');
	    List<DSpaceObject> manuscriptItems =
		getSearchService().search(c, "dc.identifier.manuscriptNumber: " + manuScriptNumber, 0, 2, false);
	    if(0 < manuscriptItems.size()){
		wfi = WorkflowItem.findByItemId(c, manuscriptItems.get(0).getID());
	    } else {
		System.out.println("No items found with the matching manuscript number.");
	    }
	} else if(line.hasOption('i')) {
	    // get a WorkflowItem using a workflow ID
	    int wfItemId = Integer.parseInt(line.getOptionValue('i'));
	    wfi = WorkflowItem.find(c, wfItemId);
	} else {
            System.out.println("No manuscript number or workflow ID was given. One of these must be provided to identify the correct item in the review stage.");
            System.exit(1);
            return;
        }
	
	// get a List of ClaimedTasks, using the WorkflowItem
        List<ClaimedTask> claimedTasks = null;

        if(wfi != null) {
            claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
	}

	
        //Check for a valid task
        // There must be a claimed actions & it must be in the review stage, else it isn't a valid workflowitem
        if(claimedTasks == null || claimedTasks.size() == 0 || !claimedTasks.get(0).getActionID().equals("reviewAction")){
            System.out.println("Invalid manuscript number");
            System.exit(1);
        }else{
            ClaimedTask claimedTask = claimedTasks.get(0);
            Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
            WorkflowActionConfig actionConfig = workflow.getStep(claimedTask.getStepID()).getActionConfig(claimedTask.getActionID());

            wfi.getItem().addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", null, approved.toString());

            WorkflowManager.doState(c, c.getCurrentUser(), null, claimedTask.getWorkflowItemID(), workflow, actionConfig);

            //Now make sure that any hanging workspace items get removed
            /*
            WorkspaceItem[] workspaceItems = WorkspaceItem.findByEPerson(c, wfi.getSubmitter());
            for (WorkspaceItem workspaceItem : workspaceItems) {
                //Find out which ones are linked to our publication
                Item dataPackage = DryadWorkflowUtils.getDataPackage(c, workspaceItem.getItem());
                if(dataPackage != null && dataPackage.getID() == wfi.getItem().getID())
                {
                    //Remove the workspace item
                    workspaceItem.deleteAll();
                }
            }
            */

        }
        c.commit();
        c.complete();
    }

    private static SearchService getSearchService()
    {
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

}
