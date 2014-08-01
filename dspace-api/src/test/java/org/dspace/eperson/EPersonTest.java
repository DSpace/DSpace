/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.junit.*;

import javax.mail.MessagingException;

import static org.junit.Assert.*;

/**
 *
 * @author mwood
 */
public class EPersonTest extends AbstractUnitTest
{
    private static final Logger log = Logger.getLogger("EPersonTest.class");

    public EPersonTest()
    {
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
    }

    protected static TableRow prepareTableRow()
    {
        // Build a TableRow for an EPerson to wrap
        final ArrayList<String> epersonColumns = new ArrayList<String>();
        epersonColumns.add("eperson_id");
        epersonColumns.add("password");
        epersonColumns.add("salt");
        epersonColumns.add("digest_algorithm");

        return new TableRow("EPerson", epersonColumns);
    }

    /**
     * Test of checkPassword method, of class EPerson.
     */
    @Test
    public void testCheckPassword()
            throws SQLException, DecoderException
    {
        final String attempt = "secret";
        EPerson instance = new EPerson(context, prepareTableRow());

        // Test old unsalted MD5 hash
        final String hash = "5ebe2294ecd0e0f08eab7690d2a6ee69"; // MD5("secret");
        instance.setPasswordHash(new PasswordHash(null, null, hash));
        boolean result = instance.checkPassword(attempt);
        assertTrue("check string with matching MD5 hash", result);
        // It should have converted the password to the new hash
        assertEquals("should have upgraded algorithm",
                PasswordHash.getDefaultAlgorithm(),
                instance.getPasswordHash().getAlgorithm());
        assertTrue("upgraded hash should still match",
                instance.checkPassword(attempt));

        // TODO test a salted multiround hash
    }

    /**
     * Test of getType method, of class EPerson.
     */
    @Test
    public void testGetType()
            throws SQLException
    {
        System.out.println("getType");
        EPerson instance = new EPerson(context, prepareTableRow());
        int expResult = Constants.EPERSON;
        int result = instance.getType();
        assertEquals("Should return Constants.EPERSON", expResult, result);
    }

    /**
     * Test creation of an EPerson.
     */
    @Test
    public void testEPersonCreation()
            throws SQLException, AuthorizeException
    {
        final int id = this.prepareEPerson();

        TableRow myRow = DatabaseManager.findByUnique(context, "eperson", "eperson_id", id);
        assertNotNull("Cannot find an EPerson's table row by its ID.", myRow);
        assertEquals("The id of an EPerson was not or not correctly stored in the database.",
                     id, myRow.getIntColumn("eperson_id"));
    }

    /**
     * Test persistence of an EPerson.
     */
    @Test
    public void testEPersonPersistence()
            throws SQLException, AuthorizeException
    {
        // email has to be unique. We use the name of the current test as local part here
        final String email = "testepersonpersistence@example.org";
        final int id = prepareEPerson(email);

        TableRow myRow = DatabaseManager.findByUnique(context, "eperson", "eperson_id", id);
        assertNotNull("Cannot find an EPerson's table row by its ID.", myRow);
        assertEquals("The id of an EPerson was not or not correctly stored in the database.",
                     id,
                     myRow.getIntColumn("eperson_id"));
        assertEquals("The email address of an EPerson was not stored correctly.", email, myRow.getStringColumn("email"));
    }

    /**
     * Simple test if deletion of an EPerson throws any exceptions.
     */
    @Test
    public void testDeleteEPerson() throws SQLException, AuthorizeException
    {
        // email has to be unique. We use the name of the current test as local part here
        final String email = "testdeleteeperson@example.org";
        final int id = prepareEPerson(email);

        context.turnOffAuthorisationSystem();
        try
        {
            EPerson ep = EPerson.find(context, id);
            ep.delete();
        }
        catch (SQLException | IOException | EPersonDeletionException | AuthorizeException ex)
        {
            log.error("Cannot delete EPersion, caught " + ex.getClass().getName() + ":", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        context.restoreAuthSystemState();
        context.commit();
        context.clearCache();

        TableRowIterator tri = DatabaseManager.query(context, "SELECT * FROM eperson WHERE eperson_id = ?", id);
        assertFalse("EPerson has not been deleted correctly!", tri.hasNext());
    }

    /**
     * Test to delete an EPerson to which metadata were added.
     */
    @Test
    public void testDeleteEPersonWithMetadata() throws SQLException, AuthorizeException
    {
        // email has to be unique. We use the name of the current test as local part here
        final String email = "testdeleteepersonwithmetadata@example.org";
        final int id = prepareEPerson(email);

        context.turnOffAuthorisationSystem();
        EPerson ep = EPerson.find(context, id);
        ep.setFirstName("Jane");
        ep.setLastName("Doe");
        ep.addMetadata("dc", "description", null, "en", "EPerson created during a unit test.");
        ep.update();
        // commit and clear cache
        context.restoreAuthSystemState();
        context.commit();
        context.clearCache();
        context.turnOffAuthorisationSystem();
        try
        {
            // reload eperson
            ep = EPerson.find(context, id);
            ep.delete();
        }
        catch (SQLException | IOException | EPersonDeletionException | AuthorizeException ex)
        {
            log.error("Cannot delete EPersion, caught " + ex.getClass().getName() + ":", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Test that an EPerson does not get deleted if it submitted an Item.
     */
    @Test
    public void testNonCascadingDeletionOfSubmitter()
            throws SQLException, AuthorizeException
    {
        // email has to be unique. We use the name of the current test as local part here
        final String email = "testnoncascadingdeletionofsubmitter@example.org";
        final int id = prepareEPerson(email);
        EPerson ep = EPerson.find(context, id);

        try
        {
            Item item = prepareItem(ep);
        }
        catch (SQLException | AuthorizeException | IOException ex)
        {
            log.error("Caught an Exception while initializing an Item. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while initializing an Item. " + ex.getClass().getName() + ": " + ex.getMessage());
        }

        context.turnOffAuthorisationSystem();

        try
        {
            ep.delete(false);
        }
        catch (SQLException | IOException | AuthorizeException ex)
        {
            log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        catch (EPersonDeletionException ex)
        {
            List<String> tableList = ex.getTables();
            Iterator<String> iterator = tableList.iterator();
            while (iterator.hasNext())
            {
                String tableName = iterator.next();
                if (StringUtils.equalsIgnoreCase(tableName, "item"))
                {
                    return;
                }
            }
        }
        // if we did not get and EPersonDeletionException or it did not contain the item table, we should fail
        // because it was not recognized that the EPerson is used as submitter.
        fail("It was not recognized that a EPerson should be deleted that is referenced in the item table.");
    }

    /**
     * Test that the submitter is set to null if the specified EPerson was deleted using cascading.
     */
    @Test
    public void testCascadingDeletionOfSubmitter()
            throws SQLException, AuthorizeException
    {
        // email has to be unique. We use the name of the current test as local part here
        final String email = "testcascadingdeletionofsubmitter@example.org";
        final int id = prepareEPerson(email);
        EPerson ep = EPerson.find(context, id);
        Item item = null;
        try
        {
            item = prepareItem(ep);
        }
        catch (SQLException | AuthorizeException | IOException ex)
        {
            log.error("Caught an Exception while initializing an Item. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while initializing an Item. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        assertNotNull(item);
        final int itemID = item.getID();

        context.turnOffAuthorisationSystem();
        try
        {
            ep.delete(true);
        }
        catch (SQLException | IOException | AuthorizeException ex)
        {
            log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        catch (EPersonDeletionException ex)
        {
            fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " + ex.getMessage());
        }

        // clear the context cache, reload item, check submitter
        context.clearCache();

        item = Item.find(context, itemID);
        assertNotNull("Could not load item after cascading deletion of the submitter.", item);
        assertNull("Cascading deletion of an EPerson did not set the submitter of an submitted item null.",
                   item.getSubmitter());
    }

    /**
     * Test that an unsubmitted workspace items get deleted when an EPerson gets deleted.
     */
    @Test
    public void testCascadingDeletionOfUnsubmittedWorkspaceItem()
            throws SQLException, AuthorizeException, IOException
    {
        // email has to be unique. We use the name of the current test as local part here
        final String email = "testcascadingdeletionofunsbumittedworkspaceitem@example.org";
        final int id = prepareEPerson(email);
        EPerson ep = EPerson.find(context, id);

        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = prepareWorkspaceItem(ep);
        Item item = wsi.getItem();
        item.addMetadata("dc", "title", null, "en", "Testdocument 1");
        item.update();
        final int wsiID = wsi.getID();
        final int itemID = item.getID();
        context.restoreAuthSystemState();
        context.commit();
        context.clearCache();

        context.turnOffAuthorisationSystem();
        try
        {
            ep.delete(true);
        }
        catch (SQLException | IOException | AuthorizeException ex)
        {
            log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        catch (EPersonDeletionException ex)
        {
            fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " + ex.getMessage());
        }

        context.restoreAuthSystemState();
        context.commit();
        // clear the context cache, reload item, check submitter
        context.clearCache();

        try
        {
            WorkspaceItem restoredWsi = WorkspaceItem.find(context, wsiID);
            Item restoredItem = Item.find(context, itemID);
            assertNull("An unsubmited WorkspaceItem wasn't deleted while cascading deleting the submitter.", restoredWsi);
            assertNull("An unsubmited Item wasn't deleted while cascading deleting the submitter.", restoredItem);
        }
        catch (SQLException ex)
        {
            log.error("SQLException while trying to load previously stored");
        }
    }

    /**
     * Test that submitted but not yet archived items do not get delete while cascading deletion of an EPerson.
     */
    @Test
    public void testCascadingDeleteSubmitterPreservesWorkflowItems()
            throws SQLException, AuthorizeException, IOException, MessagingException
    {
        // create an item used in the test
        final String email =" testCascadingDeleteSubmitterPreservesWorkflowItems@example.org";
        final int id = prepareEPerson(email);
        EPerson ep = EPerson.find(context, id);
        WorkspaceItem wsi = null;
        try
        {
            wsi = prepareWorkspaceItem(ep);
        }
        catch (SQLException | AuthorizeException | IOException ex)
        {
            log.error("Caught an Exception while initializing an WorkspaceItem. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while initializing an WorkspaceItem. " + ex.getClass().getName() + ": "
                         + ex.getMessage());
        }
        assertNotNull(wsi);
        context.turnOffAuthorisationSystem();

        // for this test we need an workflow item that is not yet submitted. Currently the Workflow advance
        // automatically if nobody is defined to perform a step (see comments of DS-1941).
        // We need to configure a collection to have a workflow step and set a person to perform this step. Then we can
        // create an item, start the workflow and delete the item's submitter.
        Group wfGroup = wsi.getCollection().createWorkflowGroup(1);
        wsi.getCollection().update();
        EPerson groupMember = EPerson.create(context);
        groupMember.setEmail("testCascadingDeleteSubmitterPreservesWorkflowItems2@example.org");
        groupMember.update();
        wfGroup.addMember(groupMember);
        wfGroup.update();

        // DSpace currently contains two workflow systems. The newer XMLWorfklow needs additional tables that are not
        // part of the test database yet. While it is expected that it becomes the default workflow system (DS-2059)
        // one day, this won't happen before it its backported to JSPUI (DS-2121).
        // TODO: add tests using the configurable workflowsystem
        final int wfiID = WorkflowManager.startWithoutNotify(context, wsi).getID();

        context.restoreAuthSystemState();
        context.commit();
        context.clearCache();
        context.turnOffAuthorisationSystem();

        // check that the workflow item exists.
        TableRow tr = DatabaseManager.findByUnique(context, "WorkflowItem", "workflow_id", wfiID);
        assertNotNull("Cannot find currently created WorkflowItem!", tr);

        // delete the submitter
        try
        {
            ep.delete(true);
        }
        catch (SQLException | IOException | AuthorizeException ex)
        {
            log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        catch (EPersonDeletionException ex)
        {
            fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " + ex.getMessage());
        }
        context.restoreAuthSystemState();
        context.commit();
        context.turnOffAuthorisationSystem();
        context.clearCache();

        // check whether the workflow item still exists.
        WorkflowItem wfi = WorkflowItem.find(context, wfiID);
        assertNotNull("Could not load WorkflowItem after cascading deletion of the submitter.", wfi);
        assertNull("Cascading deletion of an EPerson did not set the submitter of an submitted WorkflowItem null.",
                   wfi.getSubmitter());
    }

    /**
     * Test that deleting a Person that claimed a task, repools the task.
     */
    @Test
    public void testDeletingAnTaskHolderUnclaimsTask()
            throws SQLException, AuthorizeException, MessagingException, IOException
    {
        // create an item used in the test
        final String email =" testDeletingAnTaskHolderUnclaimsTask@example.org";
        EPerson submitter = EPerson.find(context, prepareEPerson(email));
        WorkspaceItem wsi = null;
        try
        {
            wsi = prepareWorkspaceItem(submitter);
        }
        catch (SQLException | AuthorizeException | IOException ex)
        {
            log.error("Caught an Exception while initializing an WorkspaceItem. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while initializing an WorkspaceItem. " + ex.getClass().getName() + ": "
                         + ex.getMessage());
        }
        assertNotNull(wsi);
        context.turnOffAuthorisationSystem();

        // for this test we need an workflow item that is not yet submitted. Currently the Workflow advance
        // automatically if nobody is defined to perform a step (see comments of DS-1941).
        // We need to configure a collection to have a workflow step and set at least two persons to perform this step.
        // Then we can create an item, start the workflow, claim the task and delete the person who claimed it.
        Group wfGroup = wsi.getCollection().createWorkflowGroup(1);
        wsi.getCollection().update();
        EPerson ep = EPerson.create(context);
        ep.setEmail("testDeletingAnTaskHolderUnclaimsTask2@example.org");
        ep.update();
        final int id = ep.getID();
        wfGroup.addMember(ep);
        EPerson coWorker = EPerson.create(context);
        coWorker.setEmail("testDeletingAnTaskHolderUnclamisTask3example.org");
        coWorker.update();
        wfGroup.addMember(coWorker);
        wfGroup.update();
        context.restoreAuthSystemState();
        context.commit();
        context.turnOffAuthorisationSystem();

        // DSpace currently contains two workflow systems. The newer XMLWorfklow needs additional tables that are not
        // part of the test database yet. While it is expected that it becomes the default workflow system (DS-2059)
        // one day, this won't happen before it its backported to JSPUI (DS-2121).
        // TODO: add tests using the configurable xmlworkflow system
        WorkflowItem wfi = WorkflowManager.startWithoutNotify(context, wsi);
        final int wfiID = wfi.getID();
        context.commit();

        WorkflowManager.claim(context, wfi, ep);
        context.restoreAuthSystemState();
        context.commit();
        context.clearCache();
        context.turnOffAuthorisationSystem();

        // delete the task owner
        try
        {
            ep.delete(true);
        }
        catch (SQLException | IOException | AuthorizeException ex)
        {
            log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
            fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        catch (EPersonDeletionException ex)
        {
            fail("Caught an EPersonDeletionException while trying to cascading delete an EPerson: " + ex.getMessage());
        }
        context.restoreAuthSystemState();
        context.commit();
        context.turnOffAuthorisationSystem();
        context.clearCache();

        // check whether the workflow item still exists and is unclaimed.
        wfi = WorkflowItem.find(context, wfiID);
        assertNotNull("Could not load WorkflowItem after cascading deletion of its owner.", wfi);
        assertNull("Cascading deletion of an EPerson did not repooled a claimed task.", wfi.getOwner());
    }

    /**
     * Creates an item, sets the specified submitter.
     *
     * This method is just an shortcut, so we must not use all the code again and again.
     * @param submitter
     * @return the created item.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    private Item prepareItem(EPerson submitter)
            throws SQLException, AuthorizeException, IOException
    {
        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = prepareWorkspaceItem(submitter);
        Item item = InstallItem.installItem(context, wsi);
        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();
        context.commit();

        return item;
    }

    /**
     * Creates a WorkspaceItem and sets the specified submitter.
     *
     * This method is just an shortcut, so we must not use all the code again and again.
     * @param submitter
     * @return the created WorkspaceItem.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    private WorkspaceItem prepareWorkspaceItem(EPerson submitter)
            throws SQLException, AuthorizeException, IOException
    {
        context.turnOffAuthorisationSystem();
        // create a community, a collection and a WorkspaceItem
        Community community = Community.create(null, context);
        Collection collection = community.createCollection();
        WorkspaceItem wsi = WorkspaceItem.create(context, collection, false);
        // set the submitter
        Item item = wsi.getItem();
        item.setSubmitter(submitter);
        item.update();
        wsi.update();
        context.restoreAuthSystemState();
        context.commit();

        return wsi;
    }

    /**
     * Creates an EPerson.
     *
     * Shortcut for calling prepareEPerson(null).
     *
     * @return The id of the create EPerson.
     * @throws SQLException
     * @throws AuthorizeException
     */
    private int prepareEPerson()
            throws SQLException, AuthorizeException
    {
        return prepareEPerson(null);
    }

    /**
     * Creates an EPerson and sets the email adress if the attribute is not empty or null.
     * @param email the email address or null
     * @return The id of the created EPerson
     * @throws SQLException
     * @throws AuthorizeException
     */
    private int prepareEPerson(String email)
            throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        EPerson ep = EPerson.create(context);
        if (StringUtils.isNotEmpty(email))
        {
            ep.setEmail(email);
        }
        ep.update();
        context.restoreAuthSystemState();
        context.commit();
        int id = ep.getID();
        context.clearCache();
        return id;
    }
}
