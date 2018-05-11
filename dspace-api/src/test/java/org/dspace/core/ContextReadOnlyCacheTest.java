/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Class to test the read-only Context cache
 */
@RunWith(MockitoJUnitRunner.class)
public class ContextReadOnlyCacheTest {

    private ContextReadOnlyCache readOnlyCache;

    @Mock
    private EPerson ePerson;

    @Before
    public void init() {
        readOnlyCache = new ContextReadOnlyCache();
        when(ePerson.getID()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void cacheAuthorizedAction() throws Exception {
        Item item = Mockito.mock(Item.class);
        when(item.getID()).thenReturn(UUID.randomUUID());

        readOnlyCache.cacheAuthorizedAction(item, Constants.READ, ePerson, true);
        readOnlyCache.cacheAuthorizedAction(item, Constants.WRITE, ePerson, false);

        assertTrue(readOnlyCache.getCachedAuthorizationResult(item, Constants.READ, ePerson));
        assertFalse(readOnlyCache.getCachedAuthorizationResult(item, Constants.WRITE, ePerson));

        assertNull(readOnlyCache.getCachedAuthorizationResult(item, Constants.ADMIN, ePerson));
        assertNull(readOnlyCache.getCachedAuthorizationResult(item, Constants.READ, null));
        assertNull(readOnlyCache.getCachedAuthorizationResult(null, Constants.READ, ePerson));
    }

    @Test
    public void cacheGroupMembership() throws Exception {
        Group group1 = buildGroupMock("Test Group 1");
        Group group2 = buildGroupMock("Test Group 2");
        Group group3 = buildGroupMock("Test Group 3");

        readOnlyCache.cacheGroupMembership(group1, ePerson, true);
        readOnlyCache.cacheGroupMembership(group2, ePerson, false);

        assertTrue(readOnlyCache.getCachedGroupMembership(group1, ePerson));
        assertFalse(readOnlyCache.getCachedGroupMembership(group2, ePerson));

        assertNull(readOnlyCache.getCachedGroupMembership(group3, ePerson));
        assertNull(readOnlyCache.getCachedGroupMembership(null, ePerson));
        assertNull(readOnlyCache.getCachedGroupMembership(group2, null));
    }

    @Test
    public void cacheAllMemberGroupsSet() throws Exception {
        Group group1 = buildGroupMock("Test Group 1");
        Group group2 = buildGroupMock("Test Group 2");
        Group group3 = buildGroupMock("Test Group 3");

        readOnlyCache.cacheAllMemberGroupsSet(ePerson, new HashSet<>(Arrays.asList(group1, group2)));

        assertTrue(readOnlyCache.getCachedGroupMembership(group1, ePerson));
        assertTrue(readOnlyCache.getCachedGroupMembership(group2, ePerson));
        assertFalse(readOnlyCache.getCachedGroupMembership(group3, ePerson));
        assertFalse(readOnlyCache.getCachedGroupMembership(null, ePerson));

        assertNull(readOnlyCache.getCachedGroupMembership(group2, null));
    }

    @Test
    public void clear() throws Exception {
        Item item = Mockito.mock(Item.class);
        when(item.getID()).thenReturn(UUID.randomUUID());
        Group group1 = buildGroupMock("Test Group 1");

        //load data into the cache
        readOnlyCache.cacheAuthorizedAction(item, Constants.READ, ePerson, true);
        readOnlyCache.cacheGroupMembership(group1, ePerson, true);

        //double check the data is there
        assertTrue(readOnlyCache.getCachedAuthorizationResult(item, Constants.READ, ePerson));
        assertTrue(readOnlyCache.getCachedGroupMembership(group1, ePerson));

        //clear the cache
        readOnlyCache.clear();

        //check that the data is not present anymore
        assertNull(readOnlyCache.getCachedAuthorizationResult(item, Constants.READ, ePerson));
        assertNull(readOnlyCache.getCachedGroupMembership(group1, ePerson));
    }

    private Group buildGroupMock(final String name) {
        Group group = Mockito.mock(Group.class);
        when(group.getName()).thenReturn(name);
        return group;
    }

}