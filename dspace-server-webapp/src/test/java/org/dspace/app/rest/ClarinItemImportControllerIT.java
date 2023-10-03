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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import org.dspace.content.Community;
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

    // Fix of this issue: https://github.com/dataquest-dev/DSpace/issues/409
    // Authors sequence was changed after a data import.
    @Test
    public void importItemWithUnsortedAuthors() throws Exception {
        String FIRST_AUTHOR_VALUE = "First author";
        int FIRST_AUTHOR_PLACE = 1;
        String SECOND_AUTHOR_VALUE = "Second author";
        int SECOND_AUTHOR_PLACE = 0;

        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("withdrawn", jsonNodeFactory.textNode("false"));
        node.set("inArchive", jsonNodeFactory.textNode("false"));
        node.set("discoverable", jsonNodeFactory.textNode("false"));

        // Metadata which should be kept after installing the new Item.
        ObjectNode metadataNode = jsonNodeFactory.objectNode();

        // `dc.contributor.author` metadata added into `metadata` of the ItemRest object
        ObjectNode firstAuthorMetadataNode = jsonNodeFactory.objectNode();
        firstAuthorMetadataNode.set("value", jsonNodeFactory.textNode(FIRST_AUTHOR_VALUE));
        firstAuthorMetadataNode.set("language", jsonNodeFactory.textNode("en_US"));
        firstAuthorMetadataNode.set("authority", jsonNodeFactory.nullNode());
        firstAuthorMetadataNode.set("confidence", jsonNodeFactory.numberNode(-1));
        firstAuthorMetadataNode.set("place", jsonNodeFactory.numberNode(FIRST_AUTHOR_PLACE));

        // `dc.contributor.author` metadata added into `metadata` of the ItemRest object
        ObjectNode secondAuthorMetadataNode = jsonNodeFactory.objectNode();
        secondAuthorMetadataNode.set("value", jsonNodeFactory.textNode(SECOND_AUTHOR_VALUE));
        secondAuthorMetadataNode.set("language", jsonNodeFactory.textNode("en_US"));
        secondAuthorMetadataNode.set("authority", jsonNodeFactory.nullNode());
        secondAuthorMetadataNode.set("confidence", jsonNodeFactory.numberNode(-1));
        secondAuthorMetadataNode.set("place", jsonNodeFactory.numberNode(SECOND_AUTHOR_PLACE));
        metadataNode.set("dc.contributor.author", jsonNodeFactory.arrayNode()
                .add(firstAuthorMetadataNode)
                .add(secondAuthorMetadataNode));

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
        assertFalse(item.isArchived());
        assertFalse(item.isDiscoverable());
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertEquals(item.getOwningCollection().getID(), col.getID());

        // get all `dc.contributor.author`metadata values
        List<MetadataValue> authorValues =
                itemService.getMetadata(item, "dc", "contributor", "author", "en_US");
        assertEquals(authorValues.size(), 2);

        int indexFirstAuthor = Objects.equals(authorValues.get(0).getValue(), FIRST_AUTHOR_VALUE) ? 0 : 1;
        int indexSecondAuthor = Objects.equals(indexFirstAuthor, 0) ? 1 : 0;
        // first metadata value should be FIRST_AUTHOR with place 1
        assertEquals(authorValues.get(indexFirstAuthor).getValue(), FIRST_AUTHOR_VALUE);
        assertEquals(authorValues.get(indexFirstAuthor).getPlace(), FIRST_AUTHOR_PLACE);
        // second metadata value should be SECOND_AUTHOR with place 0
        assertEquals(authorValues.get(indexSecondAuthor).getValue(), SECOND_AUTHOR_VALUE);
        assertEquals(authorValues.get(indexSecondAuthor).getPlace(), SECOND_AUTHOR_PLACE);

        // clean all
        context.turnOffAuthorisationSystem();
        ItemBuilder.deleteItem(uuid);
        context.restoreAuthSystemState();
    }

    @Test
    public void testImportAuthorityAndConfidenceInMetadata() throws Exception {
        String DC_RELATION_METADATA_FIELD = "dc.relation";
        String DC_RELATION_METADATA_VALUE = "this is metadata value";
        int CONFIDENCE = 300;
        int AUTHORITY = 20000;

        context.turnOffAuthorisationSystem();
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("withdrawn", jsonNodeFactory.textNode("false"));
        node.set("inArchive", jsonNodeFactory.textNode("false"));
        node.set("discoverable", jsonNodeFactory.textNode("false"));

        // Metadata which should be kept after installing the new Item.
        ObjectNode metadataNode = jsonNodeFactory.objectNode();

        // `dc.relation` metadata added into `metadata` of the ItemRest object
        ObjectNode dcRelationMetadataNode = jsonNodeFactory.objectNode();
        dcRelationMetadataNode.set("value", jsonNodeFactory.textNode(DC_RELATION_METADATA_VALUE));
        dcRelationMetadataNode.set("language", jsonNodeFactory.textNode("en_US"));
        dcRelationMetadataNode.set("authority", jsonNodeFactory.numberNode(AUTHORITY));
        dcRelationMetadataNode.set("confidence", jsonNodeFactory.numberNode(CONFIDENCE));
        metadataNode.set(DC_RELATION_METADATA_FIELD, jsonNodeFactory.arrayNode()
                .add(dcRelationMetadataNode));

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
        // controlling of the created item
        Item item = itemService.find(context, uuid);
        assertFalse(item.isWithdrawn());
        assertFalse(item.isArchived());
        assertFalse(item.isDiscoverable());
        assertEquals(item.getSubmitter().getID(), submitter.getID());
        assertEquals(item.getOwningCollection().getID(), col.getID());

        // get all `dc.contributor.author`metadata values
        List<MetadataValue> dcRelationValues =
                itemService.getMetadata(item, "dc", "relation", null, "en_US");
        assertEquals(dcRelationValues.size(), 1);

        MetadataValue dcRelationValue = dcRelationValues.get(0);
        assertEquals(dcRelationValue.getAuthority(), String.valueOf(AUTHORITY));
        assertEquals(dcRelationValue.getConfidence(), CONFIDENCE);
    }

    @Test
    public void importItemsMappedCollections() throws Exception {
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

        String startOfCollectionLink = "https://localhost:8080/spring-rest/api/core/collections/";
        // It is owning collection
        String col1SelfLink = startOfCollectionLink + col1.getID();
        String col2SelfLink = startOfCollectionLink + col2.getID();
        String col3SelfLink = startOfCollectionLink + col3.getID();
        List<String> collectionSelfLinksList = new ArrayList<>();
        collectionSelfLinksList.add(col1SelfLink);
        collectionSelfLinksList.add(col2SelfLink);
        collectionSelfLinksList.add(col3SelfLink);

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/clarin/import/item/" +
                                publicItem1.getID() + "/mappedCollections")
                                .content(mapper.writeValueAsBytes(collectionSelfLinksList))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());

        Item updatedItem = itemService.find(context, publicItem1.getID());
        assertEquals(updatedItem.getCollections().size(), 3);
        assertTrue(updatedItem.getCollections().contains(col1));
        assertTrue(updatedItem.getCollections().contains(col2));
        assertTrue(updatedItem.getCollections().contains(col3));
    }
}
