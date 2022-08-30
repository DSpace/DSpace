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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.RelationshipType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * This class carries out the same test cases as {@link RelationshipRestRepositoryIT}.
 * The only difference being that a RelationshipType is set on
 * {@link RelationshipRestRepositoryIT#isAuthorOfPublicationRelationshipType}.
 */
public class LeftTiltedRelationshipRestRepositoryIT extends RelationshipRestRepositoryIT {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        isAuthorOfPublicationRelationshipType.setTilted(RelationshipType.Tilted.LEFT);
        relationshipTypeService.update(context, isAuthorOfPublicationRelationshipType);

        context.restoreAuthSystemState();
    }

    @Override
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
            .andExpect(
                jsonPath(
                    String.format("$.metadata['%s.isPublicationOfAuthor']", MetadataSchemaEnum.RELATION.getName())
                ).doesNotExist()
            );

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
     * This method will test the deletion of a Relationship and will then
     * verify that the relation is removed
     * @throws Exception
     */
    @Override
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
            // This post request will add a first relationship to the publication and thus create
            // a first set of metadata for the author values, namely "Donald Smith"
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

            // This test checks that there's NO relationship on the first author
            // NOTE: relationship is excluded because of tilted left
            getClient(adminToken).perform(get("/api/core/items/" +
                    author1.getID() + "/relationships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("page.totalElements", is(0)));

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

            // This test checks that there's no relationship on the first author
            // NOTE: relationship is excluded because of tilted left
            getClient(adminToken).perform(get("/api/core/items/" +
                    author1.getID() + "/relationships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("page.totalElements", is(0)));

            // This test checks that there's no relationship on the second author
            // NOTE: relationship is excluded because of tilted left
            getClient(adminToken).perform(get("/api/core/items/" +
                    author2.getID() + "/relationships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("page.totalElements", is(0)));


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

            // This test checks that there's no relationship on the second author
            // NOTE: relationship is excluded because of tilted left
            getClient(adminToken).perform(get("/api/core/items/" +
                    author2.getID() + "/relationships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("page.totalElements", is(0)));


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

}
