/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public class ItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CollectionService collectionService;

    @Test
    public void findAllTest() throws Exception {
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

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/items"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.items", Matchers.containsInAnyOrder(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                                        "Public item 1", "2017-10-17"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2,
                                        "Public item 2", "2016-02-13"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3,
                                        "Public item 3", "2016-02-13")
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/core/items")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/items"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findAllWithPaginationTest() throws Exception {
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

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/items")
                   .param("size", "2"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.items", Matchers.containsInAnyOrder(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                               "Public item 1", "2017-10-17"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2,
                               "Public item 2", "2016-02-13")
                   )))
                   .andExpect(jsonPath("$._embedded.items", Matchers.not(
                           Matchers.contains(
                               ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3,
                                       "Public item 3", "2016-02-13")
                           )
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/core/items")))
        ;

        getClient(token).perform(get("/api/core/items")
                   .param("size", "2")
                   .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.items", Matchers.contains(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3,
                               "Public item 3", "2016-02-13")
                   )))
                   .andExpect(jsonPath("$._embedded.items", Matchers.not(
                       Matchers.contains(
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                                   "Public item 1", "2017-10-17"),
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2,
                                   "Public item 2", "2016-02-13")
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/core/items")))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;
    }

    @Test
    public void findOneTest() throws Exception {
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

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        Matcher<? super Object> publicItem1Matcher = ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                        "Public item 1", "2017-10-17");

        String token = getAuthToken(admin.getEmail(), password);
        // We want to test a full projection here, but only admins should expect for it to never cause
        // authorization issues
        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID())
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", publicItem1Matcher));

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andExpect(jsonPath("$", publicItem1Matcher));

        // When exact embeds are requested, response should include expected properties, links, and exact embeds.
        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                .param("embed", "bundles,owningCollection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchEmbeds(
                        "bundles[]",
                        "owningCollection"
                )))
                .andExpect(jsonPath("$", publicItem1Matcher));
    }

    @Test
    public void findOneFullProjectionTest() throws Exception {
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
        context.restoreAuthSystemState();
        Matcher<? super Object> publicItem1Matcher = ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                                                                                                 "Public item 1",
                                                                                                 "2017-10-17");

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + publicItem1.getID())
                                     .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.owningCollection._embedded.adminGroup", nullValue()));


        getClient().perform(get("/api/core/items/" + publicItem1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.adminGroup").doesNotExist());

    }

    @Test
    public void findOneRelsTest() throws Exception {
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

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        //Add a bitstream to an item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       ItemMatcher
                               .matchItemWithTitleAndDateIssued(publicItem1,
                                       "Public item 1", "2017-10-17")
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       Matchers.is(
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2,
                                   "Public item 2", "2016-02-13")
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/bundles"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .containsString("/api/core/items/" + publicItem1.getID() + "/bundles")))
        ;

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/owningCollection"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/core/collections")))
        ;
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
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

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/items/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound())
        ;

    }

    @Test
    public void withdrawPatchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        // A token must be provided for withdraw operation. The person
        // is used in the provenance note.
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // withdraw item
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));

        // check item status after the patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));

        // item already withdrawn, no-op, 200 response
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));
    }

    @Test
    public void withdrawPatchUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // withdraw item
        getClient().perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnauthorized());

        // use the admin to be sure to get the item status
        String tokenAdmin = getAuthToken(eperson.getEmail(), password);

        // check item status after the failed patch
        getClient(tokenAdmin).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(true)));
    }

    @Test
    public void withdrawPatchForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        // try to use an unauthorized user
        String token = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // withdraw item
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // use the admin to be sure to get the item status
        String tokenAdmin = getAuthToken(eperson.getEmail(), password);

        // check item status after the failed patch
        getClient(tokenAdmin).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(true)));
    }

    @Test
    public void valueMissingForWithdrawalOperation() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        // we need to set a current user as the withdrawn operation use it to add provenance information
        context.setCurrentUser(admin);

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        //2. One withdrawn item
        Item item2 = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .withdrawn()
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", null);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // check item status after the failed patch (it must be unchanged)
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(true)));

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // check item status after the failed patch (it must be unchanged)
        getClient(token).perform(get("/api/core/items/" + item2.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item2.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));
    }

    @Test
    public void reinstatePatchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        // we need to set a current user as the withdrawn operation use it to add provenance information
        context.setCurrentUser(admin);

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .withdrawn()
                               .build();

        context.restoreAuthSystemState();
        // A token must be provided for reinstate operation. The person
        // is used in the provenance note.
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                   .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                   .andExpect(jsonPath("$.inArchive", Matchers.is(true)));

        // check item status after the patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(true)));

        // reinstate an already installed item is a no-op
        getClient(token).perform(patch("/api/core/items/" + item.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                       .andExpect(jsonPath("$.withdrawn", Matchers.is(false)))
                       .andExpect(jsonPath("$.inArchive", Matchers.is(true)));
    }

    @Test
    public void reinstatePatchUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        // we need to set a current user as the withdrawn operation use it to add provenance information
        context.setCurrentUser(admin);

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .withdrawn()
                               .build();

        context.restoreAuthSystemState();
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make an anonymous request
        getClient().perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                   .andExpect(status().isUnauthorized());

        // check item status after the failed patch
        getClient(tokenAdmin).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));
    }

    @Test
    public void reinstatePatchForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        // we need to set a current user as the withdrawn operation use it to add provenance information
        context.setCurrentUser(admin);

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .withdrawn()
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make a request with an unauthorized user
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // check item status after the failed patch
        getClient(tokenAdmin).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                        .andExpect(jsonPath("$.inArchive", Matchers.is(false)));
    }

    @Test
    public void makeDiscoverablePatchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One private item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .makeUnDiscoverable()
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make discoverable
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

        // check item status after the patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

        // make discoverable an already discoverable item is a no-op
        getClient(token).perform(patch("/api/core/items/" + item.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));
    }

    @Test
    public void makeDiscoverablePatchUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One private item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .makeUnDiscoverable()
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make discoverable with anonymous user
        getClient().perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnauthorized());

        // check item status after the patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));
    }

    @Test
    public void makeDiscoverablePatchForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One private item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .makeUnDiscoverable()
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make discoverable with anonymous user
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // check item status after the patch
        getClient(tokenAdmin).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));
    }

    @Test
    public void makeUnDiscoverablePatchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make private
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));

        // check item status after the patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));

    }

    @Test
    public void useStringForBooleanTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        // String value should work.
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", "false");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make private
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));

        // check item status after the patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));

    }

    @Test
    public void makeUnDiscoverablePatchUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make private with an anonymous user
        getClient().perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isUnauthorized());

        // check item status after the failed patch
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

    }

    @Test
    public void makeUnDiscoverablePatchForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make private
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

        // check item status after the failed patch
        getClient(tokenAdmin).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

    }

    @Test
    public void valueMissingForDiscoverableOperation() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        //3. One private item
        Item item2 = ItemBuilder.createItem(context, col1)
                               .withTitle("Not discoverable item 2")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .makeUnDiscoverable()
                               .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", null);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // check item status after the failed patch (it must be unchanged)
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

        List<Operation> ops2 = new ArrayList();
        ReplaceOperation replaceOperation2 = new ReplaceOperation("/discoverable", null);
        ops.add(replaceOperation);
        String patchBody2 = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item2.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());

        // check item status after the failed patch (it must be unchanged)
        getClient(token).perform(get("/api/core/items/" + item2.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item2.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(false)));

    }

    @Test
    public void deleteOneArchivedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder
                .createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. One public item, one workspace item and one template item.
        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                     .withSubject("ExtraEntry")
                                     .build();

        //Add a bitstream to an item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                             createBitstream(context, publicItem, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();
        // Check publicItem creation
        getClient().perform(get("/api/core/items/" + publicItem.getID()))
                   .andExpect(status().isOk());

        // Check publicItem bitstream creation (shuold be stored in bundle)
        getClient().perform(get("/api/core/items/" + publicItem.getID() + "/bundles"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                           .containsString("/api/core/items/" + publicItem.getID() + "/bundles")));

        String token = getAuthToken(admin.getEmail(), password);

        //Delete public item
        getClient(token).perform(delete("/api/core/items/" + publicItem.getID()))
                   .andExpect(status().is(204));

        //Trying to get deleted item should fail with 404
        getClient().perform(get("/api/core/items/" + publicItem.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted item bitstream should fail with 404
        getClient().perform(get("/api/core/biststreams/" + bitstream.getID()))
                   .andExpect(status().is(404));
    }

    @Test
    public void deleteOneTemplateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        //2. A collection with one template item.
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withTemplateItem()
                .build();


        Item templateItem = col1.getTemplateItem();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        //Trying to delete a templateItem should fail with 422
        getClient(token).perform(delete("/api/core/items/" + templateItem.getID()))
                    .andExpect(status().is(422));

        //Check templateItem is not deleted
        getClient(token).perform(get("/api/core/items/" + templateItem.getID()))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteOneWorkspaceTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder
                .createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. One workspace item.
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                            .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        //Trying to delete a workspaceItem should fail with 422
        getClient(token).perform(delete("/api/core/items/" + workspaceItem.getItem().getID()))
                    .andExpect(status().is(422));

        //Check workspaceItem is available after failed deletion
        getClient(token).perform(get("/api/core/items/" + workspaceItem.getItem().getID()))
                    .andExpect(status().isOk());
    }

    @Test
    public void embargoAccessTest() throws Exception {
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

        //2. An embargoed item
        Item embargoedItem1 = ItemBuilder.createItem(context, col1)
                                         .withTitle("embargoed item 1")
                                         .withIssueDate("2017-12-18")
                                         .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                         .withSubject("ExtraEntry")
                                         .withEmbargoPeriod("6 months")
                                         .build();

        //3. a public item
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        //** THEN **
        //An anonymous user can view public items
        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(
                                publicItem1, "Public item 1", "2017-10-17")
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/items")));

        //An anonymous user is not allowed to view embargoed items
        getClient().perform(get("/api/core/items/" + embargoedItem1.getID()))
                   .andExpect(status().isUnauthorized());

        //An admin user is allowed to access the embargoed item
        String token1 = getAuthToken(admin.getEmail(), password);
        getClient(token1).perform(get("/api/core/items/" + embargoedItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(
                                embargoedItem1, "embargoed item 1", "2017-12-18")
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/items")));

    }

    @Test
    public void embargoAccessGrantAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson adminParentCommunity = EPersonBuilder.createEPerson(context)
                .withEmail("adminCommunity@mail.com")
                .withPassword("qwerty01")
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(adminParentCommunity)
                .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();

        EPerson adminChild2 = EPersonBuilder.createEPerson(context)
                .withEmail("adminChild2@mail.com")
                .withPassword("qwerty05")
                .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community 2")
                .withAdminGroup(adminChild2)
                .build();

        EPerson adminCollection1 = EPersonBuilder.createEPerson(context)
                .withEmail("adminCollection1@mail.com")
                .withPassword("qwerty02")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .withAdminGroup(adminCollection1)
                .build();

        EPerson adminCollection2 = EPersonBuilder.createEPerson(context)
                .withEmail("adminCollection2@mail.com")
                .withPassword("qwerty03")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 2")
                .withAdminGroup(adminCollection2)
                .build();

        Item embargoedItem = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2015-10-21")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .withEmbargoPeriod("1 week")
                .build();

        context.restoreAuthSystemState();
        // parent community's admin user is allowed access to embargoed item
        String tokenAdminParentCommunity = getAuthToken(adminParentCommunity.getEmail(), "qwerty01");
        getClient(tokenAdminParentCommunity).perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(ItemMatcher.matchItemProperties(embargoedItem))));

        // collection1's admin user is allowed access to embargoed item
        String tokenAdminCollection1 = getAuthToken(adminCollection1.getEmail(), "qwerty02");
        getClient(tokenAdminCollection1).perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(ItemMatcher.matchItemProperties(embargoedItem))));

        // collection2's admin user is NOT allowed access to embargoed item of collection1
        String tokenAdminCollection2 = getAuthToken(adminCollection2.getEmail(), "qwerty03");
        getClient(tokenAdminCollection2).perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isForbidden());

        // full admin user is allowed access to embargoed item
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(ItemMatcher.matchItemProperties(embargoedItem))));

        // child2's admin user is NOT allowed access to embargoed item of collection1
        String tokenAdminChild2 = getAuthToken(adminChild2.getEmail(), "qwerty05");
        getClient(tokenAdminChild2).perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void expiredEmbargoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        Item embargoedItem = ItemBuilder.createItem(context, col1)
                .withTitle("embargoed item 1")
                .withIssueDate("2017-11-18")
                .withAuthor("Smith, Donald")
                .withEmbargoPeriod("-2 week")
                .build();

        context.restoreAuthSystemState();

        // all are allowed access to item with embargoed expired

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemProperties(embargoedItem))));

        getClient().perform(get("/api/core/items/" + embargoedItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemProperties(embargoedItem))));
    }

    @Test
    public void undiscoverableAccessTest() throws Exception {
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

        //2. An undiscoverable item
        Item unDiscoverableYetAccessibleItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Undiscoverable item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .makeUnDiscoverable()
                .build();

        context.restoreAuthSystemState();


        //Anonymous users are allowed to access undiscoverable items
        getClient().perform(get("/api/core/items/" + unDiscoverableYetAccessibleItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(unDiscoverableYetAccessibleItem1,
                                "Undiscoverable item 1", "2017-10-17")
                )));


        //Admin users are allowed to acceess undiscoverable items
        String token1 = getAuthToken(admin.getEmail(), password);
        getClient(token1).perform(get("/api/core/items/" + unDiscoverableYetAccessibleItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(unDiscoverableYetAccessibleItem1,
                                "Undiscoverable item 1", "2017-10-17")
                )));

    }

    @Test
    public void privateGroupAccessTest() throws Exception {
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

        //2. An item restricted to a specific internal group
        Group staffGroup = GroupBuilder.createGroup(context)
                .withName("Staff")
                .build();

        Item restrictedItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Restricted item 1")
                .withIssueDate("2017-12-18")
                .withReaderGroup(staffGroup)
                .build();

        //3. A public item
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        //4. A member of the internal group
        EPerson staffEPerson = EPersonBuilder.createEPerson(context)
                .withEmail("professor@myuni.edu")
                .withPassword("s3cr3t")
                .withNameInMetadata("Doctor", "Professor")
                .withGroupMembership(staffGroup)
                .build();


        context.restoreAuthSystemState();

        //** THEN **
        //An anonymous user can view the public item
        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(
                                publicItem1, "Public item 1", "2017-10-17")
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/items")));

        //An anonymous user is not allowed to the restricted item
        getClient().perform(get("/api/core/items/" + restrictedItem1.getID()))
                .andExpect(status().isUnauthorized());

        //An admin user is allowed to access the restricted item
        String token1 = getAuthToken(admin.getEmail(), password);
        getClient(token1).perform(get("/api/core/items/" + restrictedItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(
                                restrictedItem1, "Restricted item 1", "2017-12-18")
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/items")));

        //A member of the internal group is also allowed to access the restricted item
        String token2 = getAuthToken("professor@myuni.edu", "s3cr3t");
        getClient(token2).perform(get("/api/core/items/" + restrictedItem1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ItemMatcher.matchItemWithTitleAndDateIssued(
                                restrictedItem1, "Restricted item 1", "2017-12-18")
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/items")));
    }

    @Test
    public void restrictedGroupAccessForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson memberRestrictGroup = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .build();

        Group restrictGroup = GroupBuilder.createGroup(context)
                .addMember(memberRestrictGroup)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .build();

        Item itemRestrictedByGroup = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2011-11-13")
                .withAuthor("Smith, Donald")
                .withReaderGroup(restrictGroup)
                .build();

        context.restoreAuthSystemState();

        //A member of the restricted group is also allowed access to restricted item
        String tokenMemberRestrictedGroup = getAuthToken(memberRestrictGroup.getEmail(), "qwerty01");
        getClient(tokenMemberRestrictedGroup).perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(ItemMatcher.matchItemProperties(itemRestrictedByGroup))));

        //members who are not part of the restricted group, have no access to the item
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isForbidden());

        getClient().perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void restrictedGroupAccessGrantAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson adminParentCommunity = EPersonBuilder.createEPerson(context)
                .withEmail("adminCommunity@mail.com")
                .withPassword("qwerty01")
                .build();

        Group restrictedGroup = GroupBuilder.createGroup(context)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(adminParentCommunity)
                .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();

        EPerson adminChild2 = EPersonBuilder.createEPerson(context)
                .withEmail("adminChild2@mail.com")
                .withPassword("qwerty05")
                .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community 2")
                .build();

        EPerson adminCollection1 = EPersonBuilder.createEPerson(context)
                .withEmail("adminCollection1@mail.com")
                .withPassword("qwerty02")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .withAdminGroup(adminCollection1)
                .build();

        EPerson adminCollection2 = EPersonBuilder.createEPerson(context)
                .withEmail("adminCollection2@mail.com")
                .withPassword("qwerty03")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 2")
                .withAdminGroup(adminCollection2)
                .build();

        Item itemRestrictedByGroup = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2015-10-21")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .withReaderGroup(restrictedGroup)
                .build();

        context.restoreAuthSystemState();
        // parent community's admin user is allowed access to restricted item
        String tokenAdminParentCommunity = getAuthToken(adminParentCommunity.getEmail(), "qwerty01");
        getClient(tokenAdminParentCommunity).perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(ItemMatcher.matchItemProperties(itemRestrictedByGroup))));

        // collection1's admin user is allowed access to restricted item
        String tokenAdminCollection1 = getAuthToken(adminCollection1.getEmail(), "qwerty02");
        getClient(tokenAdminCollection1).perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(ItemMatcher.matchItemProperties(itemRestrictedByGroup))));

        // collection2's admin user is NOT allowed access to restricted item of collection1
        String tokenAdminCollection2 = getAuthToken(adminCollection2.getEmail(), "qwerty03");
        getClient(tokenAdminCollection2).perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isForbidden());

        // child2's admin user is NOT allowed access to restricted item of collection1
        String tokenAdminChild2 = getAuthToken(adminChild2.getEmail(), "qwerty05");
        getClient(tokenAdminCollection2).perform(get("/api/core/items/" + itemRestrictedByGroup.getID()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateItem() throws Exception {

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

        context.restoreAuthSystemState();

        UUID idRef = null;
        AtomicReference<UUID> idRefNoEmbeds = new AtomicReference<UUID>();
        try {
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        ItemRest itemRestFull = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(true);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);

        itemRest.setMetadata(new MetadataRest()
                .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                .put("dc.description.abstract", new MetadataValueRest("Sample item created via the REST API"))
                .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                .put("dc.rights", new MetadataValueRest("Custom Copyright Text"))
                .put("dc.title", new MetadataValueRest("Title Text")));

        itemRestFull.setName("Practices of research data curation in institutional repositories:" +
                " A qualitative view from repository staff");
        itemRestFull.setInArchive(true);
        itemRestFull.setDiscoverable(true);
        itemRestFull.setWithdrawn(false);

        itemRestFull.setMetadata(new MetadataRest()
                .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                .put("dc.description.abstract", new MetadataValueRest("Sample item created via the REST API"))
                .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                .put("dc.rights", new MetadataValueRest("Custom Copyright Text"))
                .put("dc.title", new MetadataValueRest("Title Text")));

        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items?owningCollection=" +
                                                                col1.getID().toString())
                                   .content(mapper.writeValueAsBytes(itemRest)).contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String itemUuidString = String.valueOf(map.get("uuid"));
        String itemHandleString = String.valueOf(map.get("handle"));

        idRef = UUID.fromString(itemUuidString);
        //TODO Refactor this to use the converter to Item instead of checking every property separately
        getClient(token).perform(get("/api/core/items/" + idRef.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.id", is(itemUuidString)),
                            hasJsonPath("$.uuid", is(itemUuidString)),
                            hasJsonPath("$.name", is("Title Text")),
                            hasJsonPath("$.handle", is(itemHandleString)),
                            hasJsonPath("$.type", is("item")),
                            hasJsonPath("$.metadata", Matchers.allOf(
                                matchMetadata("dc.description",
                                    "<p>Some cool HTML code here</p>"),
                                matchMetadata("dc.description.abstract",
                                    "Sample item created via the REST API"),
                                matchMetadata("dc.description.tableofcontents",
                                    "<p>HTML News</p>"),
                                matchMetadata("dc.rights",
                                    "Custom Copyright Text"),
                                matchMetadata("dc.title",
                                    "Title Text")
                            )))));

        getClient(token).perform(post("/api/core/items?owningCollection=" +
                col1.getID().toString()).param("projection", "full")
                .content(mapper.writeValueAsBytes(itemRestFull)).contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", ItemMatcher.matchFullEmbeds()))
                .andDo(result -> idRefNoEmbeds
                        .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        } finally {
            ItemBuilder.deleteItem(idRef);
            ItemBuilder.deleteItem(idRefNoEmbeds.get());
        }
    }

    @Test
    public void updateTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String itemUuidString = null;
        try {
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(true);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);


        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items?owningCollection=" +
                                                                col1.getID().toString())
                                                           .content(mapper.writeValueAsBytes(itemRest))
                                                           .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        itemUuidString = String.valueOf(map.get("uuid"));
        String itemHandleString = String.valueOf(map.get("handle"));

        itemRest.setMetadata(new MetadataRest()
                .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                .put("dc.description.abstract", new MetadataValueRest("Sample item created via the REST API"))
                .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                .put("dc.rights", new MetadataValueRest("New Custom Copyright Text"))
                .put("dc.title", new MetadataValueRest("New title")));

        itemRest.setUuid(itemUuidString);
        itemRest.setHandle(itemHandleString);

        getClient(token).perform(put("/api/core/items/" + itemUuidString)
                                                           .content(mapper.writeValueAsBytes(itemRest))
                                                           .contentType(contentType))
                                              .andExpect(status().isOk());

        //TODO Refactor this to use the converter to Item instead of checking every property separately
        getClient(token).perform(get("/api/core/items/" + itemUuidString))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.id", is(itemUuidString)),
                            hasJsonPath("$.uuid", is(itemUuidString)),
                            hasJsonPath("$.name", is("New title")),
                            hasJsonPath("$.handle", is(itemHandleString)),
                            hasJsonPath("$.type", is("item")),
                            hasJsonPath("$.metadata", Matchers.allOf(
                                matchMetadata("dc.description",
                                    "<p>Some cool HTML code here</p>"),
                                matchMetadata("dc.description.abstract",
                                    "Sample item created via the REST API"),
                                matchMetadata("dc.description.tableofcontents",
                                    "<p>HTML News</p>"),
                                matchMetadata("dc.rights",
                                    "New Custom Copyright Text"),
                                matchMetadata("dc.title",
                                    "New title")
                            )))));
        } finally {
            ItemBuilder.deleteItem(UUID.fromString(itemUuidString));
        }
    }


    @Test
    public void testDeleteItem() throws Exception {

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


        context.restoreAuthSystemState();
        String itemUuidString = null;
        try {
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(true);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);

        itemRest.setMetadata(new MetadataRest()
                .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                .put("dc.description.abstract", new MetadataValueRest("Sample item created via the REST API"))
                .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                .put("dc.rights", new MetadataValueRest("Custom Copyright Text"))
                .put("dc.title", new MetadataValueRest("Title Text")));

        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items?owningCollection=" +
                                                                col1.getID().toString())
                                                           .content(mapper.writeValueAsBytes(itemRest))
                                                           .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        itemUuidString = String.valueOf(map.get("uuid"));
        String itemHandleString = String.valueOf(map.get("handle"));

        //TODO Refactor this to use the converter to Item instead of checking every property separately
        getClient(token).perform(get("/api/core/items/" + itemUuidString))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.id", is(itemUuidString)),
                            hasJsonPath("$.uuid", is(itemUuidString)),
                            hasJsonPath("$.name", is("Title Text")),
                            hasJsonPath("$.handle", is(itemHandleString)),
                            hasJsonPath("$.type", is("item")),
                            hasJsonPath("$.metadata", Matchers.allOf(
                                matchMetadata("dc.description",
                                    "<p>Some cool HTML code here</p>"),
                                matchMetadata("dc.description.abstract",
                                    "Sample item created via the REST API"),
                                matchMetadata("dc.description.tableofcontents",
                                    "<p>HTML News</p>"),
                                matchMetadata("dc.rights",
                                    "Custom Copyright Text"),
                                matchMetadata("dc.title",
                                    "Title Text")
                            )))));

        getClient(token).perform(delete("/api/core/items/" + itemUuidString))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/items/" + itemUuidString))
                        .andExpect(status().isNotFound());
        } finally {
            ItemBuilder.deleteItem(UUID.fromString(itemUuidString));
        }
    }

    @Test
    public void testDeleteItemUnauthorized() throws Exception {

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

        context.restoreAuthSystemState();

        String itemUuidString = null;
        try {
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(true);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);

        itemRest.setMetadata(new MetadataRest()
                .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                .put("dc.description.abstract", new MetadataValueRest("Sample item created via the REST API"))
                .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                .put("dc.rights", new MetadataValueRest("Custom Copyright Text"))
                .put("dc.title", new MetadataValueRest("Title Text")));

        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items?owningCollection=" +
                                                                col1.getID().toString())
                                                           .content(mapper.writeValueAsBytes(itemRest))
                                                           .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        itemUuidString = String.valueOf(map.get("uuid"));
        String itemHandleString = String.valueOf(map.get("handle"));

        //TODO Refactor this to use the converter to Item instead of checking every property separately
        getClient(token).perform(get("/api/core/items/" + itemUuidString))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.id", is(itemUuidString)),
                            hasJsonPath("$.uuid", is(itemUuidString)),
                            hasJsonPath("$.name", is("Title Text")),
                            hasJsonPath("$.handle", is(itemHandleString)),
                            hasJsonPath("$.type", is("item")),
                            hasJsonPath("$.metadata", Matchers.allOf(
                                matchMetadata("dc.description",
                                    "<p>Some cool HTML code here</p>"),
                                matchMetadata("dc.description.abstract",
                                    "Sample item created via the REST API"),
                                matchMetadata("dc.description.tableofcontents",
                                    "<p>HTML News</p>"),
                                matchMetadata("dc.rights",
                                    "Custom Copyright Text"),
                                matchMetadata("dc.title",
                                    "Title Text")
                            )))));

        getClient().perform(delete("/api/core/items/" + itemUuidString))
                        .andExpect(status().isUnauthorized());

        getClient(token).perform(get("/api/core/items/" + itemUuidString))
                        .andExpect(status().isOk());
        } finally {
            ItemBuilder.deleteItem(UUID.fromString(itemUuidString));
        }
    }

    @Test
    public void deleteOneWrongUuidResourceNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. One public item, one workspace item and one template item.
        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                     .withSubject("ExtraEntry")
                                     .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        //Delete public item
        getClient(token).perform(delete("/api/core/items/" + parentCommunity.getID()))
                        .andExpect(status().is(404));
    }

    @Test
    public void patchItemMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchItemMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1).build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/items/" + item.getID(), expectedStatus);
    }

    /**
     * This test will try creating an item with the InArchive property set to false. This endpoint does not allow
     * us to create Items which aren't final (final means that they'd be in archive) and thus it'll throw a
     * BadRequestException which is what we're testing for
     * @throws Exception    If something goes wrong
     */
    @Test
    public void testCreateItemInArchiveFalseBadRequestException() throws Exception {

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

        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(false);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);

        itemRest.setMetadata(new MetadataRest()
                                 .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                                 .put("dc.description.abstract",
                                      new MetadataValueRest("Sample item created via the REST API"))
                                 .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                                 .put("dc.rights", new MetadataValueRest("Custom Copyright Text"))
                                 .put("dc.title", new MetadataValueRest("Title Text")));

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + col1.getID().toString())
                                     .content(mapper.writeValueAsBytes(itemRest)).contentType(contentType))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateItemInvalidCollectionBadRequestException() throws Exception {

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

        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(false);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);

        itemRest.setMetadata(new MetadataRest()
                                 .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                                 .put("dc.description.abstract",
                                      new MetadataValueRest("Sample item created via the REST API"))
                                 .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                                 .put("dc.rights", new MetadataValueRest("Custom Copyright Text"))
                                 .put("dc.title", new MetadataValueRest("Title Text")));

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + parentCommunity.getID().toString())
                                     .content(mapper.writeValueAsBytes(itemRest)).contentType(contentType))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void updateTestEPersonWithoutPermissionForbidden() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String itemUuidString = null;
        try {
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = new ItemRest();
        itemRest.setName("Practices of research data curation in institutional repositories:" +
                             " A qualitative view from repository staff");
        itemRest.setInArchive(true);
        itemRest.setDiscoverable(true);
        itemRest.setWithdrawn(false);


        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items?owningCollection=" +
                                                                col1.getID().toString())
                                                           .content(mapper.writeValueAsBytes(itemRest))
                                                           .contentType(contentType))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        itemUuidString = String.valueOf(map.get("uuid"));
        String itemHandleString = String.valueOf(map.get("handle"));

        itemRest.setMetadata(new MetadataRest()
                                 .put("dc.description", new MetadataValueRest("<p>Some cool HTML code here</p>"))
                                 .put("dc.description.abstract",
                                      new MetadataValueRest("Sample item created via the REST API"))
                                 .put("dc.description.tableofcontents", new MetadataValueRest("<p>HTML News</p>"))
                                 .put("dc.rights", new MetadataValueRest("New Custom Copyright Text"))
                                 .put("dc.title", new MetadataValueRest("New title")));

        itemRest.setUuid(itemUuidString);
        itemRest.setHandle(itemHandleString);

        token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(put("/api/core/items/" + itemUuidString)
                                                 .content(mapper.writeValueAsBytes(itemRest))
                                                 .contentType(contentType))
                                    .andExpect(status().isForbidden());
        } finally {
            ItemBuilder.deleteItem(UUID.fromString(itemUuidString));
        }
    }

    @Test
    public void createItemFromExternalSources() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String itemUuidString = null;
        try {
        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/core/items?owningCollection="
                                                                + col1.getID().toString())
                                     .contentType(org.springframework.http.MediaType.parseMediaType(
                                         org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mock/entryValues/one")).andExpect(status().isCreated()).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        itemUuidString = String.valueOf(map.get("uuid"));
        String itemHandleString = String.valueOf(map.get("handle"));

        getClient(token).perform(get("/api/core/items/" + itemUuidString))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.id", is(itemUuidString)),
                            hasJsonPath("$.uuid", is(itemUuidString)),
                            hasJsonPath("$.handle", is(itemHandleString)),
                            hasJsonPath("$.type", is("item")),
                            hasJsonPath("$.metadata", Matchers.allOf(
                                matchMetadata("dc.contributor.author", "Donald, Smith")
                            )))));
        } finally {
            ItemBuilder.deleteItem(UUID.fromString(itemUuidString));
        }
    }

    @Test
    public void createItemFromExternalSourcesNoOwningCollectionUuidBadRequest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items")
                                   .contentType(org.springframework.http.MediaType.parseMediaType(
                                       org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                   .content("https://localhost:8080/server/api/integration/externalsources/" +
                                        "mock/entryValues/one")).andExpect(status().isBadRequest());
    }

    @Test
    public void createItemFromExternalSourcesRandomOwningCollectionUuidBadRequest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + UUID.randomUUID())
                                     .contentType(org.springframework.http.MediaType.parseMediaType(
                                         org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                          "mock/entryValues/one")).andExpect(status().isBadRequest());
    }

    @Test
    public void createItemFromExternalSourcesWrongUriList() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + col1.getID().toString())
                                    .contentType(org.springframework.http.MediaType.parseMediaType(
                                        org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                    .content("https://localhost:8080/server/mock/mock/mock/" +
                                        "mock/entryValues/one")).andExpect(status().isBadRequest());
    }

    @Test
    public void createItemFromExternalSourcesWrongSourceBadRequest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + col1.getID().toString())
                                    .contentType(org.springframework.http.MediaType.parseMediaType(
                                        org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                    .content("https://localhost:8080/server/api/integration/externalsources/" +
                                        "mockWrongSource/entryValues/one")).andExpect(status().isBadRequest());

    }

    @Test
    public void createItemFromExternalSourcesWrongIdResourceNotFound() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + col1.getID().toString())
                                     .contentType(org.springframework.http.MediaType.parseMediaType(
                                         org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mock/entryValues/azeazezaezeaz")).andExpect(status().is(404));

    }

    @Test
    public void createItemFromExternalSourcesForbidden() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/items?owningCollection=" + col1.getID().toString())
                                    .contentType(org.springframework.http.MediaType.parseMediaType(
                                        org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                    .content("https://localhost:8080/server/api/integration/externalsources/" +
                                        "mock/entryValues/one")).andExpect(status().isForbidden());
    }

    @Test
    public void createItemFromExternalSourcesUnauthorized() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        context.restoreAuthSystemState();

        getClient().perform(post("/api/core/items?owningCollection=" + col1.getID().toString())
                                     .contentType(org.springframework.http.MediaType.parseMediaType(
                                         org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                                     .content("https://localhost:8080/server/api/integration/externalsources/" +
                                                  "mock/entryValues/one")).andExpect(status().isUnauthorized());
    }

    @Test
    public void specificEmbedTestMultipleLevelOfLinks() throws Exception {
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

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();


        //Add a bitstream to an item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/" + publicItem1.getID() +
                                    "?embed=owningCollection/mappedItems/bundles/" +
                                    "bitstreams&embed=owningCollection/logo"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(publicItem1)))
                   .andExpect(jsonPath("$._embedded.owningCollection",
                                       CollectionMatcher.matchCollectionEntry(col1.getName(),
                                                                              col1.getID(),
                                                                              col1.getHandle())))
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.bundles").doesNotExist())
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.relationships").doesNotExist())
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.defaultAccessConditions")
                                  .doesNotExist())
                   // .nullValue() makes sure that it could be embedded, it's just null in this case
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.logo", Matchers.nullValue()))
                   // .empty() makes sure that the embed is there, but that there's no actual data
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.mappedItems._embedded.mappedItems",
                                       Matchers.empty()))
        ;
    }

    @Test
    public void specificEmbedTestMultipleLevelOfLinksWithData() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withLogo("TestingContentForLogo").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();


        collectionService.addItem(context, col1, publicItem2);

        //Add a bitstream to an item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        String bitstreamContent2 = "ThisIsSomeDummyText";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent2, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                                             createBitstream(context, publicItem2, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .build();
        }


        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/" + publicItem1.getID() +
                                    "?embed=owningCollection/mappedItems/bundles/" +
                                    "bitstreams&embed=owningCollection/logo"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(publicItem1)))
                   .andExpect(jsonPath("$._embedded.owningCollection",
                                       CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(),
                                                                              col1.getHandle())))
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.bundles").doesNotExist())
                   .andExpect(jsonPath("$._embedded.relationships").doesNotExist())
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.defaultAccessConditions")
                                  .doesNotExist())
                   // .notNullValue() makes sure that it's there and that it does actually contain a value, but not null
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.logo", Matchers.notNullValue()))
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.logo._embedded").doesNotExist())
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.mappedItems._embedded.mappedItems",
                                       Matchers.contains(ItemMatcher.matchItemProperties(publicItem2))))
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.mappedItems._embedded" +
                                           ".mappedItems[0]._embedded.bundles._embedded.bundles[0]._embedded" +
                                           ".bitstreams._embedded.bitstreams", Matchers.contains(
                       BitstreamMatcher.matchBitstreamEntryWithoutEmbed(bitstream2.getID(), bitstream2.getSizeBytes())
                   )))
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.mappedItems." +
                                           "_embedded.mappedItems[0]_embedded.relationships").doesNotExist())
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.mappedItems" +
                                           "._embedded.mappedItems[0]._embedded.bundles._embedded.bundles[0]." +
                                           "_embedded.primaryBitstream").doesNotExist())
                   .andExpect(jsonPath("$._embedded.owningCollection._embedded.mappedItems." +
                                           "_embedded.mappedItems[0]._embedded.bundles._embedded.bundles[0]." +
                                           "_embedded.bitstreams._embedded.bitstreams[0]._embedded.format")
                                  .doesNotExist())
        ;
    }

    @Test
    public void testHiddenMetadataForAnonymousUser() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withProvenanceData("Provenance data")
                                      .build();

        context.restoreAuthSystemState();


        getClient().perform(get("/api/core/items/" + item.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Public item 1")))
                   .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")));
    }

    @Test
    public void testHiddenMetadataForAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withProvenanceData("Provenance data")
                               .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/items/" + item.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Public item 1")))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.description.provenance", "Provenance data")));
    }

    @Test
    public void testHiddenMetadataForUserWithWriteRights() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withProvenanceData("Provenance data")
                               .build();

        context.restoreAuthSystemState();


        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withUser(eperson)
                             .withAction(WRITE)
                             .withDspaceObject(item)
                             .build();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Public item 1")))
                        .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")));

    }

    @Test
    public void findOneTestWithEmbedsWithNoPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Item").build();

        Bundle bundle0 = BundleBuilder.createBundle(context, item).withName("Bundle 0").build();
        Bundle bundle1 = BundleBuilder.createBundle(context, item).withName("Bundle 1").build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item).withName("Bundle 2").build();
        Bundle bundle3 = BundleBuilder.createBundle(context, item).withName("Bundle 3").build();
        Bundle bundle4 = BundleBuilder.createBundle(context, item).withName("Bundle 4").build();
        Bundle bundle5 = BundleBuilder.createBundle(context, item).withName("Bundle 5").build();
        Bundle bundle6 = BundleBuilder.createBundle(context, item).withName("Bundle 6").build();
        Bundle bundle7 = BundleBuilder.createBundle(context, item).withName("Bundle 7").build();
        Bundle bundle8 = BundleBuilder.createBundle(context, item).withName("Bundle 8").build();
        Bundle bundle9 = BundleBuilder.createBundle(context, item).withName("Bundle 9").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                                    .param("embed", "bundles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
        .andExpect(jsonPath("$._embedded.bundles._embedded.bundles",Matchers.containsInAnyOrder(
            BundleMatcher.matchProperties(bundle0.getName(), bundle0.getID(), bundle0.getHandle(), bundle0.getType()),
            BundleMatcher.matchProperties(bundle1.getName(), bundle1.getID(), bundle1.getHandle(), bundle1.getType()),
            BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(), bundle2.getHandle(), bundle2.getType()),
            BundleMatcher.matchProperties(bundle3.getName(), bundle3.getID(), bundle3.getHandle(), bundle3.getType()),
            BundleMatcher.matchProperties(bundle4.getName(), bundle4.getID(), bundle4.getHandle(), bundle4.getType()),
            BundleMatcher.matchProperties(bundle5.getName(), bundle5.getID(), bundle5.getHandle(), bundle5.getType()),
            BundleMatcher.matchProperties(bundle6.getName(), bundle6.getID(), bundle6.getHandle(), bundle6.getType()),
            BundleMatcher.matchProperties(bundle7.getName(), bundle7.getID(), bundle7.getHandle(), bundle7.getType()),
            BundleMatcher.matchProperties(bundle8.getName(), bundle8.getID(), bundle8.getHandle(), bundle8.getType()),
            BundleMatcher.matchProperties(bundle9.getName(), bundle9.getID(), bundle9.getHandle(), bundle9.getType())
        )))
        .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items/" + item.getID())))
        .andExpect(jsonPath("$._embedded.bundles.page.size", is(20)))
        .andExpect(jsonPath("$._embedded.bundles.page.totalElements", is(10)));
    }

    @Test
    public void findOneTestWithEmbedsWithPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Item").build();

        Bundle bundle0 = BundleBuilder.createBundle(context, item).withName("Bundle 0").build();
        Bundle bundle1 = BundleBuilder.createBundle(context, item).withName("Bundle 1").build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item).withName("Bundle 2").build();
        Bundle bundle3 = BundleBuilder.createBundle(context, item).withName("Bundle 3").build();
        Bundle bundle4 = BundleBuilder.createBundle(context, item).withName("Bundle 4").build();
        Bundle bundle5 = BundleBuilder.createBundle(context, item).withName("Bundle 5").build();
        Bundle bundle6 = BundleBuilder.createBundle(context, item).withName("Bundle 6").build();
        Bundle bundle7 = BundleBuilder.createBundle(context, item).withName("Bundle 7").build();
        Bundle bundle8 = BundleBuilder.createBundle(context, item).withName("Bundle 8").build();
        Bundle bundle9 = BundleBuilder.createBundle(context, item).withName("Bundle 9").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                                    .param("embed", "bundles")
                           .param("embed.size", "bundles=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
        .andExpect(jsonPath("$._embedded.bundles._embedded.bundles",Matchers.containsInAnyOrder(
            BundleMatcher.matchProperties(bundle0.getName(), bundle0.getID(), bundle0.getHandle(), bundle0.getType()),
            BundleMatcher.matchProperties(bundle1.getName(), bundle1.getID(), bundle1.getHandle(), bundle1.getType()),
            BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(), bundle2.getHandle(), bundle2.getType()),
            BundleMatcher.matchProperties(bundle3.getName(), bundle3.getID(), bundle3.getHandle(), bundle3.getType()),
            BundleMatcher.matchProperties(bundle4.getName(), bundle4.getID(), bundle4.getHandle(), bundle4.getType())
        )))
        .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items/" + item.getID())))
        .andExpect(jsonPath("$._embedded.bundles.page.size", is(5)))
        .andExpect(jsonPath("$._embedded.bundles.page.totalElements", is(10)));
    }


    @Test
    public void findOneTestWithEmbedsWithInvalidPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Item").build();

        Bundle bundle0 = BundleBuilder.createBundle(context, item).withName("Bundle 0").build();
        Bundle bundle1 = BundleBuilder.createBundle(context, item).withName("Bundle 1").build();
        Bundle bundle2 = BundleBuilder.createBundle(context, item).withName("Bundle 2").build();
        Bundle bundle3 = BundleBuilder.createBundle(context, item).withName("Bundle 3").build();
        Bundle bundle4 = BundleBuilder.createBundle(context, item).withName("Bundle 4").build();
        Bundle bundle5 = BundleBuilder.createBundle(context, item).withName("Bundle 5").build();
        Bundle bundle6 = BundleBuilder.createBundle(context, item).withName("Bundle 6").build();
        Bundle bundle7 = BundleBuilder.createBundle(context, item).withName("Bundle 7").build();
        Bundle bundle8 = BundleBuilder.createBundle(context, item).withName("Bundle 8").build();
        Bundle bundle9 = BundleBuilder.createBundle(context, item).withName("Bundle 9").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                                    .param("embed", "bundles")
                                    .param("embed.size", "bundles=invalidPage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
        .andExpect(jsonPath("$._embedded.bundles._embedded.bundles",Matchers.containsInAnyOrder(
            BundleMatcher.matchProperties(bundle0.getName(), bundle0.getID(), bundle0.getHandle(), bundle0.getType()),
            BundleMatcher.matchProperties(bundle1.getName(), bundle1.getID(), bundle1.getHandle(), bundle1.getType()),
            BundleMatcher.matchProperties(bundle2.getName(), bundle2.getID(), bundle2.getHandle(), bundle2.getType()),
            BundleMatcher.matchProperties(bundle3.getName(), bundle3.getID(), bundle3.getHandle(), bundle3.getType()),
            BundleMatcher.matchProperties(bundle4.getName(), bundle4.getID(), bundle4.getHandle(), bundle4.getType()),
            BundleMatcher.matchProperties(bundle5.getName(), bundle5.getID(), bundle5.getHandle(), bundle5.getType()),
            BundleMatcher.matchProperties(bundle6.getName(), bundle6.getID(), bundle6.getHandle(), bundle6.getType()),
            BundleMatcher.matchProperties(bundle7.getName(), bundle7.getID(), bundle7.getHandle(), bundle7.getType()),
            BundleMatcher.matchProperties(bundle8.getName(), bundle8.getID(), bundle8.getHandle(), bundle8.getType()),
            BundleMatcher.matchProperties(bundle9.getName(), bundle9.getID(), bundle9.getHandle(), bundle9.getType())
        )))
        .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items/" + item.getID())))
        .andExpect(jsonPath("$._embedded.bundles.page.size", is(20)))
        .andExpect(jsonPath("$._embedded.bundles.page.totalElements", is(10)));
}

    @Test
    public void findOneTestWithMultiLevelEmbedsWithNoPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Item").build();

        Bundle bundle0 = BundleBuilder.createBundle(context, item).withName("Bundle 0").build();

        String bitstreamContent = "ThisIsSomeDummyText";

        Bitstream bitstream0;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream0 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream0")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream1;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream2;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream3;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream3 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream3")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream4;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream4 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream4")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream5;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream5 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream5")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream6;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream6 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream6")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream7;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream7 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream7")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream8;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream8 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream8")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream9;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream9 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream9")
                                         .withMimeType("text/plain")
                                         .build();
        }
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                                    .param("embed", "bundles/bitstreams"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles", Matchers.containsInAnyOrder(
                           BundleMatcher.matchProperties(bundle0.getName(), bundle0.getID(), bundle0.getHandle(),
                                                         bundle0.getType())
                   )))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]._embedded.bitstreams" +
                                               "._embedded.bitstreams",
                                    Matchers.containsInAnyOrder(
                                            BitstreamMatcher.matchProperties(bitstream0),
                                            BitstreamMatcher.matchProperties(bitstream1),
                                            BitstreamMatcher.matchProperties(bitstream2),
                                            BitstreamMatcher.matchProperties(bitstream3),
                                            BitstreamMatcher.matchProperties(bitstream4),
                                            BitstreamMatcher.matchProperties(bitstream5),
                                            BitstreamMatcher.matchProperties(bitstream6),
                                            BitstreamMatcher.matchProperties(bitstream7),
                                            BitstreamMatcher.matchProperties(bitstream8),
                                            BitstreamMatcher.matchProperties(bitstream9)
                                    )))
                   .andExpect(
                           jsonPath("$._links.self.href", Matchers.containsString("/api/core/items/" + item.getID())))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]." +
                                               "_embedded.bitstreams.page.size", is(20)))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]" +
                                               "._embedded.bitstreams.page.totalElements",
                                       is(10)));
    }

    @Test
    public void findOneTestWithMultiLevelEmbedsWithPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Item").build();

        Bundle bundle0 = BundleBuilder.createBundle(context, item).withName("Bundle 0").build();

        String bitstreamContent = "ThisIsSomeDummyText";

        Bitstream bitstream0;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream0 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream0")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream1;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream2;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream3;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream3 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream3")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream4;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream4 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream4")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream5;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream5 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream5")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream6;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream6 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream6")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream7;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream7 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream7")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream8;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream8 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream8")
                                         .withMimeType("text/plain")
                                         .build();
        }
        Bitstream bitstream9;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream9 = BitstreamBuilder.createBitstream(context, bundle0, is)
                                         .withName("Bitstream9")
                                         .withMimeType("text/plain")
                                         .build();
        }
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                                    .param("embed", "bundles/bitstreams")
                                    .param("embed.size", "bundles/bitstreams=5"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles", Matchers.containsInAnyOrder(
                           BundleMatcher.matchProperties(bundle0.getName(), bundle0.getID(), bundle0.getHandle(),
                                                         bundle0.getType())
                   )))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]._embedded.bitstreams" +
                                               "._embedded.bitstreams",
                                    Matchers.containsInAnyOrder(
                                            BitstreamMatcher.matchProperties(bitstream0),
                                            BitstreamMatcher.matchProperties(bitstream1),
                                            BitstreamMatcher.matchProperties(bitstream2),
                                            BitstreamMatcher.matchProperties(bitstream3),
                                            BitstreamMatcher.matchProperties(bitstream4)
                                    )))
                   .andExpect(
                           jsonPath("$._links.self.href", Matchers.containsString("/api/core/items/" + item.getID())))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]" +
                                               "._embedded.bitstreams.page.size", is(5)))
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]" +
                                               "._embedded.bitstreams.page.totalElements",
                                       is(10)));
    }





}
