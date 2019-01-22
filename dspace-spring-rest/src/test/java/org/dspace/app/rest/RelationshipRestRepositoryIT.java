/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.RelationshipBuilder;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class RelationshipRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Before
    public void setup() throws Exception {

        //Set up the database for the next test
        String pathToFile = configurationService.getProperty("dspace.dir") +
            File.separator + "config" + File.separator + "entities" + File.separator + "relationship-types.xml";
        runDSpaceScript("initialize-entities", "-f", pathToFile);


    }

    @After
    public void destroy() throws Exception {
        //Clean up the database for the next test
        context.turnOffAuthorisationSystem();
        List<RelationshipType> relationshipTypeList = relationshipTypeService.findAll(context);
        List<EntityType> entityTypeList = entityTypeService.findAll(context);
        List<Relationship> relationships = relationshipService.findAll(context);

        Iterator<Relationship> relationshipIterator = relationships.iterator();
        while (relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();
            relationshipIterator.remove();
            relationshipService.delete(context, relationship);
        }

        Iterator<RelationshipType> relationshipTypeIterator = relationshipTypeList.iterator();
        while (relationshipTypeIterator.hasNext()) {
            RelationshipType relationshipType = relationshipTypeIterator.next();
            relationshipTypeIterator.remove();
            relationshipTypeService.delete(context, relationshipType);
        }

        Iterator<EntityType> entityTypeIterator = entityTypeList.iterator();
        while (entityTypeIterator.hasNext()) {
            EntityType entityType = entityTypeIterator.next();
            entityTypeIterator.remove();
            entityTypeService.delete(context, entityType);
        }

        super.destroy();
    }

    @Test
    public void findAllRelationshipTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item auhor1 = ItemBuilder.createItem(context, col1)
                                 .withTitle("Author1")
                                 .withIssueDate("2017-10-17")
                                 .withAuthor("Smith, Donald")
                                 .withRelationshipType("Person")
                                 .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Smith, Maria")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Maybe, Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item orgUnit1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("OrgUnit1")
                                   .withAuthor("Testy, TEst")
                                   .withIssueDate("2015-01-01")
                                   .withRelationshipType("OrgUnit")
                                   .build();

        Item project1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("Project1")
                                   .withAuthor("Testy, TEst")
                                   .withIssueDate("2015-01-01")
                                   .withRelationshipType("Project")
                                   .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();


        RelationshipType isOrgUnitOfPersonRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Person"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfPerson", "isPersonOfOrgUnit");
        RelationshipType isOrgUnitOfProjectRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Project"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfProject", "isProjectOfOrgUnit");
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, auhor1, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        Relationship relationship2 = RelationshipBuilder
            .createRelationshipBuilder(context, project1, orgUnit1, isOrgUnitOfProjectRelationshipType).build();

        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication, auhor1, isAuthorOfPublicationRelationshipType).build();

        getClient().perform(get("/api/core/relationships"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationship2),
                       RelationshipMatcher.matchRelationship(relationship3)
                   )))
        ;
    }

    @Test
    public void addRelationshipsAndMetadataToValidatePlaceTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withPersonIdentifierFirstName("Donald")
                                  .withPersonIdentifierLastName("Smith")
                                  .withRelationshipType("Person")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maybe")
                                  .withPersonIdentifierLastName("Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        String adminToken = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("leftItem", publication.getID().toString())
                                                                .param("rightItem", author1.getID().toString())
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication);

        List<MetadataValue> list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", publication.getID().toString())
                                                      .param("rightItem", author2.getID().toString())
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text two");
//        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);

//        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text four");
//        itemService.update(context, publication);
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                assertEquals(3, mdv.getPlace());
            }
        }


        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", publication.getID().toString())
                                                      .param("rightItem", author3.getID().toString())
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(5, mdv.getPlace());
            }
        }
    }

    @Test
    public void deleteMetadataValueAndValidatePlace() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withPersonIdentifierFirstName("Donald")
                                  .withPersonIdentifierLastName("Smith")
                                  .withRelationshipType("Person")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maybe")
                                  .withPersonIdentifierLastName("Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        String adminToken = getAuthToken(admin.getEmail(), password);


        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("leftItem", publication.getID().toString())
                                                                .param("rightItem", author1.getID().toString())
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication);

        List<MetadataValue> list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", publication.getID().toString())
                                                      .param("rightItem", author2.getID().toString())
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text two");
//        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);

//        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text four");
//        itemService.update(context, publication);
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                assertEquals(3, mdv.getPlace());
            }
        }


        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", publication.getID().toString())
                                                      .param("rightItem", author3.getID().toString())
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(5, mdv.getPlace());
            }
        }

        List<MetadataValue> authors = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> listToRemove = new LinkedList<>();
        for (MetadataValue metadataValue : authors) {
            if (StringUtils.equals(metadataValue.getValue(), "plain text two")) {
                listToRemove.add(metadataValue);
            }
        }
        itemService.removeMetadataValues(context, publication, listToRemove);

        itemService.update(context, publication);
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(4, mdv.getPlace());
            }
        }

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));
        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));
        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(3)));

    }

    @Test
    public void deleteRelationshipsAndValidatePlace() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withPersonIdentifierFirstName("Donald")
                                  .withPersonIdentifierLastName("Smith")
                                  .withRelationshipType("Person")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maybe")
                                  .withPersonIdentifierLastName("Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        String adminToken = getAuthToken(admin.getEmail(), password);


        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("leftItem", publication.getID().toString())
                                                                .param("rightItem", author1.getID().toString())
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication);

        List<MetadataValue> list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", publication.getID().toString())
                                                      .param("rightItem", author2.getID().toString())
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication);

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                assertEquals(3, mdv.getPlace());
            }
        }


        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", publication.getID().toString())
                                                      .param("rightItem", author3.getID().toString())
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(5, mdv.getPlace());
            }
        }


        getClient(adminToken).perform(delete("/api/core/relationships/" + secondRelationshipIdString));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        publication = itemService.find(context, publication.getID());
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                assertEquals(2, mdv.getPlace());
            }
        }


        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(4, mdv.getPlace());
            }
        }


        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(3)));

    }

    @Test
    public void addRelationshipsNotUseForPlace() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withRelationshipType("Person")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Smith, Maria")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Maybe, Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item orgUnit1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("OrgUnit1")
                                   .withIssueDate("2015-01-01")
                                   .withRelationshipType("OrgUnit")
                                   .build();

        RelationshipType isOrgUnitOfPersonRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Person"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfPerson", "isPersonOfOrgUnit");

        String adminToken = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("leftItem", author1.getID().toString())
                                                                .param("rightItem", orgUnit1.getID().toString())
                                                                .param("relationshipType",
                                                                       isOrgUnitOfPersonRelationshipType.getID()
                                                                                                        .toString())
                                                                .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", author2.getID().toString())
                                                      .param("rightItem", orgUnit1.getID().toString())
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();
        mapper = new ObjectMapper();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", author3.getID().toString())
                                                      .param("rightItem", orgUnit1.getID().toString())
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();
        mapper = new ObjectMapper();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(2)));
    }

    @Test
    public void addAndDeleteRelationshipsNotUseForPlace() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withRelationshipType("Person")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Smith, Maria")
                                  .withRelationshipType("Person")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Maybe, Maybe")
                                  .withRelationshipType("Person")
                                  .build();

        Item orgUnit1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("OrgUnit1")
                                   .withIssueDate("2015-01-01")
                                   .withRelationshipType("OrgUnit")
                                   .build();

        RelationshipType isOrgUnitOfPersonRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Person"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfPerson", "isPersonOfOrgUnit");

        String adminToken = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("leftItem", author1.getID().toString())
                                                                .param("rightItem", orgUnit1.getID().toString())
                                                                .param("relationshipType",
                                                                       isOrgUnitOfPersonRelationshipType.getID()
                                                                                                        .toString())
                                                                .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", author2.getID().toString())
                                                      .param("rightItem", orgUnit1.getID().toString())
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();
        mapper = new ObjectMapper();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));

        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("leftItem", author3.getID().toString())
                                                      .param("rightItem", orgUnit1.getID().toString())
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.APPLICATION_JSON).content(
                "{\"id\":530,\"leftId\":\"77877343-3f75-4c33-9492" +
                    "-6ed7c98ed84e\",\"relationshipTypeId\":0,\"rightId\":\"423d0eda-b808-4b87-97ae-b85fe9d59418\"," +
                    "\"leftPlace\":1,\"rightPlace\":1}"))
                                         .andExpect(status().isCreated())
                                         .andReturn();
        mapper = new ObjectMapper();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(2)));

        getClient(adminToken).perform(delete("/api/core/relationships/" + secondRelationshipIdString));

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));

    }
}
