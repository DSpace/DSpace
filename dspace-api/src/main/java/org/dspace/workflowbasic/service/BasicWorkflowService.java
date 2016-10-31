/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflowbasic.BasicWorkflowItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
public interface BasicWorkflowService extends WorkflowService<BasicWorkflowItem>{

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

    /**
     * Translate symbolic name of workflow state into number.
     * The name is case-insensitive.  Returns -1 when name cannot
     * be matched.
     * @param state symbolic name of workflow state, must be one of
     *        the elements of workflowText array.
     * @return numeric workflow state or -1 for error.
     */
    public int getWorkflowID(String state);

    /**
     * getOwnedTasks() returns a List of WorkflowItems containing the tasks
     * claimed and owned by an EPerson. The GUI displays this info on the
     * MyDSpace page.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param e
     *     The EPerson we want to fetch owned tasks for.
     * @return list of basic workflow items
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<BasicWorkflowItem> getOwnedTasks(Context context, EPerson e)
            throws java.sql.SQLException;

    /**
     * getPooledTasks() returns a List of WorkflowItems an EPerson could claim
     * (as a reviewer, etc.) for display on a user's MyDSpace page.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param e
     *     The Eperson we want to fetch the pooled tasks for.
     * @return list of basic workflow items
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<BasicWorkflowItem> getPooledTasks(Context context, EPerson e) throws SQLException;

    /**
     * claim() claims a workflow task for an EPerson
     *
     * @param context
     *     The relevant DSpace Context.
     * @param workflowItem
     *     WorkflowItem to do the claim on
     * @param e
     *     The EPerson doing the claim
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void claim(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException;


    /**
     * advance() sends an item forward in the workflow (reviewers,
     * approvers, and editors all do an 'approve' to move the item forward) if
     * the item arrives at the submit state, then remove the WorkflowItem and
     * call the archive() method to put it in the archive, and email notify the
     * submitter of a successful submission
     *
     * @param context
     *     The relevant DSpace Context.
     * @param workflowItem
     *     WorkflowItem do do the approval on
     * @param e
     *     EPerson doing the approval
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void advance(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException;

    /**
     * advance() sends an item forward in the workflow (reviewers,
     * approvers, and editors all do an 'approve' to move the item forward) if
     * the item arrives at the submit state, then remove the WorkflowItem and
     * call the archive() method to put it in the archive, and email notify the
     * submitter of a successful submission
     *
     * @param context
     *     The relevant DSpace Context.
     * @param workflowItem
     *     WorkflowItem do do the approval on
     * @param e
     *     EPerson doing the approval
     * @param curate
     *     boolean indicating whether curation tasks should be done
     * @param record
     *     boolean indicating whether to record action
     * @return true if the item was successfully archived
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public boolean advance(Context context, BasicWorkflowItem workflowItem, EPerson e,
                                  boolean curate, boolean record)
            throws SQLException, IOException, AuthorizeException;

    /**
     * unclaim() returns an owned task/item to the pool
     *
     * @param context
     *            Context
     * @param workflowItem
     *            WorkflowItem to operate on
     * @param e
     *            EPerson doing the operation
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void unclaim(Context context, BasicWorkflowItem workflowItem, EPerson e)
            throws SQLException, IOException, AuthorizeException;

    /**
     * Get the text representing the given workflow state
     *
     * @param state the workflow state
     * @return the text representation
     */
    public String getWorkflowText(int state);


        // send notices of curation activity
    public void notifyOfCuration(Context c, BasicWorkflowItem wi, List<EPerson> ePeople,
           String taskName, String action, String message) throws SQLException, IOException;

    /**
     * get the title of the item in this workflow
     *
     * @param wi  the workflow item object
     * @return item title
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public String getItemTitle(BasicWorkflowItem wi) throws SQLException;

    /**
     * get the name of the eperson who started this workflow
     *
     * @param wi  the workflow item
     * @return submitter's name
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public String getSubmitterName(BasicWorkflowItem wi) throws SQLException;
}
