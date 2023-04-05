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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.matcher.RelationshipTypeMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipTypeRestControllerIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Test
    public void findAllEntityTypes() throws Exception {

        getClient().perform(get("/api/core/entitytypes"))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 8))))
                   // Expect it to return these specific Entity Types (in any order)
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("Publication"),
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("Person"),
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("Project"),
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("OrgUnit"),
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("Journal"),
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("JournalVolume"),
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("JournalIssue"),
                       // None is the "empty" entity type used for allowing Collections / External Sources to work with
                       // non-Entities (i.e. normal items)
                       EntityTypeMatcher.matchEntityTypeEntryForLabel("none")
                   )))
        ;
    }

    @Test
    public void findAllRelationshipTypesForPublications() throws Exception {

        EntityType publicationEntityType = entityTypeService.findByEntityType(context, "Publication");
        EntityType personEntityType = entityTypeService.findByEntityType(context, "Person");
        EntityType projectEntityType = entityTypeService.findByEntityType(context, "Project");
        EntityType orgunitEntityType = entityTypeService.findByEntityType(context, "OrgUnit");
        EntityType journalIssueEntityType = entityTypeService.findByEntityType(context, "journalIssue");

        RelationshipType relationshipType1 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, personEntityType, "isAuthorOfPublication",
                                  "isPublicationOfAuthor");
        RelationshipType relationshipType2 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, projectEntityType, "isProjectOfPublication",
                                  "isPublicationOfProject");
        RelationshipType relationshipType3 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, orgunitEntityType, "isOrgUnitOfPublication",
                                  "isPublicationOfOrgUnit");
        RelationshipType relationshipType4 = relationshipTypeService
            .findbyTypesAndTypeName(context, journalIssueEntityType, publicationEntityType,
                    "isPublicationOfJournalIssue", "isJournalIssueOfPublication");
        RelationshipType relationshipType5 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, orgunitEntityType, "isAuthorOfPublication",
                                  "isPublicationOfAuthor");
        getClient().perform(get("/api/core/entitytypes/" + publicationEntityType.getID() + "/relationshiptypes"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType1),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType2),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType3),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType4),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType5)
                       )))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(5)))
                   .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findAllRelationshipTypesEmptyResponseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType testEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "TestEntityType").build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/entitytypes/" + testEntityType.getID() + "/relationshiptypes"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes").isEmpty())
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findAllRelationshipTypesForPublicationsPaginationTest() throws Exception {

        EntityType person = entityTypeService.findByEntityType(context, "Person");
        EntityType orgunit = entityTypeService.findByEntityType(context, "OrgUnit");
        EntityType project = entityTypeService.findByEntityType(context, "Project");
        EntityType publication = entityTypeService.findByEntityType(context, "Publication");
        EntityType journalIssue = entityTypeService.findByEntityType(context, "journalIssue");

        RelationshipType relationshipType1 = relationshipTypeService.findbyTypesAndTypeName(context,
                             publication, person, "isAuthorOfPublication", "isPublicationOfAuthor");
        RelationshipType relationshipType2 = relationshipTypeService.findbyTypesAndTypeName(context,
                          publication, project, "isProjectOfPublication", "isPublicationOfProject");
        RelationshipType relationshipType3 = relationshipTypeService.findbyTypesAndTypeName(context,
                          publication, orgunit, "isOrgUnitOfPublication", "isPublicationOfOrgUnit");
        RelationshipType relationshipType4 = relationshipTypeService.findbyTypesAndTypeName(context,
           journalIssue, publication, "isPublicationOfJournalIssue", "isJournalIssueOfPublication");
        RelationshipType relationshipType5 = relationshipTypeService.findbyTypesAndTypeName(context,
                             publication, orgunit, "isAuthorOfPublication","isPublicationOfAuthor");

        getClient().perform(get("/api/core/entitytypes/" + publication.getID() + "/relationshiptypes")
                   .param("size", "2"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType1),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType2)
                       )))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(5)))
                   .andExpect(jsonPath("$.page.number", is(0)));

        getClient().perform(get("/api/core/entitytypes/" + publication.getID() + "/relationshiptypes")
                   .param("size", "2")
                   .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType3),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType5)
                       )))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(5)))
                   .andExpect(jsonPath("$.page.number", is(1)));

        getClient().perform(get("/api/core/entitytypes/" + publication.getID() + "/relationshiptypes")
                   .param("size", "2")
                   .param("page", "2"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", contains(
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType4)
                       )))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(5)))
                   .andExpect(jsonPath("$.page.number", is(2)));
    }

    @Test
    public void findAllRelationshipTypesForPublicationsEmbedTest() throws Exception {

        EntityType publicationEntityType = entityTypeService.findByEntityType(context, "Publication");
        EntityType personEntityType = entityTypeService.findByEntityType(context, "Person");
        EntityType projectEntityType = entityTypeService.findByEntityType(context, "Project");
        EntityType orgunitEntityType = entityTypeService.findByEntityType(context, "OrgUnit");
        EntityType journalIssueEntityType = entityTypeService.findByEntityType(context, "journalIssue");

        RelationshipType relationshipType1 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, personEntityType, "isAuthorOfPublication",
                                  "isPublicationOfAuthor");
        RelationshipType relationshipType2 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, projectEntityType, "isProjectOfPublication",
                                  "isPublicationOfProject");
        RelationshipType relationshipType3 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, orgunitEntityType, "isOrgUnitOfPublication",
                                  "isPublicationOfOrgUnit");
        RelationshipType relationshipType4 = relationshipTypeService
            .findbyTypesAndTypeName(context, journalIssueEntityType, publicationEntityType,
                    "isPublicationOfJournalIssue", "isJournalIssueOfPublication");
        RelationshipType relationshipType5 = relationshipTypeService
            .findbyTypesAndTypeName(context, publicationEntityType, orgunitEntityType, "isAuthorOfPublication",
                                  "isPublicationOfAuthor");

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/relationships?embed=relationshipType"))
                             .andExpect(status().isOk());
    }

    @Test
    public void findAllRelationshipTypesNotFoundTest() throws Exception {
        getClient().perform(get("/api/core/entitytypes/" + Integer.MAX_VALUE + "/relationshiptypes"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void createAndFindRelationships() throws Exception {

        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withEntityType("Person").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                                           .withEntityType("Person").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits")
                                           .withEntityType("OrgUnit").build();
        Collection col4 = CollectionBuilder.createCollection(context, child1).withName("Publications")
                                           .withEntityType("Publication").build();
        Collection col5 = CollectionBuilder.createCollection(context, child1).withName("without entityType")
                                           .build();

        Item author1 = ItemBuilder.createItem(context, col1)
                                  .withTitle("Author1")
                                  .withIssueDate("2017-10-17")
                                  .withAuthor("Smith, Donald")
                                  .build();

        Item author2 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author2")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Smith, Maria")
                                  .build();

        Item author3 = ItemBuilder.createItem(context, col2)
                                  .withTitle("Author3")
                                  .withIssueDate("2016-02-13")
                                  .withAuthor("Maybe, Maybe")
                                  .build();

        Item orgUnit1 = ItemBuilder.createItem(context, col3)
                                   .withTitle("OrgUnit1")
                                   .withAuthor("Testy, TEst")
                                   .withIssueDate("2015-01-01")
                                   .build();

        Item project1 = ItemBuilder.createItem(context, col5)
                                   .withTitle("Project1")
                                   .withAuthor("Testy, TEst")
                                   .withIssueDate("2015-01-01")
                                   .build();

        Item publication = ItemBuilder.createItem(context, col4)
                                      .withTitle("Publication1")
                                      .withAuthor("Testy, TEst")
                                      .withIssueDate("2015-01-01")
                                      .build();

        Item publication2 = ItemBuilder.createItem(context, col4)
                                       .withTitle("Publication2")
                                       .withIssueDate("2015-01-01")
                                       .build();

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
            .createRelationshipBuilder(context, publication, author1, isAuthorOfPublicationRelationshipType).build();
        Relationship relationship2 = RelationshipBuilder
            .createRelationshipBuilder(context, publication, author2, isAuthorOfPublicationRelationshipType).build();
        Relationship relationship3 = RelationshipBuilder
            .createRelationshipBuilder(context, publication2, author2, isAuthorOfPublicationRelationshipType).build();
        Relationship relationship4 = RelationshipBuilder
            .createRelationshipBuilder(context, publication2, author3, isAuthorOfPublicationRelationshipType).build();

        context.restoreAuthSystemState();
        //verify results
        getClient().perform(get("/api/core/relationships/search/byLabel?label=isAuthorOfPublication")
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationship2),
                       RelationshipMatcher.matchRelationship(relationship3),
                       RelationshipMatcher.matchRelationship(relationship4)
                   )));

        //verify paging
        getClient().perform(get("/api/core/relationships/search/byLabel?label=isAuthorOfPublication&size=2"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(4)));

        //verify results
        getClient().perform(get("/api/core/relationships/search/byLabel?label=isAuthorOfPublication&dso="
                                    + publication.getID())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship1),
                       RelationshipMatcher.matchRelationship(relationship2)
                   )));

        //verify paging
        getClient().perform(get("/api/core/relationships/search/byLabel?label=isAuthorOfPublication&dso="
                                    + publication.getID() + "&size=1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(2)));

        //verify results
        getClient().perform(get("/api/core/relationships/search/byLabel?label=isAuthorOfPublication&dso="
                                    + publication2.getID())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationships", containsInAnyOrder(
                       RelationshipMatcher.matchRelationship(relationship3),
                       RelationshipMatcher.matchRelationship(relationship4)
                   )));

        //verify paging
        getClient().perform(get("/api/core/relationships/search/byLabel?label=isAuthorOfPublication&dso="
                                    + publication2.getID() + "&size=1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(2)));

    }
}
