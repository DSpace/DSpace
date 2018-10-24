/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.EntityType;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityTypeRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RelationshipService relationshipService;

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
    public void findAllEntityTypesSizeTest() throws SQLException {
        assertEquals(7, entityTypeService.findAll(context).size());
    }

    @Test
    public void findPublicationEntityTypeTest() throws SQLException {
        String type = "Publication";
        checkEntityType(type);
    }

    @Test
    public void findPersonEntityTypeTest() throws SQLException {
        String type = "Person";
        checkEntityType(type);
    }

    @Test
    public void findProjectEntityTypeTest() throws SQLException {
        String type = "Project";
        checkEntityType(type);
    }

    @Test
    public void findOrgUnitEntityTypeTest() throws SQLException {
        String type = "OrgUnit";
        checkEntityType(type);
    }

    @Test
    public void findJournalEntityTypeTest() throws SQLException {
        String type = "Journal";
        checkEntityType(type);
    }

    @Test
    public void findJournalVolumeEntityTypeTest() throws SQLException {
        String type = "JournalVolume";
        checkEntityType(type);
    }

    @Test
    public void findJournalIssueEntityTypeTest() throws SQLException {
        String type = "JournalIssue";
        checkEntityType(type);
    }

    private void checkEntityType(String type) throws SQLException {
        EntityType entityType = entityTypeService.findByEntityType(context, type);
        assertNotNull(entityType);
        assertEquals(type, entityType.getLabel());
    }

    @Test
    public void getAllEntityTypeEndpoint() throws Exception {
        //When we call this facets endpoint
        getClient().perform(get("/api/core/entitytypes"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Person")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Project")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "OrgUnit")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalVolume")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue"))
                   )));
    }
}