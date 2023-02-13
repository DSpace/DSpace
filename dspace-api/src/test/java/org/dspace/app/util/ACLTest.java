/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test ACL for admin and user.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ACLTest extends AbstractUnitTest {

    /**
     * Spy of AuthorizeService to use for tests
     * (initialized / setup in @Before method)
     */
    private AuthorizeService authorizeServiceSpy;
    private GroupService groupServiceSpy;

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
    public void init() {
        super.init();

        // Initialize our spy of the autowired (global) authorizeService bean.
        // This allows us to customize the bean's method return values in tests below
        authorizeServiceSpy = spy(authorizeService);
        groupServiceSpy = spy(groupService);

        ReflectionTestUtils.setField(groupService, "authorizeService", authorizeServiceSpy);
    }

    @Test
    public void testACLIsNull() {
        ACL acl = ACL.fromString(null);

        assertThat("testNullACL 0", acl.isEmpty(), equalTo(true));
    }

    @Test
    public void testACLFromString() {
        String s = "policy=allow,action=read,grantee-type=user,grantee-id=*";
        ACL acl = ACL.fromString(s);

        assertThat("testFromStringACL 0", acl.isEmpty(), equalTo(false));
    }

    @Test
    public void testACLCannotDenyForAdmin() throws SQLException {
        String s = "policy=deny,action=read,grantee-type=user,grantee-id=*";
        ACL acl = ACL.fromString(s);

        // Allow full Admin perms
        when(authorizeServiceSpy.isAdmin(context)).thenReturn(true);
        ReflectionTestUtils.setField(acl, "authorizeService", authorizeServiceSpy);

        assertThat("testACLCannotDenyForAdmin 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(true));
        assertThat("testACLCannotDenyForAdmin 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(true));
    }

    @Test
    public void testACLAllowForAdmin() throws SQLException {
        String s = "policy=allow,action=read,grantee-type=user,grantee-id=*";
        ACL acl = ACL.fromString(s);

        // Allow full Admin perms
        when(authorizeServiceSpy.isAdmin(context)).thenReturn(true);
        ReflectionTestUtils.setField(acl, "authorizeService", authorizeServiceSpy);

        assertThat("testACLAllowForAdmin 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(true));
        assertThat("testACLAllowForAdmin 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(true));
    }

    @Test
    public void testACLDenyForUser() {
        String s = "policy=deny,action=read,grantee-type=user,grantee-id=*";
        ACL acl = ACL.fromString(s);

        assertThat("testACLDenyForUser 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(false));
        assertThat("testACLDenyForUser 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(false));
    }

    @Test
    public void testACLAllowReadForUser() {
        String s = "policy=allow,action=read,grantee-type=user,grantee-id=*";
        ACL acl = ACL.fromString(s);

        assertThat("testACLAllowReadForUser 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(true));
        assertThat("testACLAllowReadForUser 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(false));
    }

    @Test
    public void testACLAllowWriteForUser() {
        String s = "policy=allow,action=write,grantee-type=user,grantee-id=*";
        ACL acl = ACL.fromString(s);

        assertThat("testACLAllowWriteForUser 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(false));
        assertThat("testACLAllowWriteForUser 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(true));
    }

    @Test
    public void testACLDenyForGroup() throws SQLException, AuthorizeException {
        // Allow full Admin perms (in new context)
        when(authorizeServiceSpy.isAdmin(context)).thenReturn(true);

        // Create a new group & add it as a special group
        Group group = groupService.create(context);
        ArrayList<Group> groups = new ArrayList<>(1);
        groups.add(group);

        String s = "policy=deny,action=read,grantee-type=group,grantee-id=" + group.getID();
        ACL acl = ACL.fromString(s);

        ReflectionTestUtils.setField(acl, "groupService", groupServiceSpy);
        when(groupServiceSpy.allMemberGroups(context, context.getCurrentUser())).thenReturn(groups);

        assertThat("testACLDenyForGroup 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(false));
        assertThat("testACLDenyForGroup 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(false));
    }

    @Test
    public void testACLAllowWriteForGroup() throws SQLException, AuthorizeException {
        // Allow full Admin perms (in new context)
        when(authorizeServiceSpy.isAdmin(context)).thenReturn(true);

        // Create a new group & add it as a special group
        Group group = groupService.create(context);
        ArrayList<Group> groups = new ArrayList<>(1);
        groups.add(group);

        String s = "policy=allow,action=read,grantee-type=group,grantee-id=" + group.getID();
        ACL acl = ACL.fromString(s);

        ReflectionTestUtils.setField(acl, "groupService", groupServiceSpy);
        when(groupServiceSpy.allMemberGroups(context, context.getCurrentUser())).thenReturn(groups);

        assertThat("testACLAllowWriteForGroup 0", acl.isAllowedAction(context, ACL.ACTION_READ), equalTo(true));
        assertThat("testACLAllowWriteForGroup 1", acl.isAllowedAction(context, ACL.ACTION_WRITE), equalTo(false));
    }
}
