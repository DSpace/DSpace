/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link org.dspace.app.rest.submit.step.CustomUrlStep}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlStepIT extends AbstractControllerIntegrationTest {

    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection =
            CollectionBuilder.createCollection(context, parentCommunity, "123456789/traditional-with-custom-url")
                                      .withName("Collection 1")
                                      .withEntityType("Publication")
                                      .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testCustomUrlReplacementOnWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .build();

        context.restoreAuthSystemState();

        Operation replaceOperation = new ReplaceOperation("/sections/custom-url/url", "my-custom-url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

        replaceOperation = new ReplaceOperation("/sections/custom-url/url", "my-custom-url-updated");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url-updated")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotHaveJsonPath());

        replaceOperation = new ReplaceOperation("/sections/custom-url/url", "new.url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("new.url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotHaveJsonPath());

    }

    @Test
    public void testRedirectedUrlDeletionOnWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withCustomUrl("my-custom-url")
                                                          .withOldCustomUrl("old-url-1")
                                                          .withOldCustomUrl("old-url-2")
                                                          .withOldCustomUrl("old-url-3")
                                                          .build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls",
                                contains("old-url-1", "old-url-2", "old-url-3")));

        Operation removeOperation = new RemoveOperation("/sections/custom-url/redirected-urls/1");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(removeOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls", contains("old-url-1", "old-url-3")));

        removeOperation = new RemoveOperation("/sections/custom-url/redirected-urls/0");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(removeOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls", contains("old-url-3")));

        removeOperation = new RemoveOperation("/sections/custom-url/redirected-urls/0");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(removeOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());
    }

    @Test
    public void testEmptyCustomUrlReplacementOnWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withCustomUrl("my-url")
                                                          .build();

        context.restoreAuthSystemState();

        Operation replaceOperation = new ReplaceOperation("/sections/custom-url/url", "");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", nullValue()))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

    }

    @Test
    public void testInvalidCustomUrlReplacementOnWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .build();

        context.restoreAuthSystemState();

        Operation replaceOperation = new ReplaceOperation("/sections/custom-url/url", "invalid?url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.custom-url.invalid-characters')]",
                                contains(hasJsonPath("$.paths",
                                                     contains(hasJsonPath("$", is("/sections/custom-url/url")))))))
            .andExpect(jsonPath("$.sections.custom-url.url", is("invalid?url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

    }

    @Test
    public void testInvalidCustomUrlReplacementWithForwardSlashesOnWorkspaceItem() throws Exception {
        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .build();

        context.restoreAuthSystemState();

        Operation replaceOperation = new ReplaceOperation("/sections/custom-url/url", "invalid/url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.custom-url.invalid-characters')]",
                                contains(hasJsonPath("$.paths",
                                                     contains(hasJsonPath("$", is("/sections/custom-url/url")))))))
            .andExpect(jsonPath("$.sections.custom-url.url", is("invalid/url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());
    }

    @Test
    public void testAlreadyExistingCustomUrlReplacementOnWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
                   .withCustomUrl("my-custom-url")
                   .withOldCustomUrl("old-url")
                   .build();

        ItemBuilder.createItem(context, collection)
                   .withCustomUrl("my-custom-url-2")
                   .withOldCustomUrl("old-url-2")
                   .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .build();

        context.restoreAuthSystemState();

        Operation replaceOperation = new ReplaceOperation("/sections/custom-url/url", "my-custom-url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.custom-url.conflict')]",
                                contains(hasJsonPath("$.paths",
                                                     contains(hasJsonPath("$", is("/sections/custom-url/url")))))))
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

        replaceOperation = new ReplaceOperation("/sections/custom-url/url", "old-url-2");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.custom-url.conflict')]",
                                contains(hasJsonPath("$.paths",
                                                     contains(hasJsonPath("$", is("/sections/custom-url/url")))))))
            .andExpect(jsonPath("$.sections.custom-url.url", is("old-url-2")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

        replaceOperation = new ReplaceOperation("/sections/custom-url/url", "new-url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("new-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

    }

    @Test
    public void testRedirectedUrlAdditionOnWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withCustomUrl("my-custom-url")
                                                          .build();

        context.restoreAuthSystemState();

        Operation addOperation = new AddOperation("/sections/custom-url/redirected-urls", "url-1");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(addOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls", contains("url-1")));

        addOperation = new AddOperation("/sections/custom-url/redirected-urls", "url-2");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(addOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls", contains("url-1", "url-2")));

        Operation replaceOperation = new ReplaceOperation("/sections/custom-url/url", "new-my-custom-url");
        addOperation = new AddOperation("/sections/custom-url/redirected-urls", "url-3");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(replaceOperation, addOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", is("new-my-custom-url")))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls", contains("url-1", "url-2", "url-3")));

    }

    @Test
    public void testSectionRemove() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withCustomUrl("my-custom-url")
                                                          .withOldCustomUrl("old-url-1")
                                                          .withOldCustomUrl("old-url-2")
                                                          .build();

        context.restoreAuthSystemState();

        Operation removeOperation = new RemoveOperation("/sections/custom-url");

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(removeOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.custom-url.url", nullValue()))
            .andExpect(jsonPath("$.sections.custom-url.redirected-urls").doesNotExist());

    }

}
