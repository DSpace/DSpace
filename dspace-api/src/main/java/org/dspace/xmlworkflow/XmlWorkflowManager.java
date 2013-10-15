/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.usage.UsageWorkflowEvent;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.*;
import org.dspace.xmlworkflow.storedcomponents.*;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * When an item is submitted and is somewhere in a workflow, it has a row in the
 * WorkflowItem table pointing to it.
 *
 * Once the item has completed the workflow it will be archived
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XmlWorkflowManager {

    /* support for 'no notification' */
    private static Map<Integer, Boolean> noEMail = new HashMap<Integer, Boolean>();

    private static Logger log = Logger.getLogger(XmlWorkflowManager.class);


    public static XmlWorkflowItem start(Context context, WorkspaceItem wsi) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException, MessagingException, WorkflowException {
        Item myitem = wsi.getItem();
        Collection collection = wsi.getCollection();
        Workflow wf = WorkflowFactory.getWorkflow(collection);

        XmlWorkflowItem wfi = XmlWorkflowItem.create(context);
        wfi.setItem(myitem);
        wfi.setCollection(wsi.getCollection());
        wfi.setMultipleFiles(wsi.hasMultipleFiles());
        wfi.setMultipleTitles(wsi.hasMultipleTitles());
        wfi.setPublishedBefore(wsi.isPublishedBefore());
        wfi.update();
        removeUserItemPolicies(context, myitem, myitem.getSubmitter());
        grantSubmitterReadPolicies(context, myitem);

        context.turnOffAuthorisationSystem();
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
        context.restoreAuthSystemState();
        return wfi;
    }

    //TODO: this is currently not used in our notifications. Look at the code used by the original WorkflowManager
    /**
     * startWithoutNotify() starts the workflow normally, but disables
     * notifications (useful for large imports,) for the first workflow step -
     * subsequent notifications happen normally
     */
    public static XmlWorkflowItem startWithoutNotify(Context c, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException, WorkflowException, WorkflowConfigurationException, MessagingException {
        // make a hash table entry with item ID for no notify
        // notify code checks no notify hash for item id
        noEMail.put(wsi.getItem().getID(), Boolean.TRUE);

        return start(c, wsi);
    }

    public static void alertUsersOnTaskActivation(Context c, XmlWorkflowItem wfi, String emailTemplate, List<EPerson> epa, String ...arguments) throws IOException, SQLException, MessagingException {
        if (noEMail.containsKey(wfi.getItem().getID())) {
            // suppress email, and delete key
            noEMail.remove(wfi.getItem().getID());
        } else {
            Email mail = Email.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), emailTemplate));
            for (String argument : arguments) {
                mail.addArgument(argument);
            }
            for (EPerson anEpa : epa) {
                mail.addRecipient(anEpa.getEmail());
            }

            mail.send();
        }
    }

    private static void grantSubmitterReadPolicies(Context context, Item item) throws SQLException, AuthorizeException {
              //A list of policies the user has for this item
        List<Integer>  userHasPolicies = new ArrayList<Integer>();
        List<ResourcePolicy> itempols = AuthorizeManager.getPolicies(context, item);
        EPerson submitter = item.getSubmitter();
        for (ResourcePolicy resourcePolicy : itempols) {
            if(resourcePolicy.getEPersonID() == submitter.getID()){
                //The user has already got this policy so add it to the list
                userHasPolicies.add(resourcePolicy.getAction());
            }
        }
        //Make sure we don't add duplicate policies
        if(!userHasPolicies.contains(Constants.READ))
            addPolicyToItem(context, item, Constants.READ, submitter);
    }


    private static void activateFirstStep(Context context, Workflow wf, Step firstStep, XmlWorkflowItem wfi) throws AuthorizeException, IOException, SQLException, WorkflowException, WorkflowConfigurationException{
        WorkflowActionConfig firstActionConfig = firstStep.getUserSelectionMethod();
        firstActionConfig.getProcessingAction().activate(context, wfi);
        log.info(LogManager.getHeader(context, "start_workflow", firstActionConfig.getProcessingAction() + " workflow_item_id="
                + wfi.getID() + "item_id=" + wfi.getItem().getID() + "collection_id="
                + wfi.getCollection().getID()));

        // record the start of the workflow w/provenance message
        recordStart(wfi.getItem(), firstActionConfig.getProcessingAction());

        //Fire an event !
        logWorkflowEvent(context, firstStep.getWorkflow().getID(),  null, null, wfi, null, firstStep, firstActionConfig);

        //If we don't have a UI activate it
        if(!firstActionConfig.requiresUI()){
            ActionResult outcome = firstActionConfig.getProcessingAction().execute(context, wfi, firstStep, null);
            processOutcome(context, null, wf, firstStep, firstActionConfig, outcome, wfi, true);
        }
    }

    /*
     * Executes an action and returns the next.
     */
    public static WorkflowActionConfig doState(Context c, EPerson user, HttpServletRequest request, int workflowItemId, Workflow workflow, WorkflowActionConfig currentActionConfig) throws SQLException, AuthorizeException, IOException, MessagingException, WorkflowConfigurationException, WorkflowException {
        try {
            XmlWorkflowItem wi = XmlWorkflowItem.find(c, workflowItemId);
            Step currentStep = currentActionConfig.getStep();
            if(currentActionConfig.getProcessingAction().isAuthorized(c, request, wi)){
                ActionResult outcome = currentActionConfig.getProcessingAction().execute(c, wi, currentStep, request);
                return processOutcome(c, user, workflow, currentStep, currentActionConfig, outcome, wi, false);
            }else{
                throw new AuthorizeException("You are not allowed to to perform this task.");
            }
        } catch (WorkflowConfigurationException e) {
            log.error(LogManager.getHeader(c, "error while executing state", "workflow:  " + workflow.getID() + " action: " + currentActionConfig.getId() + " workflowItemId: " + workflowItemId), e);
            WorkflowUtils.sendAlert(request, e);
            throw e;
        }
    }


    public static WorkflowActionConfig processOutcome(Context c, EPerson user, Workflow workflow, Step currentStep, WorkflowActionConfig currentActionConfig, ActionResult currentOutcome, XmlWorkflowItem wfi, boolean enteredNewStep) throws IOException, WorkflowConfigurationException, AuthorizeException, SQLException, WorkflowException {
        if(currentOutcome.getType() == ActionResult.TYPE.TYPE_PAGE || currentOutcome.getType() == ActionResult.TYPE.TYPE_ERROR){
            //Our outcome is a page or an error, so return our current action
            c.restoreAuthSystemState();
            return currentActionConfig;
        }else
        if(currentOutcome.getType() == ActionResult.TYPE.TYPE_CANCEL || currentOutcome.getType() == ActionResult.TYPE.TYPE_SUBMISSION_PAGE){
            //We either pressed the cancel button or got an order to return to the submission page, so don't return an action
            //By not returning an action we ensure ourselfs that we go back to the submission page
            c.restoreAuthSystemState();
            return null;
        }else
        if (currentOutcome.getType() == ActionResult.TYPE.TYPE_OUTCOME) {
            Step nextStep = null;
            WorkflowActionConfig nextActionConfig = null;
            try {
                //We have completed our action search & retrieve the next action
                if(currentOutcome.getResult() == ActionResult.OUTCOME_COMPLETE){
                    nextActionConfig = currentStep.getNextAction(currentActionConfig);
                }

                if (nextActionConfig != null) {
                    //We remain in the current step since an action is found
                    nextStep = currentStep;
                    nextActionConfig.getProcessingAction().activate(c, wfi);
                    if (nextActionConfig.requiresUI() && !enteredNewStep) {
                        createOwnedTask(c, wfi, currentStep, nextActionConfig, user);
                        return nextActionConfig;
                    } else if( nextActionConfig.requiresUI() && enteredNewStep){
                        //We have entered a new step and have encountered a UI, return null since the current user doesn't have anything to do with this
                        c.restoreAuthSystemState();
                        return null;
                    } else {
                        ActionResult newOutcome = nextActionConfig.getProcessingAction().execute(c, wfi, currentStep, null);
                        return processOutcome(c, user, workflow, currentStep, nextActionConfig, newOutcome, wfi, enteredNewStep);
                    }
                }else
                if(enteredNewStep){
                    // If the user finished his/her step, we keep processing until there is a UI step action or no step at all
                    nextStep = workflow.getNextStep(c, wfi, currentStep, currentOutcome.getResult());
                    c.turnOffAuthorisationSystem();
                    nextActionConfig = processNextStep(c, user, workflow, currentOutcome, wfi, nextStep);
                    //If we require a user interface return null so that the user is redirected to the "submissions page"
                    if(nextActionConfig == null || nextActionConfig.requiresUI()){
                        return null;
                    }else{
                        return nextActionConfig;
                    }
                } else {
                    ClaimedTask task = ClaimedTask.findByWorkflowIdAndEPerson(c, wfi.getID(), user.getID());

                    //Check if we have a task for this action (might not be the case with automatic steps)
                    //First add it to our list of finished users, since no more actions remain
                    WorkflowRequirementsManager.addFinishedUser(c, wfi, user);
                    c.turnOffAuthorisationSystem();
                    //Check if our requirements have been met
                    if((currentStep.isFinished(c, wfi) && currentOutcome.getResult() == ActionResult.OUTCOME_COMPLETE) || currentOutcome.getResult() != ActionResult.OUTCOME_COMPLETE){
                        //Delete all the table rows containing the users who performed this task
                        WorkflowRequirementsManager.clearInProgressUsers(c, wfi);
                        //Remove all the tasks
                        XmlWorkflowManager.deleteAllTasks(c, wfi);


                        nextStep = workflow.getNextStep(c, wfi, currentStep, currentOutcome.getResult());

                        nextActionConfig = processNextStep(c, user, workflow, currentOutcome, wfi, nextStep);
                        //If we require a user interface return null so that the user is redirected to the "submissions page"
                        if(nextActionConfig == null || nextActionConfig.requiresUI()){
                            return null;
                        }else{
                            return nextActionConfig;
                        }
                    }else{
                        //We are done with our actions so go to the submissions page but remove action ClaimedAction first
                        deleteClaimedTask(c, wfi, task);
                        c.restoreAuthSystemState();
                        nextStep = currentStep;
                        nextActionConfig = currentActionConfig;
                        return null;
                    }
                }
            }catch (Exception e){
                log.error("error while processing workflow outcome", e);
                e.printStackTrace();
            }
            finally {
                if((nextStep != null && nextActionConfig != null) || wfi.getItem().isArchived()){
                    logWorkflowEvent(c, currentStep.getWorkflow().getID(), currentStep.getId(), currentActionConfig.getId(), wfi, user, nextStep, nextActionConfig);
                }
            }

        }

        log.error(LogManager.getHeader(c, "Invalid step outcome", "Workflow item id: " + wfi.getID()));
        throw new WorkflowException("Invalid step outcome");
    }

    protected static void logWorkflowEvent(Context c, String workflowId, String previousStepId, String previousActionConfigId, XmlWorkflowItem wfi, EPerson actor, Step newStep, WorkflowActionConfig newActionConfig) throws SQLException {
        try {
            //Fire an event so we can log our action !
            Item item = wfi.getItem();
            Collection myCollection = wfi.getCollection();
            String workflowStepString = null;

            List<EPerson> currentEpersonOwners = new ArrayList<EPerson>();
            List<Group> currentGroupOwners = new ArrayList<Group>();
            //These are only null if our item is sent back to the submission
            if(newStep != null && newActionConfig != null){
                workflowStepString = workflowId + "." + newStep.getId() + "." + newActionConfig.getId();

                //Retrieve the current owners of the task
                List<ClaimedTask> claimedTasks = ClaimedTask.find(c, wfi.getID(), newStep.getId());
                List<PoolTask> pooledTasks = PoolTask.find(c, wfi);
                for (PoolTask poolTask : pooledTasks){
                    if(poolTask.getEpersonID() != -1){
                        currentEpersonOwners.add(EPerson.find(c, poolTask.getEpersonID()));
                    }else{
                        currentGroupOwners.add(Group.find(c, poolTask.getGroupID()));
                    }
                }
                for (ClaimedTask claimedTask : claimedTasks) {
                    currentEpersonOwners.add(EPerson.find(c, claimedTask.getOwnerID()));
                }
            }
            String previousWorkflowStepString = null;
            if(previousStepId != null && previousActionConfigId != null){
                previousWorkflowStepString = workflowId + "." + previousStepId + "." + previousActionConfigId;
            }

            //Fire our usage event !
            UsageWorkflowEvent usageWorkflowEvent = new UsageWorkflowEvent(c, item, wfi, workflowStepString, previousWorkflowStepString, myCollection, actor);

            usageWorkflowEvent.setEpersonOwners(currentEpersonOwners.toArray(new EPerson[currentEpersonOwners.size()]));
            usageWorkflowEvent.setGroupOwners(currentGroupOwners.toArray(new Group[currentGroupOwners.size()]));

            new DSpace().getEventService().fireEvent(usageWorkflowEvent);
        } catch (Exception e) {
            //Catch all errors we do not want our workflow to crash because the logging threw an exception
            log.error(LogManager.getHeader(c, "Error while logging workflow event", "Workflow Item: " + wfi.getID()), e);
        }
    }

    private static WorkflowActionConfig processNextStep(Context c, EPerson user, Workflow workflow, ActionResult currentOutcome, XmlWorkflowItem wfi, Step nextStep) throws SQLException, IOException, AuthorizeException, WorkflowException, WorkflowConfigurationException {
        WorkflowActionConfig nextActionConfig;
        if(nextStep!=null){
            nextActionConfig = nextStep.getUserSelectionMethod();
            nextActionConfig.getProcessingAction().activate(c, wfi);
//                nextActionConfig.getProcessingAction().generateTasks();

            if (nextActionConfig.requiresUI()) {
                //Since a new step has been started, stop executing actions once one with a user interface is present.
                c.restoreAuthSystemState();
                return nextActionConfig;
            } else {
                ActionResult newOutcome = nextActionConfig.getProcessingAction().execute(c, wfi, nextStep, null);
                c.restoreAuthSystemState();
                return processOutcome(c, user, workflow, nextStep, nextActionConfig, newOutcome, wfi, true);
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
    }


    /**
     * Commit the contained item to the main archive. The item is associated
     * with the relevant collection, added to the search index, and any other
     * tasks such as assigning dates are performed.
     *
     * @return the fully archived item.
     */
    public static Item archive(Context c, XmlWorkflowItem wfi)
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

        //Clear any remaining workflow metadata
        item.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, Item.ANY, Item.ANY, Item.ANY);
        item.update();

        // Log the event
        log.info(LogManager.getHeader(c, "install_item", "workflow_item_id="
                + wfi.getID() + ", item_id=" + item.getID() + "handle=FIXME"));

        return item;
    }

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
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_archive"));

            // Get the item handle to email to user
            String handle = HandleManager.findHandle(c, i);

            // Get title
            DCValue[] titles = i.getMetadata(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
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
    public static void deleteAllTasks(Context c, XmlWorkflowItem wi) throws SQLException, AuthorizeException {
        deleteAllPooledTasks(c, wi);

        List<ClaimedTask> allClaimedTasks = ClaimedTask.findByWorkflowId(c,wi.getID());
        for(ClaimedTask task: allClaimedTasks){
            deleteClaimedTask(c, wi, task);
        }
    }

    public static void deleteAllPooledTasks(Context c, XmlWorkflowItem wi) throws SQLException, AuthorizeException {
        List<PoolTask> allPooledTasks = PoolTask.find(c, wi);
        for (PoolTask poolTask : allPooledTasks) {
            deletePooledTask(c, wi, poolTask);
        }
    }

    /*
     * Deletes an eperson from the taskpool of a step
     */
    public static void deletePooledTask(Context c, XmlWorkflowItem wi, PoolTask task) throws SQLException, AuthorizeException {
        if(task != null){
            task.delete();
            if(task.getEpersonID()>0){
                removeUserItemPolicies(c, wi.getItem(), EPerson.find(c, task.getEpersonID()));
            }else{
                removeGroupItemPolicies(c, wi.getItem(), Group.find(c, task.getGroupID()));
            }
        }
    }

    public static void deleteClaimedTask(Context c, XmlWorkflowItem wi, ClaimedTask task) throws SQLException, AuthorizeException {
        if(task != null){
            task.delete();
            removeUserItemPolicies(c, wi.getItem(), EPerson.find(c, task.getOwnerID()));
        }
    }

    /*
     * Creates a task pool for a given step
     */
    public static void createPoolTasks(Context context, XmlWorkflowItem wi, RoleMembers assignees, Step step, WorkflowActionConfig action)
            throws SQLException, AuthorizeException {
        // create a tasklist entry for each eperson
        for (EPerson anEpa : assignees.getEPersons()) {
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
        for(Group group: assignees.getGroups()){
            PoolTask task = PoolTask.create(context);
            task.setStepID(step.getId());
            task.setWorkflowID(step.getWorkflow().getID());
            task.setGroupID(group.getID());
            task.setActionID(action.getId());
            task.setWorkflowItemID(wi.getID());
            task.update();
            //Make sure this user has a task
            grantGroupAllItemPolicies(context, wi.getItem(), group);
        }
    }

    /*
     * Claims an action for a given eperson
     */
    public static void createOwnedTask(Context c, XmlWorkflowItem wi, Step step, WorkflowActionConfig action, EPerson e) throws SQLException, AuthorizeException {
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
        if(epa != null){
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
            if(!userHasPolicies.contains(Constants.REMOVE))
                addPolicyToItem(context, item, Constants.REMOVE, epa);
        }
    }

    private static void grantGroupAllItemPolicies(Context context, Item item, Group group) throws AuthorizeException, SQLException {
        if(group != null){
            //A list of policies the user has for this item
            List<Integer>  groupHasPolicies = new ArrayList<Integer>();
            List<ResourcePolicy> itempols = AuthorizeManager.getPolicies(context, item);
            for (ResourcePolicy resourcePolicy : itempols) {
                if(resourcePolicy.getGroupID() == group.getID()){
                    //The user has already got this policy so it it to the list
                    groupHasPolicies.add(resourcePolicy.getAction());
                }
            }
            //Make sure we don't add duplicate policies
            if(!groupHasPolicies.contains(Constants.READ))
                addGroupPolicyToItem(context, item, Constants.READ, group);
            if(!groupHasPolicies.contains(Constants.WRITE))
                addGroupPolicyToItem(context, item, Constants.WRITE, group);
            if(!groupHasPolicies.contains(Constants.DELETE))
                addGroupPolicyToItem(context, item, Constants.DELETE, group);
            if(!groupHasPolicies.contains(Constants.ADD))
                addGroupPolicyToItem(context, item, Constants.ADD, group);
            if(!groupHasPolicies.contains(Constants.REMOVE))
                addGroupPolicyToItem(context, item, Constants.REMOVE, group);
        }
    }

    private static void addPolicyToItem(Context context, Item item, int type, EPerson epa) throws AuthorizeException, SQLException {
        if(epa != null){
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
    }
    private static void addGroupPolicyToItem(Context context, Item item, int type, Group group) throws AuthorizeException, SQLException {
        if(group != null){
            AuthorizeManager.addPolicy(context ,item, type, group);
            Bundle[] bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                AuthorizeManager.addPolicy(context ,bundle, type, group);
                Bitstream[] bits = bundle.getBitstreams();
                for (Bitstream bit : bits) {
                    AuthorizeManager.addPolicy(context, bit, type, group);
                }
            }
        }
    }

    private static void removeUserItemPolicies(Context context, Item item, EPerson e) throws SQLException, AuthorizeException {
        if(e != null){
            //Also remove any lingering authorizations from this user
            AuthorizeManager.removeEPersonPolicies(context, item, e);
            //Remove the bundle rights
            Bundle[] bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                AuthorizeManager.removeEPersonPolicies(context, bundle, e);
                Bitstream[] bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    AuthorizeManager.removeEPersonPolicies(context, bitstream, e);
                }
            }
            // Ensure that the submitter always retains his resource policies
            if(e.getID() == item.getSubmitter().getID()){
                grantSubmitterReadPolicies(context, item);
            }
        }
    }


    private static void removeGroupItemPolicies(Context context, Item item, Group e) throws SQLException, AuthorizeException {
        if(e != null){
            //Also remove any lingering authorizations from this user
            AuthorizeManager.removeGroupPolicies(context, item, e);
            //Remove the bundle rights
            Bundle[] bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                AuthorizeManager.removeGroupPolicies(context, bundle, e);
                Bitstream[] bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    AuthorizeManager.removeGroupPolicies(context, bitstream, e);
                }
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
     * @param provenance the provenance message
    * @param rejection_message
     *            message to email to user (if null no email is sent)
     * @return the workspace item that is created
     * @throws java.io.IOException ...
     * @throws java.sql.SQLException ...
     * @throws org.dspace.authorize.AuthorizeException ...
     */
    public static WorkspaceItem sendWorkflowItemBackSubmission(Context c, XmlWorkflowItem wi, EPerson e, String provenance,
            String rejection_message) throws SQLException, AuthorizeException,
            IOException
    {

        String workflowID = null;
        String currentStepId = null;
        String currentActionConfigId = null;
        ClaimedTask claimedTask = ClaimedTask.findByWorkflowIdAndEPerson(c, wi.getID(), e.getID());
        if(claimedTask != null){
            //Log it
            workflowID = claimedTask.getWorkflowID();
            currentStepId = claimedTask.getStepID();
            currentActionConfigId = claimedTask.getActionID();
        }

        // authorize a DSpaceActions.REJECT
        // stop workflow
        deleteAllTasks(c, wi);

        c.turnOffAuthorisationSystem();
        //Also clear all info for this step
        WorkflowRequirementsManager.clearInProgressUsers(c, wi);

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
        String provDescription = provenance + " Rejected by " + usersName + ", reason: "
                + rejection_message + " on " + now + " (GMT) ";

        // Add to item as a DC field
        myitem.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);

        //Clear any workflow schema related metadata
        myitem.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, Item.ANY, Item.ANY, Item.ANY);

        myitem.update();

        //Restore permissions for the submitter
        grantUserAllItemPolicies(c, myitem, myitem.getSubmitter());
        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);

        // notify that it's been rejected
        notifyOfReject(c, wi, e, rejection_message);
        log.info(LogManager.getHeader(c, "reject_workflow", "workflow_item_id="
                + wi.getID() + "item_id=" + wi.getItem().getID()
                + "collection_id=" + wi.getCollection().getID() + "eperson_id="
                + e.getID()));

        logWorkflowEvent(c, workflowID, currentStepId, currentActionConfigId, wi, e, null, null);

        c.restoreAuthSystemState();
        return wsi;
    }

    public static WorkspaceItem abort(Context c, XmlWorkflowItem wi, EPerson e) throws AuthorizeException, SQLException, IOException {
        if (!AuthorizeManager.isAdmin(c))
        {
            throw new AuthorizeException(
                    "You must be an admin to abort a workflow");
        }

        deleteAllTasks(c, wi);

        c.turnOffAuthorisationSystem();
        //Also clear all info for this step
        WorkflowRequirementsManager.clearInProgressUsers(c, wi);

        // Remove (if any) the workflowItemroles for this item
        WorkflowItemRole[] workflowItemRoles = WorkflowItemRole.findAllForItem(c, wi.getID());
        for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
            workflowItemRole.delete();
        }

        //Restore permissions for the submitter
        Item item = wi.getItem();
        grantUserAllItemPolicies(c, item, item.getSubmitter());
        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);

        log.info(LogManager.getHeader(c, "abort_workflow", "workflow_item_id="
                + wi.getID() + "item_id=" + item.getID()
                + "collection_id=" + wi.getCollection().getID() + "eperson_id="
                + e.getID()));


        c.restoreAuthSystemState();
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
    private static WorkspaceItem returnToWorkspace(Context c, XmlWorkflowItem wfi)
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
                "DELETE FROM cwf_workflowitem WHERE workflowitem_id=" + wfi.getID());

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

    private static void notifyOfReject(Context c, XmlWorkflowItem wi, EPerson e,
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
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale,"submit_reject"));

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
