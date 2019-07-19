/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.RelationshipBuilder;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class RelationshipRestRepositoryIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;


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

        context.restoreAuthSystemState();

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

        getClient().perform(get("/api/core/relationships").param("size", "2"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 2, 2, 3))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationship2)
                   )))
        ;

        getClient().perform(get("/api/core/relationships").param("size", "2").param("page", "1"))

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

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
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

        EPerson user = EPersonBuilder.createEPerson(context)
                                    .withNameInMetadata("first", "last")
                                    .withEmail("testaze@gmail.com")
                                    .withPassword(password)
                                    .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                    .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, publication, Constants.WRITE, user);

        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient().perform(get("/api/core/relationships/" + firstRelationshipIdString))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._links.leftItem.href",
                                       containsString(publication.getID().toString())))
                   .andExpect(jsonPath("$._links.rightItem.href",
                                       containsString(author1.getID().toString())));

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

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);
        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        getClient().perform(get("/api/core/relationships/" + firstRelationshipIdString))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._links.leftItem.href", containsString(publication.getID().toString())))
                   .andExpect(jsonPath("$._links.rightItem.href", containsString(author1.getID().toString())));
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

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isForbidden())
                                              .andReturn();

    }

    /**
     * This method will test the addition of a mixture of plain-text metadatavalues and relationships to then
     * verify that the place property is still being handled correctly.
     * @throws Exception
     */
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

        context.restoreAuthSystemState();
        // Here we create our first Relationship to the Publication to give it a dc.contributor.author virtual
        // metadata field.
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        // Here we call the relationship and verify that the relationship's leftplace is 0
        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        context.turnOffAuthorisationSystem();

        // Make sure we grab the latest instance of the Item from the database
        publication = itemService.find(context, publication.getID());
        // Add a plain text dc.contributor.author value
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication);

        List<MetadataValue> list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

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
        // Verfiy the leftPlace of our relationship is still 0.
        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        // Create another Relationship for the Publication, thus creating a third dc.contributor.author mdv
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author2.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        // Grab the ID for the second relationship for future calling in the rest
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);
        // Ensure that we now have three dc.contributor.author mdv ("Smith, Donald", "plain text", "Smith, Maria"
        // In that order which will be checked below the rest call
        assertEquals(3, list.size());
        // Perform the REST call to the relationship to ensure its leftPlace is 2 even though it's only the second
        // Relationship. Note that leftPlace 1 was skipped due to the dc.contributor.author plain text value and
        // This is expected behaviour and should be tested
        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
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
        publication = itemService.find(context, publication.getID());
        // Add a fourth dc.contributor.author mdv
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication);

        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

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
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author3.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        // Save the third Relationship's ID for future calling
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);
        // Assert that our dc.contributor.author mdv list is now of size 5
        assertEquals(5, list.size());
        // Assert that the third Relationship has leftPlace 4, even though 3 relationships were created.
        // This is because the plain text values 'occupy' place 1 and 3.
        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
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
        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);

        context.restoreAuthSystemState();

        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

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

        publication = itemService.find(context, publication.getID());
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text four");
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text five");
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text six");
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text seven");
        itemService.update(context, publication);

        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

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

        list = itemService.getMetadata(publication, "dc", "contributor", Item.ANY, Item.ANY);
        assertEquals(10, list.size()); //same size as authors
        list = itemService.getMetadata(publication, "dc", Item.ANY, Item.ANY, Item.ANY);
        assertEquals(16, list.size()); //also includes title, 4 date fields, uri
        list = itemService.getMetadata(publication, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        assertEquals(20, list.size()); //also includes type and 3 relation.isAuthorOfPublication values
    }

    /**
     * This method will test the deletion of a plain-text metadatavalue to then
     * verify that the place property is still being handled correctly.
     * @throws Exception
     */
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

        // First create the structure of 5 metadatavalues just like the additions test.
        context.restoreAuthSystemState();
        // This post request will add a first relationship to the publiction and thus create a first set of metadata
        // For the author values, namely "Donald Smith"
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        // This test will double check that the leftPlace for this relationship is indeed 0
        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        context.turnOffAuthorisationSystem();
        // We retrieve the publication again to ensure that we have the latest DB object of it
        publication = itemService.find(context, publication.getID());
        // Add a plain text metadatavalue to the publication
        // After this addition, the list of authors should like like "Donald Smith", "plain text"
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        List<MetadataValue> list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                // Ensure that this is indeed the second metadatavalue in the list of authors for the publication
                assertEquals(1, mdv.getPlace());
            }
        }

        // This test checks again that the first relationship is still on leftplace 0 and not altered
        // Because of the MetadataValue addition
        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        // Creates another Relationship for the Publication and thus adding a third metadata value for the author
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author2.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        // This test verifies that the newly created Relationship is on leftPlace 2, because the first relationship
        // is on leftPlace 0 and the plain text metadataValue occupies the place 1
        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));
        context.turnOffAuthorisationSystem();
        // Get the publication from the DB again to ensure that we have the latest object
        publication = itemService.find(context, publication.getID());
        // Add a fourth metadata value to the publication
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                // Ensure that this plain text metadata value is on the fourth place (place 3) for the publication
                assertEquals(3, mdv.getPlace());
            }
        }

        // The list should currently look like this: "Donald Smith", "plain text", "Maria Smith", "plain text two"

        // This creates a third relationship for the publication and thus adding a fifth value for author metadatavalues
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author3.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        // This verifies that the newly created third relationship is on leftPlace 4.
        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        context.turnOffAuthorisationSystem();
        publication = itemService.find(context, publication.getID());
        // Create another plain text metadata value on the publication
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text three")) {
                // Verify that this plain text value is indeed the 6th author in the list (place 5)
                assertEquals(5, mdv.getPlace());
            }
        }

        // Now we will have a dc.contributor.author metadatavalue list of size 6 in the following order:
        // "Smith, Donald", "plain text", "Smith, Maria", "plain text two", "Maybe, Maybe", "plain text three"
        List<MetadataValue> authors = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> listToRemove = new LinkedList<>();
        for (MetadataValue metadataValue : authors) {
            if (StringUtils.equals(metadataValue.getValue(), "plain text two")) {
                listToRemove.add(metadataValue);
            }
        }

        context.turnOffAuthorisationSystem();
        // Remove the "plain text two" metadatavalue. Ensure that all mdvs prior to that in the list are unchanged
        // And ensure that the ones coming after this mdv have its place lowered by one.
        itemService.removeMetadataValues(context, publication, listToRemove);

        itemService.update(context, publication);
        context.restoreAuthSystemState();
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

    /**
     * This method will test the deletion of a Relationship to then
     * verify that the place property is still being handled correctly.
     * @throws Exception
     */
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

        // First create the structure of 5 metadatavalues just like the additions test.
        context.restoreAuthSystemState();
        // This post request will add a first relationship to the publiction and thus create a first set of metadata
        // For the author values, namely "Donald Smith"
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));

        // This test will double check that the leftPlace for this relationship is indeed 0
        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));


        context.turnOffAuthorisationSystem();
        // We retrieve the publication again to ensure that we have the latest DB object of it
        publication = itemService.find(context, publication.getID());
        // Add a plain text metadatavalue to the publication
        // After this addition, the list of authors should like like "Donald Smith", "plain text"
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        List<MetadataValue> list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text")) {
                // Ensure that this is indeed the second metadatavalue in the list of authors for the publication
                assertEquals(1, mdv.getPlace());
            }
        }

        // This test checks again that the first relationship is still on leftplace 0 and not altered
        // Because of the MetadataValue addition
        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(0)));

        // Creates another Relationship for the Publication and thus adding a third metadata value for the author
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author2.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        // This test verifies that the newly created Relationship is on leftPlace 2, because the first relationship
        // is on leftPlace 0 and the plain text metadataValue occupies the place 1
        getClient(adminToken).perform(get("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(2)));
        context.turnOffAuthorisationSystem();
        // Get the publication from the DB again to ensure that we have the latest object
        publication = itemService.find(context, publication.getID());
        // Add a fourth metadata value to the publication
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text two");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        for (MetadataValue mdv : list) {
            if (StringUtils.equals(mdv.getValue(), "plain text two")) {
                // Ensure that this plain text metadata value is on the fourth place (place 3) for the publication
                assertEquals(3, mdv.getPlace());
            }
        }

        // The list should currently look like this: "Donald Smith", "plain text", "Maria Smith", "plain text two"

        // This creates a third relationship for the publication and thus adding a fifth value for author metadatavalues
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + author3.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        // This verifies that the newly created third relationship is on leftPlace 4.
        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("leftPlace", is(4)));

        context.turnOffAuthorisationSystem();
        publication = itemService.find(context, publication.getID());
        // Create another plain text metadata value on the publication
        itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "plain text three");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        list = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

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

    /**
     * This method will test the deletion of a Relationship and will then
     * verify that the relation is removed
     * @throws Exception
     */
    @Test
    public void deleteRelationship() throws Exception {
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

        // First create 1 relationship.
        context.restoreAuthSystemState();
        // This post request will add a first relationship to the publication and thus create a first set of metadata
        // For the author values, namely "Donald Smith"
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isAuthorOfPublicationRelationshipType.getID()
                                                                                                            .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                                        "https://localhost:8080/spring-rest/api/core/items/" + author1.getID()))
                                                   .andExpect(status().isCreated())
                                                   .andReturn();
        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String firstRelationshipIdString = String.valueOf(map.get("id"));


        // This test checks that there's one relationship on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                publication.getID() + "/relationships"))
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
        mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                      .param("relationshipType",
                                                             isAuthorOfPublicationRelationshipType.getID()
                                                                                                  .toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/spring-rest/api/core/items/" + publication.getID() + "\n" +
                                                              "https://localhost:8080/spring-rest/api/core/items/" + author2.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String secondRelationshipIdString = String.valueOf(map.get("id"));

        // This test checks that there are 2 relationships on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                publication.getID() + "/relationships"))
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
        getClient(adminToken).perform(delete("/api/core/relationships/" + firstRelationshipIdString));


        // This test checks that there's one relationship on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                publication.getID() + "/relationships"))
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
        getClient(adminToken).perform(delete("/api/core/relationships/" + secondRelationshipIdString));


        // This test checks that there's no relationship on the publication
        getClient(adminToken).perform(get("/api/core/items/" +
                publication.getID() + "/relationships"))
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
    }

    /**
     * This test will simply add Relationships between Items with a useForPlace attribute set to false for the
     * RelationshipType. We want to test that the Relationships that are created will still have their place
     * attributes handled in a correct way
     * @throws Exception
     */
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
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);

        // This is essentially a sequence of adding Relationships by POST and then checking with GET to see
        // if the place is being set properly.
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isOrgUnitOfPersonRelationshipType.getID()
                                                                                                        .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + author1.getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items/" + orgUnit1.getID()))
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
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author2.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1.getID()))
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
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author3.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1.getID()))
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

    /**
     * This test will simply add Relationships between Items with a useForPlace attribute set to false for the
     * RelationshipType. We want to test that the Relationships that are created will still have their place
     * attributes handled in a correct way. It will then delete a Relationship and once again ensure that the place
     * attributes are being handled correctly.
     * @throws Exception
     */
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
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);

        // This is essentially a sequence of adding Relationships by POST and then checking with GET to see
        // if the place is being set properly.
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/core/relationships")
                                                                .param("relationshipType",
                                                                       isOrgUnitOfPersonRelationshipType.getID()
                                                                                                        .toString())
                                                                .contentType(MediaType.parseMediaType
                                                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                         .TEXT_URI_LIST_VALUE))
                                                                .content(
                                                                    "https://localhost:8080/server/api/core/items/" + author1.getID() + "\n" +
                                                                        "https://localhost:8080/server/api/core/items/" + orgUnit1.getID()))
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
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author2.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1.getID()))
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
                                                      .param("relationshipType",
                                                             isOrgUnitOfPersonRelationshipType.getID().toString())
                                                      .contentType(MediaType.parseMediaType
                                                          (org.springframework.data.rest.webmvc.RestMediaTypes
                                                               .TEXT_URI_LIST_VALUE))
                                                      .content(
                                                          "https://localhost:8080/server/api/core/items/" + author3.getID() + "\n" +
                                                              "https://localhost:8080/server/api/core/items/" + orgUnit1.getID()))
                                         .andExpect(status().isCreated())
                                         .andReturn();
        mapper = new ObjectMapper();

        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String thirdRelationshipIdString = String.valueOf(map.get("id"));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(2)));

        // Here we will delete the secondRelationship and then verify that the others have their place handled properly
        getClient(adminToken).perform(delete("/api/core/relationships/" + secondRelationshipIdString))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/core/relationships/" + firstRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(0)));

        getClient(adminToken).perform(get("/api/core/relationships/" + thirdRelationshipIdString))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("rightPlace", is(1)));

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

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
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
                .withAuthor("Testy, TEst")
                .withIssueDate("2015-01-01")
                .withRelationshipType("Publication")
                .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType
                                                               (org.springframework.data.rest.webmvc.RestMediaTypes
                                                                    .TEXT_URI_LIST_VALUE))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        //Modify the left item in the relationship publication > publication 2
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/leftItem")
                .contentType(MediaType.parseMediaType
                        (org.springframework.data.rest.webmvc.RestMediaTypes
                                .TEXT_URI_LIST_VALUE))
                .content(
                        "https://localhost:8080/server/api/core/items/" + publication2.getID()))
                .andExpect(status().isOk())
                .andReturn();

        //verify left item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.leftItem.href",
                        containsString(publication2.getID().toString())))
                .andExpect(jsonPath("$._links.rightItem.href",
                        containsString(author1.getID().toString())));

        //Modify the right item in the relationship publication > publication 2
        MvcResult mvcResult3 = getClient(token).perform(put("/api/core/relationships/" + id + "/rightItem")
                .contentType(MediaType.parseMediaType
                        (org.springframework.data.rest.webmvc.RestMediaTypes
                                .TEXT_URI_LIST_VALUE))
                .content(
                                "https://localhost:8080/server/api/core/items/" + author2.getID()))
                .andExpect(status().isOk())
                .andReturn();

        //verify right item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.rightItem.href",
                                containsString(author2.getID().toString())))
                .andExpect(jsonPath("$._links.leftItem.href",
                                containsString(publication2.getID().toString())));

    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is possible for a user with WRITE permissions on author 1 and author 2
     */
    @Test
    public void putRelationshipWriteAccessOnAuthors() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
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



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user);

        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        //change right item from author 1 > author 2
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/rightItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + author2.getID()))
                .andExpect(status().isOk())
                .andReturn();
        //verify change  and other not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.rightItem.href",
                                containsString(author2.getID().toString())))
                        .andExpect(jsonPath("$._links.leftItem.href",
                                containsString(publication.getID().toString())));

    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is possible for a user with WRITE permissions on publication 1
     */
    @Test
    public void putRelationshipWriteAccessOnPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
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



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, publication, Constants.WRITE, user);

        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        //change rightItem from author1 > author2
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/rightItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + author2.getID()))
                .andExpect(status().isOk())
                .andReturn();
        //verify right item change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.rightItem.href",
                                containsString(author2.getID().toString())))
                        .andExpect(jsonPath("$._links.leftItem.href",
                                containsString(publication.getID().toString())));

    }


    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 2 and author 1
     * Verify this is possible for a user with WRITE permissions on publication 1 and publication 2
     */
    @Test
    public void putRelationshipWriteAccessOnPublications() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication1 = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();
        Item publication2 = ItemBuilder.createItem(context, col3)
                                      .withTitle("Publication2")
                                      .withAuthor("Testy, TEstzeaze")
                                      .withIssueDate("2015-01-01")
                                      .withRelationshipType("Publication")
                                      .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user);
        authorizeService.addPolicy(context, publication2, Constants.WRITE, user);

        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        //change leftItem
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/leftItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + publication2.getID()))
                .andExpect(status().isOk())
                .andReturn();
        //verify change  and other not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                containsString(publication2.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                containsString(author1.getID().toString())));
    }


    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 2 and author 1
     * Verify this is possible for a user with WRITE permissions on author 1
     */
    @Test
    public void putRelationshipWriteAccessOnAuthor() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication1 = ItemBuilder.createItem(context, col3)
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



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);

        context.restoreAuthSystemState();

        String token = getAuthToken(user.getEmail(), password);

        MvcResult mvcResult = getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/api/core/relationships")
                       .param("relationshipType",
                              isAuthorOfPublicationRelationshipType.getID()
                                                                   .toString())
                       .contentType(MediaType.parseMediaType("text/uri-list"))
                       .content(
                           "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                               "https://localhost:8080/server/api/core/items/" + author1.getID()))
          .andExpect(status().isCreated())
          .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        //change left item
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/leftItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + publication2.getID()))
                .andExpect(status().isOk())
                .andReturn();
        //verify change and other not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.leftItem.href",
                                containsString(publication2.getID().toString())))
                        .andExpect(jsonPath("$._links.rightItem.href",
                                containsString(author1.getID().toString())));
    }


    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is NOT possible for a user without WRITE permissions
     */
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



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        token = getAuthToken(user.getEmail(), password);
        //attempt change, expect not allowed
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/rightItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + author2.getID()))
                .andExpect(status().isForbidden())
                .andReturn();
        //verify nothing changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.leftItem.href",
                        containsString(publication.getID().toString())))
                .andExpect(jsonPath("$._links.rightItem.href",
                        containsString(author1.getID().toString())));
    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 1 and author 2
     * Verify this is NOT possible for a user with WRITE permissions on author 1
     */
    @Test
    public void putRelationshipOnlyAccessOnOneAuthor() throws Exception {

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



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user);

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        token = getAuthToken(user.getEmail(), password);
        //attempt right item change, expect not allowed
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/rightItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + author2.getID()))
                .andExpect(status().isForbidden())
                .andReturn();
        //verify not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.leftItem.href",
                        containsString(publication.getID().toString())))
                .andExpect(jsonPath("$._links.rightItem.href",
                        containsString(author1.getID().toString())));
    }

    /**
     * Create a relationship between publication 1 and author 1
     * Change it to a relationship between publication 2 and author 1
     * Verify this is NOT possible for a user with WRITE permissions on publication 1
     */
    @Test
    public void putRelationshipOnlyAccessOnOnePublication() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .withRelationshipType("Person")
                                  .build();

        Item publication1 = ItemBuilder.createItem(context, col3)
                                       .withTitle("Publication1")
                                       .withAuthor("Testy, TEst")
                                       .withIssueDate("2015-01-01")
                                       .withRelationshipType("Publication")
                                       .build();
        Item publication2 = ItemBuilder.createItem(context, col3)
                                       .withTitle("Publication2")
                                       .withAuthor("Testy, TEstzeaze")
                                       .withIssueDate("2015-01-01")
                                       .withRelationshipType("Publication")
                                       .build();

        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService
            .findbyTypesAndLabels(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"),
                                  "isAuthorOfPublication", "isPublicationOfAuthor");



        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withNameInMetadata("first", "last")
                                     .withEmail("testaze@gmail.com")
                                     .withPassword(password)
                                     .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                     .build();
        context.setCurrentUser(user);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user);

        String token = getAuthToken(user.getEmail(), password);

        context.restoreAuthSystemState();

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                                                           .param("relationshipType",
                                                                  isAuthorOfPublicationRelationshipType.getID()
                                                                                                       .toString())
                                                           .contentType(MediaType.parseMediaType("text/uri-list"))
                                                           .content(
                                                               "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                                                                   "https://localhost:8080/server/api/core/items/" + author1.getID()))
                                              .andExpect(status().isCreated())
                                              .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));
        //attempt left item change, expect not allowed
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/leftItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + publication2.getID()))
                .andExpect(status().isForbidden())
                .andReturn();
        //verify not changed
        getClient(token).perform(get("/api/core/relationships/" + id))
                        .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.leftItem.href",
                        containsString(publication1.getID().toString())))
                .andExpect(jsonPath("$._links.rightItem.href",
                        containsString(author1.getID().toString())));

    }

    @Test
    public void findRelationshipByLabelTest() throws Exception {

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

        // We're creating a Relationship of type isOrgUnitOfPerson between an author and an orgunit
        Relationship relationship1 = RelationshipBuilder
            .createRelationshipBuilder(context, author1, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        // We're creating a Relationship of type isOrgUnitOfPerson between a different author and the same orgunit
        Relationship relationshipAuthorExtra = RelationshipBuilder
            .createRelationshipBuilder(context, author2, orgUnit1, isOrgUnitOfPersonRelationshipType).build();

        // We're creating a Relationship of type isOrgUnitOfProject between a project and an orgunit
        Relationship relationship2 = RelationshipBuilder
            .createRelationshipBuilder(context, project1, orgUnit1, isOrgUnitOfProjectRelationshipType).build();

        // We're creating a Relationship of type isAuthorOfPublication between a publication and an author
        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication, author1, isAuthorOfPublicationRelationshipType).build();

        // Perform a GET request to the searchByLabel endpoint, asking for Relationships of type isOrgUnitOfPerson
        // With an extra parameter namely DSO which resolves to the author(Smith, Donald) in the first
        // relationship created.
        // As that first relationship is the only one created with the given author(Smith, Donald) that holds the
        // RelationshipType "isOrgUnitOfPerson", that Relationship should be the only one returned.
        // This is what we're checking for
        getClient().perform(get("/api/core/relationships/search/byLabel")
                    .param("label", "isOrgUnitOfPerson")
                    .param("dso", author1.getID().toString()))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1))))
                   .andExpect(jsonPath("$._embedded.relationships", hasItem(
                       RelationshipMatcher.matchRelationship(relationship1)
                   )))
        ;

        // Perform a GET request to the searchByLabel endpoint, asking for Relationships of type isOrgUnitOfPerson
        // We do not specificy a DSO param, which means ALL relationships of type isOrgUnitOfPerson should be returned
        // Which is what we're checking for, both the first relationship and the one with a different author
        // should be returned
        getClient().perform(get("/api/core/relationships/search/byLabel")
                                .param("label", "isOrgUnitOfPerson"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))))
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationshipAuthorExtra)
                   )))
        ;
    }

    @Test
    public void putRelationshipWithNonexistentID() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Item publication1 = ItemBuilder.createItem(context, col1)
                .withTitle("Publication1")
                .withAuthor("Testy, TEst")
                .withIssueDate("2015-01-01")
                .withRelationshipType("Publication")
                .build();

        String token = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        int nonexistentRelationshipID = 404404404;
        //attempt left item change on non-existent relationship
        MvcResult mvcResult = getClient(token).perform(
                put("/api/core/relationships/" + nonexistentRelationshipID + "/leftItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + publication1.getID()))
                .andExpect(status().isNotFound())
                .andReturn();

    }

    @Test
    public void putRelationshipWithInvalidItemIDInBody() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Item author1 = ItemBuilder.createItem(context, col1)
                .withTitle("Author1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withRelationshipType("Person")
                .build();
        Item publication1 = ItemBuilder.createItem(context, col1)
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

        context.restoreAuthSystemState();

        MvcResult mvcResult = getClient(token).perform(post("/api/core/relationships")
                .param("relationshipType",
                        isAuthorOfPublicationRelationshipType.getID()
                                .toString())
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content(
                        "https://localhost:8080/server/api/core/items/" + publication1.getID() + "\n" +
                                "https://localhost:8080/server/api/core/items/" + author1.getID()))
                .andExpect(status().isCreated())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String id = String.valueOf(map.get("id"));

        int nonexistentItemID = 404404404;
        //attempt left item change on non-existent relationship
        MvcResult mvcResult2 = getClient(token).perform(put("/api/core/relationships/" + id + "/leftItem")
                .contentType(MediaType.parseMediaType("text/uri-list"))
                .content("https://localhost:8080/server/api/core/items/" + nonexistentItemID))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

    }
}
