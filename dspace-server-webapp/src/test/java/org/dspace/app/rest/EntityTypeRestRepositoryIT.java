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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityTypeRestRepositoryIT extends AbstractEntityIntegrationTest {


    @Autowired
    private EntityTypeService entityTypeService;

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

    @Test
    public void getAllEntityTypeEndpointWithPaging() throws Exception {
        getClient().perform(get("/api/core/entitytypes").param("size", "5"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.size", is(5)))
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Person")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Project")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "OrgUnit")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal"))
                   )));

        getClient().perform(get("/api/core/entitytypes").param("size", "5").param("page", "1"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.size", is(5)))
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.number", is(1)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalVolume")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue"))
                   )));
    }

    @Test
    public void retrieveOneEntityType() throws Exception {
        EntityType entityType = entityTypeService.findByEntityType(context, "Publication");
        getClient().perform(get("/api/core/entitytypes/" + entityType.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", EntityTypeMatcher.matchEntityTypeEntry(entityType)));
    }

    @Test
    public void retrieveOneEntityTypeThatDoesNotExist() throws Exception {
        getClient().perform(get("/api/core/entitytypes/" + 5555))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findAllByAuthorizedCollection() throws Exception {
        try {
            context.turnOffAuthorisationSystem();

            //** GIVEN **
            //1. A community-collection structure with one parent community with sub-community and one collection.
            parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
            Collection col1 =
                CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
            Collection col2 =
                CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();
            Collection col3 =
                CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 3").build();
            Collection col4 =
                CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 4").build();
            CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

            collectionService.addMetadata(context, col1, MetadataSchemaEnum.RELATIONSHIP.getName(), "type", null, null,
                "JournalIssue");

            collectionService.addMetadata(context, col2, MetadataSchemaEnum.RELATIONSHIP.getName(), "type", null, null,
                "Publication");

            collectionService.addMetadata(context, col3, MetadataSchemaEnum.RELATIONSHIP.getName(), "type", null, null,
                "DataPackage");

            collectionService.addMetadata(context, col4, MetadataSchemaEnum.RELATIONSHIP.getName(), "type", null, null,
                "Journal");

            context.restoreAuthSystemState();

            context.setCurrentUser(eperson);
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher
                        .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "DataPackage")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal"))
                )));

        } finally {
            CommunityBuilder.deleteCommunity(parentCommunity.getID());
        }
    }
}
