/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.mail.MessagingException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.collections4.CollectionUtils;
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


    @Test
    public void testPreferences() throws Exception {

        String cookies =
            "{" +
                "\"token_item\":true," +
                "\"impersonation\":true," +
                "\"redirect\":true," +
                "\"language\":true," +
                "\"klaro\":true," +
                "\"google-analytics\":false" +
                "}";

        ePersonService.addMetadata(context, eperson, "dspace", "agreements", "cookies", null, cookies);
        ePersonService.addMetadata(context, eperson, "dspace", "agreements", "end-user", null, "true");
        ePersonService.update(context, eperson);

        assertEquals(
            cookies,
            ePersonService.getMetadataFirstValue(eperson, "dspace", "agreements", "cookies", null)
        );
        assertEquals(
            "true",
                ePersonService.getMetadataFirstValue(eperson, "dspace", "agreements", "end-user", null)
        );
    }

    /**
     * Test of equals method, of class EPerson.
     */
/*
    @Test
    public void testEquals()
    {
        System.out.println("equals");
        Object obj = null;
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hashCode method, of class EPerson.
     */
/*
    @Test
    public void testHashCode()
    {
        System.out.println("hashCode");
        EPerson instance = null;
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of find method, of class EPerson.
     */
/*
    @Test
    public void testFind()
            throws Exception
    {
        System.out.println("find");
        Context context = null;
        int id = 0;
        EPerson expResult = null;
        EPerson result = EPerson.find(context, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByEmail method, of class EPerson.
     */
/*
    @Test
    public void testFindByEmail()
            throws Exception
    {
        System.out.println("findByEmail");
        Context context = null;
        String email = "";
        EPerson expResult = null;
        EPerson result = EPerson.findByEmail(context, email);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByNetid method, of class EPerson.
     */
/*
    @Test
    public void testFindByNetid()
            throws Exception
    {
        System.out.println("findByNetid");
        Context context = null;
        String netid = "";
        EPerson expResult = null;
        EPerson result = EPerson.findByNetid(context, netid);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of search() and searchResultCount() methods of EPersonService
     * NOTE: Pagination is not verified here because it is tested in EPersonRestRepositoryIT
     */
    @Test
    public void testSearchAndCountByNameEmail() throws SQLException, AuthorizeException, IOException {
        List<EPerson> allEPeopleAdded = new ArrayList<>();
        Group testGroup = createGroup("TestingGroup");
        try {
            // Create 4 EPersons.  Add a few to a test group to verify group membership doesn't matter
            EPerson eperson1 = createEPersonAndAddToGroup("eperson1@example.com", "Jane", "Doe", testGroup);
            EPerson eperson2 = createEPerson("eperson2@example.com", "John", "Doe");
            EPerson eperson3 = createEPersonAndAddToGroup("eperson3@example.com", "John", "Smith", testGroup);
            EPerson eperson4 = createEPerson("eperson4@example.com", "Doe", "Smith");
            allEPeopleAdded.addAll(Arrays.asList(eperson1, eperson2, eperson3, eperson4));

            List<EPerson> allJohns = Arrays.asList(eperson2, eperson3);
            List<EPerson> searchJohnResults = ePersonService.search(context, "John", -1, -1);
            assertTrue(searchJohnResults.containsAll(allJohns));
            assertEquals(searchJohnResults.size(), ePersonService.searchResultCount(context, "John"));

            List<EPerson> allDoes = Arrays.asList(eperson1, eperson2, eperson4);
            List<EPerson> searchDoeResults = ePersonService.search(context, "Doe", -1, -1);
            assertTrue(searchDoeResults.containsAll(allDoes));
            assertEquals(searchDoeResults.size(), ePersonService.searchResultCount(context, "Doe"));

            List<EPerson> allSmiths = Arrays.asList(eperson3, eperson4);
            List<EPerson> searchSmithResults = ePersonService.search(context, "Smith", -1, -1);
            assertTrue(searchSmithResults.containsAll(allSmiths));
            assertEquals(searchSmithResults.size(), ePersonService.searchResultCount(context, "Smith"));

            // Assert search on example.com returns everyone
            List<EPerson> searchEmailResults = ePersonService.search(context, "example.com", -1, -1);
            assertTrue(searchEmailResults.containsAll(allEPeopleAdded));
            assertEquals(searchEmailResults.size(), ePersonService.searchResultCount(context, "example.com"));

            // Assert exact email search returns just one
            List<EPerson> exactEmailResults = ePersonService.search(context, "eperson1@example.com", -1, -1);
            assertTrue(exactEmailResults.contains(eperson1));
            assertEquals(exactEmailResults.size(), ePersonService.searchResultCount(context, "eperson1@example.com"));

            // Assert UUID search returns exact match
            List<EPerson> uuidResults = ePersonService.search(context, eperson4.getID().toString(), -1, -1);
            assertTrue(uuidResults.contains(eperson4));
            assertEquals(1, uuidResults.size());
            assertEquals(uuidResults.size(), ePersonService.searchResultCount(context, eperson4.getID().toString()));
        } finally {
            // Remove all Groups & EPersons we added for this test
            context.turnOffAuthorisationSystem();
            groupService.delete(context, testGroup);
            for (EPerson ePerson : allEPeopleAdded) {
                ePersonService.delete(context, ePerson);
            }
            context.restoreAuthSystemState();
        }
    }

    /**
     * Test of searchNonMembers() and searchNonMembersCount() methods of EPersonService
     * NOTE: Pagination is not verified here because it is tested in EPersonRestRepositoryIT
     */
    @Test
    public void testSearchAndCountByNameEmailNonMembers() throws SQLException, AuthorizeException, IOException {
        List<EPerson> allEPeopleAdded = new ArrayList<>();
        Group testGroup1 = createGroup("TestingGroup1");
        Group testGroup2 = createGroup("TestingGroup2");
        Group testGroup3 = createGroup("TestingGroup3");
        try {
            // Create two EPersons in Group 1
            EPerson eperson1 = createEPersonAndAddToGroup("eperson1@example.com", "Jane", "Doe", testGroup1);
            EPerson eperson2 = createEPersonAndAddToGroup("eperson2@example.com", "John", "Smith", testGroup1);

            // Create one more EPerson, and add it and a previous EPerson to Group 2
            EPerson eperson3 = createEPersonAndAddToGroup("eperson3@example.com", "John", "Doe", testGroup2);
            context.turnOffAuthorisationSystem();
            groupService.addMember(context, testGroup2, eperson2);
            groupService.update(context, testGroup2);
            ePersonService.update(context, eperson2);
            context.restoreAuthSystemState();

            // Create 2 more EPersons with no group memberships
            EPerson eperson4 = createEPerson("eperson4@example.com", "John", "Anthony");
            EPerson eperson5 = createEPerson("eperson5@example.org", "Smith", "Doe");
            allEPeopleAdded.addAll(Arrays.asList(eperson1, eperson2, eperson3, eperson4, eperson5));

            // FIRST, test search by last name
            // Verify all Does match a nonMember search of Group3 (which is an empty group)
            List<EPerson> allDoes = Arrays.asList(eperson1, eperson3, eperson5);
            List<EPerson> searchDoeResults = ePersonService.searchNonMembers(context, "Doe", testGroup3, -1, -1);
            assertTrue(searchDoeResults.containsAll(allDoes));
            assertEquals(searchDoeResults.size(), ePersonService.searchNonMembersCount(context, "Doe", testGroup3));

            // Verify searching "Doe" with Group 2 *excludes* the one which is already a member
            List<EPerson> allNonMemberDoes = Arrays.asList(eperson1, eperson5);
            List<EPerson> searchNonMemberDoeResults = ePersonService.searchNonMembers(context, "Doe", testGroup2,
                                                                                      -1, -1);
            assertTrue(searchNonMemberDoeResults.containsAll(allNonMemberDoes));
            assertFalse(searchNonMemberDoeResults.contains(eperson3));
            assertEquals(searchNonMemberDoeResults.size(), ePersonService.searchNonMembersCount(context, "Doe",
                                                                                                testGroup2));

            // Verify searching "Doe" with Group 1 *excludes* the one which is already a member
            allNonMemberDoes = Arrays.asList(eperson3, eperson5);
            searchNonMemberDoeResults = ePersonService.searchNonMembers(context, "Doe", testGroup1, -1, -1);
            assertTrue(searchNonMemberDoeResults.containsAll(allNonMemberDoes));
            assertFalse(searchNonMemberDoeResults.contains(eperson1));
            assertEquals(searchNonMemberDoeResults.size(), ePersonService.searchNonMembersCount(context, "Doe",
                                                                                                testGroup1));

            // SECOND, test search by first name
            // Verify all Johns match a nonMember search of Group3 (which is an empty group)
            List<EPerson> allJohns = Arrays.asList(eperson2, eperson3, eperson4);
            List<EPerson> searchJohnResults = ePersonService.searchNonMembers(context, "John",
                                                                               testGroup3, -1, -1);
            assertTrue(searchJohnResults.containsAll(allJohns));
            assertEquals(searchJohnResults.size(), ePersonService.searchNonMembersCount(context, "John",
                                                                                         testGroup3));

            // Verify searching "John" with Group 2 *excludes* the two who are already a member
            List<EPerson> allNonMemberJohns = Arrays.asList(eperson4);
            List<EPerson> searchNonMemberJohnResults = ePersonService.searchNonMembers(context, "John",
                                                                                        testGroup2, -1, -1);
            assertTrue(searchNonMemberJohnResults.containsAll(allNonMemberJohns));
            assertFalse(searchNonMemberJohnResults.contains(eperson2));
            assertFalse(searchNonMemberJohnResults.contains(eperson3));
            assertEquals(searchNonMemberJohnResults.size(), ePersonService.searchNonMembersCount(context, "John",
                                                                                                   testGroup2));

            // FINALLY, test search by email
            // Assert search on example.com excluding Group 1 returns just those not in that group
            List<EPerson> exampleNonMembers = Arrays.asList(eperson3, eperson4);
            List<EPerson> searchEmailResults = ePersonService.searchNonMembers(context, "example.com",
                                                                               testGroup1, -1, -1);
            assertTrue(searchEmailResults.containsAll(exampleNonMembers));
            assertFalse(searchEmailResults.contains(eperson1));
            assertFalse(searchEmailResults.contains(eperson2));
            assertEquals(searchEmailResults.size(), ePersonService.searchNonMembersCount(context, "example.com",
                                                                                         testGroup1));

            // Assert exact email search returns just one (if not in group)
            List<EPerson> exactEmailResults = ePersonService.searchNonMembers(context, "eperson1@example.com",
                                                                              testGroup2, -1, -1);
            assertTrue(exactEmailResults.contains(eperson1));
            assertEquals(exactEmailResults.size(), ePersonService.searchNonMembersCount(context, "eperson1@example.com",
                                                                                        testGroup2));
            // But, change the group to one they are a member of, and they won't be included
            exactEmailResults = ePersonService.searchNonMembers(context, "eperson1@example.com",
                                                                testGroup1, -1, -1);
            assertFalse(exactEmailResults.contains(eperson1));
            assertEquals(exactEmailResults.size(), ePersonService.searchNonMembersCount(context, "eperson1@example.com",
                                                                                        testGroup1));

            // Assert UUID search returns exact match (if not in group)
            List<EPerson> uuidResults = ePersonService.searchNonMembers(context, eperson3.getID().toString(),
                                                                        testGroup1, -1, -1);
            assertTrue(uuidResults.contains(eperson3));
            assertEquals(1, uuidResults.size());
            assertEquals(uuidResults.size(), ePersonService.searchNonMembersCount(context, eperson3.getID().toString(),
                                                                              testGroup1));
            // But, change the group to one they are a member of, and you'll get no results
            uuidResults = ePersonService.searchNonMembers(context, eperson3.getID().toString(),
                                                          testGroup2, -1, -1);
            assertFalse(uuidResults.contains(eperson3));
            assertEquals(0, uuidResults.size());
            assertEquals(uuidResults.size(), ePersonService.searchNonMembersCount(context, eperson3.getID().toString(),
                                                                                  testGroup2));

        } finally {
            // Remove all Groups & EPersons we added for this test
            context.turnOffAuthorisationSystem();
            groupService.delete(context, testGroup1);
            groupService.delete(context, testGroup2);
            groupService.delete(context, testGroup3);
            for (EPerson ePerson : allEPeopleAdded) {
                ePersonService.delete(context, ePerson);
            }
            context.restoreAuthSystemState();
        }
    }

    /**
     * Test of findAll method, of class EPerson.
     */
/*
    @Test
    public void testFindAll()
            throws Exception
    {
        System.out.println("findAll");
        Context context = null;
        int sortField = 0;
        EPerson[] expResult = null;
        EPerson[] result = EPerson.findAll(context, sortField);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of create method, of class EPerson.
     */
/*
    @Test
    public void testCreate()
            throws Exception
    {
        System.out.println("create");
        Context context = null;
        EPerson expResult = null;
        EPerson result = EPerson.create(context);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method, of class EPerson.
     */
/*
    @Test
    public void testDelete()
            throws Exception
    {
        System.out.println("delete");
        EPerson instance = null;
        instance.delete();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getID method, of class EPerson.
     */
/*
    @Test
    public void testGetID()
    {
        System.out.println("getID");
        EPerson instance = null;
        int expResult = 0;
        int result = instance.getID();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getLanguage method, of class EPerson.
     */
/*
    @Test
    public void testGetLanguage()
    {
        System.out.println("getLanguage");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getLanguage();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setLanguage method, of class EPerson.
     */
/*
    @Test
    public void testSetLanguage()
    {
        System.out.println("setLanguage");
        String language = "";
        EPerson instance = null;
        instance.setLanguage(language);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getHandle method, of class EPerson.
     */
/*
    @Test
    public void testGetHandle()
    {
        System.out.println("getHandle");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getHandle();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getEmail method, of class EPerson.
     */
/*
    @Test
    public void testGetEmail()
    {
        System.out.println("getEmail");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getEmail();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setEmail method, of class EPerson.
     */
/*
    @Test
    public void testSetEmail()
    {
        System.out.println("setEmail");
        String s = "";
        EPerson instance = null;
        instance.setEmail(s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getNetid method, of class EPerson.
     */
/*
    @Test
    public void testGetNetid()
    {
        System.out.println("getNetid");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getNetid();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setNetid method, of class EPerson.
     */
/*
    @Test
    public void testSetNetid()
    {
        System.out.println("setNetid");
        String s = "";
        EPerson instance = null;
        instance.setNetid(s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getFullName method, of class EPerson.
     */
/*
    @Test
    public void testGetFullName()
    {
        System.out.println("getFullName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getFullName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getFirstName method, of class EPerson.
     */
/*
    @Test
    public void testGetFirstName()
    {
        System.out.println("getFirstName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getFirstName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setFirstName method, of class EPerson.
     */
/*
    @Test
    public void testSetFirstName()
    {
        System.out.println("setFirstName");
        String firstname = "";
        EPerson instance = null;
        instance.setFirstName(firstname);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getLastName method, of class EPerson.
     */
/*
    @Test
    public void testGetLastName()
    {
        System.out.println("getLastName");
        EPerson instance = null;
        String expResult = "";
        String result = instance.getLastName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setLastName method, of class EPerson.
     */
/*
    @Test
    public void testSetLastName()
    {
        System.out.println("setLastName");
        String lastname = "";
        EPerson instance = null;
        instance.setLastName(lastname);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setCanLogIn method, of class EPerson.
     */
/*
    @Test
    public void testSetCanLogIn()
    {
        System.out.println("setCanLogIn");
        boolean login = false;
        EPerson instance = null;
        instance.setCanLogIn(login);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of canLogIn method, of class EPerson.
     */
/*
    @Test
    public void testCanLogIn()
    {
        System.out.println("canLogIn");
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.canLogIn();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setRequireCertificate method, of class EPerson.
     */
/*
    @Test
    public void testSetRequireCertificate()
    {
        System.out.println("setRequireCertificate");
        boolean isrequired = false;
        EPerson instance = null;
        instance.setRequireCertificate(isrequired);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getRequireCertificate method, of class EPerson.
     */
/*
    @Test
    public void testGetRequireCertificate()
    {
        System.out.println("getRequireCertificate");
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.getRequireCertificate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setSelfRegistered method, of class EPerson.
     */
/*
    @Test
    public void testSetSelfRegistered()
    {
        System.out.println("setSelfRegistered");
        boolean sr = false;
        EPerson instance = null;
        instance.setSelfRegistered(sr);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getSelfRegistered method, of class EPerson.
     */
/*
    @Test
    public void testGetSelfRegistered()
    {
        System.out.println("getSelfRegistered");
        EPerson instance = null;
        boolean expResult = false;
        boolean result = instance.getSelfRegistered();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getMetadata method, of class EPerson.
     */
/*
    @Test
    public void testGetMetadata()
    {
        System.out.println("getMetadata");
        String field = "";
        EPerson instance = null;
        String expResult = "";
        String result = instance.getMetadata(field);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setMetadata method, of class EPerson.
     */
/*
    @Test
    public void testSetMetadata()
    {
        System.out.println("setMetadata");
        String field = "";
        String value = "";
        EPerson instance = null;
        instance.setMetadata(field, value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setPassword method, of class EPerson.
     */
/*
    @Test
    public void testSetPassword()
    {
        System.out.println("setPassword");
        String s = "";
        EPerson instance = null;
        instance.setPassword(s);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setPasswordHash method, of class EPerson.
     */
/*
    @Test
    public void testSetPasswordHash()
    {
        System.out.println("setPasswordHash");
        PasswordHash password = null;
        EPerson instance = null;
        instance.setPasswordHash(password);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getPasswordHash method, of class EPerson.
     */
/*
    @Test
    public void testGetPasswordHash()
    {
        System.out.println("getPasswordHash");
        EPerson instance = null;
        PasswordHash expResult = null;
        PasswordHash result = instance.getPasswordHash();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

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

        // Start workflow
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

    @Test
    public void findAndCountByGroups() throws SQLException, AuthorizeException, IOException {
        // Create a group with 3 EPerson members
        Group group = createGroup("parentGroup");
        EPerson eperson1 = createEPersonAndAddToGroup("test1@example.com", group);
        EPerson eperson2 = createEPersonAndAddToGroup("test2@example.com", group);
        EPerson eperson3 = createEPersonAndAddToGroup("test3@example.com", group);
        groupService.update(context, group);

        Group group2 = null;
        EPerson eperson4 = null;

        try {
            // Assert that findByGroup is the same list of EPersons as getMembers() when pagination is ignored
            // (NOTE: Pagination is tested in GroupRestRepositoryIT)
            // NOTE: isEqualCollection() must be used for comparison because Hibernate's "PersistentBag" cannot be
            // compared directly to a List. See https://stackoverflow.com/a/57399383/3750035
            assertTrue(
                CollectionUtils.isEqualCollection(group.getMembers(),
                                                  ePersonService.findByGroups(context, Set.of(group), -1, -1)));
            // Assert countByGroups is the same as the size of members
            assertEquals(group.getMembers().size(), ePersonService.countByGroups(context, Set.of(group)));

            // Add another group with duplicate EPerson
            group2 = createGroup("anotherGroup");
            groupService.addMember(context, group2, eperson1);
            groupService.update(context, group2);

            // Verify countByGroups is still 3 (existing person should not be counted twice)
            assertEquals(3, ePersonService.countByGroups(context, Set.of(group, group2)));

            // Add a new EPerson to new group, verify count goes up by one
            eperson4 = createEPersonAndAddToGroup("test4@example.com", group2);
            assertEquals(4, ePersonService.countByGroups(context, Set.of(group, group2)));
        } finally {
            // Clean up our data
            context.turnOffAuthorisationSystem();
            groupService.delete(context, group);
            if (group2 != null) {
                groupService.delete(context, group2);
            }
            ePersonService.delete(context, eperson1);
            ePersonService.delete(context, eperson2);
            ePersonService.delete(context, eperson3);
            if (eperson4 != null) {
                ePersonService.delete(context, eperson4);
            }
            context.restoreAuthSystemState();
        }
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

    protected Group createGroup(String name) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Group group = groupService.create(context);
        group.setName(name);
        groupService.update(context, group);
        context.restoreAuthSystemState();
        return group;
    }

    protected EPerson createEPersonAndAddToGroup(String email, Group group) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = createEPerson(email);
        groupService.addMember(context, group, ePerson);
        groupService.update(context, group);
        ePersonService.update(context, ePerson);
        context.restoreAuthSystemState();
        return ePerson;
    }

    protected EPerson createEPersonAndAddToGroup(String email, String firstname, String lastname, Group group)
        throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = createEPerson(email, firstname, lastname);
        groupService.addMember(context, group, ePerson);
        groupService.update(context, group);
        ePersonService.update(context, ePerson);
        context.restoreAuthSystemState();
        return ePerson;
    }

    protected EPerson createEPerson(String email) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = ePersonService.create(context);
        ePerson.setEmail(email);
        ePersonService.update(context, ePerson);
        context.restoreAuthSystemState();
        return ePerson;
    }
    protected EPerson createEPerson(String email, String firstname, String lastname)
        throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = ePersonService.create(context);
        ePerson.setEmail(email);
        ePerson.setFirstName(context, firstname);
        ePerson.setLastName(context, lastname);
        ePersonService.update(context, ePerson);
        context.restoreAuthSystemState();
        return ePerson;
    }
}
