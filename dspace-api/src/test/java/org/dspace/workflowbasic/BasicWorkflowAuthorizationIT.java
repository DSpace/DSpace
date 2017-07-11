/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.apache.log4j.Logger;
import org.dspace.AbstractDSpaceTest;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * This is an integration test to ensure that the basic workflow system 
 * -including methods of the collection service dealing with it- works properly 
 * together with the authorization service.
 * @author Pascal-Nicolas Becker
 * @author Terry Brady
 */
public class BasicWorkflowAuthorizationIT
extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BasicWorkflowAuthorizationIT.class);
    
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected BasicWorkflowItemService basicWorkflowItemService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowItemService();
    protected BasicWorkflowService basicWorkflowService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    protected Community owningCommunity;
    protected Collection collection;
    protected Group group;
    protected EPerson member;
    
    public BasicWorkflowAuthorizationIT()
    {
        owningCommunity = null;
        collection = null;
        group = null;
        member = null;
    }
    
    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        
        try
        {
            //we have to create a new community in the database
            configurationService.setProperty("workflow.notify.returned.tasks", false);
            context.turnOffAuthorisationSystem();
            
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            this.member = ePersonService.create(context);
            this.group = groupService.create(context);
            groupService.addMember(context, group, member);
            groupService.update(context, group);
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            Assert.fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            Assert.fail("SQL Error in init: " + ex.getMessage());
        }
        finally
        {
            // restore the authorization system as tests expect it to be in place
            context.restoreAuthSystemState();
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();
            
            // reload collection, community, group and eperson
            if (collection != null) {
                try {
                    collectionService.delete(context, collection);
                } catch (Exception e) {
                    log.error("deleting collection", e);
                }
                collection = null;
            }
            if (owningCommunity != null) {
                try {
                    communityService.delete(context, owningCommunity);
                } catch (Exception e) {
                    log.error("deleting community", e);
                }
                owningCommunity = null;
            }
                
            if (member != null)
            {
                if (group != null) {
                    try {
                        groupService.removeMember(context, group, member);
                    } catch (Exception e) {
                        log.error("detaching group relationship", e);
                    }
                    try {
                        groupService.delete(context, group);
                    } catch (Exception e) {
                        log.error("detaching group relationship", e);
                    }
                    group = null;
                }
                try{
                    ePersonService.delete(context, member);
                } catch (Exception e) {
                    log.error("deleting user", e);
                } 
            }
        }
        finally
        {
            // restore the authorization system
            context.restoreAuthSystemState();
        }
        super.destroy();
    }
    

    private void setWorkflowGroup(Collection collection, Context context, int step, Group group) throws SQLException, AuthorizeException {
        collection.setWorkflowGroup(context, step, group);
        //collection.setWorkflowGroup(step, group);
    }
    
    

    /**
     * Test if setWorkflowGroup method sets the appropriate policies for the 
     * new workflow group.
     */
    @Test
    public void testsetWorkflowGroupSetsPermission() throws SQLException, AuthorizeException
    {
        int step = 1;
        try {
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, context, step, group);
            collectionService.update(context, collection);
        } finally {
            context.restoreAuthSystemState();
        }
        Assert.assertThat("testsetWorkflowGroupSetsPermission 0", collectionService.getWorkflowGroup(collection, step), CoreMatchers.equalTo(group));
        Assert.assertTrue(groupService.isDirectMember(group, member));
        Assert.assertTrue("testsetWorkflowGroupSetsPermission 1", authorizeService.authorizeActionBoolean(context, member, collection, Constants.WORKFLOW_STEP_1, true));
    }
    
    /**
     * Test if setWorkflowGroup method revokes policies when a workflow group 
     * is removed.
     */
    @Test
    public void testsetWorkflowGroupRevokesPermission() throws SQLException, AuthorizeException
    {
        int step = 1;
        try {
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, context, step, group);
            collectionService.update(context, collection);
        } finally {
            context.restoreAuthSystemState();
        }
        Assert.assertThat("testsetWorkflowGroupRevokesPermission 0", collectionService.getWorkflowGroup(collection, step), CoreMatchers
                .equalTo(group));
        Assert.assertTrue("testsetWorkflowGroupRevokesPermission 1", authorizeService.authorizeActionBoolean(context, member, collection, Constants.WORKFLOW_STEP_1, true));
        try {
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, context, step, null);
            collectionService.update(context, collection);
        } finally {
            context.restoreAuthSystemState();
        }
        Assert.assertThat("testsetWorkflowGroupRevokesPermission 2", collectionService.getWorkflowGroup(collection, step), CoreMatchers
                .nullValue());
        Assert.assertFalse("testsetWorkflowGroupRevokesPermission 3", authorizeService.authorizeActionBoolean(context, member, collection, Constants.WORKFLOW_STEP_1, true));
    }
    
    /**
     * Test that a member of a worfklow step group can claim a task and get the
     * appropriate policies.
     */
    @Test
    public void testReviewerPermissions()
            throws SQLException, AuthorizeException, IOException, WorkflowException
    {
        BasicWorkflowItem wfi = null;
        try {
            // prepare a task to claim
            // turn of the authorization system to be able to create the task
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, context, 1, group);
            collectionService.update(context, collection);
            WorkspaceItem wsi = workspaceItemService.create(context, collection, false);
            Item item = wsi.getItem();
            Bundle bundle = bundleService.create(context, item, "ORIGINAL");
            File f = new File(AbstractDSpaceTest.testProps.get("test.bitstream").toString());
            Bitstream bs = bitstreamService.create(context, bundle, new FileInputStream(f));
            bundleService.update(context, bundle);
            itemService.update(context, item);
            workspaceItemService.update(context, wsi);
            
            wfi = basicWorkflowService.startWithoutNotify(context, wsi);
            basicWorkflowItemService.update(context, wfi);
        } finally {
            // restore the authorization system to perform our tests
            context.restoreAuthSystemState();
        }

        wfi = basicWorkflowItemService.find(context, wfi.getID());
        basicWorkflowService.claim(context, wfi, member);
        Item item = wfi.getItem();
        
        int i = 0;
        // check item policies
        for (int action : new int[] {Constants.READ, Constants.WRITE, Constants.ADD, Constants.REMOVE, Constants.DELETE})
        {
            Assert.assertTrue("testReviewerPermissions 1-" + i++,
                    authorizeService.authorizeActionBoolean(context, member, item, action, false));
        }
        
        // ensure we can read the original bundle and its bitstream
        Bundle bundle = itemService.getBundles(item, "ORIGINAL").get(0);
        Bitstream bitstream = bundle.getBitstreams().get(0);
        Assert.assertTrue("testReviewerPermissions 2-1",
                    authorizeService.authorizeActionBoolean(context, member, bundle, Constants.READ, false));
        Assert.assertTrue("testReviewerPermissions 2-2" + i++,
                    authorizeService.authorizeActionBoolean(context, member, bitstream, Constants.READ, false));
    }
    
    /**
     * Test that a eperson not a member of a workflow step group can't claim a task.
     */
    @Test(expected=AuthorizeException.class)
    public void testNonWorkflowGroupMemberCannotClaimTask()
            throws SQLException, AuthorizeException, IOException, WorkflowException
    {
        BasicWorkflowItem wfi = null;
        EPerson someone = null;
        try {
            // prepare a task to claim
            // turn of the authorization system to be able to create the task
            context.turnOffAuthorisationSystem();
            someone = ePersonService.create(context);
            setWorkflowGroup(collection, context, 1, group);
            collectionService.update(context, collection);
            WorkspaceItem wsi = workspaceItemService.create(context, collection, false);
            Item item = wsi.getItem();
            Bundle bundle = bundleService.create(context, item, "ORIGINAL");
            File f = new File(AbstractDSpaceTest.testProps.get("test.bitstream").toString());
            Bitstream bs = bitstreamService.create(context, bundle, new FileInputStream(f));
            bundleService.update(context, bundle);
            itemService.update(context, item);
            workspaceItemService.update(context, wsi);
            
            wfi = basicWorkflowService.startWithoutNotify(context, wsi);
            basicWorkflowItemService.update(context, wfi);
        } finally {
            // restore the authorization system to perform our tests
            context.restoreAuthSystemState();
        }

        wfi = basicWorkflowItemService.find(context, wfi.getID());
        basicWorkflowService.claim(context, wfi, someone);
        Assert.fail("Someone, not part of a workflow step group was able to claim a "
                + "task without an AUthorizeException.");
    }
    
    /**
     * Test that the submitter of an item who is not member of the appropriate
     * workflow step group cannot claim the task of his/her own submission.
     * Submitter habe special permissions on Workflow and Workspace items, so we
     * need to test that they are still not able to claim tasks for there own
     * items.
     */
    @Test(expected=AuthorizeException.class)
    public void testNonWorkflowGroupSubmitterCannotClaimTask()
            throws SQLException, AuthorizeException, IOException, WorkflowException
    {
        BasicWorkflowItem wfi = null;
        EPerson submitter = null;
        try {
            // prepare a task to claim
            // turn of the authorization system to be able to create the task
            context.turnOffAuthorisationSystem();
            submitter = ePersonService.create(context);
            setWorkflowGroup(collection, context, 1, group);
            collectionService.update(context, collection);
            WorkspaceItem wsi = workspaceItemService.create(context, collection, false);
            Item item = wsi.getItem();
            item.setSubmitter(submitter);
            Bundle bundle = bundleService.create(context, item, "ORIGINAL");
            File f = new File(AbstractDSpaceTest.testProps.get("test.bitstream").toString());
            Bitstream bs = bitstreamService.create(context, bundle, new FileInputStream(f));
            bundleService.update(context, bundle);
            itemService.update(context, item);
            workspaceItemService.update(context, wsi);
            
            wfi = basicWorkflowService.startWithoutNotify(context, wsi);
            basicWorkflowItemService.update(context, wfi);
        } finally {
            // restore the authorization system to perform our tests
            context.restoreAuthSystemState();
        }

        wfi = basicWorkflowItemService.find(context, wfi.getID());
        basicWorkflowService.claim(context, wfi, submitter);
        Assert.fail("A submitter was able to claim a task without being a member of the "
                + "appropriate workflow step group. Expected: AuthorizeException.");
    }

}
