/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.RelationshipType;
import org.junit.Before;
import org.junit.Test;

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

}
