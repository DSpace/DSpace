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
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.XmlWorkflowManager;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;

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
                myEPerson = EPerson.findByEmail(context, eperson);
            } else {
                myEPerson = EPerson.find(context, Integer.parseInt(eperson));
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


            WorkflowItem[] workflowItems = WorkflowItem.findAll(context);
            int i = 0;
            for (WorkflowItem workflowItem : workflowItems) {
                System.out.println("Processing workflow item " + i + " of " + workflowItems.length);
                System.out.println("Removing pooled tasks");
                deleteTasks(context, workflowItem);

                // rejection provenance
                Item myitem = workflowItem.getItem();

                // Get current date
//                String now = DCDate.getCurrent().toString();

                // Add to item as a DC field
                if(provenance != null){
                    myitem.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provenance);
                }
                myitem.update();

//            TaskLog tasklog = TaskLog.create(c, wi);
//            tasklog.update();

                // convert into personal workspace
                WorkspaceItem wsi = returnToWorkspace(context, workflowItem);

                log.info(LogManager.getHeader(context, "restart_workflow", "workflow_item_id="
                        + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                        + "collection_id=" + workflowItem.getCollection().getID()));


                if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow")) {
                    if (useWorkflowSendEmail) {
                        XmlWorkflowManager.start(context, wsi);
                    } else {
                        XmlWorkflowManager.startWithoutNotify(context, wsi);
                    }
                } else {
                    if (useWorkflowSendEmail) {
                        WorkflowManager.start(context, wsi);
                    } else {
                        WorkflowManager.startWithoutNotify(context, wsi);
                    }
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


    // deletes all tasks associated with a workflowitem

    static void deleteTasks(Context c, WorkflowItem wi) throws SQLException {
        String myrequest = "DELETE FROM TaskListItem WHERE workflow_id= ? ";

        DatabaseManager.updateQuery(c, myrequest, wi.getID());
    }

    /**
     * Return the workflow item to the workspace of the submitter. The workflow
     * item is removed, and a workspace item created.
     *
     * @param c   Context
     * @param wfi WorkflowItem to be 'dismantled'
     * @return the workspace item
     */
    private static WorkspaceItem returnToWorkspace(Context c, WorkflowItem wfi)
            throws SQLException, IOException, AuthorizeException {
        Item myitem = wfi.getItem();
        Collection mycollection = wfi.getCollection();


        EPerson submitter = wfi.getSubmitter();
        //Remove all the authorizations & give the submitter the right rights
        AuthorizeManager.removeAllPolicies(c, myitem);
        AuthorizeManager.addPolicy(c, myitem, Constants.READ, submitter);

        // FIXME: How should this interact with the workflow system?
        // FIXME: Remove license
        // FIXME: Provenance statement?
        // Create the new workspace item row
        TableRow row = DatabaseManager.row("workspaceitem");
        row.setColumn("item_id", myitem.getID());
        row.setColumn("collection_id", mycollection.getID());
        DatabaseManager.insert(c, row);

        int wsi_id = row.getIntColumn("workspace_item_id");
        WorkspaceItem wi = WorkspaceItem.find(c, wsi_id);
        wi.setMultipleFiles(wfi.hasMultipleFiles());
        wi.setMultipleTitles(wfi.hasMultipleTitles());
        wi.setPublishedBefore(wfi.isPublishedBefore());
        wi.update();

        //myitem.update();
        log.info(LogManager.getHeader(c, "return_to_workspace",
                "workflow_item_id=" + wfi.getID() + "workspace_item_id="
                        + wi.getID()));

        // Now remove the workflow object manually from the database
        DatabaseManager.updateQuery(c,
                "DELETE FROM WorkflowItem WHERE workflow_id=" + wfi.getID());

        return wi;
    }
}
