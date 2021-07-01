/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.dspace.app.rest.projection.CheckSideItemInRelationshipProjection.PARAM_NAME;
import static org.dspace.app.rest.projection.CheckSideItemInRelationshipProjection.PROJECTION_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckSideItemInRelationshipProjectionIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    protected Community community1;

    protected Collection collection1;

    protected Item author1;
    protected Item author2;
    protected Item author3;

    protected Item publication1;
    protected Item publication2;

    protected Item project1;

    protected RelationshipType isAuthorOfPublicationRelationshipType;
    protected RelationshipType isProjectOfPublicationRelationshipType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        community1 = CommunityBuilder.createCommunity(context)
            .withName("Community 1")
            .build();

        collection1 = CollectionBuilder.createCollection(context, community1)
            .withName("Collection 1")
            .build();

        author1 = ItemBuilder.createItem(context, collection1)
            .withTitle("Author 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withPersonIdentifierLastName("Smith")
            .withPersonIdentifierFirstName("Donald")
            .withEntityType("Person")
            .build();

        author2 = ItemBuilder.createItem(context, collection1)
            .withTitle("Author 2")
            .withIssueDate("2016-02-13")
            .withAuthor("Smith, Maria")
            .withEntityType("Person")
            .build();

        author3 = ItemBuilder.createItem(context, collection1)
            .withTitle("Author 3")
            .withIssueDate("2016-02-13")
            .withPersonIdentifierFirstName("Maybe")
            .withPersonIdentifierLastName("Maybe")
            .withEntityType("Person")
            .build();

        publication1 = ItemBuilder.createItem(context, collection1)
            .withTitle("Publication 1")
            .withAuthor("Testy, TEst")
            .withIssueDate("2015-01-01")
            .withEntityType("Publication")
            .build();

        publication2 = ItemBuilder.createItem(context, collection1)
            .withTitle("Publication 2")
            .withAuthor("Testy, TEst")
            .withIssueDate("2015-01-01")
            .withEntityType("Publication")
            .build();

        project1 = ItemBuilder.createItem(context, collection1)
            .withTitle("Project 1")
            .withAuthor("Testy, TEst")
            .withIssueDate("2015-01-01")
            .withEntityType("Project")
            .build();

        isAuthorOfPublicationRelationshipType = relationshipTypeService.findbyTypesAndTypeName(
            context, entityTypeService.findByEntityType(context, "Publication"),
            entityTypeService.findByEntityType(context, "Person"),
            "isAuthorOfPublication", "isPublicationOfAuthor"
        );

        isProjectOfPublicationRelationshipType = relationshipTypeService.findbyTypesAndTypeName(
            context, entityTypeService.findByEntityType(context, "Publication"),
            entityTypeService.findByEntityType(context, "Project"),
            "isProjectOfPublication", "isPublicationOfProject"
        );

        context.restoreAuthSystemState();
    }

    protected Relationship createRelationship(Item leftItem, Item rightItem, RelationshipType relationshipType) {
        context.turnOffAuthorisationSystem();

        Relationship relationship = RelationshipBuilder.createRelationshipBuilder(
            context, leftItem, rightItem, relationshipType
        ).build();

        context.restoreAuthSystemState();

        return relationship;
    }

    @Test
    public void testPropertiesNotPresentWhenProjectionInactive() throws Exception {
        Relationship relationship = createRelationship(publication1, author1, isAuthorOfPublicationRelationshipType);

        getClient().perform(
            // NOTE: projection is not requested
            get("/api/core/relationships/{relationship-id}", relationship.getID())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItemLeft").doesNotExist())
            .andExpect(jsonPath("$.relatedItemRight").doesNotExist());
    }

    @Test
    public void testPropertiesPresentWhenProjectionActive() throws Exception {
        Relationship relationship = createRelationship(publication1, author1, isAuthorOfPublicationRelationshipType);

        getClient().perform(
            // NOTE: projection is requested without uuids
            get("/api/core/relationships/{relationship-id}", relationship.getID())
                .param("projection", PROJECTION_NAME)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItemLeft", Matchers.is(false)))
            .andExpect(jsonPath("$.relatedItemRight", Matchers.is(false)));
    }

    @Test
    public void testCheckRightItem() throws Exception {
        Relationship relationship = createRelationship(publication1, author1, isAuthorOfPublicationRelationshipType);

        getClient().perform(
            // NOTE: projection is requested with the uuid of author 1
            get("/api/core/relationships/{relationship-id}", relationship.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, author1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItemLeft", Matchers.is(false)))
            .andExpect(jsonPath("$.relatedItemRight", Matchers.is(true)));
    }

    @Test
    public void testCheckLeftItem() throws Exception {
        Relationship relationship = createRelationship(publication1, author1, isAuthorOfPublicationRelationshipType);

        getClient().perform(
            // NOTE: projection is requested with the uuid of publication 1
            get("/api/core/relationships/{relationship-id}", relationship.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, publication1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItemLeft", Matchers.is(true)))
            .andExpect(jsonPath("$.relatedItemRight", Matchers.is(false)));
    }

    @Test
    public void testCheckMultipleUuidsShouldReturnBadRequest() throws Exception {
        Relationship relationship = createRelationship(publication1, author1, isAuthorOfPublicationRelationshipType);

        getClient().perform(
            // NOTE: projection is requested with 2 uuids
            get("/api/core/relationships/{relationship-id}", relationship.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, "8c171da5-7e2b-4b20-8c82-1935f3b00e57")
                .param(PARAM_NAME, "f8ecadba-216d-4ed1-a6ac-721d68e458ef")
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testAllRelationshipsOfItem() throws Exception {
        Relationship relationship1 = createRelationship(publication1, author1, isAuthorOfPublicationRelationshipType);
        Relationship relationship2 = createRelationship(publication1, author2, isAuthorOfPublicationRelationshipType);
        Relationship relationship3 = createRelationship(publication1, project1, isProjectOfPublicationRelationshipType);
        Relationship relationship4 = createRelationship(publication2, author2, isAuthorOfPublicationRelationshipType);
        Relationship relationship5 = createRelationship(publication2, author3, isAuthorOfPublicationRelationshipType);

        // request the relationships of publication 1 and do not enable the projection
        getClient().perform(
            get("/api/core/items/{item-uuid}/relationships", publication1.getID())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.relationships", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship1.getID())),
                    hasNoJsonPath("$.relatedItemLeft"),
                    hasNoJsonPath("$.relatedItemRight")
                ),
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship2.getID())),
                    hasNoJsonPath("$.relatedItemLeft"),
                    hasNoJsonPath("$.relatedItemRight")
                ),
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship3.getID())),
                    hasNoJsonPath("$.relatedItemLeft"),
                    hasNoJsonPath("$.relatedItemRight")
                )
            )));

        // request the relationships of publication 1 and and request the projection for author 1
        getClient().perform(
            get("/api/core/items/{item-uuid}/relationships", publication1.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, author1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.relationships", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship1.getID())),
                    hasJsonPath("$.relatedItemLeft", Matchers.is(false)),
                    hasJsonPath("$.relatedItemRight", Matchers.is(true)) // publication 1 + author 1
                ),
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship2.getID())),
                    hasJsonPath("$.relatedItemLeft", Matchers.is(false)),
                    hasJsonPath("$.relatedItemRight", Matchers.is(false))
                ),
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship3.getID())),
                    hasJsonPath("$.relatedItemLeft", Matchers.is(false)),
                    hasJsonPath("$.relatedItemRight", Matchers.is(false))
                )
            )));

        // request the relationships of publication 1 and and request the projection for publication 1
        getClient().perform(
            get("/api/core/items/{item-uuid}/relationships", publication1.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, publication1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.relationships", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship1.getID())),
                    hasJsonPath("$.relatedItemLeft", Matchers.is(true)), // publication 1 + publication 1
                    hasJsonPath("$.relatedItemRight", Matchers.is(false))
                ),
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship2.getID())),
                    hasJsonPath("$.relatedItemLeft", Matchers.is(true)), // publication 1 + publication 1
                    hasJsonPath("$.relatedItemRight", Matchers.is(false))
                ),
                Matchers.allOf(
                    hasJsonPath("$.id", Matchers.is(relationship3.getID())),
                    hasJsonPath("$.relatedItemLeft", Matchers.is(true)), // publication 1 + publication 1
                    hasJsonPath("$.relatedItemRight", Matchers.is(false))
                )
            )));
    }

}
