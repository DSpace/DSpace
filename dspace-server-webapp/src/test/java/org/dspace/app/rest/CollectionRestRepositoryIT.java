/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataNotEmpty;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataStringEndsWith;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class CollectionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    CollectionConverter collectionConverter;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ResourcePolicyService resoucePolicyService;

    @Autowired
    GroupService groupService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    CollectionService collectionService;

    private Community topLevelCommunityA;
    private Community subCommunityA;
    private Community communityB;
    private Community communityC;
    private Collection collectionA;
    private Collection collectionB;
    private Collection collectionC;

    private EPerson topLevelCommunityAAdmin;
    private EPerson subCommunityAAdmin;
    private EPerson collectionAAdmin;
    private EPerson submitter;


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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections")
                   .param("embed", CollectionMatcher.getEmbedsParameter()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle()),
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
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
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.not(
                       Matchers.contains(
                           CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
                                                                                col2.getHandle())
                       )
                   )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$.page.size", is(1)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$.page.number", is(0)))
                   ;

        getClient().perform(get("/api/core/collections")
                                .param("size", "1")
                                .param("page", "1")
                                   .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
                                                                            col2.getHandle())
                   )))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.not(
                       Matchers.contains(
                           CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                                col1.getHandle())
                       )
                   )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                              Matchers.containsString("/api/core/collections?"),
                              Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                   .andExpect(jsonPath("$.page.size", is(1)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$.page.number", is(1)));
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
                .andExpect(jsonPath("$", CollectionMatcher.matchSpecificEmbeds()))
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
    public void findOneCollectionFullProjectionTest() throws Exception {

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

        context.restoreAuthSystemState();


        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/collections/" + col1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", CollectionMatcher.matchCollectionEntryFullProjection(
                       col1.getName(), col1.getID(), col1.getHandle())));

        getClient().perform(get("/api/core/collections/" + col1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.not(CollectionMatcher.matchCollectionEntryFullProjection(
                       col1.getName(), col1.getID(), col1.getHandle()))));
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
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
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

        getClient().perform(get("/api/core/collections/search/findSubmitAuthorized"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist())
        ;

    }

    @Test
    public void findAuthorizedCollectionsTest() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson eperson2 = EPersonBuilder.createEPerson(context)
                                         .withEmail("eperson2@example.com")
                                         .withPassword(password)
                                         .build();

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
                                           .withName("Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection 2")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 3")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Group ChildGroupOfSubmitterGroup = GroupBuilder.createGroup(context)
                                                       .withName("Child group of submitters")
                                                       .withParent(col1.getSubmitters())
                                                       .addMember(eperson2)
                                                       .build();

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenEPerson2 = getAuthToken(eperson2.getEmail(), password);

        getClient(tokenEPerson).perform(get("/api/core/collections/search/findSubmitAuthorized"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                         CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenEPerson2).perform(get("/api/core/collections/search/findSubmitAuthorized"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                            CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle())
                            )))
                 .andExpect(jsonPath("$.page.totalElements", is(1)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                         CollectionMatcher.matchProperties(col2.getName(), col2.getID(), col2.getHandle()),
                         CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    public void findAuthorizedCollectionsWithQueryTest() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
                          .withEmail("eperson2@mail.com")
                          .withPassword(password)
                          .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Sample collection")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Test collection")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection of sample items")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col4 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Testing autocomplete in submission")
                                           .withSubmitterGroup(eperson2)
                                           .build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("query", "collection"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                         CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenEPerson).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("query", "COLLECTION"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                        CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                        CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                        )))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenEPerson).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("query", "test"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.page.totalElements", is(0)));

        getClient(tokenEPerson).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("query", "auto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)));

        String tokenEPerson2 = getAuthToken(eperson2.getEmail(), password);
        getClient(tokenEPerson2).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("query", "auto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                           CollectionMatcher.matchProperties(col4.getName(), col4.getID(), col4.getHandle())
                           )))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        getClient(tokenEPerson2).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("query", "testing auto"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.page.totalElements", is(0)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("query", "sample"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                           CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                           )))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("query", "items sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                           CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                           )))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("query", "test"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col2.getName(), col2.getID(), col2.getHandle()),
                         CollectionMatcher.matchProperties(col4.getName(), col4.getID(), col4.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findAuthorizedByCommunityWithQueryTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withAdminGroup(eperson).build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Sample collection")
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Test collection")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection of sample items")
                                           .build();
        Collection col4 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Testing autocomplete in submission")
                                           .build();

        context.restoreAuthSystemState();

        String tokenAdminParentCom = getAuthToken(eperson.getEmail(), password);
        getClient(tokenAdminParentCom).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                 .param("uuid", parentCommunity.getID().toString())
                 .param("query", "sample"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                         CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(tokenAdminParentCom).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                .param("uuid", child2.getID().toString())
                .param("query", "sample"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                           CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                           )))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
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

        getClient().perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                                .param("uuid", parentCommunity.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void findAuthorizedByCommunityAdminsTest() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson adminParentCom = EPersonBuilder.createEPerson(context)
                                               .withEmail("adminParentCom@mail.com")
                                               .withPassword(password)
                                               .build();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community")
                          .withAdminGroup(adminParentCom).build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                          .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community Two")
                          .build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col3 = CollectionBuilder.createCollection(context, child2).withName("Collection 3").build();

        context.restoreAuthSystemState();

        String tokenAdminParentCom = getAuthToken(adminParentCom.getEmail(), password);
        getClient(tokenAdminParentCom).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                 .param("uuid", parentCommunity.getID().toString()))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                         CollectionMatcher.matchProperties(col2.getName(), col2.getID(), col2.getHandle()),
                         CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)));

        getClient(tokenAdminParentCom).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                 .param("uuid", child1.getID().toString()))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                            CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle())
                            )))
                 .andExpect(jsonPath("$.page.totalElements", is(1)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                 .param("uuid", parentCommunity.getID().toString()))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                         CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                         CollectionMatcher.matchProperties(col2.getName(), col2.getID(), col2.getHandle()),
                         CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle())
                         )))
                 .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    public void findAuthorizedByCommunityWithoutUUIDTest() throws Exception {
        getClient().perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findAuthorizedByCommunityWithUnexistentUUIDTest() throws Exception {
        getClient().perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
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
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$", Matchers.not(
                       is(
                           CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + col1.getID().toString())
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/collections")))
        ;

        String token = getAuthToken(admin.getEmail(), password);

        ObjectMapper mapper = new ObjectMapper();

        CollectionRest collectionRest = collectionConverter.convert(col1, Projection.DEFAULT);

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
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(
                           "Electronic theses and dissertations", col1.getID(), col1.getHandle())
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
                            CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
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
                            CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
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

        AtomicReference<UUID> idRef = new AtomicReference<>();
        AtomicReference<UUID> idRefNoEmbeds = new AtomicReference<>();
        AtomicReference<String> handle = new AtomicReference<>();
        try {

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
                            .andExpect(jsonPath("$", CollectionMatcher.matchSpecificEmbeds()))
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
                                )))))
                            // capture "handle" returned in JSON response and check against the metadata
                            .andDo(result -> handle.set(
                                    read(result.getResponse().getContentAsString(), "$.handle")))
                            .andExpect(jsonPath("$",
                                hasJsonPath("$.metadata", Matchers.allOf(
                                    matchMetadataNotEmpty("dc.identifier.uri"),
                                    matchMetadataStringEndsWith("dc.identifier.uri", handle.get())
                                )
                            )))
                            .andDo(result -> idRef
                                    .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));;


        getClient(authToken).perform(post("/api/core/collections")
                .content(mapper.writeValueAsBytes(collectionRest))
                .param("parent", parentCommunity.getID().toString())
                .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andDo(result -> idRefNoEmbeds
                        .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));
        } finally {
            CollectionBuilder.deleteCollection(idRef.get());
            CollectionBuilder.deleteCollection(idRefNoEmbeds.get());
        }
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

        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        try {
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
                                )))))
                            .andDo(result -> idRef
                                    .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));
        } finally {
            CollectionBuilder.deleteCollection(idRef.get());
        }
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
                            CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
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
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/collections")))
        ;


        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, col1, Constants.WRITE, eperson);

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        ObjectMapper mapper = new ObjectMapper();

        CollectionRest collectionRest = collectionConverter.convert(col1, Projection.DEFAULT);

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
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(
                           "Electronic theses and dissertations", col1.getID(), col1.getHandle())
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


        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections")
                      .param("embed", CollectionMatcher.getEmbedsParameter()))

                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                            col1.getHandle()),
                       CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
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

        context.restoreAuthSystemState();

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

        context.restoreAuthSystemState();

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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + col1.getID())
                                .param("projection", "level"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void testHiddenMetadataForAnonymousUser() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        // use multiple metadata to hit the scenario of the bug DS-4487 related to concurrent modification
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withProvenance("Provenance Data")
                                           .withNameForLanguage("Col 1", "en")
                                           .build();

        context.restoreAuthSystemState();


        getClient().perform(get("/api/core/collections/" + col1.getID()))
                        .andExpect(status().isOk())
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
        // use multiple metadata to hit the scenario of the bug DS-4487 related to concurrent modification
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withProvenance("Provenance Data")
                                           .withNameForLanguage("Col 1", "en")
                                           .build();



        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + col1.getID()))
                        .andExpect(status().isOk())
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
        // use multiple metadata to hit the scenario of the bug DS-4487 related to concurrent modification
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withProvenance("Provenance Data")
                                           .withNameForLanguage("Col 1", "en")
                                           .build();



        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withUser(eperson)
                             .withAction(WRITE)
                             .withDspaceObject(col1)
                             .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + col1.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", CollectionMatcher.matchProperties(col1.getName(),
                                                                              col1.getID(),
                                                                              col1.getHandle())))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Collection 1")))
                        .andExpect(jsonPath("$.metadata.['dc.description.provenance']").doesNotExist());

    }

    @Test
    public void findAllWithHiddenMetadataTest() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                         .withName("Parent Community")
                         .build();
        // use multiple metadata to hit the scenario of the bug DS-4487 related to concurrent modification
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                         .withName("Collection 1")
                         .withProvenance("Provenance Test 1")
                         .withNameForLanguage("col1", "en")
                         .build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                         .withName("Collection 2")
                         .withProvenance("Provenance Test 2")
                         .withNameForLanguage("col2", "it")
                         .build();

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);

        getClient().perform(get("/api/core/collections")
                   .param("embed", CollectionMatcher.getEmbedsParameter()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                                col1.getHandle()),
                           CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
                                                                                col2.getHandle())
                           )))
                   .andExpect(jsonPath("$.metadata.['dc.description.provenance']").doesNotExist());

        getClient(tokenEPerson).perform(get("/api/core/collections")
                .param("embed", CollectionMatcher.getEmbedsParameter()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                        CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col1.getName(), col1.getID(),
                                                                             col1.getHandle()),
                        CollectionMatcher.matchCollectionEntrySpecificEmbedProjection(col2.getName(), col2.getID(),
                                                                             col2.getHandle())
                        )))
                .andExpect(jsonPath("$.metadata.['dc.description.provenance']").doesNotExist());

    }

    @Test
    public void findAuthorizedCollectionsPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
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
                                           .withName("Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection 2")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 3")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col4 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 4")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col5 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 5")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col6 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 6")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col7 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 7")
                                           .withSubmitterGroup(eperson)
                                           .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                 .param("page", "0")
                 .param("size", "2"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                            CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                            CollectionMatcher.matchProperties(col2.getName(), col2.getID(), col2.getHandle())
                            )))
                 .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                 .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                         Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                         Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                         Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                         Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                         Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                         Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                         Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                         Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                 .andExpect(jsonPath("$.page.size", is(2)))
                 .andExpect(jsonPath("$.page.totalPages", is(4)))
                 .andExpect(jsonPath("$.page.number", is(0)))
                 .andExpect(jsonPath("$.page.totalElements", is(7)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle()),
                           CollectionMatcher.matchProperties(col4.getName(), col4.getID(), col4.getHandle())
                           )))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(4)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(7)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("page", "2")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchProperties(col5.getName(), col5.getID(), col5.getHandle()),
                           CollectionMatcher.matchProperties(col6.getName(), col6.getID(), col6.getHandle())
                           )))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=3"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(4)))
                .andExpect(jsonPath("$.page.number", is(2)))
                .andExpect(jsonPath("$.page.totalElements", is(7)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("page", "1")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchProperties(col4.getName(), col4.getID(), col4.getHandle()),
                           CollectionMatcher.matchProperties(col5.getName(), col5.getID(), col5.getHandle()),
                           CollectionMatcher.matchProperties(col6.getName(), col6.getID(), col6.getHandle())
                           )))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$.page.size", is(3)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(7)));
    }

    @Test
    public void findAuthorizedCollectionsWithQueryAndPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Sample 1 collection")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Sample 2 collection")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Sample 3 collection")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col4 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Sample 4 collection")
                                           .build();

        Collection col5 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Sample 5 collection")
                                           .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("query", "sample")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.everyItem(
                        hasJsonPath("$.type", is("collection")))
                        ))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("query", "sample")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.everyItem(
                        hasJsonPath("$.type", is("collection")))
                        ))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorized")
                .param("query", "sample")
                .param("page", "2")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.everyItem(
                        hasJsonPath("$.type", is("collection")))
                        ))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/collections/search/findSubmitAuthorized?"),
                        Matchers.containsString("query=sample"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(2)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    public void findAuthorizedByCommunityPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withAdminGroup(eperson).build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Test collection 1")
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Test collection 2")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Test collection 3")
                                           .build();
        Collection col4 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Test collection 4")
                                           .build();
        Collection col5 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Test collection 5")
                                           .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                 .param("uuid", parentCommunity.getID().toString())
                 .param("page", "0")
                 .param("size", "2"))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                            CollectionMatcher.matchProperties(col1.getName(), col1.getID(), col1.getHandle()),
                            CollectionMatcher.matchProperties(col2.getName(), col2.getID(), col2.getHandle())
                            )))
                 .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                 .andExpect(jsonPath("$.page.size", is(2)))
                 .andExpect(jsonPath("$.page.totalPages", is(3)))
                 .andExpect(jsonPath("$.page.number", is(0)))
                 .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                .param("uuid", parentCommunity.getID().toString())
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchProperties(col3.getName(), col3.getID(), col3.getHandle()),
                           CollectionMatcher.matchProperties(col4.getName(), col4.getID(), col4.getHandle())
                           )))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                .param("uuid", parentCommunity.getID().toString())
                .param("page", "2")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                           CollectionMatcher.matchProperties(col5.getName(), col5.getID(), col5.getHandle())
                           )))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(2)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

    }

    @Test
    public void findAuthorizedByCommunityWithQueryPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Sample 1 collection")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Sample 2 collection")
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Sample 3 collection")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col4 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Sample 4 collection")
                                           .build();

        Collection col5 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Sample 5 collection")
                                           .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                .param("uuid", parentCommunity.getID().toString())
                .param("query", "sample")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections",  Matchers.everyItem(
                        hasJsonPath("$.type", is("collection")))
                        ))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                .param("uuid", parentCommunity.getID().toString())
                .param("query", "sample")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.everyItem(
                        hasJsonPath("$.type", is("collection")))
                        ))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(tokenAdmin).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunity")
                .param("uuid", parentCommunity.getID().toString())
                .param("query", "sample")
                .param("page", "2")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.collections", Matchers.everyItem(
                        hasJsonPath("$.type", is("collection")))
                        ))
                .andExpect(jsonPath("$._embedded.collections").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.number", is(2)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    public void findOneTestWithEmbedsNoPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Collection mappedCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Mapped Collection")
                                                       .build();

        Item item0 = ItemBuilder.createItem(context, collection).withTitle("Item 0").build();
        Item item1 = ItemBuilder.createItem(context, collection).withTitle("Item 1").build();
        Item item2 = ItemBuilder.createItem(context, collection).withTitle("Item 2").build();
        Item item3 = ItemBuilder.createItem(context, collection).withTitle("Item 3").build();
        Item item4 = ItemBuilder.createItem(context, collection).withTitle("Item 4").build();
        Item item5 = ItemBuilder.createItem(context, collection).withTitle("Item 5").build();
        Item item6 = ItemBuilder.createItem(context, collection).withTitle("Item 6").build();
        Item item7 = ItemBuilder.createItem(context, collection).withTitle("Item 7").build();
        Item item8 = ItemBuilder.createItem(context, collection).withTitle("Item 8").build();
        Item item9 = ItemBuilder.createItem(context, collection).withTitle("Item 9").build();

        collectionService.addItem(context, mappedCollection, item0);
        collectionService.addItem(context, mappedCollection, item1);
        collectionService.addItem(context, mappedCollection, item2);
        collectionService.addItem(context, mappedCollection, item3);
        collectionService.addItem(context, mappedCollection, item4);
        collectionService.addItem(context, mappedCollection, item5);
        collectionService.addItem(context, mappedCollection, item6);
        collectionService.addItem(context, mappedCollection, item7);
        collectionService.addItem(context, mappedCollection, item8);
        collectionService.addItem(context, mappedCollection, item9);

        collectionService.update(context, mappedCollection);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + mappedCollection.getID())
                                    .param("embed", "mappedItems"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", CollectionMatcher.matchCollection(mappedCollection)))
                   .andExpect(jsonPath("$._embedded.mappedItems._embedded.mappedItems", Matchers.containsInAnyOrder(
                           ItemMatcher.matchItemProperties(item0),
                           ItemMatcher.matchItemProperties(item1),
                           ItemMatcher.matchItemProperties(item2),
                           ItemMatcher.matchItemProperties(item3),
                           ItemMatcher.matchItemProperties(item4),
                           ItemMatcher.matchItemProperties(item5),
                           ItemMatcher.matchItemProperties(item6),
                           ItemMatcher.matchItemProperties(item7),
                           ItemMatcher.matchItemProperties(item8),
                           ItemMatcher.matchItemProperties(item9)
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/core/collections/" + mappedCollection.getID())))
                   .andExpect(jsonPath("$._embedded.mappedItems.page.size", is(20)))
                   .andExpect(jsonPath("$._embedded.mappedItems.page.totalElements", is(10)));
    }


    @Test
    public void findOneTestWithEmbedsWithPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Collection mappedCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Mapped Collection")
                                                       .build();

        List<Item> items = new ArrayList();
        // Hibernate 5.x's org.hibernate.dialect.H2Dialect sorts UUIDs as if they are Strings.
        // So, we must compare UUIDs as if they are strings.
        // In Hibernate 6, the H2Dialect has been updated with native UUID type support, at which point
        // we'd need to update the below comparator to compare them as java.util.UUID (which sorts based on RFC 4412).
        Comparator<Item> compareByUUID = Comparator.comparing(i -> i.getID().toString());

        Item item0 = ItemBuilder.createItem(context, collection).withTitle("Item 0").build();
        items.add(item0);
        Item item1 = ItemBuilder.createItem(context, collection).withTitle("Item 1").build();
        items.add(item1);
        Item item2 = ItemBuilder.createItem(context, collection).withTitle("Item 2").build();
        items.add(item2);
        Item item3 = ItemBuilder.createItem(context, collection).withTitle("Item 3").build();
        items.add(item3);
        Item item4 = ItemBuilder.createItem(context, collection).withTitle("Item 4").build();
        items.add(item4);
        Item item5 = ItemBuilder.createItem(context, collection).withTitle("Item 5").build();
        items.add(item5);
        Item item6 = ItemBuilder.createItem(context, collection).withTitle("Item 6").build();
        items.add(item6);
        Item item7 = ItemBuilder.createItem(context, collection).withTitle("Item 7").build();
        items.add(item7);
        Item item8 = ItemBuilder.createItem(context, collection).withTitle("Item 8").build();
        items.add(item8);
        Item item9 = ItemBuilder.createItem(context, collection).withTitle("Item 9").build();
        items.add(item9);

        // sort items list by UUID (as Items will come back ordered by UUID)
        items.sort(compareByUUID);

        collectionService.addItem(context, mappedCollection, item0);
        collectionService.addItem(context, mappedCollection, item1);
        collectionService.addItem(context, mappedCollection, item2);
        collectionService.addItem(context, mappedCollection, item3);
        collectionService.addItem(context, mappedCollection, item4);
        collectionService.addItem(context, mappedCollection, item5);
        collectionService.addItem(context, mappedCollection, item6);
        collectionService.addItem(context, mappedCollection, item7);
        collectionService.addItem(context, mappedCollection, item8);
        collectionService.addItem(context, mappedCollection, item9);

        collectionService.update(context, mappedCollection);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + mappedCollection.getID())
                                    .param("embed", "mappedItems")
                                    .param("embed.size", "mappedItems=5"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", CollectionMatcher.matchCollection(mappedCollection)))
                   .andExpect(jsonPath("$._embedded.mappedItems._embedded.mappedItems",
                       Matchers.containsInRelativeOrder(
                           ItemMatcher.matchItemProperties(items.get(0)),
                           ItemMatcher.matchItemProperties(items.get(1)),
                           ItemMatcher.matchItemProperties(items.get(2)),
                           ItemMatcher.matchItemProperties(items.get(3)),
                           ItemMatcher.matchItemProperties(items.get(4))
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/core/collections/" + mappedCollection.getID())))
                   .andExpect(jsonPath("$._embedded.mappedItems.page.size", is(5)))
                   .andExpect(jsonPath("$._embedded.mappedItems.page.totalElements", is(10)));
    }


    @Test
    public void findOneTestWithEmbedsWithInvalidPageSize() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        Collection mappedCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Mapped Collection")
                                                       .build();

        Item item0 = ItemBuilder.createItem(context, collection).withTitle("Item 0").build();
        Item item1 = ItemBuilder.createItem(context, collection).withTitle("Item 1").build();
        Item item2 = ItemBuilder.createItem(context, collection).withTitle("Item 2").build();
        Item item3 = ItemBuilder.createItem(context, collection).withTitle("Item 3").build();
        Item item4 = ItemBuilder.createItem(context, collection).withTitle("Item 4").build();
        Item item5 = ItemBuilder.createItem(context, collection).withTitle("Item 5").build();
        Item item6 = ItemBuilder.createItem(context, collection).withTitle("Item 6").build();
        Item item7 = ItemBuilder.createItem(context, collection).withTitle("Item 7").build();
        Item item8 = ItemBuilder.createItem(context, collection).withTitle("Item 8").build();
        Item item9 = ItemBuilder.createItem(context, collection).withTitle("Item 9").build();

        collectionService.addItem(context, mappedCollection, item0);
        collectionService.addItem(context, mappedCollection, item1);
        collectionService.addItem(context, mappedCollection, item2);
        collectionService.addItem(context, mappedCollection, item3);
        collectionService.addItem(context, mappedCollection, item4);
        collectionService.addItem(context, mappedCollection, item5);
        collectionService.addItem(context, mappedCollection, item6);
        collectionService.addItem(context, mappedCollection, item7);
        collectionService.addItem(context, mappedCollection, item8);
        collectionService.addItem(context, mappedCollection, item9);

        collectionService.update(context, mappedCollection);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + mappedCollection.getID())
                                    .param("embed", "mappedItems")
                                    .param("embed.size", "mappedItems=invalidPage"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", CollectionMatcher.matchCollection(mappedCollection)))
                   .andExpect(jsonPath("$._embedded.mappedItems._embedded.mappedItems", Matchers.containsInAnyOrder(
                           ItemMatcher.matchItemProperties(item0),
                           ItemMatcher.matchItemProperties(item1),
                           ItemMatcher.matchItemProperties(item2),
                           ItemMatcher.matchItemProperties(item3),
                           ItemMatcher.matchItemProperties(item4),
                           ItemMatcher.matchItemProperties(item5),
                           ItemMatcher.matchItemProperties(item6),
                           ItemMatcher.matchItemProperties(item7),
                           ItemMatcher.matchItemProperties(item8),
                           ItemMatcher.matchItemProperties(item9)
                   )))
                   .andExpect(jsonPath("$._links.self.href",
                                       Matchers.containsString("/api/core/collections/" + mappedCollection.getID())))
                   .andExpect(jsonPath("$._embedded.mappedItems.page.size", is(20)))
                   .andExpect(jsonPath("$._embedded.mappedItems.page.totalElements", is(10)));
    }


    private void setUpAuthorizedSearch() throws Exception {
        super.setUp();

        /**
         * The common Community/Collection structure for the AuthorizedSearch tests:
         *
         * topLevelCommunityA
         * └── subCommunityA
         *     └── collectionA
         */
        context.turnOffAuthorisationSystem();

        topLevelCommunityAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("topLevelCommunityAAdmin@my.edu")
            .withPassword(password)
            .build();
        topLevelCommunityA = CommunityBuilder.createCommunity(context)
            .withName("The name of this community is topLevelCommunityA")
            .withAdminGroup(topLevelCommunityAAdmin)
            .build();

        subCommunityAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("subCommunityAAdmin@my.edu")
            .withPassword(password)
            .build();
        subCommunityA = CommunityBuilder.createCommunity(context)
            .withName("The name of this sub-community is subCommunityA")
            .withAdminGroup(subCommunityAAdmin)
            .addParentCommunity(context, topLevelCommunityA)
            .build();

        submitter = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("submitter@my.edu")
            .withPassword(password)
            .build();
        collectionAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("collectionAAdmin@my.edu")
            .withPassword(password)
            .build();
        collectionA = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("The name of this collection is collectionA")
            .withAdminGroup(collectionAAdmin)
            .withSubmitterGroup(submitter)
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", "true");
    }

    @Test
    public void testAdminAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .withAdminGroup(admin)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is named topLevelCommunityC")
            .build();
        collectionB = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        // Verify the site admin gets all collections
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle()),
                CollectionMatcher.matchProperties(collectionC.getName(), collectionC.getID(), collectionC.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));
    }

    @Test
    public void testCommunityAdminAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        /**
         * The Community/Collection structure for this test:
         *
         * topLevelCommunityA
         * ├── subCommunityA
         * |   └── collectionA
         * └── collectionB
         * communityB
         * communityC
         * └── collectionC
         */
        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .withAdminGroup(topLevelCommunityAAdmin)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is named topLevelCommunityC")
            .build();
        collectionB = CollectionBuilder.createCollection(context, topLevelCommunityA)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(topLevelCommunityAAdmin.getEmail(), password);

        // Verify the community admin gets all the communities he's admin for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testSubCommunityAdminAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, subCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(subCommunityAAdmin.getEmail(), password);

        // Verify the subcommunity admin gets all the communities he's admin for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testCollectionAdminAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, subCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB is a very original name")
            .withAdminGroup(collectionAAdmin)
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(collectionAAdmin.getEmail(), password);

        // Verify the collection admin gets all the communities he's admin for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testSubmitterAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, subCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB is a very original name")
            .withSubmitterGroup(submitter)
            .withAdminGroup(collectionAAdmin)
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(submitter.getEmail(), password);

        // Verify the submitter doesn't have any matches for collections
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testSubGroupOfAdminGroupAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("adminSubGroup")
            .withParent(groupService.findByName(context, Group.ADMIN))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        collectionB = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the site admins' subgroups members get all collections
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle()),
                CollectionMatcher.matchProperties(collectionC.getName(), collectionC.getID(), collectionC.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));
    }

    @Test
    public void testSubGroupOfCommunityAdminGroupAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("communityAdminSubGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunityA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(communityB)
            .withAction(Constants.ADMIN)
            .withGroup(groupService.findByName(context, "COMMUNITY_" + topLevelCommunityA.getID() + "_ADMIN"))
            .build();
        collectionB = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a community admin group gets all the collections he's admin for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroupAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("communityAdminSubGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunityA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(communityB)
            .withAction(Constants.ADMIN)
            .withGroup(groupService.findByName(context, "COMMUNITY_" + subCommunityA.getID() + "_ADMIN"))
            .build();
        collectionB = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a subcommunity admin group gets all the collections he's admin for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroupAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("collectionAdminSubGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB is a very original name")
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionB)
            .withAction(Constants.ADMIN)
            .withGroup(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_ADMIN"))
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a collection admin group gets all the collections he's admin for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionA.getName(), collectionA.getID(), collectionA.getHandle()),
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                CollectionMatcher.matchProperties(collectionB.getName(), collectionB.getID(), collectionB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testSubGroupOfSubmitterGroupAuthorizedSearch() throws Exception {
        setUpAuthorizedSearch();

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("collectionAdminSubGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB is a very original name")
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionB)
            .withAction(Constants.ADD)
            .withGroup(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_SUBMIT"))
            .build();
        collectionC = CollectionBuilder.createCollection(context, communityC)
            .withName("the last collection is collectionC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have any matches for collections
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/collections/search/findAdminAuthorized")
            .param("query", collectionC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").doesNotExist());
    }

    @Test
    public void testAdminAuthorizedSearchUnauthenticated() throws Exception {
        // Verify a non-authenticated user can't use this function
        getClient().perform(get("/api/core/collections/search/findAdminAuthorized"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void patchMetadataCheckReindexingTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        CollectionBuilder.createCollection(context, parentCommunity)
                         .withName("Collection 1")
                         .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("MyTest")
                                           .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/collections/search/findAdminAuthorized")
                             .param("query", "MyTest"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.collections", Matchers.contains(CollectionMatcher
                                       .matchProperties(col.getName(), col.getID(), col.getHandle())
                                       )))
                             .andExpect(jsonPath("$.page.totalElements", is(1)));

        List<Operation> updateTitle = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Name");
        updateTitle.add(new ReplaceOperation("/metadata/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);
        getClient(adminToken).perform(patch("/api/core/collections/" + col.getID())
                             .content(patchBody)
                             .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'][0].value", is("New Name")));

        getClient(adminToken).perform(get("/api/core/collections/search/findAdminAuthorized")
                             .param("query", "MyTest"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded").doesNotExist())
                             .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void removeColAdminGroupToCheckReindexingTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("MyTest")
                                           .withAdminGroup(eperson)
                                           .build();

        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/core/collections/search/findAdminAuthorized")
                               .param("query", "MyTest"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.collections", Matchers.contains(CollectionMatcher
                                          .matchProperties(col1.getName(), col1.getID(), col1.getHandle())
                                          )))
                               .andExpect(jsonPath("$.page.totalElements", is(1)));

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + col1.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(epersonToken).perform(get("/api/core/collections/search/findAdminAuthorized")
                               .param("query", "MyTest"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded").doesNotExist())
                               .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void addColAdminGroupToCheckReindexingTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("MyTest")
                                           .build();

        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/core/collections/search/findAdminAuthorized")
                               .param("query", "MyTest"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded").doesNotExist())
                               .andExpect(jsonPath("$.page.totalElements", is(0)));

        AtomicReference<UUID> idRef = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + col1.getID() + "/adminGroup")
                        .content(mapper.writeValueAsBytes(groupRest))
                        .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef.set(
                               UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/eperson/groups/" + idRef.get() + "/epersons")
                             .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                             .content(REST_SERVER_URL + "eperson/groups/" + eperson.getID()
                             ));

        getClient(epersonToken).perform(get("/api/core/collections/search/findAdminAuthorized")
                               .param("query", "MyTest"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.collections", Matchers.contains(CollectionMatcher
                                          .matchProperties(col1.getName(), col1.getID(), col1.getHandle())
                                          )))
                               .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findAuthorizedCollectionsByEntityType() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType journal = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunityA = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                  .withName("SubCommunity A")
                                                  .build();

        Community subCommunityB = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                  .withName("SubCommunity B")
                                                  .build();

        Collection col1 = CollectionBuilder.createCollection(context, subCommunityA)
                                           .withEntityType(journal.getLabel())
                                           .withName("Collection 1")
                                           .withAdminGroup(eperson)
                                           .build();

        Collection col2 = CollectionBuilder.createCollection(context, subCommunityB)
                                           .withEntityType(journal.getLabel())
                                           .withName("Collection 2")
                                           .withAdminGroup(eperson)
                                           .build();

        CollectionBuilder.createCollection(context, subCommunityA)
                         .withEntityType(publication.getLabel())
                         .withName("Collection 3")
                         .withAdminGroup(eperson)
                         .build();

        CollectionBuilder.createCollection(context, subCommunityA)
                         .withEntityType(journal.getLabel())
                         .withName("Collection 4")
                         .build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(ePersonToken).perform(get("/api/core/collections/search/findSubmitAuthorizedByEntityType")
                               .param("entityType", journal.getLabel()))
                               .andExpect(status().isOk())
                               .andExpect(content().contentType(contentType))
                               .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
                               .andExpect(jsonPath("$._embedded.collections", containsInAnyOrder(
                                          CollectionMatcher.matchCollection(col1),
                                          CollectionMatcher.matchCollection(col2)
                                          )));
    }

    @Test
    public void findSubmitAuthorizedByEntityTypeNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType journal = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType(journal.getLabel())
                         .withName("Collection 1")
                         .withAdminGroup(eperson)
                         .build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        String adminToken = getAuthToken(eperson.getEmail(), password);

        getClient(ePersonToken).perform(get("/api/core/collections/search/findSubmitAuthorizedByEntityType")
                               .param("entityType", "test"))
                               .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/core/collections/search/findSubmitAuthorizedByEntityType")
                             .param("entityType", "test"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findAuthorizedCollectionsByEntityTypeEmptyResponseTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(ePersonToken).perform(get("/api/core/collections/search/findSubmitAuthorizedByEntityType")
                               .param("entityType", publication.getLabel()))
                               .andExpect(status().isOk())
                               .andExpect(content().contentType(contentType))
                               .andExpect(jsonPath("$.page.totalElements", equalTo(0)));
    }

    @Test
    public void findAuthorizedCollectionsByEntityAndQueryType() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType journal = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunityA = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                  .withName("SubCommunity A")
                                                  .build();

        Community subCommunityB = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                  .withName("SubCommunity B")
                                                  .build();

        Collection col1 = CollectionBuilder.createCollection(context, subCommunityA)
                                           .withEntityType(journal.getLabel())
                                           .withName("Thesis Collection")
                                           .withAdminGroup(eperson)
                                           .build();

        CollectionBuilder.createCollection(context, subCommunityB)
                         .withEntityType(journal.getLabel())
                         .withName("Work Collection")
                         .withAdminGroup(eperson)
                         .build();

        CollectionBuilder.createCollection(context, subCommunityA)
                         .withEntityType(publication.getLabel())
                         .withName("Thesis")
                         .withAdminGroup(eperson)
                         .build();

        CollectionBuilder.createCollection(context, subCommunityA)
                         .withEntityType(journal.getLabel())
                         .withName("Collection 1")
                         .build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(ePersonToken).perform(get("/api/core/collections/search/findSubmitAuthorizedByEntityType")
                               .param("entityType", journal.getLabel())
                               .param("query", "Thesis"))
                               .andExpect(status().isOk())
                               .andExpect(content().contentType(contentType))
                               .andExpect(jsonPath("$.page.totalElements", equalTo(1)))
                               .andExpect(jsonPath("$._embedded.collections", contains(
                                          CollectionMatcher.matchCollection(col1)
                                          )));
    }

    @Test
    public void findSubmitAuthorizedCollectionsByCommunityAndEntityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community Two")
                                           .build();

        CollectionBuilder.createCollection(context, child1)
                         .withEntityType(publication.getLabel())
                         .withName("Test Collection 1")
                         .withSubmitterGroup(eperson)
                         .build();

        Collection col2 = CollectionBuilder.createCollection(context, child2)
                                           .withEntityType(publication.getLabel())
                                           .withName("Publication Collection 2")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withEntityType(publication.getLabel())
                                           .withName("Publication Collection 3")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection colWithoutEntity = CollectionBuilder.createCollection(context, child2)
                                           .withName(" Test Collection 4")
                                           .withSubmitterGroup(eperson)
                                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("uuid", child2.getID().toString())
                        .param("entityType", publication.getLabel()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
                        .andExpect(jsonPath("$._embedded.collections", Matchers.containsInAnyOrder(
                                   CollectionMatcher.matchCollection(col2),
                                   CollectionMatcher.matchCollection(col3))))
                        .andExpect(jsonPath("$._embedded.collections", not(containsInAnyOrder(
                                   CollectionMatcher.matchCollection(colWithoutEntity)))));
    }

    @Test
    public void findSubmitAuthorizedCollectionsByCommunityAndEntityWithQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType journal = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType(journal.getLabel())
                                           .withName("Test Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType(journal.getLabel())
                                           .withName("Publication Collection 2")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType(publication.getLabel())
                                           .withName("Publication Collection 3 Test")
                                           .withSubmitterGroup(eperson)
                                           .build();

        Collection colWithoutEntity = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Collection 4 Test")
                                                       .withSubmitterGroup(eperson)
                                                       .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("uuid", parentCommunity.getID().toString())
                        .param("entityType", journal.getLabel())
                        .param("query", "test"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(1)))
                .andExpect(jsonPath("$._embedded.collections", contains(CollectionMatcher.matchCollection(col1))))
                .andExpect(jsonPath("$._embedded.collections",not(contains(
                                    CollectionMatcher.matchCollection(colWithoutEntity)))));

        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("uuid", parentCommunity.getID().toString())
                        .param("entityType", publication.getLabel())
                        .param("query", "publication"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(1)))
                .andExpect(jsonPath("$._embedded.collections", contains(CollectionMatcher.matchCollection(col3))))
                .andExpect(jsonPath("$._embedded.collections", not(containsInAnyOrder(
                           CollectionMatcher.matchCollection(colWithoutEntity),
                           CollectionMatcher.matchCollection(col2)))));
    }

    @Test
    public void findSubmitAuthorizedAllCollectionsByCommunityAndEntityBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType(publication.getLabel())
                         .withName("Test Collection 1")
                         .withSubmitterGroup(eperson)
                         .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType"))
                        .andExpect(status().isBadRequest());

        // missing entityType param
        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("uuid", parentCommunity.getID().toString()))
                        .andExpect(status().isBadRequest());

        // missing community uuid param
        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("entityType", publication.getLabel()))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void findSubmitAuthorizedByCommunityAndEntityTypeNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        CollectionBuilder.createCollection(context, parentCommunity)
                         .withEntityType(publication.getLabel())
                         .withName("Test Collection 1")
                         .withSubmitterGroup(eperson)
                         .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("entityType", publication.getLabel())
                        .param("uuid", UUID.randomUUID().toString()))
                        .andExpect(status().isNotFound());

        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndEntityType")
                        .param("entityType", "test")
                        .param("uuid", parentCommunity.getID().toString()))
                        .andExpect(status().isNotFound());
    }

}
