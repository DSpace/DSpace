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
import static junit.framework.TestCase.assertEquals;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration Tests against the /api/core/communities endpoint (including any subpaths)
 */
public class CommunityRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConverterService converter;

    @Autowired
    CommunityService communityService;

    @Autowired
    AuthorizeService authorizeService;

    @Test
    public void createTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        // We send a name but the created community should set this to the title
        comm.setName("Test Top-Level Community");

        MetadataRest metadataRest = new MetadataRest();

        MetadataValueRest description = new MetadataValueRest();
        description.setValue("<p>Some cool HTML code here</p>");
        metadataRest.put("dc.description", description);

        MetadataValueRest abs = new MetadataValueRest();
        abs.setValue("Sample top-level community created via the REST API");
        metadataRest.put("dc.description.abstract", abs);

        MetadataValueRest contents = new MetadataValueRest();
        contents.setValue("<p>HTML News</p>");
        metadataRest.put("dc.description.tableofcontents", contents);

        MetadataValueRest copyright = new MetadataValueRest();
        copyright.setValue("Custom Copyright Text");
        metadataRest.put("dc.rights", copyright);

        MetadataValueRest title = new MetadataValueRest();
        title.setValue("Title Text");
        metadataRest.put("dc.title", title);

        comm.setMetadata(metadataRest);

        String authToken = getAuthToken(admin.getEmail(), password);

        // Capture the UUID of the created Community (see andDo() below)
        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        try {
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
                                    hasJsonPath("$.metadata", Matchers.allOf(
                                        matchMetadata("dc.description", "<p>Some cool HTML code here</p>"),
                                        matchMetadata("dc.description.abstract",
                                               "Sample top-level community created via the REST API"),
                                        matchMetadata("dc.description.tableofcontents", "<p>HTML News</p>"),
                                        matchMetadata("dc.rights", "Custom Copyright Text"),
                                        matchMetadata("dc.title", "Title Text")
                                        )
                                    )
                                )))
                                // capture "id" returned in JSON response
                                .andDo(result -> idRef
                                    .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));
        } finally {
            // Delete the created community (cleanup after ourselves!)
            CommunityBuilder.deleteCommunity(idRef.get());
        }
    }

    @Test
    public void createSubCommunityUnAuthorizedTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // Create a parent community to POST a new sub-community to
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        // We send a name but the created community should set this to the title
        comm.setName("Test Sub-Level Community");

        // Anonymous user tries to create a community.
        // Should fail because user is not authenticated. Error 401.
        getClient().perform(post("/api/core/communities")
            .content(mapper.writeValueAsBytes(comm))
            .param("parent", parentCommunity.getID().toString())
            .contentType(contentType))
                   .andExpect(status().isUnauthorized());

        // Non-admin Eperson tries to create a community.
        // Should fail because user doesn't have permissions. Error 403.
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/core/communities")
            .content(mapper.writeValueAsBytes(comm))
            .param("parent", parentCommunity.getID().toString())
            .contentType(contentType))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void createSubCommunityAuthorizedTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // Create a parent community to POST a new sub-community to
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        // ADD authorization on parent community
        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADD, eperson);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        // We send a name but the created community should set this to the title
        comm.setName("Test Sub-Level Community");

        comm.setMetadata(new MetadataRest()
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

        // Capture the UUID of the created Community (see andDo() below)
        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        try {
            getClient(authToken).perform(post("/api/core/communities")
                .content(mapper.writeValueAsBytes(comm))
                .param("parent", parentCommunity.getID().toString())
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
                                        )
                                    )
                                )))
                                // capture "id" returned in JSON response
                                .andDo(result -> idRef
                                    .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));
        } finally {
            // Delete the created community (cleanup after ourselves!)
            CommunityBuilder.deleteCommunity(idRef.get());
        }
    }

    @Test
    public void createUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        comm.setName("Test Top-Level Community");

        MetadataRest metadataRest = new MetadataRest();

        MetadataValueRest title = new MetadataValueRest();
        title.setValue("Title Text");
        metadataRest.put("dc.title", title);

        comm.setMetadata(metadataRest);

        context.restoreAuthSystemState();

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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities")
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                       CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                            parentCommunity.getHandle()),
                       CommunityMatcher
                           .matchCommunityEntry(child1.getName(), child1.getID(), child1.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
        ;
    }

    @Test
    public void findAllNoDuplicatesOnMultipleCommunityTitlesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<String> titles = Arrays.asList("First title", "Second title", "Third title", "Fourth title");
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName(titles.get(0))
                .withTitle(titles.get(1))
                .withTitle(titles.get(2))
                .withTitle(titles.get(3))
                .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();


        getClient().perform(get("/api/core/communities").param("size", "2").param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                        CommunityMatcher.matchCommunityEntryMultipleTitles(titles, parentCommunity.getID(),
                                parentCommunity.getHandle()),
                        CommunityMatcher.matchCommunityEntry(child1.getID(), child1.getHandle())
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(1)));
    }

    @Test
    public void findAllNoDuplicatesOnMultipleCommunityTitlesPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<String> titles = Arrays.asList("First title", "Second title", "Third title", "Fourth title");
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName(titles.get(0))
                                          .withTitle(titles.get(1))
                                          .withTitle(titles.get(2))
                                          .withTitle(titles.get(3))
                                          .build();
        Community childCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity).build();
        Community secondParentCommunity = CommunityBuilder.createCommunity(context).withName("testing").build();
        Community thirdParentCommunity = CommunityBuilder.createCommunity(context).withName("testingTitleTwo").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities").param("size", "2").param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                       CommunityMatcher.matchCommunityEntryMultipleTitles(titles, parentCommunity.getID(),
                                                                          parentCommunity.getHandle()),
                       CommunityMatcher.matchCommunityEntry(childCommunity.getID(), childCommunity.getHandle())
                       )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 2, 2, 4)));

        getClient().perform(get("/api/core/communities").param("size", "2").param("page", "1")
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                       CommunityMatcher.matchCommunityEntry(secondParentCommunity.getID(),
                                                            secondParentCommunity.getHandle()),
                       CommunityMatcher.matchCommunityEntry(thirdParentCommunity.getID(),
                                                            thirdParentCommunity.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(1, 2, 2, 4)));
    }


    @Test
    public void findAllNoNameCommunityIsReturned() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();

        getClient().perform(get("/api/core/communities")
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.communities", Matchers.contains(
                        CommunityMatcher.matchCommunityEntry(parentCommunity.getID(),
                                parentCommunity.getHandle())
                )))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findAllCommunitiesAreReturnedInCorrectOrder() throws Exception {
        // The hibernate query for finding all communities is "SELECT ... ORDER BY STR(dc_title.value)"
        // So the communities should be returned in alphabetical order

        context.turnOffAuthorisationSystem();

        List<String> orderedTitles = Arrays.asList("Abc", "Bcd", "Cde");

        Community community1 = CommunityBuilder.createCommunity(context)
            .withName(orderedTitles.get(0))
            .build();

        Community community2 = CommunityBuilder.createCommunity(context)
            .withName(orderedTitles.get(1))
            .build();

        Community community3 = CommunityBuilder.createCommunity(context)
            .withName(orderedTitles.get(2))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        MvcResult result = getClient().perform(get("/api/core/communities")).andReturn();
        String response = result.getResponse().getContentAsString();
        JSONArray communities = new JSONObject(response).getJSONObject("_embedded").getJSONArray("communities");
        List<String> responseTitles = StreamSupport.stream(communities.spliterator(), false)
                                        .map(JSONObject.class::cast)
                                        .map(x -> x.getString("name"))
                                        .collect(Collectors.toList());

        assertEquals(orderedTitles, responseTitles);
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities")
                                .param("size", "1")
                                .param("projection", "full"))
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
                                .param("page", "1")
                   .param("projection", "full"))
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

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", CommunityMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", CommunityMatcher.matchCommunityEntry(
                        parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle())));

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andExpect(jsonPath("$", CommunityMatcher.matchLinks(parentCommunity.getID())))
                .andExpect(jsonPath("$", CommunityMatcher.matchProperties(
                        parentCommunity.getName(), parentCommunity.getID(), parentCommunity.getHandle())));
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                   .param("projection", "full"))
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities/search/top")
                   .param("projection", "full"))
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities/search/subCommunities")
                .param("parent", parentCommunity.getID().toString())
                .param("projection", "full"))
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
                .param("parent", parentCommunityChild2.getID().toString())
                .param("projection", "full"))
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
                 .andExpect(status().isBadRequest());
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities/" + UUID.randomUUID())).andExpect(status().isNotFound());
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

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                   .param("projection", "full"))
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

        String token = getAuthToken(admin.getEmail(), password);

        ObjectMapper mapper = new ObjectMapper();

        CommunityRest communityRest = converter.toRest(parentCommunity, Projection.DEFAULT);

        communityRest.setMetadata(new MetadataRest()
                .put("dc.title", new MetadataValueRest("Electronic theses and dissertations")));

        context.restoreAuthSystemState();

        getClient(token).perform(put("/api/core/communities/" + parentCommunity.getID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsBytes(communityRest)))
                   .andExpect(status().isOk())
        ;

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CommunityMatcher.matchCommunityEntry("Electronic theses and dissertations",
                                                            parentCommunity.getID(),
                                                            parentCommunity.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
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

        String token = getAuthToken(admin.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                            CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                                 parentCommunity.getHandle())
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                            Matchers.containsString("/api/core/communities")))        ;
        getClient(token).perform(delete("/api/core/communities/" + parentCommunity.getID().toString()))
                        .andExpect(status().isNoContent())
        ;
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                        .andExpect(status().isNotFound())
        ;

        getClient(token).perform(get("/api/core/communities/" + parentCommunityChild1.getID().toString()))
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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                            CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                                 parentCommunity.getHandle())
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                            Matchers.containsString("/api/core/communities")))        ;
        getClient().perform(delete("/api/core/communities/" + parentCommunity.getID().toString()))
                        .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    public void deleteCommunityEpersonWithDeleteRightsTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, parentCommunity, Constants.DELETE, eperson);

        String token = getAuthToken(eperson.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                        .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                            CommunityMatcher.matchCommunityEntry(parentCommunity.getName(), parentCommunity.getID(),
                                                                 parentCommunity.getHandle())
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                            Matchers.containsString("/api/core/communities")))        ;
        getClient(token).perform(delete("/api/core/communities/" + parentCommunity.getID().toString()))
                        .andExpect(status().isNoContent())
        ;
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID().toString()))
                        .andExpect(status().isNotFound())
        ;

        authorizeService.removePoliciesActionFilter(context, eperson, Constants.DELETE);
    }

    @Test
    public void updateCommunityEpersonWithWriteRightsTest() throws Exception {
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

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                   .param("projection", "full"))
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

        ObjectMapper mapper = new ObjectMapper();

        CommunityRest communityRest = converter.toRest(parentCommunity, Projection.DEFAULT);

        communityRest.setMetadata(new MetadataRest()
                .put("dc.title", new MetadataValueRest("Electronic theses and dissertations")));

        context.setCurrentUser(eperson);
        authorizeService.addPolicy(context, parentCommunity, Constants.WRITE, eperson);

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(put("/api/core/communities/" + parentCommunity.getID().toString())
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsBytes(communityRest)))
                        .andExpect(status().isOk())
        ;

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID().toString())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.is(
                       CommunityMatcher.matchCommunityEntry("Electronic theses and dissertations",
                                                            parentCommunity.getID(),
                                                            parentCommunity.getHandle())
                   )))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/core/communities")))
        ;

        authorizeService.removePoliciesActionFilter(context, eperson, Constants.DELETE);

    }

    @Test
    public void patchCommunityMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchCommunityMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/communities/"
                + parentCommunity.getID(), expectedStatus);
    }

    @Test
    public void createTestInvalidParentCommunityBadRequest() throws Exception {
        context.turnOffAuthorisationSystem();

        ObjectMapper mapper = new ObjectMapper();
        CommunityRest comm = new CommunityRest();
        // We send a name but the created community should set this to the title
        comm.setName("Test Top-Level Community");

        MetadataRest metadataRest = new MetadataRest();

        MetadataValueRest description = new MetadataValueRest();
        description.setValue("<p>Some cool HTML code here</p>");
        metadataRest.put("dc.description", description);

        MetadataValueRest abs = new MetadataValueRest();
        abs.setValue("Sample top-level community created via the REST API");
        metadataRest.put("dc.description.abstract", abs);

        MetadataValueRest contents = new MetadataValueRest();
        contents.setValue("<p>HTML News</p>");
        metadataRest.put("dc.description.tableofcontents", contents);

        MetadataValueRest copyright = new MetadataValueRest();
        copyright.setValue("Custom Copyright Text");
        metadataRest.put("dc.rights", copyright);

        MetadataValueRest title = new MetadataValueRest();
        title.setValue("Title Text");
        metadataRest.put("dc.title", title);

        comm.setMetadata(metadataRest);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/core/communities")
                                         .param("parent", "123")
                                         .content(mapper.writeValueAsBytes(comm))
                                         .contentType(contentType))
                            .andExpect(status().isBadRequest());
    }
}
