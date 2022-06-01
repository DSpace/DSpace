/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the UnitServiceImpl class
 */
public class UnitServiceImplTest extends AbstractUnitTest {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(UnitServiceImplTest.class);

    //TODO: test duplicate names ?

    private Unit unit1;
    private Unit unit2;
    private Unit unit3;

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            unit1 = UnitTestUtils.createUnit(context, "Unit One", true);
            unit2 = UnitTestUtils.createUnit(context, "Unit Two", false);
            unit3 = UnitTestUtils.createUnit(context, "Unit Three", false);
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
    }

    @After
    @Override
    public void destroy() {
        try {
            UnitTestUtils.deleteUnit(context, unit3);
            UnitTestUtils.deleteUnit(context, unit2);
            UnitTestUtils.deleteUnit(context, unit1);
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
    public void testCreateUnit() throws SQLException, AuthorizeException, IOException {
        Unit unit = null;
        try {
            context.turnOffAuthorisationSystem();
            unit = unitService.create(context);
            assertThat("testCreateUnit", unit, notNullValue());
        } finally {
            if (unit != null) {
                unitService.delete(context, unit);
            }
            context.restoreAuthSystemState();
        }
    }

    @Test(expected = AuthorizeException.class)
    public void createUnitUnAuthorized() throws SQLException, AuthorizeException {
        context.setCurrentUser(null);
        unitService.create(context);
    }

    @Test
    public void setUnitName() throws SQLException, AuthorizeException {
        unit1.setName("new name");
        try {
            context.turnOffAuthorisationSystem();
            unitService.update(context, unit1);
        } finally {
            context.restoreAuthSystemState();
        }
        assertThat("setUnitName 1", unit1.getName(), notNullValue());
        assertThat("setUnitName 2", unit1.getName(), equalTo("new name"));
    }

    @Test
    public void findByName() throws SQLException {
        Unit unit = unitService.findByName(context, "Unit One");
        assertThat("findByName 1", unit, notNullValue());
        assertThat("findByName 2", unit.getName(), notNullValue());
        assertThat("findByName 2", unit.getName(), equalTo("Unit One"));
    }

    @Test
    public void findAll() throws SQLException {
        List<Unit> units = unitService.findAll(context, -1, -1);
        assertThat("findAll 1", units, notNullValue());
        assertTrue("findAll 2", !units.isEmpty());
    }

    @Test
    public void findAllNameSort() throws SQLException {
        // Retrieve units sorted by name
        List<Unit> units = unitService.findAll(context, -1, -1);

        assertThat("findAllNameSort 1", units, notNullValue());

        // Add all unit names to two arraylists (arraylists are unsorted)
        // NOTE: we use lists here because we don't want duplicate names removed
        List<String> names = new ArrayList<>();
        List<String> sortedNames = new ArrayList<>();
        for (Unit unit : units) {
            // Ignore any unnamed units. This is only necessary when running unit tests via a persistent database
            // (e.g. Postgres) as unnamed groups may be created by other tests.
            if (unit.getName() == null) {
                continue;
            }
            names.add(unit.getName());
            sortedNames.add(unit.getName());
        }

        // Now, sort the "sortedNames" Arraylist
        Collections.sort(sortedNames);

        // Verify the sorted arraylist is still equal to the original (unsorted) one
        assertThat("findAllNameSort compareLists", names, equalTo(sortedNames));
    }

    @Test
    public void searchByName() throws SQLException, AuthorizeException {
        //We can find 2 units so attempt to retrieve with offset 0 and a max of one
        List<Unit> units = unitService.search(context, "Unit T", 0, 1);
        assertThat("search 1", units, notNullValue());
        assertThat("search 2", units.size(), equalTo(1));
        String firstUnitName = units.iterator().next().getName();
        assertTrue("search 3", firstUnitName.equals("Unit Two") || firstUnitName.equals("Unit Three"));

        //Retrieve the second unit
        units = unitService.search(context, "Unit T", 1, 2);
        assertThat("search 4", units, notNullValue());
        assertThat("search 5", units.size(), equalTo(1));
        String secondUnitName = units.iterator().next().getName();
        assertTrue("search 6", secondUnitName.equals("Unit Two") || secondUnitName.equals("Unit Three"));
    }

    @Test
    public void searchByID() throws SQLException {
        List<Unit> searchResult = unitService.search(context, String.valueOf(unit1.getID()), 0, 10);
        assertThat("searchID 1", searchResult.size(), equalTo(1));
        assertThat("searchID 2", searchResult.iterator().next(), equalTo(unit1));
    }


    @Test
    public void searchByNameResultCount() throws SQLException {
        assertThat("searchByNameResultCount", unitService.searchResultCount(context, "Unit T"), equalTo(2));
    }

    @Test
    public void searchByIdResultCount() throws SQLException {
        assertThat("searchByIdResultCount",
                   unitService.searchResultCount(context, String.valueOf(unit1.getID())), equalTo(1));
    }

    @Test
    public void addGroup() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        context.turnOffAuthorisationSystem();
        Group group = groupService.findByName(context, "Administrator");
        unitService.addGroup(context, unit1, group);
        unit1 = context.reloadEntity(unit1);
        group = context.reloadEntity(group);
        assertTrue(unit1.isMember(group));
        context.restoreAuthSystemState();
    }

    @Test
    public void removeGroup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Group group = groupService.findByName(context, "Administrator");
        unitService.addGroup(context, unit1, group);
        unit1 = context.reloadEntity(unit1);
        group = context.reloadEntity(group);
        assertTrue(unit1.isMember(group));

        unitService.removeGroup(context, unit1, group);
        unit1 = context.reloadEntity(unit1);
        group = context.reloadEntity(group);
        assertFalse(unit1.isMember(group));

        context.restoreAuthSystemState();
    }

    @Test
    public void getAllGroups() throws SQLException, AuthorizeException, EPersonDeletionException, IOException {
        context.turnOffAuthorisationSystem();
        Group adminGroup = groupService.findByName(context, "Administrator");
        unitService.addGroup(context, unit1, adminGroup);

        Group anonGroup = groupService.findByName(context, "Anonymous");
        unitService.addGroup(context, unit1, anonGroup);


        unit1 = context.reloadEntity(unit1);
        anonGroup = context.reloadEntity(anonGroup);

        assertTrue(unit1.isMember(adminGroup));
        assertTrue(unit1.isMember(anonGroup));

        List<Group> allGroups = unitService.getAllGroups(context, unit1);

        assertTrue(allGroups.containsAll(Arrays.asList(adminGroup, anonGroup)));

        context.restoreAuthSystemState();
    }

    @Test
    public void isFacultyOnly()
        throws SQLException {
        Unit u1 = unitService.findByName(context, "Unit One");
        assertTrue(u1.getFacultyOnly());

        Unit u2 = unitService.findByName(context, "Unit Two");
        assertFalse(u2.getFacultyOnly());
    }

    @Test
    public void setFacultyOnly()
        throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        assertTrue(unit1.getFacultyOnly());
        unit1.setFacultyOnly(false);

        unitService.update(context, unit1);

        Unit u1 = unitService.find(context, unit1.getID());
        assertFalse(u1.getFacultyOnly());
        context.restoreAuthSystemState();
    }
}
