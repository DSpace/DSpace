/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogHelper;
import org.dspace.curate.service.XmlWorkflowCuratorService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Event;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageWorkflowEvent;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.Action;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When an item is submitted and is somewhere in a workflow, it has a row in the
 * {@code cwf_workflowitem} table pointing to it.
 *
 * Once the item has completed the workflow it will be archived.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XmlWorkflowServiceImpl implements XmlWorkflowService {

    /* support for 'no notification' */
    protected Map<UUID, Boolean> noEMail = new HashMap<>();

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(XmlWorkflowServiceImpl.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected CollectionRoleService collectionRoleService;
    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected InstallItemService installItemService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected PoolTaskService poolTaskService;
    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;
    @Autowired(required = true)
    protected WorkflowRequirementsService workflowRequirementsService;
    @Autowired(required = true)
    protected XmlWorkflowFactory xmlWorkflowFactory;
    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected XmlWorkflowItemService xmlWorkflowItemService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;
    @Autowired(required = true)
    protected XmlWorkflowCuratorService xmlWorkflowCuratorService;

    protected XmlWorkflowServiceImpl() {

    }


    @Override
    public void deleteCollection(Context context, Collection collection)
        throws SQLException, IOException, AuthorizeException {
        xmlWorkflowItemService.deleteByCollection(context, collection);
        collectionRoleService.deleteByCollection(context, collection);
    }

    @Override
    public List<String> getEPersonDeleteConstraints(Context context, EPerson ePerson) throws SQLException {
        List<String> constraints = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(claimedTaskService.findByEperson(context, ePerson))) {
            constraints.add("cwf_claimtask");
        }
        if (CollectionUtils.isNotEmpty(poolTaskService.findByEPerson(context, ePerson))) {
            constraints.add("cwf_pooltask");
        }
        if (CollectionUtils.isNotEmpty(workflowItemRoleService.findByEPerson(context, ePerson))) {
            constraints.add("cwf_workflowitemrole");
        }
        return constraints;
    }

    @Override
    public Group getWorkflowRoleGroup(Context context, Collection collection, String roleName, Group roleGroup)
        throws SQLException, IOException, WorkflowException, AuthorizeException {
        try {
            Role role = WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(roleName);
            if (role.getScope() == Role.Scope.COLLECTION || role.getScope() == Role.Scope.REPOSITORY) {
                roleGroup = WorkflowUtils.getRoleGroup(context, collection, role);
            }
            return roleGroup;
        } catch (WorkflowConfigurationException e) {
            throw new WorkflowException(e);
        }
    }

    @Override
    public Group createWorkflowRoleGroup(Context context, Collection collection, String roleName)
        throws AuthorizeException, SQLException, IOException, WorkflowConfigurationException {
        Group roleGroup;
        authorizeService.authorizeAction(context, collection, Constants.WRITE);
        roleGroup = groupService.create(context);
        Role role = WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(roleName);
        if (role.getScope() == Role.Scope.COLLECTION) {
            groupService.setName(roleGroup,
                                 "COLLECTION_" + collection.getID().toString()
                                     + "_WORKFLOW_ROLE_" + roleName);
        } else {
            groupService.setName(roleGroup, role.getName());
        }
        groupService.update(context, roleGroup);
        authorizeService.addPolicy(context, collection, Constants.ADD, roleGroup);
        if (role.getScope() == Role.Scope.COLLECTION) {
            WorkflowUtils.createCollectionWorkflowRole(context, collection, roleName, roleGroup);
        }
        return roleGroup;
    }

    @Override
    public List<String> getFlywayMigrationLocations() {
        return Collections.singletonList("classpath:org/dspace/storage/rdbms/xmlworkflow");
    }

    @Override
    public XmlWorkflowItem start(Context context, WorkspaceItem wsi)
        throws SQLException, AuthorizeException, IOException, WorkflowException {
        try {
            Item myitem = wsi.getItem();
            Collection collection = wsi.getCollection();
            Workflow wf = xmlWorkflowFactory.getWorkflow(collection);

            XmlWorkflowItem wfi = xmlWorkflowItemService.create(context, myitem, collection);
            wfi.setMultipleFiles(wsi.hasMultipleFiles());
            wfi.setMultipleTitles(wsi.hasMultipleTitles());
            wfi.setPublishedBefore(wsi.isPublishedBefore());
            xmlWorkflowItemService.update(context, wfi);
            removeUserItemPolicies(context, myitem, myitem.getSubmitter());
            grantSubmitterReadPolicies(context, myitem);

            context.turnOffAuthorisationSystem();
            Step firstStep = wf.getFirstStep();
            if (firstStep.isValidStep(context, wfi)) {
                activateFirstStep(context, wf, firstStep, wfi);
            } else {
                //Get our next step, if none is found, archive our item
                firstStep = wf.getNextStep(context, wfi, firstStep, ActionResult.OUTCOME_COMPLETE);
                if (firstStep == null) {
                    archive(context, wfi);
                } else {
                    activateFirstStep(context, wf, firstStep, wfi);
                }

            }
            // remove the WorkspaceItem
            workspaceItemService.deleteWrapper(context, wsi);
            context.restoreAuthSystemState();
            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, wfi.getItem().getID(), null,
                    itemService.getIdentifiers(context, wfi.getItem())));
            return wfi;
        } catch (WorkflowConfigurationException e) {
            throw new WorkflowException(e);
        }
    }

    //TODO: this is currently not used in our notifications. Look at the code used by the original WorkflowManager

    @Override
    public XmlWorkflowItem startWithoutNotify(Context context, WorkspaceItem wsi)
        throws SQLException, AuthorizeException, IOException, WorkflowException {
        // make a hash table entry with item ID for no notify
        // notify code checks no notify hash for item id
        noEMail.put(wsi.getItem().getID(), Boolean.TRUE);

        return start(context, wsi);
    }

    @Override
    public void alertUsersOnTaskActivation(Context c, XmlWorkflowItem wfi, String emailTemplate, List<EPerson> epa,
                                           String... arguments) throws IOException, SQLException, MessagingException {
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

    protected void grantSubmitterReadPolicies(Context context, Item item) throws SQLException, AuthorizeException {
        EPerson submitter = item.getSubmitter();
        if (null != submitter) {
            //A list of policies the user has for this item
            List<Integer> userHasPolicies = new ArrayList<>();
            List<ResourcePolicy> itempols = authorizeService.getPolicies(context, item);
            for (ResourcePolicy resourcePolicy : itempols) {
                if (submitter.equals(resourcePolicy.getEPerson())) {
                    //The user has already got this policy so add it to the list
                    userHasPolicies.add(resourcePolicy.getAction());
                }
            }
            //Make sure we don't add duplicate policies
            if (!userHasPolicies.contains(Constants.READ)) {
                addPolicyToItem(context, item, Constants.READ, submitter, ResourcePolicy.TYPE_SUBMISSION);
            }
        }
    }

    /**
     * Activate the first step in a workflow for a WorkflowItem.
     * If the step has no UI then execute it as well.
     *
     * @param context current DSpace session.
     * @param wf workflow being traversed.
     * @param firstStep the step to be activated.
     * @param wfi the item upon which to activate the step.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws SQLException passed through.
     * @throws WorkflowException passed through.
     * @throws WorkflowConfigurationException unused.
     */
    protected void activateFirstStep(Context context, Workflow wf, Step firstStep, XmlWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException, WorkflowException, WorkflowConfigurationException {
        WorkflowActionConfig firstActionConfig = firstStep.getUserSelectionMethod();

        // Check for curation tasks at the start of this step.
        //
        // If doCuration returns true, either no curation tasks are mapped to
        // this step, or they were run and succeeded.  In this scenario, we
        // continue with the current step.
        //
        // If doCuration returns false, either curation tasks were queued, or
        // they resulted in rejection of the item.  In this scenario, the
        // current step cannot be completed and we must exit immediately.
        if (!xmlWorkflowCuratorService.doCuration(context, wfi)) {
            // don't proceed - either curation tasks queued, or item rejected
            log.info(LogHelper.getHeader(context, "start_workflow",
                    "workflow_item_id=" + wfi.getID()
                            + ",item_id=" + wfi.getItem().getID()
                            + ",collection_id=" + wfi.getCollection().getID()
                            + ",current_action=" + firstActionConfig.getId()
                            + ",doCuration=false"));
            return;
        }

        // Activate the step.
        firstActionConfig.getProcessingAction().activate(context, wfi);
        log.info(LogHelper.getHeader(context, "start_workflow",
                firstActionConfig.getProcessingAction()
                        + " workflow_item_id=" + wfi.getID()
                        + "item_id=" + wfi.getItem().getID()
                        + "collection_id=" + wfi.getCollection().getID()));

        // record the start of the workflow w/provenance message
        recordStart(context, wfi.getItem(), firstActionConfig.getProcessingAction());

        //Fire an event !
        logWorkflowEvent(context, firstStep.getWorkflow().getID(), null, null, wfi, null, firstStep, firstActionConfig);

        //If we don't have a UI then execute the action.
        if (!firstActionConfig.requiresUI()) {
            ActionResult outcome = firstActionConfig.getProcessingAction().execute(context, wfi, firstStep, null);
            processOutcome(context, null, wf, firstStep, firstActionConfig, outcome, wfi, true);
        }
    }

    @Override
    public WorkflowActionConfig doState(Context c, EPerson user,
            HttpServletRequest request, int workflowItemId, Workflow workflow,
            WorkflowActionConfig currentActionConfig)
            throws SQLException, AuthorizeException, IOException,
            MessagingException, WorkflowException {
        XmlWorkflowItem wi = xmlWorkflowItemService.find(c, workflowItemId);

        // Check for curation tasks.
        //
        // If doCuration returns true, either no curation tasks are mapped to
        // this step, or they were run and succeeded.  In this scenario, we
        // continue with the current step.
        //
        // If doCuration returns false, either curation tasks were queued, or
        // they resulted in rejection of the item.  In this scenario, the
        // current step cannot be completed and we must exit immediately.
        if (!xmlWorkflowCuratorService.doCuration(c, wi)) {
            // don't proceed - either curation tasks queued, or item rejected
            log.info(LogHelper.getHeader(c, "advance_workflow",
                    "workflow_item_id=" + wi.getID()
                            + ",item_id=" + wi.getItem().getID()
                            + ",collection_id=" + wi.getCollection().getID()
                            + ",current_action=" + currentActionConfig.getId()
                            + ",doCuration=false"));
            return currentActionConfig;
        }

        try {
            Step currentStep = currentActionConfig.getStep();
            if (currentActionConfig.getProcessingAction().isAuthorized(c, request, wi)) {
                ActionResult outcome = currentActionConfig.getProcessingAction().execute(c, wi, currentStep, request);
                // the cancel action is the default when the request is not understood or a "back to mydspace" was
                // pressed in the old UI
                if (outcome.getType() == ActionResult.TYPE.TYPE_CANCEL) {
                    throw new WorkflowException("Unprocessable request for the action " + currentStep.getId());
                }
                c.addEvent(new Event(Event.MODIFY, Constants.ITEM, wi.getItem().getID(), null,
                        itemService.getIdentifiers(c, wi.getItem())));
                return processOutcome(c, user, workflow, currentStep, currentActionConfig, outcome, wi, false);
            } else {
                throw new AuthorizeException("You are not allowed to to perform this task.");
            }
        } catch (WorkflowConfigurationException e) {
            log.error(LogHelper.getHeader(c, "error while executing state",
                    "workflow:  " + workflow.getID()
                            + " action: " + currentActionConfig.getId()
                            + " workflowItemId: " + workflowItemId), e);
            WorkflowUtils.sendAlert(request, e);
            throw new WorkflowException(e);
        }
    }

    @Override
    public WorkflowActionConfig processOutcome(Context c, EPerson user, Workflow workflow, Step currentStep,
                                               WorkflowActionConfig currentActionConfig, ActionResult currentOutcome,
                                               XmlWorkflowItem wfi, boolean enteredNewStep)
        throws IOException, AuthorizeException, SQLException, WorkflowException {
        if (currentOutcome.getType() == ActionResult.TYPE.TYPE_PAGE
                || currentOutcome.getType() == ActionResult.TYPE.TYPE_ERROR) {
            //Our outcome is a page or an error, so return our current action
            c.restoreAuthSystemState();
            return currentActionConfig;
        } else if (currentOutcome.getType() == ActionResult.TYPE.TYPE_CANCEL
                || currentOutcome.getType() == ActionResult.TYPE.TYPE_SUBMISSION_PAGE) {
            //We either pressed the cancel button or got an order to return to the submission page, so don't return
            // an action
            //By not returning an action we ensure ourselfs that we go back to the submission page
            c.restoreAuthSystemState();
            return null;
        } else if (currentOutcome.getType() == ActionResult.TYPE.TYPE_OUTCOME) {
            // An action was taken by a reviewer.
            Step nextStep = null;
            WorkflowActionConfig nextActionConfig = null;
            try {
                if (currentOutcome.getResult() == ActionResult.OUTCOME_COMPLETE) {
                    //We have completed our action.  Retrieve the next action.
                    nextActionConfig = currentStep.getNextAction(currentActionConfig);
                }

                if (nextActionConfig != null) {
                    //We remain in the current step since another action is found.
                    nextStep = currentStep;
                    nextActionConfig.getProcessingAction().activate(c, wfi);
                    if (nextActionConfig.requiresUI() && !enteredNewStep) {
                        createOwnedTask(c, wfi, currentStep, nextActionConfig, user);
                        return nextActionConfig;
                    } else if (nextActionConfig.requiresUI() && enteredNewStep) {
                        //We have entered a new step and have encountered a UI, return null since the current user
                        // doesn't have anything to do with this
                        c.restoreAuthSystemState();
                        return null;
                    } else {
                        // UI not required by next action.
                        ActionResult newOutcome = nextActionConfig.getProcessingAction()
                                                                  .execute(c, wfi, currentStep, null);
                        return processOutcome(c, user, workflow, currentStep, nextActionConfig, newOutcome, wfi,
                                              enteredNewStep);
                    }
                } else if (enteredNewStep) {
                    // If the user finished his/her step, we keep processing until there is a UI step action or no
                    // step at all
                    nextStep = workflow.getNextStep(c, wfi, currentStep, currentOutcome.getResult());
                    c.turnOffAuthorisationSystem();
                    nextActionConfig = processNextStep(c, user, workflow, currentOutcome, wfi, nextStep);
                    //If we require a user interface return null so that the user is redirected to the "submissions
                    // page"
                    if (nextActionConfig == null || nextActionConfig.requiresUI()) {
                        return null;
                    } else {
                        return nextActionConfig;
                    }
                } else {
                    ClaimedTask task = claimedTaskService.findByWorkflowIdAndEPerson(c, wfi, user);

                    //Check if we have a task for this action (might not be the case with automatic steps)
                    //First add it to our list of finished users, since no more actions remain
                    workflowRequirementsService.addFinishedUser(c, wfi, user);
                    c.turnOffAuthorisationSystem();
                    //Check if our requirements have been met
                    if ((currentStep.isFinished(c, wfi) && currentOutcome
                        .getResult() == ActionResult.OUTCOME_COMPLETE) || currentOutcome
                        .getResult() != ActionResult.OUTCOME_COMPLETE) {
                        //Delete all the table rows containing the users who performed this task
                        workflowRequirementsService.clearInProgressUsers(c, wfi);
                        //Remove all the tasks
                        deleteAllTasks(c, wfi);


                        nextStep = workflow.getNextStep(c, wfi, currentStep, currentOutcome.getResult());

                        nextActionConfig = processNextStep(c, user, workflow, currentOutcome, wfi, nextStep);
                        //If we require a user interface return null so that the user is redirected to the
                        // "submissions page"
                        if (nextActionConfig == null || nextActionConfig.requiresUI()) {
                            return null;
                        } else {
                            return nextActionConfig;
                        }
                    } else {
                        //We are done with our actions so go to the submissions page but remove action ClaimedAction
                        // first
                        deleteClaimedTask(c, wfi, task);
                        c.restoreAuthSystemState();
                        nextStep = currentStep;
                        nextActionConfig = currentActionConfig;
                        return null;
                    }
                }
            } catch (IOException | SQLException | AuthorizeException
                    | WorkflowException | WorkflowConfigurationException e) {
                log.error("error while processing workflow outcome", e);
            } finally {
                if ((nextStep != null && currentStep != null && nextActionConfig != null)
                        || (wfi.getItem().isArchived() && currentStep != null)) {
                    logWorkflowEvent(c, currentStep.getWorkflow().getID(), currentStep.getId(),
                                     currentActionConfig.getId(), wfi, user, nextStep, nextActionConfig);
                }
            }

        }

        log.error(LogHelper.getHeader(c, "Invalid step outcome", "Workflow item id: " + wfi.getID()));
        throw new WorkflowException("Invalid step outcome");
    }

    protected void logWorkflowEvent(Context c, String workflowId, String previousStepId, String previousActionConfigId,
                                    XmlWorkflowItem wfi, EPerson actor, Step newStep,
                                    WorkflowActionConfig newActionConfig) throws SQLException {
        try {
            //Fire an event so we can log our action !
            Item item = wfi.getItem();
            Collection myCollection = wfi.getCollection();
            String workflowStepString = null;

            List<EPerson> currentEpersonOwners = new ArrayList<>();
            List<Group> currentGroupOwners = new ArrayList<>();
            //These are only null if our item is sent back to the submission
            if (newStep != null && newActionConfig != null) {
                workflowStepString = workflowId + "." + newStep.getId() + "." + newActionConfig.getId();

                //Retrieve the current owners of the task
                List<ClaimedTask> claimedTasks = claimedTaskService.find(c, wfi, newStep.getId());
                List<PoolTask> pooledTasks = poolTaskService.find(c, wfi);
                for (PoolTask poolTask : pooledTasks) {
                    if (poolTask.getEperson() != null) {
                        currentEpersonOwners.add(poolTask.getEperson());
                    } else {
                        currentGroupOwners.add(poolTask.getGroup());
                    }
                }
                for (ClaimedTask claimedTask : claimedTasks) {
                    currentEpersonOwners.add(claimedTask.getOwner());
                }
            }
            String previousWorkflowStepString = null;
            if (previousStepId != null && previousActionConfigId != null) {
                previousWorkflowStepString = workflowId + "." + previousStepId + "." + previousActionConfigId;
            }

            //Fire our usage event !
            UsageWorkflowEvent usageWorkflowEvent = new UsageWorkflowEvent(c, item, wfi, workflowStepString,
                                                                           previousWorkflowStepString, myCollection,
                                                                           actor);

            usageWorkflowEvent.setEpersonOwners(currentEpersonOwners.toArray(new EPerson[currentEpersonOwners.size()]));
            usageWorkflowEvent.setGroupOwners(currentGroupOwners.toArray(new Group[currentGroupOwners.size()]));

            DSpaceServicesFactory.getInstance().getEventService().fireEvent(usageWorkflowEvent);
        } catch (SQLException e) {
            //Catch all errors we do not want our workflow to crash because the logging threw an exception
            log.error(LogHelper.getHeader(c, "Error while logging workflow event", "Workflow Item: " + wfi.getID()),
                      e);
        }
    }

    protected WorkflowActionConfig processNextStep(Context c, EPerson user, Workflow workflow,
                                                   ActionResult currentOutcome, XmlWorkflowItem wfi, Step nextStep)
        throws SQLException, IOException, AuthorizeException, WorkflowException, WorkflowConfigurationException {
        WorkflowActionConfig nextActionConfig;
        if (nextStep != null) {
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
        } else {
            if (currentOutcome.getResult() != ActionResult.OUTCOME_COMPLETE) {
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
     * @param context The relevant DSpace Context.
     * @param wfi     workflow item
     * @return the fully archived item.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    protected Item archive(Context context, XmlWorkflowItem wfi)
        throws SQLException, IOException, AuthorizeException {
        // FIXME: Check auth
        Item item = wfi.getItem();
        Collection collection = wfi.getCollection();

        // Remove (if any) the workflowItemroles for this item
        workflowItemRoleService.deleteForWorkflowItem(context, wfi);

        log.info(LogHelper.getHeader(context, "archive_item", "workflow_item_id="
            + wfi.getID() + "item_id=" + item.getID() + "collection_id="
            + collection.getID()));

        installItemService.installItem(context, wfi);

        //Notify
        notifyOfArchive(context, item, collection);

        //Clear any remaining workflow metadata
        itemService
            .clearMetadata(context, item, WorkflowRequirementsService.WORKFLOW_SCHEMA, Item.ANY, Item.ANY, Item.ANY);
        itemService.update(context, item);

        // Log the event
        log.info(LogHelper.getHeader(context, "install_item", "workflow_item_id="
            + wfi.getID() + ", item_id=" + item.getID() + "handle=FIXME"));

        return item;
    }

    /**
     * notify the submitter that the item is archived
     *
     * @param context The relevant DSpace Context.
     * @param item    which item was archived
     * @param coll    collection name to display in template
     * @throws SQLException An exception that provides information on a database access error or other errors.
     * @throws IOException  A general class of exceptions produced by failed or interrupted I/O operations.
     */
    protected void notifyOfArchive(Context context, Item item, Collection coll)
        throws SQLException, IOException {
        try {
            // Get submitter
            EPerson ep = item.getSubmitter();
            // send the notification to the submitter unless the submitter eperson has been deleted
            if (null != ep) {
                // Get the Locale
                Locale supportedLocale = I18nUtil.getEPersonLocale(ep);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_archive"));

                // Get the item handle to email to user
                String handle = handleService.findHandle(context, item);

                // Get title
                List<MetadataValue> titles = itemService
                    .getMetadata(item, MetadataSchemaEnum.DC.getName(), "title", null, Item.ANY);
                String title = "";
                try {
                    title = I18nUtil.getMessage("org.dspace.xmlworkflow.XMLWorkflowService.untitled");
                } catch (MissingResourceException e) {
                    title = "Untitled";
                }
                if (titles.size() > 0) {
                    title = titles.iterator().next().getValue();
                }

                email.addRecipient(ep.getEmail());
                email.addArgument(title);
                email.addArgument(coll.getName());
                email.addArgument(handleService.getCanonicalForm(handle));

                email.send();
            }
        } catch (MessagingException e) {
            log.warn(LogHelper.getHeader(context, "notifyOfArchive",
                    "cannot email user" + " item_id=" + item.getID()), e);
        }
    }

    // send notices of curation activity
    @Override
    public void notifyOfCuration(Context c, XmlWorkflowItem wi,
            List<EPerson> ePeople, String taskName, String action, String message)
            throws SQLException, IOException {
        try {
            // Get the item title
            String title = getItemTitle(wi);

            // Get the submitter's name
            String submitter = getSubmitterName(wi);

            // Get the collection
            Collection coll = wi.getCollection();

            for (EPerson epa : ePeople) {
                Locale supportedLocale = I18nUtil.getEPersonLocale(epa);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "flowtask_notify"));
                email.addArgument(title);
                email.addArgument(coll.getName());
                email.addArgument(submitter);
                email.addArgument(taskName);
                email.addArgument(message);
                email.addArgument(action);
                email.addRecipient(epa.getEmail());
                email.send();
            }
        } catch (MessagingException e) {
            log.warn(LogHelper.getHeader(c, "notifyOfCuration",
                    "cannot email users of workflow_item_id " + wi.getID()
                            + ":  " + e.getMessage()), e);
        }
    }

    protected String getItemTitle(XmlWorkflowItem wi) throws SQLException {
        Item myitem = wi.getItem();
        String title = myitem.getName();

        // only return the first element, or "Untitled"
        if (StringUtils.isNotBlank(title)) {
            return title;
        } else {
            return I18nUtil.getMessage("org.dspace.xmlworkflow.XMLWorkflowService.untitled ");
        }
    }

    protected String getSubmitterName(XmlWorkflowItem wi) throws SQLException {
        EPerson e = wi.getSubmitter();

        return getEPersonName(e);
    }

    /***********************************
     * WORKFLOW TASK MANAGEMENT
     **********************************/

    @Override
    public void deleteAllTasks(Context context, XmlWorkflowItem wi) throws SQLException, AuthorizeException {
        deleteAllPooledTasks(context, wi);

        Iterator<ClaimedTask> allClaimedTasks = claimedTaskService.findByWorkflowItem(context, wi).iterator();
        while (allClaimedTasks.hasNext()) {
            ClaimedTask task = allClaimedTasks.next();
            allClaimedTasks.remove();
            deleteClaimedTask(context, wi, task);
        }
    }

    @Override
    public void deleteAllPooledTasks(Context context, XmlWorkflowItem wi) throws SQLException, AuthorizeException {
        Iterator<PoolTask> allPooledTasks = poolTaskService.find(context, wi).iterator();
        while (allPooledTasks.hasNext()) {
            PoolTask poolTask = allPooledTasks.next();
            allPooledTasks.remove();
            deletePooledTask(context, wi, poolTask);
        }
    }

    @Override
    public void deletePooledTask(Context context, XmlWorkflowItem wi, PoolTask task)
        throws SQLException, AuthorizeException {
        if (task != null) {
            if (task.getEperson() != null) {
                removeUserItemPolicies(context, wi.getItem(), task.getEperson());
            } else {
                removeGroupItemPolicies(context, wi.getItem(), task.getGroup());
            }
            poolTaskService.delete(context, task);
        }
    }

    @Override
    public void deleteClaimedTask(Context c, XmlWorkflowItem wi, ClaimedTask task)
        throws SQLException, AuthorizeException {
        if (task != null) {
            removeUserItemPolicies(c, wi.getItem(), task.getOwner());
            claimedTaskService.delete(c, task);
        }
        c.addEvent(new Event(Event.MODIFY, Constants.ITEM, wi.getItem().getID(), null,
                itemService.getIdentifiers(c, wi.getItem())));
    }

    @Override
    public void createPoolTasks(Context context, XmlWorkflowItem wi, RoleMembers assignees, Step step,
                                WorkflowActionConfig action)
        throws SQLException, AuthorizeException {
        // create a tasklist entry for each eperson
        for (EPerson anEpa : assignees.getEPersons()) {
            PoolTask task = poolTaskService.create(context);
            task.setStepID(step.getId());
            task.setWorkflowID(step.getWorkflow().getID());
            task.setEperson(anEpa);
            task.setActionID(action.getId());
            task.setWorkflowItem(wi);
            poolTaskService.update(context, task);
            //Make sure this user has a task
            grantUserAllItemPolicies(context, wi.getItem(), anEpa, ResourcePolicy.TYPE_WORKFLOW);
        }
        for (Group group : assignees.getGroups()) {
            PoolTask task = poolTaskService.create(context);
            task.setStepID(step.getId());
            task.setWorkflowID(step.getWorkflow().getID());
            task.setGroup(group);
            task.setActionID(action.getId());
            task.setWorkflowItem(wi);
            poolTaskService.update(context, task);
            //Make sure this user has a task
            grantGroupAllItemPolicies(context, wi.getItem(), group, ResourcePolicy.TYPE_WORKFLOW);
        }
    }

    @Override
    public void createOwnedTask(Context context, XmlWorkflowItem wi, Step step, WorkflowActionConfig action, EPerson e)
        throws SQLException, AuthorizeException {
        ClaimedTask task = claimedTaskService.create(context);
        task.setWorkflowItem(wi);
        task.setStepID(step.getId());
        task.setActionID(action.getId());
        task.setOwner(e);
        task.setWorkflowID(step.getWorkflow().getID());
        claimedTaskService.update(context, task);
        //Make sure this user has a task
        grantUserAllItemPolicies(context, wi.getItem(), e, ResourcePolicy.TYPE_WORKFLOW);
    }

    @Override
    public void grantUserAllItemPolicies(Context context, Item item, EPerson epa, String policyType)
        throws AuthorizeException, SQLException {
        if (epa != null) {
            //A list of policies the user has for this item
            List<Integer> userHasPolicies = new ArrayList<>();
            List<ResourcePolicy> itempols = authorizeService.getPolicies(context, item);
            for (ResourcePolicy resourcePolicy : itempols) {
                if (epa.equals(resourcePolicy.getEPerson())) {
                    //The user has already got this policy so it it to the list
                    userHasPolicies.add(resourcePolicy.getAction());
                }
            }

            //Make sure we don't add duplicate policies
            if (!userHasPolicies.contains(Constants.READ)) {
                addPolicyToItem(context, item, Constants.READ, epa, policyType);
            }
            if (!userHasPolicies.contains(Constants.WRITE)) {
                addPolicyToItem(context, item, Constants.WRITE, epa, policyType);
            }
            if (!userHasPolicies.contains(Constants.DELETE)) {
                addPolicyToItem(context, item, Constants.DELETE, epa, policyType);
            }
            if (!userHasPolicies.contains(Constants.ADD)) {
                addPolicyToItem(context, item, Constants.ADD, epa, policyType);
            }
            if (!userHasPolicies.contains(Constants.REMOVE)) {
                addPolicyToItem(context, item, Constants.REMOVE, epa, policyType);
            }
        }
    }

    protected void grantGroupAllItemPolicies(Context context, Item item, Group group, String policyType)
        throws AuthorizeException, SQLException {
        if (group != null) {
            //A list of policies the user has for this item
            List<Integer> groupHasPolicies = new ArrayList<>();
            List<ResourcePolicy> itempols = authorizeService.getPolicies(context, item);
            for (ResourcePolicy resourcePolicy : itempols) {
                if (group.equals(resourcePolicy.getGroup())) {
                    //The user has already got this policy so it it to the list
                    groupHasPolicies.add(resourcePolicy.getAction());
                }
            }
            //Make sure we don't add duplicate policies
            if (!groupHasPolicies.contains(Constants.READ)) {
                addGroupPolicyToItem(context, item, Constants.READ, group, policyType);
            }
            if (!groupHasPolicies.contains(Constants.WRITE)) {
                addGroupPolicyToItem(context, item, Constants.WRITE, group, policyType);
            }
            if (!groupHasPolicies.contains(Constants.DELETE)) {
                addGroupPolicyToItem(context, item, Constants.DELETE, group, policyType);
            }
            if (!groupHasPolicies.contains(Constants.ADD)) {
                addGroupPolicyToItem(context, item, Constants.ADD, group, policyType);
            }
            if (!groupHasPolicies.contains(Constants.REMOVE)) {
                addGroupPolicyToItem(context, item, Constants.REMOVE, group, policyType);
            }
        }
    }

    protected void addPolicyToItem(Context context, Item item, int action, EPerson epa, String policyType)
        throws AuthorizeException, SQLException {
        if (epa != null) {
            authorizeService.addPolicy(context, item, action, epa, policyType);
            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                authorizeService.addPolicy(context, bundle, action, epa, policyType);
                List<Bitstream> bits = bundle.getBitstreams();
                for (Bitstream bit : bits) {
                    authorizeService.addPolicy(context, bit, action, epa, policyType);
                }
            }
        }
    }

    protected void addGroupPolicyToItem(Context context, Item item, int action, Group group, String policyType)
        throws AuthorizeException, SQLException {
        if (group != null) {
            authorizeService.addPolicy(context, item, action, group, policyType);
            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                authorizeService.addPolicy(context, bundle, action, group, policyType);
                List<Bitstream> bits = bundle.getBitstreams();
                for (Bitstream bit : bits) {
                    authorizeService.addPolicy(context, bit, action, group, policyType);
                }
            }
        }
    }

    @Override
    public void removeUserItemPolicies(Context context, Item item, EPerson e) throws SQLException, AuthorizeException {
        if (e != null && item.getSubmitter() != null) {
            //Also remove any lingering authorizations from this user
            authorizeService.removeEPersonPolicies(context, item, e);
            //Remove the bundle rights
            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                authorizeService.removeEPersonPolicies(context, bundle, e);
                List<Bitstream> bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    authorizeService.removeEPersonPolicies(context, bitstream, e);
                }
            }
            // Ensure that the submitter always retains his resource policies
            if (e.getID().equals(item.getSubmitter().getID())) {
                grantSubmitterReadPolicies(context, item);
            }
        }
    }


    protected void removeGroupItemPolicies(Context context, Item item, Group e)
        throws SQLException, AuthorizeException {
        if (e != null && item.getSubmitter() != null) {
            //Also remove any lingering authorizations from this user
            authorizeService.removeGroupPolicies(context, item, e);
            //Remove the bundle rights
            List<Bundle> bundles = item.getBundles();
            for (Bundle bundle : bundles) {
                authorizeService.removeGroupPolicies(context, bundle, e);
                List<Bitstream> bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    authorizeService.removeGroupPolicies(context, bitstream, e);
                }
            }
        }
    }

    @Override
    public void deleteWorkflowByWorkflowItem(Context context, XmlWorkflowItem wi, EPerson e)
            throws SQLException, AuthorizeException, IOException {
        Item myitem = wi.getItem();
        UUID itemID = myitem.getID();
        Integer workflowID = wi.getID();
        UUID collID = wi.getCollection().getID();
        // stop workflow
        deleteAllTasks(context, wi);
        context.turnOffAuthorisationSystem();
        //Also clear all info for this step
        workflowRequirementsService.clearInProgressUsers(context, wi);
        // Remove (if any) the workflowItemroles for this item
        workflowItemRoleService.deleteForWorkflowItem(context, wi);
        // Now remove the workflow object manually from the database
        xmlWorkflowItemService.deleteWrapper(context, wi);
        // Now delete the item
        itemService.delete(context, myitem);
        log.info(LogHelper.getHeader(context, "delete_workflow", "workflow_item_id="
                + workflowID + "item_id=" + itemID
                + "collection_id=" + collID + "eperson_id="
                + e.getID()));
        context.restoreAuthSystemState();
    }

    @Override
    public WorkspaceItem sendWorkflowItemBackSubmission(Context context, XmlWorkflowItem wi, EPerson e,
                                                        String provenance,
                                                        String rejection_message)
        throws SQLException, AuthorizeException,
        IOException {

        String workflowID = null;
        String currentStepId = null;
        String currentActionConfigId = null;
        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, wi, e);
        if (claimedTask != null) {
            //Log it
            workflowID = claimedTask.getWorkflowID();
            currentStepId = claimedTask.getStepID();
            currentActionConfigId = claimedTask.getActionID();
        }
        context.turnOffAuthorisationSystem();

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
        itemService
            .addMetadata(context, myitem, MetadataSchemaEnum.DC.getName(),
                         "description", "provenance", "en", provDescription);

        //Clear any workflow schema related metadata
        itemService
            .clearMetadata(context, myitem, WorkflowRequirementsService.WORKFLOW_SCHEMA, Item.ANY, Item.ANY, Item.ANY);

        itemService.update(context, myitem);

        // remove policy for controller
        removeUserItemPolicies(context, myitem, e);
        revokeReviewerPolicies(context, myitem);

        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(context, wi);

        // notify that it's been rejected
        notifyOfReject(context, wi, e, rejection_message);
        log.info(LogHelper.getHeader(context, "reject_workflow", "workflow_item_id="
            + wi.getID() + "item_id=" + wi.getItem().getID()
            + "collection_id=" + wi.getCollection().getID() + "eperson_id="
            + e.getID()));

        logWorkflowEvent(context, workflowID, currentStepId, currentActionConfigId, wi, e, null, null);

        context.restoreAuthSystemState();
        return wsi;
    }

    @Override
    public WorkspaceItem abort(Context c, XmlWorkflowItem wi, EPerson e)
        throws AuthorizeException, SQLException, IOException {
        if (!authorizeService.isAdmin(c)) {
            throw new AuthorizeException(
                "You must be an admin to abort a workflow");
        }

        c.turnOffAuthorisationSystem();
        //Restore permissions for the submitter
        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);

        log.info(LogHelper.getHeader(c, "abort_workflow", "workflow_item_id="
            + wi.getID() + "item_id=" + wsi.getItem().getID()
            + "collection_id=" + wi.getCollection().getID() + "eperson_id="
            + e.getID()));

        c.addEvent(new Event(Event.MODIFY, Constants.ITEM, wsi.getItem().getID(), null,
                itemService.getIdentifiers(c, wsi.getItem())));

        c.restoreAuthSystemState();
        return wsi;
    }

    /**
     * Return the workflow item to the workspace of the submitter. The workflow
     * item is removed, and a workspace item created.
     *
     * @param c   Context
     * @param wfi WorkflowItem to be 'dismantled'
     * @return the workspace item
     * @throws java.io.IOException                     ...
     * @throws java.sql.SQLException                   ...
     * @throws org.dspace.authorize.AuthorizeException ...
     */
    protected WorkspaceItem returnToWorkspace(Context c, XmlWorkflowItem wfi)
        throws SQLException, IOException, AuthorizeException {
        // authorize a DSpaceActions.REJECT
        // stop workflow
        deleteAllTasks(c, wfi);

        c.turnOffAuthorisationSystem();
        //Also clear all info for this step
        workflowRequirementsService.clearInProgressUsers(c, wfi);

        // Remove (if any) the workflowItemroles for this item
        workflowItemRoleService.deleteForWorkflowItem(c, wfi);

        Item myitem = wfi.getItem();
        //Restore permissions for the submitter
        grantUserAllItemPolicies(c, myitem, myitem.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);

        // FIXME: How should this interact with the workflow system?
        // FIXME: Remove license
        // FIXME: Provenance statement?
        // Create the new workspace item row
        WorkspaceItem workspaceItem = workspaceItemService.create(c, wfi);
        workspaceItem.setMultipleFiles(wfi.hasMultipleFiles());
        workspaceItem.setMultipleTitles(wfi.hasMultipleTitles());
        workspaceItem.setPublishedBefore(wfi.isPublishedBefore());
        workspaceItemService.update(c, workspaceItem);

        //myitem.update();
        log.info(LogHelper.getHeader(c, "return_to_workspace",
                                      "workflow_item_id=" + wfi.getID() + "workspace_item_id="
                                          + workspaceItem.getID()));

        // Now remove the workflow object manually from the database
        xmlWorkflowItemService.deleteWrapper(c, wfi);
        return workspaceItem;
    }

    @Override
    public String getEPersonName(EPerson ePerson) {
        String submitter = ePerson.getFullName();

        submitter = submitter + "(" + ePerson.getEmail() + ")";

        return submitter;
    }

    // Create workflow start provenance message
    protected void recordStart(Context context, Item myitem, Action action)
        throws SQLException, IOException, AuthorizeException {
        // get date
        DCDate now = DCDate.getCurrent();

        // Create provenance description
        String provmessage = "";

        if (myitem.getSubmitter() != null) {
            provmessage = "Submitted by " + myitem.getSubmitter().getFullName()
                + " (" + myitem.getSubmitter().getEmail() + ") on "
                + now.toString() + " workflow start=" + action.getProvenanceStartId() + "\n";
        } else {
            // else, null submitter
            provmessage = "Submitted by unknown (probably automated) on"
                + now.toString() + " workflow start=" + action.getProvenanceStartId() + "\n";
        }

        // add sizes and checksums of bitstreams
        provmessage += installItemService.getBitstreamProvenanceMessage(context, myitem);

        // Add message to the DC
        itemService
            .addMetadata(context, myitem, MetadataSchemaEnum.DC.getName(),
                         "description", "provenance", "en", provmessage);
        itemService.update(context, myitem);
    }

    protected void notifyOfReject(Context c, XmlWorkflowItem wi, EPerson e,
                                  String reason) {
        try {
            // send the notification only if the person was not deleted in the
            // meantime between submission and archiving.
            EPerson eperson = wi.getSubmitter();
            if (eperson != null) {
                // Get the item title
                String title = wi.getItem().getName();

                // Get the collection
                Collection coll = wi.getCollection();

                // Get rejector's name
                String rejector = getEPersonName(e);
                Locale supportedLocale = I18nUtil.getEPersonLocale(e);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_reject"));

                email.addRecipient(eperson.getEmail());
                email.addArgument(title);
                email.addArgument(coll.getName());
                email.addArgument(rejector);
                email.addArgument(reason);
                email.addArgument(configurationService.getProperty("dspace.ui.url") + "/mydspace");

                email.send();
            } else {
                // DO nothing
            }
        } catch (IOException | MessagingException ex) {
            // log this email error
            log.warn(LogHelper.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + e.getID()
                    + " eperson_email" + e.getEmail()
                    + " workflow_item_id" + wi.getID()), ex);
        }
    }

    @Override
    public String getMyDSpaceLink() {
        return configurationService.getProperty("dspace.ui.url") + "/mydspace";
    }

    protected void revokeReviewerPolicies(Context context, Item item) throws SQLException, AuthorizeException {
        List<Bundle> bundles = item.getBundles();

        for (Bundle originalBundle : bundles) {
            // remove bitstream and bundle level policies
            for (Bitstream bitstream : originalBundle.getBitstreams()) {
                authorizeService.removeAllPoliciesByDSOAndType(context, bitstream, ResourcePolicy.TYPE_WORKFLOW);
            }

            authorizeService.removeAllPoliciesByDSOAndType(context, originalBundle, ResourcePolicy.TYPE_WORKFLOW);
        }

        // remove item level policies
        authorizeService.removeAllPoliciesByDSOAndType(context, item, ResourcePolicy.TYPE_WORKFLOW);
    }
}
