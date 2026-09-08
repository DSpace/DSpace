/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authorize;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by pbecker to write a test against DS-3572.
 * This definitely needs to be extended, but it's at least a start.
 */
public class AuthorizeServiceTest extends AbstractUnitTest {

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance()
                                                                                   .getResourcePolicyService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    public AuthorizeServiceTest() {
    }

    @Test
    public void testauthorizeMethodDoesNotConfuseEPersonWithCurrentUser() {
        Community dso;
        EPerson eperson1;
        EPerson eperson2;
        Group group;

        try {
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
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        } finally {
            context.restoreAuthSystemState();
        }

        try {
            // eperson1 should be able to write as it is a member of a group that has write permissions
            Assert.assertTrue(authorizeService.authorizeActionBoolean(context, eperson1, dso, Constants.WRITE, true));
            // person2 shouldn't have write access
            Assert.assertFalse(authorizeService.authorizeActionBoolean(context, eperson2, dso, Constants.WRITE, true));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testauthorizeMethodRespectSpecialGroups() {

        EPerson eperson;
        Group group1;

        Community dso;
        try {
            context.turnOffAuthorisationSystem();

            // create an eperson and a group
            eperson = ePersonService.create(context);
            group1 = groupService.create(context);
            // A group has to have a name, otherwise there are queries that break
            groupService.setName(group1, "My test group 2");

            // Use a top level community as DSpaceObject to test permissions
            dso = communityService.create(null, context);

            // allow the group some action on a DSpaceObject and set it as
            // special group to the user. Then test if the action on the DSO
            // is allowed for the user
            authorizeService.addPolicy(context, dso, Constants.ADD, group1);
            context.setCurrentUser(eperson);
            context.setSpecialGroup(group1.getID());
            context.commit();
        } catch (SQLException | AuthorizeException ex) {
            throw new AssertionError(ex);
        } finally {
            context.restoreAuthSystemState();
        }

        try {
            Assert.assertTrue(authorizeService.authorizeActionBoolean(context, eperson, dso, Constants.ADD, true));
        } catch (SQLException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * When a bundle is created it should inherit custom policies (deduped)
     * from the item, as otherwise bitstream bundles created via filter-media etc.
     * will be created without READ policies
     */
    @Test
    public void testInheritanceOfCustomPolicies() {
        try {
            context.turnOffAuthorisationSystem();
            Community community = communityService.create(null, context);
            Collection collection = collectionService.create(context, community);
            WorkspaceItem wsItem = workspaceItemService.create(context, collection, false);
            Item item = installItemService.installItem(context, wsItem);
            // Simulate access conditions adding READ policy to the item
            ResourcePolicy itemCustomRead = resourcePolicyService.create(context, eperson, null);
            itemCustomRead.setAction(Constants.READ);
            itemCustomRead.setRpType(ResourcePolicy.TYPE_CUSTOM);
            // Simulate a random ADMIN action policy that might have been added manually
            ResourcePolicy itemCustomAdmin = resourcePolicyService.create(context, eperson, null);
            itemCustomAdmin.setAction(Constants.ADMIN);
            itemCustomAdmin.setRpType(ResourcePolicy.TYPE_CUSTOM);
            List<ResourcePolicy> customPolicies = new ArrayList<>();
            customPolicies.add(itemCustomRead);
            customPolicies.add(itemCustomAdmin);
            authorizeService.addPolicies(context, customPolicies, item);
            // Create a bundle, this should call inheritPolicies via itemService.addBundle
            Bundle bundle = bundleService.create(context, item, "THUMBNAIL");
            List<ResourcePolicy> newPolicies = authorizeService
                .findPoliciesByDSOAndType(context, bundle, ResourcePolicy.TYPE_CUSTOM);
            Assert.assertEquals("Bundle should inherit custom policy from item", 1, newPolicies.size());
            Assert.assertNotEquals("Bundle should ONLY inherit non-admin custom policy from item",
                    Constants.ADMIN, newPolicies.get(0).getAction());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * For other DSOs (which pass false) and for a bundle explicitly calling
     * inheritPolicies(..., false), the TYPE_CUSTOM policies should not be inherited
     * but other non-admin policies should be inherited as usual
     */
    @Test
    public void testNonInheritanceOfCustomPolicies() {
        try {
            context.turnOffAuthorisationSystem();
            Community community = communityService.create(null, context);
            Collection collection = collectionService.create(context, community);
            WorkspaceItem wsItem = workspaceItemService.create(context, collection, false);
            Item item = installItemService.installItem(context, wsItem);
            Bundle bundle = bundleService.create(context, item, "THUMBNAIL");
            // Simulate a custom READ policy added by access conditions step
            ResourcePolicy itemCustomRead = resourcePolicyService.create(context, eperson, null);
            itemCustomRead.setAction(Constants.READ);
            itemCustomRead.setRpType(ResourcePolicy.TYPE_CUSTOM);
            // Simulate an ordinary default read item policy inherited from collection
            ResourcePolicy itemDefaultRead = resourcePolicyService.create(context, eperson, null);
            itemDefaultRead.setAction(Constants.READ);
            itemDefaultRead.setRpType(ResourcePolicy.TYPE_INHERITED);
            List<ResourcePolicy> customPolicies = new ArrayList<>();
            customPolicies.add(itemCustomRead);
            customPolicies.add(itemDefaultRead);
            authorizeService.addPolicies(context, customPolicies, item);
            // Now, inherit policies for bundle with includeCustom=false (which is how other DSOs behave)
            authorizeService.inheritPolicies(context, item, bundle, false);
            List<ResourcePolicy> newCustomPolicies = authorizeService
                .findPoliciesByDSOAndType(context, bundle, ResourcePolicy.TYPE_CUSTOM);
            List<ResourcePolicy> newInheritedPolicies = authorizeService
                .findPoliciesByDSOAndType(context, bundle, ResourcePolicy.TYPE_INHERITED);
            Assert.assertEquals("Bundle should not inherit custom policy from item, if false passed",
                    0, newCustomPolicies.size());
            Assert.assertEquals("Bundle should inherit non-custom, non-admin policies as usual",
                    ResourcePolicy.TYPE_INHERITED, newInheritedPolicies.get(0).getRpType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

//
//    @Test
//    public void testIsCollectionAdmin() throws SQLException, AuthorizeException, IOException {
//
//        Community community = null;
//        EPerson eperson = null;
//
//        try {
//
//            context.turnOffAuthorisationSystem();
//
//            community = communityService.create(null, context);
//            Collection collection = collectionService.create(context, community);
//            eperson = ePersonService.create(context);
//
//            Group administrators = collectionService.createAdministrators(context, collection);
//            groupService.addMember(context, administrators, eperson);
//            context.commit();
//            context.setCurrentUser(eperson);
//
//            Assert.assertTrue(authorizeService.isCollectionAdmin(context));
//
//        } finally {
//
//            if (community != null) {
//                communityService.delete(context, context.reloadEntity(community));
//            }
//            if (eperson != null) {
//                ePersonService.delete(context, context.reloadEntity(eperson));
//            }
//
//            context.restoreAuthSystemState();
//        }
//    }
//
//    @Test
//    public void testIsCollectionAdminReturnsTrueIfTheUserIsCommunityAdmin()
//        throws SQLException, AuthorizeException, IOException {
//
//        Community community = null;
//        EPerson eperson = null;
//
//        try {
//
//            context.turnOffAuthorisationSystem();
//
//            community = communityService.create(null, context);
//            eperson = ePersonService.create(context);
//
//            Group administrators = communityService.createAdministrators(context, community);
//            groupService.addMember(context, administrators, eperson);
//            context.setCurrentUser(eperson);
//            context.commit();
//
//            Assert.assertTrue(authorizeService.isCollectionAdmin(context));
//
//        } finally {
//
//            if (community != null) {
//                communityService.delete(context, context.reloadEntity(community));
//            }
//            if (eperson != null) {
//                ePersonService.delete(context, context.reloadEntity(eperson));
//            }
//
//            context.restoreAuthSystemState();
//        }
//    }
}
