/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.matcher.CommunityMetadataMatcher;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.hamcrest.Matchers;
import org.junit.Test;

public class CommunityRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void createTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        // We send a name but the created community should set this to the title
        comm.setName("Test Top-Level Community");

        MetadataEntryRest description = new MetadataEntryRest();
        description.setKey("dc.description");
        description.setValue("<p>Some cool HTML code here</p>");

        MetadataEntryRest abs = new MetadataEntryRest();
        abs.setKey("dc.description.abstract");
        abs.setValue("Sample top-level community created via the REST API");

        MetadataEntryRest contents = new MetadataEntryRest();
        contents.setKey("dc.description.tableofcontents");
        contents.setValue("<p>HTML News</p>");

        MetadataEntryRest copyright = new MetadataEntryRest();
        copyright.setKey("dc.rights");
        copyright.setValue("Custom Copyright Text");

        MetadataEntryRest title = new MetadataEntryRest();
        title.setKey("dc.title");
        title.setValue("Title Text");

        comm.setMetadata(Arrays.asList(description,
                                       abs,
                                       contents,
                                       copyright,
                                       title));

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/core/communities")
                                        .content(mapper.writeValueAsBytes(comm))
                                        .contentType(contentType))
                   .andExpect(status().isCreated())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                               hasJsonPath("$.id", not(empty())),
                               hasJsonPath("$.uuid", not(empty())),
                               hasJsonPath("$.name", is("Title Text")),
                               hasJsonPath("$.handle", not(empty())),
                               hasJsonPath("$.type", is("community")),
                               hasJsonPath("$._links.collections.href", not(empty())),
                               hasJsonPath("$._links.logo.href", not(empty())),
                               hasJsonPath("$._links.subcommunities.href", not(empty())),
                               hasJsonPath("$._links.self.href", not(empty())),
                               hasJsonPath("$.metadata", Matchers.containsInAnyOrder(
                                   CommunityMetadataMatcher.matchMetadata("dc.description",
                                           "<p>Some cool HTML code here</p>"),
                                   CommunityMetadataMatcher.matchMetadata("dc.description.abstract",
                                           "Sample top-level community created via the REST API"),
                                   CommunityMetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                           "<p>HTML News</p>"),
                                   CommunityMetadataMatcher.matchMetadata("dc.rights",
                                           "Custom Copyright Text"),
                                   CommunityMetadataMatcher.matchMetadata("dc.title",
                                           "Title Text")
                               )))));

    }

    @Test
    public void createUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        comm.setName("Test Top-Level Community");

        MetadataEntryRest title = new MetadataEntryRest();
        title.setKey("dc.title");
        title.setValue("Title Text");

        comm.setMetadata(Arrays.asList(title));

        // Anonymous user tries to create a community.
        // Should fail because user is not authenticated. Error 401.
        getClient().perform(post("/api/core/communities")
                                        .content(mapper.writeValueAsBytes(comm))
                                        .contentType(contentType))
                   .andExpect(status().isUnauthorized());

        // Non-admin Eperson tries to create a community.
        // Should fail because user doesn't have permissions. Error 403.
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/core/communities")
                                        .content(mapper.writeValueAsBytes(comm))
                                        .contentType(contentType))
                   .andExpect(status().isForbidden());
    }

    @Test
    public void findAllTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withLogo("Test Logo")
                                           .build();

        getClient().perform(get("/api/core/communities"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                       CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                            parentCommunity.getHandle()),
                       CommunityMatcher
                           .matchCommunityWithCollectionEntry(child1.getName(), child1.getID(), child1.getHandle(),
                                                              col1)
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities")
                                .param("size", "1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                       CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                            parentCommunity.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.not(
                       Matchers.contains(
                           CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;

        getClient().perform(get("/api/core/communities")
                                .param("size", "1")
                                .param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                       CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.not(
                       Matchers.contains(
                           CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                                parentCommunity.getHandle())
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                   .andExpect(jsonPath("$.page.size", is(1)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;
    }

    @Test
    public void findOneTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                            parentCommunity.getHandle())
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       Matchers.is(
                           CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;
    }

    @Test
    public void findOneRelsTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                            parentCommunity.getHandle())
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       Matchers.is(
                           CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                       )
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .containsString("/api/core/communities/" + parentCommunity.getID().toString())))
                   .andExpect(jsonPath("$._links.logo.href", Matchers
                       .containsString("/api/core/communities/" + parentCommunity.getID().toString() + "/logo")))
                   .andExpect(jsonPath("$._links.collections.href", Matchers
                       .containsString("/api/core/communities/" + parentCommunity.getID().toString() + "/collections")))
                   .andExpect(jsonPath("$._links.subcommunities.href", Matchers
                        .containsString("/api/core/communities/" + parentCommunity.getID().toString() +
                                "/subcommunities")))
        ;

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/logo"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType));

        getClient().perform(get("/api/core/communities/" + child1.getID().toString() + "/logo"))
                   .andExpect(status().isNoContent());

        //Main community has no collections, therefore contentType is not set
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/collections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));

        getClient().perform(get("/api/core/communities/" + child1.getID().toString() + "/collections"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType));

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString() + "/subcommunities"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType));

        //child1 subcommunity has no subcommunities, therefore contentType is not set
        getClient().perform(get("/api/core/communities/" + child1.getID().toString() + "/subcommunities"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }


    @Test
    public void findAllSearchTop() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();

        Community parentCommunity2 = CommunityBuilder.createCommunity(context)
                                                     .withName("Parent Community 2")
                                                     .withLogo("SomeTest")
                                                     .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        Community child12 = CommunityBuilder.createSubCommunity(context, child1)
                                            .withName("Sub Sub Community")
                                            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();


        getClient().perform(get("/api/core/communities/search/top"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                       CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                            parentCommunity.getHandle()),
                       CommunityMatcher.matchCommunityEntry(parentCommunity2.getName(), parentCommunity2.getID(),
                                                            parentCommunity2.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.not(Matchers.containsInAnyOrder(
                       CommunityMatcher.matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle()),
                       CommunityMatcher.matchCommunityEntry(child12.getName(), child12.getID(), child12.getHandle())
                   ))))
                   .andExpect(
                       jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities/search/top")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;
    }

    @Test
    public void findAllSubCommunities() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();

        Community parentCommunity2 = CommunityBuilder.createCommunity(context)
                                                     .withName("Parent Community 2")
                                                     .withLogo("SomeTest")
                                                     .build();

        Community parentCommunityChild1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();

        Community parentCommunityChild2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community2")
                .build();

        Community parentCommunityChild2Child1 = CommunityBuilder.createSubCommunity(context, parentCommunityChild2)
                .withName("Sub Sub Community")
                .build();


        Community parentCommunity2Child1 = CommunityBuilder.createSubCommunity(context, parentCommunity2)
                .withName("Sub2 Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunityChild1)
                                           .withName("Collection 1")
                                           .build();

        getClient().perform(get("/api/core/communities/search/subCommunities")
                .param("parent", parentCommunity.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                //Checking that these communities are present
                .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                        CommunityMatcher.matchCommunityEntry(parentCommunityChild1.getName(),
                                                             parentCommunityChild1.getID(),
                                                             parentCommunityChild1.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunityChild2.getName(),
                                                             parentCommunityChild2.getID(),
                                                             parentCommunityChild2.getHandle())
                )))
                //Checking that these communities are not present
                .andExpect(jsonPath("$._embedded.communities", Matchers.not(Matchers.anyOf(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getName(),
                                                             parentCommunity.getID(),
                                                             parentCommunity.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunity2.getName(),
                                                             parentCommunity2.getID(),
                                                             parentCommunity2.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunity2Child1.getName(),
                                                             parentCommunity2Child1.getID(),
                                                             parentCommunity2Child1.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunityChild2Child1.getName(),
                                                             parentCommunityChild2Child1.getID(),
                                                             parentCommunityChild2Child1.getHandle())
                ))))
                .andExpect(jsonPath("$._links.self.href",
                                    Matchers.containsString("/api/core/communities/search/subCommunities")))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;

        getClient().perform(get("/api/core/communities/search/subCommunities")
                .param("parent", parentCommunityChild2.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                //Checking that these communities are present
                .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                        CommunityMatcher.matchCommunityEntry(parentCommunityChild2Child1.getName(),
                                                             parentCommunityChild2Child1.getID(),
                                                             parentCommunityChild2Child1.getHandle())
                )))
                //Checking that these communities are not present
                .andExpect(jsonPath("$._embedded.communities", Matchers.not(Matchers.anyOf(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getName(),
                                                             parentCommunity.getID(),
                                                             parentCommunity.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunity2.getName(),
                                                             parentCommunity2.getID(),
                                                             parentCommunity2.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunity2Child1.getName(),
                                                             parentCommunity2Child1.getID(),
                                                             parentCommunity2Child1.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunityChild2Child1.getName(),
                                                             parentCommunityChild2Child1.getID(),
                                                             parentCommunityChild2Child1.getHandle()),
                        CommunityMatcher.matchCommunityEntry(parentCommunityChild1.getName(),
                                                             parentCommunityChild1.getID(),
                                                             parentCommunityChild1.getHandle())
                ))))
                .andExpect(jsonPath("$._links.self.href",
                                    Matchers.containsString("/api/core/communities/search/subCommunities")))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)))
        ;

        getClient().perform(get("/api/core/communities/search/subCommunities")
                .param("parent", parentCommunityChild2Child1.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.self.href",
                                    Matchers.containsString("/api/core/communities/search/subCommunities")))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(0)))
        ;
    }

    @Test
    public void findAllSubCommunitiesWithoutUUID() throws Exception {
        getClient().perform(get("/api/core/communities/search/subCommunities"))
                 .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findAllSubCommunitiesWithUnexistentUUID() throws Exception {
        getClient().perform(get("/api/core/communities/search/subCommunities")
                 .param("parent", UUID.randomUUID().toString()))
                 .andExpect(status().isNotFound());
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        getClient().perform(get("/api/core/communities/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
