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
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.CollectionConverter;
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
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.hamcrest.Matchers;
import org.junit.Before;
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
    MetadataValueService metadataValueService;
    @Autowired
    MetadataSchemaService metadataSchemaService;
    @Autowired
    MetadataFieldService metadataFieldService;
    @Autowired
    EntityTypeService entityTypeService;
    private EntityType publicationType;
    private EntityType journalType;
    private EntityType orgUnitType;
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        publicationType = entityTypeService.findByEntityType(context, "Publication");
        if (publicationType == null) {
            publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        }
        journalType = entityTypeService.findByEntityType(context, "Journal");
        if (journalType == null) {
            journalType = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        }
        orgUnitType = entityTypeService.findByEntityType(context, "OrgUnit");
        if (orgUnitType == null) {
            orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        }
        context.restoreAuthSystemState();
    }
    @Autowired
    GroupService groupService;

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
                   )));

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
    public void findAuthorizedCollectionAndMetadata() throws Exception {
        String entityType = "OrgUnit";
        Set<MetadataValue> metadataValueSet = new HashSet();
        try {
            //We turn off the authorization system in order to create the structure as defined below
            context.turnOffAuthorisationSystem();
            //** GIVEN **
            //1. A community-collection structure with one parent community with sub-community and one collection.
            parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withRelationshipType(entityType)
                    .withName("Collection 1")
                    .withSubmitterGroup(eperson)
                    .build();

            context.setCurrentUser(eperson);
            authorizeService.addPolicy(context, parentCommunity, Constants.ADD, eperson);
            context.restoreAuthSystemState();

            String token = getAuthToken(eperson.getEmail(), password);

            getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedAndMetadata")
                .param("metadata", "relationship.type")
                .param("metadataValue", entityType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(1)))
                .andExpect(jsonPath("$._embedded.collections[0].metadata['relationship.type'][0].value",
                    equalTo(entityType)));
        } finally {

        }
    }

    @Test
    public void findAuthorizedCollectionsAndMetadata() throws Exception {
        String entityType = "Journal";
        Set<MetadataValue> metadataValueSet = new HashSet();
        try {
            //We turn off the authorization system in order to create the structure as defined below
            context.turnOffAuthorisationSystem();
            //** GIVEN **
            //1. A community-collection structure with one parent community with sub-community and one collection.
            parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withRelationshipType(entityType)
                    .withName("Collection 1")
                    .withSubmitterGroup(eperson)
                    .build();
            Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withRelationshipType(entityType)
                    .withName("Collection 2")
                    .withSubmitterGroup(eperson)
                    .build();

            context.setCurrentUser(eperson);
            authorizeService.addPolicy(context, parentCommunity, Constants.ADD, eperson);

            context.restoreAuthSystemState();

            String token = getAuthToken(eperson.getEmail(), password);

            getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedAndMetadata")
                .param("metadata", "relationship.type")
                .param("metadatavalue", entityType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
                .andExpect(jsonPath("$._embedded.collections", containsInAnyOrder(
                    CollectionMatcher.matchCollection(col1),
                    CollectionMatcher.matchCollection(col2))));

        } finally {

        }
    }

    @Test
    public void findAuthorizedAllCollectionsAndMetadata() throws Exception {
        String entityType = "Journal";
        String entityType2 = "Publication";
        String entityType3 = "JournaIssued";
        Set<MetadataValue> metadataValueSet = new HashSet();
        try {
            //We turn off the authorization system in order to create the structure as defined below
            context.turnOffAuthorisationSystem();
            //** GIVEN **
            //1. A community-collection structure with one parent community with sub-community and one collection.
            parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

            Collection col1 =
                CollectionBuilder.createCollection(context, parentCommunity).withRelationshipType(entityType)
                        .withName("Collection 1").withSubmitterGroup(eperson).build();
            Collection col2 =
                CollectionBuilder.createCollection(context, parentCommunity).withRelationshipType(entityType2)
                        .withName("Collection 2").withSubmitterGroup(eperson).build();
            Collection col3 =
                    CollectionBuilder.createCollection(context, parentCommunity).withRelationshipType(entityType)
                            .withName("Collection 3").withSubmitterGroup(eperson).build();
            Collection colWithoutMetadata =
                CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 4").
                        withSubmitterGroup(eperson).build();

            context.setCurrentUser(eperson);
            authorizeService.addPolicy(context, parentCommunity, Constants.ADD, eperson);

            context.restoreAuthSystemState();

            String token = getAuthToken(eperson.getEmail(), password);

            getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedAndMetadata")
                .param("metadata", "relationship.type"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(3)))
                .andExpect(jsonPath("$._embedded.collections", containsInAnyOrder(
                    CollectionMatcher.matchCollection(col1),
                    CollectionMatcher.matchCollection(col2),
                    CollectionMatcher.matchCollection(col3))))
                .andExpect(jsonPath("$._embedded.collections",
                    not(contains(CollectionMatcher.matchCollection(colWithoutMetadata)))));

        } finally {

        }
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
    public void findSubmitAuthorizedAllCollectionsByCommunityWithQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String entityType = "Journal";
        String entityType2 = "Publication";

        parentCommunity = CommunityBuilder.createCommunity(context)
                                           .withName("Parent Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType(entityType)
                                           .withName("Test Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType(entityType)
                                           .withName("Publication Collection 2")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType(entityType2)
                                           .withName("Publication Collection 3")
                                           .withSubmitterGroup(eperson)
                                           .build();
        Collection colWithoutMetadata = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName(" Test Collection 4")
                                           .withSubmitterGroup(eperson)
                                           .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndMetadata")
                        .param("uuid", parentCommunity.getID().toString())
                        .param("metadata", "relationship.type")
                        .param("metadatavalue", entityType)
                        .param("query", "test"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(1)))
                .andExpect(jsonPath("$._embedded.collections", contains(CollectionMatcher.matchCollection(col1))))
                .andExpect(jsonPath("$._embedded.collections",
                        not(contains(CollectionMatcher.matchCollection(colWithoutMetadata)))));

        getClient(token).perform(get("/api/core/collections/search/findSubmitAuthorizedByCommunityAndMetadata")
                        .param("uuid", parentCommunity.getID().toString())
                        .param("metadata", "relationship.type")
                        .param("query", "publication"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", equalTo(2)))
                .andExpect(jsonPath("$._embedded.collections", containsInAnyOrder(
                                CollectionMatcher.matchCollection(col2),
                                CollectionMatcher.matchCollection(col3))))
                .andExpect(jsonPath("$._embedded.collections", not(containsInAnyOrder(
                                CollectionMatcher.matchCollection(col1),
                                CollectionMatcher.matchCollection(colWithoutMetadata))
                                )));
    }
}
