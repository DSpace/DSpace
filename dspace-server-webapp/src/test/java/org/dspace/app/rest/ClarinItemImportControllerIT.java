/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to test the /api/clarin/import/* endpoints
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinItemImportControllerIT extends AbstractControllerIntegrationTest {
    private JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(true);
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private WorkflowItemService workflowItemService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ConfigurationService configurationService;

    private Collection col;
    private EPerson submitter;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();
        submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void importWorkspaceItemAndItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("discoverable", jsonNodeFactory.textNode("false"));
        node.set("inArchive", jsonNodeFactory.textNode("false"));
        node.set("lastModified", jsonNodeFactory.textNode(null));
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);

        int id = read(getClient(token).perform(post("/api/clarin/import/workspaceitem")
                .content(mapper.writeValueAsBytes(node))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .param("owningCollection", col.getID().toString())
                .param("multipleTitles", "true")
                .param("publishedBefore", "false")
                .param("multipleFiles", "false")
                .param("stageReached", "1")
                .param("pageReached", "123")
                .param("epersonUUID", submitter.getID().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
                "$.id");

        WorkspaceItem workspaceItem = workspaceItemService.find(context, id);
        assertNotNull(workspaceItem);
        assertTrue(workspaceItem.hasMultipleTitles());
        assertFalse(workspaceItem.isPublishedBefore());
        assertFalse(workspaceItem.hasMultipleFiles());
        assertEquals(workspaceItem.getStageReached(), 1);
        assertEquals(workspaceItem.getPageReached(), 123);
        assertEquals(workspaceItem.getCollection().getID(), col.getID());

        UUID uuid = UUID.fromString(read(getClient(token).perform(get("/api/clarin/import/" + id + "/item"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
                        "$.id"));
        Item item = itemService.find(context, uuid);
        assertNotNull(item);
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertFalse(item.isDiscoverable());
        assertFalse(item.isArchived());
        assertNull(item.getOwningCollection());

        //clean all
        context.turnOffAuthorisationSystem();
        WorkspaceItemBuilder.deleteWorkspaceItem(workspaceItem.getID());
        context.restoreAuthSystemState();
    }

    @Test
    public void importWorkflowItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("discoverable", jsonNodeFactory.textNode("false"));
        node.set("inArchive", jsonNodeFactory.textNode("false"));
        node.set("lastModified", jsonNodeFactory.textNode(null));
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);

        int id = read(getClient(token).perform(post("/api/clarin/import/workspaceitem")
                        .content(mapper.writeValueAsBytes(node))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .param("owningCollection", col.getID().toString())
                        .param("multipleTitles", "true")
                        .param("publishedBefore", "false")
                        .param("multipleFiles", "false")
                        .param("stageReached", "1")
                        .param("pageReached", "123")
                        .param("epersonUUID", submitter.getID().toString()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                    "$.id");

        getClient(token).perform(post("/api/clarin/import/workflowitem")
                .contentType(MediaType.APPLICATION_JSON)
                .param("id", Integer.toString(id)))
                .andExpect(status().isOk());

        List<WorkflowItem> workflowItems = workflowItemService.findAll(context);
        assertEquals(workflowItems.size(), 1);
        WorkflowItem workflowItem = workflowItems.get(0);
        assertEquals(workflowItem.getCollection().getID(), col.getID());
        assertFalse(workflowItem.isPublishedBefore());
        assertFalse(workflowItem.hasMultipleFiles());
        assertTrue(workflowItem.hasMultipleTitles());
        Item item = itemService.find(context, workflowItem.getItem().getID());
        assertNotNull(item);
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertFalse(item.isDiscoverable());
        assertFalse(item.isArchived());
        assertNull(item.getOwningCollection());

        //clean all
        context.turnOffAuthorisationSystem();
        WorkflowItemBuilder.deleteWorkflowItem(workflowItem.getID());
        context.restoreAuthSystemState();
    }

    @Test
    public void importWithdrawnItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("withdrawn", jsonNodeFactory.textNode("true"));
        node.set("inArchive", jsonNodeFactory.textNode("false"));
        node.set("discoverable", jsonNodeFactory.textNode("true"));
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);

        UUID uuid = UUID.fromString(read(getClient(token).perform(post("/api/clarin/import/item")
                                .content(mapper.writeValueAsBytes(node))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .param("owningCollection", col.getID().toString())
                                .param("epersonUUID", submitter.getID().toString()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.id"));

        //workspaceitem should nt exist
        List<WorkspaceItem> workflowItems = workspaceItemService.findAll(context);
        assertEquals(workflowItems.size(), 0);
        //contoling of the created item
        Item item = itemService.find(context, uuid);
        assertTrue(item.isWithdrawn());
        assertFalse(item.isArchived());
        assertTrue(item.isDiscoverable());
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertEquals(item.getOwningCollection().getID(), col.getID());

        //clean all
        context.turnOffAuthorisationSystem();
        ItemBuilder.deleteItem(uuid);
        context.restoreAuthSystemState();
    }

    @Test
    public void importArchivedItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("withdrawn", jsonNodeFactory.textNode("false"));
        node.set("inArchive", jsonNodeFactory.textNode("true"));
        node.set("discoverable", jsonNodeFactory.textNode("false"));
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);

        UUID uuid = UUID.fromString(read(getClient(token).perform(post("/api/clarin/import/item")
                                .content(mapper.writeValueAsBytes(node))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .param("owningCollection", col.getID().toString())
                                .param("epersonUUID", submitter.getID().toString()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.id"));

        //workspaceitem should nt exist
        List<WorkspaceItem> workflowItems = workspaceItemService.findAll(context);
        assertEquals(workflowItems.size(), 0);
        //contoling of the created item
        Item item = itemService.find(context, uuid);
        assertFalse(item.isWithdrawn());
        assertTrue(item.isArchived());
        assertFalse(item.isDiscoverable());
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertEquals(item.getOwningCollection().getID(), col.getID());

        //clean all
        context.turnOffAuthorisationSystem();
        ItemBuilder.deleteItem(uuid);
        context.restoreAuthSystemState();
    }
}