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

import java.util.Arrays;

import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.matcher.RelationshipTypeMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Constants;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the entity type endpoint
 *
 * @author Mykhaylo Boychuk - 4Science
 */
public class EntityTypeRestRepositoryIT extends AbstractEntityIntegrationTest {

    @Autowired
    private ExternalDataService externalDataService;
    @Autowired
    private EntityTypeService entityTypeService;

    @Test
    public void getAllEntityTypeEndpoint() throws Exception {
        //When we call this facets endpoint
        getClient().perform(get("/api/core/entitytypes"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.totalElements", is(8)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                                Constants.ENTITY_TYPE_NONE)),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "Publication")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "Person")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "Project")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "OrgUnit")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "Journal")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "JournalVolume")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                               "JournalIssue"))
                   )));
    }

    @Test
    public void getAllEntityTypeEndpointWithPaging() throws Exception {
        getClient().perform(get("/api/core/entitytypes").param("size", "5"))

                //We expect a 200 OK status
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.page.size", is(5)))
                .andExpect(jsonPath("$.page.totalElements", is(8)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                //There needs to be a self link to this endpoint
                .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                //We have 4 facets in the default configuration, they need to all be present in the embedded section
                .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "Journal")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "JournalIssue")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "JournalVolume")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "OrgUnit")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "Person"))
                )));

        getClient().perform(get("/api/core/entitytypes").param("size", "5").param("page", "1"))

                //We expect a 200 OK status
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.page.size", is(5)))
                .andExpect(jsonPath("$.page.totalElements", is(8)))
                .andExpect(jsonPath("$.page.totalPages", is(2)))
                .andExpect(jsonPath("$.page.number", is(1)))
                //There needs to be a self link to this endpoint
                .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                //We have 4 facets in the default configuration, they need to all be present in the embedded section
                .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "Project")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "Publication")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                             Constants.ENTITY_TYPE_NONE))
                )));
    }

    @Test
    public void retrieveOneEntityType() throws Exception {
        EntityType entityType = entityTypeService.findByEntityType(context.getSession(), "Publication");
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
    public void findAllPaginationTest() throws Exception {
        getClient().perform(get("/api/core/entitytypes")
                   .param("page", "0")
                   .param("size", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                                                                                              "Journal")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                                                                                              "JournalIssue")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                                                                                              "JournalVolume"))
                    )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$.page.size", is(3)))
                   .andExpect(jsonPath("$.page.totalElements", is(8)))
                   .andExpect(jsonPath("$.page.totalPages", is(3)))
                   .andExpect(jsonPath("$.page.number", is(0)));

        getClient().perform(get("/api/core/entitytypes")
                   .param("page", "1")
                   .param("size", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "OrgUnit")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "Person")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                            "Project"))
                    )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$.page.size", is(3)))
                   .andExpect(jsonPath("$.page.totalElements", is(8)))
                   .andExpect(jsonPath("$.page.totalPages", is(3)))
                   .andExpect(jsonPath("$.page.number", is(1)));
    }

    @Test
    public void findEntityTypeWithEmbedRelatioshipTypeTest() throws Exception {

        EntityType person = entityTypeService.findByEntityType(context.getSession(), "Person");
        EntityType orgunit = entityTypeService.findByEntityType(context.getSession(), "OrgUnit");
        EntityType project = entityTypeService.findByEntityType(context.getSession(), "Project");
        EntityType publication = entityTypeService.findByEntityType(context.getSession(), "Publication");
        EntityType journalIssue = entityTypeService.findByEntityType(context.getSession(), "journalIssue");

        RelationshipType relationshipType1 = relationshipTypeService.findbyTypesAndTypeName(context.getSession(),
                             publication, person, "isAuthorOfPublication", "isPublicationOfAuthor");
        RelationshipType relationshipType2 = relationshipTypeService.findbyTypesAndTypeName(context.getSession(),
                          publication, project, "isProjectOfPublication", "isPublicationOfProject");
        RelationshipType relationshipType3 = relationshipTypeService.findbyTypesAndTypeName(context.getSession(),
                          publication, orgunit, "isOrgUnitOfPublication", "isPublicationOfOrgUnit");
        RelationshipType relationshipType4 = relationshipTypeService.findbyTypesAndTypeName(context.getSession(),
           journalIssue, publication, "isPublicationOfJournalIssue", "isJournalIssueOfPublication");
        RelationshipType relationshipType5 = relationshipTypeService.findbyTypesAndTypeName(context.getSession(),
                             publication, orgunit, "isAuthorOfPublication","isPublicationOfAuthor");

        getClient().perform(get("/api/core/entitytypes/" + publication.getID())
                   .param("embed", "relationshiptypes"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", EntityTypeMatcher.matchEntityTypeEntry(publication)))
                   .andExpect(jsonPath("$._embedded.relationshiptypes._embedded.relationshiptypes", containsInAnyOrder(
                           RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType1),
                           RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType2),
                           RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType3),
                           RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType4),
                           RelationshipTypeMatcher.matchRelationshipTypeEntry(relationshipType5)
                           )));
    }

    @Test
    public void findAllByAuthorizedCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

       CollectionBuilder.createCollection(context, parentCommunity)
                        .withEntityType("JournalIssue")
                        .withSubmitterGroup(eperson)
                        .withName("Collection 1")
                        .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType("Publication")
                         .withSubmitterGroup(eperson)
                         .withName("Collection 2")
                         .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType("Project")
                         .withSubmitterGroup(eperson)
                         .withName("Collection 3")
                         .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType("Journal")
                         .withSubmitterGroup(eperson)
                         .withName("Collection 4")
                         .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "JournalIssue")),
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "Publication")),
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "Project")),
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "Journal"))
                   )));
    }

    @Test
    public void findAllByAuthorizedCollectionPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                        .withEntityType("JournalIssue")
                        .withSubmitterGroup(eperson)
                        .withName("Collection 1")
                        .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType("Publication")
                         .withSubmitterGroup(eperson)
                         .withName("Collection 2")
                         .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType("Project")
                         .withSubmitterGroup(eperson)
                         .withName("Collection 3")
                         .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType("Journal")
                         .withSubmitterGroup(eperson)
                         .withName("Collection 4")
                         .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection")
                        .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "JournalIssue")),
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "Journal"))
                         )))
                        .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                                  Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                  Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                                  Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                  Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                                  Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                  Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                                  Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                  Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$.page.size", is(2)))
                        .andExpect(jsonPath("$.page.totalElements", is(4)))
                        .andExpect(jsonPath("$.page.totalPages", is(2)))
                        .andExpect(jsonPath("$.page.number", is(0)));

        getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection")
                        .param("page", "1")
                        .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "Publication")),
                   EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context.getSession(),
                           "Project"))
                         )))
                        .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                                Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                                Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                                Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                                Matchers.containsString("api/core/entitytypes/search/findAllByAuthorizedCollection?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                        .andExpect(jsonPath("$.page.size", is(2)))
                        .andExpect(jsonPath("$.page.totalElements", is(4)))
                        .andExpect(jsonPath("$.page.totalPages", is(2)))
                        .andExpect(jsonPath("$.page.number", is(1)));
    }

    @Test
    public void findAllByAuthorizedExternalSource() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = entityTypeService.findByEntityType(context.getSession(), "Publication");
        EntityType orgUnit = entityTypeService.findByEntityType(context.getSession(), "OrgUnit");
        EntityType project = entityTypeService.findByEntityType(context.getSession(), "Project");
        EntityType funding = EntityTypeBuilder.createEntityTypeBuilder(context, "Funding").build();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Parent Community")
                                                  .build();

        CollectionBuilder.createCollection(context, rootCommunity)
                                           .withEntityType(orgUnit.getLabel())
                                           .withName("Collection 1")
                                           .build();

        CollectionBuilder.createCollection(context, rootCommunity)
                                           .withEntityType(publication.getLabel())
                                           .withSubmitterGroup(eperson)
                                           .withName("Collection 2")
                                           .build();

        CollectionBuilder.createCollection(context, rootCommunity)
                                           .withEntityType(project.getLabel())
                                           .withSubmitterGroup(eperson)
                                           .withName("Collection 3")
                                           .build();

        CollectionBuilder.createCollection(context, rootCommunity)
                                           .withEntityType(funding.getLabel())
                                           .withSubmitterGroup(eperson)
                                           .withName("Collection 4")
                                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                                   EntityTypeMatcher.matchEntityTypeEntry(publication),
                                   EntityTypeMatcher.matchEntityTypeEntry(funding),
                                   EntityTypeMatcher.matchEntityTypeEntry(project))))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                                        EntityTypeMatcher.matchEntityTypeEntry(orgUnit),
                                        EntityTypeMatcher.matchEntityTypeEntry(funding),
                                        EntityTypeMatcher.matchEntityTypeEntry(project),
                                        EntityTypeMatcher.matchEntityTypeEntry(publication))))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

        try {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(Arrays.asList("Publication"));
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("pubmed"))
                    .setSupportedEntityTypes(Arrays.asList("Publication"));

            // these are similar to the previous checks but now we have restricted the mock and pubmed providers
            // to support only publication, this mean that there are no providers suitable for funding
            getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                                       EntityTypeMatcher.matchEntityTypeEntry(publication),
                                       EntityTypeMatcher.matchEntityTypeEntry(project))))
                            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

            getClient(adminToken).perform(get("/api/core/entitytypes/search/findAllByAuthorizedExternalSource"))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                                            EntityTypeMatcher.matchEntityTypeEntry(project),
                                            EntityTypeMatcher.matchEntityTypeEntry(orgUnit),
                                            EntityTypeMatcher.matchEntityTypeEntry(publication))))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));

        } finally {
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("mock"))
                    .setSupportedEntityTypes(null);
            ((AbstractExternalDataProvider) externalDataService.getExternalDataProvider("pubmed"))
                    .setSupportedEntityTypes(null);
        }

    }

}