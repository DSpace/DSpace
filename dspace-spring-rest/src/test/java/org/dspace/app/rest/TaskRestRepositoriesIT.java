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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.builder.ClaimedTaskBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.PoolTaskBuilder;
import org.dspace.app.rest.builder.WorkflowItemBuilder;
import org.dspace.app.rest.matcher.ClaimedTaskMatcher;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.PoolTaskMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Test suite for the pooltasks and claimedtasks endpoints
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class TaskRestRepositoriesIT extends AbstractControllerIntegrationTest {

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
        ;

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
        ;

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

        getClient(reviewerToken).perform(post("/api/workflow/pooltasks/" + poolTask.getID())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

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
        getClient().perform(post("/api/workflow/pooltasks/" + poolTask.getID())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
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
        getClient(reviewer2Token).perform(post("/api/workflow/pooltasks/" + poolTask.getID())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isForbidden());

        // verify that the pool task is still here
        getClient(reviewerToken).perform(get("/api/workflow/pooltasks/" + poolTask.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void claimTaskNotExistingTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/workflow/pooltasks/" + Integer.MAX_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNotFound());
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

        // the reviewer2 is a reviewer in a different step for the col1 and with the same role than reviewer1 for
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

        // the reviewer2 is a reviewer in a different step for the col1 and with the same role than reviewer1 for
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

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();

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
        getClient(reviewer1Token).perform(post("/api/workflow/pooltasks/" + idRef.get())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // get the id of the claimed task
        getClient(reviewer1Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks/")),
                            hasJsonPath("$.type", Matchers.is("claimedtask")),
                            hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.claimedtasks[0].id"))));

        // approve the claimedTask, wf step 1
        getClient(reviewer1Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));

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
        getClient(reviewer2Token).perform(post("/api/workflow/pooltasks/" + idRef.get())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // get the id of the claimed task
        getClient(reviewer2Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks/")),
                            hasJsonPath("$.type", Matchers.is("claimedtask")),
                            hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.claimedtasks[0].id"))));

        // approve the claimedTask, wf step 2
        getClient(reviewer2Token).perform(post("/api/workflow/claimedtasks/" + idRef.get())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // verify that the underline item is still unpublished
        getClient(adminToken).perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inArchive", is(false)));

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
        getClient(reviewer3Token).perform(post("/api/workflow/pooltasks/" + idRef.get())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        // get the id of the claimed task
        getClient(reviewer3Token).perform(get("/api/workflow/claimedtasks/search/findByUser")
                .param("uuid", reviewer3.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.claimedtasks", Matchers.contains(
                    Matchers.allOf(
                            hasJsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks/")),
                            hasJsonPath("$.type", Matchers.is("claimedtask")),
                            hasJsonPath("$._embedded.workflowitem",
                                     Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(
                                             witem, "Test item full workflow", "2019-03-06", "ExtraEntry")))
                    ))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/claimedtasks")))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andDo((result -> idRef
                        .set(read(result.getResponse().getContentAsString(), "$._embedded.claimedtasks[0].id"))));

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

}
