/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authorize;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;

/**
 * Created by pbecker as he wanted to write a test against DS-3572.
 * This definitely needs to be extended, but it's at least a start.
 */
public class AuthorizeServiceTest  extends AbstractUnitTest
{

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    public AuthorizeServiceTest()
    {}

    @Test
    public void testauthorizeMethodDoesNotConfuseEPersonWithCurrentUser()
    {
        Community dso;
        EPerson eperson1;
        EPerson eperson2;
        Group group;

        try
        {
            context.turnOffAuthorisationSystem();

            // create two epersons: one to test a permission the other one to be used as currentUser
            eperson1 = ePersonService.create(context);
            eperson2 = ePersonService.create(context);
            // create a group as the bug described in DS-3572 contains a wrong group membership check
            group = groupService.create(context);
            // A group has to have a name, otherwise there are queries that break
            groupService.setName(group, "My test group");
            // add eperson1 to the group.
            groupService.addMember(context, group, eperson1);
            groupService.update(context, group);

            // Use a top level community as DSpaceObject to test permissions
            dso = communityService.create(null, context);

            // grant write permission to the eperson1 by its group membership
            authorizeService.addPolicy(context, dso, Constants.WRITE, group);
            context.commit();

            // set the other eperson as the current user
            // Notice that it is not a member of the group, and does not have write permission
            context.setCurrentUser(eperson2);
        }
        catch (SQLException | AuthorizeException ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            context.restoreAuthSystemState();
        }

        try {
            // eperson1 should be able to write as he is member of a group that has write permissions
            Assert.assertTrue(authorizeService.authorizeActionBoolean(context, eperson1, dso, Constants.WRITE, true));
            // person2 shouldn't have write access
            Assert.assertFalse(authorizeService.authorizeActionBoolean(context, eperson2, dso, Constants.WRITE, true));
        }
        catch (SQLException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testauthorizeMethodRespectSpecialGroups()
    {

        EPerson eperson1;
        EPerson eperson2;
        Group group1;

        Community dso;
        try
        {
            context.turnOffAuthorisationSystem();

            // create an eperson and a group
            eperson1 = ePersonService.create(context);
            group1 = groupService.create(context);
            // A group has to have a name, otherwise there are queries that break
            groupService.setName(group1, "My test group 2");

            // Use a top level community as DSpaceObject to test permissions
            dso = communityService.create(null, context);

            // allow the group some action on a DSpaceObject and set it as
            // special group to the user. Then test if the action on the DSO
            // is allowed for the user
            authorizeService.addPolicy(context, dso, Constants.ADD, group1);
            context.setCurrentUser(eperson1);
            context.setSpecialGroup(group1.getID());
            context.commit();
        }
        catch (SQLException | AuthorizeException ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            context.restoreAuthSystemState();
        }

        try {
            Assert.assertTrue(authorizeService.authorizeActionBoolean(context, eperson1, dso, Constants.ADD, true));
        }
        catch (SQLException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
