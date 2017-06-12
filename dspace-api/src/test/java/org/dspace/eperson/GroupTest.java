/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

/**
 * Unit tests for the Group class
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupTest extends AbstractUnitTest {

    private static final Logger log = Logger.getLogger(GroupTest.class);

    //TODO: test duplicate names ?

    private Group topGroup;
    private Group level1Group;
    private Group level2Group;

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();



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
        try {
            //Only admins can perform group operations, so add as default user
            context.turnOffAuthorisationSystem();

            topGroup = createGroup("topGroup");
            level1Group = createGroup("level1Group");
            groupService.addMember(context, topGroup, level1Group);
            level2Group = createGroup("level2Group");
            groupService.addMember(context, level1Group, level2Group);

            groupService.update(context,  topGroup);
            groupService.update(context,  level1Group);
            groupService.update(context,  level2Group);
            context.restoreAuthSystemState();


        }catch(SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
    }

    @After
    @Override
    public void destroy()
    {
        try {
            context.turnOffAuthorisationSystem();
            if(level2Group != null)
            {
                groupService.delete(context,level2Group);
                level2Group = null;
            }
            if(level1Group != null)
            {
                groupService.delete(context, level1Group);
                level1Group = null;
            }
            if(topGroup != null)
            {
                groupService.delete(context,topGroup);
                topGroup = null;
            }
            context.restoreAuthSystemState();
            super.destroy();
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("IO Error in init: " + ex.getMessage());
        }
    }

    @Test
    public void createGroup() throws SQLException, AuthorizeException, IOException {
        Group group = null;
        try {
            context.turnOffAuthorisationSystem();
            group = groupService.create(context);
            assertThat("testCreateGroup", group, notNullValue());
        } finally {
            if(group != null)
            {
                groupService.delete(context, group);
            }
            context.restoreAuthSystemState();
        }
    }

    @Test(expected = AuthorizeException.class)
    public void createGroupUnAuthorized() throws SQLException, AuthorizeException {
        context.setCurrentUser(null);
        groupService.create(context);
    }

    @Test
    public void setGroupName() throws SQLException, AuthorizeException {
        topGroup.setName("new name");
        groupService.update(context, topGroup);
        assertThat("setGroupName 1", topGroup.getName(), notNullValue());
        assertEquals("setGroupName 2", topGroup.getName(), "new name");
    }

    @Test
    public void setGroupNameOnPermanentGroup() throws SQLException, AuthorizeException {
        topGroup.setPermanent(true);

        topGroup.setName("new name");
        groupService.update(context, topGroup);
        assertThat("setGroupName 1", topGroup.getName(), notNullValue());
        assertEquals("setGroupName 2", topGroup.getName(), "topGroup");

        topGroup.setPermanent(false);
        groupService.update(context, topGroup);
    }

    @Test
    public void findByName() throws SQLException {
        Group group = groupService.findByName(context, "topGroup");
        assertThat("findByName 1", group, notNullValue());
        assertThat("findByName 2", group.getName(), notNullValue());
        assertEquals("findByName 2", group.getName(), "topGroup");
    }

    @Test
    public void findAll() throws SQLException {
        List<Group> groups = groupService.findAll(context, null);
        assertThat("findAll 1", groups, notNullValue());
        System.out.println("TEST GROUP OUTPUT " + groups);
        assertTrue("findAll 2", 0 < groups.size());
    }

    //No longer possible, wouldn't make sense since we are using UUID'S
//    @Test
//    public void findAllIdSort() throws SQLException {
//        List<Group> groups = groupService.findAll(context, GroupService.ID);
//
//        assertThat("findAllIdSort 1", groups, notNullValue());
//
//        //Check our sorting order by adding to a treeSet & check against arraylist values
//        List<String> listNames = new ArrayList<String>();
//        Set<String> setNames = new TreeSet<String>();
//        for (Group group : groups) {
//            listNames.add(group.getID().toString());
//            setNames.add(group.getID().toString());
//        }
//        assertTrue("findAllIdSort 2 ", ArrayUtils.isEquals(setNames.toArray(new String[setNames.size()]), listNames.toArray(new String[listNames.size()])));
//    }


    @Test
    public void findAllNameSort() throws SQLException {
        // Retrieve groups sorted by name
        List<Group> groups = groupService.findAll(context, null);

        assertThat("findAllNameSort 1", groups, notNullValue());

        // Add all group names to two arraylists (arraylists are unsorted)
        // NOTE: we use lists here because we don't want duplicate names removed
        List<String> names = new ArrayList<>();
        List<String> sortedNames = new ArrayList<>();
        for (Group group : groups) {
            // Ignore any unnamed groups. This is only necessary when running unit tests via a persistent database (e.g. Postgres) as unnamed groups may be created by other tests.
            if (group.getName() == null) {
                continue;
            }
            names.add(group.getName());
            sortedNames.add(group.getName());
        }

        // Now, sort the "sortedNames" Arraylist
        Collections.sort(sortedNames);

        // Verify the sorted arraylist is still equal to the original (unsorted) one
        assertEquals("findAllNameSort compareLists", sortedNames, names);
    }

    @Test
    public void searchByName() throws SQLException {
        //We can find 2 groups so attempt to retrieve with offset 0 and a max of one
        List<Group> groups = groupService.search(context, "level", 0, 1);
        assertThat("search 1", groups, notNullValue());
        assertEquals("search 2", groups.size(), 1);
        String firstGroupName = groups.iterator().next().getName();
        assertTrue("search 3", firstGroupName.equals("level1Group") || firstGroupName.equals("level2Group"));

        //Retrieve the second group
        groups = groupService.search(context, "level", 1, 2);
        assertThat("search 1", groups, notNullValue());
        assertEquals("search 2", groups.size(), 1);
        String secondGroupName = groups.iterator().next().getName();
        assertTrue("search 3", secondGroupName.equals("level1Group") || secondGroupName.equals("level2Group"));
    }

    @Test
    public void searchByID() throws SQLException
    {
        List<Group> searchResult = groupService.search(context, String.valueOf(topGroup.getID()), 0, 10);
        assertEquals("searchID 1", searchResult.size(), 1);
        assertEquals("searchID 2", searchResult.iterator().next(), topGroup);
    }


    @Test
    public void searchResultCount() throws SQLException {
        assertEquals("searchResultCount", groupService.searchResultCount(context, "level"), 2);
    }

    @Test
    public void addMemberEPerson() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson =  null;
        try {
            ePerson = createEPersonAndAddToGroup("addMemberEPerson@dspace.org", topGroup);
            groupService.update(context, topGroup);

            assertEquals("addMemberEPerson 1", topGroup.getMembers().size(), 1);
            assertTrue("addMemberEPerson 2", topGroup.getMembers().contains(ePerson));
        } finally {
            if(ePerson != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson);
                context.restoreAuthSystemState();
            }
        }
    }

    @Test
    public void addMemberGroup() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        context.turnOffAuthorisationSystem();
        Group parentGroup = createGroup("parentGroup");
        Group childGroup = createGroup("childGroup");
        groupService.addMember(context, parentGroup, childGroup);
        groupService.update(context, parentGroup);
        groupService.update(context, childGroup);
        groupService.delete(context, parentGroup);
        groupService.delete(context, childGroup);
        context.restoreAuthSystemState();
    }


    @Test
    public void deleteGroupEPersonMembers() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson =  null;
        context.turnOffAuthorisationSystem();
        try {
            Group toDeleteGroup = createGroup("toDelete");
            ePerson = createEPerson("deleteGroupEPersonMembers@dspace.org");
            groupService.addMember(context, toDeleteGroup, ePerson);
            groupService.update(context, toDeleteGroup);
            groupService.delete(context, toDeleteGroup);
            assertEquals("deleteGroupEPersonMembers", ePerson.getGroups().size(), 0);
        } finally {
            if(ePerson != null)
            {
                ePersonService.delete(context, ePerson);
            }
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void deleteGroupGroupMembers() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        //Delete parent first
        Group parentGroup = createGroup("toDeleteParent");
        Group childGroup = createGroup("toDeleteChild");
        groupService.addMember(context, parentGroup, childGroup);
        groupService.update(context, parentGroup);
        groupService.update(context, childGroup);
        groupService.delete(context, parentGroup);
        groupService.delete(context, childGroup);

        //Delete child first
        parentGroup = createGroup("toDeleteParent");
        childGroup = createGroup("toDeleteChild");
        groupService.addMember(context, parentGroup, childGroup);
        groupService.update(context, parentGroup);
        groupService.update(context, childGroup);
        groupService.delete(context, childGroup);
        groupService.delete(context, parentGroup);
    }

    @Test
    public void isMemberGroup() throws SQLException
    {
        assertTrue("isMemberGroup 1", groupService.isMember(topGroup, level1Group));
        assertTrue("isMemberGroup 2", groupService.isMember(level1Group, level2Group));
        assertFalse("isMemberGroup 3", groupService.isMember(level1Group, topGroup));
        assertFalse("isMemberGroup 4", groupService.isMember(level2Group, level1Group));
    }

    @Test
    public void isSubgroupOf() throws SQLException {
        assertTrue("isMemberGroup 1", groupService.isParentOf(context, topGroup, level1Group));
        assertTrue("isMemberGroup 2", groupService.isParentOf(context, level1Group, level2Group));
        assertFalse("isMemberGroup 3", groupService.isParentOf(context, level1Group, topGroup));
        assertFalse("isMemberGroup 4", groupService.isParentOf(context, level2Group, level1Group));

        //Also check ancestor relations
        assertTrue("isMemberGroup 5", groupService.isParentOf(context, topGroup, level2Group));
        assertFalse("isMemberGroup 6", groupService.isParentOf(context, level2Group, topGroup));
    }

    @Test
    public void isMemberEPerson() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = null;
        try {
            context.turnOffAuthorisationSystem();
            ePerson = createEPersonAndAddToGroup("isMemberEPerson@dspace.org", level1Group);
            assertTrue(groupService.isDirectMember(level1Group, ePerson));
            assertFalse(groupService.isDirectMember(topGroup, ePerson));
        } finally {
            if(ePerson != null)
            {
                ePersonService.delete(context, ePerson);
            }
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void isMemberContext() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = null;
        try {
            ePerson = createEPersonAndAddToGroup("isMemberContext@dspace.org", level2Group);

            context.setCurrentUser(ePerson);
            assertTrue(groupService.isMember(context, ePerson, topGroup));
            assertTrue(groupService.isMember(context, ePerson, level1Group));
            assertTrue(groupService.isMember(context, ePerson, level2Group));
        } finally {
            if(ePerson != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson);
            }
        }
    }

    @Test
    public void isMemberContextGroupId() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = null;
        try {
            ePerson = createEPersonAndAddToGroup("isMemberContextGroupId@dspace.org", level2Group);

            assertTrue(groupService.isMember(context, ePerson, topGroup.getName()));
            assertTrue(groupService.isMember(context, ePerson, level1Group.getName()));
            assertTrue(groupService.isMember(context, ePerson, level2Group.getName()));
        } finally {
            if(ePerson != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson);
            }
        }
    }

    @Test
    public void isMemberContextSpecialGroup() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = null;
        Group specialGroup = null;
        try {
            specialGroup = createGroup("specialGroup");
            groupService.addMember(context, level1Group, specialGroup);
            groupService.update(context, level1Group);

            ePerson = createEPerson("isMemberContextGroupSpecial@dspace.org");

            context.setCurrentUser(ePerson);
            context.setSpecialGroup(specialGroup.getID());

            assertTrue(groupService.isMember(context, topGroup));
            assertTrue(groupService.isMember(context, level1Group));
            assertFalse(groupService.isMember(context, level2Group));
            assertTrue(groupService.isMember(context, specialGroup));

        } finally {
            if(ePerson != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson);
            }
            if(specialGroup != null)
            {
                context.turnOffAuthorisationSystem();
                groupService.delete(context, specialGroup);
            }
        }
    }

    @Test
    public void isMemberContextSpecialGroupOtherUser() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson1 = null;
        EPerson ePerson2 = null;
        Group specialGroup = null;
        try {
            specialGroup = createGroup("specialGroup");
            groupService.addMember(context, level2Group, specialGroup);
            groupService.update(context, level2Group);

            //The authenticated user has a special group
            ePerson1 = createEPerson("isMemberContextGroupSpecial@dspace.org");
            context.setCurrentUser(ePerson1);
            context.setSpecialGroup(specialGroup.getID());

            //Or second user is member of the level 1 group
            ePerson2 = createEPersonAndAddToGroup("isMemberContextSpecialGroupOtherUser@dspace.org", level1Group);

            assertTrue(groupService.isMember(context, ePerson2, topGroup));
            assertTrue(groupService.isMember(context, ePerson2, level1Group));
            assertFalse(groupService.isMember(context, ePerson2, level2Group));
            assertFalse(groupService.isMember(context, ePerson2, specialGroup));

            assertTrue(groupService.isMember(context, ePerson1, level2Group));
            assertTrue(groupService.isMember(context, ePerson1, specialGroup));

        } finally {
            if(ePerson1 != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson1);
            }
            if(ePerson2 != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson2);
            }
            if(specialGroup != null)
            {
                context.turnOffAuthorisationSystem();
                groupService.delete(context, specialGroup);
            }
        }
    }

    @Test
    public void isMemberContextSpecialGroupDbMembership() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = null;
        Group specialGroup = null;
        try {
            specialGroup = createGroup("specialGroup");
            groupService.addMember(context, level1Group, specialGroup);
            groupService.update(context, level1Group);

            ePerson = createEPersonAndAddToGroup("isMemberContextGroupSpecialDbMembership@dspace.org", level2Group);

            context.setCurrentUser(ePerson);
            context.setSpecialGroup(specialGroup.getID());

            assertTrue(groupService.isMember(context, topGroup));
            assertTrue(groupService.isMember(context, level1Group));
            assertTrue(groupService.isMember(context, level2Group));
            assertTrue(groupService.isMember(context, specialGroup));

        } finally {
            if(ePerson != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson);
            }
            if(specialGroup != null)
            {
                context.turnOffAuthorisationSystem();
                groupService.delete(context, specialGroup);
            }
        }
    }

    @Test
    public void isPermanent()
            throws SQLException
    {
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        assertTrue("Anonymous group should be 'permanent'", anonymousGroup.isPermanent());
        assertFalse("topGroup should *not* be 'permanent'", topGroup.isPermanent());
    }

    @Test
    public void setPermanent()
            throws SQLException, AuthorizeException, IOException
    {
        Group permaGroup = new Group();
        permaGroup.setPermanent(true);
        assertTrue("setPermanent(true) should be reflected in the group's state",
                permaGroup.isPermanent());
        permaGroup.setPermanent(false);
        assertFalse("setPermanent(false) should be reflected in the group's state",
                permaGroup.isPermanent());
    }

    @Test
    public void removeMemberEPerson() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = null;
        try {
            // Test normal behavior, add user to group & remove
            ePerson = createEPersonAndAddToGroup("removeMemberEPerson@dspace.org", level2Group);
            context.setCurrentUser(ePerson);
            assertTrue(groupService.isMember(context, topGroup));
            assertTrue(groupService.isMember(context, level1Group));
            assertTrue(groupService.isMember(context, level2Group));
            groupService.removeMember(context, level2Group, ePerson);
            assertFalse(groupService.isMember(context, topGroup));
            assertFalse(groupService.isMember(context, level1Group));
            assertFalse(groupService.isMember(context, level2Group));


            //Test non recursive removal, if not a member do not add
            groupService.addMember(context, level2Group, ePerson);
            assertTrue(groupService.isMember(context, topGroup));
            assertTrue(groupService.isMember(context, level1Group));
            assertTrue(groupService.isMember(context, level2Group));
            groupService.removeMember(context, topGroup, ePerson);
            assertTrue(groupService.isMember(context, topGroup));
            assertTrue(groupService.isMember(context, level1Group));
            assertTrue(groupService.isMember(context, level2Group));
        } finally {
            if(ePerson != null)
            {
                context.turnOffAuthorisationSystem();
                ePersonService.delete(context, ePerson);
            }
        }
    }

    @Test
    public void removeMemberGroup() throws SQLException, AuthorizeException {
        assertTrue(groupService.isMember(topGroup, level1Group));
        assertTrue(groupService.isParentOf(context, topGroup, level1Group));

        groupService.removeMember(context, topGroup, level1Group);
        groupService.update(context, topGroup);

        assertFalse(groupService.isMember(topGroup, level1Group));
        assertFalse(groupService.isParentOf(context, topGroup, level1Group));
    }

    @Test
    public void allMemberGroups() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        EPerson ePerson = createEPersonAndAddToGroup("allMemberGroups@dspace.org", level1Group);
        try {
            assertTrue(groupService.allMemberGroups(context, ePerson).containsAll(Arrays.asList(topGroup, level1Group)));
        } finally {
            context.turnOffAuthorisationSystem();
            ePersonService.delete(context, ePerson);
            context.restoreAuthSystemState();
        }

    }

    @Test
    public void allMembers() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        List<EPerson> allEPeopleAdded = new ArrayList<>();
        try {
            context.turnOffAuthorisationSystem();
            allEPeopleAdded.add(createEPersonAndAddToGroup("allMemberGroups1@dspace.org", topGroup));
            allEPeopleAdded.add(createEPersonAndAddToGroup("allMemberGroups2@dspace.org", level1Group));
            allEPeopleAdded.add(createEPersonAndAddToGroup("allMemberGroups3@dspace.org", level2Group));

            assertTrue(groupService.allMembers(context, topGroup).containsAll(allEPeopleAdded));
            assertTrue(groupService.allMembers(context, level1Group).containsAll(allEPeopleAdded.subList(1, 2)));
            assertTrue(groupService.allMembers(context, level2Group).containsAll(allEPeopleAdded.subList(2, 2)));
        } finally {
            //Remove all the people added
            for (EPerson ePerson : allEPeopleAdded) {
                ePersonService.delete(context, ePerson);
            }
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void isEmpty() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        assertTrue(groupService.isEmpty(topGroup));
        assertTrue(groupService.isEmpty(level1Group));
        assertTrue(groupService.isEmpty(level2Group));

        EPerson person = createEPersonAndAddToGroup("isEmpty@dspace.org", level2Group);
        assertFalse(groupService.isEmpty(topGroup));
        assertFalse(groupService.isEmpty(level1Group));
        assertFalse(groupService.isEmpty(level2Group));
        context.turnOffAuthorisationSystem();
        ePersonService.delete(context, person);
        context.restoreAuthSystemState();
        assertTrue(groupService.isEmpty(level2Group));
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

    protected EPerson createEPerson(String email) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = ePersonService.create(context);
        ePerson.setEmail(email);
        ePersonService.update(context, ePerson);
        context.restoreAuthSystemState();
        return ePerson;
    }


}
