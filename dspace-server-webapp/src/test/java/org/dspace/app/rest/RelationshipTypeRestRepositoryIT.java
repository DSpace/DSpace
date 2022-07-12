
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.matcher.RelationshipTypeMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipTypeRestRepositoryIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Test
    public void findAllRelationshipTypesTest() throws SQLException {
        assertEquals(12, relationshipTypeService.findAll(context).size());
    }

    @Test
    public void findPublicationPersonRelationshipType() throws SQLException {
        String leftTypeString = "Publication";
        String rightTypeString = "Person";
        String leftwardType = "isAuthorOfPublication";
        String rightwardType = "isPublicationOfAuthor";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findPublicationProjectRelationshipType() throws SQLException {
        String leftTypeString = "Publication";
        String rightTypeString = "Project";
        String leftwardType = "isProjectOfPublication";
        String rightwardType = "isPublicationOfProject";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findPublicationOrgUnitRelationshipType() throws SQLException {
        String leftTypeString = "Publication";
        String rightTypeString = "OrgUnit";
        String leftwardType = "isOrgUnitOfPublication";
        String rightwardType = "isPublicationOfOrgUnit";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findPersonProjectRelationshipType() throws SQLException {
        String leftTypeString = "Person";
        String rightTypeString = "Project";
        String leftwardType = "isProjectOfPerson";
        String rightwardType = "isPersonOfProject";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findPersonOrgUnitRelationshipType() throws SQLException {
        String leftTypeString = "Person";
        String rightTypeString = "OrgUnit";
        String leftwardType = "isOrgUnitOfPerson";
        String rightwardType = "isPersonOfOrgUnit";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findProjectOrgUnitRelationshipType() throws SQLException {
        String leftTypeString = "Project";
        String rightTypeString = "OrgUnit";
        String leftwardType = "isOrgUnitOfProject";
        String rightwardType = "isProjectOfOrgUnit";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findJournalJournalVolumeRelationshipType() throws SQLException {
        String leftTypeString = "Journal";
        String rightTypeString = "JournalVolume";
        String leftwardType = "isVolumeOfJournal";
        String rightwardType = "isJournalOfVolume";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    @Test
    public void findJournalVolumeJournalIssueRelationshipType() throws SQLException {
        String leftTypeString = "JournalVolume";
        String rightTypeString = "JournalIssue";
        String leftwardType = "isIssueOfJournalVolume";
        String rightwardType = "isJournalVolumeOfIssue";
        checkRelationshipType(leftTypeString, rightTypeString, leftwardType, rightwardType);
    }

    private void checkRelationshipType(String leftType, String rightType, String leftwardType, String rightwardType)
        throws SQLException {
        RelationshipType relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, leftType),
                                  entityTypeService.findByEntityType(context, rightType),
                                  leftwardType, rightwardType);
        assertNotNull(relationshipType);
        assertEquals(entityTypeService.findByEntityType(context, leftType),
                     relationshipType.getLeftType());
        assertEquals(entityTypeService.findByEntityType(context, rightType),
                     relationshipType.getRightType());
        assertEquals(leftwardType, relationshipType.getLeftwardType());
        assertEquals(rightwardType, relationshipType.getRightwardType());
    }

    @Test
    public void getAllRelationshipTypesEndpointTest() throws Exception {
        //When we call this facets endpoint
        List<RelationshipType> relationshipTypes = relationshipTypeService.findAll(context);

        getClient().perform(get("/api/core/relationshiptypes").param("projection", "full"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.totalElements", is(12)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/relationshiptypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(0)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(1)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(2)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(3)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(4)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(5)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(6)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(7)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(8)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(9)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(10)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(11)))
                   ));
    }

    @Test
    public void entityTypeForPublicationPersonRelationshipTypeTest() throws Exception {

        List<RelationshipType> relationshipTypes = relationshipTypeService.findAll(context);

        RelationshipType foundRelationshipType = null;
        for (RelationshipType relationshipType : relationshipTypes) {
            if (StringUtils.equals(relationshipType.getLeftwardType(), "isAuthorOfPublication") && StringUtils
                .equals(relationshipType.getRightwardType(), "isPublicationOfAuthor")) {
                foundRelationshipType = relationshipType;
                break;
            }
        }

        if (foundRelationshipType != null) {
            getClient().perform(get("/api/core/relationshiptypes/" + foundRelationshipType.getID())
                       .param("projection", "full"))
                       .andExpect(jsonPath("$._embedded.leftType",
                                           EntityTypeMatcher.matchEntityTypeEntryForLabel("Publication")))
                       .andExpect(
                           jsonPath("$._embedded.rightType", EntityTypeMatcher.matchEntityTypeEntryForLabel("Person")));
        } else {
            throw new Exception("RelationshipType not found for isIssueOfJournalVolume");
        }

    }

    @Test
    public void cardinalityOnAuthorPublicationRelationshipTypesTest() throws Exception {
        RelationshipType relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                  entityTypeService.findByEntityType(context, "Person"), "isAuthorOfPublication",
                                  "isPublicationOfAuthor");
        assertEquals(((Integer) 0), relationshipType.getLeftMinCardinality());
        assertEquals(((Integer) 0), relationshipType.getRightMinCardinality());
        assertNull(relationshipType.getLeftMaxCardinality());
        assertNull(null, relationshipType.getRightMaxCardinality());

        getClient().perform(get("/api/core/relationshiptypes/" + relationshipType.getID()))
                   .andExpect(jsonPath("$.leftMinCardinality", is(0)))
                   .andExpect(jsonPath("$.rightMinCardinality", is(0)))
                   .andExpect(jsonPath("$.leftMaxCardinality", isEmptyOrNullString()))
                   .andExpect(jsonPath("$.rightMaxCardinality", isEmptyOrNullString()));

    }

    @Test
    public void entityTypeForIssueJournalRelationshipTypeTest() throws Exception {

        List<RelationshipType> relationshipTypes = relationshipTypeService.findAll(context);

        RelationshipType foundRelationshipType = null;
        for (RelationshipType relationshipType : relationshipTypes) {
            if (StringUtils.equals(relationshipType.getLeftwardType(), "isIssueOfJournalVolume") && StringUtils
                .equals(relationshipType.getRightwardType(), "isJournalVolumeOfIssue")) {
                foundRelationshipType = relationshipType;
                break;
            }
        }

        if (foundRelationshipType != null) {
            getClient().perform(get("/api/core/relationshiptypes/" + foundRelationshipType.getID())
                       .param("projection", "full"))
                       .andExpect(jsonPath("$._embedded.leftType",
                                           EntityTypeMatcher.matchEntityTypeEntryForLabel("JournalVolume")))
                       .andExpect(jsonPath("$._embedded.rightType",
                                           EntityTypeMatcher.matchEntityTypeEntryForLabel("JournalIssue")));
        } else {
            throw new Exception("RelationshipType not found for isIssueOfJournalVolume");
        }

    }

    @Test
    public void cardinalityOnIssueJournalJournalVolumeRelationshipTypesTest() throws Exception {
        RelationshipType relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "JournalVolume"),
                                  entityTypeService.findByEntityType(context, "JournalIssue"), "isIssueOfJournalVolume",
                                  "isJournalVolumeOfIssue");
        assertEquals(((Integer) 0), relationshipType.getLeftMinCardinality());
        assertEquals(((Integer) 1), relationshipType.getRightMinCardinality());
        assertNull(relationshipType.getLeftMaxCardinality());
        assertEquals(((Integer) 1), relationshipType.getRightMaxCardinality());

        getClient().perform(get("/api/core/relationshiptypes/" + relationshipType.getID()))
                   .andExpect(jsonPath("$.leftMinCardinality", is(0)))
                   .andExpect(jsonPath("$.rightMinCardinality", is(1)))
                   .andExpect(jsonPath("$.leftMaxCardinality", isEmptyOrNullString()))
                   .andExpect(jsonPath("$.rightMaxCardinality", is(1)));

    }

    @Test
    public void findByEntityTypePublicationTest() throws Exception {
        getClient().perform(get("/api/core/relationshiptypes/search/byEntityType")
                   .param("type", "Publication"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                           RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                                "isAuthorOfPublication", "isPublicationOfAuthor"),
                           RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                              "isProjectOfPublication", "isPublicationOfProject"),
                           RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                              "isOrgUnitOfPublication", "isPublicationOfOrgUnit"),
                           RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                                "isAuthorOfPublication", "isPublicationOfAuthor"),
                           RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                    "isPublicationOfJournalIssue", "isJournalIssueOfPublication")
                           )))
                   .andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    public void findByEntityTypeMissingParamTest() throws Exception {

        getClient().perform(get("/api/core/relationshiptypes/search/byEntityType"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findByEntityTypeInvalidEntityTypeTest() throws Exception {
        getClient().perform(get("/api/core/relationshiptypes/search/byEntityType")
                   .param("type", "WrongEntityType"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findByEntityTypePublicationPaginationTest() throws Exception {
        getClient().perform(get("/api/core/relationshiptypes/search/byEntityType")
                   .param("type", "Publication")
                   .param("size", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                              RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                                   "isAuthorOfPublication", "isPublicationOfAuthor"),
                              RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                                 "isProjectOfPublication", "isPublicationOfProject"),
                              RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                                 "isOrgUnitOfPublication", "isPublicationOfOrgUnit")
                              )))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient().perform(get("/api/core/relationshiptypes/search/byEntityType")
                   .param("type", "Publication")
                   .param("page", "1")
                   .param("size", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                              RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                                   "isAuthorOfPublication", "isPublicationOfAuthor"),
                              RelationshipTypeMatcher.matchExplicitRestrictedRelationshipTypeValues(
                                       "isPublicationOfJournalIssue", "isJournalIssueOfPublication")
                              )))
                   .andExpect(jsonPath("$.page.number", is(1)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.totalElements", is(5)));
    }

}
