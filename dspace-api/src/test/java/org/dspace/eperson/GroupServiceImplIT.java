/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.dspace.AbstractUnitTest;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

/**
 * Test integration of GroupServiceImpl.
 *
 * @author mwood
 */
public class GroupServiceImplIT
        extends AbstractUnitTest
{
    public GroupServiceImplIT()
    {
        super();
    }

/*
    @BeforeClass
    public static void setUpClass()
    {
    }
*/

/*
    @AfterClass
    public static void tearDownClass()
    {
    }
*/

    @Before
    @Override
    public void init()
    {
        super.init();
    }

    @After
    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Test of create method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testCreate()
            throws Exception
    {
        System.out.println("create");
        Context context = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        Group expResult = null;
        Group result = instance.create(context);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setName method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testSetName()
            throws Exception
    {
        System.out.println("setName");
        Context context = null;
        Group group = null;
        String name = "";
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.setName(context, group, name);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setName method applied to a 'permanent' Group.
     */
    @Test(expected = SQLException.class)
    public void testSetName_permanent()
            throws Exception
    {
        System.out.println("setName on a 'permanent' Group");
        String name = "NOTANONYMOUS";
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        groupService.setName(group, name);
    }

    /**
     * Test of addMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testAddMember_3args_1()
    {
        System.out.println("addMember");
        Context context = null;
        Group group = null;
        EPerson e = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.addMember(context, group, e);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of addMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testAddMember_3args_2()
            throws Exception
    {
        System.out.println("addMember");
        Context context = null;
        Group groupParent = null;
        Group groupChild = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.addMember(context, groupParent, groupChild);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of removeMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testRemoveMember_3args_1()
    {
        System.out.println("removeMember");
        Context context = null;
        Group group = null;
        EPerson ePerson = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.removeMember(context, group, ePerson);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of removeMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testRemoveMember_3args_2()
            throws Exception
    {
        System.out.println("removeMember");
        Context context = null;
        Group groupParent = null;
        Group childGroup = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.removeMember(context, groupParent, childGroup);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isDirectMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testIsDirectMember()
    {
        System.out.println("isDirectMember");
        Group group = null;
        EPerson ePerson = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        boolean expResult = false;
        boolean result = instance.isDirectMember(group, ePerson);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testIsMember_Group_Group()
    {
        System.out.println("isMember");
        Group owningGroup = null;
        Group childGroup = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        boolean expResult = false;
        boolean result = instance.isMember(owningGroup, childGroup);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isMember method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testIsMember_Context_Group()
            throws Exception
    {
        System.out.println("isMember");
        Context context = null;
        Group group = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        boolean expResult = false;
        boolean result = instance.isMember(context, group);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of allMemberGroups method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testAllMemberGroups()
            throws Exception
    {
        System.out.println("allMemberGroups");
        Context context = null;
        EPerson ePerson = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        List<Group> expResult = null;
        List<Group> result = instance.allMemberGroups(context, ePerson);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of allMembers method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testAllMembers()
            throws Exception
    {
        System.out.println("allMembers");
        Context c = null;
        Group g = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        List<EPerson> expResult = null;
        List<EPerson> result = instance.allMembers(c, g);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of find method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testFind()
            throws Exception
    {
        System.out.println("find");
        Context context = null;
        UUID id = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        Group expResult = null;
        Group result = instance.find(context, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByName method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testFindByName()
            throws Exception
    {
        System.out.println("findByName");
        Context context = null;
        String name = "";
        GroupServiceImpl instance = new GroupServiceImpl();
        Group expResult = null;
        Group result = instance.findByName(context, name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findAll method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testFindAll()
            throws Exception
    {
        System.out.println("findAll");
        Context context = null;
        int sortField = 0;
        GroupServiceImpl instance = new GroupServiceImpl();
        List<Group> expResult = null;
        List<Group> result = instance.findAll(context, sortField);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of search method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testSearch_Context_String()
            throws Exception
    {
        System.out.println("search");
        Context context = null;
        String query = "";
        GroupServiceImpl instance = new GroupServiceImpl();
        List<Group> expResult = null;
        List<Group> result = instance.search(context, query);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of search method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testSearch_4args()
            throws Exception
    {
        System.out.println("search");
        Context context = null;
        String query = "";
        int offset = 0;
        int limit = 0;
        GroupServiceImpl instance = new GroupServiceImpl();
        List<Group> expResult = null;
        List<Group> result = instance.search(context, query, offset, limit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of searchResultCount method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testSearchResultCount()
            throws Exception
    {
        System.out.println("searchResultCount");
        Context context = null;
        String query = "";
        GroupServiceImpl instance = new GroupServiceImpl();
        int expResult = 0;
        int result = instance.searchResultCount(context, query);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of delete method applied to a 'permanent' Group.
     */
    @Test(expected = SQLException.class)
    public void testDelete_permanent()
            throws Exception
    {
        System.out.println("delete on a 'permanent' Group");
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        groupService.delete(context, group);
    }

    /**
     * Test of getSupportsTypeConstant method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testGetSupportsTypeConstant()
    {
        System.out.println("getSupportsTypeConstant");
        GroupServiceImpl instance = new GroupServiceImpl();
        int expResult = 0;
        int result = instance.getSupportsTypeConstant();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isEmpty method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testIsEmpty()
    {
        System.out.println("isEmpty");
        Group group = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        boolean expResult = false;
        boolean result = instance.isEmpty(group);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of initDefaultGroupNames method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testInitDefaultGroupNames()
            throws Exception
    {
        System.out.println("initDefaultGroupNames");
        Context context = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.initDefaultGroupNames(context);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getEmptyGroups method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testGetEmptyGroups()
            throws Exception
    {
        System.out.println("getEmptyGroups");
        Context context = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        List<Group> expResult = null;
        List<Group> result = instance.getEmptyGroups(context);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of update method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testUpdate()
            throws Exception
    {
        System.out.println("update");
        Context context = null;
        Group group = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.update(context, group);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of epersonInGroup method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testEpersonInGroup()
            throws Exception
    {
        System.out.println("epersonInGroup");
        Context c = null;
        Group group = null;
        EPerson e = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        boolean expResult = false;
        boolean result = instance.epersonInGroup(c, group, e);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of rethinkGroupCache method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testRethinkGroupCache()
            throws Exception
    {
        System.out.println("rethinkGroupCache");
        Context context = null;
        boolean flushQueries = false;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.rethinkGroupCache(context, flushQueries);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getParentObject method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testGetParentObject()
            throws Exception
    {
        System.out.println("getParentObject");
        Context context = null;
        Group group = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        DSpaceObject expResult = null;
        DSpaceObject result = instance.getParentObject(context, group);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of updateLastModified method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testUpdateLastModified()
    {
        System.out.println("updateLastModified");
        Context context = null;
        Group dso = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        instance.updateLastModified(context, dso);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getChildren method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testGetChildren()
    {
        System.out.println("getChildren");
        Map<UUID, Set<UUID>> parents = null;
        UUID parent = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        Set<UUID> expResult = null;
        Set<UUID> result = instance.getChildren(parents, parent);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByIdOrLegacyId method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testFindByIdOrLegacyId()
            throws Exception
    {
        System.out.println("findByIdOrLegacyId");
        Context context = null;
        String id = "";
        GroupServiceImpl instance = new GroupServiceImpl();
        Group expResult = null;
        Group result = instance.findByIdOrLegacyId(context, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of findByLegacyId method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testFindByLegacyId()
            throws Exception
    {
        System.out.println("findByLegacyId");
        Context context = null;
        int id = 0;
        GroupServiceImpl instance = new GroupServiceImpl();
        Group expResult = null;
        Group result = instance.findByLegacyId(context, id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of countTotal method, of class GroupServiceImpl.
     */
/*
    @Test
    public void testCountTotal()
            throws Exception
    {
        System.out.println("countTotal");
        Context context = null;
        GroupServiceImpl instance = new GroupServiceImpl();
        int expResult = 0;
        int result = instance.countTotal(context);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
