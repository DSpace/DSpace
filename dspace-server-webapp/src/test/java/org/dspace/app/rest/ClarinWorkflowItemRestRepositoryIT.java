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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;

/**
 * For testing the ClarinVersionedHandleIdentifierProvider
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinWorkflowItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private XmlWorkflowFactory xmlWorkflowFactory;

    @Autowired
    private CollectionRoleService collectionRoleService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private CreativeCommonsService creativeCommonsService;

    @Autowired
    private ItemService itemService;

    Item item;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .build();

        item = ItemBuilder.createItem(context, col1)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .withCCLicense("http://creativecommons.org/licenses/by-nc-sa/4.0/")
                .build();
        context.restoreAuthSystemState();

        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);
    }

    // The new item should have the handle not based on the history.
    @Test
    public void createNewVersionOfItemWithHandleNotBasedOnHistory() throws Exception {
                // hold the id of the item version
        AtomicReference<Integer> idVersionRef = new AtomicReference<Integer>();
        AtomicReference<Integer> idWorkspaceItemRef = new AtomicReference<Integer>();
        AtomicReference<String> idNewItemRef = new AtomicReference<String>();
        AtomicReference<String> handleNewItemRef = new AtomicReference<String>();
        try {
            String adminToken = getAuthToken(admin.getEmail(), password);

            // Create the item version history record - this object is created after clicking on the
            // `Create new version` button, but in testing it must be called manually before creating the item version.
            getClient(adminToken).perform(post("/api/versioning/versions")
                            .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                            .content("/api/core/items/" + item.getID()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.version", is(2)),
                            hasJsonPath("$.type", is("version")))))
                    .andDo(result -> idVersionRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // Get id of created workspaceitem
            getClient(adminToken).perform(get("/api/submission/workspaceitems"))
                    .andExpect(status().isOk())
                    .andDo(result -> idWorkspaceItemRef.set(read(result.getResponse().getContentAsString(),
                            "$._embedded.workspaceitems[0].id")));

            // 2. Check if was created the new workspace item - creating of the item version history creates
            // the new workspaceitem - it should have the id = 2
            getClient(adminToken).perform(get("/api/submission/workspaceitems/" + idWorkspaceItemRef.get()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(idWorkspaceItemRef.get())));


            // Create a new version of the item
            getClient(adminToken)
                    .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                            .content("/api/submission/workspaceitems/" + idWorkspaceItemRef.get())
                            .contentType(textUriContentType))
                    .andExpect(status().isCreated());

            // Get a new version of the item
            getClient(adminToken).perform(get("/api/versioning/versions/" + idVersionRef.get() + "/item"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", not(item.getID())))
                    .andDo(result -> handleNewItemRef.set(read(result.getResponse().getContentAsString(),
                            "$.handle")))
                    .andDo(result -> idNewItemRef.set(read(result.getResponse().getContentAsString(),
                            "$.id")));

            // The new Item has different handle than the old one
            Assert.assertFalse(StringUtils.equals(handleNewItemRef.get(), item.getID().toString()));

            // Check the handle doesn't contains '.2' - because DSpace creates the handle based on the history
            Assert.assertFalse(StringUtils.contains(handleNewItemRef.get(), ".2"));

        } finally {
            VersionBuilder.delete(idVersionRef.get());
            ItemBuilder.deleteItem(UUID.fromString(idNewItemRef.get()));
            WorkspaceItemBuilder.deleteWorkspaceItem(idWorkspaceItemRef.get());
        }
    }

    // The new item should have the handle not based on the history.
    @Test
    public void shouldAddNewHandleToItemMetadata() throws Exception {
        // 1. Create the item version history record
        // 2. Check if was created the new workspace item
        // 3. Create a new version of the item
        // 4. Get handle and id of the new version of the item
        // 5. Get the new version item from the API and check if it has the handle of the previous item in the metadata
        // `dc.relation.replaces`
        // 6. Get the first version item and check if it has the handle of the new version item in the
        // metadata`dc.relation.isreplacedby`

        AtomicReference<Integer> idVersionRef = new AtomicReference<Integer>();
        AtomicReference<Integer> idWorkspaceItemRef = new AtomicReference<Integer>();
        AtomicReference<String> idNewItemRef = new AtomicReference<String>();
        AtomicReference<String> handleNewItemRef = new AtomicReference<String>();
        AtomicReference<String> identifierUriPrevItemRef = new AtomicReference<String>();
        try {
            String adminToken = getAuthToken(admin.getEmail(), password);

            // // 1. Create the item version history record - this object is created after clicking on the
            // `Create new version` button, but in testing it must be called manually before creating the item version.
            getClient(adminToken).perform(post("/api/versioning/versions")
                            .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                            .content("/api/core/items/" + item.getID()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", Matchers.allOf(
                            hasJsonPath("$.version", is(2)),
                            hasJsonPath("$.type", is("version")))))
                    .andDo(result -> idVersionRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // Get id of created workspaceitem
            getClient(adminToken).perform(get("/api/submission/workspaceitems"))
                    .andExpect(status().isOk())
                    .andDo(result -> idWorkspaceItemRef.set(read(result.getResponse().getContentAsString(),
                            "$._embedded.workspaceitems[0].id")));

            // 2. Check if was created the new workspace item - creating of the item version history creates
            // the new workspaceitem - it should have the id = 2
            getClient(adminToken).perform(get("/api/submission/workspaceitems/" + idWorkspaceItemRef.get()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(idWorkspaceItemRef.get())));

            // 3. Create a new version of the item
            getClient(adminToken)
                    .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                            .content("/api/submission/workspaceitems/" + idWorkspaceItemRef.get())
                            .contentType(textUriContentType))
                    .andExpect(status().isCreated());

            // 4. Get handle and id of the new version of the item
            getClient(adminToken).perform(get("/api/versioning/versions/" + idVersionRef.get() + "/item"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", not(item.getID())))
                    .andDo(result -> identifierUriPrevItemRef.set(read(result.getResponse().getContentAsString(),
                            "$.metadata['dc.identifier.uri'][0].value")))
                    .andDo(result -> handleNewItemRef.set(read(result.getResponse().getContentAsString(),
                            "$.handle")))
                    .andDo(result -> idNewItemRef.set(read(result.getResponse().getContentAsString(),
                            "$.id")));

            // The new Item has different handle than the old one
            Assert.assertFalse(StringUtils.equals(handleNewItemRef.get(), item.getID().toString()));

            // Check the handle doesn't contains '.2' - because DSpace creates the handle based on the history
            Assert.assertFalse(StringUtils.contains(handleNewItemRef.get(), ".2"));

            // 5. Get the new version item from the API and check if it has the handle of the previous item in the
            // metadata `dc.relation.replaces`
            getClient(adminToken).perform(get("/api/core/items/" + idNewItemRef.get()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata", Matchers.allOf(
                            matchMetadata("dc.relation.replaces",
                                    itemService.getMetadataFirstValue(item, "dc", "identifier",
                                            "uri", Item.ANY)))));

            // 6. Get the first version item and check if it has the handle of the new version item in the
            // metadata`dc.relation.isreplacedby`
            getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata", Matchers.allOf(
                            matchMetadata("dc.relation.isreplacedby",
                                    identifierUriPrevItemRef.get()))));

        } finally {
            VersionBuilder.delete(idVersionRef.get());
            ItemBuilder.deleteItem(UUID.fromString(idNewItemRef.get()));
            WorkspaceItemBuilder.deleteWorkspaceItem(idWorkspaceItemRef.get());
        }
    }
}
