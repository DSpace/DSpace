/*
 * WorkflowManager
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.workflow;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.sql.SQLException;
import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.LogManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.InstallItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.history.HistoryManager;
import org.dspace.search.DSIndexer;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;




/**
 * Workflow state machine
 *
 * Notes:
 *
 * Determining item status from the database:
 *
 * When an item has not been submitted yet, it is in the user's
 * personal workspace (there is a row in PersonalWorkspace pointing
 * to it.)
 *
 * When an item is submitted and is somewhere in a workflow, it has
 * a row in the WorkflowItem table pointing to it.  The state of the
 * workflow can be determined by looking at WorkflowItem.getState()
 *
 * When a submission is complete, the WorkflowItem pointing to the item
 * is destroyed and SubmitServlet.insertItem() is called, which hooks
 * the item up to the archive.
 *
 * Notification:
 *  When an item enters a state that requires notification, (WFSTATE_STEP1POOL,
 *  WFSTATE_STEP2POOL, WFSTATE_STEP3POOL,) the workflow needs to notify
 *  the appropriate groups that they have a pending task to claim.
 *
 * Revealing lists of approvers, editors, and reviewers.  A method could
 * be added to do this, but it isn't strictly necessary.
 * (say public List getStateEPeople( WorkflowItem wi, int state ) could
 * return people affected by the item's current state.
 */
public class WorkflowManager
{
	// states to store in WorkflowItem for the GUI to report on
	// fits our current set of workflow states (stored in WorkflowItem.state)
	public static final int WFSTATE_SUBMIT		= 0; // hmm, probably don't need
	public static final int WFSTATE_STEP1POOL	= 1; // waiting for a reviewer to claim it
	public static final int WFSTATE_STEP1		= 2; // task - reviewer has claimed it
	public static final int WFSTATE_STEP2POOL	= 3; // waiting for an admin to claim it
	public static final int WFSTATE_STEP2		= 4; // task - admin has claimed item
	public static final int WFSTATE_STEP3POOL	= 5; // waiting for an editor to claim it
	public static final int WFSTATE_STEP3		= 6; // task - editor has claimed the item
	public static final int WFSTATE_ARCHIVE		= 7; // probably don't need this one either

    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkflowManager.class);

	/** startWorkflow() begins a workflow - in a single transaction
	 *   do away with the PersonalWorkspace entry and turn it into
	 *   a WorkflowItem.
	 *
     * @param c  Context
	 * @param pw The PersonalWorkspace to convert to a workflow item
	 * @return   The resulting workflow item
	 */
    public static WorkflowItem start(Context c,WorkspaceItem wsi)
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME Check auth

        Item myitem = wsi.getItem();
        Collection collection = wsi.getCollection();
                
        log.info(LogManager.getHeader(c,
            "start_workflow",
            "workspace_item_id=" + wsi.getID() +
                "item_id="       + myitem.getID() +
                "collection_id=" + collection.getID()));

        // record the start of the workflow w/provenance message
        recordStart(c, myitem);
        
        // create the WorkflowItem
        TableRow row = DatabaseManager.create(c, "workflowitem");
        row.setColumn( "item_id", myitem.getID() );
        row.setColumn( "collection_id", wsi.getCollection().getID() );
                
        WorkflowItem wfi = new WorkflowItem(c, row);
        
        wfi.setMultipleFiles  ( wsi.hasMultipleFiles()  );
        wfi.setMultipleTitles ( wsi.hasMultipleTitles() );
        wfi.setPublishedBefore( wsi.isPublishedBefore() );

        // Write history creation event
        HistoryManager.saveHistory(c,
            wfi,
            HistoryManager.CREATE,
            c.getCurrentUser(),
            c.getExtraLogInfo());

        // remove the WorkspaceItem
        wsi.deleteWrapper();

        // now get the worflow started
        doState(c, wfi, WFSTATE_STEP1POOL, null);

        // Return the workflow item
        return wfi;
    }


	/** getOwnedTasks() returns a List of WorkflowItems containing
	 *  the tasks claimed and owned by an EPerson.  The GUI displays
	 *  this info on the MyDSpace page.
	 * @param e The EPerson we want to fetch owned tasks for.
	 */
    public static List getOwnedTasks(Context c, EPerson e)
        throws java.sql.SQLException
    {
        ArrayList mylist = new ArrayList();

        String myquery = "SELECT * FROM WorkflowItem WHERE owner="
            + e.getID();

        TableRowIterator tri = DatabaseManager.query( c, "workflowitem", myquery );
        
        while (tri.hasNext())
        {
            mylist.add( new WorkflowItem( c, tri.next() ) );
        }

        return mylist;
    }
    
    
	/** getPooledTasks() returns a List of WorkflowItems an EPerson could claim
	 *   (as a reviewer, etc.) for display on a user's MyDSpace page.
	 * @param e The Eperson we want to fetch the pooled tasks for.
	 */
    public static List getPooledTasks(Context c, EPerson e)
        throws SQLException
    {
        ArrayList mylist = new ArrayList();

        String myquery =
            "SELECT workflowitem.* FROM workflowitem, TaskListItem"
            + " WHERE tasklistitem.eperson_id=" + e.getID()
            + " AND tasklistitem.workflow_id=workflowitem.workflow_id";
            
        TableRowIterator tri = DatabaseManager.query(c, "workflowitem", myquery);

        while( tri.hasNext() )
        {
            mylist.add( new WorkflowItem( c, tri.next() ) );
        }

        return mylist;
    }


	/** claim() claims a workflow task for an EPerson
	 * @param wi WorkflowItem to do the claim on
	 * @param e  The EPerson doing the claim
	 */
    public static void claim(Context c,WorkflowItem wi, EPerson e)
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
        log.info(LogManager.getHeader(c,
                    "claim_task",
                    "workflow_item_id="  + wi.getID()                 +
                        "item_id="       + wi.getItem().getID()       +
                        "collection_id=" + wi.getCollection().getID() +
                        "newowner_id="   + wi.getOwner().getID()      +
                        "old_state="     + taskstate                  +
                        "new_state="     + wi.getState()
                        )
                );
    }

	/** approveAction() sends an item forward in the workflow (reviewers, approvers, and
	 *   editors all do an 'approve' to move the item forward)
	 *   if the item arrives at the submit state, then remove the WorkflowItem
	 *   and call SubmitServlet.insertItem() to put it in the archive,
	 *   and email notify the submitter of a successful submission
     *
     * @param c  Context
	 * @param wi WorkflowItem do do the approval on
	 * @param e  EPerson doing the approval
	 */
    public static void advance(Context c,WorkflowItem wi, EPerson e)
        throws SQLException, IOException, AuthorizeException
    {
        int taskstate = wi.getState();

        switch (taskstate)
        {
            case WFSTATE_STEP1:
                // authorize DSpaceActions.SUBMIT_REVIEW
                // Record provenance
                recordApproval(c, wi, e);
                doState(c, wi, WFSTATE_STEP2POOL, e);
                break;

            case WFSTATE_STEP2:
                // authorize DSpaceActions.SUBMIT_STEP2
                // Record provenance
                recordApproval(c, wi, e);
                doState(c, wi, WFSTATE_STEP3POOL, e);
                break;

            case WFSTATE_STEP3:
                // authorize DSpaceActions.SUBMIT_STEP3
                // We don't record approval for editors, since they can't reject,
                // and thus didn't actually make a decision
                doState(c, wi, WFSTATE_ARCHIVE, e);
                break;

                // error handling?  shouldn't get here
        }
        log.info(LogManager.getHeader(c,
                    "advance_workflow",
                    "workflow_item_id="  + wi.getID()                 +
                        ",item_id="       + wi.getItem().getID()       +
                        ",collection_id=" + wi.getCollection().getID() +
                        ",old_state="     + taskstate                  +
                        ",new_state="     + wi.getState()
                        )
                );

    }
    
    
	/** unclaim() returns an owned task/item to the pool
     * @param c  Context
	 * @param wi WorkflowItem to operate on
	 * @param e  EPerson doing the operation
	 */
    public static void unclaim(Context c,WorkflowItem wi, EPerson e)
        throws SQLException, IOException, AuthorizeException
    {

        int taskstate = wi.getState();

        switch( taskstate )
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
    
                // error handling?  shouldn't get here
                // FIXME - what to do with error - log it?
        }

        log.info(LogManager.getHeader(c,
                    "unclaim_workflow",
                    "workflow_item_id="  + wi.getID()                 +
                        ",item_id="       + wi.getItem().getID()       +
                        ",collection_id=" + wi.getCollection().getID() +
                        ",old_state="     + taskstate                  +
                        ",new_state="     + wi.getState()
                        )
                );
    }


	/** abort() aborts a workflow, completely deleting it (administrator do this)
     *   (it will basically do a reject from any state - the item
	 *   ends up back in the user's PersonalWorkspace
     *
     * @param c  Context
	 * @param wi WorkflowItem to operate on
	 * @param e  EPerson doing the operation
	 */
    public static void abort(Context c,WorkflowItem wi, EPerson e)
        throws SQLException, AuthorizeException, IOException
    {
        // authorize a DSpaceActions.ABORT
        if( !AuthorizeManager.isAdmin(c) )
        {
            throw new AuthorizeException(
                "You must be an admin to abort a workflow" );
        }

        // stop workflow regardless of its state
        deleteTasks(c, wi);

        log.info(LogManager.getHeader(c,
                    "abort_workflow",
                    "workflow_item_id="  + wi.getID()                 +
                        "item_id="       + wi.getItem().getID()       +
                        "collection_id=" + wi.getCollection().getID() +
                        "eperson_id="    + e.getID()
                        )
                );

        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);
    }

    // returns true if archived

    private static boolean doState(Context c,WorkflowItem wi, int newstate, EPerson newowner)
        throws SQLException, IOException, AuthorizeException
    {
        Collection mycollection = wi.getCollection();
        Group mygroup = null;
        boolean archived = false;
                
        wi.setState(newstate);

        switch (newstate)
        {
            case WFSTATE_STEP1POOL:
                // any reviewers?
                // if so, add them to the tasklist
                wi.setOwner( null );

                // get reviewers (group 1 )
                mygroup = mycollection.getWorkflowGroup( 1 );
                if (mygroup != null )
                {
                    // there were reviewers, change the state
                    //  and add them to the list
                    createTasks(c, wi, mygroup);
                    wi.update();

                    // email notification
                    notifyGroupOfTask(c, mygroup, wi);
                }
                else
                {
                    // no reviewers, skip ahead
                    archived = doState(c, wi, WFSTATE_STEP2POOL, null);
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
            wi.setOwner( null );

            // get approvers (group 2)
            mygroup = mycollection.getWorkflowGroup( 2 );
            if( mygroup != null )
            {
                // there were approvers, change the state
                //  timestamp, and add them to the list
                createTasks(c, wi, mygroup );

                // email notification
                notifyGroupOfTask(c, mygroup, wi);
            }
            else
            {
                // no reviewers, skip ahead
                archived = doState(c, wi, WFSTATE_STEP3POOL, null);
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
            wi.setOwner( null );

            mygroup = mycollection.getWorkflowGroup( 3 );
            
            if( mygroup != null )
            {
                // there were editors, change the state
                //  timestamp, and add them to the list
                createTasks(c, wi, mygroup);

                // email notification
                notifyGroupOfTask(c, mygroup, wi);
            }
            else
            {
                // no editors, skip ahead
                archived = doState(c, wi, WFSTATE_ARCHIVE, newowner);
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
            try
            {
                // remove workflow tasks
                deleteTasks(c, wi);
                
                mycollection = wi.getCollection();
                Item myitem = archive(c, wi);

                // now email notification
                notifyOfArchive(c, myitem, mycollection);
                archived = true;
            }
            catch(IOException e)
            {
                // indexer causes this
                throw e;
            }
            catch (SQLException e)
            {
                // problem starting workflow
                throw e;
            }

            break;
        }

        if( (wi != null) && !archived ) wi.update();
        
        return archived;
    }


    
    /**
     * Commit the contained item to the main archive.  The item is
     * associated with the relevant collection, added to the search index,
     * and any other tasks such as assigning dates are performed.
     *
     * @return  the fully archived item.
     */
    
    private static Item archive(Context c, WorkflowItem wfi)
        throws SQLException, IOException, AuthorizeException
    {
        // FIXME: Check auth

        Item item = wfi.getItem();
        Collection collection = wfi.getCollection();

        log.info(LogManager.getHeader(c,
            "archive_item",
            "workflow_item_id=" + wfi.getID() + "item_id=" + item.getID() +
                "collection_id=" + collection.getID()));

        InstallItem.installItem(c, wfi);

        // Log the event
        log.info(LogManager.getHeader(
            c,
            "install_item",
            "workflow_id=" + wfi.getID() + ", item_id=" + item.getID() +
            "handle=FIXME")
        );

        return item;
    }   


    /**
     * notify the submitter that the item is archived
     */
    private static void notifyOfArchive(Context c, Item i,
            Collection coll)
        throws SQLException, IOException
    {
        try
        {
            // Get the item handle to email to user
            String handle = HandleManager.findHandle(c, i);
            
            // Get title
            DCValue titles[] = i.getDC("title", null, Item.ANY);
            String title = "Untitled";

            if (titles.length > 0) title = titles[0].value;

            // Get submitter
            EPerson ep = i.getSubmitter();

            Email email = ConfigurationManager.getEmail(
                 "submit_archive" );
        
            email.addRecipient( ep.getEmail() );

            email.addArgument( title );
            email.addArgument( coll.getMetadata("name")               );
            email.addArgument( HandleManager.getCanonicalForm(handle) );
            
            email.send();
        }
        catch( MessagingException e )
        {
            log.warn(LogManager.getHeader(c,
                        "notifyOfArchive",
                        "cannot email user"
                            + " item_id=" + i.getID()
                        )
                    );
        }
    }


    /**
     * Return the workflow item to the workspace of the submitter.
     * The workflow item is removed, and a workspace item created.
     * 
     * @param c Context
     * @param wfi WorkflowItem to be 'dismantled'
     * @return  the workspace item
     */
    private static WorkspaceItem returnToWorkspace(Context c,
             WorkflowItem wfi)
        throws SQLException, IOException, AuthorizeException
    {
        Item myitem = wfi.getItem();
        Collection mycollection = wfi.getCollection();
        
        // FIXME: How should this interact with the workflow system?
        // FIXME: Remove license
        // FIXME: Provenance statement?
        
        // Create the new workspace item row
        TableRow row = DatabaseManager.create(c, "workspaceitem");
        row.setColumn("item_id", myitem.getID());
        row.setColumn("collection_id", mycollection.getID());
        DatabaseManager.update(c, row);

        int wsi_id = row.getIntColumn("workspace_item_id");
        WorkspaceItem wi = WorkspaceItem.find(c, wsi_id);
        wi.setMultipleFiles  (wfi.hasMultipleFiles() );
        wi.setMultipleTitles (wfi.hasMultipleTitles());
        wi.setPublishedBefore(wfi.isPublishedBefore());
        wi.update();

        // remove any licenses that the item may have been given
        myitem.removeLicenses();
        //myitem.update();
        
        log.info(LogManager.getHeader(c,
            "return_to_workspace",
            "workflow_item_id=" + wfi.getID() +
            "workspace_item_id=" + wi.getID()));

        // Now remove the workflow object manually from the database
        DatabaseManager.updateQuery(c,
            "DELETE FROM WorkflowItem WHERE workflow_id=" +
               	wfi.getID() );

        return wi;
    }


	/** rejects an item - rejection means undoing
	 *  a submit - WorkspaceItem is created, and the WorkflowItem
	 *  is removed, user is emailed rejection_message.
     *
     * @param c  Context
	 * @param wi WorkflowItem to operate on
	 * @param e  EPerson doing the operation
	 * @param rejection_message message to email to user
	 */
    public static WorkspaceItem reject(Context c,WorkflowItem wi, EPerson e,
             String rejection_message)
        throws SQLException, AuthorizeException, IOException
    {
        // authorize a DSpaceActions.REJECT

        // stop workflow
        deleteTasks(c, wi);


        // rejection provenance
        Item myitem = wi.getItem();

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = getEPersonName( e );

        // Here's what happened
        String provDescription = "Rejected by " + usersName
            + ", reason: " + rejection_message
            + " on " + now + " (GMT) ";
            
        // Add to item as a DC field
        myitem.addDC("description", "provenance", null, provDescription);
        myitem.update();

        // convert into personal workspace
        WorkspaceItem wsi = returnToWorkspace(c, wi);

        // notify that it's been rejected
        notifyOfReject(c, wi, e, rejection_message);

        log.info(LogManager.getHeader(c,
                    "reject_workflow",
                    "workflow_item_id="  + wi.getID()                 +
                        "item_id="       + wi.getItem().getID()       +
                        "collection_id=" + wi.getCollection().getID() +
                        "eperson_id="    + e.getID()
                        )
                );


        return wsi;
    }
    

    // creates workflow tasklist entries for a workflow
    //  from a given eperson group
    private static void createTasks(Context c, WorkflowItem wi,
            Group eg)
        throws SQLException
    {
        // get a list of epeople
        EPerson [] epa = eg.getMembers();
        
        // create a tasklist entry for each eperson
        for( int i=0; i<epa.length; i++ )
        {
            // can we get away without creating a tasklistitem class?
            // do we want to?
            TableRow tr = DatabaseManager.create(c, "tasklistitem");
            tr.setColumn( "eperson_id",  epa[i].getID() );
            tr.setColumn( "workflow_id", wi.getID()     );
            DatabaseManager.update( c, tr );
        }
    }


    // deletes all tasks associated with a workflowitem
    private static void deleteTasks(Context c,WorkflowItem wi)
        throws SQLException
    {
        String myrequest =
            "DELETE FROM TaskListItem WHERE workflow_id="
            + wi.getID();
        
        DatabaseManager.updateQuery( c, myrequest );
    }


    private static void notifyGroupOfTask(Context c, Group mygroup,
            WorkflowItem wi)
        throws SQLException, IOException
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

            switch (wi.getState())
            {
                case WFSTATE_STEP1POOL:
                    message = "It requires reviewing.";
                    break;

                case WFSTATE_STEP2POOL:
                    message = "The submission must be checked before inclusion in the archive.";
                    break;

                case WFSTATE_STEP3POOL:
                    message = "The metadata needs to be checked to ensure compliance with the " +
                                "collection's standards, and edited if necessary.";
                    break;
            }

            Email email = ConfigurationManager.getEmail("submit_task");
        
            email.addArgument( title                    );
            email.addArgument( coll.getMetadata("name") );
            email.addArgument( submitter                );
            email.addArgument( message                  );
            email.addArgument( getMyDSpaceLink()        );

            emailGroup(c, mygroup, email);
        }
        catch( MessagingException e )
        {
            log.warn(LogManager.getHeader(c,
                "notifyGroupofTask",
                "cannot email user"
                + " group_id" + mygroup.getID()
                + " workflow_item_id" + wi.getID() ));
        }
    }


    /**
     * Add the members of a group to the recipeients of an email, and send it
     * @param c Context
     * @param mygroup Group to get members from
     * @param email Email object containing the message
     */
    private static void emailGroup(Context c, Group mygroup,
            Email email )
        throws SQLException, MessagingException
    {
        // send message to each member of the group
        EPerson [] epa = mygroup.getMembers();
        
        for( int i = 0; i < epa.length; i++ )
        {
            email.addRecipient( epa[i].getEmail() );
        }
        
        email.send();
    }


    private static String getMyDSpaceLink()
    {
        return ConfigurationManager.getProperty( "dspace.url" ) + "/mydspace";
    }

    
    private static void notifyOfReject(Context c,WorkflowItem wi,
            EPerson e, String reason)
    {
        try
        {
            // Get the item title
            String title = getItemTitle(wi);

            // Get the collection
            Collection coll = wi.getCollection();

            // Get rejector's name
            String rejector = getEPersonName(e);

            Email email = ConfigurationManager.getEmail("submit_reject");
        
            email.addRecipient( getSubmitterEPerson(wi).getEmail() );
            email.addArgument( title                    );
            email.addArgument( coll.getMetadata("name") );
            email.addArgument( rejector                 );
            email.addArgument( reason                   );
            email.addArgument( getMyDSpaceLink()        );
        
            email.send();
        }
        catch( Exception ex )
        {
            // log this email error
            log.warn(LogManager.getHeader(c,
                "notify_of_reject",
                "cannot email user"
                + " eperson_id"       + e.getID()
                + " eperson_email"    + e.getEmail()
                + " workflow_item_id" + wi.getID() ));

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
     * @param workflowitem
     */
    public static String getItemTitle(WorkflowItem wi)
        throws SQLException
    {
        Item   myitem   = wi.getItem();
        DCValue titles[] = myitem.getDC("title", null, Item.ANY);

        // only return the first element, or "Untitled"
        if( titles.length > 0 )
            return titles[0].value;
        else
            return "Untitled";
    }

    /**
     * get the name of the eperson who started this workflow
     *
     * @param workflowitem
     */
    public static String getSubmitterName(WorkflowItem wi)
        throws SQLException
    {
        EPerson e = wi.getSubmitter();

        return getEPersonName( e );
    }


    private static String getEPersonName(EPerson e)
        throws SQLException
    {
        String submitter = e.getFullName();

        submitter = submitter + "(" + e.getEmail() + ")";

        return submitter;
    }


    // Record approval provenance statement
    private static void recordApproval(Context c,WorkflowItem wi,
            EPerson e)
        throws SQLException, AuthorizeException
    {
        Item item = wi.getItem();

        // Get user's name + email address
        String usersName = getEPersonName( e );

        // Get current date
        String now = DCDate.getCurrent().toString();

        // Here's what happened
        String provDescription = "Approved for entry into archive by " +
            usersName + " on " + now + " (GMT) ";
            
        // add bitstream descriptions (name, size, checksums)
        provDescription += InstallItem.getBitstreamProvenanceMessage(item);    

        // Add to item as a DC field
        item.addDC("description", "provenance", null, provDescription);
        item.update();
    }


    // Create workflow start provenance message
    private static void recordStart(Context c, Item myitem)
        throws SQLException, AuthorizeException
    {
        // Get non-internal format bitstreams
        Bitstream[] bitstreams = myitem.getNonInternalBitstreams();

        // get date
        DCDate now = DCDate.getCurrent();

        // Create provenance description
        String provmessage = "";
        
        if( myitem.getSubmitter() != null )
        {
            provmessage = "Submitted by " + 
            myitem.getSubmitter().getFullName() + " (" +
            myitem.getSubmitter().getEmail() + ") on " + now.toString() + "\n";
        }
        else // null submitter
        {
            provmessage = "Submitted by unknown (probably automated) on" + 
            now.toString() + "\n";
        }

        // add sizes and checksums of bitstreams
        provmessage += InstallItem.getBitstreamProvenanceMessage(myitem);                    

        // Add message to the DC
        myitem.addDC("description", "provenance", "en", provmessage);
        myitem.update();
    }
}
