/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Matchers;
import org.junit.Test;


/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */

public class GroupRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void createTest()
        throws Exception {

        // hold the id of the created workflow item
        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            GroupRest groupRest = new GroupRest();
            String groupName = "testGroup1";

            groupRest.setName(groupName);

            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(post("/api/eperson/groups")
                    .content(mapper.writeValueAsBytes(groupRest)).contentType(contentType))
                    .andExpect(status().isCreated())
                    .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            getClient(authToken).perform(get("/api/eperson/groups"))
                       //The status has to be 200 OK
                       .andExpect(status().isOk())
                       .andExpect(content().contentType(contentType))
                       .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                               GroupMatcher.matchGroupWithName(groupName),
                               GroupMatcher.matchGroupWithName("Administrator"),
                               GroupMatcher.matchGroupWithName("Anonymous"))));
        } finally {
            // remove the created group if any
            GroupBuilder.deleteGroup(idRef.get());
        }
    }

    @Test
    public void createUnauthauthorizedTest()
            throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        String groupName = "testGroupUnauth1";

        groupRest.setName(groupName);

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient().perform(post("/api/eperson/groups")
                .content(mapper.writeValueAsBytes(groupRest)).contentType(contentType))
                .andExpect(status().isUnauthorized());

    }

    @Test
    public void createForbiddenTest()
            throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        String groupName = "testGroupForbidden1";

        groupRest.setName(groupName);

        String authToken = getAuthToken(eperson.getEmail(), password);

        authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/eperson/groups")
                .content(mapper.writeValueAsBytes(groupRest)).contentType(contentType))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findAllTest() throws Exception {

        getClient().perform(get("/api/eperson/groups"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/eperson/groups"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   //The array of groups should have a size 2
                   .andExpect(jsonPath("$._embedded.groups", hasSize(2)))
                   // The default groups should consist of "Anonymous" and "Anonymous"
                   .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                       GroupMatcher.matchGroupWithName("Administrator"),
                       GroupMatcher.matchGroupWithName("Anonymous")
                   )))
        ;
    }

    @Test
    public void findAllPaginationTest() throws Exception {

        getClient().perform(get("/api/eperson/groups"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/eperson/groups"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.number", is(0)));
    }


    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String testGroupName = "Test group";
        Group group = GroupBuilder.createGroup(context)
                                  .withName(testGroupName)
                                  .build();

        String token = getAuthToken(admin.getEmail(), password);

        // When full projection is requested, response should include expected properties, links, and embeds.
        String generatedGroupId = group.getID().toString();
        String groupIdCall = "/api/eperson/groups/" + generatedGroupId;
        getClient(token).perform(get(groupIdCall).param("projection", "full"))
        //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", GroupMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", GroupMatcher.matchLinks(group.getID())))
                   .andExpect(jsonPath("$", Matchers.is(
                       GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                   )))
        ;

        getClient(token).perform(get("/api/eperson/groups"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient(token).perform(get("/api/eperson/groups/"  + generatedGroupId))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void readGroupAuthorizationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group = GroupBuilder.createGroup(context)
                                  .withName("Group1")
                                  .build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Group2")
                                   .addMember(eperson)
                                   .build();

        //Admin can access
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + group2.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                GroupMatcher.matchGroupEntry(group2.getID(), group2.getName())
                        )))
                        .andExpect(jsonPath("$", Matchers.not(
                                Matchers.is(
                                        GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                                )
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                        Matchers.containsString("/api/eperson/groups/" + group2.getID())));


        //People in group should be able to access token
        token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/eperson/groups/" + group2.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                GroupMatcher.matchGroupEntry(group2.getID(), group2.getName())
                        )))
                        .andExpect(jsonPath("$", Matchers.not(
                                Matchers.is(
                                        GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                                )
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                        Matchers.containsString("/api/eperson/groups/" + group2.getID())));
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        context.turnOffAuthorisationSystem();

        String testGroupName = "Test group";
        Group group = GroupBuilder.createGroup(context)
                                  .withName(testGroupName)
                                  .build();

        String generatedGroupId = group.getID().toString();
        String groupIdCall = "/api/eperson/groups/" + UUID.randomUUID();
        getClient().perform(get(groupIdCall))
                   //The status has to be 200 OK
                   .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void patchGroupMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchGroupMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context).withName("Group").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/eperson/groups/" + group.getID(), expectedStatus);
    }
}
