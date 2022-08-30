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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.matcher.ClaimedTaskMatcher;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.PoolTaskMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.matcher.WorkflowStepMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Test suite for the pooltasks and claimedtasks endpoints
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Maria Verdonck (Atmire) on 10/02/2020
 */
public class TaskRestRepositoriesIT extends AbstractControllerIntegrationTest {

    @Autowired
    private XmlWorkflowFactory xmlWorkflowFactory;

    @Test
    /**
     * Retrieve a specific pooltask
     *
     * @throws Exception
     */
    public void findOnePoolTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. create a normal user to use as reviewer
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

        //3. a workflow item
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        // the reviewer and the administrator can access the pooltask
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(PoolTaskMatcher.matchPoolTask(poolTask, "reviewstep"))))
                .andExpect(jsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                poolTask.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry"))))
        ;

        getClient(adminToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(PoolTaskMatcher.matchPoolTask(poolTask, "reviewstep"))))
                .andExpect(jsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                poolTask.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Verify that only authenticated user can retrieve a specific pooltask
     *
     * @throws Exception
     */
    public void findOnePoolUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //3. a workflow item
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, admin)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * Verify that an other reviewer cannot access a pooltask of a colleague
     *
     * @throws Exception
     */
    public void findOnePoolForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. create two normal users to use as reviewers
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
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
                .withWorkflowGroup(1, reviewer)
                .withWorkflowGroup(2, reviewer2).build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //3. a workflow item
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        // the reviewer2 cannot access the pool task of reviewer1
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
                .andExpect(status().isForbidden());

        // verify that the task exists
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(PoolTaskMatcher.matchPoolTask(poolTask, "reviewstep"))))
                .andExpect(jsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                poolTask.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Create three workflowitem in three different collections with different, partially overlapping reviewers, and
     * verify that each reviewer get the proper list of tasks
     *
     * @throws Exception
     */
    public void findByUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-subcommunity structure
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        //2. define two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        // 3. two collections at different level of our communities structure with different reviewers
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1, reviewer2).build();

        // reviewer2 and admin are only in the wf of one colletion
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer1, admin).build();

        //4. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //5. our workflow items
        XmlWorkflowItem witem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        XmlWorkflowItem witem2 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 2")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        XmlWorkflowItem witem3 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                .withTitle("Workflow Item 3")
                .withIssueDate("2017-10-18")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String authReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        getClient(authReviewer1).perform(get("/api/workflow/pooltasks/search/findByUser")
                    .param("uuid", reviewer1.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pooltasks", Matchers.containsInAnyOrder(
                        Matchers.allOf(
                                Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                                hasJsonPath("$._embedded.workflowitem",
                                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                                witem1, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                                ),
                       Matchers.allOf(
                                Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                                hasJsonPath("$._embedded.workflowitem",
                                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                                witem2, "Workflow Item 2", "2017-10-17", "ExtraEntry")))
                                        ),
                       Matchers.allOf(
                                Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                                hasJsonPath("$._embedded.workflowitem",
                                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                                witem3, "Workflow Item 3", "2017-10-18", "ExtraEntry")))
                                ))))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(3)));

        String authReviewer2 = getAuthToken(reviewer2.getEmail(), password);
        getClient(authReviewer2).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.containsInAnyOrder(
                    Matchers.allOf(
                             Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                             hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem1, "Workflow Item 1", "2017-10-17", "ExtraEntry")))),
                    Matchers.allOf(
                             Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                             hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem2, "Workflow Item 2", "2017-10-17", "ExtraEntry")))
                             ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        String authAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authAdmin).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", admin.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.containsInAnyOrder(
                    Matchers.allOf(
                             Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                             hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem3, "Workflow Item 3", "2017-10-18", "ExtraEntry")))
                             ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));

        // admins is also able to get the list of other users
        // reviewer1 has three tasks
        getClient(authAdmin).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(3)));

        // reviewer2 has two tasks
        getClient(authAdmin).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByUserForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-subcommunity structure
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        //2. define two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        // 3. two collections at different level of our communities structure with different reviewers
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1, reviewer2).build();

        // reviewer2 and admin are only in the wf of one colletion
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer1, admin).build();

        //4. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //5. our workflow items
        XmlWorkflowItem witem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        XmlWorkflowItem witem2 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 2")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        XmlWorkflowItem witem3 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                .withTitle("Workflow Item 3")
                .withIssueDate("2017-10-18")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String authReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        // reviewer1 tries to get the pooltask of reviewer2 and viceversa
        getClient(authReviewer1).perform(get("/api/workflow/pooltasks/search/findByUser")
                    .param("uuid", reviewer2.getID().toString()))
                .andExpect(status().isForbidden());

        String authReviewer2 = getAuthToken(reviewer2.getEmail(), password);
        getClient(authReviewer2).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findByUserUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/workflow/pooltasks/search/findByUser")
                    .param("uuid", admin.getID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * A delete request over a pooltask should result in a 405 Method not supported exception and no changes applied
     * workspace
     *
     * @throws Exception
     */
    public void deletePoolTaskTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //3. a workflow item
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, admin)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(delete("/api/workflow/pooltasks/" + poolTask.getID()))
            .andExpect(status().isMethodNotAllowed());

        // check that the task is still here
        getClient(adminToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    /**
     * Test claiming of a pool task
     *
     * @throws Exception
     */
    public void claimTaskTest() throws Exception {
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

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        getClient(reviewerToken).perform(post("/api/workflow/claimedtasks")
                 .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                 .content("/api/workflow/pooltasks/" + poolTask.getID()))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))));

        // verify that the pool task no longer exists
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
            .andExpect(status().isNotFound());

        // verify that the task has been claimed
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks/")),
                            hasJsonPath("$.type", Matchers.is("claimedtask")),
                            hasJsonPath("$._embedded.owner",
                                    Matchers.is(EPersonMatcher.matchEPersonOnEmail(reviewer.getEmail()))),
                            hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    /**
     * Test unauthorized claiming of a pool task
     *
     * @throws Exception
     */
    public void claimTaskUnauthorizedTest() throws Exception {
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

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        // try to claim the task with an anonymous user
        getClient().perform(post("/api/workflow/claimedtasks")
                   .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                   .content("/api/workflow/pooltasks/" + poolTask.getID()))
                   .andExpect(status().isUnauthorized());

        // verify that the pool task is still here
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    /**
     * Test claiming of another user pool task
     *
     * @throws Exception
     */
    public void claimTaskForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. two reviewers
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
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
                .withWorkflowGroup(1, reviewer)
                .withWorkflowGroup(2, reviewer2).build();

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

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        // reviewer2 try to claim a task that is only available for reviewer1
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks")
                 .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                 .content("/api/workflow/pooltasks/" + poolTask.getID()))
                 .andExpect(status().isForbidden());

        // verify that the pool task is still here
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void claimTaskNotExistingTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/workflow/claimedtasks")
                             .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                             .content("/api/workflow/pooltasks/" + Integer.MAX_VALUE))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    /**
     * Test retrieval of a claimed task
     *
     * @throws Exception
     */
    public void findOneClaimedTaskTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        // get the claimed task as reviewer
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(ClaimedTaskMatcher.matchClaimedTask(claimedTask, "reviewstep"))))
            .andExpect(jsonPath("$._embedded.workflowitem",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Workflow Item 1", "2017-10-17", "ExtraEntry"))));

        // get the claimed task as admin
        getClient(adminToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(ClaimedTaskMatcher.matchClaimedTask(claimedTask, "reviewstep"))))
            .andExpect(jsonPath("$._embedded.workflowitem",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Workflow Item 1", "2017-10-17", "ExtraEntry"))));

    }

    @Test
    /**
     * Test unauthorized retrieval of a claimed task
     *
     * @throws Exception
     */
    public void findOneClaimedTaskUnauthorizedTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        // try to get the claimed task as anonymous
        getClient().perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isUnauthorized());

        // verify that the claimed task exists
        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    /**
     * Test forbidden retrieval of a claimed task
     *
     * @throws Exception
     */
    public void findOneClaimedTaskForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. two reviewers
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
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
                .withWorkflowGroup(1, reviewer, reviewer2).build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        // get the claimed task as reviewer
        getClient(reviewer2Token).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isForbidden());

        // verify that the claimed task exists
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    /**
     * Test unclaiming a task
     *
     * @throws Exception
     */
    public void unclaimTaskTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        getClient(reviewerToken).perform(delete("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isNoContent());

        // verify that the task has been unclaimed and it is back in the pool
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks/")),
                            hasJsonPath("$.type", Matchers.is("pooltask")),
                            hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    /**
     * Test unclaiming a task as admin
     *
     * @throws Exception
     */
    public void unclaimTaskAdminTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(delete("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isNoContent());

        // verify that the task has been unclaimed and it is back in the pool
        getClient(adminToken).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks/")),
                            hasJsonPath("$.type", Matchers.is("pooltask")),
                            hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void unclaimTaskUnauthorizedTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void unclaimTaskForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. two reviewers
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .withPassword(password)
                .build();
        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
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
                .withWorkflowGroup(1, reviewer, reviewer2).build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        getClient(reviewer2Token).perform(delete("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isForbidden());

        // verify that the task is still here
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void unclaimTaskNotExistingTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(delete("/api/workflow/claimedtasks/" + Integer.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Test findByUser of claimed tasks
     *
     * @throws Exception
     */
    public void findClaimedByUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        //2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1, reviewer2).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer2, admin).build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //4. a pool task
        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        ClaimedTask claimedTask2 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 2")
                .withIssueDate("2017-10-18")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry2")
                .build();
        ClaimedTask claimedTask3 = ClaimedTaskBuilder.createClaimedTask(context, col2, reviewer2)
                .withTitle("Workflow Item 3")
                .withIssueDate("2017-10-19")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry3")
                .build();

        XmlWorkflowItem witem1 = claimedTask1.getWorkflowItem();
        XmlWorkflowItem witem2 = claimedTask2.getWorkflowItem();
        XmlWorkflowItem witem3 = claimedTask3.getWorkflowItem();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        // verify that each reviewer is able to get it own tasks
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.containsInAnyOrder(
                    Matchers.allOf(
                            hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask1, "reviewstep")),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem1, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                    ),
                    Matchers.allOf(
                            hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask2, "reviewstep")),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem2, "Workflow Item 2", "2017-10-18", "ExtraEntry2")))
                    )
            )))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(reviewer2Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask3, "reviewstep")),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem3, "Workflow Item 3", "2017-10-19", "ExtraEntry3")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));

        // verify that the admins is able the tasks list of both reviewers
        getClient(adminToken).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.containsInAnyOrder(
                    Matchers.allOf(
                            hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask1, "reviewstep")),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem1, "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                    ),
                    Matchers.allOf(
                            hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask2, "reviewstep")),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem2, "Workflow Item 2", "2017-10-18", "ExtraEntry2")))
                    )
            )))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(adminToken).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask3, "reviewstep")),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem3, "Workflow Item 3", "2017-10-19", "ExtraEntry3")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findClaimedByUserForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        //2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1, reviewer2).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer2, admin).build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //4. some claimed tasks
        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        ClaimedTask claimedTask2 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 2")
                .withIssueDate("2017-10-18")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry2")
                .build();
        ClaimedTask claimedTask3 = ClaimedTaskBuilder.createClaimedTask(context, col2, reviewer2)
                .withTitle("Workflow Item 3")
                .withIssueDate("2017-10-19")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry3")
                .build();

        XmlWorkflowItem witem1 = claimedTask1.getWorkflowItem();
        XmlWorkflowItem witem2 = claimedTask2.getWorkflowItem();
        XmlWorkflowItem witem3 = claimedTask3.getWorkflowItem();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        // verify that a reviewer is not able to get the list of the other one
        getClient(reviewer2Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isForbidden());

        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findClaimedByUserUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", admin.getID().toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * Test the approval of a claimed task
     *
     * @throws Exception
     */
    public void approvalTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        getClient(reviewerToken).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the task has been processed and is not anymore available
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isNotFound());

        // verify that the item has been published
        getClient().perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(true)));
    }

    @Test
    /**
     * Test that only the task owner can approve it
     *
     * @throws Exception
     */
    public void approvalForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        //2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        // the reviewer2 is a reviewer in a different step for the colAA1 and with the same role than reviewer1 for
        // another collection
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1)
                .withWorkflowGroup(2, reviewer2)
                .build();

        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer2)
                .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isForbidden());

        // nor the administrator can approve a task that he doesn't own
        getClient(adminToken).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isForbidden());

        // verify that the task is still here
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    /**
     * Test the rejection of a claimed task
     *
     * @throws Exception
     */
    public void rejectTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        // without a reason the reject should be refused
        getClient(reviewerToken).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_reject", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isUnprocessableEntity());

        // verify that the task has not been processed and is still available
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());

        // give a reason to reject
        getClient(reviewerToken).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_reject", "true")
                .param("reason", "I need to test the reject")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the task has been processed and is not anymore available
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isNotFound());

        // verify that the task is send back to the user and not to the pool
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        // verify that the task is send back to the user and not to the pool
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/search/findBySubmitter")
                .param("size", "20")
                .param("uuid", submitter.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workspaceitems",
                    Matchers.contains(
                            WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(null, "Workflow Item 1",
                                    "2017-10-17", "ExtraEntry"))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));

        // verify that the item has not been published
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));

    }

    @Test
    /**
     * Test that only the task owner can reject it
     *
     * @throws Exception
     */
    public void rejectForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        //2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        // the reviewer2 is a reviewer in a different step for the colAA1 and with the same role than reviewer1 for
        // another collection
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1)
                .withWorkflowGroup(2, reviewer2)
                .build();

        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer2)
                .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        // try to reject with reviewer2
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_reject", "true")
                .param("reason", "I need to test the reject")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isForbidden());

        // nor the administrator can approve a task that he doesn't own
        getClient(adminToken).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_reject", "true")
                .param("reason", "I need to test the reject")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isForbidden());

        // verify that the task has not been processed and is still here
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());

    }

    @Test
    /**
     * Test an undefined action on a claimed task
     *
     * @throws Exception
     */
    public void undefinedActionTest() throws Exception {
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

        //4. a claimed task
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        // try an undefined action
        getClient(reviewerToken).perform(post("/api/workflow/claimedtasks/" + claimedTask.getID())
                .param("submit_undefinedaction", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isUnprocessableEntity());

        // verify that the task has not been processed and is still available
        getClient(reviewerToken).perform(get("/api/workflow/claimedtasks/" + claimedTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void actionOnNotExistingClaimedTaskTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/workflow/claimedtasks/" + Integer.MAX_VALUE)
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Test the run over a complete workflow
     *
     * @throws Exception
     */
    public void fullWorkflowTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. three reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer3 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer3@example.com")
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
                .withWorkflowGroup(1, reviewer1)
                .withWorkflowGroup(2, reviewer2)
                .withWorkflowGroup(3, reviewer3)
                .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                        .withTitle("Test item full workflow")
                        .withIssueDate("2019-03-06")
                        .withSubject("ExtraEntry")
                        .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);
        String reviewer3Token = getAuthToken(reviewer3.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        Step step = xmlWorkflowFactory.getStepByName("reviewstep");
        // step 1
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                            hasJsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                            ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        WorkflowActionConfig workflowAction = xmlWorkflowFactory.getActionByName("reviewaction");

        // approve the claimedTask, wf step 1
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));

        step = xmlWorkflowFactory.getStepByName("editstep");

        // step 2
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            Matchers.is(PoolTaskMatcher.matchPoolTask(null, "editstep")),
                            hasJsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                            ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        workflowAction = xmlWorkflowFactory.getActionByName("editaction");

        // approve the claimedTask, wf step 2
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));

        step = xmlWorkflowFactory.getStepByName("finaleditstep");

        // step 3
        getClient(reviewer3Token).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer3.getID().toString()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            Matchers.is(PoolTaskMatcher.matchPoolTask(null, "finaleditstep")),
                            hasJsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        workflowAction = xmlWorkflowFactory.getActionByName("finaleditaction");

        // approve the claimedTask, wf step 3
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the item has been published!!!
        getClient().perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(true)));
    }

    @Test
    /**
     * Test the run of the default workflow where the wfi gets rejected in the first step (review step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilReviewStep_Reject() throws Exception {

        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A reviewer
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
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
            .withWorkflowGroup(1, reviewer1)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 1
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // reject the claimedTask with reason, default wf step 1 - review step
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_reject", "true")
            .param("reason", "I need to test the reject in review step")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the task has been processed and is not anymore available
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/" + idRef.get()))
            .andExpect(status().isNotFound());

        // verify that the task is send back to the user and not to the pool
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));
    }

    @Test
    /**
     * Test the run of the default workflow where the reviewer attempts a non-valid option in the first step
     * (review step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilReviewStep_NonValidOption_EditMetadata() throws Exception {

        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A reviewer
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
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
            .withWorkflowGroup(1, reviewer1)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 1
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // try non valid option (submit_edit_metadata), in default wf step 1 (review step)
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_edit_metadata", "true")
            .param("reason", "I need to test the submit_edit_metadata")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isUnprocessableEntity());

        // verify that the task has not been processed and is still available
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/" + idRef.get()))
            .andExpect(status().isOk());
    }

    @Test
    /**
     * Test the run of the default workflow where the wfi gets rejected in the 2nd step (edit step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilEditStep_Reject() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. reviewer of 2nd step
        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer2@example.com")
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
            .withWorkflowGroup(2, reviewer2)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 2
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "editstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // reject the claimedTask, default wf step 2 (edit step)
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_reject", "true")
            .param("reason", "I need to test the submit_reject in edit step")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the task has been processed and is not anymore available
        getClient(reviewer2Token).perform(get("/api/workflow/claimedtasks/" + idRef.get()))
            .andExpect(status().isNotFound());

        // verify that the task is send back to the user and not to the pool
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));
    }

    @Test
    /**
     * Test the run of the default workflow where the reviewer attempts a non-valid option in the 2d step (edit step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilEditStep_NonValidOption() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. reviewer of 2nd step
        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer2@example.com")
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
            .withWorkflowGroup(2, reviewer2)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> idRefClaimedTask = new AtomicReference<>();

        // step 2
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "editstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // try non valid option (submit_edit_metadata), default wf step 2 (edit step)
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_non_valid_option", "true")
            .param("reason", "I need to test an unvalid option")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isUnprocessableEntity());

        // verify that the task has not been processed and is still available
        getClient(reviewer2Token).perform(get("/api/workflow/claimedtasks/" + idRef.get()))
            .andExpect(status().isOk());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));
    }

    @Test
    /**
     * Test the run of the default workflow where the reviewer attempts a reject option in the 3rd step
     * (final edit step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilFinalEditStep_Reject() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. reviewer of 3rd step
        EPerson reviewer3 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer3@example.com")
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
            .withWorkflowGroup(3, reviewer3)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer3Token = getAuthToken(reviewer3.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 3
        getClient(reviewer3Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer3.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "finaleditstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks")
                 .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                 .content("/api/workflow/pooltasks/" + idRef.get()))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                 .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // reject the claimedTask, default wf step 3 (final edit step)
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_reject", "true")
            .param("reason", "I need to test reject in fina edit step")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isUnprocessableEntity());

        // verify that the task has not been processed and is still available
        getClient(reviewer3Token).perform(get("/api/workflow/claimedtasks/" + idRef.get()))
            .andExpect(status().isOk());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));
    }

    @Test
    /**
     * Test the run of the default workflow where the reviewer attempts an edit metadata in the 3rd step
     * (final edit step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilFinalEditStep_EditMetadata() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. reviewer of 3rd step
        EPerson reviewer3 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer3@example.com")
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
            .withWorkflowGroup(3, reviewer3)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer3Token = getAuthToken(reviewer3.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 3
        getClient(reviewer3Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer3.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "finaleditstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // edit metadata of the claimedTask, default wf step 3 (final edit step)
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_edit_metadata", "true")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());
    }

    @Test
    /**
     * Test the run of the default workflow where the reviewer attempts a non-valid option in the 3rd step
     * (final edit step)
     *
     * @throws Exception
     */
    public void defaultWorkflowTest_UntilFinalEditStep_NonValidOption() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. reviewer of 3rd step
        EPerson reviewer3 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer3@example.com")
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
            .withWorkflowGroup(3, reviewer3)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer3Token = getAuthToken(reviewer3.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 3
        getClient(reviewer3Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer3.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "finaleditstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        // non valid option in the default wf step 3 (final edit step)
        getClient(reviewer3Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
            .param("submit_non_valid_option", "true")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isUnprocessableEntity());

        // verify that the task has not been processed and is still available
        getClient(reviewer3Token).perform(get("/api/workflow/claimedtasks/" + idRef.get()))
            .andExpect(status().isOk());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));
    }

    @Test
    /**
     * Test to try an upload on unclaimed task in step where edit_metadata is allowed
     *
     * @throws Exception
     */
    public void unclaimedTaskTest_Upload_EditMetadataOptionAllowed() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. a reviewer
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
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
            .withWorkflowGroup(1, reviewer1)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //4. a pool task
        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .build();
        XmlWorkflowItem witem = poolTask.getWorkflowItem();
        poolTask.setStepID("editstep");
        poolTask.setActionID("editaction");

        context.restoreAuthSystemState();
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);

        // try to upload file on workspace item while it is unclaimed
        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "bibtex-test.bib", "application/x-bibtex",
            bibtex);
        getClient(reviewer1Token).perform(multipart("/api/workflow/workflowitems/" + witem.getID())
            .file(bibtexFile))
            .andExpect(status().isUnprocessableEntity());

        bibtex.close();
    }

    @Test
    /**
     * Test to try a patch on unclaimed task in step where edit_metadata is allowed
     *
     * @throws Exception
     */
    public void unclaimedTaskTest_Patch_EditMetadataOptionAllowed() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. reviewer for second step
        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer2@example.com")
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
            .withWorkflowGroup(2, reviewer2)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 2
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "editstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // try to patch a workspace item while it is unclaimed
        String authToken = getAuthToken(eperson.getEmail(), password);

        // a simple patch to update an existent metadata
        List<Operation> updateTitle = new ArrayList<>();
        Map<String, String> value = new HashMap<>();
        value.put("value", "New Title");
        updateTitle.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);

        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
            .content(patchBody)
            .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    /**
     * Test to try an upload on a claimed task in step where edit_metadata is Not allowed (review step)
     *
     * @throws Exception
     */
    public void uploadTest_ClaimedTask_EditMetadataOptionNotAllowed() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
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
            .withWorkflowGroup(1, reviewer1)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //4. a claimed task with workflow item in review step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .build();
        claimedTask.setStepID("reviewstep");
        claimedTask.setActionID("reviewaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        // try to upload to a workspace item while it is in a step that does not have the edit_metadata option
        // (review step)
        String authToken = getAuthToken(reviewer1.getEmail(), password);
        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "bibtex-test.bib", "application/x-bibtex",
            bibtex);
        getClient(authToken).perform(multipart("/api/workflow/workflowitems/" + witem.getID())
            .file(bibtexFile))
            .andExpect(status().isUnprocessableEntity());

        bibtex.close();
    }

    @Test
    /**
     * Test to try a patch on a claimed task that is in a step that does not allow edit_metadata (review step)
     *
     * @throws Exception
     */
    public void patchTest_ClaimedTask_EditMetadataOptionNotAllowed() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
            .withPassword(password)
            .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer2@example.com")
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
            .withWorkflowGroup(1, reviewer1)
            .withWorkflowGroup(2, reviewer2)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //3. create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
            .withTitle("Test item full workflow")
            .withIssueDate("2019-03-06")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        // step 1
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByUser")
            .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                Matchers.allOf(
                    Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                    hasJsonPath("$._embedded.workflowitem",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andDo((result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks")
                 .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                 .content("/api/workflow/pooltasks/" + idRef.get()))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))));

        // try to patch a workspace item while it is in a step that does not have the edit_metadata option (review step)
        String authToken = getAuthToken(eperson.getEmail(), password);

        // a simple patch to update an existent metadata
        List<Operation> updateTitle = new ArrayList<>();
        Map<String, String> value = new HashMap<>();
        value.put("value", "New Title");
        updateTitle.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);

        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
            .content(patchBody)
            .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    /**
     * Test to upload on claimed task in step where edit_metadata is allowed
     *
     * @throws Exception
     */
    public void claimedTaskTest_Upload_EditMetadataOptionAllowed() throws Exception {
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. a reviewer
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
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
            .withWorkflowGroup(1, reviewer1)
            .build();

        //3. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        context.setCurrentUser(submitter);

        //4. a claimed task with workflow item in review step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);

        // upload a file on workspace item that is claimed and in the edit step
        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "bibtex-test.bib", "application/x-bibtex",
            bibtex);
        getClient(reviewer1Token).perform(multipart("/api/workflow/workflowitems/" + witem.getID())
            .file(bibtexFile))
            .andExpect(status().isCreated());

        bibtex.close();
    }

    @Test
    public void findAllPooltasksByItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, admin).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        PoolTask poolTask2 = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                .withTitle("Workflow Item 2")
                .withIssueDate("2020-01-19")
                .withAuthor("Tommaso, Donald").withAuthor("Shon, John")
                .withSubject("ExtraEntry").build();

        Item item2 = poolTask2.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/pooltasks/search/findAllByItem")
               .param("uuid", item1.getID().toString()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$._embedded.pooltasks", Matchers.containsInAnyOrder(
                Matchers.allOf(
                Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")), hasJsonPath("$._embedded.workflowitem",
                Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                            poolTask.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry")))
                ))))
               .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
               .andExpect(jsonPath("$.page.totalElements", is(1)));

        getClient(tokenAdmin).perform(get("/api/workflow/pooltasks/search/findAllByItem")
                .param("uuid", item2.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pooltasks", Matchers.containsInAnyOrder(
                 Matchers.allOf(
                 Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")), hasJsonPath("$._embedded.workflowitem",
                 Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                             poolTask2.getWorkflowItem(), "Workflow Item 2", "2020-01-19", "ExtraEntry")))
                 ))))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findAllPooltasksByItemUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, admin).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/workflow/pooltasks/search/findAllByItem")
                   .param("uuid", item1.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllPooltasksByItemForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, admin).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        // Only Admin has access to this end point

        String tokenSubmitter = getAuthToken(submitter.getEmail(), password);
        getClient(tokenSubmitter).perform(get("/api/workflow/pooltasks/search/findAllByItem")
                                 .param("uuid", item1.getID().toString()))
                                 .andExpect(status().isForbidden());

        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        getClient(tokenReviewer1).perform(get("/api/workflow/pooltasks/search/findAllByItem")
                                 .param("uuid", item1.getID().toString()))
                                 .andExpect(status().isForbidden());
    }

    @Test
    public void findAllPooltasksByItemWrongUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, admin).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        // Only Admin has access to this end point

        String tokenSubmitter = getAuthToken(admin.getEmail(), password);
        getClient(tokenSubmitter).perform(get("/api/workflow/pooltasks/search/findAllByItem")
                                 .param("uuid", UUID.randomUUID().toString()))
                                 .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void findAllPooltasksByItemBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, admin).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        // Only Admin has access to this end point

        String tokenSubmitter = getAuthToken(admin.getEmail(), password);
        getClient(tokenSubmitter).perform(get("/api/workflow/pooltasks/search/findAllByItem")
                                 .param("uuid", "wrongID"))
                                 .andExpect(status().isBadRequest());

        // the required param is no provided
        getClient(tokenSubmitter).perform(get("/api/workflow/pooltasks/search/findAllByItem"))
                                 .andExpect(status().isBadRequest());
    }

    @Test
    public void findPooltaskByItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        PoolTask poolTask2 = PoolTaskBuilder.createPoolTask(context, col2, reviewer2)
                .withTitle("Workflow Item 2")
                .withIssueDate("2020-01-19")
                .withAuthor("Tommaso, Donald").withAuthor("Shon, John")
                .withSubject("ExtraEntry").build();

        Item item2 = poolTask2.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        getClient(tokenReviewer1).perform(get("/api/workflow/pooltasks/search/findByItem")
               .param("uuid", item1.getID().toString()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id", is(poolTask.getID())))
               .andExpect(jsonPath("$.type", is("pooltask")))
               .andExpect(jsonPath("$", Matchers.is(PoolTaskMatcher.matchPoolTask(poolTask, "reviewstep"))))
               .andExpect(jsonPath("$._embedded.workflowitem",
                          Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                      poolTask.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry"))));

        String tokenReviewer2 = getAuthToken(reviewer2.getEmail(), password);
        getClient(tokenReviewer2).perform(get("/api/workflow/pooltasks/search/findByItem")
               .param("uuid", item2.getID().toString()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id", is(poolTask2.getID())))
               .andExpect(jsonPath("$.type", is("pooltask")))
               .andExpect(jsonPath("$", Matchers.is(PoolTaskMatcher.matchPoolTask(poolTask2, "reviewstep"))))
               .andExpect(jsonPath("$._embedded.workflowitem",
                          Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                      poolTask2.getWorkflowItem(), "Workflow Item 2", "2020-01-19", "ExtraEntry"))));

    }

    @Test
    public void findPooltaskByItemNoContentTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        PoolTask poolTask2 = PoolTaskBuilder.createPoolTask(context, col2, reviewer2)
                .withTitle("Workflow Item 2")
                .withIssueDate("2020-01-19")
                .withAuthor("Tommaso, Donald").withAuthor("Shon, John")
                .withSubject("ExtraEntry").build();

        Item item2 = poolTask2.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        String tokenSubmitter = getAuthToken(submitter.getEmail(), password);
        getClient(tokenSubmitter).perform(get("/api/workflow/pooltasks/search/findByItem")
                                 .param("uuid", item1.getID().toString()))
                                 .andExpect(status().isNoContent());

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/pooltasks/search/findByItem")
                             .param("uuid", item1.getID().toString()))
                             .andExpect(status().isNoContent());

        String tokenReviewer2 = getAuthToken(reviewer2.getEmail(), password);
        getClient(tokenReviewer2).perform(get("/api/workflow/pooltasks/search/findByItem")
                                 .param("uuid", item1.getID().toString()))
                                 .andExpect(status().isNoContent());
    }

    @Test
    public void findPooltaskByItemWrongUuidOfItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        getClient(tokenReviewer1).perform(get("/api/workflow/pooltasks/search/findByItem")
                                 .param("uuid", UUID.randomUUID().toString()))
                                 .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void findPooltaskByItemBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        getClient(tokenReviewer1).perform(get("/api/workflow/pooltasks/search/findByItem")
                                 .param("uuid", "wrongID"))
                                 .andExpect(status().isBadRequest());

        getClient(tokenReviewer1).perform(get("/api/workflow/pooltasks/search/findByItem"))
                                 .andExpect(status().isBadRequest());
    }

    @Test
    public void findPooltaskByItemUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTask poolTask = PoolTaskBuilder.createPoolTask(context, col1, reviewer1)
                                           .withTitle("Workflow Item 1")
                                           .withIssueDate("2017-10-17")
                                           .withAuthor("Smith, Donald")
                                           .withAuthor("Doe, John")
                                           .withSubject("ExtraEntry").build();

        Item item1 = poolTask.getWorkflowItem().getItem();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/workflow/pooltasks/search/findByItem")
                   .param("uuid", item1.getID().toString()))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void findPoolTaskByItemArchivedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item")
                                     .withIssueDate("2020-06-25")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByItem")
                                 .param("uuid", publicItem.getID().toString()))
                                 .andExpect(status().isNoContent());
    }

    @Test
    public void findAllPoolTaskByItemArchivedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item")
                                     .withIssueDate("2020-06-25")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(admin.getEmail(), password);
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findAllByItem")
                                 .param("uuid", publicItem.getID().toString()))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(contentType))
                                 .andExpect(jsonPath("$.page.totalPages", is(0)))
                                 .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findAllClaimedTaskByItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        ClaimedTask claimedTask2 = ClaimedTaskBuilder.createClaimedTask(context, col2, reviewer2)
                .withTitle("Workflow Item 2")
                .withIssueDate("2020-10-20")
                .withAuthor("Tommaso, Donald").withAuthor("Shon, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.containsInAnyOrder(
                    Matchers.allOf(hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask1, "reviewstep")),
                                   hasJsonPath("$._embedded.workflowitem",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                claimedTask1.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry")))))))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        getClient(adminToken).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                .param("uuid", claimedTask2.getWorkflowItem().getItem().getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.containsInAnyOrder(
                    Matchers.allOf(hasJsonPath("$", ClaimedTaskMatcher.matchClaimedTask(claimedTask2, "reviewstep")),
                                   hasJsonPath("$._embedded.workflowitem",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                claimedTask2.getWorkflowItem(), "Workflow Item 2", "2020-10-20", "ExtraEntry")))))))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findAllClaimedTaskByItemForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String tokenSubmitter = getAuthToken(submitter.getEmail(), password);
        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);
        String tokenReviewer2 = getAuthToken(reviewer2.getEmail(), password);

        // Only Admin has access to this end point

        getClient(tokenSubmitter).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                                 .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isForbidden());

        getClient(tokenReviewer1).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                                 .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isForbidden());

        getClient(tokenReviewer2).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                                 .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isForbidden());
    }

    @Test
    public void findAllClaimedTaskByItemUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                                 .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isUnauthorized());

    }

    @Test
    public void findAllClaimedTaskByItemWrongUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                             .param("uuid", UUID.randomUUID().toString()))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findAllClaimedTaskByItemBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                             .param("uuid", "wrongID"))
                             .andExpect(status().isBadRequest());

        // the required param is no provided
        getClient(tokenAdmin).perform(get("/api/workflow/claimedtasks/search/findAllByItem"))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void findAllClaimedTaskByItemArchivedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item")
                                     .withIssueDate("2020-06-25")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(admin.getEmail(), password);
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/search/findAllByItem")
                                 .param("uuid", publicItem.getID().toString()))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(contentType))
                                 .andExpect(jsonPath("$.page.totalPages", is(0)))
                                 .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findClaimedTaskByItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, reviewer2).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        ClaimedTask claimedTask2 = ClaimedTaskBuilder.createClaimedTask(context, col2, reviewer2)
                .withTitle("Workflow Item 2")
                .withIssueDate("2020-10-19")
                .withAuthor("Tommaso, Donald")
                .withAuthor("Shon, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        getClient(reviewer1Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(claimedTask1.getID())))
                .andExpect(jsonPath("$.type", is("claimedtask")))
                .andExpect(jsonPath("$", Matchers.is(ClaimedTaskMatcher.matchClaimedTask(claimedTask1, "reviewstep"))))
                .andExpect(jsonPath("$._embedded.workflowitem",
                           Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                   claimedTask1.getWorkflowItem(), "Workflow Item 1", "2017-10-17", "ExtraEntry"))));

        getClient(reviewer2Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                .param("uuid", claimedTask2.getWorkflowItem().getItem().getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(claimedTask2.getID())))
                .andExpect(jsonPath("$.type", is("claimedtask")))
                .andExpect(jsonPath("$", Matchers.is(ClaimedTaskMatcher.matchClaimedTask(claimedTask2, "reviewstep"))))
                .andExpect(jsonPath("$._embedded.workflowitem",
                           Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                   claimedTask2.getWorkflowItem(), "Workflow Item 2", "2020-10-19", "ExtraEntry"))));

    }

    @Test
    public void findClaimedTaskByItemNoContentTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1, reviewer2).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2")
                                           .withWorkflowGroup(1, reviewer2).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        ClaimedTask claimedTask2 = ClaimedTaskBuilder.createClaimedTask(context, col2, reviewer2)
                .withTitle("Workflow Item 2")
                .withIssueDate("2020-10-19")
                .withAuthor("Tommaso, Donald")
                .withAuthor("Shon, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        getClient(reviewer2Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isNoContent());

        getClient(reviewer1Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", claimedTask2.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/workflow/claimedtask/search/findByItem")
                             .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                             .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/workflow/claimedtask/search/findByItem")
                             .param("uuid", claimedTask2.getWorkflowItem().getItem().getID().toString()))
                             .andExpect(status().isNoContent());

        getClient(submitterToken).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", claimedTask1.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isNoContent());

        getClient(submitterToken).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", claimedTask2.getWorkflowItem().getItem().getID().toString()))
                                 .andExpect(status().isNoContent());

    }

    @Test
    public void findClaimedTaskByItemUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/workflow/claimedtask/search/findByItem")
                   .param("uuid", claimedTask.getWorkflowItem().getItem().getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findClaimedTaskByItemWrongUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", UUID.randomUUID().toString()))
                                 .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findClaimedTaskByItemBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1)
                                                    .withTitle("Workflow Item 1")
                                                    .withIssueDate("2017-10-17")
                                                    .withAuthor("Smith, Donald")
                                                    .withAuthor("Doe, John")
                                                    .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", "wrongID"))
                                 .andExpect(status().isBadRequest());

        // the required param is no provided
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtask/search/findByItem"))
                                 .andExpect(status().isBadRequest());
    }

    @Test
    public void findClaimedTaskByItemArchivedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        Item publicItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item")
                                     .withIssueDate("2020-06-25")
                                     .withAuthor("Smith, Donald")
                                     .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);

        getClient(reviewer1Token).perform(get("/api/workflow/claimedtask/search/findByItem")
                                 .param("uuid", publicItem.getID().toString()))
                                 .andExpect(status().isNoContent());

    }

    @Test
    public void findPooltaksByItemInWorkflowWithoutPooltaskTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        EPerson reviewer = EPersonBuilder.createEPerson(context)
                                         .withEmail("reviewer1@example.com")
                                         .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer).build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password).build();

        context.setCurrentUser(submitter);

        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item")
                                                   .withIssueDate("2010-04-24")
                                                   .withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/workflow/pooltasks/search/findByItem")
                               .param("uuid", witem.getItem().getID().toString()))
                               .andExpect(status().isNoContent());
    }
    @Test
    public void poolTaskSerchMethodWithSingleModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/pooltask/search"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void claimedtaskSerchMethodWithSingleModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/claimedtask/search"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void poolTaskSerchMethodWithPluralModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/pooltasks/search"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._links.findByUser.href", Matchers.allOf(
                                        Matchers.containsString("/api/workflow/pooltasks/search/findByUser"))));
    }

    @Test
    public void claimedtaskSerchMethodWithPluralModelTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/claimedtasks/search"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._links.findByUser.href", Matchers.allOf(
                                        Matchers.containsString("/api/workflow/claimedtasks/search/findByUser"))));
    }


    @Test
    public void addReviewerToRunningWorkflowTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password).build();

        EPerson reviewerOther = EPersonBuilder.createEPerson(context)
                .withEmail("reviewerOther@example.com")
                .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1)
                                           .withWorkflowGroup(2, reviewer2)
                                           .build();

        Group firstWorkflowGroup = col1.getWorkflowStep1(context);

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        context.setCurrentUser(submitter);

        // create a workflowitem (so a pool task in step1)
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                        .withTitle("Test title")
                        .withIssueDate("2021-02-11")
                        .withSubject("ExtraEntry").build();

        Item item = witem.getItem();

        context.restoreAuthSystemState();

        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);
        String reviewerOtherToken = getAuthToken(reviewerOther.getEmail(), password);

        String adminToken = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        Step step = xmlWorkflowFactory.getStepByName("reviewstep");

        // step 1
        getClient(reviewer1Token).perform(get("/api/workflow/pooltasks/search/findByUser")
                                 .param("uuid", reviewer1.getID().toString())
                                 .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                            hasJsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem, "Test title", "2021-02-11", "ExtraEntry")))
                            ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        assertFalse(groupService.isMember(context, reviewerOther, firstWorkflowGroup));

        getClient(adminToken).perform(post("/api/eperson/groups/" + firstWorkflowGroup.getID() + "/epersons")
                             .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                             .content(REST_SERVER_URL + "eperson/groups/" + reviewerOther.getID()))
                             .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, reviewerOther, firstWorkflowGroup));

        getClient(reviewerOtherToken).perform(get("/api/workflow/pooltasks/search/findByUser")
                                     .param("uuid", reviewerOther.getID().toString())
                                     .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pooltasks",
                        Matchers.contains(Matchers.allOf(Matchers.is(PoolTaskMatcher.matchPoolTask(null, "reviewstep")),
                                hasJsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)),
                                hasJsonPath("$._embedded.workflowitem",
                                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                "Test title", "2021-02-11", "ExtraEntry")))))))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewerOtherToken).perform(post("/api/workflow/claimedtasks")
                                     .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                                     .content("/api/workflow/pooltasks/" + idRef.get()))
                                 .andExpect(status().isCreated())
                                 .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                                 .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(reviewerOtherToken).perform(get("/api/workflow/claimedtasks/search/findByUser")
                                     .param("uuid", reviewerOther.getID().toString()))
                                     .andExpect(status().isOk())
                                     .andExpect(jsonPath("$._embedded.claimedtasks[0]._embedded.workflowitem",
                                         Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                                             witem, "Test title", "2021-02-11", "ExtraEntry"))))
                                     .andExpect(jsonPath("$.page.totalElements", is(1)));

        WorkflowActionConfig workflowAction = xmlWorkflowFactory.getActionByName("reviewaction");

        // approve the claimedTask, wf step 1
        getClient(reviewerOtherToken).perform(post("/api/workflow/claimedtasks/" + idRef.get())
                                     .param("submit_approve", "true")
                                     .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                                     .andExpect(status().isNoContent());

        step = xmlWorkflowFactory.getStepByName("editstep");

        // step 2
        getClient(reviewer2Token).perform(get("/api/workflow/pooltasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.pooltasks", Matchers.contains(
                    Matchers.allOf(
                            Matchers.is(PoolTaskMatcher.matchPoolTask(null, "editstep")),
                            hasJsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)),
                            hasJsonPath("$._embedded.workflowitem",
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                            witem, "Test title", "2021-02-11", "ExtraEntry")))
                            ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/pooltasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.pooltasks[0].id"))));

        // claim the task
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks")
                .contentType(MediaType.parseMediaType(RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("/api/workflow/pooltasks/" + idRef.get()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.allOf(hasJsonPath("$.type", is("claimedtask")))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));
    }

}
