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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.RelationshipBuilder;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Ignore
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
    private EPersonService ePersonService;

    @Autowired
    private AuthorizeService authorizeService;

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
    public void createRelationshipWriteAccessLeftItem() throws Exception {

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

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("testaze@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, publication, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                               "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

    }

    @Test
    public void createRelationshipWriteAccessRightItem() throws Exception {

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

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("testazhfhdfhe@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
            "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
            "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

    }


    @Test
    public void createRelationshipNoWriteAccess() throws Exception {

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

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("testazeazeazezae@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                               "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isForbidden())
                                              .andReturn();

    }

    @Test
    public void putRelationshipAdminAccess() throws Exception {

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

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2017-10-12")
                                  .withAuthor("Smith, Donalaze")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                               "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id)
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                               "https://localhost:8080/spring-rest/api/core/items/" + author2.getID()))
                                              .andExpect(status().isOk())
                                              .andReturn();

        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.rightId", is(author2.getID().toString())));

    }

    @Test
    public void putRelationshipRightItemWriteAccess() throws Exception {

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

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2017-10-12")
                                  .withAuthor("Smith, Donalaze")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("rrarz@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id)
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                    "https://localhost:8080/spring-rest/api/core/items/" + author2.getID()))
                                               .andExpect(status().isOk())
                                               .andReturn();

        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.rightId", is(author2.getID().toString())));

    }

    @Test
    public void putRelationshipNewRightItemWriteAccess() throws Exception {

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

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2017-10-12")
                                  .withAuthor("Smith, Donalaze")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("uiytirthery@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id)
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                    "https://localhost:8080/spring-rest/api/core/items/" + author2.getID()))
                                               .andExpect(status().isOk())
                                               .andReturn();

        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.rightId", is(author2.getID().toString())));

    }


    @Test
    public void putRelationshipLeftItemWriteAccess() throws Exception {

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

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2017-10-12")
                                  .withAuthor("Smith, Donalaze")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("tturturu@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, publication, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id)
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                    "https://localhost:8080/spring-rest/api/core/items/" + author2.getID()))
                                               .andExpect(status().isOk())
                                               .andReturn();

        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.rightId", is(author2.getID().toString())));

    }

    @Test
    public void putRelationshipNewLeftItemWriteAccess() throws Exception {

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

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2017-10-12")
                                  .withAuthor("Smith, Donalaze")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        Item publication2 = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication2")
                                      .withAuthor("Testy, TEstzea")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("tryhrtureery@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);
        authorizeService.addPolicy(context, publication2, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/api/core/relationships")
                       .param("relationshipType",
                              isAuthorOfPublicationRelationshipType.getID()
                                                                   .toString())
                       .contentType(MediaType.parseMediaType("text/uri-list"))
                       .content(
                           "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                               "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
          .andExpect(status().isCreated())
          .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id)
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/spring-rest/api/core/items/" + publication2.getID() + "\n" +
                                                                    "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                               .andExpect(status().isOk())
                                               .andReturn();

        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.leftId", is(publication2.getID().toString())));

    }


    @Test
    public void putRelationshipNoAccess() throws Exception {

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

        Item author2 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author2")
                                  .withIssueDate("2017-10-12")
                                  .withAuthor("Smith, Donalaze")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = ePersonService.create(context);
        user.setFirstName(context, "first");
        user.setLastName(context, "last");
        user.setEmail("ytureye@email.com");
        user.setCanLogIn(true);
        user.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
        ePersonService.setPassword(user, password);
        // actually save the eperson to unit testing DB
        ePersonService.update(context, user);
        context.setCurrentUser(user);


        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id)
                                                            .contentType(MediaType.parseMediaType("text/uri-list"))
                                                            .content(
                                                                "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                    "https://localhost:8080/spring-rest/api/core/items/" + author2.getID()))
                                               .andExpect(status().isForbidden())
                                               .andReturn();

    }
}
