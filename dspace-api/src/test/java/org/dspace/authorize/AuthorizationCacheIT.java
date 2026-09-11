/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test to test the authorization cache used with ReadOnly Context
 */
public class AuthorizationCacheIT extends AbstractIntegrationTestWithDatabase {

    AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    @Test
    public void testCachedAuthorizationAfterAddSpecialGroupWithReadOnlyContext() throws Exception {

        context.turnOffAuthorisationSystem();

        // First disable the index consumer. The indexing process calls the authorizeService
        // function used in this test and may affect the test
        context.setDispatcher("noindex");

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .withAdminGroup(admin)
            .build();
        context.restoreAuthSystemState();

        context.setCurrentUser(eperson);

        context.setMode(Context.Mode.READ_ONLY);

        // check if the user is a community admin, this should cache the response
        Assert.assertFalse("Should not be a community admin",
            authorizeService.authorizeActionBoolean(context, parentCommunity,
                Constants.ADMIN, false));

        Group adminGroup = parentCommunity.getAdministrators();

        // Add a special group to the current user, this should invalidate the cached response
        context.setSpecialGroup(adminGroup.getID());

        // check if the user is a community admin, this could return the cached response
        // if the cache is not invalidated
        Assert.assertTrue("Should be a community admin because of the special group",
            authorizeService.authorizeActionBoolean(context, parentCommunity,
                Constants.ADMIN, false));

    }

    @Test
    public void testCachedAuthorizationWihInheritanceWithReadOnlyContext() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        // First disable the index consumer. The indexing process calls the authorizeService
        // function used in this test and may affect the test
        context.setDispatcher("noindex");

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .withAdminGroup(eperson)
            .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();

        Item item = ItemBuilder.createItem(context, col)
            .withTitle("Item")
            .build();

        context.restoreAuthSystemState();

        context.setCurrentUser(eperson);

        context.setMode(Context.Mode.READ_ONLY);

        // Do a first request to cache the authorization result with inheritance
        authorizeService.authorizeActionBoolean(context, eperson, item, Constants.WRITE, true);

        Assert.assertFalse("Should return false without inheritance",
            authorizeService.authorizeActionBoolean(context, eperson, item, Constants.WRITE, false));
        Assert.assertFalse("Should return false without inheritance",
            authorizeService.authorizeActionBoolean(context, eperson, col, Constants.ADMIN, false));
        Assert.assertTrue("Should return true without inheritance",
            authorizeService.authorizeActionBoolean(context, eperson, parentCommunity, Constants.ADMIN, false));

    }

    @Ignore
    @Test
    public void testValuesCachedAuthorizationWihInheritanceWithReadOnlyContext()
        throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        // First disable the index consumer. The indexing process calls the authorizeService
        // function used in this test and may affect the test
        context.setDispatcher("noindex");

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .withAdminGroup(eperson)
            .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();

        Item item = ItemBuilder.createItem(context, col)
            .withTitle("Item")
            .build();

        context.restoreAuthSystemState();

        context.setCurrentUser(eperson);

        context.setMode(Context.Mode.READ_ONLY);

        // Do a first request to cache the authorization result without inheritance
        authorizeService.authorizeActionBoolean(context, eperson, item, Constants.WRITE, false);

        Assert.assertFalse("Should return a cached response false",
            context.getCachedAuthorizationResult(item, Constants.WRITE, eperson));
        Assert.assertNull("Should not be cached the admin permission",
            context.getCachedAuthorizationResult(col, Constants.ADMIN, eperson));
        Assert.assertNull("Should not be cached the admin permission",
            context.getCachedAuthorizationResult(parentCommunity, Constants.ADMIN, eperson));

        //Change context mode to clear cache
        context.setMode(Context.Mode.READ_WRITE);
        context.setMode(Context.Mode.READ_ONLY);

        // Do a second request to cache the authorization result with inheritance
        authorizeService.authorizeActionBoolean(context, eperson, item, Constants.WRITE, true);

        // Test cache without inheritance
        Assert.assertFalse("Should return a cached response false without inheritance",
            context.getCachedAuthorizationResult(item, Constants.WRITE, eperson));
        Assert.assertFalse("Should return a cached response false for collection without inheritance",
            context.getCachedAuthorizationResult(col, Constants.ADMIN, eperson));
        Assert.assertTrue("Should return a cached response true for parentCommunity without inheritance",
            context.getCachedAuthorizationResult(parentCommunity, Constants.ADMIN, eperson));

        // Test cache with inheritance
        Assert.assertTrue("Should return a cached response true with inheritance",
            context.getCachedAuthorizationResultWithInheritance(item, Constants.WRITE, eperson));
        Assert.assertTrue("Should return a cached response true for collection with inheritance",
            context.getCachedAuthorizationResultWithInheritance(col, Constants.ADMIN, eperson));
        Assert.assertTrue("Should return a cached response true for parentCommunity with inheritance",
            context.getCachedAuthorizationResultWithInheritance(parentCommunity, Constants.ADMIN, eperson));
    }

}
