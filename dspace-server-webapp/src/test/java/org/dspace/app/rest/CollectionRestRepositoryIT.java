/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class CollectionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConverterService converter;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ResourcePolicyService resoucePolicyService;

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();


        getClient().perform(get("/api/core/collections")
                   .param("embed", CollectionMatcher.getEmbedsParameter()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle()),
                       CollectionMatcher.matchCollectionEntryFullProjection(col2.getName(), col2.getID(),
                                                                            col2.getHandle())
                   )));
    }

    @Test
    public void findAllUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 2")
                .build();

        resoucePolicyService.removePolicies(context, col2, Constants.READ);
        context.restoreAuthSystemState();

        // anonymous can see only public collections
        getClient().perform(get("/api/core/collections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                         CollectionMatcher.matchCollection(col1))))
                  .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 2")
                .build();

        resoucePolicyService.removePolicies(context, col2, Constants.READ);
        context.restoreAuthSystemState();

        // eperson logged can see only public collections
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/collections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                         CollectionMatcher.matchCollection(col1))))
                  .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findAllGrantAccessAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson parentAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(parentAdmin)
                .build();

        EPerson col1Admin = EPersonBuilder.createEPerson(context)
                .withEmail("eperson2@mail.com")
                .withPassword("qwerty02")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withAdminGroup(col1Admin)
                .build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 2")
                .build();

        resoucePolicyService.removePolicies(context, parentCommunity, Constants.READ);
        resoucePolicyService.removePolicies(context, col1, Constants.READ);
        context.restoreAuthSystemState();

        // parent community admin can see all sub collections
        String tokenParentAdmin = getAuthToken(parentAdmin.getEmail(), "qwerty01");
        getClient(tokenParentAdmin).perform(get("/api/core/collections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchCollection(col1),
                         CollectionMatcher.matchCollection(col2))))
                  .andExpect(jsonPath("$.page.totalElements", is(2)));

        // admin of col1 can see owner collections and any public collections
        String tokenCol1Admin = getAuthToken(col1Admin.getEmail(), "qwerty02");
        getClient(tokenCol1Admin).perform(get("/api/core/collections"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchCollection(col1),
                         CollectionMatcher.matchCollection(col2))))
                  .andExpect(jsonPath("$.page.totalElements", is(2)));
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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections")
                .param("size", "1")
                   .param("embed", CollectionMatcher.getEmbedsParameter()))

                .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.not(
                       Matchers.contains(
                           CollectionMatcher.matchCollectionEntryFullProjection(col2.getName(), col2.getID(),
                                                                                col2.getHandle())
                       )
                   )));

        getClient().perform(get("/api/core/collections")
                                .param("size", "1")
                                .param("page", "1")
                                   .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                       CollectionMatcher.matchCollectionEntryFullProjection(col2.getName(), col2.getID(),
                                                                            col2.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.not(
                       Matchers.contains(
                           CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                                col1.getHandle())
                       )
                   )));
    }


    @Test
    public void findOneCollectionTest() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/collections/" + col1.getID())
                   .param("embed", CollectionMatcher.getEmbedsParameter()))

                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", CollectionMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", CollectionMatcher.matchCollectionEntry(
                        col1.getName(), col1.getID(), col1.getHandle())));

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient().perform(get("/api/core/collections/" + col1.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andExpect(jsonPath("$", CollectionMatcher.matchProperties(
                        col1.getName(), col1.getID(), col1.getHandle())));
    }

    @Test
    public void findOneCollectionUnAuthenticatedTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        resoucePolicyService.removePolicies(context, col1, Constants.READ);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + col1.getID()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneCollectionForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        resoucePolicyService.removePolicies(context, col1, Constants.READ);
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/core/collections/" + col1.getID()))
                   .andExpect(status().isForbidden());
    }

    @Test
    public void findOneCollectionGrantAccessAdminsTest() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson parentAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(parentAdmin)
                .build();

        EPerson col1Admin = EPersonBuilder.createEPerson(context)
                .withEmail("eperson2@mail.com")
                .withPassword("qwerty02")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withAdminGroup(col1Admin)
                .build();

        EPerson col2Admin = EPersonBuilder.createEPerson(context)
                .withEmail("eperson3@mail.com")
                .withPassword("qwerty03")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 2")
                .withAdminGroup(col2Admin)
                .build();

        resoucePolicyService.removePolicies(context, parentCommunity, Constants.READ);
        resoucePolicyService.removePolicies(context, col1, Constants.READ);
        resoucePolicyService.removePolicies(context, col2, Constants.READ);
        context.restoreAuthSystemState();

        String tokenParentAdmin = getAuthToken(parentAdmin.getEmail(), "qwerty01");
        getClient(tokenParentAdmin).perform(get("/api/core/collections/" + col1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is((CollectionMatcher.matchCollection(col1)))));

        String tokenCol1Admin = getAuthToken(col1Admin.getEmail(), "qwerty02");
        getClient(tokenCol1Admin).perform(get("/api/core/collections/" + col1.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is((CollectionMatcher.matchCollection(col1)))));

        String tokenCol2Admin = getAuthToken(col2Admin.getEmail(), "qwerty03");
        getClient(tokenCol2Admin).perform(get("/api/core/collections/" + col1.getID()))
                .andExpect(status().isForbidden());
    }
    @Test
    public void findOneCollectionRelsTest() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withLogo("TestingContentForLogo").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + col1.getID())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", is(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           CollectionMatcher.matchCollectionEntryFullProjection(col2.getName(), col2.getID(),
                                                                                col2.getHandle())
                       )))
                   )
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID() + "/logo"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.format.href", Matchers.containsString("/api/core/bitstreams")))
                   .andExpect(jsonPath("$._links.format.href", Matchers.containsString("/format")))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/bitstreams")))
                   .andExpect(jsonPath("$._links.content.href", Matchers.containsString("/api/core/bitstreams")))
                   .andExpect(jsonPath("$._links.content.href", Matchers.containsString("/content")))
        ;

    }


    @Test
    public void findAuthorizedTest() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/search/findAuthorized"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist())
        ;

    }


    @Test
    public void findAuthorizedByCommunityTest() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/search/findAuthorizedByCommunity")
                                .param("uuid", parentCommunity.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void findAuthorizedByCommunityWithoutUUIDTest() throws Exception {
        getClient().perform(get("/api/core/collections/search/findAuthorizedByCommunity"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findAuthorizedByCommunityWithUnexistentUUIDTest() throws Exception {
        getClient().perform(get("/api/core/collections/search/findAuthorizedByCommunity")
                                .param("uuid", UUID.randomUUID().toString()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneCollectionTestWrongUUID() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findCollectionWithParentCommunity() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + col1.getID())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", is(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           CollectionMatcher.matchCollectionEntryFullProjection(col2.getName(), col2.getID(),
                                                                                col2.getHandle())
                       ))));
    }

    @Test
    public void updateTest() throws Exception {
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

        getClient().perform(get("/api/core/collections/" + col1.getID().toString())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/collections")))
        ;

        String token = getAuthToken(admin.getEmail(), password);

        ObjectMapper mapper = new ObjectMapper();

        CollectionRest collectionRest = converter.toRest(col1, Projection.DEFAULT);

        collectionRest.setMetadata(new MetadataRest()
                .put("dc.title", new MetadataValueRest("Electronic theses and dissertations")));

        context.restoreAuthSystemState();

        getClient(token).perform(put("/api/core/collections/" + col1.getID().toString())
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsBytes(collectionRest)))
                        .andExpect(status().isOk())
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID().toString())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CollectionMatcher.matchCollectionEntryFullProjection("Electronic theses and dissertations",
                                                              col1.getID(), col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/core/collections")))
        ;
    }

    @Test
    public void deleteTest() throws Exception {
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

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunityChild1)
                                           .withName("Collection 1")
                                           .build();

        String token = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/core/collections/" + col1.getID().toString())
                           .param("embed", CollectionMatcher.getEmbedsParameter()))

                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                            CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                                 col1.getHandle())
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                            Matchers.containsString("/api/core/collections")));
        getClient(token).perform(delete("/api/core/collections/" + col1.getID().toString()))
                        .andExpect(status().isNoContent())
        ;
        getClient(token).perform(get("/api/core/collections/" + col1.getID().toString()))
                        .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void deleteTestUnAuthorized() throws Exception {
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

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunityChild1)
                                           .withName("Collection 1")
                                           .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + col1.getID().toString())
                           .param("embed", CollectionMatcher.getEmbedsParameter()))

                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                            CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                                 col1.getHandle())
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                            Matchers.containsString("/api/core/collections")));
        getClient().perform(delete("/api/core/collections/" + col1.getID().toString()))
                        .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    public void createTest() throws Exception {
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest = new CollectionRest();
        // We send a name but the created collection should set this to the title
        collectionRest.setName("Collection");

        collectionRest.setMetadata(new MetadataRest()
                .put("dc.description",
                        new MetadataValueRest("<p>Some cool HTML code here</p>"))
                .put("dc.description.abstract",
                        new MetadataValueRest("Sample top-level community created via the REST API"))
                .put("dc.description.tableofcontents",
                        new MetadataValueRest("<p>HTML News</p>"))
                .put("dc.rights",
                        new MetadataValueRest("Custom Copyright Text"))
                .put("dc.title",
                        new MetadataValueRest("Title Text")));

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/core/collections")
                                         .content(mapper.writeValueAsBytes(collectionRest))
                                         .param("parent", parentCommunity.getID().toString())
                                         .contentType(contentType)
                                            .param("embed", CollectionMatcher.getEmbedsParameter()))

                            .andExpect(status().isCreated())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", CollectionMatcher.matchFullEmbeds()))
                            .andExpect(jsonPath("$", Matchers.allOf(
                                hasJsonPath("$.id", not(empty())),
                                hasJsonPath("$.uuid", not(empty())),
                                hasJsonPath("$.name", is("Title Text")),
                                hasJsonPath("$.handle", not(empty())),
                                hasJsonPath("$.type", is("collection")),
                                hasJsonPath("$.metadata", Matchers.allOf(
                                    MetadataMatcher.matchMetadata("dc.description",
                                            "<p>Some cool HTML code here</p>"),
                                    MetadataMatcher.matchMetadata("dc.description.abstract",
                                            "Sample top-level community created via the REST API"),
                                    MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                            "<p>HTML News</p>"),
                                    MetadataMatcher.matchMetadata("dc.rights",
                                            "Custom Copyright Text"),
                                    MetadataMatcher.matchMetadata("dc.title",
                                            "Title Text")
                                )))));

        getClient(authToken).perform(post("/api/core/collections")
                .content(mapper.writeValueAsBytes(collectionRest))
                .param("parent", parentCommunity.getID().toString())
                .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));
    }

    @Test
    public void createTestByAuthorizedUser() throws Exception {
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();

        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest = new CollectionRest();
        // We send a name but the created collection should set this to the title
        collectionRest.setName("Collection");

        collectionRest.setMetadata(new MetadataRest()
            .put("dc.description",
                new MetadataValueRest("<p>Some cool HTML code here</p>"))
            .put("dc.description.abstract",
                new MetadataValueRest("Sample top-level community created via the REST API"))
            .put("dc.description.tableofcontents",
                new MetadataValueRest("<p>HTML News</p>"))
            .put("dc.rights",
                new MetadataValueRest("Custom Copyright Text"))
            .put("dc.title",
                new MetadataValueRest("Title Text")));

        // ADD authorization on parent community
        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADD, eperson);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(post("/api/core/collections")
            .content(mapper.writeValueAsBytes(collectionRest))
            .param("parent", parentCommunity.getID().toString())
            .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", Matchers.allOf(
                                hasJsonPath("$.id", not(empty())),
                                hasJsonPath("$.uuid", not(empty())),
                                hasJsonPath("$.name", is("Title Text")),
                                hasJsonPath("$.handle", not(empty())),
                                hasJsonPath("$.type", is("collection")),
                                hasJsonPath("$.metadata", Matchers.allOf(
                                    MetadataMatcher.matchMetadata("dc.description",
                                        "<p>Some cool HTML code here</p>"),
                                    MetadataMatcher.matchMetadata("dc.description.abstract",
                                        "Sample top-level community created via the REST API"),
                                    MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                        "<p>HTML News</p>"),
                                    MetadataMatcher.matchMetadata("dc.rights",
                                        "Custom Copyright Text"),
                                    MetadataMatcher.matchMetadata("dc.title",
                                        "Title Text")
                                )))));

    }

    @Test
    public void createTestByUnauthorizedUser() throws Exception {
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();

        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest = new CollectionRest();
        // We send a name but the created collection should set this to the title
        collectionRest.setName("Collection");

        collectionRest.setMetadata(new MetadataRest()
            .put("dc.description",
                new MetadataValueRest("<p>Some cool HTML code here</p>"))
            .put("dc.description.abstract",
                new MetadataValueRest("Sample top-level community created via the REST API"))
            .put("dc.description.tableofcontents",
                new MetadataValueRest("<p>HTML News</p>"))
            .put("dc.rights",
                new MetadataValueRest("Custom Copyright Text"))
            .put("dc.title",
                new MetadataValueRest("Title Text")));

        context.setCurrentUser(eperson);
        context.restoreAuthSystemState();

        // User doesn't have add permission on the collection.
        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(post("/api/core/collections")
            .content(mapper.writeValueAsBytes(collectionRest))
            .param("parent", parentCommunity.getID().toString())
            .contentType(contentType))
                            .andExpect(status().isForbidden());

    }

    @Test
    public void deleteCollectionEpersonWithDeleteRightsTest() throws Exception {
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

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunityChild1)
                                           .withName("Collection 1")
                                           .build();


        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, col1, Constants.DELETE, eperson);
        authorizeService.addPolicy(context, col1, Constants.WRITE, eperson);

        String token = getAuthToken(eperson.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/core/collections/" + col1.getID().toString())
                           .param("embed", CollectionMatcher.getEmbedsParameter()))

                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                            CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                                 col1.getHandle())
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                            Matchers.containsString("/api/core/collections")));
        getClient(token).perform(delete("/api/core/collections/" + col1.getID().toString()))
                        .andExpect(status().isNoContent())
        ;
        getClient(token).perform(get("/api/core/collections/" + col1.getID().toString()))
                        .andExpect(status().isNotFound())
        ;

        authorizeService.removePoliciesActionFilter(context, eperson, Constants.DELETE);
        authorizeService.removePoliciesActionFilter(context, eperson, Constants.WRITE);

    }

    @Test
    public void updateCollectionEpersonWithWriteRightsTest() throws Exception {
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

        getClient().perform(get("/api/core/collections/" + col1.getID().toString())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/collections")))
        ;


        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, col1, Constants.WRITE, eperson);

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        ObjectMapper mapper = new ObjectMapper();

        CollectionRest collectionRest = converter.toRest(col1, Projection.DEFAULT);

        collectionRest.setMetadata(new MetadataRest()
                .put("dc.title", new MetadataValueRest("Electronic theses and dissertations")));

        getClient(token).perform(put("/api/core/collections/" + col1.getID().toString())
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsBytes(collectionRest)))
                        .andExpect(status().isOk())
        ;

        getClient().perform(get("/api/core/collections/" + col1.getID().toString())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CollectionMatcher.matchCollectionEntryFullProjection("Electronic theses and dissertations",
                                                              col1.getID(), col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/core/collections")))
        ;

        authorizeService.removePoliciesActionFilter(context, eperson, Constants.WRITE);
    }

    public void patchCollectionMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchCollectionMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/collections/" + col.getID(), expectedStatus);
    }

    @Test
    public void createTestInvalidParentCommunityUUIDBadRequestException() throws Exception {
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();

        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest = new CollectionRest();
        // We send a name but the created collection should set this to the title
        collectionRest.setName("Collection");

        collectionRest.setMetadata(new MetadataRest()
                                       .put("dc.description",
                                            new MetadataValueRest("<p>Some cool HTML code here</p>"))
                                       .put("dc.description.abstract",
                                            new MetadataValueRest("top-level community created via the REST API"))
                                       .put("dc.description.tableofcontents",
                                            new MetadataValueRest("<p>HTML News</p>"))
                                       .put("dc.rights",
                                            new MetadataValueRest("Custom Copyright Text"))
                                       .put("dc.title",
                                            new MetadataValueRest("Title Text")));

        String authToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(authToken).perform(post("/api/core/collections")
                                         .content(mapper.writeValueAsBytes(collectionRest))
                                         .param("parent", "123")
                                         .contentType(contentType))
                            .andExpect(status().isBadRequest());

    }


    @Test
    public void createTestWithoutParentCommunityUUIDBadRequestException() throws Exception {
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withLogo("ThisIsSomeDummyText")
                                          .build();

        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest = new CollectionRest();
        // We send a name but the created collection should set this to the title
        collectionRest.setName("Collection");

        collectionRest.setMetadata(new MetadataRest()
                                       .put("dc.description",
                                            new MetadataValueRest("<p>Some cool HTML code here</p>"))
                                       .put("dc.description.abstract",
                                            new MetadataValueRest("top-level community created via the REST API"))
                                       .put("dc.description.tableofcontents",
                                            new MetadataValueRest("<p>HTML News</p>"))
                                       .put("dc.rights",
                                            new MetadataValueRest("Custom Copyright Text"))
                                       .put("dc.title",
                                            new MetadataValueRest("Title Text")));

        String authToken = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(authToken).perform(post("/api/core/collections")
                                         .content(mapper.writeValueAsBytes(collectionRest))
                                         .contentType(contentType))
                            .andExpect(status().isBadRequest());

    }


    @Test
    public void findAllCollectionsWithMultilanguageTitlesTest() throws Exception {

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
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withNameForLanguage("Collection 1", "en")
                                           .withNameForLanguage("Col 1", "fr")
                                           .withNameForLanguage("Coll 1", "de")
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child2).withName("Collection 2").build();


        getClient().perform(get("/api/core/collections")
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntryFullProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle()),
                       CollectionMatcher.matchCollectionEntryFullProjection(col2.getName(), col2.getID(),
                                                                            col2.getHandle())
                   )))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20,
                                                                                                1, 2)));
    }

    @Test
    public void projectonLevelTest() throws Exception {
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
        Community child1child = CommunityBuilder.createSubCommunity(context, child1)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withLogo("TestingContentForLogo")
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1child).withName("Collection 2").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + col1.getID())
                            .param("projection", "level")
                            .param("embedLevelDepth", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", CollectionMatcher.matchCollectionEntry(col1.getName(),
                                                                                   col1.getID(),
                                                                                   col1.getHandle())))
                   // .exists() makes sure that the embed is there, but it could be empty
                   .andExpect(jsonPath("$._embedded.mappedItems").exists())
                   // .isEmpty() makes sure that the embed is there, but that there's no actual data
                   .andExpect(jsonPath("$._embedded.mappedItems._embedded.mappedItems").isEmpty())
                   .andExpect(jsonPath("$._embedded.parentCommunity",
                                       CommunityMatcher.matchCommunityEntry(child1.getName(),
                                                                            child1.getID(),
                                                                            child1.getHandle())))
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.parentCommunity._embedded.subcommunities").doesNotExist())
                   .andExpect(jsonPath("$._embedded.logo", Matchers.not(Matchers.empty())))
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.logo._embedded.format").doesNotExist());

        // Need this admin call for the AdminGroup embed in the Parentcommunity
        getClient(token).perform(get("/api/core/collections/" + col1.getID())
                            .param("projection", "level")
                            .param("embedLevelDepth", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", CollectionMatcher.matchCollectionEntry(col1.getName(),
                                                                                   col1.getID(),
                                                                                   col1.getHandle())))
                   // .exists() makes sure that the embed is there, but it could be empty
                   .andExpect(jsonPath("$._embedded.mappedItems").exists())
                   // .isEmpty() makes sure that the embed is there, but that there's no actual data
                   .andExpect(jsonPath("$._embedded.mappedItems._embedded.mappedItems").isEmpty())
                   .andExpect(jsonPath("$._embedded.parentCommunity",
                                       CommunityMatcher.matchCommunityEntry(child1.getName(),
                                                                            child1.getID(),
                                                                            child1.getHandle())))
                   // .exists() makes sure that the embed is there, but it could be empty
                   .andExpect(jsonPath("$._embedded.parentCommunity._embedded.subcommunities").exists())
                   .andExpect(jsonPath("$._embedded.parentCommunity._embedded.subcommunities._embedded.subcommunities",
                                       Matchers.contains(CommunityMatcher.matchCommunityEntry(child1child.getID(),
                                                                                              child1child.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.parentCommunity._embedded.subcommunities" +
                                           "._embedded.subcommunities[0]._embedded.collections._embedded.collections",
                                       Matchers.contains(CollectionMatcher.matchCollectionEntry(col2.getName(),
                                                                                                col2.getID(),
                                                                                                col2.getHandle())
                   )))
                   // .doesNotExist() makes sure that this section is not embedded, it's not there at all
                   .andExpect(jsonPath("$._embedded.parentCommunity._embedded.subcommunities" +
                                           "._embedded.subcommunities[0]._embedded.collections._embedded" +
                                           ".collections[0]._embedded.logo").doesNotExist())
                   .andExpect(jsonPath("$._embedded.logo", Matchers.not(Matchers.empty())))
                   // .exists() makes sure that the embed is there, but it could be empty
                   .andExpect(jsonPath("$._embedded.logo._embedded.format").exists());
    }

    @Test
    public void projectonLevelEmbedLevelDepthHigherThanEmbedMaxBadRequestTest() throws Exception {
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
        Community child1child = CommunityBuilder.createSubCommunity(context, child1)
                                                .withName("Sub Community Two")
                                                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withLogo("TestingContentForLogo")
                                           .build();

        getClient().perform(get("/api/core/collections/" + col1.getID())
                                .param("projection", "level")
                                .param("embedLevelDepth", "100"))
                   .andExpect(status().isBadRequest());
    }
    @Test
    public void projectonLevelEmbedLevelDepthNotPresentBadRequestTest() throws Exception {
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
        Community child1child = CommunityBuilder.createSubCommunity(context, child1)
                                                .withName("Sub Community Two")
                                                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withLogo("TestingContentForLogo")
                                           .build();

        getClient().perform(get("/api/core/collections/" + col1.getID())
                                .param("projection", "level"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void testHiddenMetadataForAonymousUser() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withProvenance("Provenance Data")
                                           .build();

        context.restoreAuthSystemState();


        getClient().perform(get("/api/core/collections/" + col1.getID())
                                         .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", CollectionMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", CollectionMatcher.matchProperties(col1.getName(),
                                                                                   col1.getID(),
                                                                                   col1.getHandle())))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Collection 1")))
                   .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")));

    }


    @Test
    public void testHiddenMetadataForAdminUser() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withProvenance("Provenance Data")
                                           .build();



        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + col1.getID())
                                         .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", CollectionMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", CollectionMatcher.matchProperties(col1.getName(),
                                                                              col1.getID(),
                                                                              col1.getHandle())))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Collection 1")))
                        .andExpect(jsonPath("$.metadata",
                                            matchMetadata("dc.description.provenance", "Provenance Data")));
    }

    @Test
    public void testHiddenMetadataForUserWithWriteRights() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withProvenance("Provenance Data")
                                           .build();



        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withUser(eperson)
                             .withAction(WRITE)
                             .withDspaceObject(col1)
                             .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + col1.getID())
                                         .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", CollectionMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", CollectionMatcher.matchProperties(col1.getName(),
                                                                              col1.getID(),
                                                                              col1.getHandle())))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Collection 1")))
                        .andExpect(jsonPath("$.metadata.['dc.description.provenance']").doesNotExist());

    }
}
