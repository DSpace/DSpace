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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.WorkflowItemBuilder;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test suite for the WorkflowItem endpoint
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class WorkflowItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    /**
     * All the workflowitems should be returned regardless of the collection where they were created
     * 
     * @throws Exception
     */
    public void findAllTest() throws Exception {
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

        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is(WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                "Workflow Item 1", "2017-10-17", "ExtraEntry"))));
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

        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID() + "/collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers
                        .is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle()))));

        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID() + "/item")).andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemWithTitleAndDateIssued(witem.getItem(),
                        "Workflow Item 1", "2017-10-17"))));

        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID() + "/submissionDefinition"))
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
     * Removing a workflowitem should result in delete of all the underline resources (item and bitstreams)
     * 
     * @throws Exception
     */
    @Ignore
    public void deleteOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                .withWorkflowGroup(1, admin).build();

        //2. a workspace item
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                .withTitle("Workflow Item 1")
                .withIssueDate("2017-10-17")
                .build();

        Item item = witem.getItem();

        //Add a bitstream to the item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder
                    .createBitstream(context, item, is)
                    .withName("Bitstream1")
                    .withMimeType("text/plain").build();
        }

        String token = getAuthToken(admin.getEmail(), password);

        //Delete the workflowitem
        getClient(token).perform(delete("/api/workflow/workflowitems/" + witem.getID()))
                    .andExpect(status().is(204));

        //Trying to get deleted item should fail with 404
        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted workflowitem's item should fail with 404
        getClient().perform(get("/api/core/items/" + item.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted workflowitem's bitstream should fail with 404
        getClient().perform(get("/api/core/biststreams/" + bitstream.getID()))
                   .andExpect(status().is(404));
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

}
