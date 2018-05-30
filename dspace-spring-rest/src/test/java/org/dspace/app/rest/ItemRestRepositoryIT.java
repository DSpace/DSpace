/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;

public class ItemRestRepositoryIT extends AbstractControllerIntegrationTest {

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


        getClient().perform(get("/api/core/items"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.items", Matchers.containsInAnyOrder(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "Public item 1", "2017-10-17"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3, "Public item 3", "2016-02-13")
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;
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

        getClient().perform(get("/api/core/items")
                   .param("size", "2"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.items", Matchers.containsInAnyOrder(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "Public item 1", "2017-10-17"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13")
                   )))
                   .andExpect(jsonPath("$._embedded.items", Matchers.not(
                       Matchers.contains(
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3, "Public item 3", "2016-02-13")
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/items")
                   .param("size", "2")
                   .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.items", Matchers.contains(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3, "Public item 3", "2016-02-13")
                   )))
                   .andExpect(jsonPath("$._embedded.items", Matchers.not(
                       Matchers.contains(
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "Public item 1", "2017-10-17"),
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13")
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
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


        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "Public item 1", "2017-10-17")
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       Matchers.is(
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13")
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;
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

        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "Public item 1", "2017-10-17")
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       Matchers.is(
                           ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13")
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/bitstreams"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .containsString("/api/core/items/" + publicItem1.getID() + "/bitstreams")))
        ;

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/owningCollection"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/collections")))
        ;

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/templateItemOf"))
                   .andExpect(status().isNoContent())
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

        getClient().perform(get("/api/core/items/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound())
        ;

    }

    @Test
    public void widthdrawPatchTest() throws Exception {
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

        makeUserAdmin();

        // A token must be provided for withdraw operation. The person
        // is used in the provenance note.
        String token = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList();
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

        // item already withdrawn, no-op, 200 response
        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

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

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        makeUserAdmin();

        String token = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", null);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());
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

        //2. One public item
        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Public item 1")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .withSubject("ExtraEntry")
                               .build();

        makeUserAdmin();

        // A token must be provided for reinstate operation. The person
        // is used in the provenance note.
        String token = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList();
        // first, withdraw item
        ReplaceOperation initalizeOperation = new ReplaceOperation("/withdrawn", true);
        ops.add(initalizeOperation);
        String patchInitialState = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchInitialState)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        // now reinstate item
        ops.clear();
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
                               .makePrivate()
                               .build();
        List<Operation> ops = new ArrayList();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make discoverable
        getClient().perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                        .andExpect(jsonPath("$.discoverable", Matchers.is(true)));

    }

    @Test
    public void makePrivatePatchTest() throws Exception {
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

        List<Operation> ops = new ArrayList();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", false);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        // make private
        getClient().perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.uuid", Matchers.is(item.getID().toString())))
                   .andExpect(jsonPath("$.discoverable", Matchers.is(false)));

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

        String token = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", null);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(token).perform(patch("/api/core/items/" + item.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isBadRequest());
    }

}
