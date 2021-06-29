/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.projection.CheckRelatedItemProjection.RELATIONSHIP_UUID_SEPARATOR;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckRelatedItemProjectionIT extends AbstractEntityIntegrationTest {

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

    @Override
    @Before
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

        context.restoreAuthSystemState();
    }

    @Test
    public void testSingleRelationship() throws Exception {
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService.findbyTypesAndTypeName(
            context, entityTypeService.findByEntityType(context, "Publication"),
            entityTypeService.findByEntityType(context, "Person"),
            "isAuthorOfPublication", "isPublicationOfAuthor"
        );

        // add a relationship between publication 1 and author 1
        context.turnOffAuthorisationSystem();
        RelationshipBuilder.createRelationshipBuilder(
            context, publication1, author1, isAuthorOfPublicationRelationshipType
        ).build();
        context.restoreAuthSystemState();

        // get publication 1 => property relatedItems should not exist because the projection was not requested
        getClient().perform(get("/api/core/items/{item-uuid}", publication1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").doesNotExist());

        // get publication 2 => property relatedItems should not exist because the projection was not requested
        getClient().perform(get("/api/core/items/{item-uuid}", publication2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").doesNotExist());

        // get author 1 => property relatedItems should not exist because the projection was not requested
        getClient().perform(get("/api/core/items/{item-uuid}", author1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").doesNotExist());

        // get publication 1 with projection => property relatedItems should be empty because no uuids to check
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication1.getID().toString())
                .param("projection", "CheckRelatedItem")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get publication 2 with projection => property relatedItems should be empty because no uuids to check
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication2.getID().toString())
                .param("projection", "CheckRelatedItem")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get author 1 with projection => property relatedItems should be empty because no uuids to check
        getClient().perform(
            get("/api/core/items/{item-uuid}", author1.getID().toString())
                .param("projection", "CheckRelatedItem")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get publication 1 with projection and check author 1 -> property relatedItems contains uuid of author 1
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems", Matchers.contains(
                Matchers.is(author1.getID().toString())
            )));

        // get publication 2 with projection and check author 1 -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication2.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get author 1 with projection and check author 1 -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", author1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get publication 1 with projection and check publication 1 -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", publication1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get publication 2 with projection and check publication 1 -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication2.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", publication1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get author 1 with projection and check publication 1 -> property relatedItems contains uuid of publication 1
        getClient().perform(
            get("/api/core/items/{item-uuid}", author1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", publication1.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems", Matchers.contains(
                Matchers.is(publication1.getID().toString())
            )));

        // get publication 1 with projection and check isPublicationOfAuthor author 1
        // -> property relatedItems contains uuid of author 1
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + author1.getID().toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems", Matchers.contains(
                Matchers.is(author1.getID().toString())
            )));

        // get publication 2 with projection and check isPublicationOfAuthor author 1
        // -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication2.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + author1.getID().toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get author 1 with projection and check isAuthorOfPublication publication 1
        // -> property relatedItems contains uuid of publication 1
        getClient().perform(
            get("/api/core/items/{item-uuid}", author1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isAuthorOfPublication" + RELATIONSHIP_UUID_SEPARATOR + publication1.getID().toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems", Matchers.contains(
                Matchers.is(publication1.getID().toString())
            )));

        // get publication 1 with projection and check isAuthorOfPublication author 1
        // -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", publication1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isAuthorOfPublication" + RELATIONSHIP_UUID_SEPARATOR + author1.getID().toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());

        // get author 1 with projection and check isPublicationOfAuthor publication 1
        // -> property relatedItems should be empty
        getClient().perform(
            get("/api/core/items/{item-uuid}", author1.getID().toString())
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + publication1.getID().toString()
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedItems").isEmpty());
    }

    @Test
    public void testMultipleRelationships() throws Exception {
        RelationshipType isAuthorOfPublicationRelationshipType = relationshipTypeService.findbyTypesAndTypeName(
            context, entityTypeService.findByEntityType(context, "Publication"),
            entityTypeService.findByEntityType(context, "Person"),
            "isAuthorOfPublication", "isPublicationOfAuthor"
        );

        RelationshipType isProjectOfPublicationRelationshipType = relationshipTypeService.findbyTypesAndTypeName(
            context, entityTypeService.findByEntityType(context, "Publication"),
            entityTypeService.findByEntityType(context, "Project"),
            "isProjectOfPublication", "isPublicationOfProject"
        );

        context.turnOffAuthorisationSystem();

        // add a relationship between publication 1 and author 1
        RelationshipBuilder.createRelationshipBuilder(
            context, publication1, author1, isAuthorOfPublicationRelationshipType
        ).build();

        // add a relationship between publication 1 and author 2
        RelationshipBuilder.createRelationshipBuilder(
            context, publication1, author2, isAuthorOfPublicationRelationshipType
        ).build();

        // add a relationship between publication 1 and project 1
        RelationshipBuilder.createRelationshipBuilder(
            context, publication1, project1, isProjectOfPublicationRelationshipType
        ).build();

        // add a relationship between publication 2 and author 2
        RelationshipBuilder.createRelationshipBuilder(
            context, publication2, author2, isAuthorOfPublicationRelationshipType
        ).build();

        // add a relationship between publication 2 and author 3
        RelationshipBuilder.createRelationshipBuilder(
            context, publication2, author3, isAuthorOfPublicationRelationshipType
        ).build();

        context.restoreAuthSystemState();

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author1.getID().toString())
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author1.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.empty())
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + author1.getID().toString()
                )
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author1.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.empty())
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author2.getID().toString())
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + author2.getID().toString()
                )
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author2.getID().toString())
                .param("checkRelatedItem", author3.getID().toString())
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString()),
                        Matchers.is(author3.getID().toString())
                    ))
                ))
            )));

        // search people with projection -> 3 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Person,equals")
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", publication1.getID().toString())
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(author1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(publication1.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(author2),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(publication1.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(author3),
                    hasJsonPath("$.relatedItems", Matchers.empty())
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param("checkRelatedItem", author2.getID().toString())
                .param("checkRelatedItem", project1.getID().toString())
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString()),
                        Matchers.is(project1.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + author2.getID().toString()
                )
                .param(
                    "checkRelatedItem",
                    "isPublicationOfProject" + RELATIONSHIP_UUID_SEPARATOR + project1.getID().toString()
                )
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString()),
                        Matchers.is(project1.getID().toString())
                    ))
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.contains(
                        Matchers.is(author2.getID().toString())
                    ))
                ))
            )));

        // search publications with projection -> 2 items with property relatedItems
        getClient().perform(
            get("/api/discover/search/objects")
                .param("f.entityType", "Publication,equals")
                .param("projection", "CheckRelatedItem")
                .param(
                    "checkRelatedItem",
                    "isPublicationOfProject" + RELATIONSHIP_UUID_SEPARATOR + author2.getID().toString()
                )
                .param(
                    "checkRelatedItem",
                    "isPublicationOfAuthor" + RELATIONSHIP_UUID_SEPARATOR + project1.getID().toString()
                )
        )
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication1),
                    hasJsonPath("$.relatedItems", Matchers.empty())
                )),
                hasJsonPath("$._embedded.indexableObject", Matchers.allOf(
                    ItemMatcher.matchItemProperties(publication2),
                    hasJsonPath("$.relatedItems", Matchers.empty())
                ))
            )));
    }

}
