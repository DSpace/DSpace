/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import javax.mail.MessagingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author mwood
 */
public class EPersonTest extends AbstractUnitTest {

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkflowItemService workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
    protected WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
                                                          .getWorkspaceItemService();

    private Community community = null;
    private Collection collection = null;
    private Item item = null;

    private static final String EMAIL = "test@example.com";
    private static final String FIRSTNAME = "Kevin";
    private static final String LASTNAME = "Van de Velde";
    private static final String NETID = "1985";
    private static final String PASSWORD = "test";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EPersonTest.class);

    public EPersonTest() {
    }

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses but no
     * execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();

        context.turnOffAuthorisationSystem();
        try {
            EPerson eperson = ePersonService.create(context);
            eperson.setEmail(EMAIL);
            eperson.setFirstName(context, FIRSTNAME);
            eperson.setLastName(context, LASTNAME);
            eperson.setNetid(NETID);
            eperson.setPassword(PASSWORD);
            ePersonService.update(context, eperson);
            this.community = communityService.create(null, context);
            this.collection = collectionService.create(context, this.community);
        } catch (SQLException | AuthorizeException ex) {
            log.error("Error in init", ex);
            fail("Error in init: " + ex.getMessage());
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public void destroy() {
        context.turnOffAuthorisationSystem();
        try {
            EPerson testPerson = ePersonService.findByEmail(context, EMAIL);
            if (testPerson != null) {
                ePersonService.delete(context, testPerson);
            }
        } catch (IOException | SQLException | AuthorizeException ex) {
            log.error("Error in destroy", ex);
            fail("Error in destroy: " + ex.getMessage());
        }
        if (item != null) {
            try {
                item = itemService.find(context, item.getID());
                itemService.delete(context, item);
            } catch (SQLException | AuthorizeException | IOException ex) {
                log.error("Error in destroy", ex);
                fail("Error in destroy: " + ex.getMessage());
            }
        }
        if (this.collection != null) {
            try {
                this.collection = collectionService.find(context, this.collection.getID());
                collectionService.delete(context, this.collection);
            } catch (SQLException | AuthorizeException | IOException ex) {
                log.error("Error in destroy", ex);
                fail("Error in destroy: " + ex.getMessage());
            }
        }
        if (this.community != null) {
            try {
                this.community = communityService.find(context, this.community.getID());
                communityService.delete(context, this.community);
            } catch (SQLException | AuthorizeException | IOException ex) {
                log.error("Error in destroy", ex);
                fail("Error in destroy: " + ex.getMessage());
            }
        }
        context.restoreAuthSystemState();
        item = null;
        this.collection = null;
        this.community = null;
        super.destroy();
    }

    /**
     * Test of checkPassword method, of class EPerson.
     *
     * @throws SQLException
     * @throws DecoderException
     */
    @Test
    public void testCheckPassword()
            throws SQLException, DecoderException {
        EPerson eperson = ePersonService.findByEmail(context, EMAIL);
        ePersonService.checkPassword(context, eperson, PASSWORD);
    }

    /**
     * Test of getType method, of class EPerson.
     *
     * @throws SQLException
     */
    @Test
    public void testGetType()
            throws SQLException {
        System.out.println("getType");
        int expResult = Constants.EPERSON;
        int result = eperson.getType();
        assertEquals("Should return Constants.EPERSON", expResult, result);
    }

    /**
     * Simple test if deletion of an EPerson throws any exceptions.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Test
    public void testDeleteEPerson() throws SQLException, AuthorizeException {
        EPerson deleteEperson = ePersonService.findByEmail(context, EMAIL);
        context.turnOffAuthorisationSystem();

        try {
            ePersonService.delete(context, deleteEperson);
        } catch (AuthorizeException | IOException ex) {
            log.error("Cannot delete EPersion, caught " + ex.getClass().getName() + ":", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                 ": " + ex.getMessage());
        }
        context.restoreAuthSystemState();
        context.commit();
        EPerson findDeletedEperson = ePersonService.findByEmail(context, EMAIL);
        assertNull("EPerson has not been deleted correctly!", findDeletedEperson);
    }

    /**
     * Test that an EPerson has a delete constraint if it submitted an Item.
     *
     * @throws SQLException
     */
    @Test
    public void testDeletionConstraintOfSubmitter()
            throws SQLException {
        EPerson ep = ePersonService.findByEmail(context, EMAIL);
        try {
            item = prepareItem(ep);
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("Caught an Exception while initializing an Item. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while initializing an Item. " + ex.getClass().getName() +
                 ": " + ex.getMessage());
        }

        context.turnOffAuthorisationSystem();

        List<String> tableList = ePersonService.getDeleteConstraints(context, ep);
        Iterator<String> iterator = tableList.iterator();
        while (iterator.hasNext()) {
            String tableName = iterator.next();
            if (StringUtils.equalsIgnoreCase(tableName, "item")) {
                return;
            }
        }
        // if we did not get and EPersonDeletionException or it did not contain the item table, we should fail
        // because it was not recognized that the EPerson is used as submitter.
        fail("It was not recognized that a EPerson is referenced in the item table.");
    }

    /**
     * Test that the submitter is set to null if the specified EPerson was
     * deleted using cascading.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Test
    public void testDeletionOfSubmitterWithAnItem()
            throws SQLException, AuthorizeException {
        EPerson ep = ePersonService.findByEmail(context, EMAIL);
        try {
            item = prepareItem(ep);
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("Caught an Exception while initializing an Item. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while initializing an Item. " + ex.getClass().getName() +
                 ": " + ex.getMessage());
        }
        assertNotNull(item);
        context.turnOffAuthorisationSystem();
        try {
            ePersonService.delete(context, ep);
        } catch (SQLException | IOException | AuthorizeException ex) {
            if (ex.getCause() instanceof EPersonDeletionException) {
                fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " +
                      ex.getMessage());
            } else {
                log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
                fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                     ": " + ex.getMessage());
            }
        }
        item = itemService.find(context, item.getID());
        assertNotNull("Could not load item after cascading deletion of the submitter.", item);
        assertNull("Cascading deletion of an EPerson did not set the submitter of an submitted item null.",
                item.getSubmitter());
    }

    /**
     * Test that an unsubmitted workspace items get deleted when an EPerson gets
     * deleted.
     *
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Test
    public void testCascadingDeletionOfUnsubmittedWorkspaceItem()
            throws SQLException, AuthorizeException, IOException {
        EPerson ep = ePersonService.findByEmail(context, EMAIL);

        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = prepareWorkspaceItem(ep);
        Item item = wsi.getItem();
        itemService.addMetadata(context, item, "dc", "title", null, "en", "Testdocument 1");
        itemService.update(context, item);
        context.restoreAuthSystemState();
        context.commit();
        context.turnOffAuthorisationSystem();

        try {
            ePersonService.delete(context, ep);
        } catch (SQLException | IOException | AuthorizeException ex) {
            if (ex.getCause() instanceof EPersonDeletionException) {
                fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " +
                     ex.getMessage());
            } else {
                log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                          ": ", ex);
                fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                     ": " + ex.getMessage());
            }
        }

        context.restoreAuthSystemState();
        context.commit();

        try {
            WorkspaceItem restoredWsi = workspaceItemService.find(context, wsi.getID());
            Item restoredItem = itemService.find(context, item.getID());
            assertNull("An unsubmited WorkspaceItem wasn't deleted while cascading deleting the submitter.",
                       restoredWsi);
            assertNull("An unsubmited Item wasn't deleted while cascading deleting the submitter.", restoredItem);
        } catch (SQLException ex) {
            log.error("SQLException while trying to load previously stored. " + ex);
        }
    }

    /**
     * Test that submitted but not yet archived items do not get delete while
     * cascading deletion of an EPerson.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws MessagingException
     * @throws WorkflowException
     */
    @Test
    public void testCascadingDeleteSubmitterPreservesWorkflowItems()
            throws SQLException, AuthorizeException, IOException, MessagingException, WorkflowException {
        EPerson ep = ePersonService.findByEmail(context, EMAIL);
        WorkspaceItem wsi = null;

        try {
            wsi = prepareWorkspaceItem(ep);
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("Caught an Exception while initializing an WorkspaceItem. " + ex.getClass().getName() +
                      ": ", ex);
            fail("Caught an Exception while initializing an WorkspaceItem. " + ex.getClass().getName() +
                 ": " + ex.getMessage());
        }
        assertNotNull(wsi);
        context.turnOffAuthorisationSystem();

        // for this test we need an workflow item that is not yet submitted. Currently the Workflow advance
        // automatically if nobody is defined to perform a step (see comments of DS-1941).
        // We need to configure a collection to have a workflow step and set a person to perform this step. Then we can
        // create an item, start the workflow and delete the item's submitter.
        Group wfGroup = collectionService.createWorkflowGroup(context, wsi.getCollection(), 1);
        collectionService.update(context, wsi.getCollection());
        EPerson groupMember = ePersonService.create(context);
        groupMember.setEmail("testCascadingDeleteSubmitterPreservesWorkflowItems2@example.org");
        ePersonService.update(context, groupMember);
        wfGroup.addMember(groupMember);
        groupService.update(context, wfGroup);

        // DSpace currently contains two workflow systems. The newer XMLWorfklow needs additional tables that are not
        // part of the test database yet. While it is expected that it becomes the default workflow system (DS-2059)
        // one day, this won't happen before it its backported to JSPUI (DS-2121).
        // TODO: add tests using the configurable workflowsystem
        int wfiID = workflowService.startWithoutNotify(context, wsi).getID();
        context.restoreAuthSystemState();
        context.commit();
        context.turnOffAuthorisationSystem();

        // check that the workflow item exists.
        assertNotNull("Cannot find currently created WorkflowItem!", workflowItemService.find(context, wfiID));

        // delete the submitter
        try {
            ePersonService.delete(context, ep);
        } catch (SQLException | IOException | AuthorizeException ex) {
            if (ex.getCause() instanceof EPersonDeletionException) {
                fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " +
                     ex.getMessage());
            } else {
                log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                          ": ", ex);
                fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                     ": " + ex.getMessage());
            }
        }

        context.restoreAuthSystemState();
        context.commit();
        context.turnOffAuthorisationSystem();

        // check whether the workflow item still exists.
        WorkflowItem wfi = workflowItemService.find(context, wfiID);
        assertNotNull("Could not load WorkflowItem after cascading deletion of the submitter.", wfi);
        assertNull("Cascading deletion of an EPerson did not set the submitter of an submitted WorkflowItem null.",
                wfi.getSubmitter());
    }

    /**
     * Creates an item, sets the specified submitter.
     *
     * This method is just an shortcut, so we must not use all the code again
     * and again.
     *
     * @param submitter
     * @return the created item.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    private Item prepareItem(EPerson submitter)
        throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = prepareWorkspaceItem(submitter);
        item = installItemService.installItem(context, wsi);
        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();
        return item;
    }

    /**
     * Creates a WorkspaceItem and sets the specified submitter.
     *
     * This method is just an shortcut, so we must not use all the code again
     * and again.
     *
     * @param submitter
     * @return the created WorkspaceItem.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    private WorkspaceItem prepareWorkspaceItem(EPerson submitter)
        throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        // create a community, a collection and a WorkspaceItem

        WorkspaceItem wsi = workspaceItemService.create(context, this.collection, false);
        // set the submitter
        wsi.getItem().setSubmitter(submitter);
        workspaceItemService.update(context, wsi);
        context.restoreAuthSystemState();
        return wsi;
    }
}
