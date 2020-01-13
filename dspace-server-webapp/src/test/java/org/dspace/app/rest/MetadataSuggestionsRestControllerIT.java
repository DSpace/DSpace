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

import java.sql.SQLException;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.external.provider.metadata.service.MetadataSuggestionProviderService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataSuggestionsRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private MetadataSuggestionProviderService metadataSuggestionProviderService;
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private ItemService itemService;

    WorkspaceItem workspaceItem = null;

    @Before
    public void setup() throws SQLException {
        context.setCurrentUser(admin);

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


        //2. Three workspace items in two different collections
        workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                            .withTitle("Workspace Item 1")
                                            .withIssueDate("2017-10-17")
                                            .build();
    }

    @Test
    public void getMetadataSuggestionEntryTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestions/mock/entryValues/one")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(jsonPath("$.id", Matchers.is("one")))
                        .andExpect(jsonPath("$.display", Matchers.is("one")))
                        .andExpect(jsonPath("$.value", Matchers.is("one")))
                        .andExpect(
                            jsonPath("metadata['dc.contributor.author'][0].value", Matchers.is("Donald, Smith")));
    }

    @Test
    public void getMetadataSuggestionEntryTestWithoutWorkflowWorkspaceItemBadRequest() throws Exception {
        getClient().perform(get("/api/integration/metadatasuggestions/mock/entryValues/one"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void getMetadataSuggestionEntryTestWrongInProgressSubmissionIdResourceNotFound() throws Exception {
        getClient().perform(get("/api/integration/metadatasuggestions/mock/entryValues/one")
                                .param("workspaceitem", "123123123"))
                   .andExpect(status().is(404));
    }

    @Test
    public void getMetadataSuggestionEntryTestWrongSuggestionNameResourceNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/integration/metadatasuggestions/mockInvalid/entryValues/one")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().is(404));
    }

    @Test
    public void getMetadataSuggestionEntryTestUnAuthorizedInProgressSubmissionAccessDenied() throws Exception {
        getClient().perform(get("/api/integration/metadatasuggestions/mock/entryValues/one")
                                .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getMetadataSuggestionEntryTestForbiddenInProgressSubmissionAccessDenied() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestions/mock/entryValues/one")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getMetadataSuggestionDifferencesTestWithoutWorkflowWorkspaceItemBadRequest() throws Exception {
        getClient().perform(get("/api/integration/metadatasuggestions/mock/entryValuesDifferences/one"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void getMetadataSuggestionDifferencesTestWrongInProgressSubmissionIdResourceNotFound() throws Exception {
        getClient().perform(get("/api/integration/metadatasuggestions/mock/entryValuesDifferences/one")
                                .param("workspaceitem", "123123123"))
                   .andExpect(status().is(404));
    }

    @Test
    public void getMetadataSuggestionDifferencesTestWrongSuggestionNameResourceNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/integration/metadatasuggestions/mockInvalid/entryValuesDifferences/one")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().is(404));
    }


    @Test
    public void getMetadataSuggestionDifferencesTestUnAuthorizedInProgressSubmissionAccessDenied() throws Exception {
        getClient().perform(get("/api/integration/metadatasuggestions/mock/entryValuesDifferences/one")
                                .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getMetadataSuggestionDifferencesTestForbiddenInProgressSubmissionAccessDenied() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestions/mock/entryValuesDifferences/one")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getMetadataSuggestionEntryDifferencesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = workspaceItem.getItem();
        itemService.addMetadata(context, item, "dc", "title", null, null, "Test, test");
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/integration/metadatasuggestions/mock/entryValuesDifferences/one")
                                     .param("workspaceitem", String.valueOf(workspaceItem.getID())))
                        .andExpect(jsonPath("$.differences['dc.contributor.author'].suggestions[0].operations[0]",
                                            Matchers.containsString("add/metadata/dc.contributor.author")))
                        .andExpect(jsonPath("$.differences['dc.contributor.author'].suggestions[0].newvalue",
                                            Matchers.is("Donald, Smith")));

    }

}
