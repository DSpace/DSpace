/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;


import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataLanguageDoesNotExist;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceRestRepositoryLanguageIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    private Collection collection;

    @Before
    public void setup() {
        configurationService.setProperty("webui.supported.locales", List.of("en,de"));

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withEntityType("Publication")
                                      .withName("Publications")
                                      .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testFindItem() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Item")
                               .withTitleForLanguage("english title", "en")
                               .withTitleForLanguage("german title", "de")
                               .withAuthorForLanguage("english author1", "en")
                               .withAuthorForLanguage("english author2", "en")
                               .withAuthorForLanguage("german author1", "de")
                               .withAuthorForLanguage("german author2", "de")
                               .withSubjectForLanguage("german subject", "de")
                               .build();

        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item2")
                                .withTitleForLanguage("italy title2", "it")
                                .withSubjectForLanguage("spain subject2", "es")
                                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

//        When no projection is requested
        getClient(token).perform(get("/api/core/items/" + item.getID())
                            .header("Accept-Language", Locale.ENGLISH.getLanguage()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 0, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "english author1", 0, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "english author2", 1, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadataLanguageDoesNotExist("dc.contributor.author", "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadataLanguageDoesNotExist("dc.contributor.author", "de")));

//        When no projection is requested
        getClient(token).perform(get("/api/core/items/" + item.getID())
                            .header("Accept-Language", "en_US"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 0, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "english author1", 0, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "english author2", 1, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadataLanguageDoesNotExist("dc.contributor.author", "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadataLanguageDoesNotExist("dc.contributor.author", "de")));

//        When no projection is requested
        getClient(token).perform(get("/api/core/items/" + item.getID())
                            .header("Accept-Language", Locale.GERMAN.getLanguage()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "german title", 0, "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "german author1", 0, "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "german author2", 1, "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadataLanguageDoesNotExist("dc.contributor.author", "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadataLanguageDoesNotExist("dc.contributor.author", "en")));

//         When allLanguages projection is requested.
        getClient(token).perform(get("/api/core/items/" + item.getID())
                            .param("projection", "allLanguages"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Item", 0, null)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 1, "en")))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "german title", 2, "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "english author1", 0, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "english author2", 1, "en")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "german author1", 2, "de")))
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.contributor.author", "german author2", 3, "de")))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.subject", "german subject", 0, "de")));

//        When no projection is requested
        getClient(token).perform(get("/api/core/items/" + item2.getID())
                            .header("Accept-Language", "es"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item2)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Item2", 0, null)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "italy title2", 1, "it")))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.subject", "spain subject2", 0, "es")));

    }

    @Test
    public void testFindCollection() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("collection")
                                           .withNameForLanguage("english title", "en")
                                           .withNameForLanguage("german title", "de")
                                           .build();

        context.restoreAuthSystemState();

//        When no projection is requested
        getClient().perform(get("/api/core/collections/" + collection.getID())
                       .header("Accept-Language", Locale.ENGLISH.getLanguage()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 0, "en")))
                   .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "de")))
                   .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", null)));

//        When allLanguages projection is requested
        getClient().perform(get("/api/core/collections/" + collection.getID())
                       .param("projection", "allLanguages"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "collection", 0, null)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 1,  "en")))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "german title", 2,  "de")));
    }

    @Test
    public void testFindCommunity() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                               .withName("community")
                                               .withNameForLanguage("english title", "en")
                                               .withNameForLanguage("german title", "de")
                                               .build();

        context.restoreAuthSystemState();

//        When no projection is requested
        getClient().perform(get("/api/core/communities/" + community.getID())
                       .header("Accept-Language", Locale.ENGLISH.getLanguage()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 0, "en")))
                   .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "de")))
                   .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", null)));

//        When allLanguages projection is requested
        getClient().perform(get("/api/core/communities/" + community.getID())
                       .param("projection", "allLanguages"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "community", 0, null)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 1,  "en")))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "german title", 2,  "de")));
    }

    @Test
    public void testFindWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("workspaceItem title")
                                .withTitleForLanguage("english title", "en")
                                .withTitleForLanguage("german title", "de")
                                .withSubjectForLanguage("german subject", "de")
                                .withSubjectForLanguage("spain subject", "es")
                                .withIssueDate("2024-09-26")
                                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

//         When no projection is requested
        getClient(token).perform(
                            get("/api/submission/workspaceitems/" + workspaceItem.getID())
                                .param("embed", "item")
                                .header("Accept-Language", Locale.ENGLISH.getLanguage())
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.item",
                                            ItemMatcher.matchItemProperties(workspaceItem.getItem())))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "english title", 0, "en")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dspace.entity.type", "Publication", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadataLanguageDoesNotExist("dc.title", null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadataLanguageDoesNotExist("dc.title", "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadataLanguageDoesNotExist("dc.subject", "es")));

//        When allLanguages projection is requested
        getClient(token).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID())
                                     .param("embed", "item")
                                     .param("projection", "allLanguages"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.item",
                                            ItemMatcher.matchItemProperties(workspaceItem.getItem())))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "workspaceItem title", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "english title", 1, "en")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "german title", 2, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dspace.entity.type", "Publication", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.subject", "spain subject", 1, "es")));

    }

    @Test
    public void testFindWorkflowItem() throws Exception {

        context.turnOffAuthorisationSystem();

        Community child = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                          .withName("Sub Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, child)
                                                 .withName("Collection 1")
                                                 .withEntityType("Publication")
                                                 .withWorkflowGroup(1, admin).build();

        XmlWorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, collection)
                                                          .withTitle("workflowItem title")
                                                          .withTitleForLanguage("english title", "en")
                                                          .withTitleForLanguage("german title", "de")
                                                          .withSubjectForLanguage("german subject", "de")
                                                          .withSubjectForLanguage("spain subject", "es")
                                                          .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

//        When no projection is requested
        getClient(token).perform(get("/api/workflow/workflowitems/" + workflowItem.getID())
                                     .param("embed", "item")
                                     .header("Accept-Language", Locale.ENGLISH.getLanguage()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.item",
                                            ItemMatcher.matchItemProperties(workflowItem.getItem())))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "english title", 0, "en")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dspace.entity.type", "Publication", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadataLanguageDoesNotExist("dc.title", null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadataLanguageDoesNotExist("dc.title", "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadataLanguageDoesNotExist("dc.subject", "es")));

//         When allLanguages projection is requested
        getClient(token).perform(get("/api/workflow/workflowitems/" + workflowItem.getID())
                                     .param("embed", "item")
                                     .param("projection", "allLanguages"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.item",
                                            ItemMatcher.matchItemProperties(workflowItem.getItem())))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "workflowItem title", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "english title", 1, "en")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.title", "german title", 2, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dspace.entity.type", "Publication", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                                            matchMetadata("dc.subject", "spain subject", 1, "es")));

    }

    @Test
    public void testFindItemWithMultipleProjections() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Item")
                               .withTitleForLanguage("english title", "en")
                               .withTitleForLanguage("german title", "de")
                               .withSubjectForLanguage("german subject", "de")
                               .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

//         When allLanguages and full projections are requested
        getClient(token).perform(get("/api/core/items/" + item.getID())
                            .param("projection", "allLanguages", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(item)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Item", 0, null)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 1, "en")))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "german title", 2, "de")))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.subject", "german subject", 0, "de")));

    }

    @Test
    public void testFindCollectionWithMultipleProjections() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("collection")
                                                 .withNameForLanguage("english title", "en")
                                                 .withNameForLanguage("german title", "de")
                                                 .build();

        context.restoreAuthSystemState();

//        When allLanguages and full projections are requested
        getClient().perform(get("/api/core/collections/" + collection.getID())
                       .param("projection", "allLanguages", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "collection", 0, null)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "english title", 1,  "en")))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "german title", 2,  "de")));
    }

    @Test
    public void testFindWorkspaceItemWithMultipleProjections() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("workspaceItem title")
                                                          .withTitleForLanguage("english title", "en")
                                                          .withTitleForLanguage("german title", "de")
                                                          .withSubjectForLanguage("german subject", "de")
                                                          .withSubjectForLanguage("spain subject", "es")
                                                          .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

//        When allLanguages and full projections are requested
        getClient(token).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID())
                            .param("projection", "allLanguages", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.item",
                            ItemMatcher.matchItemProperties(workspaceItem.getItem())))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                            matchMetadata("dc.title", "workspaceItem title", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                            matchMetadata("dc.title", "english title", 1, "en")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                            matchMetadata("dc.title", "german title", 2, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                            matchMetadata("dspace.entity.type", "Publication", 0, null)))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                            matchMetadata("dc.subject", "german subject", 0, "de")))
                        .andExpect(jsonPath("$._embedded.item.metadata",
                            matchMetadata("dc.subject", "spain subject", 1, "es")));

    }

}
