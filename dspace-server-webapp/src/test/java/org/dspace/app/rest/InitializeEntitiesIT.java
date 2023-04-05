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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.List;

import org.dspace.app.rest.matcher.RelationshipTypeMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.EntityType;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is used to verify the behavior of the {@link org.dspace.app.util.InitializeEntities} script
 * It will take the relationship-types.xml as initial input and check if all the objects that created properly.
 * It will then also update and verify these objects by using a second XML to verify the update behaviour
 * of this script.
 * This will ensure that all the EntityTypes and RelationshipTypes are instantiated correctly.
 */
public class InitializeEntitiesIT extends AbstractControllerIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Build the relationships using the standard test XML with the initialize-entities script
     */
    @Before
    public void setup() throws Exception {

        //Set up the database for the next test
        String pathToFile = configurationService.getProperty("dspace.dir") +
            File.separator + "config" + File.separator + "entities" + File.separator + "relationship-types.xml";
        runDSpaceScript("initialize-entities", "-f", pathToFile);

    }

    @After
    @Override
    public void destroy() throws Exception {
        //Clean up the database for the next test
        context.turnOffAuthorisationSystem();
        List<RelationshipType> relationshipTypeList = relationshipTypeService.findAll(context);
        List<EntityType> entityTypeList = entityTypeService.findAll(context);
        List<Relationship> relationships = relationshipService.findAll(context);

        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }

        for (RelationshipType relationshipType : relationshipTypeList) {
            relationshipTypeService.delete(context, relationshipType);
        }

        for (EntityType entityType: entityTypeList) {
            if (!Constants.ENTITY_TYPE_NONE.equals(entityType.getLabel())) {
                entityTypeService.delete(context, entityType);
            }
        }
        context.restoreAuthSystemState();

        super.destroy();
    }

    /**
     * Verifies that the initialize-entities script ran properly and that the objects are created properly
     * @throws Exception
     */
    @Test
    public void getAllRelationshipTypesTest() throws Exception {
        List<RelationshipType> relationshipTypes = relationshipTypeService.findAll(context);

        getClient().perform(get("/api/core/relationshiptypes")
                .param("projection", "full"))

                //We expect a 200 OK status
                .andExpect(status().isOk())
                //10 relationship types should be created
                .andExpect(jsonPath("$.page.totalElements", is(10)))
                //There needs to be a self link to this endpoint
                .andExpect(jsonPath("$._links.self.href", containsString("api/core/relationshiptypes")))
                //We have 10 relationship types, they need to all be present in the embedded section
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
                        RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(9)))
                ));

        //Verify the left min cardinality of the first relationship type (isAuthorOfPublication) is 0
        getClient().perform(get("/api/core/relationshiptypes/" + relationshipTypes.get(0).getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leftMinCardinality", is(0)));
    }

    /**
     * Verify whether the initialize-entities script can update the relationship types correctly
     */
    @Test
    public void updateRelationshipTypesTest() throws Exception {
        List<RelationshipType> relationshipTypes = relationshipTypeService.findAll(context);

        //Update the relationships using a different test XML with the initialize-entities script
        String pathToFile = configurationService.getProperty("dspace.dir") +
            File.separator + "config" + File.separator + "entities" + File.separator + "relationship-types-update.xml";
        runDSpaceScript("initialize-entities", "-f", pathToFile);

        // This is a helper object to compare whether the update was successful. We're simply taking the first
        // RelationshipType object in the list and altering this by setting the LeftMinCardinality on 10. We've
        // Made sure that this RelationshipType object is altered in the same way in the relationship-types-update.xml
        // File and thus by running the script, the RelationshipType object in the Database should be the same as our
        // alteredRelationshipType object that we just created.
        // It's important to note that this object will not alter anything in the Database, this object is merely
        // made for the comparison between the REST response and we expect to have happened through the script
        RelationshipType alteredRelationshipType = relationshipTypes.get(0);
        alteredRelationshipType.setLeftMinCardinality(10);
        getClient().perform(get("/api/core/relationshiptypes")
                   .param("projection", "full"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //10 relationship types should remain present (no duplicates created)
                   .andExpect(jsonPath("$.page.totalElements", is(10)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/relationshiptypes")))
                   //We have 10 relationship types, they need to all be present in the embedded section
                   //Verify the left min cardinality of the isAuthorOfPublication has been updated to 10
                   .andExpect(jsonPath("$._embedded.relationshiptypes", containsInAnyOrder(
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(alteredRelationshipType),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(1)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(2)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(3)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(4)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(5)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(6)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(7)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(8)),
                       RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipTypes.get(9)))
                   ));

        //Verify the left min cardinality of the first relationship type (isAuthorOfPublication) has been updated to 10
        getClient().perform(get("/api/core/relationshiptypes/" + relationshipTypes.get(0).getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.leftMinCardinality", is(10)));
    }
}
