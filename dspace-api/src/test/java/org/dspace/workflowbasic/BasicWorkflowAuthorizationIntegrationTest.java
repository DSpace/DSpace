/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.CollectionHelper;
import org.dspace.eperson.EPersonDeletionException;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * This is an integration test to ensure that the basic workflow system
 * -including methods of the collection service dealing with it- works properly
 * together with the authorization service.
 * @author Pascal-Nicolas Becker
 * @author Terry Brady
 */
public class BasicWorkflowAuthorizationIntegrationTest
extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BasicWorkflowAuthorizationIntegrationTest.class);

    protected ConfigurationService configurationService
            = new DSpace().getConfigurationService();

    protected Community owningCommunity;
    protected Collection collection;
    protected Group group;
    protected EPerson member;

    public BasicWorkflowAuthorizationIntegrationTest()
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
            context.turnOffAuthorisationSystem();

            this.owningCommunity = Community.create(null, context);
            this.collection = owningCommunity.createCollection();
            this.member = EPerson.create(context);
            this.member.setEmail("john.smith@example.com");
            this.member.setFirstName("John");
            this.member.setLastName("Smith");
            this.group = Group.create(context);
            group.addMember(member);
            group.update();
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
            if (collection != null)
            {
                try {
                    CollectionHelper.delete(collection);
                } catch (IOException | SQLException | AuthorizeException e) {
                    log.error("deleting collection", e);
                }
                collection = null;
            }

            if (owningCommunity != null)
            {
                try {
                    owningCommunity.delete();
                } catch (IOException | SQLException | AuthorizeException e) {
                    log.error("deleting community", e);
                }
                owningCommunity = null;
            }

            if (member != null)
            {
                if (group != null)
                {
                    try {
                        group.removeMember(member);
                    } catch (Exception e) {
                        log.error("detaching group relationship", e);
                    }
                    try {
                        group.delete();
                    } catch (SQLException e) {
                        log.error("deleting group");
                    }
                    group = null;
                }
                try {
                    member.delete();
                } catch (SQLException | AuthorizeException | EPersonDeletionException e) {
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

    private void setWorkflowGroup(Collection collection, int step, Group group)
            throws SQLException, AuthorizeException
    {
        collection.setWorkflowGroup(step, group);
    }

    /**
     * Test if setWorkflowGroup method sets the appropriate policies for the
     * new workflow group.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Test
    public void testsetWorkflowGroupSetsPermission() throws SQLException, AuthorizeException
    {
        int step = 1;
        try {
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, step, group);
            collection.update();
        } finally {
            context.restoreAuthSystemState();
        }
        context.setCurrentUser(member);
        Assert.assertThat("Workflow step " + step + " Group is not our test Group",
                collection.getWorkflowGroup(step), CoreMatchers.equalTo(group));
        Assert.assertTrue("Test EPerson is not member of test Group", group.isMember(member));
        Assert.assertTrue("Test EPerson is not authorized for step " + step,
                AuthorizeManager.authorizeActionBoolean(context, collection, Constants.WORKFLOW_STEP_1, true));
    }

    /**
     * Test if setWorkflowGroup method revokes policies when a workflow group
     * is removed.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Test
    public void testsetWorkflowGroupRevokesPermission()
            throws SQLException, AuthorizeException
    {
        int step = 1;
        try {
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, step, group);
            collection.update();
        } finally {
            context.restoreAuthSystemState();
        }
        context.setCurrentUser(member);
        Assert.assertThat("Test workflow group is not the group for step " + step,
                collection.getWorkflowGroup(step),
                CoreMatchers.equalTo(group));
        Assert.assertTrue("Member of test workflow group not authorized for step " + step,
                AuthorizeManager.authorizeActionBoolean(context, collection,
                        Constants.WORKFLOW_STEP_1, true));
        try {
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, step, null);
            collection.update();
        } finally {
            context.restoreAuthSystemState();
        }
        Assert.assertThat("Workflow step " + step + "group is not null",
                collection.getWorkflowGroup(step),
                CoreMatchers.nullValue());
        Assert.assertFalse("Member of test workflow group is still authorized for step " + step,
                AuthorizeManager.authorizeActionBoolean(context, collection,
                        Constants.WORKFLOW_STEP_1, true));
    }

    /**
     * Test that a member of a workflow step group can claim a task and get the
     * appropriate policies.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.io.IOException
     */
    @Test
    public void testReviewerPermissions()
            throws SQLException, AuthorizeException, IOException
    {
        WorkflowItem wfi = null;
        try {
            // prepare a task to claim
            // turn of the authorization system to be able to create the task
            context.turnOffAuthorisationSystem();
            setWorkflowGroup(collection, 1, group);
            collection.update();
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, false);
            Item item = wsi.getItem();
            Bundle bundle = item.createBundle("ORIGINAL");
            File f = new File(testProps.get("test.bitstream").toString());
            Bitstream bs = bundle.createBitstream(new FileInputStream(f));
            bundle.update();
            item.update();
            wsi.update();

            wfi = WorkflowManager.startWithoutNotify(context, wsi);
            wfi.update();
        } finally {
            // restore the authorization system to perform our tests
            context.restoreAuthSystemState();
        }

        context.setCurrentUser(member);

        wfi = WorkflowItem.find(context, wfi.getID());
        WorkflowManager.claim(context, wfi, context.getCurrentUser());
        Item item = wfi.getItem();

        int i = 0;
        // check item policies
        for (int action : new int[] {Constants.READ, Constants.WRITE, Constants.ADD, Constants.REMOVE, Constants.DELETE})
        {
            Assert.assertTrue("testReviewerPermissions 1-" + i++,
                    AuthorizeManager.authorizeActionBoolean(context, item, action, false));
        }

        // ensure we can read the original bundle and its bitstream
        Bundle bundle = item.getBundles("ORIGINAL")[0];
        Bitstream bitstream = bundle.getBitstreams()[0];
        Assert.assertTrue("testReviewerPermissions 2-1",
                    AuthorizeManager.authorizeActionBoolean(context, bundle, Constants.READ, false));
        Assert.assertTrue("testReviewerPermissions 2-2" + i++,
                    AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ, false));
    }

    /**
     * Test that a eperson not a member of a workflow step group can't claim a task.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    @Test(expected=AuthorizeException.class)
    public void testNonWorkflowGroupMemberCannotClaimTask()
            throws SQLException, AuthorizeException, IOException
    {
        WorkflowItem wfi = null;
        EPerson someone = null;
        try {
            // prepare a task to claim
            // turn of the authorization system to be able to create the task
            context.turnOffAuthorisationSystem();
            someone = EPerson.create(context);
            someone.setEmail("jane.doe@example.com");
            someone.setFirstName("Jane");
            someone.setLastName("Doe");
            setWorkflowGroup(collection, 1, group);
            collection.update();
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, false);
            Item item = wsi.getItem();
            Bundle bundle = item.createBundle("ORIGINAL");
            File f = new File(testProps.get("test.bitstream").toString());
            Bitstream bs = bundle.createBitstream(new FileInputStream(f));
            bundle.update();
            item.update();
            wsi.update();

            wfi = WorkflowManager.startWithoutNotify(context, wsi);
            wfi.update();
        } finally {
            // restore the authorization system to perform our tests
            context.restoreAuthSystemState();
        }

        context.setCurrentUser(someone);

        wfi = WorkflowItem.find(context, wfi.getID());
        WorkflowManager.claim(context, wfi, context.getCurrentUser());
        Assert.fail("Someone, not part of a workflow step group was able to claim a "
                + "task without an AUthorizeException.");
    }

    /**
     * Test that the submitter of an item who is not member of the appropriate
     * workflow step group cannot claim the task of his/her own submission.
     * Submitters have special permissions on Workflow and Workspace items, so we
     * need to test that they are still not able to claim tasks for there own
     * items.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.io.IOException passed through.
     */
    @Test(expected=AuthorizeException.class)
    public void testNonWorkflowGroupSubmitterCannotClaimTask()
            throws SQLException, AuthorizeException, IOException
    {
        WorkflowItem wfi = null;
        EPerson submitter = null;
        try {
            // prepare a task to claim
            // turn of the authorization system to be able to create the task
            context.turnOffAuthorisationSystem();
            submitter = EPerson.create(context);
            submitter.setEmail("richard.roe@example.com");
            submitter.setFirstName("Richard");
            submitter.setLastName("Roe");
            setWorkflowGroup(collection, 1, group);
            collection.update();
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, false);
            Item item = wsi.getItem();
            item.setSubmitter(submitter);
            Bundle bundle = item.createBundle("ORIGINAL");
            File f = new File(testProps.get("test.bitstream").toString());
            Bitstream bs = bundle.createBitstream(new FileInputStream(f));
            bundle.update();
            item.update();
            wsi.update();

            wfi = WorkflowManager.startWithoutNotify(context, wsi);
            wfi.update();
        } finally {
            // restore the authorization system to perform our tests
            context.restoreAuthSystemState();
        }

        context.setCurrentUser(submitter);
        wfi = WorkflowItem.find(context, wfi.getID());
        WorkflowManager.claim(context, wfi, context.getCurrentUser());
        Assert.fail("A submitter was able to claim a task without being a member of the "
                + "appropriate workflow step group. Expected: AuthorizeException.");
    }

}
