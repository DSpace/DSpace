/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.curate.service.WorkflowCuratorService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageWorkflowEvent;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.dspace.workflowbasic.service.TaskListItemService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;

public class BasicWorkflowServiceImpl implements BasicWorkflowService
{

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected InstallItemService installItemService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected TaskListItemService taskListItemService;
    @Autowired(required = true)
    protected WorkflowCuratorService workflowCuratorService;
    @Autowired(required = true)
    protected BasicWorkflowItemService workflowItemService;
    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;

    protected BasicWorkflowServiceImpl()
    {

    }

    /** Symbolic names of workflow steps. */
    protected final String workflowText[] =
    {
        "SUBMIT",           // 0
        "STEP1POOL",        // 1
        "STEP1",            // 2
        "STEP2POOL",        // 3
        "STEP2",            // 4
        "STEP3POOL",        // 5
        "STEP3",            // 6
        "ARCHIVE"           // 7
    };

    /* support for 'no notification' */
    protected Map<UUID, Boolean> noEMail = new HashMap<>();

    /** log4j logger */
    private final Logger log = Logger.getLogger(BasicWorkflowServiceImpl.class);

    @Override
    public int getWorkflowID(String state)
    {
        for (int i = 0; i < workflowText.length; ++i)
        {
            if (state.equalsIgnoreCase(workflowText[i]))
            {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * This methods grants the appropriate permissions to reviewers so that they
     * can read and edit metadata and read files and edit files if allowed by 
     * configuration.
     * In most cases this method must be called within a try-finally-block that
     * temporary disables the authentication system. This is not done by this
     * method as it should be done carefully and only in contexts in which
     * granting the permissions is authorized by some previous checks.
     * 
     * @param context
     * @param wfi While all policies are granted on item, bundle or bitstream 
     *            level, this method takes an workflowitem for convenience and 
     *            uses wfi.getItem() to get the actual item.
     * @param reviewer EPerson to grant the rights to.
     * @throws SQLException
     * @throws AuthorizeException 
     */
    protected void grantReviewerPolicies(Context context, BasicWorkflowItem wfi, EPerson reviewer) throws SQLException, AuthorizeException
    {
        if(reviewer == null) {
            return;
        }

        // get item and bundle "ORIGINAL"
        Item item = wfi.getItem();
        Bundle originalBundle;
        try {
            originalBundle = itemService.getBundles(item, "ORIGINAL").get(0);
        } catch (IndexOutOfBoundsException ex) {
            originalBundle = null;
        }
        
        // grant item level policies
        for (int action : new int[] {Constants.READ, Constants.WRITE, Constants.ADD, Constants.REMOVE, Constants.DELETE})
        {
            authorizeService.addPolicy(context, item, action, reviewer, ResourcePolicy.TYPE_WORKFLOW);
        }
        
        // set bitstream and bundle policies
        if (originalBundle != null)
        {
            authorizeService.addPolicy(context, originalBundle, Constants.READ, reviewer, ResourcePolicy.TYPE_WORKFLOW);

            // shall reviewers be able to edit files?
            boolean editFiles = configurationService.getBooleanProperty("workflow.reviewer.file-edit", false);
            // if a reviewer should be able to edit bitstreams, we need add
            // permissions regarding the bundle "ORIGINAL" and its bitstreams
            if (editFiles)
            {
                authorizeService.addPolicy(context, originalBundle, Constants.ADD, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                authorizeService.addPolicy(context, originalBundle, Constants.REMOVE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                // Whenever a new bitstream is added, it inherit the policies of the bundle.
                // So we need to add all policies newly created bitstreams should get.
                authorizeService.addPolicy(context, originalBundle, Constants.WRITE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                authorizeService.addPolicy(context, originalBundle, Constants.DELETE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
            }
            for (Bitstream bitstream : originalBundle.getBitstreams())
            {
                authorizeService.addPolicy(context, bitstream, Constants.READ, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                
                // add further rights if reviewer should be able to edit bitstreams
                if (editFiles)
                {
                    authorizeService.addPolicy(context, bitstream, Constants.WRITE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                    authorizeService.addPolicy(context, bitstream, Constants.DELETE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                }
            }
        }
    }
    
    /**
     * This methods revokes any permission granted by the basic workflow systems
     * on the item specified as attribute. At time of writing this method these
     * permissions will all be granted by
     * {@link #grantReviewerPolicies(org.dspace.core.Context, org.dspace.workflowbasic.BasicWorkflowItem, org.dspace.eperson.EPerson)}.
     * In most cases this method must be called within a try-finally-block that
     * temporary disables the authentication system. This is not done by this
     * method as it should be done carefully and only in contexts in which
     * revoking the permissions is authorized by some previous checks.
     * 
     * @param context
     * @param item
     * @throws SQLException
     * @throws AuthorizeException 
     */
    protected void revokeReviewerPolicies(Context context, Item item) throws SQLException, AuthorizeException
    {
        // get bundle "ORIGINAL"
        Bundle originalBundle;
        try {
            originalBundle = itemService.getBundles(item, "ORIGINAL").get(0);
        } catch (IndexOutOfBoundsException ex) {
            originalBundle = null;
        }
        
        // remove bitstream and bundle level policies
        if (originalBundle != null)
        {
            // We added policies for Bitstreams of the bundle "original" only
            for (Bitstream bitstream : originalBundle.getBitstreams())
            {
                authorizeService.removeAllPoliciesByDSOAndType(context, bitstream, ResourcePolicy.TYPE_WORKFLOW);
            }
            
            authorizeService.removeAllPoliciesByDSOAndType(context, originalBundle, ResourcePolicy.TYPE_WORKFLOW);
        }
        
        // remove item level policies
        authorizeService.removeAllPoliciesByDSOAndType(context, item, ResourcePolicy.TYPE_WORKFLOW);
    }

    @Override
    public BasicWorkflowItem start(Context context, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        Item myitem = wsi.getItem();
        Collection collection = wsi.getCollection();

        log.info(LogManager.getHeader(context, "start_workflow", "workspace_item_id="
                + wsi.getID() + "item_id=" + myitem.getID() + "collection_id="
                + collection.getID()));

        // record the start of the workflow w/provenance message
        recordStart(context, myitem);

        // create the WorkflowItem
        BasicWorkflowItem wfi = workflowItemService.create(context, myitem, collection);
        wfi.setMultipleFiles(wsi.hasMultipleFiles());
        wfi.setMultipleTitles(wsi.hasMultipleTitles());
        wfi.setPublishedBefore(wsi.isPublishedBefore());

        // remove the WorkspaceItem
        workspaceItemService.deleteWrapper(context, wsi);

        // now get the workflow started
        wfi.setState(WFSTATE_SUBMIT);
        advance(context, wfi, null);

        // Return the workflow item
        return wfi;
    }

    @Override
    public BasicWorkflowItem startWithoutNotify(Context c, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        // make a hash table entry with item ID for no notify
        // notify code checks no notify hash for item id
        noEMail.put(wsi.getItem().getID(), Boolean.TRUE);

        return start(c, wsi);
    }

    @Override
    public List<BasicWorkflowItem> getOwnedTasks(Context context, EPerson e)
            throws java.sql.SQLException
    {
        return workflowItemService.findByOwner(context, e);
    }


    @Override
    public List<BasicWorkflowItem> getPooledTasks(Context context, EPerson e) throws SQLException
    {
        return workflowItemService.findPooledTasks(context, e);
    }


    @Override
    public void claim(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = workflowItem.getState();

        switch (taskstate)
        {
        case WFSTATE_STEP1POOL:

            authorizeService.authorizeAction(context, e, workflowItem.getCollection(), Constants.WORKFLOW_STEP_1, true);
            doState(context, workflowItem, WFSTATE_STEP1, e);

            break;

        case WFSTATE_STEP2POOL:

            authorizeService.authorizeAction(context, e, workflowItem.getCollection(), Constants.WORKFLOW_STEP_2, true);
            doState(context, workflowItem, WFSTATE_STEP2, e);

            break;

        case WFSTATE_STEP3POOL:

            authorizeService.authorizeAction(context, e, workflowItem.getCollection(), Constants.WORKFLOW_STEP_3, true);
            doState(context, workflowItem, WFSTATE_STEP3, e);

            break;

        default:
            throw new IllegalArgumentException("Workflow Step " + taskstate + " is out of range.");
        }

        log.info(LogManager.getHeader(context, "claim_task", "workflow_item_id="
                + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                + "collection_id=" + workflowItem.getCollection().getID()
                + "newowner_id=" + workflowItem.getOwner().getID() + "old_state="
                + taskstate + "new_state=" + workflowItem.getState()));
    }

    @Override
    public void advance(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        advance(context, workflowItem, e, true, true);
    }

    @Override
    public boolean advance(Context context, BasicWorkflowItem workflowItem, EPerson e,
                                  boolean curate, boolean record)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = workflowItem.getState();
        boolean archived = false;

        // perform curation tasks if needed
        if (curate && workflowCuratorService.needsCuration(workflowItem))
        {
            if (! workflowCuratorService.doCuration(context, workflowItem)) {
                // don't proceed - either curation tasks queued, or item rejected
                log.info(LogManager.getHeader(context, "advance_workflow",
                        "workflow_item_id=" + workflowItem.getID() + ",item_id="
                        + workflowItem.getItem().getID() + ",collection_id="
                        + workflowItem.getCollection().getID() + ",old_state="
                        + taskstate + ",doCuration=false"));
                return false;
            }
        }
        
        switch (taskstate)
        {
        case WFSTATE_SUBMIT:
            archived = doState(context, workflowItem, WFSTATE_STEP1POOL, e);

            break;

        case WFSTATE_STEP1:
            // advance(...) will call itself if no workflow step group exists
            // so we need to check permissions only if a workflow step group is
            // in place.
            if (workflowItem.getCollection().getWorkflowStep1() != null && e != null)
            {
                authorizeService.authorizeAction(context, e, workflowItem.getCollection(), Constants.WORKFLOW_STEP_1, true);
            }
            
            // Record provenance
            if (record)
            {
                recordApproval(context, workflowItem, e);
            }
            archived = doState(context, workflowItem, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP2:
            // advance(...) will call itself if no workflow step group exists
            // so we need to check permissions only if a workflow step group is
            // in place.
            if (workflowItem.getCollection().getWorkflowStep2() != null && e != null)
            {
                authorizeService.authorizeAction(context, e, workflowItem.getCollection(), Constants.WORKFLOW_STEP_2, true);
            }
            
            // Record provenance
            if (record)
            {
                recordApproval(context, workflowItem, e);
            }
            archived = doState(context, workflowItem, WFSTATE_STEP3POOL, e);

            break;

        case WFSTATE_STEP3:
            // advance(...) will call itself if no workflow step group exists
            // so we need to check permissions only if a workflow step group is
            // in place.
            if (workflowItem.getCollection().getWorkflowStep3() != null && e != null)
            {
                authorizeService.authorizeAction(context, e, workflowItem.getCollection(), Constants.WORKFLOW_STEP_3, true);
            }

            // We don't record approval for editors, since they can't reject,
            // and thus didn't actually make a decision
            archived = doState(context, workflowItem, WFSTATE_ARCHIVE, e);

            break;

        // error handling? shouldn't get here
        }

        log.info(LogManager.getHeader(context, "advance_workflow",
                "workflow_item_id=" + workflowItem.getID() + ",item_id="
                        + workflowItem.getItem().getID() + ",collection_id="
                        + workflowItem.getCollection().getID() + ",old_state="
                        + taskstate + ",new_state=" + workflowItem.getState()));
        return archived;
    }

    @Override
    public void unclaim(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = workflowItem.getState();

        switch (taskstate)
        {
        case WFSTATE_STEP1:

            doState(context, workflowItem, WFSTATE_STEP1POOL, e);

            break;

        case WFSTATE_STEP2:

            doState(context, workflowItem, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP3:

            doState(context, workflowItem, WFSTATE_STEP3POOL, e);

            break;
        default:
            throw new IllegalStateException("WorkflowItem reach an unknown state.");
        }

        log.info(LogManager.getHeader(context, "unclaim_workflow",
                "workflow_item_id=" + workflowItem.getID() + ",item_id="
                        + workflowItem.getItem().getID() + ",collection_id="
                        + workflowItem.getCollection().getID() + ",old_state="
                        + taskstate + ",new_state=" + workflowItem.getState()));
    }

    @Override
    public WorkspaceItem abort(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, AuthorizeException, IOException
    {
        // authorize a DSpaceActions.ABORT
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to abort a workflow");
        }

        // stop workflow regardless of its state
        taskListItemService.deleteByWorkflowItem(context, workflowItem);

        log.info(LogManager.getHeader(context, "abort_workflow", "workflow_item_id="
                + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                + "collection_id=" + workflowItem.getCollection().getID() + "eperson_id="
                + e.getID()));

        // convert into personal workspace
        return returnToWorkspace(context, workflowItem);
    }

        protected boolean doState(Context context, BasicWorkflowItem workflowItem, int newstate,
            EPerson newowner) throws SQLException, IOException,
            AuthorizeException
    {
        Collection collection = workflowItem.getCollection();

        //Gather our old data for launching the workflow event
        int oldState = workflowItem.getState();
        
        // in case we don't want to inform reviewers about tasks returned to
        // the pool by other reviewers, we'll ne to know whether they were owned
        // before. => keep this information before setting the new owner.
        EPerson oldOwner = workflowItem.getOwner();

        switch (newstate)
        {
        case WFSTATE_STEP1POOL:
            return pool(context, workflowItem, 1);

        case WFSTATE_STEP1:
            assignToReviewer(context, workflowItem, 1, newowner);
            return false;

        case WFSTATE_STEP2POOL:
            return pool(context, workflowItem, 2);

        case WFSTATE_STEP2:
            assignToReviewer(context, workflowItem, 2, newowner);
            return false;

        case WFSTATE_STEP3POOL:
            return pool(context, workflowItem, 3);

        case WFSTATE_STEP3:
            assignToReviewer(context, workflowItem, 3, newowner);
            return false;

        case WFSTATE_ARCHIVE:
            // put in archive in one transaction
            // remove workflow tasks
            taskListItemService.deleteByWorkflowItem(context, workflowItem);

            collection = workflowItem.getCollection();

            Item myitem = archive(context, workflowItem);

            // now email notification
            notifyOfArchive(context, myitem, collection);

            // remove any workflow policies left
            try
            {
                context.turnOffAuthorisationSystem();
                revokeReviewerPolicies(context, myitem);
            } finally {
                context.restoreAuthSystemState();
            }

            logWorkflowEvent(context, workflowItem.getItem(), workflowItem, context.getCurrentUser(), newstate, newowner, collection, oldState, null);

            return true;

        default:
            throw new IllegalArgumentException("BasicWorkflowService cannot handle workflowItemState " + newstate);

        }
    }

    /**
     * Helper method to take an item out of the pool, to assign it to a reviewer and to deal with reviewer policies.
     * Don't use this method directly. Instead: use {@link #start(Context, WorkspaceItem)} to start a workflow and
     * {@link #claim(Context, BasicWorkflowItem, EPerson)} as those methods handles the internal states and checks for
     * the appropriate permissions.
     * @param context DSpace context object
     * @param workflowItem The item that shall be pooled.
     * @param step The step (1-3) of the pool the item should be put to.
     * @param newowner The EPerson that should do the review.
     * @return True if the item was archived because no reviewers were assigned to any of the following workflow steps, false otherwise.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws IllegalArgumentException If {@code param} has another value than either 1, 2, or 3.

     */
    protected void assignToReviewer(Context context, BasicWorkflowItem workflowItem, int step, EPerson newowner)
    throws AuthorizeException, SQLException
    {
        // shortcut to the collection
        Collection collection = workflowItem.getCollection();

        // from the step we can recoginze the new state and the corresponding policy action.
        int newState;
        int correspondingAction;
        if (step == 1) {
            newState = WFSTATE_STEP1;
            correspondingAction = Constants.WORKFLOW_STEP_1;
        } else if (step == 2) {
            newState = WFSTATE_STEP2;
            correspondingAction = Constants.WORKFLOW_STEP_2;
        } else if (step == 3) {
            newState = WFSTATE_STEP3;
            correspondingAction = Constants.WORKFLOW_STEP_3;
        } else {
            throw new IllegalArgumentException("Got a task to pool with an improperly or unknown state.");
        }

        // Gather the old state for logging
        int oldState = workflowItem.getState();

        // if there is a workflow state group and it contains any members,
        // then we have to check permissions first
        if ((collectionService.getWorkflowGroup(collection, step) != null)
                && !(groupService.isEmpty(collectionService.getWorkflowGroup(collection, step)))
                && newowner != null)
        {
            authorizeService.authorizeAction(context, newowner, collection, correspondingAction, true);
        }

        // give the owner the appropriate permissions
        try {
            context.turnOffAuthorisationSystem();
            // maybe unnecessary, but revoke any previously granted permissions
            revokeReviewerPolicies(context, workflowItem.getItem());
            // finally grant the new permissions
            grantReviewerPolicies(context, workflowItem, newowner);
        } finally {
            context.restoreAuthSystemState();
        }

        // remove task from tasklist as someone is working on it now
        taskListItemService.deleteByWorkflowItem(context, workflowItem);
        // assign new owner
        workflowItem.setState(newState);
        workflowItem.setOwner(newowner);

        logWorkflowEvent(context, workflowItem.getItem(), workflowItem, context.getCurrentUser(), newState, newowner, collection, oldState, null);
    }

    /**
     * Helper method that manages state, policies, owner, notifies, tasklistitems and so on whenever an WorkflowItem
     * should be added to a workflow step pool. Don't use this method directly. Either use
     * {@link #unclaim(Context, BasicWorkflowItem, EPerson)} if the item is claimed,
     * {@link #start(Context, WorkspaceItem)} to start the workflow or {@link #advance(Context, BasicWorkflowItem, EPerson)}
     * to move an item to the next state.
     * @param context DSpace context object
     * @param workflowItem The item that shall be pooled.
     * @param step The step (1-3) of the pool the item should be put to.
     * @return True if the item was archived because no reviewers were assigned to any of the following workflow steps, false otherwise.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws IllegalArgumentException If {@code param} has another value than either 1, 2, or 3.
     */
    protected boolean pool(Context context, BasicWorkflowItem workflowItem, int step)
    throws SQLException, AuthorizeException, IOException
    {
        // shortcut to the collection
        Collection collection = workflowItem.getCollection();

        // from the step we can recoginze the new state and the corresponding state.
        // the new state is the pool of the step
        // the corresponding state is the state an item gets when it gets claimed. That is important to recognize if we
        // have to send notifications and if we have to skip a pool.
        int newState;
        int correspondingState;
        if (step == 1) {
            newState = WFSTATE_STEP1POOL;
            correspondingState = WFSTATE_STEP1;
        } else if (step == 2) {
            newState = WFSTATE_STEP2POOL;
            correspondingState = WFSTATE_STEP2;
        } else if (step == 3) {
            newState = WFSTATE_STEP3POOL;
            correspondingState = WFSTATE_STEP3;
        } else {
            throw new IllegalArgumentException("Got a task to pool with an improperly or unknown state.");
        }

        // Gather our old owner and state as we need those as well to determine if we have to send notifies
        int oldState = workflowItem.getState();
        EPerson oldOwner = workflowItem.getOwner();

        // clear owner
        workflowItem.setOwner(null);
        // don't revoke the reviewer policies yet, they may be needed to advance the item

        // any approvers?
        // if so, add them to tasklist
        // if not, skip to next state
        Group workflowStepGroup = collectionService.getWorkflowGroup(collection, step);

        if ((workflowStepGroup != null) && !(groupService.isEmpty(workflowStepGroup)))
        {
            // set new item state
            workflowItem.setState(newState);

            // revoke previously granted reviewer policies and grant read permissions
            try
            {
                context.turnOffAuthorisationSystem();
                // revoke previously granted policies
                revokeReviewerPolicies(context, workflowItem.getItem());

                // JSPUI offers a preview to every task before a reviewer claims it.
                // So we need to grant permissions in advance, so that all possible reviewers can read the item and all
                // bitstreams in the bundle "ORIGINAL".
                authorizeService.addPolicy(context, workflowItem.getItem(), Constants.READ, workflowStepGroup, ResourcePolicy.TYPE_WORKFLOW);
                Bundle originalBundle;
                try {
                    originalBundle = itemService.getBundles(workflowItem.getItem(), "ORIGINAL").get(0);
                } catch (IndexOutOfBoundsException ex) {
                    originalBundle = null;
                }
                if (originalBundle != null)
                {
                    authorizeService.addPolicy(context, originalBundle, Constants.READ, workflowStepGroup, ResourcePolicy.TYPE_WORKFLOW);
                    for (Bitstream bitstream : originalBundle.getBitstreams())
                    {
                        authorizeService.addPolicy(context, bitstream, Constants.READ, workflowStepGroup, ResourcePolicy.TYPE_WORKFLOW);
                    }
                }
            } finally {
                context.restoreAuthSystemState();
            }

            // get a list of all epeople in group (or any subgroups)
            List<EPerson> epa = groupService.allMembers(context, workflowStepGroup);

            // there were reviewers, change the state
            // and add them to the list
            createTasks(context, workflowItem, epa);

            if (configurationService.getBooleanProperty("workflow.notify.returned.tasks", true)
                    || oldState != correspondingState
                    || oldOwner == null)
            {
                // email notification
                notifyGroupOfTask(context, workflowItem, workflowStepGroup, epa);
            }
            logWorkflowEvent(context, workflowItem.getItem(), workflowItem, context.getCurrentUser(), newState, null, collection, oldState, workflowStepGroup);
            return false;
        }
        else
        {
            // no reviewers, skip ahead
            workflowItem.setState(correspondingState);
            boolean archived = advance(context, workflowItem, null, true, false);
            if (archived)
            {
                // remove any workflow policies that may have left over
                try {
                    context.turnOffAuthorisationSystem();
                    revokeReviewerPolicies(context, workflowItem.getItem());
                } finally {
                    context.restoreAuthSystemState();
                }
            }
            return archived;
        }
    }

    protected void logWorkflowEvent(Context context, Item item, BasicWorkflowItem workflowItem, EPerson actor, int newstate, EPerson newOwner, Collection mycollection, int oldState, Group newOwnerGroup) {
        if(newstate == WFSTATE_ARCHIVE || newstate == WFSTATE_STEP1POOL || newstate == WFSTATE_STEP2POOL || newstate == WFSTATE_STEP3POOL){
            //Clear the newowner variable since this one isn't owned anymore !
            newOwner = null;
        }

        UsageWorkflowEvent usageWorkflowEvent = new UsageWorkflowEvent(context, item, workflowItem, workflowText[newstate], workflowText[oldState], mycollection, actor);
        if(newOwner != null){
            usageWorkflowEvent.setEpersonOwners(newOwner);
        }
        if(newOwnerGroup != null){
            usageWorkflowEvent.setGroupOwners(newOwnerGroup);
        }
        DSpaceServicesFactory.getInstance().getEventService().fireEvent(usageWorkflowEvent);
    }

    @Override
    public String getWorkflowText(int state)
    {
        if (state > -1 && state < workflowText.length) {
            return workflowText[state];
        }

        throw new IllegalArgumentException("Invalid workflow state passed");
    }

    /**
     * Commit the contained item to the main archive. The item is associated
     * with the relevant collection, added to the search index, and any other
     * tasks such as assigning dates are performed.
     *
     * @return the fully archived item.
     */
    protected Item archive(Context context, BasicWorkflowItem workflowItem)
            throws SQLException, IOException, AuthorizeException
    {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();

        log.info(LogManager.getHeader(context, "archive_item", "workflow_item_id="
                + workflowItem.getID() + "item_id=" + item.getID() + "collection_id="
                + collection.getID()));

        installItemService.installItem(context, workflowItem);

        // Log the event
        log.info(LogManager.getHeader(context, "install_item", "workflow_id="
                + workflowItem.getID() + ", item_id=" + item.getID() + "handle=FIXME"));

        return item;
    }

    /**
     * notify the submitter that the item is archived
     */
    protected void notifyOfArchive(Context context, Item item, Collection coll)
            throws SQLException, IOException
    {
        try
        {
            // Get submitter
            EPerson ep = item.getSubmitter();
            // Get the Locale
            Locale supportedLocale = I18nUtil.getEPersonLocale(ep);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_archive"));

            // Get the item handle to email to user
            String handle = handleService.findHandle(context, item);

            // Get title
            String title = item.getName();
            if (StringUtils.isBlank(title))
            {
                try
                {
                    title = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
                }
                catch (MissingResourceException e)
                {
                    title = "Untitled";
                }
            }

            email.addRecipient(ep.getEmail());
            email.addArgument(title);
            email.addArgument(coll.getName());
            email.addArgument(handleService.getCanonicalForm(handle));

            email.send();
        }
        catch (MessagingException e)
        {
            log.warn(LogManager.getHeader(context, "notifyOfArchive",
                    "cannot email user; item_id=" + item.getID()
                    + ":  " + e.getMessage()));
        }
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
     */
    protected WorkspaceItem returnToWorkspace(Context c, BasicWorkflowItem wfi)
            throws SQLException, IOException, AuthorizeException
    {
        // Regarding auth: this method is protected.
        // Authorization should be checked in all public methods calling this one.
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
        log.info(LogManager.getHeader(c, "return_to_workspace",
                "workflow_item_id=" + wfi.getID() + "workspace_item_id="
                        + workspaceItem.getID()));

        // Now remove the workflow object manually from the database
        workflowItemService.deleteWrapper(c, wfi);

        return workspaceItem;
    }



    @Override
    public WorkspaceItem sendWorkflowItemBackSubmission(Context context, BasicWorkflowItem workflowItem, EPerson ePerson,
            String provenancePrefix, String rejection_message) throws SQLException, AuthorizeException,
            IOException
    {
        int oldState = workflowItem.getState();

        switch(oldState)
        {
            case WFSTATE_STEP1:
                authorizeService.authorizeActionBoolean(context, ePerson, workflowItem.getItem(), Constants.WORKFLOW_STEP_1, true);
                break;
            case WFSTATE_STEP2:
                authorizeService.authorizeActionBoolean(context, ePerson, workflowItem.getItem(), Constants.WORKFLOW_STEP_2, true);
                break;
            case WFSTATE_STEP3:
                authorizeService.authorizeActionBoolean(context, ePerson, workflowItem.getItem(), Constants.WORKFLOW_STEP_3, true);
                break;
            default:
                throw new IllegalArgumentException("Workflow Step " + oldState + " is out of range.");
        }


        // stop workflow
        taskListItemService.deleteByWorkflowItem(context, workflowItem);

        // rejection provenance
        Item myitem = workflowItem.getItem();

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = getEPersonName(ePerson);

        // Here's what happened
        String provDescription = "Rejected by " + usersName + ", reason: "
                + rejection_message + " on " + now + " (GMT) ";

        // Add to item as a DC field
        itemService.addMetadata(context, myitem, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        itemService.update(context, myitem);

        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(context, workflowItem);

        // notify that it's been rejected
        notifyOfReject(context, workflowItem, ePerson, rejection_message);

        log.info(LogManager.getHeader(context, "reject_workflow", "workflow_item_id="
                + workflowItem.getID() + "item_id=" + workflowItem.getItem().getID()
                + "collection_id=" + workflowItem.getCollection().getID() + "eperson_id="
                + ePerson.getID()));

        logWorkflowEvent(context, wsi.getItem(), workflowItem, ePerson, WFSTATE_SUBMIT, null, wsi.getCollection(), oldState, null);

        return wsi;
    }

    // creates workflow tasklist entries for a workflow
    // for all the given EPeople
    protected void createTasks(Context c, BasicWorkflowItem wi, List<EPerson> epa)
            throws SQLException
    {
        // create a tasklist entry for each eperson
        for (EPerson anEpa : epa) {
            // can we get away without creating a tasklistitem class?
            // do we want to?
            taskListItemService.create(c, wi, anEpa);
        }
    }

    // send notices of curation activity
    @Override
    public void notifyOfCuration(Context c, BasicWorkflowItem wi, List<EPerson> ePeople,
           String taskName, String action, String message) throws SQLException, IOException
    {
        try
        {
            // Get the item title
            String title = getItemTitle(wi);

            // Get the submitter's name
            String submitter = getSubmitterName(wi);

            // Get the collection
            Collection coll = wi.getCollection();

            for (EPerson epa : ePeople)
            {
                Locale supportedLocale = I18nUtil.getEPersonLocale(epa);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale,
                                                                                  "flowtask_notify"));
                email.addArgument(title);
                email.addArgument(coll.getName());
                email.addArgument(submitter);
                email.addArgument(taskName);
                email.addArgument(message);
                email.addArgument(action);
                email.addRecipient(epa.getEmail());
                email.send();
            }
        }
        catch (MessagingException e)
        {
            log.warn(LogManager.getHeader(c, "notifyOfCuration",
                    "cannot email users of workflow_item_id " + wi.getID()
                            + ":  " + e.getMessage()));
        }
    }

    protected void notifyGroupOfTask(Context c, BasicWorkflowItem wi,
            Group mygroup, List<EPerson> epa) throws SQLException, IOException
    {
        // check to see if notification is turned off
        // and only do it once - delete key after notification has
        // been suppressed for the first time
        UUID myID = wi.getItem().getID();

        if (noEMail.containsKey(myID))
        {
            // suppress email, and delete key
            noEMail.remove(myID);
        }
        else
        {
            try
            {
                // Get the item title
                String title = getItemTitle(wi);

                // Get the submitter's name
                String submitter = getSubmitterName(wi);

                // Get the collection
                Collection coll = wi.getCollection();

                String message = "";

                for (EPerson anEpa : epa)
                {
                    Locale supportedLocale = I18nUtil.getEPersonLocale(anEpa);
                    Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_task"));
                    email.addArgument(title);
                    email.addArgument(coll.getName());
                    email.addArgument(submitter);

                    ResourceBundle messages = ResourceBundle.getBundle("Messages", supportedLocale);
                    switch (wi.getState())
                    {
                        case WFSTATE_STEP1POOL:
                            message = messages.getString("org.dspace.workflow.WorkflowManager.step1");

                            break;

                        case WFSTATE_STEP2POOL:
                            message = messages.getString("org.dspace.workflow.WorkflowManager.step2");

                            break;

                        case WFSTATE_STEP3POOL:
                            message = messages.getString("org.dspace.workflow.WorkflowManager.step3");

                            break;
                    }
                    email.addArgument(message);
                    email.addArgument(getMyDSpaceLink());
                    email.addRecipient(anEpa.getEmail());
                    email.send();
                }
            }
            catch (MessagingException e)
            {
                String gid = (mygroup != null) ?
                             String.valueOf(mygroup.getID()) : "none";
                log.warn(LogManager.getHeader(c, "notifyGroupofTask",
                        "cannot email user group_id=" + gid
                                + " workflow_item_id=" + wi.getID()
                                + ":  " + e.getMessage()));
            }
        }
    }

    @Override
    public String getMyDSpaceLink()
    {
        return configurationService.getProperty("dspace.url") + "/mydspace";
    }

    protected void notifyOfReject(Context context, BasicWorkflowItem workflowItem, EPerson e,
            String reason)
    {
        try
        {
            // Get the item title
            String title = getItemTitle(workflowItem);

            // Get the collection
            Collection coll = workflowItem.getCollection();

            // Get rejector's name
            String rejector = getEPersonName(e);
            Locale supportedLocale = I18nUtil.getEPersonLocale(e);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale,"submit_reject"));

            email.addRecipient(workflowItem.getSubmitter().getEmail());
            email.addArgument(title);
            email.addArgument(coll.getName());
            email.addArgument(rejector);
            email.addArgument(reason);
            email.addArgument(getMyDSpaceLink());

            email.send();
        }
        catch (RuntimeException re)
        {
            // log this email error
            log.warn(LogManager.getHeader(context, "notify_of_reject",
                    "cannot email user eperson_id=" + e.getID()
                            + " eperson_email=" + e.getEmail()
                            + " workflow_item_id=" + workflowItem.getID()
                            + ":  " + re.getMessage()));

            throw re;
        }
        catch (Exception ex)
        {
            // log this email error
            log.warn(LogManager.getHeader(context, "notify_of_reject",
                    "cannot email user eperson_id=" + e.getID()
                            + " eperson_email=" + e.getEmail()
                            + " workflow_item_id=" + workflowItem.getID()
                            + ":  " + ex.getMessage()));
        }
    }

    @Override
    public String getItemTitle(BasicWorkflowItem wi) throws SQLException
    {
        Item myitem = wi.getItem();
        String title = myitem.getName();

        // only return the first element, or "Untitled"
        if (StringUtils.isNotBlank(title))
        {
            return title;
        }
        else
        {
            return I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled ");
        }
    }

    @Override
    public String getSubmitterName(BasicWorkflowItem wi) throws SQLException
    {
        EPerson e = wi.getSubmitter();

        return getEPersonName(e);
    }

    protected String getEPersonName(EPerson e) throws SQLException
    {
        if (e == null)
        {
            return "Unknown";
        }
        String submitter = e.getFullName();

        submitter = submitter + " (" + e.getEmail() + ")";

        return submitter;
    }

    // Record approval provenance statement
    protected void recordApproval(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        Item item = workflowItem.getItem();

        // Get user's name + email address
        String usersName = getEPersonName(e);

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Here's what happened
        String provDescription = "Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // add bitstream descriptions (name, size, checksums)
        provDescription += installItemService.getBitstreamProvenanceMessage(context, item);

        // Add to item as a DC field
        itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        itemService.update(context, item);
    }

    // Create workflow start provenance message
    protected void recordStart(Context context, Item myitem)
            throws SQLException, IOException, AuthorizeException
    {
        // get date
        DCDate now = DCDate.getCurrent();

        // Create provenance description
        String provmessage;

        if (myitem.getSubmitter() != null)
        {
            provmessage = "Submitted by " + myitem.getSubmitter().getFullName()
                    + " (" + myitem.getSubmitter().getEmail() + ") on "
                    + now.toString() + "\n";
        }
        else
        // null submitter
        {
            provmessage = "Submitted by unknown (probably automated) on"
                    + now.toString() + "\n";
        }

        // add sizes and checksums of bitstreams
        provmessage += installItemService.getBitstreamProvenanceMessage(context, myitem);

        // Add message to the DC
        itemService.addMetadata(context, myitem, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provmessage);
        itemService.update(context, myitem);
    }

    @Override
    public void deleteCollection(Context context, Collection collection) throws SQLException, IOException, AuthorizeException {
        authorizeService.authorizeAction(context, collection, Constants.WRITE);
        collection.setWorkflowGroup(context, 1, null);
        collection.setWorkflowGroup(context, 2, null);
        collection.setWorkflowGroup(context, 3, null);
        workflowItemService.deleteByCollection(context, collection);
    }

    @Override
    public List<String> getEPersonDeleteConstraints(Context context, EPerson ePerson) throws SQLException {
        List<String> resultList = new ArrayList<>();
        List<BasicWorkflowItem> workflowItems = workflowItemService.findByOwner(context, ePerson);
        if(CollectionUtils.isNotEmpty(workflowItems))
        {
            resultList.add("workflowitem");
        }
        List<TaskListItem> taskListItems = taskListItemService.findByEPerson(context, ePerson);
        if(CollectionUtils.isNotEmpty(taskListItems))
        {
            resultList.add("tasklistitem");
        }
        return resultList;
    }

    @Override
    public Group getWorkflowRoleGroup(Context context, Collection collection, String roleName, Group roleGroup) throws SQLException, AuthorizeException {
        if ("WF_STEP1".equals(roleName))
        {
            roleGroup = collection.getWorkflowStep1();
            if (roleGroup == null)
                roleGroup = collectionService.createWorkflowGroup(context, collection, 1);

		}
		else if ("WF_STEP2".equals(roleName))
		{
            roleGroup = collection.getWorkflowStep2();
            if (roleGroup == null)
                roleGroup = collectionService.createWorkflowGroup(context, collection, 2);
        }
		else if ("WF_STEP3".equals(roleName))
		{
            roleGroup = collection.getWorkflowStep3();
            if (roleGroup == null)
                roleGroup = collectionService.createWorkflowGroup(context, collection, 3);

		}
        return roleGroup;
    }

    @Override
    public List<String> getFlywayMigrationLocations() {
        return Collections.emptyList();
    } // TODO
}
