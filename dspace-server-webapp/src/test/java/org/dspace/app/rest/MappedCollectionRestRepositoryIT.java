/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.SearchResultMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;

public class MappedCollectionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void itemHasNoExtraCollectionsAndCollectionHasNoExtraItemsTest() throws Exception {
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

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

//        collectionService.addItem(context, colAA2, publicItem1);
//        collectionService.update(context, colAA2);
//        itemService.update(context, publicItem1);

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.not(Matchers.contains(
                       CollectionMatcher.matchCollectionEntry("Collection 1", col1.getID(), col1.getHandle()),
                       CollectionMatcher.matchCollectionEntry("Collection 3", col2.getID(), col2.getHandle()))
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )));
        getClient().perform(get("/api/core/collections/" + col2.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )));
    }

    @Test
    public void itemAndCollectionHaveOneMappingTest() throws Exception {
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

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(
                post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n"
                        )
        );

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntry("Collection 2", col2.getID(), col2.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
        getClient().perform(get("/api/core/collections/" + col2.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                ))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(1)));
    }

    @Test
    public void itemAndTwoCollectionsHaveTwoMappingsTest() throws Exception {
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
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3").build();

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
            .perform(post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                    .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                    .content(
                            "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col3.getID() + "\n"
                    )
            );

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntry("Collection 2", col2.getID(), col2.getHandle()),
                       CollectionMatcher.matchCollectionEntry("Collection 3", col3.getID(), col3.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
        getClient().perform(get("/api/core/collections/" + col2.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                ))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(1)));
        getClient().perform(get("/api/core/collections/" + col3.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                ))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(1)));
    }

    @Test
    public void itemHasNoDuplicatesInMappedCollectionAndCollectionHasNoDuplicatesInMappedItemsTest()
        throws Exception {
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
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3").build();

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
            .perform(post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                    .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                    .content(
                            "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col3.getID()
                    )
            );

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntry("Collection 2", col2.getID(), col2.getHandle()),
                       CollectionMatcher.matchCollectionEntry("Collection 3", col3.getID(), col3.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;

        getClient().perform(get("/api/core/collections/" + col2.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.containsInAnyOrder(
                        ItemMatcher.matchItemProperties(publicItem1),
                        ItemMatcher.matchItemProperties(publicItem1)))
                ));
    }

    @Test
    public void itemHasNoOriginalCollectionInMappedCollectionAndCollectionHasNoOriginalItemInMappedItemsTest()
        throws Exception {
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
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3").build();

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
            .perform(post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                    .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                    .content(
                            "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col3.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col3.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col1.getID() + "\n"
                    )
            );

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.not(Matchers.contains(
                       CollectionMatcher.matchCollectionEntry("Collection 1", col1.getID(), col1.getHandle())
                   ))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;
        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
    }

    @Test
    public void removeMappedCollectionTest() throws Exception {
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
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3").build();

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
            .perform(post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                    .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                    .content(
                            "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID() + "\n" +
                            "https://localhost:8080/spring-rest/api/core/collections/" + col3.getID() + "\n"
                    )
            );

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.not(Matchers.contains(
                       CollectionMatcher.matchCollectionEntry("Collection 1", col1.getID(), col1.getHandle()))
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;
        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));

        getClient(adminToken)
            .perform(delete("/api/core/items/" + publicItem1.getID() + "/mappedCollections/" + col2.getID()));

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.not(Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntry("Collection 2", col2.getID(), col2.getHandle()),
                       CollectionMatcher.matchCollectionEntry("Collection 1", col1.getID(), col1.getHandle())
                   ))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")));
        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
        getClient().perform(get("/api/core/collections/" + col2.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
        getClient().perform(get("/api/core/collections/" + col3.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                ))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(1)));

        getClient(adminToken)
            .perform(delete("/api/core/items/" + publicItem1.getID() + "/mappedCollections/" + col1.getID()));

        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.not(Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntry("Collection 2", col2.getID(), col2.getHandle()),
                       CollectionMatcher.matchCollectionEntry("Collection 1", col1.getID(), col1.getHandle())
                   ))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;
        getClient().perform(get("/api/core/collections/" + col1.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
        getClient().perform(get("/api/core/collections/" + col2.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.not(Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                )))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(0)));
        getClient().perform(get("/api/core/collections/" + col3.getID() + "/mappedItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.contains(
                        ItemMatcher.matchItemProperties(publicItem1))
                ))
                .andExpect(jsonPath("$._embedded.mappedItems", Matchers.hasSize(1)));
    }

    @Test
    public void doNotAllowMappedCollectionIfGivenCollectionIsOwningCollectionOfGivenItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and a collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/collections/" + col1.getID())
                )
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void doNotAllowDeleteMappedCollectionIfGivenCollectionIsOwningCollectionOfGivenItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and a collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(delete("/api/core/items/" + publicItem1.getID() + "/mappedCollections/"
                        + col1.getID())                )
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void doNotAllowMappedCollectionWithATemplateItem() throws Exception {
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
                .withTemplateItem()
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .build();

        //2. A template item for the 1st collection
        Item templateItem = col1.getTemplateItem();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(post("/api/core/items/" + templateItem.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/collections/" + col2.getID())
                )
                .andExpect(status().is(405));
    }

    @Test
    public void doNotAllowDeleteMappedCollectionWithATemplateItem() throws Exception {
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
                .withTemplateItem()
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .build();

        //2. A template item for the 1st collection
        Item templateItem = col1.getTemplateItem();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(delete("/api/core/items/" + templateItem.getID() + "/mappedCollections/"
                        + col2.getID()))
                .andExpect(status().is(405));
    }

    @Test
    public void mappedCollectionNeedsValidIDs() throws Exception {
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
                .build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/collections/" + "badCollectionID")
                )
                .andExpect(status().isUnprocessableEntity());

        getClient(adminToken)
                .perform(post("/api/core/items/" + "badItemID" + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/collections/" + col1.getID())
                )
                .andExpect(status().is(405));
    }

    @Test
    public void itemHasNoExtraCollectionsCanBeRetrievedAnonymouslyTest() throws Exception {

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

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

//        collectionService.addItem(context, colAA2, publicItem1);
//        collectionService.update(context, colAA2);
//        itemService.update(context, publicItem1);

        context.restoreAuthSystemState();
        context.setCurrentUser(null);
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/mappedCollections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.mappedCollections", Matchers.not(Matchers.contains(
                       CollectionMatcher.matchCollectionEntry("Collection 1", col1.getID(), col1.getHandle()),
                       CollectionMatcher.matchCollectionEntry("Collection 3", col2.getID(), col2.getHandle()))
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/items")))
        ;
    }

    @Test
    public void mappingNewCollectionCannotBeDoneAnonymouslyTest() throws Exception {

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

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

//        collectionService.addItem(context, colAA2, publicItem1);
//        collectionService.update(context, colAA2);
//        itemService.update(context, publicItem1);

        getClient().perform(
                post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                        )
        ).andExpect(status().is(401));
    }

    @Test
    public void removingMappedCollectionCannotBeDoneAnonymouslyTest() throws Exception {

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

        //2. Public item that is readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

//        collectionService.addItem(context, colAA2, publicItem1);
//        collectionService.update(context, colAA2);
//        itemService.update(context, publicItem1);


        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(
                post("/api/core/items/" + publicItem1.getID() + "/mappedCollections/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                        )
        );

        getClient().perform(delete("/api/core/items/" + publicItem1.getID() + "/mappedCollections/"
                                       + col2.getID()))
            .andExpect(status().is(401));


    }

    @Test
    public void mappedItemAppearsInCollectionBrowseTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection source = CollectionBuilder.createCollection(context, parentCommunity).withName("Source").build();
        Collection sink = CollectionBuilder.createCollection(context, parentCommunity).withName("Sink").build();

        //2. An item in the source collection
        Item item = ItemBuilder.createItem(context, source)
                                 .withTitle("Mapped item")
                                 .withIssueDate("2020")
                                 .build();

        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);

        // Source collection browse is empty
        getClient().perform(
            get("/api/discover/search/objects")
                .param("dsoType", "ITEM")
                .param("scope", sink.getID().toString()))
                   // should return with 200 OK
                   .andExpect(status().isOk())
                   // should contain no search results
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                   )));

        // Map the item to the sink collection
        getClient(adminToken).perform(
            post("/api/core/items/" + item.getID() + "/mappedCollections/")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/spring-rest/api/core/collections/" + sink.getID() + "\n"
                )
        );

        // Source collection browse contains mapped item
        getClient().perform(
            get("/api/discover/search/objects")
                .param("dsoType", "ITEM")
                .param("scope", sink.getID().toString()))
                   // should return with 200 OK
                   .andExpect(status().isOk())
                   // should contain no search results
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                   )))
                   // should match mapped item title
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                       SearchResultMatcher.matchOnItemName("item", "items", "Mapped item")
                   )));
    }

    @Test
    public void unmappedItemIsRemovedFromCollectionBrowseTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection source = CollectionBuilder.createCollection(context, parentCommunity).withName("Source").build();
        Collection sink = CollectionBuilder.createCollection(context, parentCommunity).withName("Sink").build();

        //2. An item in the source collection
        Item item = ItemBuilder.createItem(context, source)
                               .withTitle("Mapped item")
                               .withIssueDate("2020")
                               .build();

        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);

        //3. The item mapped to the sink collection
        getClient(adminToken).perform(
            post("/api/core/items/" + item.getID() + "/mappedCollections/")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/spring-rest/api/core/collections/" + sink.getID() + "\n"
                )
        );

        // Source collection browse contains mapped item
        getClient().perform(
            get("/api/discover/search/objects")
                .param("dsoType", "ITEM")
                .param("scope", sink.getID().toString()))
                   // should return with 200 OK
                   .andExpect(status().isOk())
                   // should contain no search results
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                   )))
                   // should match mapped item title
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                       SearchResultMatcher.matchOnItemName("item", "items", "Mapped item")
                   )));

        // Unmap item from sink collection
        getClient(adminToken).perform(
            delete("/api/core/items/" + item.getID() + "/mappedCollections/" + sink.getID()));

        // Source collection browse is empty
        getClient().perform(
            get("/api/discover/search/objects")
                .param("dsoType", "ITEM")
                .param("scope", sink.getID().toString()))
                   // should return with 200 OK
                   .andExpect(status().isOk())
                   // should contain no search results
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                   )));
    }
}
