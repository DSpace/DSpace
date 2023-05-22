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
import org.dspace.content.MetadataValue;
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
        String PROVENANCE_VALUE = "first provenance metadata value";
        String DATE_VALUE = "2014-07-30T21:26:36Z";
        String IDENTIFIER_VALUE = "some handle url";

        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("withdrawn", jsonNodeFactory.textNode("false"));
        node.set("inArchive", jsonNodeFactory.textNode("true"));
        node.set("discoverable", jsonNodeFactory.textNode("false"));

        // Metadata which should be kept after installing the new Item.
        ObjectNode metadataNode = jsonNodeFactory.objectNode();

        // `dc.description.provenance` metadata added into `metadata` of the ItemRest object
        ObjectNode provenanceMetadataNode = jsonNodeFactory.objectNode();
        provenanceMetadataNode.set("value", jsonNodeFactory.textNode(PROVENANCE_VALUE));
        provenanceMetadataNode.set("language", jsonNodeFactory.textNode("en_US"));
        provenanceMetadataNode.set("authority", jsonNodeFactory.nullNode());
        provenanceMetadataNode.set("confidence", jsonNodeFactory.numberNode(-1));
        provenanceMetadataNode.set("place", jsonNodeFactory.numberNode(-1));
        metadataNode.set("dc.description.provenance", jsonNodeFactory.arrayNode().add(provenanceMetadataNode));

        // `dc.date.available` metadata added into `metadata` of the ItemRest object
        ObjectNode dateAvailableMetadataNode = jsonNodeFactory.objectNode();
        dateAvailableMetadataNode.set("value", jsonNodeFactory.textNode(DATE_VALUE));
        dateAvailableMetadataNode.set("language", jsonNodeFactory.nullNode());
        dateAvailableMetadataNode.set("authority", jsonNodeFactory.nullNode());
        dateAvailableMetadataNode.set("confidence", jsonNodeFactory.numberNode(-1));
        dateAvailableMetadataNode.set("place", jsonNodeFactory.numberNode(-1));
        metadataNode.set("dc.date.available", jsonNodeFactory.arrayNode().add(dateAvailableMetadataNode));

        // `dc.date.accesioned` metadata added into `metadata` of the ItemRest object
        ObjectNode dateAccesionedMetadataNode = jsonNodeFactory.objectNode();
        dateAccesionedMetadataNode.set("value", jsonNodeFactory.textNode(DATE_VALUE));
        dateAccesionedMetadataNode.set("language", jsonNodeFactory.nullNode());
        dateAccesionedMetadataNode.set("authority", jsonNodeFactory.nullNode());
        dateAccesionedMetadataNode.set("confidence", jsonNodeFactory.numberNode(-1));
        dateAccesionedMetadataNode.set("place", jsonNodeFactory.numberNode(-1));
        metadataNode.set("dc.date.accessioned", jsonNodeFactory.arrayNode().add(dateAccesionedMetadataNode));

        // `dc.identifier.uri` metadata added into `metadata` of the ItemRest object
        ObjectNode identifierMetadataNode = jsonNodeFactory.objectNode();
        identifierMetadataNode.set("value", jsonNodeFactory.textNode(IDENTIFIER_VALUE));
        identifierMetadataNode.set("language", jsonNodeFactory.nullNode());
        identifierMetadataNode.set("authority", jsonNodeFactory.nullNode());
        identifierMetadataNode.set("confidence", jsonNodeFactory.numberNode(-1));
        identifierMetadataNode.set("place", jsonNodeFactory.numberNode(1));
        metadataNode.set("dc.identifier.uri", jsonNodeFactory.arrayNode().add(identifierMetadataNode));

        node.set("metadata", metadataNode);
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

        // workspaceitem should nt exist
        List<WorkspaceItem> workflowItems = workspaceItemService.findAll(context);
        assertEquals(workflowItems.size(), 0);
        // contoling of the created item
        Item item = itemService.find(context, uuid);
        assertFalse(item.isWithdrawn());
        assertTrue(item.isArchived());
        assertFalse(item.isDiscoverable());
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertEquals(item.getOwningCollection().getID(), col.getID());

        // check `dc.description.provenance` - there should be just one value
        List<MetadataValue> provenanceValues =
                itemService.getMetadata(item, "dc", "description", "provenance", "en_US");
        assertEquals(provenanceValues.size(), 1);
        assertEquals(provenanceValues.get(0).getValue(), PROVENANCE_VALUE);

        // check `dc.date.available` - there should be just one value
        List<MetadataValue> dateAvailableValue =
                itemService.getMetadata(item, "dc", "date", "available", null);
        assertEquals(dateAvailableValue.size(), 1);
        assertEquals(dateAvailableValue.get(0).getValue(), DATE_VALUE);

        // check `dc.description.accessioned` - there should be just one value
        List<MetadataValue> dateAvailableAccesioned =
                itemService.getMetadata(item, "dc", "date", "accessioned", null);
        assertEquals(dateAvailableAccesioned.size(), 1);
        assertEquals(dateAvailableAccesioned.get(0).getValue(), DATE_VALUE);

        // check `dc.identifier.uri` - there should be just one value
        List<MetadataValue> identifierValue =
                itemService.getMetadata(item, "dc", "identifier", "uri", null);
        assertEquals(identifierValue.size(), 1);
        assertEquals(identifierValue.get(0).getValue(), IDENTIFIER_VALUE);

        //clean all
        context.turnOffAuthorisationSystem();
        ItemBuilder.deleteItem(uuid);
        context.restoreAuthSystemState();
    }
}