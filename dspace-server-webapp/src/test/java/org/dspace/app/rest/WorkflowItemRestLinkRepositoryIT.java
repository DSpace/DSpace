/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test suite for the WorkflowItem Link repositories
 */
public class WorkflowItemRestLinkRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    /**
     * The workflowitem resource endpoint must have an embeddable submitter
     *
     * @throws Exception
     */
    public void findOneEmbedSubmitterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@dspace.org")
                                          .withNameInMetadata("Sub", "Mitter")
                                          .build();

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
                                           .withWorkflowGroup("reviewer", admin)
                                           .build();

        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withSubmitter(submitter)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                            .andExpect(jsonPath("$", Matchers.is(
                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                                                                  "Workflow Item 1",
                                                                                                  "2017-10-17",
                                                                                                  "ExtraEntry"))))
                            .andExpect(jsonPath("$", JsonPathMatchers.hasNoJsonPath("$._embedded.submitter")));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()).param("embed", "submitter"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", Matchers.is(
                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                                                                  "Workflow Item 1",
                                                                                                  "2017-10-17",
                                                                                                  "ExtraEntry"))))
                            .andExpect(jsonPath("$._embedded.submitter", Matchers.is(
                                    EPersonMatcher.matchEPersonEntry(submitter))));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID() + "/submitter"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", EPersonMatcher.matchEPersonEntry(submitter)));

    }

    @Test
    /**
     * The workflowitem resource endpoint must have an embeddable collection
     *
     * @throws Exception
     */
    public void findOneEmbedCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@dspace.org")
                                          .withNameInMetadata("Sub", "Mitter")
                                          .build();

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
                                           .withWorkflowGroup("reviewer", admin)
                                           .build();

        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withSubmitter(submitter)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                            .andExpect(jsonPath("$", Matchers.is(
                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                                                                  "Workflow Item 1",
                                                                                                  "2017-10-17",
                                                                                                  "ExtraEntry"))))
                            .andExpect(jsonPath("$", JsonPathMatchers.hasNoJsonPath("$._embedded.collection")));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()).param("embed", "collection"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", Matchers.is(
                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                                                                  "Workflow Item 1",
                                                                                                  "2017-10-17",
                                                                                                  "ExtraEntry"))))
                            .andExpect(jsonPath("$._embedded.collection", Matchers.is(
                                    CollectionMatcher.matchCollection(col1))));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID() + "/collection"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", CollectionMatcher.matchCollection(col1)));

    }

    @Test
    /**
     * The workflowitem resource endpoint must have an embeddable item
     *
     * @throws Exception
     */
    public void findOneEmbedItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@dspace.org")
                                          .withNameInMetadata("Sub", "Mitter")
                                          .build();

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
                                           .withWorkflowGroup("reviewer", admin)
                                           .build();

        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withSubmitter(submitter)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry")
                                                   .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                            .andExpect(jsonPath("$", Matchers.is(
                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                                                                  "Workflow Item 1",
                                                                                                  "2017-10-17",
                                                                                                  "ExtraEntry"))))
                            .andExpect(jsonPath("$", JsonPathMatchers.hasNoJsonPath("$._embedded.item")));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID()).param("embed", "item"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", Matchers.is(
                                    WorkflowItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                                                                                  "Workflow Item 1",
                                                                                                  "2017-10-17",
                                                                                                  "ExtraEntry"))))
                            .andExpect(jsonPath("$._embedded.item", Matchers.is(
                                    ItemMatcher.matchItemProperties(witem.getItem()))));

        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID() + "/item"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(witem.getItem())));

    }


}
