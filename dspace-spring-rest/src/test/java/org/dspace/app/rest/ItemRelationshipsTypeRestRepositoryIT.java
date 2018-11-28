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
import org.dspace.content.ItemRelationshipsType;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemRelationshipTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ItemRelationshipsTypeRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private ItemRelationshipTypeService itemRelationshipTypeService;

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
        List<ItemRelationshipsType> itemRelationshipsTypeList = itemRelationshipTypeService.findAll(context);
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

        Iterator<ItemRelationshipsType> entityTypeIterator = itemRelationshipsTypeList.iterator();
        while (entityTypeIterator.hasNext()) {
            ItemRelationshipsType itemRelationshipsType = entityTypeIterator.next();
            entityTypeIterator.remove();
            itemRelationshipTypeService.delete(context, itemRelationshipsType);
        }

        super.destroy();
    }

    @Test
    public void findAllEntityTypesSizeTest() throws SQLException {
        assertEquals(7, itemRelationshipTypeService.findAll(context).size());
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
        ItemRelationshipsType itemRelationshipsType = itemRelationshipTypeService.findByEntityType(context, type);
        assertNotNull(itemRelationshipsType);
        assertEquals(type, itemRelationshipsType.getLabel());
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
                           .matchEntityTypeEntry(itemRelationshipTypeService.findByEntityType(context, "Publication")),
                       EntityTypeMatcher.matchEntityTypeEntry(
                           itemRelationshipTypeService.findByEntityType(context, "Person")),
                       EntityTypeMatcher.matchEntityTypeEntry(
                           itemRelationshipTypeService.findByEntityType(context, "Project")),
                       EntityTypeMatcher.matchEntityTypeEntry(
                           itemRelationshipTypeService.findByEntityType(context, "OrgUnit")),
                       EntityTypeMatcher.matchEntityTypeEntry(
                           itemRelationshipTypeService.findByEntityType(context, "Journal")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(itemRelationshipTypeService.findByEntityType(context, "JournalVolume"
                           )),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(itemRelationshipTypeService.findByEntityType(context, "JournalIssue"))
                   )));
    }
}