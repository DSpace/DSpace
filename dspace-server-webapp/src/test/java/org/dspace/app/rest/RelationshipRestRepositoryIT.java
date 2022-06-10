/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.MockSolrSearchCore;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class RelationshipRestRepositoryIT extends AbstractEntityIntegrationTest {

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    protected EntityTypeService entityTypeService;

    @Autowired
    protected AuthorizeService authorizeService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected MetadataFieldService metadataFieldService;

    @Autowired
    protected MetadataSchemaService metadataSchemaService;

    @Autowired
    MockSolrSearchCore mockSolrSearchCore;
    protected Community parentCommunity;
    protected Community child1;

    protected Collection col1;
    protected Collection col2;
    protected Collection col3;
    protected Collection col4;
    protected Collection col5;
    protected Collection col6;
    protected Collection col7;

    protected Item author1;
    protected Item author2;
    protected Item author3;

    protected Item orgUnit1;
    protected Item orgUnit2;
    protected Item orgUnit3;
    protected Item project1;

    protected Item publication1;
    protected Item publication2;

    protected RelationshipType isAuthorOfPublicationRelationshipType;
    protected RelationshipType isOrgUnitOfPersonRelationshipType;
    protected EPerson user1;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                 .withName("Sub Community")
                                 .build();

        col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                .withEntityType("Person").build();
        col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                                .withEntityType("Publication").build();
        col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits")
                                .withEntityType("OrgUnit").build();
        col4 = CollectionBuilder.createCollection(context, child1).withName("Projects")
                                .withEntityType("Project").build();
        col5 = CollectionBuilder.createCollection(context, child1).withName("Projects")
                                .withEntityType("Journal").build();
        col6 = CollectionBuilder.createCollection(context, child1).withName("Projects")
                                .withEntityType("JournalVolume").build();
        col7 = CollectionBuilder.createCollection(context, child1).withName("Projects")
                                .withEntityType("JournalIssue").build();

        author1 = ItemBuilder.createItem(context, col1)
                             .withTitle("Author1")
                             .withIssueDate("2017-10-17")
                             .withAuthor("Smith, Donald")
                             .withPersonIdentifierLastName("Smith")
                             .withPersonIdentifierFirstName("Donald")
                             .build();

        author2 = ItemBuilder.createItem(context, col1)
                             .withTitle("Author2")
                             .withIssueDate("2016-02-13")
                             .withAuthor("Smith, Maria")
                             .withPersonIdentifierLastName("Smith")
                             .withPersonIdentifierFirstName("Maria")
                             .withMetadata("dspace", "entity", "type", "Person")
                             .build();

        author3 = ItemBuilder.createItem(context, col1)
                             .withTitle("Author3")
                             .withIssueDate("2016-02-13")
                             .withPersonIdentifierFirstName("Maybe")
                             .withPersonIdentifierLastName("Maybe")
                             .build();

        publication1 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Publication1")
                                  .withAuthor("Testy, TEst")
                                  .withIssueDate("2015-01-01")
                                  .build();

        publication2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Publication2")
                                  .withAuthor("Testy, TEst")
                                  .withIssueDate("2015-01-01")
                                  .build();

        orgUnit1 = ItemBuilder.createItem(context, col3)
                              .withTitle("OrgUnit1")
                              .withAuthor("Testy, TEst")
                              .withIssueDate("2015-01-01")
                              .build();

        orgUnit2 = ItemBuilder.createItem(context, col3)
                .withTitle("OrgUnit2")
                .withAuthor("Testy, TEst")
                .withIssueDate("2015-01-01")
                .build();

        orgUnit3 = ItemBuilder.createItem(context, col3)
                              .withTitle("OrgUnit3")
                              .withAuthor("Test, Testy")
                              .withIssueDate("2015-02-01")
                              .build();

        project1 = ItemBuilder.createItem(context, col4)
                              .withTitle("Project1")
                              .withAuthor("Testy, TEst")
                              .withIssueDate("2015-01-01")
                              .build();

        isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        isOrgUnitOfPersonRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Person"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfPerson", "isPersonOfOrgUnit");

        user1 = EPersonBuilder.createEPerson(context)
                              .withNameInMetadata("first", "last")
                              .withEmail("testaze@gmail.com")
                              .withPassword(password)
                              .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                              .build();

        context.restoreAuthSystemState();
    }


    @Test
    public void findAllRelationshipTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isOrgUnitOfPersonRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Person"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfPerson", "isPersonOfOrgUnit");
        RelationshipType isOrgUnitOfProjectRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Project"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfProject", "isProjectOfOrgUnit");
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, author1, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        Relationship relationship2 = RelationshipBuilder
            .createRelationshipBuilder(context, project1, orgUnit1, isOrgUnitOfProjectRelationshipType).build();

        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author1, isAuthorOfPublicationRelationshipType).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships")
                   .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationship2),
                       RelationshipMatcher.matchRelationship(relationship3)
                   )))
        ;

        getClient().perform(get("/api/core/relationships").param("size", "2").param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 2, 2, 3))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationship2)
                   )))
        ;

        getClient().perform(get("/api/core/relationships").param("size", "2").param("page", "1")
                   .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(1, 2, 2, 3))))
                   .andExpect(jsonPath("$._embedded.relationships", contains(
                       RelationshipMatcher.matchRelationship(relationship3)
                   )))
        ;

    }

    @Test
    public void createRelationshipWriteAccessLeftItem() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._links.leftItem.href",
                                       containsString(publication1.getID().toString())))
                   .andExpect(jsonPath("$._links.rightItem.href",
                                       containsString(author1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void createRelationshipWriteAccessRightItem() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                         .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._links.leftItem.href", containsString(publication1.getID().toString())))
                   .andExpect(jsonPath("$._links.rightItem.href", containsString(author1.getID().toString())));

        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }


    @Test
    public void createRelationshipNoWriteAccess() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();

        String token = getAuthToken(user1.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isForbidden());
    }

    @Test
    public void createRelationshipWithLeftWardValue() throws Exception {
        context.turnOffAuthorisationSystem();

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);
        String leftwardValue = "Name variant test left";

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .param("leftwardValue", leftwardValue)
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                                  .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", containsString(leftwardValue)))
                   .andExpect(jsonPath("$.rightwardValue", is(nullValue())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void createRelationshipWithRightwardValue() throws Exception {
        context.turnOffAuthorisationSystem();

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);
        String rightwardValue = "Name variant test right";

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .param("rightwardValue", rightwardValue)
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                              .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", is(nullValue())))
                   .andExpect(jsonPath("$.rightwardValue", containsString(rightwardValue)));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void createRelationshipWithRightwardValueAndLeftWardValue() throws Exception {
        context.turnOffAuthorisationSystem();

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);
        String leftwardValue = "Name variant test left";
        String rightwardValue = "Name variant test right";

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .param("leftwardValue", leftwardValue)
                                                           .param("rightwardValue", rightwardValue)
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                           .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", containsString(leftwardValue)))
                   .andExpect(jsonPath("$.rightwardValue", containsString(rightwardValue)));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void createMultipleRelationshipsAppendToEndTest() throws Exception {
        context.turnOffAuthorisationSystem();

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user1);

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();


        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        try {
            String token = getAuthToken(user1.getEmail(), password);

            // Add a relationship @ leftPlace 2
            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                isAuthorOfPublicationRelationshipType.getID()
                                                                                     .toString())
                                         .contentType(MediaType.parseMediaType
                                                                   (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                        .TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + publication1
                                                 .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author1
                                                 .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient().perform(get("/api/core/relationships/" + idRef))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.id", is(idRef.get())))
                       .andExpect(jsonPath("$.leftPlace", is(1)));

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                isAuthorOfPublicationRelationshipType.getID()
                                                                                     .toString())
                                         .contentType(MediaType.parseMediaType
                                                                   (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                        .TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + publication1
                                                 .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author2
                                                 .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient().perform(get("/api/core/relationships/" + idRef2))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.id", is(idRef2.get())))
                       .andExpect(jsonPath("$.leftPlace", is(2)));

            // Check Item author order
            getClient().perform(get("/api/core/items/" + publication1.getID()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.metadata", allOf(
                           matchMetadata("dc.contributor.author", "Testy, TEst", 0),
                           matchMetadata("dc.contributor.author", "Smith, Donald", 1),
                           matchMetadata("dc.contributor.author", "Smith, Maria", 2)
                       )));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
            if (idRef2.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef2.get());
            }
        }
    }

    @Test
    public void createRelationshipAndAddLeftWardValueAfterwards() throws Exception {
        context.turnOffAuthorisationSystem();

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);
        String leftwardValue = "Name variant test label";

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .param("projection", "full")
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                                              .andExpect(jsonPath("$", RelationshipMatcher.matchFullEmbeds()))
                          .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // Verify labels are not present
        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", is(nullValue())))
                   .andExpect(jsonPath("$.rightwardValue", is(nullValue())));

        Map<String, String> map = new HashMap<>();
        map.put("leftwardValue", leftwardValue);
        String json = new ObjectMapper().writeValueAsString(map);

        // Add leftwardValue
        getClient(token).perform(put("/api/core/relationships/" + idRef)
                                     .contentType("application/json")
                                     .content(json))
                        .andExpect(status().isOk());

        // Verify leftwardValue is present and rightwardValue not
        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", containsString(leftwardValue)))
                   .andExpect(jsonPath("$.rightwardValue", is(nullValue())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void createRelationshipThenAddLabelsAndRemoveThem() throws Exception {
        context.turnOffAuthorisationSystem();

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.setCurrentUser(user1);
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
        String token = getAuthToken(user1.getEmail(), password);
        String leftwardValue = "Name variant test left";
        String rightwardValue = "Name variant test right";

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                         .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // Verify labels are not present
        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", is(nullValue())))
                   .andExpect(jsonPath("$.rightwardValue", is(nullValue())));

        Map<String, String> map = new HashMap<>();
        map.put("leftwardValue", leftwardValue);
        map.put("rightwardValue", rightwardValue);
        String json = new ObjectMapper().writeValueAsString(map);

        // Add leftwardValue and rightwardValue
        getClient(token).perform(put("/api/core/relationships/" + idRef)
                                     .contentType("application/json")
                                     .content(json))
                        .andExpect(status().isOk());

        // Verify leftwardValue and rightwardValue are present
        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", containsString(leftwardValue)))
                   .andExpect(jsonPath("$.rightwardValue", containsString(rightwardValue)));

        // Remove leftwardValue and rightwardValue
        getClient(token).perform(put("/api/core/relationships/" + idRef)
                                     .contentType("application/json")
                                     .content("{}"))
                        .andExpect(status().isOk());

        // Verify leftwardValue and rightwardValue are both gone
        getClient().perform(get("/api/core/relationships/" + idRef))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(idRef.get())))
                   .andExpect(jsonPath("$.leftwardValue", is(nullValue())))
                   .andExpect(jsonPath("$.rightwardValue", is(nullValue())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    /**
     * This method will test the addition of a mixture of plain-text metadatavalues and relationships to then
     * verify that the place property is still being handled correctly.
     * @throws Exception
     */
    @Test
    public void addRelationshipsAndMetadataToValidatePlaceTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withPersonIdentifierFirstName("Donald")
                                  .withPersonIdentifierLastName("Smith")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maybe")
                                  .withPersonIdentifierLastName("Maybe")
                                  .build();

        Item publication1 = ItemBuilder.createItem(context, col2)
                                       .withTitle("Publication1")
                                       .withIssueDate("2015-01-01")
                                       .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");


        String adminToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        AtomicReference<Integer> idRef3 = new AtomicReference<>();
        try {
        // Here we create our first Relationship to the Publication to give it a dc.contributor.author virtual
        // metadata field.
        getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + publication1
                                                                        .getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items" +
                                                                        "/" + author1
                                                                        .getID()))
                                                   .andExpect(status().isCreated())
                              .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        // Here we call the relationship and verify that the relationship's leftplace is 0
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        context.turnOffAuthorisationSystem();

        // Make sure we grab the latest instance of the Item from the database
        publication1 = itemService.find(context, publication1.getID());
        // Add a plain text dc.contributor.author value
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication1);

        List<MetadataValue> list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        // Ensure that the list of dc.contributor.author values now holds two values ("Smith, Donald" and "plain text")
        assertEquals(2, list.size());
        for (MetadataValue mdv : list) {
            // Here we want to ensure that the "plain text" metadatavalue has place 1 because it was added later than
            // the Relationship, so the "Smith, Donald" should have place 0 and "plain text" should have place 1
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }
        // Testing what was describe above
        MetadataValue author0MD = list.get(0);
        assertEquals("Smith, Donald", author0MD.getValue());
        MetadataValue author1MD = list.get(1);
        assertEquals("plain text", author1MD.getValue());


        context.restoreAuthSystemState();
        // Verify the leftPlace of our relationship is still 0.
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        // Create another Relationship for the Publication, thus creating a third dc.contributor.author mdv
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author2
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        publication1 = itemService.find(context, publication1.getID());
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        // Ensure that we now have three dc.contributor.author mdv ("Smith, Donald", "plain text", "Smith, Maria"
        // In that order which will be checked below the rest call
        assertEquals(3, list.size());
        // Perform the REST call to the relationship to ensure its leftPlace is 2 even though it's only the second
        // Relationship. Note that leftPlace 1 was skipped due to the dc.contributor.author plain text value and
        // This is expected behaviour and should be tested
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef2))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));

        author0MD = list.get(0);
        assertEquals("Smith, Donald", author0MD.getValue());
        author1MD = list.get(1);
        assertEquals("plain text", author1MD.getValue());
        MetadataValue author2MD = list.get(2);
        assertEquals("Smith, Maria", author2MD.getValue());

        context.turnOffAuthorisationSystem();
        // Ensure we have the latest instance of the Item from the database
        publication1 = itemService.find(context, publication1.getID());
        // Add a fourth dc.contributor.author mdv
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication1);

        context.restoreAuthSystemState();
        publication1 = itemService.find(context, publication1.getID());
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        // Assert that the list of dc.contributor.author mdv is now of size 4 in the following order:
        // "Smith, Donald", "plain text", "Smith, Maria", "plain text two"
        assertEquals(4, list.size());
        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                assertEquals(3, mdv.getPlace());
            }
        }

        author0MD = list.get(0);
        assertEquals("Smith, Donald", author0MD.getValue());
        author1MD = list.get(1);
        assertEquals("plain text", author1MD.getValue());
        author2MD = list.get(2);
        assertEquals("Smith, Maria", author2MD.getValue());
        MetadataValue author3MD = list.get(3);
        assertEquals("plain text two", author3MD.getValue());


        // Create the third Relationship thus adding a fifth dc.contributor.author mdv
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author3
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef3.set(read(result.getResponse().getContentAsString(), "$.id")));

        publication1 = itemService.find(context, publication1.getID());
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        // Assert that our dc.contributor.author mdv list is now of size 5
        assertEquals(5, list.size());
        // Assert that the third Relationship has leftPlace 4, even though 3 relationships were created.
        // This is because the plain text values 'occupy' place 1 and 3.
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        // Assert that the list is of size 5 and in the following order:
        // "Smith, Donald", "plain text", "Smith, Maria", "plain text two", "Maybe, Maybe"
        // Thus the order they were added in
        author0MD = list.get(0);
        assertEquals("Smith, Donald", author0MD.getValue());
        author1MD = list.get(1);
        assertEquals("plain text", author1MD.getValue());
        author2MD = list.get(2);
        assertEquals("Smith, Maria", author2MD.getValue());
        author3MD = list.get(3);
        assertEquals("plain text two", author3MD.getValue());
        MetadataValue author4MD = list.get(4);
        assertEquals("Maybe, Maybe", author4MD.getValue());

        context.turnOffAuthorisationSystem();
        // The following additions of Metadata will perform the same sequence of logic and tests as described above
        publication1 = itemService.find(context, publication1.getID());
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication1);

        context.restoreAuthSystemState();

        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        assertEquals(6, list.size());
        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(5, mdv.getPlace());
            }
        }

        author0MD = list.get(0);
        assertEquals("Smith, Donald", author0MD.getValue());
        author1MD = list.get(1);
        assertEquals("plain text", author1MD.getValue());
        author2MD = list.get(2);
        assertEquals("Smith, Maria", author2MD.getValue());
        author3MD = list.get(3);
        assertEquals("plain text two", author3MD.getValue());
        author4MD = list.get(4);
        assertEquals("Maybe, Maybe", author4MD.getValue());
        MetadataValue author5MD = list.get(5);
        assertEquals("plain text three", author5MD.getValue());

        context.turnOffAuthorisationSystem();

        publication1 = itemService.find(context, publication1.getID());
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text four");
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text five");
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text six");
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text seven");
        itemService.update(context, publication1);

        context.restoreAuthSystemState();
        publication1 = itemService.find(context, publication1.getID());
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        assertEquals(10, list.size());
        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text four")) {
                assertEquals(6, mdv.getPlace());
            }
            if (StringUtils.equals(mdv.getValue(), "plain text five")) {
                assertEquals(7, mdv.getPlace());
            }
            if (StringUtils.equals(mdv.getValue(), "plain text six")) {
                assertEquals(8, mdv.getPlace());
            }
            if (StringUtils.equals(mdv.getValue(), "plain text seven")) {
                assertEquals(9, mdv.getPlace());
            }
        }


        author0MD = list.get(0);
        assertEquals("Smith, Donald", author0MD.getValue());
        author1MD = list.get(1);
        assertEquals("plain text", author1MD.getValue());
        author2MD = list.get(2);
        assertEquals("Smith, Maria", author2MD.getValue());
        author3MD = list.get(3);
        assertEquals("plain text two", author3MD.getValue());
        author4MD = list.get(4);
        assertEquals("Maybe, Maybe", author4MD.getValue());
        author5MD = list.get(5);
        assertEquals("plain text three", author5MD.getValue());
        MetadataValue author6MD = list.get(6);
        assertEquals("plain text four", author6MD.getValue());
        MetadataValue author7MD = list.get(7);
        assertEquals("plain text five", author7MD.getValue());
        MetadataValue author8MD = list.get(8);
        assertEquals("plain text six", author8MD.getValue());
        MetadataValue author9MD = list.get(9);
        assertEquals("plain text seven", author9MD.getValue());

        list = itemService.getMetadata(publication1, "dc", "contributor", Item.ANY, Item.ANY);
        assertEquals(10, list.size()); //same size as authors
        list = itemService.getMetadata(publication1, "dc", Item.ANY, Item.ANY, Item.ANY);
        assertEquals(16, list.size()); //also includes title, 4 date fields, uri
        list = itemService.getMetadata(publication1, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        // also includes type, 3 relation.isAuthorOfPublication and 3 relation.isAuthorOfPublication.latestForDiscovery
        // values
        assertEquals(23, list.size());

        } finally {
            RelationshipBuilder.deleteRelationship(idRef1.get());
            RelationshipBuilder.deleteRelationship(idRef2.get());
            RelationshipBuilder.deleteRelationship(idRef3.get());
        }
    }

    /**
     * This method will test the deletion of a plain-text metadatavalue to then
     * verify that the place property is still being handled correctly.
     * @throws Exception
     */
    @Test
    public void deleteMetadataValueAndValidatePlace() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication1 = ItemBuilder.createItem(context, col2)
                                       .withTitle("Publication1")
                                       .withIssueDate("2015-01-01")
                                       .build();

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maybe")
                                  .withPersonIdentifierLastName("Maybe")
                                  .build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // First create the structure of 5 metadatavalues just like the additions test.
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        AtomicReference<Integer> idRef3 = new AtomicReference<>();
        try {
        // This post request will add a first relationship to the publiction and thus create a first set of metadata
        // For the author values, namely "Donald Smith"
        getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + publication1
                                                                        .getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items" +
                                                                        "/" + author1
                                                                        .getID()))
                                                   .andExpect(status().isCreated())
                           .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This test will double check that the leftPlace for this relationship is indeed 0
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        context.turnOffAuthorisationSystem();
        // We retrieve the publication again to ensure that we have the latest DB object of it
        publication1 = itemService.find(context, publication1.getID());
        // Add a plain text metadatavalue to the publication
        // After this addition, the list of authors should like like "Donald Smith", "plain text"
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        List<MetadataValue> list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                // Ensure that this is indeed the second metadatavalue in the list of authors for the publication
                assertEquals(1, mdv.getPlace());
            }
        }

        // This test checks again that the first relationship is still on leftplace 0 and not altered
        // Because of the MetadataValue addition
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        // Creates another Relationship for the Publication and thus adding a third metadata value for the author
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author2
                                                              .getID()))
                                         .andExpect(status().isCreated())
                         .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This test verifies that the newly created Relationship is on leftPlace 2, because the first relationship
        // is on leftPlace 0 and the plain text metadataValue occupies the place 1
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef2))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));
        context.turnOffAuthorisationSystem();
        // Get the publication from the DB again to ensure that we have the latest object
        publication1 = itemService.find(context, publication1.getID());
        // Add a fourth metadata value to the publication
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                // Ensure that this plain text metadata value is on the fourth place (place 3) for the publication
                assertEquals(3, mdv.getPlace());
            }
        }

        // The list should currently look like this: "Donald Smith", "plain text", "Maria Smith", "plain text two"

        // This creates a third relationship for the publication and thus adding a fifth value for author metadatavalues
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author3
                                                              .getID()))
                                         .andExpect(status().isCreated())
                    .andDo(result -> idRef3.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This verifies that the newly created third relationship is on leftPlace 4.
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        context.turnOffAuthorisationSystem();
        publication1 = itemService.find(context, publication1.getID());
        // Create another plain text metadata value on the publication
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                // Verify that this plain text value is indeed the 6th author in the list (place 5)
                assertEquals(5, mdv.getPlace());
            }
        }

        // Now we will have a dc.contributor.author metadatavalue list of size 6 in the following order:
        // "Smith, Donald", "plain text", "Smith, Maria", "plain text two", "Maybe, Maybe", "plain text three"
        List<MetadataValue> authors = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> listToRemove = new LinkedList<>();
        for (MetadataValue metadataValue : authors) {
            if (StringUtils.equals(metadataValue.getValue(), "plain text two")) {
                listToRemove.add(metadataValue);
            }
        }

        context.turnOffAuthorisationSystem();
        // Remove the "plain text two" metadatavalue. Ensure that all mdvs prior to that in the list are unchanged
        // And ensure that the ones coming after this mdv have its place lowered by one.
        itemService.removeMetadataValues(context, publication1, listToRemove);

        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(4, mdv.getPlace());
            }
        }

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef2))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(3)));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef1.get());
            RelationshipBuilder.deleteRelationship(idRef2.get());
            RelationshipBuilder.deleteRelationship(idRef3.get());
        }

    }

    /**
     * This method will test the deletion of a Relationship to then
     * verify that the place property is still being handled correctly.
     * @throws Exception
     */
    @Test
    public void deleteRelationshipsAndValidatePlace() throws Exception {


        context.turnOffAuthorisationSystem();

        Item publication1 = ItemBuilder.createItem(context, col2)
                                       .withTitle("Publication1")
                                       .withIssueDate("2015-01-01")
                                       .build();

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maybe")
                                  .withPersonIdentifierLastName("Maybe")
                                  .build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // First create the structure of 5 metadatavalues just like the additions test.
        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        AtomicReference<Integer> idRef3 = new AtomicReference<>();
        try {
        // This post request will add a first relationship to the publiction and thus create a first set of metadata
        // For the author values, namely "Donald Smith"
        getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + publication1
                                                                        .getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items" +
                                                                        "/" + author1
                                                                        .getID()))
                                                   .andExpect(status().isCreated())
                              .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This test will double check that the leftPlace for this relationship is indeed 0
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        context.turnOffAuthorisationSystem();
        // We retrieve the publication again to ensure that we have the latest DB object of it
        publication1 = itemService.find(context, publication1.getID());
        // Add a plain text metadatavalue to the publication
        // After this addition, the list of authors should like like "Donald Smith", "plain text"
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        List<MetadataValue> list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                // Ensure that this is indeed the second metadatavalue in the list of authors for the publication
                assertEquals(1, mdv.getPlace());
            }
        }

        // This test checks again that the first relationship is still on leftplace 0 and not altered
        // Because of the MetadataValue addition
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        // Creates another Relationship for the Publication and thus adding a third metadata value for the author
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author2
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));


        // This test verifies that the newly created Relationship is on leftPlace 2, because the first relationship
        // is on leftPlace 0 and the plain text metadataValue occupies the place 1
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef2))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));
        context.turnOffAuthorisationSystem();
        // Get the publication from the DB again to ensure that we have the latest object
        publication1 = itemService.find(context, publication1.getID());
        // Add a fourth metadata value to the publication
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                // Ensure that this plain text metadata value is on the fourth place (place 3) for the publication
                assertEquals(3, mdv.getPlace());
            }
        }

        // The list should currently look like this: "Donald Smith", "plain text", "Maria Smith", "plain text two"

        // This creates a third relationship for the publication and thus adding a fifth value for author metadatavalues
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author3
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef3.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This verifies that the newly created third relationship is on leftPlace 4.
        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        context.turnOffAuthorisationSystem();
        publication1 = itemService.find(context, publication1.getID());
        // Create another plain text metadata value on the publication
        itemService.addMetadata(context, publication1, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication1);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                // Verify that this plain text value is indeed the 6th author in the list (place 5)
                assertEquals(5, mdv.getPlace());
            }
        }

        // Now we will have a dc.contributor.author metadatavalue list of size 6 in the following order:
        // "Smith, Donald", "plain text", "Smith, Maria", "plain text two", "Maybe, Maybe", "plain text three"


        // Now we delete the second relationship, the one that made "Smith, Maria" metadatavalue
        // Ensure that all metadatavalues before this one in the list still hold the same place
        // Ensure that all the metadatavalues after this one have their place lowered by one
        getClient(adminToken).perform(delete("/api/core/relationships/" + idRef2));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        publication1 = itemService.find(context, publication1.getID());
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                assertEquals(1, mdv.getPlace());
            }
        }
        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                assertEquals(2, mdv.getPlace());
            }
        }


        list = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                assertEquals(4, mdv.getPlace());
            }
        }


        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(3)));

        } finally {
            RelationshipBuilder.deleteRelationship(idRef1.get());
            RelationshipBuilder.deleteRelationship(idRef2.get());
            RelationshipBuilder.deleteRelationship(idRef3.get());
        }
    }

    /**
     * This method will test the deletion of a Relationship and will then
     * verify that the relation is removed
     * @throws Exception
     */
    @Test
    public void deleteRelationship() throws Exception {
        context.turnOffAuthorisationSystem();

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withPersonIdentifierFirstName("Maria")
                                  .withPersonIdentifierLastName("Smith")
                                  .build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // First create 1 relationship.
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        try {
        // This post request will add a first relationship to the publication and thus create a first set of metadata
        // For the author values, namely "Donald Smith"
        getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/spring-rest/api/core" +
                                                                        "/items/" + publication1
                                                                        .getID() + "\n" +
                                                                        "https://localhost:8080/spring-rest/api/core" +
                                                                        "/items/" + author1
                                                                        .getID()))
                                                   .andExpect(status().isCreated())
                             .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This test checks that there's one relationship on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                                              publication1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(1)));

        // This test checks that there's one relationship on the first author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(1)));

        // This test checks that there's no relationship on the second author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author2.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(0)));

        // Creates another Relationship for the Publication
        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/spring-rest/api/core/items/" + publication1
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/spring-rest/api/core/items/" + author2
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        // This test checks that there are 2 relationships on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                                              publication1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(2)));

        // This test checks that there's one relationship on the first author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(1)));

        // This test checks that there's one relationship on the second author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author2.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(1)));


        // Now we delete the first relationship
        getClient(adminToken).perform(delete("/api/core/relationships/" + idRef1));


        // This test checks that there's one relationship on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                                              publication1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(1)));

        // This test checks that there's no relationship on the first author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(0)));

        // This test checks that there are one relationship on the second author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author2.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(1)));


        // Now we delete the second relationship
        getClient(adminToken).perform(delete("/api/core/relationships/" + idRef2));


        // This test checks that there's no relationship on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                                              publication1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(0)));

        // This test checks that there's no relationship on the first author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author1.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(0)));

        // This test checks that there are no relationship on the second author
        getClient(adminToken).perform(get("/api/core/items/" +
                                              author2.getID() + "/relationships"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("page.totalElements", is(0)));
        } finally {
            if (idRef1.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef1.get());
            }
            if (idRef2.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef2.get());
            }
        }
    }

    /**
     * This test will simply add Relationships between Items with a useForPlace attribute set to false for the
     * RelationshipType. We want to test that the Relationships that are created will still have their place
     * attributes handled in a correct way
     * @throws Exception
     */
    @Test
    public void addRelationshipsNotUseForPlace() throws Exception {
        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        AtomicReference<Integer> idRef3 = new AtomicReference<>();
        try {

        String adminToken = getAuthToken(admin.getEmail(), password);

        // This is essentially a sequence of adding Relationships by POST and then checking with GET to see
        // if the place is being set properly.
        getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isOrgUnitOfPersonRelationshipType.getID()
                                                                                                        .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + author1
                                                                        .getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items" +
                                                                        "/" + orgUnit1
                                                                        .getID()))
                                                   .andExpect(status().isCreated())
                             .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author2
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef2))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));

        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author3
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef3.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(2)));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef1.get());
            RelationshipBuilder.deleteRelationship(idRef2.get());
            RelationshipBuilder.deleteRelationship(idRef3.get());
        }
    }

    /**
     * This test will simply add Relationships between Items with a useForPlace attribute set to false for the
     * RelationshipType. We want to test that the Relationships that are created will still have their place
     * attributes handled in a correct way. It will then delete a Relationship and once again ensure that the place
     * attributes are being handled correctly.
     * @throws Exception
     */
    @Test
    public void addAndDeleteRelationshipsNotUseForPlace() throws Exception {
        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        AtomicReference<Integer> idRef3 = new AtomicReference<>();
        try {

        String adminToken = getAuthToken(admin.getEmail(), password);

        // This is essentially a sequence of adding Relationships by POST and then checking with GET to see
        // if the place is being set properly.
        getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isOrgUnitOfPersonRelationshipType.getID()
                                                                                                        .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + author1
                                                                        .getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items" +
                                                                        "/" + orgUnit1
                                                                        .getID()))
                                                   .andExpect(status().isCreated())
                             .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author2
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef2.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef2))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));

        getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author3
                                                              .getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1
                                                              .getID()))
                                         .andExpect(status().isCreated())
                   .andDo(result -> idRef3.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(2)));

        // Here we will delete the secondRelationship and then verify that the others have their place handled properly
        getClient(adminToken).perform(delete("/api/core/relationships/" + idRef2))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef1))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        getClient(adminToken).perform(get("/api/core/relationships/" + idRef3))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef1.get());
            RelationshipBuilder.deleteRelationship(idRef2.get());
            RelationshipBuilder.deleteRelationship(idRef3.get());
        }
    }

    /**
     * This test will create a relationship with author 1 - publication 1
     * Then modify this relationship by changing the left item to author 2 via PUT > Verify
     * Then modify this relationship by changing the right item to publication 2 via PUT > Verify
     *
     * @throws Exception
     */
    @Test
    public void putRelationshipAdminAccess() throws Exception {
        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        try {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")));

        //Modify the left item in the relationship publication > publication 2
        getClient(token).perform(put("/api/core/relationships/" + idRef1 + "/leftItem")
                                                            .contentType(MediaType.parseMediaType
                                                                (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                     .TEXT_URI_LIST_VALUE))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + publication2
                                                                    .getID()))
                                               .andExpect(status().isOk());

        //verify left item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication2.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author1.getID().toString())));

        //Modify the right item in the relationship publication > publication 2
       getClient(token).perform(put("/api/core/relationships/" + idRef1 + "/rightItem")
                                                            .contentType(MediaType.parseMediaType
                                                                (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                     .TEXT_URI_LIST_VALUE))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + author2
                                                                    .getID()))
                                               .andExpect(status().isOk());

        //verify right item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author2.getID().toString())))
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication2.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef1.get());
        }
    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is possible for a user with WRITE permissions on author 1 and author 2
     */
    @Test
    public void putRelationshipWriteAccessOnAuthors() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user1);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        String token = getAuthToken(user1.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        //change right item from author 1 > author 2
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/rightItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + author2
                                                                    .getID()))
                                               .andExpect(status().isOk());

        //verify change  and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author2.getID().toString())))
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is possible for a user with WRITE permissions on publication 1
     */
    @Test
    public void putRelationshipWriteAccessOnPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        String token = getAuthToken(user1.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        //change rightItem from author1 > author2
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/rightItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + author2
                                                                    .getID()))
                                               .andExpect(status().isOk());

        //verify right item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author2.getID().toString())))
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication1.getID().toString())));

        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }


    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 2 and author 1
     * Verify this is possible for a user with WRITE permissions on publication 1 and publication 2
     */
    @Test
    public void putRelationshipWriteAccessOnPublications() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, publication2, Constants.WRITE, user1);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        String token = getAuthToken(user1.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        //change leftItem
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/leftItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + publication2
                                                                    .getID()))
                                               .andExpect(status().isOk());

        //verify change  and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication2.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }


    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 2 and author 1
     * Verify this is possible for a user with WRITE permissions on author 1
     */
    @Test
    public void putRelationshipWriteAccessOnAuthor() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        String token = getAuthToken(user1.getEmail(), password);

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/api/core/relationships")
                         .param("relationshipType",
                                isAuthorOfPublicationRelationshipType.getID()
                                                                     .toString())
                         .contentType(MediaType.parseMediaType("text/uri-list"))
                         .content(
                             "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                                 "https://localhost:8080/server/api/core/items/" + author1.getID()))
            .andExpect(status().isCreated())
            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        //change left item
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/leftItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + publication2
                                                                    .getID()))
                                               .andExpect(status().isOk());

        //verify change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication2.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }


    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is NOT possible for a user without WRITE permissions
     */
    @Test
    public void putRelationshipNoAccess() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                       .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        token = getAuthToken(user1.getEmail(), password);
        //attempt change, expect not allowed
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/rightItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + author2
                                                                    .getID()))
                                               .andExpect(status().isForbidden());
        //verify nothing changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication1.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is NOT possible for a user with WRITE permissions on author 1
     */
    @Test
    public void putRelationshipOnlyAccessOnOneAuthor() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        token = getAuthToken(user1.getEmail(), password);
        //attempt right item change, expect not allowed
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/rightItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + author2
                                                                    .getID()))
                                               .andExpect(status().isForbidden());

        //verify not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication1.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 2 and author 1
     * Verify this is NOT possible for a user with WRITE permissions on publication 1
     */
    @Test
    public void putRelationshipOnlyAccessOnOnePublication() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);

        String token = getAuthToken(user1.getEmail(), password);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

       getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        //attempt left item change, expect not allowed
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/leftItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + publication2
                                                                    .getID()))
                                               .andExpect(status().isForbidden());
        //verify not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                            containsString(publication1.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                            containsString(author1.getID().toString())));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }

    }

    @Test
    public void findRelationshipByLabelTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isOrgUnitOfPersonRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Person"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfPerson", "isPersonOfOrgUnit");
        RelationshipType isOrgUnitOfProjectRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Project"),
                                  entityTypeService.findByEntityType(context, "OrgUnit"),
                                  "isOrgUnitOfProject", "isProjectOfOrgUnit");
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        // We're creating a Relationship of type isOrgUnitOfPerson between an author and an orgunit
        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, author1, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        Relationship relationshipOrgunitExtra = RelationshipBuilder
                .createRelationshipBuilder(context, author1, orgUnit2, isOrgUnitOfPersonRelationshipType).build();

        // We're creating a Relationship of type isOrgUnitOfPerson between a different author and the same orgunit
        Relationship relationshipAuthorExtra = RelationshipBuilder
            .createRelationshipBuilder(context, author2, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        // We're creating a Relationship of type isOrgUnitOfProject between a project and an orgunit
        Relationship relationship2 = RelationshipBuilder
            .createRelationshipBuilder(context, project1, orgUnit1, isOrgUnitOfProjectRelationshipType).build();

        // We're creating a Relationship of type isAuthorOfPublication between a publication and an author
        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author1, isAuthorOfPublicationRelationshipType).build();

        context.restoreAuthSystemState();
        // Perform a GET request to the searchByLabel endpoint, asking for Relationships of type isOrgUnitOfPerson
        // With an extra parameter namely DSO which resolves to the author(Smith, Donald) in the first
        // relationship created.
        // As that first relationship is the only one created with the given author(Smith, Donald) that holds the
        // RelationshipType "isOrgUnitOfPerson", that Relationship should be the only one returned.
        // This is what we're checking for
        getClient().perform(get("/api/core/relationships/search/byLabel")
                                .param("label", "isOrgUnitOfPerson")
                                .param("dso", author1.getID().toString())
                                .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))))
                   .andExpect(jsonPath("$._embedded.relationships", hasItem(
                       RelationshipMatcher.matchRelationship(relationship1)
                   ))) //check ordering
                   .andExpect(jsonPath("$._embedded.relationships[0]._links.rightItem.href",
                       containsString(orgUnit1.getID().toString())
                   ))
                   .andExpect(jsonPath("$._embedded.relationships[1]._links.rightItem.href",
                       containsString(orgUnit2.getID().toString())
                   ))
        ;

        // Perform a GET request to the searchByLabel endpoint, asking for Relationships of type isOrgUnitOfPerson
        // We do not specificy a DSO param, which means ALL relationships of type isOrgUnitOfPerson should be returned
        // Which is what we're checking for, both the first relationship and the one with a different author
        // should be returned
        getClient().perform(get("/api/core/relationships/search/byLabel")
                                .param("label", "isOrgUnitOfPerson")
                                .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationshipAuthorExtra),
                       RelationshipMatcher.matchRelationship(relationshipOrgunitExtra)
                   )))
        ;
    }

    @Test
    public void putRelationshipWithNonexistentID() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        int nonexistentRelationshipID = 404404404;
        //attempt left item change on non-existent relationship
        getClient(token).perform(put("/api/core/relationships/" + nonexistentRelationshipID + "/leftItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + publication1.getID()))
                .andExpect(status().isNotFound());

    }

    @Test
    public void putRelationshipWithInvalidItemIDInBody() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {

        getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        int nonexistentItemID = 404404404;
        //attempt left item change on non-existent relationship
        getClient(token).perform(put("/api/core/relationships/" + idRef + "/leftItem")
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/server/api/core/items/" + nonexistentItemID))
                                               .andExpect(status().isUnprocessableEntity());
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    /**
     * Verify when a rightward value is present which has been configured to
     * be used for virtual metadata, that the virtual metadata is populated
     * with the custom value
     */
    @Test
    public void rightwardValueRelationshipTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author1, isAuthorOfPublicationRelationshipType)
            .withRightwardValue("RightwardValueTest").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships")
                   .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship3)
                   )))
        ;

        getClient().perform(get("/api/core/items/" + publication1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata("dc.contributor.author", "RightwardValueTest"),
                       matchMetadata("dc.title", "Publication1"))));
    }

    /**
     * Verify when no rightward value is present, that the virtual metadata is populated
     * with the metadata from the related item
     */
    @Test
    public void nonRightwardValueRelationshipTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author3, isAuthorOfPublicationRelationshipType)
            .withLeftPlace(1).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships")
                   .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship3)
                   )))
        ;

        getClient().perform(get("/api/core/items/" + publication1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata("dc.contributor.author", "Maybe, Maybe"),
                       matchMetadata("dc.contributor.author", "Testy, TEst"),
                       matchMetadata("dc.title", "Publication1"))));
    }

    /**
     * Verify when a rightward value is present which has been configured to
     * be used for virtual metadata, that the virtual metadata is populated
     * with the custom value
     * Verify that only the relationship containing the rightward value will be updated
     */
    @Test
    public void mixedRightwardValueAndRegularRelationshipTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withPersonIdentifierFirstName("testingFirstName")
                                  .withPersonIdentifierLastName("testingLastName")
                                  .build();

        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author3, isAuthorOfPublicationRelationshipType)
            .withLeftPlace(1).build();

        Relationship relationship2 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author1, isAuthorOfPublicationRelationshipType)
            .withRightwardValue("TestingRightwardValue").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships")
                   .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship3),
                       RelationshipMatcher.matchRelationship(relationship2)
                   )))
        ;

        getClient().perform(get("/api/core/items/" + publication1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata("dc.contributor.author", "Maybe, Maybe"),
                       matchMetadata("dc.contributor.author", "Testy, TEst"),
                       matchMetadata("dc.contributor.author", "TestingRightwardValue"),
                       not(matchMetadata("dc.contributor.author", "testingLastName, testingFirstName")),
                       matchMetadata("dc.title", "Publication1"))));
    }

    /**
     * Verify when a leftward value is present which has NOT been configured to
     * be used for virtual metadata, that the virtual metadata is NOT populated
     * with the custom value
     */
    @Test
    public void leftwardValueRelationshipTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication1, author3, isAuthorOfPublicationRelationshipType)
            .withLeftwardValue("leftwardValue").withLeftPlace(1).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships")
                   .param("projection", "full"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship3)
                   )))
        ;

        getClient().perform(get("/api/core/items/" + publication1.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata("dc.contributor.author", "Maybe, Maybe"),
                       matchMetadata("dc.contributor.author", "Testy, TEst"),
                       matchMetadata("dc.title", "Publication1"))));
    }

    @Test
    public void putRelationshipWithJson() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        Integer idRef = null;
        try {

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1
                                                                   .getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1
                                                                   .getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        idRef = Integer.parseInt(id);

        RelationshipRest relationshipRest = new RelationshipRest();
        relationshipRest.setLeftPlace(0);
        relationshipRest.setRightPlace(1);
        relationshipRest.setLeftwardValue(null);
        relationshipRest.setRightwardValue(null);
        //Modify the left item in the relationship publication > publication 2
        getClient(token).perform(put("/api/core/relationships/" + idRef)
                                                            .contentType(contentType)
                                                            .content(mapper.writeValueAsBytes(relationshipRest)))
                                               .andExpect(status().isOk());

        //verify left item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + idRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.leftPlace", is(0)))
                        .andExpect(jsonPath("$.rightPlace", is(1)));

        } finally {
            RelationshipBuilder.deleteRelationship(idRef);
        }

    }

    @Test
    public void putRelationshipWithJsonMoveInFrontOtherMetadata() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        Integer idRef = null;
        Integer idRef2 = null;
        try {
            // Add a relationship
            MvcResult mvcResult = getClient(token)
                .perform(post("/api/core/relationships")
                             .param("relationshipType", isAuthorOfPublicationRelationshipType.getID().toString())
                             .contentType(MediaType.parseMediaType(
                                 org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                             .content(
                                 "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                                     "https://localhost:8080/server/api/core/items/" + author1.getID()))
                .andExpect(status().isCreated())
                .andReturn();

            ObjectMapper mapper = new ObjectMapper();
            String content = mvcResult.getResponse().getContentAsString();
            Map<String, Object> map = mapper.readValue(content, Map.class);
            String id = String.valueOf(map.get("id"));
            idRef = Integer.parseInt(id);

            // Add some more metadata
            List<Operation> ops = new ArrayList<Operation>();
            ops.add(new AddOperation("/metadata/dc.contributor.author/-", "Metadata, First"));
            ops.add(new AddOperation("/metadata/dc.contributor.author/-", "Metadata, Second"));

            getClient(token).perform(patch("/api/core/items/" + publication1.getID())
                                         .content(getPatchContent(ops))
                                         .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON));

            // Add another relationship
            mvcResult = getClient(token)
                .perform(post("/api/core/relationships")
                             .param("relationshipType", isAuthorOfPublicationRelationshipType.getID().toString())
                             .contentType(MediaType.parseMediaType(
                                 org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                             .content(
                                 "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                                     "https://localhost:8080/server/api/core/items/" + author2.getID()))
                .andExpect(status().isCreated())
                .andReturn();

            content = mvcResult.getResponse().getContentAsString();
            map = mapper.readValue(content, Map.class);
            id = String.valueOf(map.get("id"));
            idRef2 = Integer.parseInt(id);

            // Check Item author order
            getClient().perform(get("/api/core/items/" + publication1.getID()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.metadata", allOf(
                           matchMetadata("dc.contributor.author", "Testy, TEst", 0),
                           matchMetadata("dc.contributor.author", "Smith, Donald", 1),      // first relationship
                           matchMetadata("dc.contributor.author", "Metadata, First", 2),
                           matchMetadata("dc.contributor.author", "Metadata, Second", 3),
                           matchMetadata("dc.contributor.author", "Smith, Maria", 4)        // second relationship
                       )));

            RelationshipRest relationshipRest = new RelationshipRest();
            relationshipRest.setLeftPlace(0);
            relationshipRest.setRightPlace(1);
            relationshipRest.setLeftwardValue(null);
            relationshipRest.setRightwardValue(null);

            // Modify the place of the second relationship -> put it in front of all other metadata
            getClient(token).perform(put("/api/core/relationships/" + idRef2)
                                         .contentType(contentType)
                                         .content(mapper.writeValueAsBytes(relationshipRest)))
                            .andExpect(status().isOk());

            // Verify the place has changed to the new value
            getClient(token).perform(get("/api/core/relationships/" + idRef2))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.leftPlace", is(0)))
                            .andExpect(jsonPath("$.rightPlace", is(1)));

            // Verify the other metadata have moved back
            getClient().perform(get("/api/core/items/" + publication1.getID()))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.metadata", allOf(
                           matchMetadata("dc.contributor.author", "Smith, Maria", 0),       // second relationship
                           matchMetadata("dc.contributor.author", "Testy, TEst", 1),
                           matchMetadata("dc.contributor.author", "Smith, Donald", 2),      // first relationship
                           matchMetadata("dc.contributor.author", "Metadata, First", 3),
                           matchMetadata("dc.contributor.author", "Metadata, Second", 4)
                       )));

        } finally {
            RelationshipBuilder.deleteRelationship(idRef);
            RelationshipBuilder.deleteRelationship(idRef2);
        }

    }

    @Test
    public void orgUnitAndOrgUnitRelationshipVirtualMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType orgUnit = entityTypeService.findByEntityType(context, "OrgUnit");
        RelationshipType isParentOrgUnitOf = relationshipTypeService
            .findbyTypesAndTypeName(context, orgUnit, orgUnit, "isParentOrgUnitOf", "isChildOrgUnitOf");

        MetadataSchema metadataSchema = metadataSchemaService.find(context, "relation");
        MetadataFieldBuilder.createMetadataField(context, metadataSchema, "isParentOrgUnitOf", null, null).build();
        MetadataFieldBuilder.createMetadataField(context, metadataSchema, "isChildOrgUnitOf", null, null).build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
            // Here we create our first Relationship to the Publication to give it a dc.contributor.author virtual
            // metadata field.
            getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType",
                    isParentOrgUnitOf.getID().toString())
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                        .TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/server/api/core/items/" + orgUnit1
                        .getID() + "\n" +
                    "https://localhost:8080/server/api/core/items" +
                    "/" + orgUnit2
                        .getID()))
                                 .andExpect(status().isCreated())
                                 .andDo(result -> idRef
                                     .set(read(result.getResponse().getContentAsString(), "$.id")));

            itemService.getMetadata(orgUnit1, "*", "*", "*", "*", true);

            getClient(adminToken).perform(get("/api/core/items/" + orgUnit1.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.metadata['relation.isParentOrgUnitOf'][0].value",
                                     is(String.valueOf(orgUnit2.getID()))));
            getClient(adminToken).perform(get("/api/core/items/" + orgUnit2.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.metadata['relation.isChildOrgUnitOf'][0].value",
                                     is(String.valueOf(orgUnit1.getID()))));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void orgUnitFindByLabelParentChildOfCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType orgUnit = entityTypeService.findByEntityType(context, "OrgUnit");
        RelationshipType isParentOrgUnitOf = relationshipTypeService
            .findbyTypesAndTypeName(context, orgUnit, orgUnit, "isParentOrgUnitOf", "isChildOrgUnitOf");

        MetadataSchema metadataSchema = metadataSchemaService.find(context, "relation");
        MetadataFieldBuilder.createMetadataField(context, metadataSchema, "isParentOrgUnitOf", null, null).build();
        MetadataFieldBuilder.createMetadataField(context, metadataSchema, "isChildOrgUnitOf", null, null).build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> idRef2 = new AtomicReference<>();
        try {
            // Here we create our first Relationship to the Publication to give it a dc.contributor.author virtual
            // metadata field.
            getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType",
                    isParentOrgUnitOf.getID().toString())
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                        .TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/server/api/core/items/" + orgUnit1
                        .getID() + "\n" +
                    "https://localhost:8080/server/api/core/items" +
                    "/" + orgUnit2
                        .getID()))
                                 .andExpect(status().isCreated())
                                 .andDo(result -> idRef
                                     .set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType",
                    isParentOrgUnitOf.getID().toString())
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                        .TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/server/api/core/items/" + orgUnit2
                        .getID() + "\n" +
                    "https://localhost:8080/server/api/core/items" +
                    "/" + orgUnit3
                        .getID()))
                                 .andExpect(status().isCreated())
                                 .andDo(result -> idRef2
                                     .set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient().perform(get("/api/core/relationships/search/byLabel")
                .param("label", "isChildOrgUnitOf")
                .param("dso", String.valueOf(orgUnit2.getID()))
                .param("page", "0")
                .param("size", "1"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 1, 1, 1)));
        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
            RelationshipBuilder.deleteRelationship(idRef2.get());
        }
    }


    @Test
    public void orgUnitLeftMaxCardinalityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType orgUnit = entityTypeService.findByEntityType(context, "OrgUnit");
        RelationshipType isParentOrgUnitOf = relationshipTypeService
            .findbyTypesAndTypeName(context, orgUnit, orgUnit, "isParentOrgUnitOf", "isChildOrgUnitOf");

        MetadataSchema metadataSchema = metadataSchemaService.find(context, "relation");
        MetadataFieldBuilder.createMetadataField(context, metadataSchema, "isParentOrgUnitOf", null, null).build();
        MetadataFieldBuilder.createMetadataField(context, metadataSchema, "isChildOrgUnitOf", null, null).build();

        String adminToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
            // Here we create our first Relationship to the Publication to give it a dc.contributor.author virtual
            // metadata field.
            getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType",
                    isParentOrgUnitOf.getID().toString())
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                        .TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/server/api/core/items/" + orgUnit1
                        .getID() + "\n" +
                    "https://localhost:8080/server/api/core/items" +
                    "/" + orgUnit2
                        .getID()))
                                 .andExpect(status().isCreated())
                                 .andDo(result -> idRef
                                     .set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(adminToken).perform(post("/api/core/relationships")
                .param("relationshipType",
                    isParentOrgUnitOf.getID().toString())
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                        .TEXT_URI_LIST_VALUE))
                .content(
                    "https://localhost:8080/server/api/core/items/" + orgUnit1
                        .getID() + "\n" +
                    "https://localhost:8080/server/api/core/items" +
                    "/" + orgUnit3
                        .getID()))
                                 .andExpect(status().isBadRequest());

            getClient().perform(get("/api/core/relationships/search/byLabel")
                .param("label", "isParentOrgUnitOf")
                .param("dso", String.valueOf(orgUnit1.getID()))
                .param("page", "0"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)));

        } finally {
            RelationshipBuilder.deleteRelationship(idRef.get());
        }
    }

    @Test
    public void testVirtualMdInRESTAndSolrDoc() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity types if needed
        EntityType journalEntityType = entityTypeService.findByEntityType(context, "Journal");
        if (journalEntityType == null) {
            journalEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        }
        EntityType journalVolumeEntityType = entityTypeService.findByEntityType(context, "JournalVolume");
        if (journalVolumeEntityType == null) {
            journalVolumeEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        }
        EntityType journalIssueEntityType = entityTypeService.findByEntityType(context, "JournalIssue");
        if (journalIssueEntityType == null) {
            journalIssueEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();
        }
        EntityType publicationEntityType = entityTypeService.findByEntityType(context, "Publication");
        if (publicationEntityType == null) {
            publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        }

        // Create relationship types if needed
        RelationshipType isPublicationOfJournalIssue = relationshipTypeService
            .findbyTypesAndTypeName(context, journalIssueEntityType, publicationEntityType,
                "isPublicationOfJournalIssue", "isJournalIssueOfPublication");
        if (isPublicationOfJournalIssue == null) {
            isPublicationOfJournalIssue = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
                journalIssueEntityType, publicationEntityType, "isPublicationOfJournalIssue",
                "isJournalIssueOfPublication", null, null, null, null).build();
        }
        RelationshipType isIssueOfJournalVolume = relationshipTypeService
            .findbyTypesAndTypeName(context, journalVolumeEntityType, journalIssueEntityType,
                "isIssueOfJournalVolume", "isJournalVolumeOfIssue");
        if (isIssueOfJournalVolume == null) {
            isIssueOfJournalVolume = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
                journalVolumeEntityType, journalIssueEntityType, "isIssueOfJournalVolume",
                "isJournalVolumeOfIssue", null, null, null, null).build();
        } else {
            // Otherwise error in destroy methods when removing Journal Issue-Journal Volume relationship
            // since the rightMinCardinality constraint would be violated upon deletion
            isIssueOfJournalVolume.setRightMinCardinality(0);
        }
        RelationshipType isVolumeOfJournal = relationshipTypeService
            .findbyTypesAndTypeName(context, journalEntityType, journalVolumeEntityType,
                "isVolumeOfJournal", "isJournalOfVolume");
        if (isVolumeOfJournal == null) {
            isVolumeOfJournal = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
                journalEntityType, journalVolumeEntityType, "isVolumeOfJournal", "isJournalOfVolume",
                null, null, null, null).build();
        } else {
            // Otherwise error in destroy methods when removing Journal Volume - Journal relationship
            // since the rightMinCardinality constraint would be violated upon deletion
            isVolumeOfJournal.setRightMinCardinality(0);
        }

        // Create virtual metadata fields if needed
        MetadataSchema journalSchema = metadataSchemaService.find(context, "journal");
        if (journalSchema == null) {
            journalSchema = metadataSchemaService.create(context, "journal", "journal");
        }
        String journalTitleVirtualMdField = "journal.title";
        MetadataField journalTitleField = metadataFieldService.findByString(context, journalTitleVirtualMdField, '.');
        if (journalTitleField == null) {
            metadataFieldService.create(context, journalSchema, "title", null, "Journal Title");
        }

        String journalTitle = "Journal Title Test";

        // Create entity items
        Item journal = ItemBuilder.createItem(context, col5).withTitle(journalTitle).build();
        Item journalVolume = ItemBuilder.createItem(context, col6).withTitle("JournalVolume").build();
        Item journalIssue = ItemBuilder.createItem(context, col7).withTitle("JournalIssue").build();
        Item publication = ItemBuilder.createItem(context, col2).withTitle("Publication").build();

        // Link Publication-Journal Issue
        RelationshipBuilder.createRelationshipBuilder(context, journalIssue, publication, isPublicationOfJournalIssue)
                           .build();
        // Link Journal Issue-Journal Volume
        RelationshipBuilder.createRelationshipBuilder(context, journalVolume, journalIssue, isIssueOfJournalVolume)
                           .build();
        mockSolrSearchCore.getSolr().commit(false, false);

        // Verify Publication item via REST does not contain virtual md journal.title
        getClient().perform(get("/api/core/items/" + publication.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata." + journalTitleVirtualMdField).doesNotExist());

        // Verify Publication item via Solr does not contain virtual md journal.title
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("search.resourceid:" + publication.getID());
        QueryResponse queryResponse = mockSolrSearchCore.getSolr().query(solrQuery);
        assertThat(queryResponse.getResults().size(), equalTo(1));
        assertNull(queryResponse.getResults().get(0).getFieldValues(journalTitleVirtualMdField));

        // Link Journal Volume - Journal
        RelationshipBuilder.createRelationshipBuilder(context, journal, journalVolume, isVolumeOfJournal).build();
        mockSolrSearchCore.getSolr().commit(false, false);

        // Verify Publication item via REST does contain virtual md journal.title
        getClient().perform(get("/api/core/items/" + publication.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata(journalTitleVirtualMdField, journalTitle))));

        // Verify Publication item via Solr contains virtual md journal.title
        queryResponse = mockSolrSearchCore.getSolr().query(solrQuery);
        assertThat(queryResponse.getResults().size(), equalTo(1));
        assertEquals(journalTitle,
            ((List) queryResponse.getResults().get(0).getFieldValues(journalTitleVirtualMdField)).get(0));

        // Verify Journal Volume item via REST also contains virtual md journal.title
        getClient().perform(get("/api/core/items/" + journalVolume.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata(journalTitleVirtualMdField, journalTitle))));

        // Verify Journal Volume item via Solr also contains virtual md journal.title
        solrQuery.setQuery("search.resourceid:" + journalVolume.getID());
        queryResponse = mockSolrSearchCore.getSolr().query(solrQuery);
        assertThat(queryResponse.getResults().size(), equalTo(1));
        assertEquals(journalTitle,
            ((List) queryResponse.getResults().get(0).getFieldValues(journalTitleVirtualMdField)).get(0));

        // Verify Journal Issue item via REST also contains virtual md journal.title
        getClient().perform(get("/api/core/items/" + journalIssue.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata", allOf(
                       matchMetadata(journalTitleVirtualMdField, journalTitle))));

        // Verify Journal Issue item via Solr also contains virtual md journal.title
        solrQuery.setQuery("search.resourceid:" + journalIssue.getID());
        queryResponse = mockSolrSearchCore.getSolr().query(solrQuery);
        assertThat(queryResponse.getResults().size(), equalTo(1));
        assertEquals(journalTitle,
            ((List) queryResponse.getResults().get(0).getFieldValues(journalTitleVirtualMdField)).get(0));

        context.restoreAuthSystemState();
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        getClient().perform(get("/api/core/relationships/" + 1000))
                .andExpect(status().isNotFound());
    }

    /**
     * Verify whether the relationship metadata appears correctly on both members.
     * {@link #isAuthorOfPublicationRelationshipType} is tested again in
     * {@link LeftTiltedRelationshipRestRepositoryIT} with tilted set to left.
     */
    @Test
    public void testIsAuthorOfPublicationRelationshipMetadataViaREST() throws Exception {
        context.turnOffAuthorisationSystem();

        RelationshipBuilder.createRelationshipBuilder(
                context, publication1, author1, isAuthorOfPublicationRelationshipType
        ).build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // get author metadata using REST
        getClient(adminToken)
            .perform(
                get("/api/core/items/{uuid}", author1.getID())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", matchMetadata(
                String.format("%s.isPublicationOfAuthor", MetadataSchemaEnum.RELATION.getName()),
                publication1.getID().toString()
            )));

        // get publication metadata using REST
        getClient(adminToken)
            .perform(
                get("/api/core/items/{uuid}", publication1.getID())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", matchMetadata(
                String.format("%s.isAuthorOfPublication", MetadataSchemaEnum.RELATION.getName()),
                author1.getID().toString()
            )));
    }

    /**
     * Verify whether the relationship metadata appears correctly on both members.
     * {@link #isOrgUnitOfPersonRelationshipType} is tested again in
     * {@link RightTiltedRelationshipRestRepositoryIT} with tilted set to right.
     */
    @Test
    public void testIsOrgUnitOfPersonRelationshipMetadataViaREST() throws Exception {
        context.turnOffAuthorisationSystem();

        RelationshipBuilder.createRelationshipBuilder(
            context, author1, orgUnit1, isOrgUnitOfPersonRelationshipType
        ).build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // get author metadata using REST
        getClient(adminToken)
            .perform(
                get("/api/core/items/{uuid}", author1.getID())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", matchMetadata(
                String.format("%s.isOrgUnitOfPerson", MetadataSchemaEnum.RELATION.getName()),
                orgUnit1.getID().toString()
            )));

        // get org unit metadata using REST
        getClient(adminToken)
            .perform(
                get("/api/core/items/{uuid}", orgUnit1.getID())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", matchMetadata(
                String.format("%s.isPersonOfOrgUnit", MetadataSchemaEnum.RELATION.getName()),
                author1.getID().toString()
            )));
    }

    @Test
    public void findByItemsAndTypeTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship1 = RelationshipBuilder.createRelationshipBuilder(context, publication1, author3,
                                                         isAuthorOfPublicationRelationshipType)
                                                        .withLeftPlace(1)
                                                        .build();
        Relationship relationship2 = RelationshipBuilder.createRelationshipBuilder(context, publication1, author1,
                                                         isAuthorOfPublicationRelationshipType)
                                                        .withLeftPlace(1)
                                                        .build();

        context.restoreAuthSystemState();

        // by left relation
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString())
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                              RelationshipMatcher.matchRelationshipValues(relationship1),
                              RelationshipMatcher.matchRelationshipValues(relationship2)
                              )))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));

        // by right relation
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                   .param("relationshipLabel", "isPublicationOfAuthor")
                   .param("focusItem", author1.getID().toString())
                   .param("relatedItem", publication1.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", contains(
                              RelationshipMatcher.matchRelationshipValues(relationship2)
                              )))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByItemsAndTypeBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                             entityTypeService.findByEntityType(context, "Person"),
                                             "isAuthorOfPublication", "isPublicationOfAuthor");

        RelationshipBuilder.createRelationshipBuilder(context, publication1, author3,
                            isAuthorOfPublicationRelationshipType)
                           .withLeftPlace(1)
                           .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1, author1,
                            isAuthorOfPublicationRelationshipType)
                           .withLeftPlace(1)
                           .build();

        context.restoreAuthSystemState();

        // missing relationshipLabel
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", "1")
                   .param("focusItem", publication1.getID().toString())
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isBadRequest());

        // missing typeId
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString())
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isBadRequest());

        // missing focusItem
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", "1")
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isBadRequest());

        // missing relatedItem
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", "1")
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString()))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findByItemsAndTypeUnprocessableEntityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");

        RelationshipBuilder.createRelationshipBuilder(context, publication1, author3,
                            isAuthorOfPublicationRelationshipType)
                           .withLeftPlace(1)
                           .build();
        RelationshipBuilder.createRelationshipBuilder(context, publication1, author1,
                            isAuthorOfPublicationRelationshipType)
                           .withLeftPlace(1)
                           .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                   .param("relationshipLabel", "wrongLabel")
                   .param("focusItem", orgUnit1.getID().toString())
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByItemsAndTypeEmptyResponceTest() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                             entityTypeService.findByEntityType(context, "Person"),
                                             "isAuthorOfPublication", "isPublicationOfAuthor");

        RelationshipBuilder.createRelationshipBuilder(context, publication1, author3,
                            isAuthorOfPublicationRelationshipType)
                           .withLeftPlace(1)
                           .build();
        RelationshipBuilder.createRelationshipBuilder(context, publication1, author1,
                            isAuthorOfPublicationRelationshipType)
                           .withLeftPlace(1)
                           .build();

        context.restoreAuthSystemState();

        Integer typeId = Integer.MAX_VALUE;

        // with typeId that does not exist
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", typeId.toString())
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString())
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships").doesNotExist());

        // with focus item that does not exist
        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                .param("relationshipLabel", "isAuthorOfPublication")
                .param("focusItem", UUID.randomUUID().toString())
                .param("relatedItem", author1.getID().toString(),
                                      author2.getID().toString(),
                                      author3.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.relationships").doesNotExist());
    }

    @Test
    public void findByItemsAndTypePaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                             entityTypeService.findByEntityType(context, "Person"),
                                             "isAuthorOfPublication", "isPublicationOfAuthor");

        Relationship relationship1 = RelationshipBuilder.createRelationshipBuilder(context, publication1, author3,
                                                         isAuthorOfPublicationRelationshipType)
                                                        .withLeftPlace(2)
                                                        .build();
        Relationship relationship2 = RelationshipBuilder.createRelationshipBuilder(context, publication1, author1,
                                                         isAuthorOfPublicationRelationshipType)
                                                        .withLeftPlace(2)
                                                        .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString())
                   .param("size", "1")
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", contains(
                              RelationshipMatcher.matchRelationshipValues(relationship1)
                              )))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString())
                   .param("page", "1")
                   .param("size", "1")
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", contains(
                              RelationshipMatcher.matchRelationshipValues(relationship2)
                              )))
                   .andExpect(jsonPath("$.page.number", is(1)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient().perform(get("/api/core/relationships/search/byItemsAndType")
                   .param("typeId", isAuthorOfPublicationRelationshipType.getID().toString())
                   .param("relationshipLabel", "isAuthorOfPublication")
                   .param("focusItem", publication1.getID().toString())
                   .param("page", "5")
                   .param("relatedItem", author1.getID().toString(),
                                         author2.getID().toString(),
                                         author3.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships").doesNotExist())
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.number", is(5)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findTheCreatedRelationshipTypeTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Relationship relationship = RelationshipBuilder
            .createRelationshipBuilder(context, author1, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        context.restoreAuthSystemState();

        Integer relationshipId = relationship.getID();
        getClient().perform(get("/api/core/relationships/" + relationshipId))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(relationship.getID())))
                   .andExpect(jsonPath("$._embedded.relationships").doesNotExist())
                   .andExpect(jsonPath("$._links.relationshipType.href",
                       containsString("/api/core/relationships/" + relationshipId + "/relationshipType"))
                   );

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/relationships/" + relationshipId + "/relationshipType"))
                             .andExpect(status().isOk());
    }

}
