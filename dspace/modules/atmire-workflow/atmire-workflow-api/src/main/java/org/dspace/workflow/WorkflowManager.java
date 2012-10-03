package org.dspace.workflow;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.actions.Action;
import org.dspace.workflow.actions.ActionResult;
import org.dspace.workflow.actions.WorkflowActionConfig;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:32:44
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowManager {

    private static Logger log = Logger.getLogger(WorkflowManager.class);

    public static void start(Context context, WorkspaceItem wsi) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException, MessagingException, WorkflowException {
        Item myitem = wsi.getItem();
        Collection collection = wsi.getCollection();
        Workflow wf = WorkflowFactory.getWorkflow(collection);
        TableRow row = DatabaseManager.create(context, "workflowitem");
        row.setColumn("item_id", myitem.getID());
        row.setColumn("collection_id", wsi.getCollection().getID());

        WorkflowItem wfi = new WorkflowItem(context, row);
        wfi.setMultipleFiles(wsi.hasMultipleFiles());
        wfi.setMultipleTitles(wsi.hasMultipleTitles());
        wfi.setPublishedBefore(wsi.isPublishedBefore());
        wfi.update();
        Step firstStep = wf.getFirstStep();
        if(firstStep.isValidStep(context, wfi)){
             activateFirstStep(context, wf, firstStep, wfi);
        } else {
            //Get our next step, if none is found, archive our item
            firstStep = wf.getNextStep(context, wfi, firstStep, ActionResult.OUTCOME_COMPLETE);
            if(firstStep == null){
                archive(context, wfi);
            }else{
                activateFirstStep(context, wf, firstStep, wfi);
            }

        }

        // remove the WorkspaceItem
        wsi.deleteWrapper();
    }

    private static void activateFirstStep(Context context, Workflow wf, Step firstStep, WorkflowItem wfi) throws AuthorizeException, IOException, SQLException, WorkflowException, WorkflowConfigurationException{
        WorkflowActionConfig firstActionConfig = firstStep.getUserSelectionMethod();
        firstActionConfig.getProcessingAction().activate(context, wfi);
        log.info(LogManager.getHeader(context, "start_workflow", firstActionConfig.getProcessingAction() + " workflow_item_id="
                + wfi.getID() + "item_id=" + wfi.getItem().getID() + "collection_id="
                + wfi.getCollection().getID()));

        // record the start of the workflow w/provenance message
        recordStart(wfi.getItem(), firstActionConfig.getProcessingAction());

        //If we don't have a UI activate it
        if(!firstActionConfig.hasUserInterface()){
            ActionResult outcome = firstActionConfig.getProcessingAction().execute(context, wfi, firstStep, null);
            processOutcome(context, null, wf, firstStep, firstActionConfig, outcome, wfi);
        }
    }

    /*
     * Executes an action and returns the next.
     */
    public static WorkflowActionConfig doState(Context c, EPerson user, HttpServletRequest request, int workflowItemId, Workflow workflow, WorkflowActionConfig currentActionConfig) throws SQLException, AuthorizeException, IOException, MessagingException, WorkflowConfigurationException, WorkflowException {
        try {
            WorkflowItem wi = WorkflowItem.find(c, workflowItemId);
            Step currentStep = currentActionConfig.getStep();
            ActionResult outcome = currentActionConfig.getProcessingAction().execute(c, wi, currentStep, request);
            return processOutcome(c, user, workflow, currentStep, currentActionConfig, outcome, wi);
        } catch (WorkflowConfigurationException e) {
            log.error(LogManager.getHeader(c, "error while executing state", "workflow:  " + workflow.getID() + " action: " + currentActionConfig.getId() + " workflowItemId: " + workflowItemId), e);
            WorkflowUtils.sendAlert(request, e);
            throw e;
        }
    }


    public static WorkflowActionConfig processOutcome(Context c, EPerson user, Workflow workflow, Step currentStep, WorkflowActionConfig currentActionConfig, ActionResult currentOutcome, WorkflowItem wfi) throws IOException, WorkflowConfigurationException, AuthorizeException, SQLException, WorkflowException {
        if(currentOutcome.getType() == ActionResult.TYPE.TYPE_PAGE || currentOutcome.getType() == ActionResult.TYPE.TYPE_ERROR){
            //Our outcome is a page or an error, so return our current action
            return currentActionConfig;
        }else
        if(currentOutcome.getType() == ActionResult.TYPE.TYPE_CANCEL || currentOutcome.getType() == ActionResult.TYPE.TYPE_SUBMISSION_PAGE){
            //We either pressed the cancel button or got an order to return to the submission page, so don't return an action
            //By not returning an action we ensure ourselfs that we go back to the submission page
            return null;
        }else
        if (currentOutcome.getType() == ActionResult.TYPE.TYPE_OUTCOME) {
            //We have completed our action search & retrieve the next action
            WorkflowActionConfig nextActionConfig = null;
            if(currentOutcome.getResult() == ActionResult.OUTCOME_COMPLETE){
                nextActionConfig = currentStep.getNextAction(currentActionConfig);
            }

            if (nextActionConfig != null) {
                nextActionConfig.getProcessingAction().activate(c, wfi);
                if (nextActionConfig.hasUserInterface()) {
                    //TODO: if user is null, then throw a decent exception !
                    createOwnedTask(c, wfi, currentStep, nextActionConfig, user);
                    return nextActionConfig;
                } else {
                    ActionResult newOutcome = nextActionConfig.getProcessingAction().execute(c, wfi, currentStep, null);
                    return processOutcome(c, user, workflow, currentStep, nextActionConfig, newOutcome, wfi);
                }
            }else{
                
                //First add it to our list of finished users, since no more actions remain
                WorkflowRequirementsManager.addFinishedUser(c, wfi, user);
                c.turnOffAuthorisationSystem();
                //Check if our requirements have been met
                if((currentStep.isFinished(wfi) && currentOutcome.getResult() == ActionResult.OUTCOME_COMPLETE) || currentOutcome.getResult() != ActionResult.OUTCOME_COMPLETE){
                    //Clear all the metadata that might be saved by this step
                    WorkflowRequirementsManager.clearStepMetadata(wfi);
                    //Remove all the tasks
                    WorkflowManager.deleteAllTasks(c, wfi);


                    Step nextStep = workflow.getNextStep(c, wfi, currentStep, currentOutcome.getResult());
                    if(nextStep!=null){
                        //TODO: is generate tasks nog nodig of kan dit mee in activate?
                        //TODO: vorige step zou meegegeven moeten worden om evt rollen te kunnen overnemen
                        nextActionConfig = nextStep.getUserSelectionMethod();
                        nextActionConfig.getProcessingAction().activate(c, wfi);
        //                nextActionConfig.getProcessingAction().generateTasks();

                        //Deze kunnen afhangen van de step (rol, min, max, ...). Evt verantwoordelijkheid bij userassignmentaction leggen
                        if (nextActionConfig.hasUserInterface()) {
                            //Since a new step has been started, stop executing actions once one with a user interface is present.
                            c.restoreAuthSystemState();
                            return null;
                        } else {
                            ActionResult newOutcome = nextActionConfig.getProcessingAction().execute(c, wfi, nextStep, null);
                            c.restoreAuthSystemState();
                            return processOutcome(c, user, workflow, nextStep, nextActionConfig, newOutcome, wfi);
                        }
                    }else{
                        if(currentOutcome.getResult() != ActionResult.OUTCOME_COMPLETE){
                            c.restoreAuthSystemState();
                            throw new WorkflowException("No alternate step was found for outcome: " + currentOutcome.getResult());
                        }

                        archive(c, wfi);
                        c.restoreAuthSystemState();
                        return null;
                    }
                }else{
                    //We are done with our actions so go to the submissions page
                    c.restoreAuthSystemState();
                    return null;
                }
            }

        }
        //TODO: log & go back to submission, We should not come here
        //TODO: remove assertion - can be used for testing (will throw assertionexception)
        assert false;
        return null;
    }


    //TODO: nakijken

    /**
     * Commit the contained item to the main archive. The item is associated
     * with the relevant collection, added to the search index, and any other
     * tasks such as assigning dates are performed.
     *
     * @return the fully archived item.
     */
    public static Item archive(Context c, WorkflowItem wfi)
            throws SQLException, IOException, AuthorizeException {
        // FIXME: Check auth
        Item item = wfi.getItem();
        Collection collection = wfi.getCollection();

        // Remove (if any) the workflowItemroles for this item
        WorkflowItemRole[] workflowItemRoles = WorkflowItemRole.findAllForItem(c, wfi.getID());
        for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
            workflowItemRole.delete();
        }

        log.info(LogManager.getHeader(c, "archive_item", "workflow_item_id="
                + wfi.getID() + "item_id=" + item.getID() + "collection_id="
                + collection.getID()));

        InstallItem.installItem(c, wfi);

        //Notify
        notifyOfArchive(c, item, collection);
        
        // Log the event
        log.info(LogManager.getHeader(c, "install_item", "workflow_item_id="
                + wfi.getID() + ", item_id=" + item.getID() + "handle=FIXME"));

        return item;
    }
    //TODO: nakijken  - altijd submitter mailen of config optie?
    
    /**
     * notify the submitter that the item is archived
     */
    private static void notifyOfArchive(Context c, Item i, Collection coll)
            throws SQLException, IOException {
        try {
            // Get submitter
            EPerson ep = i.getSubmitter();
            // Get the Locale
            Locale supportedLocale = I18nUtil.getEPersonLocale(ep);
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_archive"));

            // Get the item handle to email to user
            String handle = HandleManager.findHandle(c, i);

            // Get title
            DCValue[] titles = i.getDC("title", null, Item.ANY);
            String title = "";
            try {
                title = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e) {
                title = "Untitled";
            }
            if (titles.length > 0) {
                title = titles[0].value;
            }

            email.addRecipient(ep.getEmail());
            email.addArgument(title);
            email.addArgument(coll.getMetadata("name"));
            email.addArgument(HandleManager.getCanonicalForm(handle));

            email.send();
        }
        catch (MessagingException e) {
            log.warn(LogManager.getHeader(c, "notifyOfArchive",
                    "cannot email user" + " item_id=" + i.getID()));
        }
    }

    /***********************************
     * WORKFLOW TASK MANAGEMENT
     **********************************/
    /**
     * Deletes all tasks from this workflowflowitem
     * @param c the dspace context
     * @param wi the workflow item for whom we are to delete the tasks
     * @throws SQLException ...
     * @throws org.dspace.authorize.AuthorizeException ...
     */
    public static void deleteAllTasks(Context c, WorkflowItem wi) throws SQLException, AuthorizeException {
        deleteAllPooledTasks(c, wi);

        List<ClaimedTask> allClaimedTasks = ClaimedTask.findByWorkflowId(c,wi.getID());
        for(ClaimedTask task: allClaimedTasks){
            deleteClaimedTask(c, wi, task);
        }
    }

    public static void deleteAllPooledTasks(Context c, WorkflowItem wi) throws SQLException, AuthorizeException {
        List<PoolTask> allPooledTasks = PoolTask.find(c, wi);
        for (PoolTask poolTask : allPooledTasks) {
            deletePooledTask(c, wi, poolTask);
        }
    }

    /*
     * Deletes an eperson from the taskpool of a step
     */
    public static void deletePooledTask(Context c, WorkflowItem wi, PoolTask task) throws SQLException, AuthorizeException {
        if(task != null){
            task.delete();
            removeUserItemPolicies(c, wi.getItem(), EPerson.find(c, task.getEpersonID()));
        }
    }

    public static void deleteClaimedTask(Context c, WorkflowItem wi, ClaimedTask task) throws SQLException, AuthorizeException {
        if(task != null){
            task.delete();
            removeUserItemPolicies(c, wi.getItem(), EPerson.find(c, task.getOwnerID()));
        }
        
    }

    /*
     * Creates a task pool for a given step
     */
    public static void createPoolTasks(Context context, WorkflowItem wi, EPerson[] epa, Step step, WorkflowActionConfig action)
            throws SQLException, AuthorizeException {
        // create a tasklist entry for each eperson
        for (EPerson anEpa : epa) {
            PoolTask task = PoolTask.create(context);
            task.setStepID(step.getId());
            task.setWorkflowID(step.getWorkflow().getID());
            task.setEpersonID(anEpa.getID());
            task.setActionID(action.getId());
            task.setWorkflowItemID(wi.getID());

            task.update();
            //Make sure this user has a task
            grantUserAllItemPolicies(context, wi.getItem(), anEpa);
        }
    }

    /*
     * Claims an action for a given eperson
     */
    public static void createOwnedTask(Context c, WorkflowItem wi, Step step, WorkflowActionConfig action, EPerson e) throws SQLException, AuthorizeException {
        ClaimedTask task = ClaimedTask.create(c);
        task.setWorkflowItemID(wi.getID());
        task.setStepID(step.getId());
        task.setActionID(action.getId());
        task.setOwnerID(e.getID());
        task.setWorkflowID(step.getWorkflow().getID());
        task.update();
        //Make sure this user has a task
        grantUserAllItemPolicies(c, wi.getItem(), e);
    }

    private static void grantUserAllItemPolicies(Context context, Item item, EPerson epa) throws AuthorizeException, SQLException {
        //A list of policies the user has for this item
        List<Integer>  userHasPolicies = new ArrayList<Integer>();
        List<ResourcePolicy> itempols = AuthorizeManager.getPolicies(context, item);
        for (ResourcePolicy resourcePolicy : itempols) {
            if(resourcePolicy.getEPersonID() == epa.getID()){
                //The user has already got this policy so it it to the list
                userHasPolicies.add(resourcePolicy.getAction());
            }
        }

        //Make sure we don't add duplicate policies
        if(!userHasPolicies.contains(Constants.READ))
            addPolicyToItem(context, item, Constants.READ, epa);
        if(!userHasPolicies.contains(Constants.WRITE))
            addPolicyToItem(context, item, Constants.WRITE, epa);
        if(!userHasPolicies.contains(Constants.DELETE))
            addPolicyToItem(context, item, Constants.DELETE, epa);
        if(!userHasPolicies.contains(Constants.ADD))
            addPolicyToItem(context, item, Constants.ADD, epa);
    }

    private static void addPolicyToItem(Context context, Item item, int type, EPerson epa) throws AuthorizeException, SQLException {
        AuthorizeManager.addPolicy(context ,item, type, epa);
        Bundle[] bundles = item.getBundles();
        for (Bundle bundle : bundles) {
            AuthorizeManager.addPolicy(context ,bundle, type, epa);
            Bitstream[] bits = bundle.getBitstreams();
            for (Bitstream bit : bits) {
                AuthorizeManager.addPolicy(context, bit, type, epa);
            }

        }
    }

    private static void removeUserItemPolicies(Context context, Item item, EPerson e) throws SQLException, AuthorizeException {
        //Also remove any lingering authorizations from this user
        removePoliciesFromDso(context, item, e);
        //Remove the bundle rights
        Bundle[] bundles = item.getBundles();
        for (Bundle bundle : bundles) {
            removePoliciesFromDso(context, bundle, e);
            Bitstream[] bitstreams = bundle.getBitstreams();
            for (Bitstream bitstream : bitstreams) {
                removePoliciesFromDso(context, bitstream, e);
            }
        }
    }

    private static void removePoliciesFromDso(Context context, DSpaceObject item, EPerson e) throws SQLException, AuthorizeException {
        List<ResourcePolicy> policies = AuthorizeManager.getPolicies(context, item);
        AuthorizeManager.removeAllPolicies(context, item);
        for (ResourcePolicy resourcePolicy : policies) {
            if( resourcePolicy.getEPerson() ==null || resourcePolicy.getEPersonID() != e.getID()){
                if(resourcePolicy.getEPerson() != null)
                    AuthorizeManager.addPolicy(context, item, resourcePolicy.getAction(), resourcePolicy.getEPerson());
                else
                    AuthorizeManager.addPolicy(context, item, resourcePolicy.getAction(), resourcePolicy.getGroup());
            }
        }
    }

   /**
     * rejects an item - rejection means undoing a submit - WorkspaceItem is
     * created, and the WorkflowItem is removed, user is emailed
     * rejection_message.
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem to operate on
     * @param e
     *            EPerson doing the operation
     * @param action the action which triggered this reject
     * @param rejection_message
     *            message to email to user
     * @return the workspace item that is created
     * @throws java.io.IOException ...
     * @throws java.sql.SQLException ...
     * @throws org.dspace.authorize.AuthorizeException ...
     */
    public static WorkspaceItem rejectWorkflowItem(Context c, WorkflowItem wi, EPerson e, Action action,
            String rejection_message) throws SQLException, AuthorizeException,
            IOException
    {
        // authorize a DSpaceActions.REJECT
        // stop workflow
        deleteAllTasks(c, wi);

        //Also clear all info for this step
        WorkflowRequirementsManager.clearStepMetadata(wi);

        // Remove (if any) the workflowItemroles for this item
        WorkflowItemRole[] workflowItemRoles = WorkflowItemRole.findAllForItem(c, wi.getID());
        for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
            workflowItemRole.delete();
        }

        // rejection provenance
        Item myitem = wi.getItem();

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = getEPersonName(e);

        // Here's what happened
        String provDescription = action.getProvenanceStartId() + " Rejected by " + usersName + ", reason: "
                + rejection_message + " on " + now + " (GMT) ";

        // Add to item as a DC field
        myitem.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        myitem.update();

        //TODO: MAKE SURE THAT SUBMITTER GETS PROPER RIGHTS
        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);

        // notify that it's been rejected
        notifyOfReject(c, wi, e, rejection_message);

        log.info(LogManager.getHeader(c, "reject_workflow", "workflow_item_id="
                + wi.getID() + "item_id=" + wi.getItem().getID()
                + "collection_id=" + wi.getCollection().getID() + "eperson_id="
                + e.getID()));

        return wsi;
    }

    /**
     * Return the workflow item to the workspace of the submitter. The workflow
     * item is removed, and a workspace item created.
     *
     * @param c
     *            Context
     * @param wfi
     *            WorkflowItem to be 'dismantled'
     * @return the workspace item
     * @throws java.io.IOException ...
     * @throws java.sql.SQLException ...
     * @throws org.dspace.authorize.AuthorizeException ...
     */
    private static WorkspaceItem returnToWorkspace(Context c, WorkflowItem wfi)
            throws SQLException, IOException, AuthorizeException
    {
        Item myitem = wfi.getItem();
        Collection myCollection = wfi.getCollection();

        // FIXME: How should this interact with the workflow system?
        // FIXME: Remove license
        // FIXME: Provenance statement?
        // Create the new workspace item row
        TableRow row = DatabaseManager.create(c, "workspaceitem");
        row.setColumn("item_id", myitem.getID());
        row.setColumn("collection_id", myCollection.getID());
        DatabaseManager.update(c, row);

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

    public static String getEPersonName(EPerson e) throws SQLException
    {
        String submitter = e.getFullName();

        submitter = submitter + "(" + e.getEmail() + ")";

        return submitter;
    }

    // Create workflow start provenance message
    private static void recordStart(Item myitem, Action action)
            throws SQLException, IOException, AuthorizeException
    {
        // get date
        DCDate now = DCDate.getCurrent();

        // Create provenance description
        String provmessage = "";

        if (myitem.getSubmitter() != null)
        {
            provmessage = "Submitted by " + myitem.getSubmitter().getFullName()
                    + " (" + myitem.getSubmitter().getEmail() + ") on "
                    + now.toString() + " workflow start=" + action.getProvenanceStartId() + "\n";
        }
        else
        // null submitter
        {
            provmessage = "Submitted by unknown (probably automated) on"
                    + now.toString() + " workflow start=" + action.getProvenanceStartId() + "\n";
        }

        // add sizes and checksums of bitstreams
        provmessage += InstallItem.getBitstreamProvenanceMessage(myitem);

        // Add message to the DC
        myitem.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provmessage);
        myitem.update();
    }

    private static void notifyOfReject(Context c, WorkflowItem wi, EPerson e,
        String reason)
    {
        try
        {
            // Get the item title
            String title = wi.getItem().getName();

            // Get the collection
            Collection coll = wi.getCollection();

            // Get rejector's name
            String rejector = getEPersonName(e);
            Locale supportedLocale = I18nUtil.getEPersonLocale(e);
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale,"submit_reject"));

            email.addRecipient(wi.getSubmitter().getEmail());
            email.addArgument(title);
            email.addArgument(coll.getMetadata("name"));
            email.addArgument(rejector);
            email.addArgument(reason);
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/mydspace");

            email.send();
        }
        catch (Exception ex)
        {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + e.getID()
                            + " eperson_email" + e.getEmail()
                            + " workflow_item_id" + wi.getID()));
        }
    }

    public static String getMyDSpaceLink() {
        return ConfigurationManager.getProperty("dspace.url") + "/mydspace";
    }
}
