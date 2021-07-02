/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import static org.dspace.app.rest.projection.CheckSideEntityInRelationshipTypeProjection.PARAM_NAME;
import static org.dspace.app.rest.projection.CheckSideEntityInRelationshipTypeProjection.PROJECTION_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckSideEntityInRelationshipTypeProjectionIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    String leftEntityType;
    String rightEntityType;
    String leftwardType;
    String rightwardType;
    protected RelationshipType relationshipType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        leftEntityType = "Publication";
        rightEntityType = "Person";
        leftwardType = "isAuthorOfPublication";
        rightwardType = "isPublicationOfAuthor";

        relationshipType = relationshipTypeService.findbyTypesAndTypeName(
            context, entityTypeService.findByEntityType(context, leftEntityType),
            entityTypeService.findByEntityType(context, rightEntityType),
            leftwardType, rightwardType
        );
    }

    @Test
    public void testPropertiesNotPresentWhenProjectionInactive() throws Exception {
        getClient().perform(
            // projection is not active
            get("/api/core/relationshiptypes/{relationship-type-id}", relationshipType.getID())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(relationshipType.getID())))
            .andExpect(jsonPath("$.relatedTypeLeft").doesNotExist())
            .andExpect(jsonPath("$.relatedTypeRight").doesNotExist());
    }

    @Test
    public void testPropertiesPresentWhenProjectionActive() throws Exception {
        getClient().perform(
            // projection is active and no entity type should be checked
            get("/api/core/relationshiptypes/{relationship-type-id}", relationshipType.getID())
                .param("projection", PROJECTION_NAME)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(relationshipType.getID())))
            .andExpect(jsonPath("$.relatedTypeLeft", Matchers.is(false)))
            .andExpect(jsonPath("$.relatedTypeRight", Matchers.is(false)));
    }

    @Test
    public void testCheckLeftEntityType() throws Exception {
        getClient().perform(
            // projection is active and left entity type should be checked
            get("/api/core/relationshiptypes/{relationship-type-id}", relationshipType.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, leftEntityType)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(relationshipType.getID())))
            .andExpect(jsonPath("$.relatedTypeLeft", Matchers.is(true)))
            .andExpect(jsonPath("$.relatedTypeRight", Matchers.is(false)));
    }

    @Test
    public void testCheckRightEntityType() throws Exception {
        getClient().perform(
            // projection is active and right entity type should be checked
            get("/api/core/relationshiptypes/{relationship-type-id}", relationshipType.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, rightEntityType)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(relationshipType.getID())))
            .andExpect(jsonPath("$.relatedTypeLeft", Matchers.is(false)))
            .andExpect(jsonPath("$.relatedTypeRight", Matchers.is(true)));
    }

    @Test
    public void testCheckMultipleEntityTypesShouldReturnBadRequest() throws Exception {
        getClient().perform(
            // projection is active, but two entity type params
            get("/api/core/relationshiptypes/{relationship-type-id}", relationshipType.getID())
                .param("projection", PROJECTION_NAME)
                .param(PARAM_NAME, leftEntityType)
                .param(PARAM_NAME, rightEntityType)
        )
            .andExpect(status().isBadRequest());
    }

}
