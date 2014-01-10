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
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.curate.WorkflowCurator;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.usage.UsageWorkflowEvent;
import org.dspace.utils.DSpace;

/**
 * Workflow state machine
 *
 * Notes:
 *
 * Determining item status from the database:
 *
 * When an item has not been submitted yet, it is in the user's personal
 * workspace (there is a row in PersonalWorkspace pointing to it.)
 *
 * When an item is submitted and is somewhere in a workflow, it has a row in the
 * WorkflowItem table pointing to it. The state of the workflow can be
 * determined by looking at WorkflowItem.getState()
 *
 * When a submission is complete, the WorkflowItem pointing to the item is
 * destroyed and the archive() method is called, which hooks the item up to the
 * archive.
 *
 * Notification: When an item enters a state that requires notification,
 * (WFSTATE_STEP1POOL, WFSTATE_STEP2POOL, WFSTATE_STEP3POOL,) the workflow needs
 * to notify the appropriate groups that they have a pending task to claim.
 *
 * Revealing lists of approvers, editors, and reviewers. A method could be added
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
    private static Map<Integer, Boolean> noEMail = new HashMap<Integer, Boolean>();

    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkflowManager.class);

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
     */
    public static WorkflowItem start(Context c, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        // FIXME Check auth
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

        String myquery = "SELECT * FROM WorkflowItem WHERE owner= ? ";

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
        		" AND tasklistitem.workflow_id=workflowitem.workflow_id";

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
     * @param wi
     *            WorkflowItem to do the claim on
     * @param e
     *            The EPerson doing the claim
     */
    public static void claim(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = wi.getState();

        switch (taskstate)
        {
        case WFSTATE_STEP1POOL:

            // authorize DSpaceActions.SUBMIT_REVIEW
            doState(c, wi, WFSTATE_STEP1, e);

            break;

        case WFSTATE_STEP2POOL:

            // authorize DSpaceActions.SUBMIT_STEP2
            doState(c, wi, WFSTATE_STEP2, e);

            break;

        case WFSTATE_STEP3POOL:

            // authorize DSpaceActions.SUBMIT_STEP3
            doState(c, wi, WFSTATE_STEP3, e);

            break;

        // if we got here, we weren't pooled... error?
        // FIXME - log the error?
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
     */
    public static void advance(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        advance(c, wi, e, true, true);
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
     *
     * @param curate
     *            boolean indicating whether curation tasks should be done
     *
     * @param record
     *            boolean indicating whether to record action
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

            // authorize DSpaceActions.SUBMIT_REVIEW
            // Record provenance
            if (record)
            {
                recordApproval(c, wi, e);
            }
            archived = doState(c, wi, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP2:

            // authorize DSpaceActions.SUBMIT_STEP2
            // Record provenance
            if (record)
            {
                recordApproval(c, wi, e);
            }
            archived = doState(c, wi, WFSTATE_STEP3POOL, e);

            break;

        case WFSTATE_STEP3:

            // authorize DSpaceActions.SUBMIT_STEP3
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
     * unclaim() returns an owned task/item to the pool
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem to operate on
     * @param e
     *            EPerson doing the operation
     */
    public static void unclaim(Context c, WorkflowItem wi, EPerson e)
            throws SQLException, IOException, AuthorizeException
    {
        int taskstate = wi.getState();

        switch (taskstate)
        {
        case WFSTATE_STEP1:

            // authorize DSpaceActions.STEP1
            doState(c, wi, WFSTATE_STEP1POOL, e);

            break;

        case WFSTATE_STEP2:

            // authorize DSpaceActions.APPROVE
            doState(c, wi, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP3:

            // authorize DSpaceActions.STEP3
            doState(c, wi, WFSTATE_STEP3POOL, e);

            break;

        // error handling? shouldn't get here
        // FIXME - what to do with error - log it?
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

    // returns true if archived
    private static boolean doState(Context c, WorkflowItem wi, int newstate,
            EPerson newowner) throws SQLException, IOException,
            AuthorizeException
    {
        Collection mycollection = wi.getCollection();
        Group mygroup = null;
        boolean archived = false;

        //Gather our old data for launching the workflow event
        int oldState = wi.getState();

        wi.setState(newstate);

        switch (newstate)
        {
        case WFSTATE_STEP1POOL:

            // any reviewers?
            // if so, add them to the tasklist
            wi.setOwner(null);

            // get reviewers (group 1 )
            mygroup = mycollection.getWorkflowGroup(1);

            if ((mygroup != null) && !(mygroup.isEmpty()))
            {
                // get a list of all epeople in group (or any subgroups)
                EPerson[] epa = Group.allMembers(c, mygroup);

                // there were reviewers, change the state
                //  and add them to the list
                createTasks(c, wi, epa);
                wi.update();

                // email notification
                notifyGroupOfTask(c, wi, mygroup, epa);
            }
            else
            {
                // no reviewers, skip ahead
                wi.setState(WFSTATE_STEP1);
                archived = advance(c, wi, null, true, false);
            }

            break;

        case WFSTATE_STEP1:

            // remove reviewers from tasklist
            // assign owner
            deleteTasks(c, wi);
            wi.setOwner(newowner);

            break;

        case WFSTATE_STEP2POOL:

            // clear owner
            // any approvers?
            // if so, add them to tasklist
            // if not, skip to next state
            wi.setOwner(null);

            // get approvers (group 2)
            mygroup = mycollection.getWorkflowGroup(2);

            if ((mygroup != null) && !(mygroup.isEmpty()))
            {
                //get a list of all epeople in group (or any subgroups)
                EPerson[] epa = Group.allMembers(c, mygroup);

                // there were approvers, change the state
                //  timestamp, and add them to the list
                createTasks(c, wi, epa);

                // email notification
                notifyGroupOfTask(c, wi, mygroup, epa);
            }
            else
            {
                // no reviewers, skip ahead
                wi.setState(WFSTATE_STEP2);
                archived = advance(c, wi, null, true, false);
            }

            break;

        case WFSTATE_STEP2:

            // remove admins from tasklist
            // assign owner
            deleteTasks(c, wi);
            wi.setOwner(newowner);

            break;

        case WFSTATE_STEP3POOL:

            // any editors?
            // if so, add them to tasklist
            wi.setOwner(null);
            mygroup = mycollection.getWorkflowGroup(3);

            if ((mygroup != null) && !(mygroup.isEmpty()))
            {
                // get a list of all epeople in group (or any subgroups)
                EPerson[] epa = Group.allMembers(c, mygroup);

                // there were editors, change the state
                //  timestamp, and add them to the list
                createTasks(c, wi, epa);

                // email notification
                notifyGroupOfTask(c, wi, mygroup, epa);
            }
            else
            {
                // no editors, skip ahead
                wi.setState(WFSTATE_STEP3);
                archived = advance(c, wi, null, true, false);
            }

            break;

        case WFSTATE_STEP3:

            // remove editors from tasklist
            // assign owner
            deleteTasks(c, wi);
            wi.setOwner(newowner);

            break;

        case WFSTATE_ARCHIVE:

            // put in archive in one transaction
            // remove workflow tasks
            deleteTasks(c, wi);

            mycollection = wi.getCollection();

            Item myitem = archive(c, wi);

            // now email notification
            notifyOfArchive(c, myitem, mycollection);
            archived = true;

            break;
        }

        logWorkflowEvent(c, wi.getItem(), wi, c.getCurrentUser(), newstate, newowner, mycollection, oldState, mygroup);

        if (!archived)
        {
            wi.update();
        }

        return archived;
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
            DCValue[] titles = i.getDC("title", null, Item.ANY);
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
                    "cannot email user" + " item_id=" + i.getID()));
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

    // deletes all tasks associated with a workflowitem
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
            log.warn(LogManager.getHeader(c, "notifyOfCuration", "cannot email users" +
                                          " of workflow_item_id" + wi.getID()));
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
                        "cannot email user" + " group_id" + gid
                                + " workflow_item_id" + wi.getID()));
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
                    "cannot email user" + " eperson_id" + e.getID()
                            + " eperson_email" + e.getEmail()
                            + " workflow_item_id" + wi.getID()));

            throw re;
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
        DCValue[] titles = myitem.getDC("title", null, Item.ANY);

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
     */
    public static String getSubmitterName(WorkflowItem wi) throws SQLException
    {
        EPerson e = wi.getSubmitter();

        return getEPersonName(e);
    }

    private static String getEPersonName(EPerson e) throws SQLException
    {
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
}
