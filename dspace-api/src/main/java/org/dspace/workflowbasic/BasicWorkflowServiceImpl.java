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
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.*;
import org.dspace.curate.service.WorkflowCuratorService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageWorkflowEvent;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.dspace.workflowbasic.service.TaskListItemService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

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

    @Override
    public void addInitialWorkspaceItemPolicies(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
                // Now create the policies for the submitter and workflow
        // users to modify item and contents
        // contents = bitstreams, bundles
        // FIXME: icky hardcoded workflow steps
        Collection collection = workspaceItem.getCollection();
        Group step1group = collectionService.getWorkflowGroup(collection, 1);
        Group step2group = collectionService.getWorkflowGroup(collection, 2);
        Group step3group = collectionService.getWorkflowGroup(collection, 3);

        Item item = workspaceItem.getItem();

        if (step1group != null)
        {
            authorizeService.addPolicy(context, item, Constants.READ, step1group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step2group != null)
        {
            authorizeService.addPolicy(context, item, Constants.READ, step2group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step3group != null)
        {
            authorizeService.addPolicy(context, item, Constants.READ, step3group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step1group != null)
        {
            authorizeService.addPolicy(context, item, Constants.WRITE, step1group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step2group != null)
        {
            authorizeService.addPolicy(context, item, Constants.WRITE, step2group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step3group != null)
        {
            authorizeService.addPolicy(context, item, Constants.WRITE, step3group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step1group != null)
        {
            authorizeService.addPolicy(context, item, Constants.ADD, step1group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step2group != null)
        {
            authorizeService.addPolicy(context, item, Constants.ADD, step2group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step3group != null)
        {
            authorizeService.addPolicy(context, item, Constants.ADD, step3group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step1group != null)
        {
            authorizeService.addPolicy(context, item, Constants.REMOVE, step1group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step2group != null)
        {
            authorizeService.addPolicy(context, item, Constants.REMOVE, step2group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step3group != null)
        {
            authorizeService.addPolicy(context, item, Constants.REMOVE, step3group, ResourcePolicy.TYPE_WORKFLOW);
        }
        if (step1group != null)
        {
            authorizeService.addPolicy(context, item, Constants.DELETE, step1group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step2group != null)
        {
            authorizeService.addPolicy(context, item, Constants.DELETE, step2group, ResourcePolicy.TYPE_WORKFLOW);
        }

        if (step3group != null)
        {
            authorizeService.addPolicy(context, item, Constants.DELETE, step3group, ResourcePolicy.TYPE_WORKFLOW);
        }
    }

    @Override
    public BasicWorkflowItem start(Context context, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        // FIXME Check auth
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

            // authorize DSpaceActions.SUBMIT_REVIEW
            doState(context, workflowItem, WFSTATE_STEP1, e);

            break;

        case WFSTATE_STEP2POOL:

            // authorize DSpaceActions.SUBMIT_STEP2
            doState(context, workflowItem, WFSTATE_STEP2, e);

            break;

        case WFSTATE_STEP3POOL:

            // authorize DSpaceActions.SUBMIT_STEP3
            doState(context, workflowItem, WFSTATE_STEP3, e);

            break;

        // if we got here, we weren't pooled... error?
        // FIXME - log the error?
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

            // authorize DSpaceActions.SUBMIT_REVIEW
            // Record provenance
            if (record)
            {
                recordApproval(context, workflowItem, e);
            }
            archived = doState(context, workflowItem, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP2:

            // authorize DSpaceActions.SUBMIT_STEP2
            // Record provenance
            if (record)
            {
                recordApproval(context, workflowItem, e);
            }
            archived = doState(context, workflowItem, WFSTATE_STEP3POOL, e);

            break;

        case WFSTATE_STEP3:

            // authorize DSpaceActions.SUBMIT_STEP3
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

            // authorize DSpaceActions.STEP1
            doState(context, workflowItem, WFSTATE_STEP1POOL, e);

            break;

        case WFSTATE_STEP2:

            // authorize DSpaceActions.APPROVE
            doState(context, workflowItem, WFSTATE_STEP2POOL, e);

            break;

        case WFSTATE_STEP3:

            // authorize DSpaceActions.STEP3
            doState(context, workflowItem, WFSTATE_STEP3POOL, e);

            break;

        // error handling? shouldn't get here
        // FIXME - what to do with error - log it?
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

    // returns true if archived
    protected boolean doState(Context context, BasicWorkflowItem workflowItem, int newstate,
            EPerson newowner) throws SQLException, IOException,
            AuthorizeException
    {
        Collection mycollection = workflowItem.getCollection();
        Group mygroup = null;
        boolean archived = false;

        //Gather our old data for launching the workflow event
        int oldState = workflowItem.getState();
        
        // in case we don't want to inform reviewers about tasks returned to
        // the pool by other reviewers, we'll ne to know whether they were owned
        // before. => keep this information before setting the new owner.
        EPerson oldOwner = workflowItem.getOwner();

        workflowItem.setState(newstate);

        switch (newstate)
        {
        case WFSTATE_STEP1POOL:

            // any reviewers?
            // if so, add them to the tasklist
            workflowItem.setOwner(null);

            // get reviewers (group 1 )
            mygroup = collectionService.getWorkflowGroup(mycollection, 1);

            if ((mygroup != null) && !(groupService.isEmpty(mygroup)))
            {
                // get a list of all epeople in group (or any subgroups)
                List<EPerson> epa = groupService.allMembers(context, mygroup);

                // there were reviewers, change the state
                //  and add them to the list
                createTasks(context, workflowItem, epa);
                workflowItemService.update(context, workflowItem);

                if (ConfigurationManager.getBooleanProperty("workflow", "notify.returned.tasks", true)
                        || oldState != WFSTATE_STEP1
                        || oldOwner == null)
                {
                    // email notification
                    notifyGroupOfTask(context, workflowItem, mygroup, epa);
                }                
            }
            else
            {
                // no reviewers, skip ahead
                workflowItem.setState(WFSTATE_STEP1);
                archived = advance(context, workflowItem, null, true, false);
            }

            break;

        case WFSTATE_STEP1:

            // remove reviewers from tasklist
            // assign owner
            taskListItemService.deleteByWorkflowItem(context, workflowItem);
            workflowItem.setOwner(newowner);

            break;

        case WFSTATE_STEP2POOL:

            // clear owner
            // any approvers?
            // if so, add them to tasklist
            // if not, skip to next state
            workflowItem.setOwner(null);

            // get approvers (group 2)
            mygroup = collectionService.getWorkflowGroup(mycollection, 2);

            if ((mygroup != null) && !(groupService.isEmpty(mygroup)))
            {
                //get a list of all epeople in group (or any subgroups)
                List<EPerson> epa = groupService.allMembers(context, mygroup);

                // there were approvers, change the state
                //  timestamp, and add them to the list
                createTasks(context, workflowItem, epa);

                if (ConfigurationManager.getBooleanProperty("workflow", "notify.returned.tasks", true) 
                        || oldState != WFSTATE_STEP2
                        || oldOwner == null)
                {
                    // email notification
                    notifyGroupOfTask(context, workflowItem, mygroup, epa);
                }
            }
            else
            {
                // no reviewers, skip ahead
                workflowItem.setState(WFSTATE_STEP2);
                archived = advance(context, workflowItem, null, true, false);
            }

            break;

        case WFSTATE_STEP2:

            // remove admins from tasklist
            // assign owner
            taskListItemService.deleteByWorkflowItem(context, workflowItem);
            workflowItem.setOwner(newowner);

            break;

        case WFSTATE_STEP3POOL:

            // any editors?
            // if so, add them to tasklist
            workflowItem.setOwner(null);
            mygroup = collectionService.getWorkflowGroup(mycollection, 3);

            if ((mygroup != null) && !(groupService.isEmpty(mygroup)))
            {
                // get a list of all epeople in group (or any subgroups)
                List<EPerson> epa = groupService.allMembers(context, mygroup);

                // there were editors, change the state
                //  timestamp, and add them to the list
                createTasks(context, workflowItem, epa);

                if (ConfigurationManager.getBooleanProperty("workflow", "notify.returned.tasks", true)
                        || oldState != WFSTATE_STEP3
                        || oldOwner == null)
                {
                    // email notification
                    notifyGroupOfTask(context, workflowItem, mygroup, epa);
                }
            }
            else
            {
                // no editors, skip ahead
                workflowItem.setState(WFSTATE_STEP3);
                archived = advance(context, workflowItem, null, true, false);
            }

            break;

        case WFSTATE_STEP3:

            // remove editors from tasklist
            // assign owner
            taskListItemService.deleteByWorkflowItem(context, workflowItem);
            workflowItem.setOwner(newowner);

            break;

        case WFSTATE_ARCHIVE:

            // put in archive in one transaction
            // remove workflow tasks
            taskListItemService.deleteByWorkflowItem(context, workflowItem);

            mycollection = workflowItem.getCollection();

            Item myitem = archive(context, workflowItem);

            // now email notification
            notifyOfArchive(context, myitem, mycollection);
            archived = true;

            break;
        }

        logWorkflowEvent(context, workflowItem.getItem(), workflowItem, context.getCurrentUser(), newstate, newowner, mycollection, oldState, mygroup);

        if (!archived)
        {
            workflowItemService.update(context, workflowItem);
        }

        return archived;
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
    @Override
    public Item archive(Context context, BasicWorkflowItem workflowItem)
            throws SQLException, IOException, AuthorizeException
    {
        // FIXME: Check auth
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
        // authorize a DSpaceActions.REJECT
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
        return ConfigurationManager.getProperty("dspace.url") + "/mydspace";
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
        collection.setWorkflowGroup(1, null);
        collection.setWorkflowGroup(2, null);
        collection.setWorkflowGroup(3, null);
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
    }
}
