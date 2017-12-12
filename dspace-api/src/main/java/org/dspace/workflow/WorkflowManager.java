/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.curate.WorkflowCurator;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.usage.UsageWorkflowEvent;
import org.dspace.utils.DSpace;

/**
 * Workflow state machine.
 *
 * <p>Notes:
 *
 * <p>Determining item status from the database:
 *
 * <ul>
 * <li>When an item has not been submitted yet, it is in the user's personal
 * workspace (there is a row in PersonalWorkspace pointing to it.)
 *
 * <li>When an item is submitted and is somewhere in a workflow, it has a row in the
 * WorkflowItem table pointing to it. The state of the workflow can be
 * determined by looking at {@link WorkflowItem#getState()}
 *
 * <li>When a submission is complete, the {@link WorkflowItem} pointing to the
 * item is destroyed and the
 * {@link #archive(org.dspace.core.Context, org.dspace.workflow.WorkflowItem)}
 * method is called, which hooks the item up to the archive.
 * </ul>
 *
 * <p>Notification: When an item enters a state that requires notification,
 * (WFSTATE_STEP1POOL, WFSTATE_STEP2POOL, WFSTATE_STEP3POOL,) the workflow needs
 * to notify the appropriate groups that they have a pending task to claim.
 *
 * <p>Revealing lists of approvers, editors, and reviewers. A method could be added
 * to do this, but it isn't strictly necessary. (say public List
 * getStateEPeople( WorkflowItem wi, int state ) could return people affected by
 * the item's current state.
 */
public class WorkflowManager
{
    // states to store in WorkflowItem for the GUI to report on
    // fits our current set of workflow states (stored in WorkflowItem.state)
    public static final int WFSTATE_SUBMIT = 0; // hmm, probably don't need

    public static final int WFSTATE_STEP1POOL = 1; // waiting for a reviewer to
                                                   // claim it

    public static final int WFSTATE_STEP1 = 2; // task - reviewer has claimed it

    public static final int WFSTATE_STEP2POOL = 3; // waiting for an admin to
                                                   // claim it

    public static final int WFSTATE_STEP2 = 4; // task - admin has claimed item

    public static final int WFSTATE_STEP3POOL = 5; // waiting for an editor to
                                                   // claim it

    public static final int WFSTATE_STEP3 = 6; // task - editor has claimed the
                                               // item

    public static final int WFSTATE_ARCHIVE = 7; // probably don't need this one
                                                 // either

    /** Symbolic names of workflow steps. */
    private static final String workflowText[] =
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
    private static final Map<Integer, Boolean> noEMail = new HashMap<Integer, Boolean>();

    /** log4j logger */
    private static final Logger log = Logger.getLogger(WorkflowManager.class);

    /**
     * Translate symbolic name of workflow state into number.
     * The name is case-insensitive.  Returns -1 when name cannot
     * be matched.
     * @param state symbolic name of workflow state, must be one of
     *        the elements of workflowText array.
     * @return numeric workflow state or -1 for error.
     */
    public static int getWorkflowID(String state)
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
     * startWorkflow() begins a workflow - in a single transaction do away with
     * the PersonalWorkspace entry and turn it into a WorkflowItem.
     *
     * @param c
     *            Context
     * @param wsi
     *            The WorkspaceItem to convert to a workflow item
     * @return The resulting workflow item
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    public static WorkflowItem start(Context c, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        Item myitem = wsi.getItem();
        Collection collection = wsi.getCollection();

        log.info(LogManager.getHeader(c, "start_workflow", "workspace_item_id="
                + wsi.getID() + "item_id=" + myitem.getID() + "collection_id="
                + collection.getID()));

        // record the start of the workflow w/provenance message
        recordStart(c, myitem);

        // create the WorkflowItem
        TableRow row = DatabaseManager.row("workflowitem");
        row.setColumn("item_id", myitem.getID());
        row.setColumn("collection_id", wsi.getCollection().getID());
        DatabaseManager.insert(c, row);

        WorkflowItem wfi = new WorkflowItem(c, row);

        wfi.setMultipleFiles(wsi.hasMultipleFiles());
        wfi.setMultipleTitles(wsi.hasMultipleTitles());
        wfi.setPublishedBefore(wsi.isPublishedBefore());

        // remove the WorkspaceItem
        wsi.deleteWrapper();

        // now get the workflow started
        wfi.setState(WFSTATE_SUBMIT);
        advance(c, wfi, null);

        // Return the workflow item
        return wfi;
    }

    /**
     * startWithoutNotify() starts the workflow normally, but disables
     * notifications (useful for large imports,) for the first workflow step -
     * subsequent notifications happen normally
     */
    public static WorkflowItem startWithoutNotify(Context c, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        // make a hash table entry with item ID for no notify
        // notify code checks no notify hash for item id
        noEMail.put(Integer.valueOf(wsi.getItem().getID()), Boolean.TRUE);

        return start(c, wsi);
    }

    /**
     * getOwnedTasks() returns a List of WorkflowItems containing the tasks
     * claimed and owned by an EPerson. The GUI displays this info on the
     * MyDSpace page.
     *
     * @param e
     *            The EPerson we want to fetch owned tasks for.
     */
    public static List<WorkflowItem> getOwnedTasks(Context c, EPerson e)
            throws java.sql.SQLException
    {
        ArrayList<WorkflowItem> mylist = new ArrayList<WorkflowItem>();

        String myquery = "SELECT * FROM WorkflowItem WHERE owner= ? ORDER BY workflow_id";

        TableRowIterator tri = DatabaseManager.queryTable(c,
        		"workflowitem", myquery,e.getID());

        try
        {
            while (tri.hasNext())
            {
                mylist.add(new WorkflowItem(c, tri.next()));
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }

        return mylist;
    }

    /**
     * getPooledTasks() returns a List of WorkflowItems an EPerson could claim
     * (as a reviewer, etc.) for display on a user's MyDSpace page.
     *
     * @param e
     *            The Eperson we want to fetch the pooled tasks for.
     */
    public static List<WorkflowItem> getPooledTasks(Context c, EPerson e) throws SQLException
    {
        ArrayList<WorkflowItem> mylist = new ArrayList<WorkflowItem>();

        String myquery = "SELECT workflowitem.* FROM workflowitem, TaskListItem" +
        		" WHERE tasklistitem.eperson_id= ? " +
        		" AND tasklistitem.workflow_id=workflowitem.workflow_id ORDER BY workflowitem.workflow_id";

        TableRowIterator tri = DatabaseManager
                .queryTable(c, "workflowitem", myquery, e.getID());

        try
        {
            while (tri.hasNext())
            {
                mylist.add(new WorkflowItem(c, tri.next()));
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }

        return mylist;
    }

    /**
     * claim() claims a workflow task for an EPerson
     *
     * @param c
     *            Current user context.
     * @param wi
     *            WorkflowItem to do the claim on
     * @param e
     *            The EPerson doing the claim
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static void claim(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = wi.getState();

        switch (taskstate)
        {
        case WFSTATE_STEP1POOL:

            // FIXME note:  authorizeAction ASSUMES that c.getCurrentUser() == e!
            AuthorizeManager.authorizeAction(c, wi.getCollection(), Constants.WORKFLOW_STEP_1, true);
            doState(c, wi, WFSTATE_STEP1, e);

            break;

        case WFSTATE_STEP2POOL:

            AuthorizeManager.authorizeAction(c, wi.getCollection(), Constants.WORKFLOW_STEP_2, true);
            doState(c, wi, WFSTATE_STEP2, e);

            break;

        case WFSTATE_STEP3POOL:

            AuthorizeManager.authorizeAction(c, wi.getCollection(), Constants.WORKFLOW_STEP_3, true);
            doState(c, wi, WFSTATE_STEP3, e);

            break;

        default:
            throw new IllegalArgumentException("Workflow Step " + taskstate + " is out of range.");
        }

        log.info(LogManager.getHeader(c, "claim_task", "workflow_item_id="
                + wi.getID() + "item_id=" + wi.getItem().getID()
                + "collection_id=" + wi.getCollection().getID()
                + "newowner_id=" + wi.getOwner().getID() + "old_state="
                + taskstate + "new_state=" + wi.getState()));
    }

    /**
     * advance() sends an item forward in the workflow (reviewers,
     * approvers, and editors all do an 'approve' to move the item forward) if
     * the item arrives at the submit state, then remove the WorkflowItem and
     * call the archive() method to put it in the archive, and email notify the
     * submitter of a successful submission
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem do do the approval on
     * @param e
     *            EPerson doing the approval
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static void advance(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        advance(c, wi, e, true, true);
    }

    /**
     * advance() sends an item forward in the workflow. Reviewers,
     * approvers, and editors all do an 'approve' to move the item forward.
     * If the item arrives at the submit state, then remove the WorkflowItem,
     * call the {@link #archive(org.dspace.core.Context, org.dspace.workflow.WorkflowItem)}
     * method to put it in the archive, and email notify the
     * submitter of a successful submission.
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem do do the approval on
     * @param e
     *            EPerson doing the approval
     *
     * @param curate
     *            boolean indicating whether curation tasks should be done
     *
     * @param record
     *            boolean indicating whether to record action
     * @return true if the state was advanced.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static boolean advance(Context c, WorkflowItem wi, EPerson e,
                                  boolean curate, boolean record)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = wi.getState();
        boolean archived = false;

        // perform curation tasks if needed
        if (curate && WorkflowCurator.needsCuration(wi))
        {
            if (! WorkflowCurator.doCuration(c, wi)) {
                // don't proceed - either curation tasks queued, or item rejected
                log.info(LogManager.getHeader(c, "advance_workflow",
                        "workflow_item_id=" + wi.getID() + ",item_id="
                        + wi.getItem().getID() + ",collection_id="
                        + wi.getCollection().getID() + ",old_state="
                        + taskstate + ",doCuration=false"));
                return archived;
            }
        }

        switch (taskstate)
        {
        case WFSTATE_SUBMIT:
            archived = doState(c, wi, WFSTATE_STEP1POOL, e);

            break;

        case WFSTATE_STEP1:
            // advance(...) will call itself if no workflow step group exists
            // so we need to check permissions only if a workflow step group is
            // in place.
            if (wi.getCollection().getWorkflowGroup(1) != null)
            {
                // FIXME note:  authorizeAction ASSUMES that c.getCurrentUser() == e!
                AuthorizeManager.authorizeAction(c, wi.getCollection(), Constants.WORKFLOW_STEP_1, true);
            }

            // Record provenance
            if (record)
            {
                recordApproval(c, wi, e);
            }
            archived = doState(c, wi, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP2:
            // advance(...) will call itself if no workflow step group exists
            // so we need to check permissions only if a workflow step group is
            // in place.
            if (wi.getCollection().getWorkflowGroup(2) != null)
            {
                AuthorizeManager.authorizeAction(c, wi.getCollection(), Constants.WORKFLOW_STEP_2, true);
            }

            // Record provenance
            if (record)
            {
                recordApproval(c, wi, e);
            }
            archived = doState(c, wi, WFSTATE_STEP3POOL, e);

            break;

        case WFSTATE_STEP3:
            // advance(...) will call itself if no workflow step group exists
            // so we need to check permissions only if a workflow step group is
            // in place.
            if (wi.getCollection().getWorkflowGroup(3) != null)
            {
                AuthorizeManager.authorizeAction(c, wi.getCollection(), Constants.WORKFLOW_STEP_3, true);
            }

            // We don't record approval for editors, since they can't reject,
            // and thus didn't actually make a decision
            archived = doState(c, wi, WFSTATE_ARCHIVE, e);

            break;

        // error handling? shouldn't get here
        }

        log.info(LogManager.getHeader(c, "advance_workflow",
                "workflow_item_id=" + wi.getID() + ",item_id="
                        + wi.getItem().getID() + ",collection_id="
                        + wi.getCollection().getID() + ",old_state="
                        + taskstate + ",new_state=" + wi.getState()));
        return archived;
    }

    /**
     * returns an owned task/item to the pool
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem to operate on
     * @param e
     *            EPerson doing the operation
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static void unclaim(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = wi.getState();

        switch (taskstate)
        {
        case WFSTATE_STEP1:

            doState(c, wi, WFSTATE_STEP1POOL, e);

            break;

        case WFSTATE_STEP2:

            doState(c, wi, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP3:

            doState(c, wi, WFSTATE_STEP3POOL, e);

            break;

        default:
            throw new IllegalStateException("WorkflowItem reached an unknown state.");
        }

        try {
            c.turnOffAuthorisationSystem();
            wi.update();
        } finally {
            c.restoreAuthSystemState();
        }

        log.info(LogManager.getHeader(c, "unclaim_workflow",
                "workflow_item_id=" + wi.getID() + ",item_id="
                        + wi.getItem().getID() + ",collection_id="
                        + wi.getCollection().getID() + ",old_state="
                        + taskstate + ",new_state=" + wi.getState()));
    }

    /**
     * abort() aborts a workflow, completely deleting it (administrator do this)
     * (it will basically do a reject from any state - the item ends up back in
     * the user's PersonalWorkspace
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem to operate on
     * @param e
     *            EPerson doing the operation
     */
    public static void abort(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, AuthorizeException, IOException
    {
        // authorize a DSpaceActions.ABORT
        if (!AuthorizeManager.isAdmin(c))
        {
            throw new AuthorizeException(
                    "You must be an admin to abort a workflow");
        }

        // stop workflow regardless of its state
        deleteTasks(c, wi);

        log.info(LogManager.getHeader(c, "abort_workflow", "workflow_item_id="
                + wi.getID() + "item_id=" + wi.getItem().getID()
                + "collection_id=" + wi.getCollection().getID() + "eperson_id="
                + e.getID()));

        // convert into personal workspace
        returnToWorkspace(c, wi);
    }

    /**
     * Move a workflow item to a new state.  The item may be put in a pool,
     * removed from a pool and assigned to a user, or archived.
     *
     * @param c current DSpace context.
     * @param wi workflow item whose state should transition.
     * @param newstate move {@link wi} to this state.
     * @param newowner assign {@link wi} to this user.
     * @return true if archived.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     * @throws AuthorizeException passed through.
     */
    private static boolean doState(Context c, WorkflowItem wi, int newstate,
            EPerson newowner) throws SQLException, IOException,
            AuthorizeException
    {
        Collection mycollection = wi.getCollection();

        //Gather our old data for launching the workflow event
        int oldState = wi.getState();

        wi.setState(newstate);

        boolean archived;
        switch (newstate)
        {
        case WFSTATE_STEP1POOL:
            archived = pool(c, wi, 1);
            break;

        case WFSTATE_STEP1:
            assignToReviewer(c, wi, 1, newowner);
            archived = false;
            break;

        case WFSTATE_STEP2POOL:
            archived = pool(c, wi, 2);
            break;

        case WFSTATE_STEP2:
            assignToReviewer(c, wi, 2, newowner);
            archived = false;
            break;

        case WFSTATE_STEP3POOL:
            archived = pool(c, wi, 3);
            break;

        case WFSTATE_STEP3:
            assignToReviewer(c, wi, 3, newowner);
            archived = false;
            break;

        case WFSTATE_ARCHIVE:
            // put in archive in one transaction
            // remove workflow tasks
            deleteTasks(c, wi);
            mycollection = wi.getCollection();
            Item myItem = archive(c, wi);

            // now email notification
            notifyOfArchive(c, myItem, mycollection);

            // remove any workflow policies left
            try {
                c.turnOffAuthorisationSystem();
                revokeReviewerPolicies(c, myItem);
            } finally {
                c.restoreAuthSystemState();
            }

            logWorkflowEvent(c, wi.getItem(), wi, c.getCurrentUser(), newstate,
                    newowner, mycollection, oldState, null);
            return true;
        default:
            throw new IllegalArgumentException("WorkflowManager cannot handle workflowItemState " + newstate);
        }

        try {
            c.turnOffAuthorisationSystem();
            wi.update();
        } finally {
            c.restoreAuthSystemState();
        }
        return archived;
    }

    /**
     * Assign this workflow item to a reviewer.
     *
     * @param context current DSpace context.
     * @param workflowItem the item to be assigned.
     * @param step review step.
     * @param newowner the reviewer to be assigned.
     * @throws AuthorizeException passed through.
     * @throws SQLException passed through.
     * @throws IllegalArgumentException if {@code step} is unknown.
     */
    protected static void assignToReviewer(Context context, WorkflowItem workflowItem,
            int step, EPerson newowner)
            throws AuthorizeException, SQLException
    {
        // shortcut to the collection
        Collection collection = workflowItem.getCollection();
        // from the step we can recognize the new state and the corresponding policy action.
        int newState;
        int correspondingAction;
        switch (step)
        {
        case 1:
            newState = WFSTATE_STEP1;
            correspondingAction = Constants.WORKFLOW_STEP_1;
            break;
        case 2:
            newState = WFSTATE_STEP2;
            correspondingAction = Constants.WORKFLOW_STEP_2;
            break;
        case 3:
            newState = WFSTATE_STEP3;
            correspondingAction = Constants.WORKFLOW_STEP_3;
            break;
        default:
            throw new IllegalArgumentException("Unknown workflow step " + step);
        }

        // Gather the old state for logging.
        int oldState = workflowItem.getState();

        // If there is a workflow state group and it contains any members,
        // then we have to check the permissions first.
        Group stateGroup = collection.getWorkflowGroup(step);
        if ((stateGroup != null) && !(stateGroup.isEmpty()))
        {
            // FIXME note:  authorizeAction ASSUMES that c.getCurrentUser() == newowner!
            AuthorizeManager.authorizeAction(context, collection, correspondingAction, true);
        }

        // Give the owner the appropriate permissions.
        try {
            context.turnOffAuthorisationSystem();
            // maybe unnecessary, but revoke any perviously granted permissions.
            revokeReviewerPolicies(context, workflowItem.getItem());
            // Finally grant the new permissions.
            grantReviewerPolicies(context, workflowItem, newowner);
        } finally {
            context.restoreAuthSystemState();
        }

        // Remove task from tasklist as someone is working on it now.
        deleteTasks(context, workflowItem);
        // Assign new owner.
        workflowItem.setState(newState);
        workflowItem.setOwner(newowner);

        logWorkflowEvent(context, workflowItem.getItem(), workflowItem,
                context.getCurrentUser(), newState, newowner, collection, oldState, null);
    }

    /**
     * Helper method that manages state, policies, owner, notifies, task list items
     * and so on whenever a WorkflowItem should be added to a workflow step pool.
     * Don't use this method directly.  Either use
     * {@link #unclaim(Context, WorkflowItem, EPerson)} if the item is claimed,
     * {@link #start(Context, WorkspaceItem)} to start the workflow, or
     * {@link #advance(Context, WorkflowItem, EPerson)} to move an item to the next state.
     *
     * @param context DSpace context object.
     * @param workflowItem the item to be pooled.
     * @param step the step (1-3) of the pool the item should be put into.
     * @return true if the item was archived because no reviewers were assigned
     *         to any of the following workflow steps, false otherwise.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws IllegalArgumentException if {@code step} has another value than
     *         either 1, 2, or 3.
     */
    protected static boolean pool(Context context, WorkflowItem workflowItem, int step)
            throws SQLException, AuthorizeException, IOException
    {
        // shortcut to the collection
        Collection collection = workflowItem.getCollection();
        
        // From the step we can recognize the new state and the corresponding state.
        // The new state is the pool of the step.
        // The corresponding state is the state an item gets when it is claimed.
        // That is important to recognize if we have to send notifications
        // and if we have to skip a pool.
        int newState;
        int correspondingState;
        switch (step)
        {
        case 1:
            newState = WFSTATE_STEP1POOL;
            correspondingState = WFSTATE_STEP1;
            break;
        case 2:
            newState = WFSTATE_STEP2POOL;
            correspondingState = WFSTATE_STEP2;
            break;
        case 3:
            newState = WFSTATE_STEP3POOL;
            correspondingState = WFSTATE_STEP3;
            break;
        default:
            throw new IllegalArgumentException("Unknown workflow step " + step);
        }
        
        // Gather our old owner and state, as we need those as well to determine
        // whether we have to send notifications.
        int oldState = workflowItem.getState();
        EPerson oldOwner = workflowItem.getOwner();
        // Clear owner.
        workflowItem.setOwner(null);
        // Don't revoke the reviewer policies yet.  They may be needed to advance the item.
        
        // Any approvers?  If so, add them to the tasklist; if not, skip to next state.
        Group workflowStepGroup = collection.getWorkflowGroup(step);
        if ((workflowStepGroup != null) && !(workflowStepGroup.isEmpty()))
        {
            // Set new item state.
            workflowItem.setState(newState);
            
            // Revoke previously granted reviewer policies and grant read permissions.
            try {
                context.turnOffAuthorisationSystem();
                // Revoke previously granted policies.
                revokeReviewerPolicies(context, workflowItem.getItem());
                
                // JSPUI offers a preview to every task before a reviewer claims it.
                // So we need to grant permissions in advance, so that all
                // possible reviewers can read the item and all bitstreams in
                // the bundle "ORIGINAL".
                AuthorizeManager.addPolicy(context, workflowItem.getItem(),
                        Constants.READ, workflowStepGroup,
                        ResourcePolicy.TYPE_WORKFLOW);
                Bundle originalBundle;
                try {
                    originalBundle = workflowItem.getItem().getBundles("ORIGINAL")[0];
                } catch (IndexOutOfBoundsException ex) {
                    originalBundle = null;
                }
                if (originalBundle != null)
                {
                    AuthorizeManager.addPolicy(context, originalBundle, Constants.READ,
                            workflowStepGroup, ResourcePolicy.TYPE_WORKFLOW);
                    for (Bitstream bitstream : originalBundle.getBitstreams())
                    {
                        AuthorizeManager.addPolicy(context, bitstream, Constants.READ,
                                workflowStepGroup, ResourcePolicy.TYPE_WORKFLOW);
                    }
                }
            } finally {
                context.restoreAuthSystemState();
            }
            
            // Get a list of all epeople in group (or any subgroups)
            EPerson[] epa = Group.allMembers(context, workflowStepGroup);
            
            // There were reviewers.  Change the state and then add them to the list.
            createTasks(context, workflowItem, epa);
            ConfigurationService configurationService = new DSpace().getConfigurationService();
            if (configurationService.getPropertyAsType("workflow.notify.returned.tasks", true)
                    || oldState != correspondingState
                    || oldOwner == null)
            {
                // Email notification
                notifyGroupOfTask(context, workflowItem, workflowStepGroup, epa);
            }
            logWorkflowEvent(context, workflowItem.getItem(), workflowItem,
                    context.getCurrentUser(), newState, null, collection,
                    oldState, workflowStepGroup);
            return false;
        }
        else
        {
            // No reviewers -- skip ahead.
            workflowItem.setState(correspondingState);
            boolean archived = advance(context, workflowItem, null, true, false);
            if (archived)
            {
                // Remove any workflow policies that may be left over.
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

    private static void logWorkflowEvent(Context c, Item item, WorkflowItem workflowItem, EPerson actor, int newstate, EPerson newOwner, Collection mycollection, int oldState, Group newOwnerGroup) {
        if(newstate == WFSTATE_ARCHIVE || newstate == WFSTATE_STEP1POOL || newstate == WFSTATE_STEP2POOL || newstate == WFSTATE_STEP3POOL){
            //Clear the newowner variable since this one isn't owned anymore !
            newOwner = null;
        }

        UsageWorkflowEvent usageWorkflowEvent = new UsageWorkflowEvent(c, item, workflowItem, workflowText[newstate], workflowText[oldState], mycollection, actor);
        if(newOwner != null){
            usageWorkflowEvent.setEpersonOwners(newOwner);
        }
        if(newOwnerGroup != null){
            usageWorkflowEvent.setGroupOwners(newOwnerGroup);
        }
        new DSpace().getEventService().fireEvent(usageWorkflowEvent);
    }

    /**
     * Get the text representing the given workflow state
     *
     * @param state the workflow state
     * @return the text representation
     */
    public static String getWorkflowText(int state)
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
    private static Item archive(Context c, WorkflowItem wfi)
            throws SQLException, IOException, AuthorizeException
    {
        // FIXME: Check auth
        Item item = wfi.getItem();
        Collection collection = wfi.getCollection();

        log.info(LogManager.getHeader(c, "archive_item", "workflow_item_id="
                + wfi.getID() + "item_id=" + item.getID() + "collection_id="
                + collection.getID()));

        InstallItem.installItem(c, wfi);

        // Log the event
        log.info(LogManager.getHeader(c, "install_item", "workflow_id="
                + wfi.getID() + ", item_id=" + item.getID() + "handle=FIXME"));

        return item;
    }

    /**
     * notify the submitter that the item is archived
     */
    private static void notifyOfArchive(Context c, Item i, Collection coll)
            throws SQLException, IOException
    {
        try
        {
            // Get submitter
            EPerson ep = i.getSubmitter();
            // Get the Locale
            Locale supportedLocale = I18nUtil.getEPersonLocale(ep);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_archive"));

            // Get the item handle to email to user
            String handle = HandleManager.findHandle(c, i);

            // Get title
            Metadatum[] titles = i.getDC("title", null, Item.ANY);
            String title = "";
            try
            {
                title = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                title = "Untitled";
            }
            if (titles.length > 0)
            {
                title = titles[0].value;
            }

            email.addRecipient(ep.getEmail());
            email.addArgument(title);
            email.addArgument(coll.getMetadata("name"));
            email.addArgument(HandleManager.getCanonicalForm(handle));

            email.send();
        }
        catch (MessagingException e)
        {
            log.warn(LogManager.getHeader(c, "notifyOfArchive",
                    "cannot email user; item_id=" + i.getID()
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
    private static WorkspaceItem returnToWorkspace(Context c, WorkflowItem wfi)
            throws SQLException, IOException, AuthorizeException
    {
        Item myitem = wfi.getItem();
        Collection mycollection = wfi.getCollection();

        // Regarding auth:  this method s private.
        // Authorization should be checked in all public methods calling this one.
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
     * @param rejection_message
     *            message to email to user
     */
    public static WorkspaceItem reject(Context c, WorkflowItem wi, EPerson e,
            String rejection_message) throws SQLException, AuthorizeException,
            IOException
    {

        int oldState = wi.getState();
        // authorize a DSpaceActions.REJECT
        // stop workflow
        deleteTasks(c, wi);

        // rejection provenance
        Item myitem = wi.getItem();

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = getEPersonName(e);

        // Here's what happened
        String provDescription = "Rejected by " + usersName + ", reason: "
                + rejection_message + " on " + now + " (GMT) ";

        // Add to item as a DC field
        myitem.addDC("description", "provenance", "en", provDescription);
        myitem.update();

        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);

        // notify that it's been rejected
        notifyOfReject(c, wi, e, rejection_message);

        log.info(LogManager.getHeader(c, "reject_workflow", "workflow_item_id="
                + wi.getID() + "item_id=" + wi.getItem().getID()
                + "collection_id=" + wi.getCollection().getID() + "eperson_id="
                + e.getID()));

        logWorkflowEvent(c, wsi.getItem(), wi, e, WFSTATE_SUBMIT, null, wsi.getCollection(), oldState, null);

        return wsi;
    }

    // creates workflow tasklist entries for a workflow
    // for all the given EPeople
    private static void createTasks(Context c, WorkflowItem wi, EPerson[] epa)
            throws SQLException
    {
        // create a tasklist entry for each eperson
        for (int i = 0; i < epa.length; i++)
        {
            // can we get away without creating a tasklistitem class?
            // do we want to?
            TableRow tr = DatabaseManager.row("tasklistitem");
            tr.setColumn("eperson_id", epa[i].getID());
            tr.setColumn("workflow_id", wi.getID());
            DatabaseManager.insert(c, tr);
        }
    }

    /** Deletes all tasks associated with a workflowitem. */
    static void deleteTasks(Context c, WorkflowItem wi) throws SQLException
    {
        String myrequest = "DELETE FROM TaskListItem WHERE workflow_id= ? ";

        DatabaseManager.updateQuery(c, myrequest, wi.getID());
    }

    // send notices of curation activity
    public static void notifyOfCuration(Context c, WorkflowItem wi, EPerson[] epa,
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

            for (int i = 0; i < epa.length; i++)
            {
                Locale supportedLocale = I18nUtil.getEPersonLocale(epa[i]);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale,
                                                                                  "flowtask_notify"));
                email.addArgument(title);
                email.addArgument(coll.getMetadata("name"));
                email.addArgument(submitter);
                email.addArgument(taskName);
                email.addArgument(message);
                email.addArgument(action);
                email.addRecipient(epa[i].getEmail());
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

    private static void notifyGroupOfTask(Context c, WorkflowItem wi,
            Group mygroup, EPerson[] epa) throws SQLException, IOException
    {
        // check to see if notification is turned off
        // and only do it once - delete key after notification has
        // been suppressed for the first time
        Integer myID = Integer.valueOf(wi.getItem().getID());

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

                for (int i = 0; i < epa.length; i++)
                {
                    Locale supportedLocale = I18nUtil.getEPersonLocale(epa[i]);
                    Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_task"));
                    email.addArgument(title);
                    email.addArgument(coll.getMetadata("name"));
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
                    email.addRecipient(epa[i].getEmail());
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

    private static String getMyDSpaceLink()
    {
        return ConfigurationManager.getProperty("dspace.url") + "/mydspace";
    }

    private static void notifyOfReject(Context c, WorkflowItem wi, EPerson e,
            String reason)
    {
        try
        {
            // Get the item title
            String title = getItemTitle(wi);

            // Get the collection
            Collection coll = wi.getCollection();

            // Get rejector's name
            String rejector = getEPersonName(e);
            Locale supportedLocale = I18nUtil.getEPersonLocale(e);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale,"submit_reject"));

            email.addRecipient(getSubmitterEPerson(wi).getEmail());
            email.addArgument(title);
            email.addArgument(coll.getMetadata("name"));
            email.addArgument(rejector);
            email.addArgument(reason);
            email.addArgument(getMyDSpaceLink());

            email.send();
        }
        catch (RuntimeException re)
        {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user eperson_id=" + e.getID()
                            + " eperson_email=" + e.getEmail()
                            + " workflow_item_id=" + wi.getID()
                            + ":  " + re.getMessage()));

            throw re;
        }
        catch (Exception ex)
        {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user eperson_id=" + e.getID()
                            + " eperson_email=" + e.getEmail()
                            + " workflow_item_id=" + wi.getID()
                            + ":  " + ex.getMessage()));
        }
    }

    // FIXME - are the following methods still needed?
    private static EPerson getSubmitterEPerson(WorkflowItem wi)
            throws SQLException
    {
        EPerson e = wi.getSubmitter();

        return e;
    }

    /**
     * get the title of the item in this workflow
     *
     * @param wi  the workflow item object
     */
    public static String getItemTitle(WorkflowItem wi) throws SQLException
    {
        Item myitem = wi.getItem();
        Metadatum[] titles = myitem.getDC("title", null, Item.ANY);

        // only return the first element, or "Untitled"
        if (titles.length > 0)
        {
            return titles[0].value;
        }
        else
        {
            return I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled ");
        }
    }

    /**
     * get the name of the eperson who started this workflow
     *
     * @param wi  the workflow item
     * @return "user name (email@address)"
     * @throws java.sql.SQLException passed through.
     */
    public static String getSubmitterName(WorkflowItem wi) throws SQLException
    {
        EPerson e = wi.getSubmitter();

        return getEPersonName(e);
    }

    private static String getEPersonName(EPerson e) throws SQLException
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
    private static void recordApproval(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        Item item = wi.getItem();

        // Get user's name + email address
        String usersName = getEPersonName(e);

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Here's what happened
        String provDescription = "Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // add bitstream descriptions (name, size, checksums)
        provDescription += InstallItem.getBitstreamProvenanceMessage(item);

        // Add to item as a DC field
        item.addDC("description", "provenance", "en", provDescription);
        item.update();
    }

    // Create workflow start provenance message
    private static void recordStart(Context c, Item myitem)
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
                    + now.toString() + "\n";
        }
        else
        // null submitter
        {
            provmessage = "Submitted by unknown (probably automated) on"
                    + now.toString() + "\n";
        }

        // add sizes and checksums of bitstreams
        provmessage += InstallItem.getBitstreamProvenanceMessage(myitem);

        // Add message to the DC
        myitem.addDC("description", "provenance", "en", provmessage);
        myitem.update();
    }

    /**
     * This method grants the appropriate permissions to reviewers so that they
     * can read and edit metadata and read files and edit files if allowed by
     * configuration.
     * <p>
     * In most cases this method must be called within a try-finally-block that
     * temporarily disables the authentication system. That is not done by this
     * method as it should be done carefully and only in contexts in which
     * granting the permissions is authorized by some previous checks.
     *
     * @param context
     * @param wfi While all policies are granted on item, bundle or bitstream
     *            level, this method takes a {@link WorkflowItem} for convenience and
     *            uses wfi.getItem() to get the actual item.
     * @param reviewer EPerson to grant the rights to.
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected static void grantReviewerPolicies(Context context, WorkflowItem wfi, EPerson reviewer)
            throws SQLException, AuthorizeException
    {
        // get item and bundle "ORIGINAL"
        Item item = wfi.getItem();
        Bundle originalBundle;
        try {
            originalBundle = item.getBundles("ORIGINAL")[0];
        } catch (IndexOutOfBoundsException ex) {
            originalBundle = null;
        }

        // grant item level policies
        for (int action : new int[] {Constants.READ, Constants.WRITE, Constants.ADD, Constants.REMOVE, Constants.DELETE})
        {
            AuthorizeManager.addPolicy(context, item, action, reviewer, ResourcePolicy.TYPE_WORKFLOW);
        }

        // set bitstream and bundle policies
        if (originalBundle != null)
        {
            AuthorizeManager.addPolicy(context, originalBundle, Constants.READ, reviewer, ResourcePolicy.TYPE_WORKFLOW);

            // shall reviewers be able to edit files?
            ConfigurationService configurationService = new DSpace().getConfigurationService();
            boolean editFiles = Boolean.parseBoolean(configurationService.getProperty("workflow.reviewer.file-edit"));
            // if a reviewer should be able to edit bitstreams, we need add
            // permissions regarding the bundle "ORIGINAL" and its bitstreams
            if (editFiles)
            {
                AuthorizeManager.addPolicy(context, originalBundle, Constants.ADD, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(context, originalBundle, Constants.REMOVE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                // Whenever a new bitstream is added, it inherit the policies of the bundle.
                // So we need to add all policies newly created bitstreams should get.
                AuthorizeManager.addPolicy(context, originalBundle, Constants.WRITE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(context, originalBundle, Constants.DELETE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
            }
            for (Bitstream bitstream : originalBundle.getBitstreams())
            {
                AuthorizeManager.addPolicy(context, bitstream, Constants.READ, reviewer, ResourcePolicy.TYPE_WORKFLOW);

                // add further rights if reviewer should be able to edit bitstreams
                if (editFiles)
                {
                    AuthorizeManager.addPolicy(context, bitstream, Constants.WRITE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                    AuthorizeManager.addPolicy(context, bitstream, Constants.DELETE, reviewer, ResourcePolicy.TYPE_WORKFLOW);
                }
            }
        }
    }

    /**
     * This method revokes any permission granted by the basic workflow system
     * on the item specified as attribute. At time of writing this method these
     * permissions will all be granted by
     * {@link #grantReviewerPolicies(org.dspace.core.Context, org.dspace.workflowbasic.BasicWorkflowItem, org.dspace.eperson.EPerson)}.
     * <p>
     * In most cases this method must be called within a try-finally-block that
     * temporarily disables the authentication system. That is not done by this
     * method as it should be done carefully and only in contexts in which
     * revoking the permissions is authorized by some previous checks.
     *
     * @param context
     * @param item
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    protected static void revokeReviewerPolicies(Context context, Item item)
            throws SQLException, AuthorizeException
    {
        // get bundle "ORIGINAL"
        Bundle originalBundle;
        try {
            originalBundle = item.getBundles("ORIGINAL")[0];
        } catch (IndexOutOfBoundsException ex) {
            originalBundle = null;
        }

        // remove bitstream and bundle level policies
        if (originalBundle != null)
        {
            // We added policies for Bitstreams of the bundle "original" only
            for (Bitstream bitstream : originalBundle.getBitstreams())
            {
                AuthorizeManager.removeAllPoliciesByDSOAndType(context, bitstream, ResourcePolicy.TYPE_WORKFLOW);
            }

            AuthorizeManager.removeAllPoliciesByDSOAndType(context, originalBundle, ResourcePolicy.TYPE_WORKFLOW);
        }

        // remove item level policies
        AuthorizeManager.removeAllPoliciesByDSOAndType(context, item, ResourcePolicy.TYPE_WORKFLOW);
     }
}
