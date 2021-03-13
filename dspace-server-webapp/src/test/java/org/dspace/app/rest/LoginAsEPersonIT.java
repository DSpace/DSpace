/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class LoginAsEPersonIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    @Before
    public void setup() {
        configurationService.setProperty("webui.user.assumelogin", true);
    }

    @Test
    public void loggedInUserRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                    .param("projection", "full"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eperson", EPersonMatcher.matchEPersonOnEmail(admin.getEmail())));


    }
    @Test
    public void loggedInAsOtherUserRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                    .param("projection", "full")
                                    .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.eperson",
                                            EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));


    }

    @Test
    public void loggedInAsOtherUserNotAUuidInHeaderBadRequestRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .param("projection", "full")
                                     .header("X-On-Behalf-Of", "not-a-uuid"))
                        .andExpect(status().isBadRequest());


    }

    @Test
    public void loggedInAsOtherUserWrongUuidInHeaderBadRequestRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .param("projection", "full")
                                     .header("X-On-Behalf-Of", UUID.randomUUID()))
                        .andExpect(status().isBadRequest());


    }

    @Test
    public void loggedInAsOtherUserNoPermissionForbiddenRetrievalTest() throws Exception {


        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .param("projection", "full")
                                     .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isForbidden());


    }


    @Test
    public void loggedInUserPropertyFalseTest() throws Exception {
        configurationService.setProperty("webui.user.assumelogin", false);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isBadRequest());
    }


    @Test
    public void loggedInUserOtherAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson testEperson = EPersonBuilder.createEPerson(context).withEmail("loginasuseradmin@test.com").build();


        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        groupService.addMember(context, adminGroup, testEperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                     .header("X-On-Behalf-Of", testEperson.getID()))
                        .andExpect(status().isBadRequest());

    }

    /**
     * This test will try to create an empty Workspace item whilst using the LoginOnBehalfOf feature
     * It'll then check that the submitter of that workspace item is indeed the eperson that was being
     * impersonated by the loginOnBehalfOf feature
     * @throws Exception
     */
    @Test
    public void createEmptyWorkspaceItemLoginOnBehalfOfCheckSubmitterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        // create a workspaceitem explicitly in the col1
        MvcResult mvcResult = getClient(authToken).perform(post("/api/submission/workspaceitems")
                                                               .param("owningCollection", col1.getID().toString())
                                                               .header("X-On-Behalf-Of", eperson.getID())
                                                               .contentType(org.springframework
                                                                                .http.MediaType.APPLICATION_JSON))
                                                  .andExpect(status().isCreated())
                                                  .andExpect(jsonPath("$._embedded.collection.id",
                                                                      is(col1.getID().toString()))).andReturn();

        ObjectMapper mapper = new ObjectMapper();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String workspaceItemId = String.valueOf(map.get("id"));

        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItemId))
                            .andExpect(jsonPath("$._embedded.submitter", EPersonMatcher.matchProperties(eperson)));
    }

    @Test
    /**
     * Test claiming of a pool task with the LoginOnBehalfOf header. Thus checking that an admin can impersonate
     * an eperson to claim a pooltask and checking later on that the owner of this claimedTask is indeed
     * the reviwer
     *
     * @throws Exception
     */
    public void claimTaskLoginOnBehalfOfTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. a reviewer
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                                         .withEmail("reviewer@example.com")
                                         .withPassword(password)
                                         .build();

        //2. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer).build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password)
                                          .build();

        context.setCurrentUser(submitter);

        //4. a pool task
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                           .withSubject("ExtraEntry")
                                           .build();
        XmlWorkflowItem witem = poolTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/workflow/claimedtasks")
                 .header("X-On-Behalf-Of", reviewer.getID())
                 .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                 .content("/api/workflow/pooltasks/" + poolTask.getID()))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))));

        // verify that the pool task no longer exists
        getClient(authToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
                                .andExpect(status().isNotFound());

        // verify that the task has been claimed
        getClient(authToken).perform(get("/api/workflow/claimedtasks/search/findByUser")
                                             .param("uuid", reviewer.getID().toString()))
                                                           .andExpect(status().isOk())
                                                           .andExpect(jsonPath("$._embedded.claimedtasks",
                                                                               Matchers.contains(
                                    Matchers.allOf(
                                        hasJsonPath("$._links.self.href",
                                                    Matchers.containsString("/api/workflow/claimedtasks/")),
                                        hasJsonPath("$.type", Matchers.is("claimedtask")),
                                        hasJsonPath("$._embedded.owner",
                                                    Matchers.is(EPersonMatcher.matchEPersonOnEmail(
                                                        reviewer.getEmail()))),
                                        hasJsonPath("$._embedded.workflowitem",
                                                    Matchers.is(WorkflowItemMatcher
                                                                    .matchItemWithTitleAndDateIssuedAndSubject(
                                                        witem, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                                    ))))
                                                           .andExpect(jsonPath("$._links.self.href",
                                                                Matchers.containsString("/api/workflow/claimedtasks")))
                                                           .andExpect(jsonPath("$.page.size", is(20)))
                                                           .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    /**
     * This test will try to delete an archived item whilst using the LoginOnBehalfOf feature to impersonate as a
     * normal eperson and thus requiring it to fail with a forbidden flag
     * @throws Exception
     */
    @Test
    public void deleteOneArchivedLoginOnBehalfOfNonAdminForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. One public item, one workspace item and one template item.
        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                     .withSubject("ExtraEntry")
                                     .build();

        //Add a bitstream to an item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem, is)
                                        .withName("Bitstream1")
                                        .withMimeType("text/plain")
                                        .build();
        }

        // Check publicItem creation
        getClient().perform(get("/api/core/items/" + publicItem.getID()))
                   .andExpect(status().isOk());

        // Check publicItem bitstream creation (shuold be stored in bundle)
        getClient().perform(get("/api/core/items/" + publicItem.getID() + "/bundles"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._links.self.href", Matchers
                       .containsString("/api/core/items/" + publicItem.getID() + "/bundles")));

        String token = getAuthToken(admin.getEmail(), password);

        //Delete public item
        getClient(token).perform(delete("/api/core/items/" + publicItem.getID())
                                     .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isForbidden());

    }

}
