/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SubResourcePermissionsIT extends AbstractControllerIntegrationTest {


    @Autowired
    private AuthorizeService authorizeService;

    @Test
    public void itemBundlePrivateItemPermissionTest() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item privateItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        Bitstream bitstream;
        Bundle bundle;
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, privateItem1, is)
                                        .withName("Bitstream")
                                        .withMimeType("text/plain")
                                        .build();
        }

        bundle = BundleBuilder.createBundle(context, privateItem1)
                              .withName("testname")
                              .withBitstream(bitstream)
                              .build();


        authorizeService.removeAllPolicies(context, privateItem1);

        String token = getAuthToken(admin.getEmail(), password);

        // Test admin retrieval of subresource bundle of private item
        // should succeed
        getClient(token).perform(get("/api/core/items/" + privateItem1.getID() + "/bundles"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bundles", Matchers.hasItem(BundleMatcher
                                                                                        .matchProperties(
                                                                                            bundle.getName(),
                                                                                            bundle.getID(),
                                                                                            bundle.getHandle(),
                                                                                            bundle.getType()))));

        token = getAuthToken(eperson.getEmail(), password);

        // Test eperson retrieval of subresource bundle of private item
        // shouldn't succeed
        getClient(token).perform(get("/api/core/items/" + privateItem1.getID() + "/bundles"))
                        .andExpect(status().isForbidden());

        // Test anon retrieval of subresource bundle of private item
        // shouldn't succeed
        getClient().perform(get("/api/core/items/" + privateItem1.getID() + "/bundles"))
                   .andExpect(status().isUnauthorized());

        token = getAuthToken(admin.getEmail(), password);

        // Test item retrieval for admin on private item
        // Should succeed
        getClient(token).perform(get("/api/core/items/" + privateItem1.getID())
                                     .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.bundles._embedded.bundles", Matchers.hasItem(BundleMatcher
                                                  .matchProperties(bundle.getName(), bundle.getID(),
                                                      bundle.getHandle(), bundle.getType()))));

        token = getAuthToken(eperson.getEmail(), password);

        // Test item retrieval for normal eperson on private item
        // Shouldn't succeed
        getClient(token).perform(get("/api/core/items/" + privateItem1.getID())
                                     .param("projection", "full"))
                        .andExpect(status().isForbidden());

        // Test item retrieval for anon on private item
        // Shouldn't succeed
        getClient().perform(get("/api/core/items/" + privateItem1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isUnauthorized());


        // Test item retrieval for normal eperson on public bundle
        // Should succeed
        getClient(token).perform(get("/api/core/bundles/" + bundle.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BundleMatcher
                            .matchProperties(bundle.getName(), bundle.getID(), bundle.getHandle(), bundle.getType())));


        // Test item retrieval for anon on public bundle
        // Should succeed
        getClient().perform(get("/api/core/bundles/" + bundle.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", BundleMatcher
                       .matchProperties(bundle.getName(), bundle.getID(), bundle.getHandle(), bundle.getType())));

    }

    @Test
    public void itemBundlePrivateBundlePermissionTest() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        Bitstream bitstream;
        Bundle bundle;
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withMimeType("text/plain")
                                        .build();
        }

        bundle = BundleBuilder.createBundle(context, publicItem1)
                              .withName("testname")
                              .withBitstream(bitstream)
                              .build();


        authorizeService.removeAllPolicies(context, bundle);

        String token = getAuthToken(admin.getEmail(), password);

        // Bundle retrieval for public item, checking private bundle as admin
        // Should succeed
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID() + "/bundles"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bundles", Matchers.hasItem(BundleMatcher
                                                                                        .matchProperties(
                                                                                            bundle.getName(),
                                                                                            bundle.getID(),
                                                                                            bundle.getHandle(),
                                                                                            bundle.getType()))));

        token = getAuthToken(eperson.getEmail(), password);

        // Bundle retrieval for public item, checking private bundle as normal eperson
        // Shouldn't contain the private bundle
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID() + "/bundles"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bundles", Matchers.not(Matchers.hasItem(
                            BundleMatcher.matchProperties(bundle.getName(), bundle.getID(), bundle.getHandle(),
                                                          bundle.getType())))));

        // Bundle retrieval for public item, checking private bundle as anon
        // Shouldn't contain the private bundle
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/bundles"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.bundles", Matchers.not(Matchers.hasItem(
                       BundleMatcher.matchProperties(bundle.getName(), bundle.getID(), bundle.getHandle(),
                                                     bundle.getType())))));

        token = getAuthToken(admin.getEmail(), password);

        // Admin retrieval for public item
        // Should succeed
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID()))
                        .andExpect(status().isOk());

        token = getAuthToken(eperson.getEmail(), password);

        // Normal EPerson retrieval for public item
        // Should succeed
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID()))
                        .andExpect(status().isOk());

        // Anon retrieval for public item
        // Should succeed
        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isOk());

        token = getAuthToken(admin.getEmail(), password);

        // Admin full projection retrieval for public item with private bundles
        // Should succeed
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID())
                                     .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bundles._embedded.bundles", Matchers.hasItem(
                            BundleMatcher.matchProperties(bundle.getName(), bundle.getID(),
                                                          bundle.getHandle(), bundle.getType()))));

        token = getAuthToken(eperson.getEmail(), password);

        // Normal EPerson full projection retrieval for public item with private bundles
        // Shouldn't succeed
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID())
                                     .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bundles._embedded.bundles", Matchers.not(Matchers.hasItem(
                            BundleMatcher.matchProperties(bundle.getName(), bundle.getID(),
                                                          bundle.getHandle(), bundle.getType())))));


        // Anon full projection retrieval for public item with private bundles
        // Shouldn't succeed
        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles", Matchers.not(Matchers.hasItem(
                       BundleMatcher.matchProperties(bundle.getName(), bundle.getID(),
                                                     bundle.getHandle(), bundle.getType())))));


        token = getAuthToken(admin.getEmail(), password);

        // Admin retrieval of private bundle
        // Should succeed
        getClient(token).perform(get("/api/core/bundles/" + bundle.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", BundleMatcher
                            .matchProperties(bundle.getName(), bundle.getID(), bundle.getHandle(), bundle.getType())));

        token = getAuthToken(eperson.getEmail(), password);

        // Normal EPerson retrieval of private bundle
        // Shouldn't succeed
        getClient(token).perform(get("/api/core/bundles/" + bundle.getID()))
                        .andExpect(status().isForbidden());

        // Anon retrieval of private bundle
        // Shouldn't succeed
        getClient().perform(get("/api/core/bundles/" + bundle.getID()))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void parentCommunityOfPrivateCollectionPermissionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        authorizeService.removeAllPolicies(context, col1);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Calling parentCommunity of a private collection as an admin
        // Should succeed
        getClient(adminToken).perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        String epersonToken = getAuthToken(eperson.getEmail(), password);

        // Calling parentCommunity of a private collection as a normal eperson
        // Shouldn't succeed
        getClient(epersonToken).perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
                               .andExpect(status().isForbidden());

        // Calling parentCommunity of a private collection as an anon user
        // Shouldn't succeed
        getClient().perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
                   .andExpect(status().isUnauthorized());

        // Calling public parentCommunity as an admin user
        // Should succeed
        getClient(adminToken).perform(get("/api/core/communities/" + parentCommunity.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        // Calling public parentCommunity as a normal EPerson
        // Should succeed
        getClient(epersonToken).perform(get("/api/core/communities/" + parentCommunity.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        // Calling public parentCommunity as an anon user
        // Should succeed
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        // Calling fullProjection, as an admin user, of a private Collection should contain the parentCommunity
        getClient(adminToken).perform(get("/api/core/collections/" + col1.getID())
                                        .param("projection", "full"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.parentCommunity", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        // Calling full projection, as a normal eperson ,of a private collection should return 403
        getClient(epersonToken).perform(get("/api/core/collections/" + col1.getID())
                                          .param("projection", "full"))
                             .andExpect(status().isForbidden());

        // Calling full projection, as an anon user, of a collection should return 401
        getClient().perform(get("/api/core/collections/" + col1.getID())
                                          .param("projection", "full"))
                             .andExpect(status().isUnauthorized());
    }

    @Test
    public void privateParentCommunityOfCollectionPermissionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        authorizeService.removeAllPolicies(context, parentCommunity);

        String adminToken = getAuthToken(admin.getEmail(), password);

        // Calling private parentCommunity of a collection as an admin
        // Should succeed
        getClient(adminToken).perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        String epersonToken = getAuthToken(eperson.getEmail(), password);

        // Calling private parentCommunity of a collection as a normal eperson
        // Shouldn't succeed
        getClient(epersonToken).perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
                               .andExpect(status().isNoContent());

        // Calling private parentCommunity of a collection as an anon user
        // Shouldn't succeed
        getClient().perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
                   .andExpect(status().isNoContent());

        // Calling private parentCommunity as an admin user
        // Should succeed
        getClient(adminToken).perform(get("/api/core/communities/" + parentCommunity.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        // Calling private parentCommunity as a normal EPerson
        // Shouldn't succeed
        getClient(epersonToken).perform(get("/api/core/communities/" + parentCommunity.getID()))
                               .andExpect(status().isForbidden());

        // Calling private parentCommunity as an anon user
        // Shouldn't succeed
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID()))
                   .andExpect(status().isUnauthorized());

        // Calling fullProjection, as an admin user, of a private Collection should contain the parentCommunity
        getClient(adminToken).perform(get("/api/core/collections/" + col1.getID())
                                          .param("projection", "full"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.parentCommunity", CommunityMatcher
                                 .matchCommunityEntry(parentCommunity.getID(), parentCommunity.getHandle())));

        // Calling full projection, as a normal eperson, of a public collection shouldn't return private parentCommunity
        getClient(epersonToken).perform(get("/api/core/collections/" + col1.getID())
                                            .param("projection", "full"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.parentCommunity").doesNotExist());

        // Calling full projection, as an anon user, of a collection shouldn't return private parentCommunity
        getClient().perform(get("/api/core/collections/" + col1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.parentCommunity").doesNotExist());
    }

}
