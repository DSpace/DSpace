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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.matcher.WorkflowStepMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the WorkflowItem endpoint
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class WorkflowItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private XmlWorkflowFactory xmlWorkflowFactory;

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);
    }

    @Test
    /**
     * All the workflowitems should be returned regardless of the collection where they were created
     *
     * @throws Exception
     */
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(admin);

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, admin).build();


        //2. Three workflow items in two different collections
        XmlWorkflowItem workflowItem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                      .withTitle("Workflow Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        XmlWorkflowItem workflowItem2 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        XmlWorkflowItem workflowItem3 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/workflow/workflowitems"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.workflowitems", Matchers.containsInAnyOrder(
                        WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem1, "Workflow Item 1",
                                "2017-10-17"),
                        WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem2, "Workflow Item 2",
                                "2016-02-13"),
                        WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem3, "Workflow Item 3",
                                "2016-02-13"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/workflow/workflowitems")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    /**
     * The workflowitem endpoint must provide proper pagination
     *
     * @throws Exception
     */
    public void findAllWithPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, admin).build();

        //2. Three workflow items in two different collections
        XmlWorkflowItem workflowItem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                      .withTitle("Workflow Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        XmlWorkflowItem workflowItem2 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        XmlWorkflowItem workflowItem3 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/workflow/workflowitems").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workflowitems",
                        Matchers.containsInAnyOrder(
                                WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem1, "Workflow Item 1",
                                        "2017-10-17"),
                                WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem2, "Workflow Item 2",
                                        "2016-02-13"))))
                .andExpect(jsonPath("$._embedded.workflowitems",
                        Matchers.not(Matchers.contains(WorkflowItemMatcher
                                .matchItemWithTitleAndDateIssued(workflowItem3, "Workflow Item 3", "2016-02-13")))));

        getClient(token).perform(get("/api/workflow/workflowitems").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workflowitems",
                        Matchers.contains(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem3,
                                "Workflow Item 3", "2016-02-13"))))
                .andExpect(jsonPath("$._embedded.workflowitems",
                        Matchers.not(Matchers.contains(
                                WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem1, "Workflow Item 1",
                                        "2017-10-17"),
                                WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workflowItem2, "Workflow Item 2",
                                        "2016-02-13")))))
                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    /**
     * The findAll should be available only to admins regardless to having or less a role in the workflow
     *
     * @throws Exception
     */
    public void findAllForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(admin);

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, eperson).build();


        //2. Three workflow items in two different collections
        XmlWorkflowItem workflowItem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                      .withTitle("Workflow Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        // these two items will be visible individually to the user
        XmlWorkflowItem workflowItem2 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        XmlWorkflowItem workflowItem3 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        // a normal user cannot access the workflowitems collection endpoint
        getClient(token).perform(get("/api/workflow/workflowitems"))
                   .andExpect(status().isForbidden());

        // the workflowitems collection endpoint requires authentication
        getClient().perform(get("/api/workflow/workflowitems"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    /**
     * The workflowitem resource endpoint must expose the proper structure
     *
     * @throws Exception
     */
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                "Workflow Item 1", "2017-10-17", "ExtraEntry"))));
    }

    @Test
    /**
     * The workflowitem resource endpoint should be visible only to member of the corresponding workflow step
     *
     * @throws Exception
     */
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. A community-collection structure with one parent community with sub-community and three collections
        // (different workflow steps and reviewers).
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        EPerson reviewer1 = EPersonBuilder.createEPerson(context).withEmail("reviewer1@example.com")
                .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, reviewer1).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context).withEmail("reviewer2@example.com")
                .withPassword(password).build();

        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(2, reviewer2).build();

        EPerson reviewer3 = EPersonBuilder.createEPerson(context).withEmail("reviewer3@example.com")
                .withPassword(password).build();

        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3")
                .withWorkflowGroup(3, reviewer3).build();

        //2. three workflow items in the three collections (this will lead to pool task)
        XmlWorkflowItem witem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .build();

        XmlWorkflowItem witem2 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                .withTitle("Workflow Item 2")
                .build();

        XmlWorkflowItem witem3 = WorkflowItemBuilder.createWorkflowItem(context, col3)
                .withTitle("Workflow Item 3")
                .build();

        //3. Three claimed tasks (and corresponding workflowitems)
        ClaimedTask claimed1 = ClaimedTaskBuilder.createClaimedTask(context, col1, reviewer1).withTitle("Pool 1")
                .build();
        XmlWorkflowItem wClaimed1 = claimed1.getWorkflowItem();

        ClaimedTask claimed2 = ClaimedTaskBuilder.createClaimedTask(context, col2, reviewer2).withTitle("Pool 2")
                .build();
        XmlWorkflowItem wClaimed2 = claimed2.getWorkflowItem();

        ClaimedTask claimed3 = ClaimedTaskBuilder.createClaimedTask(context, col3, reviewer3).withTitle("Pool 3")
                .build();
        XmlWorkflowItem wClaimed3 = claimed3.getWorkflowItem();

        context.restoreAuthSystemState();

        String rev1Token = getAuthToken(reviewer1.getEmail(), password);
        String rev2Token = getAuthToken(reviewer2.getEmail(), password);
        String rev3Token = getAuthToken(reviewer3.getEmail(), password);

        // anonymous users should be unable to see workflowitem regardless to their workflow status (pool/claimed)
        getClient().perform(get("/api/workflow/workflowitems/" + witem1.getID()))
                .andExpect(status().isUnauthorized());

        getClient().perform(get("/api/workflow/workflowitems/" + claimed1.getID()))
                .andExpect(status().isUnauthorized());

        // reviewer 1 should be able to access only the first workflow item of each group
        getClient(rev1Token).perform(get("/api/workflow/workflowitems/" + witem1.getID()))
                .andExpect(status().isOk());

        getClient(rev1Token).perform(get("/api/workflow/workflowitems/" + witem2.getID()))
                .andExpect(status().isForbidden());

        getClient(rev1Token).perform(get("/api/workflow/workflowitems/" + witem3.getID()))
                .andExpect(status().isForbidden());

        getClient(rev1Token).perform(get("/api/workflow/workflowitems/" + wClaimed1.getID()))
                .andExpect(status().isOk());

        getClient(rev1Token).perform(get("/api/workflow/workflowitems/" + wClaimed2.getID()))
                .andExpect(status().isForbidden());

        getClient(rev1Token).perform(get("/api/workflow/workflowitems/" + wClaimed3.getID()))
                .andExpect(status().isForbidden());

        // reviewer 2 should be able to access only the second workflow item of each group
        getClient(rev2Token).perform(get("/api/workflow/workflowitems/" + witem1.getID()))
                .andExpect(status().isForbidden());

        getClient(rev2Token).perform(get("/api/workflow/workflowitems/" + witem2.getID()))
                .andExpect(status().isOk());

        getClient(rev2Token).perform(get("/api/workflow/workflowitems/" + witem3.getID()))
                .andExpect(status().isForbidden());

        getClient(rev2Token).perform(get("/api/workflow/workflowitems/" + wClaimed1.getID()))
                .andExpect(status().isForbidden());

        getClient(rev2Token).perform(get("/api/workflow/workflowitems/" + wClaimed2.getID()))
                .andExpect(status().isOk());

        getClient(rev2Token).perform(get("/api/workflow/workflowitems/" + wClaimed3.getID()))
                .andExpect(status().isForbidden());

        // reviewer 3 should be able to access only the third workflow item of each group
        getClient(rev3Token).perform(get("/api/workflow/workflowitems/" + witem1.getID()))
                .andExpect(status().isForbidden());

        getClient(rev3Token).perform(get("/api/workflow/workflowitems/" + witem2.getID()))
                .andExpect(status().isForbidden());

        getClient(rev3Token).perform(get("/api/workflow/workflowitems/" + witem3.getID()))
                .andExpect(status().isOk());

        getClient(rev3Token).perform(get("/api/workflow/workflowitems/" + wClaimed1.getID()))
                .andExpect(status().isForbidden());

        getClient(rev3Token).perform(get("/api/workflow/workflowitems/" + wClaimed2.getID()))
                .andExpect(status().isForbidden());

        getClient(rev3Token).perform(get("/api/workflow/workflowitems/" + wClaimed3.getID()))
                .andExpect(status().isOk());
    }

    @Test
    /**
     * The workflowitem resource endpoint must expose the proper structure
     *
     * @throws Exception
     */
    public void findOneRelsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID() + "/collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers
                        .is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle()))));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID() + "/item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemWithTitleAndDateIssued(witem.getItem(),
                        "Workflow Item 1", "2017-10-17"))));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID() + "/submissionDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(hasJsonPath("$.id", is("traditional")))));

    }

    @Test
    /**
     * Check the response code for unexistent workflowitem
     *
     * @throws Exception
     */
    public void findOneWrongIDTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/workflow/workflowitems/" + Integer.MAX_VALUE))
                    .andExpect(status().isNotFound());

        getClient(token).perform(get("/api/workflow/workflowitems/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Create three workflowitem with two different submitter and verify that the findBySubmitter return the proper
     * list of workflowitem for each submitter also paginating
     *
     * @throws Exception
     */
    public void findBySubmitterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, admin).build();

        //2. create two users to use as submitters
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withEmail("submitter1@example.com")
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withEmail("submitter2@example.com")
                .build();

        // create two workflowitems with the first submitter
        context.setCurrentUser(submitter1);


        //3. Two workflow items in two different collections
        XmlWorkflowItem workspaceItem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                      .withTitle("Workflow Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        XmlWorkflowItem workspaceItem2 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        //4. A workflowitem for the second submitter
        context.setCurrentUser(submitter2);
        XmlWorkflowItem workspaceItem3 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                      .withTitle("Workflow Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        context.restoreAuthSystemState();

        // use our admin to retrieve all the workspace by submitter
        String token = getAuthToken(admin.getEmail(), password);

        // the first submitter has two workspace
        getClient(token).perform(get("/api/workflow/workflowitems/search/findBySubmitter")
                .param("size", "20")
                .param("uuid", submitter1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowitems",
                    Matchers.containsInAnyOrder(
                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workflow Item 1",
                                    "2017-10-17"),
                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workflow Item 2",
                                    "2016-02-13"))))
            .andExpect(jsonPath("$._embedded.workflowitems",
                    Matchers.not(Matchers.contains(WorkflowItemMatcher
                            .matchItemWithTitleAndDateIssued(workspaceItem3, "Workflow Item 3", "2016-02-13")))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        // the first submitter has two workspace so if we paginate with a 1-size windows the page 1 will contains the
        // second workspace
        getClient(token).perform(get("/api/workflow/workflowitems/search/findBySubmitter")
                .param("size", "1")
                .param("page", "1")
                .param("uuid", submitter1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowitems",
                    Matchers.contains(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2,
                            "Workflow Item 2", "2016-02-13"))))
            .andExpect(jsonPath("$._embedded.workflowitems",
                    Matchers.not(Matchers.contains(
                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workflow Item 1",
                                    "2017-10-17"),
                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3, "Workflow Item 3",
                                    "2016-02-13")))))
            .andExpect(jsonPath("$.page.size", is(1)))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

        // the second submitter has a single workspace
        getClient(token).perform(get("/api/workflow/workflowitems/search/findBySubmitter")
                .param("size", "20")
                .param("uuid", submitter2.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.workflowitems",
                    Matchers.contains(
                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3, "Workflow Item 3",
                                    "2016-02-13"))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    /**
     * A delete request over a workflowitem should result in abort the workflow sending the item back to the submitter
     * workspace
     *
     * @throws Exception
     */
    public void deleteOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .build();

        Item item = witem.getItem();

        //Add a bitstream to the item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, Charset.defaultCharset())) {
            bitstream = BitstreamBuilder
                    .createBitstream(context, item, is)
                    .withName("Bitstream1")
                    .withMimeType("text/plain").build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        String tokenSubmitter = getAuthToken(submitter.getEmail(), "dspace");

        // Delete the workflowitem
        getClient(token).perform(delete("/api/workflow/workflowitems/" + witem.getID()))
                    .andExpect(status().is(204));

        // Trying to get deleted workflowitem should fail with 404
        getClient(token).perform(get("/api/workflow/workflowitems/" + witem.getID()))
                   .andExpect(status().is(404));

        // the workflowitem's item should still exist
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                   .andExpect(status().is(200));

        // the workflowitem's bitstream should still exist
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().is(200));

        // a workspaceitem should exist now in the submitter workspace
        getClient(tokenSubmitter).perform(get("/api/submission/workspaceitems/search/findBySubmitter")
                .param("uuid", submitter.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems", Matchers.containsInAnyOrder(
                  WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(null, "Workflow Item 1",
                      "2017-10-17"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/submission/workspaceitems")))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    /**
     * Test the deposit of a workspaceitem POSTing to the resource workflowitems collection endpoint a workspaceitem (as
     * uri-list) in a collection without workflow. This corresponds to the deposit action done by the submitter.
     *
     * @throws Exception
     */
    public void depositWorkspaceItemWithoutWorkflowTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        //2. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.setCurrentUser(submitter);

        //3. a workspace item ready to go
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Submission Item")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .grantLicense()
                .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(submitter.getEmail(), password);

        // submit the workspaceitem to complete the deposit (as there is no workflow configured)
        getClient(authToken)
                .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                        .content("/api/submission/workspaceitems/" + wsitem.getID())
                        .contentType(textUriContentType))
                .andExpect(status().isCreated());
    }

    @Test
    /**
     * Test the creation of workflowitem POSTing to the resource workflowitems collection endpoint a workspaceitem (as
     * uri-list). This corresponds to the deposit action done by the submitter.
     *
     * @throws Exception
     */
    public void createWorkflowItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // hold the id of the created workflow item
        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            //** GIVEN **
            //1. A community with one collection.
            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                    .withWorkflowGroup(1, admin).build();

            //2. create a normal user to use as submitter
            EPerson submitter = EPersonBuilder.createEPerson(context)
                    .withEmail("submitter@example.com")
                    .withPassword("dspace")
                    .build();

            context.setCurrentUser(submitter);

            //3. a workspace item
            WorkspaceItem wsitem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                    .withTitle("Submission Item")
                    .withIssueDate("2017-10-17")
                    .grantLicense()
                    .build();

            context.restoreAuthSystemState();

            // get the submitter auth token
            String authToken = getAuthToken(submitter.getEmail(), "dspace");

            // submit the workspaceitem to start the workflow
            getClient(authToken)
                    .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                            .content("/api/submission/workspaceitems/" + wsitem.getID())
                            .contentType(textUriContentType))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$",
                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(null, "Submission Item", "2017-10-17")))
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // check that the workflowitem is persisted
            getClient(authToken).perform(get("/api/workflow/workflowitems/" + idRef.get())).andExpect(status().isOk())
                    .andExpect(jsonPath("$",
                            Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(null,
                                    "Submission Item", "2017-10-17"))));
        } finally {
            // remove the workflowitem if any
            WorkflowItemBuilder.deleteWorkflowItem(idRef.get());
        }
    }

    @Test
    /**
     * Test invalid attempts to create workflowitem POSTing to the resource workflowitems collection endpoint an not
     * existing workspaceitem (as uri-list) or an empty list. This should result in a 422 error
     *
     * @throws Exception
     */
    public void unvalidCreateWorkflowItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();
        // get the submitter auth token
        String authToken = getAuthToken(submitter.getEmail(), "dspace");

        // submit a not existing workspaceitem
        getClient(authToken)
                .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                        .content("/api/submission/workspaceitems/" + UUID.randomUUID().toString())
                        .contentType(textUriContentType))
                .andExpect(status().isUnprocessableEntity());

        // submit an invalid URL
        getClient(authToken)
                .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                        .content("")
                        .contentType(textUriContentType))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Test the exposition of validation error for missing required metadata
     *
     * @throws Exception
     */
    public void validationErrorsRequiredMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a workflow item will all the required fields
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .build();

        //4. a workflow item without the dateissued required field
        XmlWorkflowItem witemMissingFields = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/worfklowitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
        ;

        getClient(authToken).perform(get("/api/workflow/worfklowitems/" + witemMissingFields.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                        Matchers.contains(
                                hasJsonPath("$.paths", Matchers.contains(
                                        hasJsonPath("$", Matchers.is("/sections/traditionalpageone/dc.date.issued"))
                                )))))
        ;
    }

    @Test
    /**
     * Test the update of metadata
     *
     * @throws Exception
     */
    public void patchUpdateMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(2, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a claimed task with workflow item in edit step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .grantLicense()
            .build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        // a simple patch to update an existent metadata
        List<Operation> updateTitle = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        updateTitle.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);

        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.errors").doesNotExist())
                        .andExpect(jsonPath("$",
                                // check the new title and untouched values
                                Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                        "New Title", "2017-10-17", "ExtraEntry"))));

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            "New Title", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    @Ignore(value = "This demonstrate the bug logged in DS-4179")
    /**
     * Verify that update of metadata is forbidden in step 1.
     *
     * @throws Exception
     */
    public void patchUpdateMetadataStep1ForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        // a simple patch to update an existent metadata
        List<Operation> updateTitle = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        updateTitle.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);

        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden())
        ;

        // verify that the patch changes have been rejected
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            "New Title", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Test the update of metadata in step3
     *
     * @throws Exception
     */
    public void patchUpdateMetadataStep3Test() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(3, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a claimed task with workflow item in edit step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withSubject("ExtraEntry")
            .grantLicense()
            .build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        // a simple patch to update an existent metadata
        List<Operation> updateTitle = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        updateTitle.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));

        String patchBody = getPatchContent(updateTitle);

        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.errors").doesNotExist())
                        .andExpect(jsonPath("$",
                                // check the new title and untouched values
                                Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                        "New Title", "2017-10-17", "ExtraEntry"))))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            "New Title", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Test delete of a metadata
     *
     * @throws Exception
     */
    public void patchDeleteMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. some claimed tasks with workflow items in edit step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .grantLicense()
            .build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        ClaimedTask claimedTask2 = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withTitle("Workflow Item 2")
            .withIssueDate("2017-10-17")
            .withSubject("Subject1")
            .withSubject("Subject2")
            .withSubject("Subject3")
            .withSubject("Subject4")
            .grantLicense()
            .build();
        claimedTask2.setStepID("editstep");
        claimedTask2.setActionID("editaction");
        XmlWorkflowItem witemMultipleSubjects  = claimedTask2.getWorkflowItem();

        ClaimedTask claimedTask3 = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withTitle("Workflow Item 3")
            .withIssueDate("2017-10-17")
            .withSubject("Subject1")
            .withSubject("Subject2")
            .withSubject("Subject3")
            .withSubject("Subject4")
            .grantLicense()
            .build();
        claimedTask3.setStepID("editstep");
        claimedTask3.setActionID("editaction");
        XmlWorkflowItem witemWithTitleDateAndSubjects = claimedTask3.getWorkflowItem();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        // try to remove the title
        List<Operation> removeTitle = new ArrayList<Operation>();
        removeTitle.add(new RemoveOperation("/sections/traditionalpageone/dc.title/0"));

        String patchBody = getPatchContent(removeTitle);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                                Matchers.contains(hasJsonPath("$.paths",
                                        Matchers.contains(
                                                hasJsonPath("$",
                                                        Matchers.is("/sections/traditionalpageone/dc.title")))))))
                            .andExpect(jsonPath("$",
                                    // check the new title and untouched values
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                            null, "2017-10-17", "ExtraEntry"))));
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[?(@.message=='error.validation.required')]",
                    Matchers.contains(
                            hasJsonPath("$.paths", Matchers.contains(
                                    hasJsonPath("$", Matchers.is("/sections/traditionalpageone/dc.title"))
                            )))))
            .andExpect(jsonPath("$",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            null, "2017-10-17", "ExtraEntry"))))
        ;

        // try to remove a metadata in a specific position
        List<Operation> removeMidSubject = new ArrayList<Operation>();
        removeMidSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/1"));

        patchBody = getPatchContent(removeMidSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject3")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Subject4")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject3")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Subject4")))
        ;

        List<Operation> removeFirstSubject = new ArrayList<Operation>();
        removeFirstSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/0"));

        patchBody = getPatchContent(removeFirstSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject4")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject4")))
        ;

        List<Operation> removeLastSubject = new ArrayList<Operation>();
        removeLastSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/1"));

        patchBody = getPatchContent(removeLastSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject3")))
        ;

        List<Operation> removeFinalSubject = new ArrayList<Operation>();
        removeFinalSubject.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/0"));

        patchBody = getPatchContent(removeFinalSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witemMultipleSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witemMultipleSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;

        // remove all the subjects with a single operation
        List<Operation> removeSubjectsAllAtOnce = new ArrayList<Operation>();
        removeSubjectsAllAtOnce.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject"));

        patchBody = getPatchContent(removeSubjectsAllAtOnce);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witemWithTitleDateAndSubjects.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witemWithTitleDateAndSubjects.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject']").doesNotExist())
        ;
    }

    @Test
    /**
     * Test the addition of metadata
     *
     * @throws Exception
     */
    public void patchAddMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a claimed task with workflow item in edit step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withIssueDate("2017-10-17")
            .withSubject("ExtraEntry")
            .grantLicense()
            .build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        // try to add the title
        List<Operation> addTitle = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        values.add(value);
        addTitle.add(new AddOperation("/sections/traditionalpageone/dc.title", values));

        String patchBody = getPatchContent(addTitle);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$",
                                    // check if the new title if back and the other values untouched
                                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                            "New Title", "2017-10-17", "ExtraEntry"))));
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$",
                    Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                            "New Title", "2017-10-17", "ExtraEntry"))))
        ;
    }

    @Test
    /**
     * Test the addition of metadata
     *
     * @throws Exception
     */
    public void patchAddMultipleMetadataValuesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, eperson).build();

        //2. create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword("dspace")
                .build();

        context.setCurrentUser(submitter);

        //3. a claimed task with workflow item in edit step
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, col1, eperson)
            .withTitle("Workflow Item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .grantLicense()
            .build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        // try to add multiple subjects at once
        List<Operation> addSubjects = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value1 = new HashMap<String, String>();
        value1.put("value", "Subject1");
        Map<String, String> value2 = new HashMap<String, String>();
        value2.put("value", "Subject2");
        values.add(value1);
        values.add(value2);

        addSubjects.add(new AddOperation("/sections/traditionalpagetwo/dc.subject", values));

        String patchBody = getPatchContent(addSubjects);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject2")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject2")))
        ;

        // add a subject in the first position
        List<Operation> addFirstSubject = new ArrayList<Operation>();
        Map<String, String> firstSubject = new HashMap<String, String>();
        firstSubject.put("value", "First Subject");

        addFirstSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/0", firstSubject));

        patchBody = getPatchContent(addFirstSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Subject2")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Subject2")))
        ;

        // add a subject in a central position
        List<Operation> addMidSubject = new ArrayList<Operation>();
        Map<String, String> midSubject = new HashMap<String, String>();
        midSubject.put("value", "Mid Subject");

        addMidSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/2", midSubject));

        patchBody = getPatchContent(addMidSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Mid Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value",
                                    is("Subject2")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Mid Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value", is("Subject2")))
        ;

        // append a last subject without specifying the index
        List<Operation> addLastSubject = new ArrayList<Operation>();
        Map<String, String> lastSubject = new HashMap<String, String>();
        lastSubject.put("value", "Last Subject");

        addLastSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/4", lastSubject));

        patchBody = getPatchContent(addLastSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Mid Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value",
                                    is("Subject2")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value",
                                    is("Last Subject")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Mid Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value", is("Subject2")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value", is("Last Subject")))
        ;

        // append a last subject without specifying the index
        List<Operation> addFinalSubject = new ArrayList<Operation>();
        Map<String, String> finalSubject = new HashMap<String, String>();
        finalSubject.put("value", "Final Subject");

        addFinalSubject.add(new AddOperation("/sections/traditionalpagetwo/dc.subject/-", finalSubject));

        patchBody = getPatchContent(addFinalSubject);
        getClient(authToken).perform(patch("/api/workflow/workflowitems/" + witem.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.errors").doesNotExist())
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value",
                                    is("First Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value",
                                    is("Subject1")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value",
                                    is("Mid Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value",
                                    is("Subject2")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value",
                                    is("Last Subject")))
                            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][5].value",
                                    is("Final Subject")))
        ;

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][0].value", is("First Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][1].value", is("Subject1")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][2].value", is("Mid Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][3].value", is("Subject2")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][4].value", is("Last Subject")))
            .andExpect(jsonPath("$.sections.traditionalpagetwo['dc.subject'][5].value", is("Final Subject")))
        ;
    }


    @Test
    public void findByItemUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();

        //2. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/workflowitems/search/item")
                                        .param("uuid", String.valueOf(witem.getItem().getID())))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$",
                                                Matchers.is(
                                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                              "Workflow Item 1", "2017-10-17", "ExtraEntry"))));

    }

    @Test
    public void findByItemUuidMissingParameterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();


        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/workflow/workflowitems/search/item"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void findByItemUuidDoesntExistTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();


        Item item = ItemBuilder.createItem(context, col1).build();
        //2. a workspace item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/workflow/workflowitems/search/item")
                                     .param("uuid", String.valueOf(item.getID())))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void findByItemUuidForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(admin);
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();


        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/workflow/workflowitems/search/item")
                                     .param("uuid", String.valueOf(witem.getItem().getID())))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void findByItemUuidUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();


        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();
        getClient().perform(get("/api/workflow/workflowitems/search/item")
                                .param("uuid", String.valueOf(witem.getItem().getID())))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void stepEmbedTest() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. A community-collection structure with one parent community with sub-community and three collections
        // (different workflow steps and reviewers).
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        EPerson reviewer1 = EPersonBuilder.createEPerson(context).withEmail("reviewer1@example.com")
                                          .withPassword(password).build();

        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, reviewer1).build();

        EPerson reviewer2 = EPersonBuilder.createEPerson(context).withEmail("reviewer2@example.com")
                                          .withPassword(password).build();

        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                                           .withWorkflowGroup(2, reviewer2).build();

        EPerson reviewer3 = EPersonBuilder.createEPerson(context).withEmail("reviewer3@example.com")
                                          .withPassword(password).build();

        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("Collection 3")
                                           .withWorkflowGroup(3, reviewer3).build();

        //2. three workflow items in the three collections (this will lead to pool task)
        XmlWorkflowItem witem1 = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                    .withTitle("Workflow Item 1")
                                                    .withIssueDate("2016-02-13")
                                                    .build();

        XmlWorkflowItem witem2 = WorkflowItemBuilder.createWorkflowItem(context, col2)
                                                    .withTitle("Workflow Item 2")
                                                    .withIssueDate("2016-02-13")
                                                    .build();

        XmlWorkflowItem witem3 = WorkflowItemBuilder.createWorkflowItem(context, col3)
                                                    .withTitle("Workflow Item 3")
                                                    .withIssueDate("2016-02-13")
                                                    .build();

        Step step = xmlWorkflowFactory.getStepByName("reviewstep");

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/workflow/workflowitems/" + witem1.getID())
                                .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$",
                                       WorkflowItemMatcher.matchItemWithTitleAndDateIssued(witem1,
                                                                   "Workflow Item 1", "2016-02-13")))
                   .andExpect(jsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)));

        step = xmlWorkflowFactory.getStepByName("editstep");

        getClient(token).perform(get("/api/workflow/workflowitems/" + witem2.getID())
                                     .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$",
                                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(witem2,
                                                                 "Workflow Item 2", "2016-02-13")))
                        .andExpect(jsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)));

        step = xmlWorkflowFactory.getStepByName("finaleditstep");

        getClient(token).perform(get("/api/workflow/workflowitems/" + witem3.getID())
                                     .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$",
                                            WorkflowItemMatcher.matchItemWithTitleAndDateIssued(witem3,
                                                                 "Workflow Item 3", "2016-02-13")))
                        .andExpect(jsonPath("$._embedded.step", WorkflowStepMatcher.matchWorkflowStepEntry(step)));
    }

    @Test
    public void discoverableNestedLinkTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links",Matchers.allOf(
                                hasJsonPath("$.claimedtasks.href",
                                         is("http://localhost/api/workflow/claimedtasks")),
                                hasJsonPath("$.claimedtasks-search.href",
                                         is("http://localhost/api/workflow/claimedtasks/search")),
                                hasJsonPath("$.pooltasks.href",
                                         is("http://localhost/api/workflow/pooltasks")),
                                hasJsonPath("$.pooltasks-search.href",
                                         is("http://localhost/api/workflow/pooltasks/search"))
                        )));
    }

    @Test
    public void findOneFullProjectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();

        //2. a workflow item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/workflow/workflowitems/" + witem.getID())
                                    .param("projection", "full"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.collection._embedded.adminGroup").doesNotExist());

        getClient(adminToken).perform(get("/api/workflow/workflowitems/" + witem.getID())
                                         .param("projection", "full"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.collection._embedded.adminGroup", nullValue()));

    }

    @Test
    public void whenWorkspaceitemBecomeWorkflowitemWithAccessConditionsTheBitstreamMustBeDownloableTest()
            throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password)
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password)
                                          .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection1 = CollectionBuilder.createCollection(context, parentCommunity)
                                                  .withName("Collection 1")
                                                  .withWorkflowGroup(1, reviewer1)
                                                  .build();

        Bitstream bitstream = null;
        WorkspaceItem witem = null;

        String bitstreamContent = "0123456789";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, Charset.defaultCharset())) {

            context.setCurrentUser(submitter);
            witem = WorkspaceItemBuilder.createWorkspaceItem(context, collection1)
                                        .withTitle("Test WorkspaceItem")
                                        .withIssueDate("2019-10-01")
                                        .grantLicense()
                                        .build();

            bitstream = BitstreamBuilder.createBitstream(context, witem.getItem(), is)
                                        .withName("Test bitstream")
                                        .withDescription("This is a bitstream to test range requests")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenSubmitter = getAuthToken(submitter.getEmail(), password);
        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);

        // submitter can download the bitstream
        getClient(tokenSubmitter).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                 .andExpect(status().isOk())
                                 .andExpect(header().string("Accept-Ranges", "bytes"))
                                 .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                                 .andExpect(content().contentType("text/plain;charset=UTF-8"))
                                 .andExpect(content().bytes(bitstreamContent.getBytes()));

        // reviewer can't still download the bitstream
        getClient(tokenReviewer1).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                 .andExpect(status().isForbidden());

        // others can't download the bitstream
        getClient(tokenEPerson).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                               .andExpect(status().isForbidden());

        // create a list of values to use in add operation
        List<Operation> addAccessCondition = new ArrayList<>();
        List<Map<String, String>> accessConditions = new ArrayList<Map<String,String>>();

        Map<String, String> value = new HashMap<>();
        value.put("name", "administrator");

        accessConditions.add(value);

        addAccessCondition.add(new AddOperation("/sections/upload/files/0/accessConditions", accessConditions));

        String patchBody = getPatchContent(addAccessCondition);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].name",is("administrator")))
                 .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].startDate",nullValue()))
                 .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].endDate", nullValue()));

        // verify that the patch changes have been persisted
        getClient(tokenSubmitter).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].name",is("administrator")))
                 .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].startDate",nullValue()))
                 .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].endDate", nullValue()));

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();

        try {
            // submit the workspaceitem to start the workflow
            getClient(tokenSubmitter).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                     .content("/api/submission/workspaceitems/" + witem.getID())
                     .contentType(textUriContentType))
                     .andExpect(status().isCreated())
                     .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // check that the workflowitem is persisted
            getClient(tokenSubmitter).perform(get("/api/workflow/workflowitems/" + idRef.get()))
                     .andExpect(status().isOk())
                     .andExpect(jsonPath("$",Matchers.is(WorkflowItemMatcher.
                           matchItemWithTitleAndDateIssued(null, "Test WorkspaceItem", "2019-10-01"))))
                     .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].name",is("administrator")))
                     .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].startDate",nullValue()))
                     .andExpect(jsonPath("$.sections.upload.files[0].accessConditions[0].endDate", nullValue()));

            // reviewer can download the bitstream
            getClient(tokenReviewer1).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                     .andExpect(status().isOk())
                     .andExpect(header().string("Accept-Ranges", "bytes"))
                     .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                     .andExpect(content().contentType("text/plain;charset=UTF-8"))
                     .andExpect(content().bytes(bitstreamContent.getBytes()));

            // submitter can download the bitstream
            getClient(tokenSubmitter).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                     .andExpect(status().isOk())
                     .andExpect(header().string("Accept-Ranges", "bytes"))
                     .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                     .andExpect(content().contentType("text/plain;charset=UTF-8"))
                     .andExpect(content().bytes(bitstreamContent.getBytes()));

            // others can't download the bitstream
            getClient(tokenEPerson).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                   .andExpect(status().isForbidden());
        } finally {
            // remove the workflowitem if any
            WorkflowItemBuilder.deleteWorkflowItem(idRef.get());
        }
    }

    @Test
    public void whenWorkspaceitemBecomeWorkflowitemWithAccessConditionsTheItemMustBeAccessibleTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password)
                                          .build();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                                          .withEmail("reviewer1@example.com")
                                          .withPassword(password)
                                          .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection collection1 = CollectionBuilder.createCollection(context, parentCommunity)
                                                  .withName("Collection 1")
                                                  .withWorkflowGroup(1, reviewer1)
                                                  .build();

        context.setCurrentUser(submitter);

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, collection1)
                                    .withTitle("Test WorkspaceItem")
                                    .withIssueDate("2019-10-01")
                                    .grantLicense()
                                    .build();

        UUID itemUuid = witem.getItem().getID();

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        String tokenSubmitter = getAuthToken(submitter.getEmail(), password);
        String tokenReviewer1 = getAuthToken(reviewer1.getEmail(), password);

        // submitter can see the item
        getClient(tokenSubmitter).perform(get("/api/core/items/" + itemUuid))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$",  ItemMatcher.matchItemWithTitleAndDateIssued(witem.getItem(),
                                                     "Test WorkspaceItem", "2019-10-01")));

        // reviewer can't still see the item
        getClient(tokenReviewer1).perform(get("/api/core/items/" + itemUuid))
                                 .andExpect(status().isForbidden());

        // others can't see the item
        getClient(tokenEPerson).perform(get("/api/core/items/" + itemUuid))
                               .andExpect(status().isForbidden());

        // create a list of values to use in add operation
        List<Operation> addAccessCondition = new ArrayList<Operation>();
        List<Map<String, String>> accessConditions = new ArrayList<Map<String,String>>();

        Map<String, String> accessCondition2 = new HashMap<String, String>();
        accessCondition2.put("name", "administrator");
        accessConditions.add(accessCondition2);

        addAccessCondition.add(new AddOperation("/sections/defaultAC/accessConditions",
                               accessConditions));

        String patchBody = getPatchContent(addAccessCondition);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                                 .andExpect(status().isOk());

            // verify that the patch changes have been persisted
        getClient(tokenSubmitter).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.defaultAC.discoverable", is(true)))
                 .andExpect(jsonPath("$.sections.defaultAC.accessConditions[0].name", is("administrator")))
                 .andExpect(jsonPath("$.sections.defaultAC.accessConditions[1].name").doesNotExist());

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            // submit the workspaceitem to start the workflow
            getClient(tokenSubmitter).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                     .content("/api/submission/workspaceitems/" + witem.getID())
                     .contentType(textUriContentType))
                     .andExpect(status().isCreated())
                     .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // check that the workflowitem is persisted
            getClient(tokenSubmitter).perform(get("/api/workflow/workflowitems/" + idRef.get()))
                     .andExpect(status().isOk())
                     .andExpect(jsonPath("$",Matchers.is(WorkflowItemMatcher.
                                   matchItemWithTitleAndDateIssued(null, "Test WorkspaceItem", "2019-10-01"))))
                     .andExpect(jsonPath("$.sections.defaultAC.discoverable", is(true)))
                     .andExpect(jsonPath("$.sections.defaultAC.accessConditions[0].name", is("administrator")))
                     .andExpect(jsonPath("$.sections.defaultAC.accessConditions[1].name").doesNotExist());

            // submitter can see the item
            getClient(tokenSubmitter).perform(get("/api/core/items/" + itemUuid))
                     .andExpect(status().isOk())
                     .andExpect(jsonPath("$", ItemMatcher.
                                matchItemWithTitleAndDateIssued(witem.getItem(), "Test WorkspaceItem", "2019-10-01")));

            // now reviewer can see the item
            getClient(tokenReviewer1).perform(get("/api/core/items/" + itemUuid))
                     .andExpect(status().isOk())
                     .andExpect(jsonPath("$", ItemMatcher.
                               matchItemWithTitleAndDateIssued(witem.getItem(), "Test WorkspaceItem", "2019-10-01")));

            // others can't see the item
            getClient(tokenEPerson).perform(get("/api/core/items/" + itemUuid))
                                   .andExpect(status().isForbidden());
        } finally {
            // remove the workflowitem if any
            WorkflowItemBuilder.deleteWorkflowItem(idRef.get());
        }
    }

}
