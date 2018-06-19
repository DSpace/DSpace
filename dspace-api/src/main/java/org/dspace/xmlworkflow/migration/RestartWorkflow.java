/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.migration;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import java.util.List;
import java.util.UUID;

/**
 * A utility class that will send all the worklfow items
 * back to their submitters
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class RestartWorkflow {

    /**
     * log4j category
     */
    private static Logger log = Logger.getLogger(RestartWorkflow.class);

    public static boolean useWorkflowSendEmail = false;

    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    public static void main(String[] args) {
        try {
            System.out.println("All workflowitems will be sent back to the first workflow step.");
            Context context = new Context();
            context.turnOffAuthorisationSystem();
            // create an options object and populate it
            CommandLineParser parser = new PosixParser();

            Options options = new Options();
            options.addOption("e", "eperson", true,
                    "email of eperson doing importing");
            options.addOption("n", "notify", false,
                    "if sending submissions through the workflow, send notification emails");
            options.addOption("p", "provenance", true,
                    "the provenance description to be added to the item");
            options.addOption("h", "help", false, "help");

            CommandLine line = parser.parse(options, args);

            String eperson = null; // db ID or email

            if (line.hasOption('h')) {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("RestartWorkflow\n", options);
                System.exit(0);
            }
            if (line.hasOption('n')) {
                useWorkflowSendEmail = true;
            }
            if (line.hasOption('e')) // eperson
            {
                eperson = line.getOptionValue('e');
            }else{
                System.out.println("The -e (eperson) option is mandatory !");
                System.exit(1);
            }

            // find the EPerson, assign to context
            EPerson myEPerson = null;

            if (eperson.indexOf('@') != -1) {
                // @ sign, must be an email
                myEPerson = ePersonService.findByEmail(context, eperson);
            } else {
                myEPerson = ePersonService.find(context, UUID.fromString(eperson));
            }

            if (myEPerson == null) {
                System.out.println("Error, eperson cannot be found: " + eperson);
                System.exit(1);
            }

            String provenance = null;
            if(line.hasOption('p')){
                provenance = line.getOptionValue('p');
            }

            context.setCurrentUser(myEPerson);

            System.out.println("Sending all workflow items back to the workspace");


            WorkflowServiceFactory workflowServiceFactory = WorkflowServiceFactory.getInstance();
            List<WorkflowItem> workflowItems = workflowServiceFactory.getWorkflowItemService().findAll(context);
            WorkflowService workflowService = workflowServiceFactory.getWorkflowService();
            int i = 0;
            for (WorkflowItem workflowItem : workflowItems) {
                System.out.println("Processing workflow item " + i + " of " + workflowItems.size());
                System.out.println("Removing pooled tasks");

                // rejection provenance
                Item myitem = workflowItem.getItem();

                // Get current date
//                String now = DCDate.getCurrent().toString();

                // Add to item as a DC field

//            TaskLog tasklog = TaskLog.create(c, wi);
//            tasklog.update();

                // convert into personal workspace
                WorkspaceItem wsi = workflowService.sendWorkflowItemBackSubmission(context, workflowItem, myEPerson, provenance, "");

                log.info(LogManager.getHeader(context, "restart_workflow", "workflow_item_id="
                        + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                        + "collection_id=" + workflowItem.getCollection().getID()));


                if (useWorkflowSendEmail) {
                    workflowService.start(context, wsi);
                } else {
                    workflowService.startWithoutNotify(context, wsi);
                }
                i++;
            }

            System.out.println("All done, committing context");
            context.complete();
            System.exit(0);
        } catch (Exception e) {
            log.error("Error while sending all workflow items back to the workspace", e);
            e.printStackTrace();
        }
    }
}
