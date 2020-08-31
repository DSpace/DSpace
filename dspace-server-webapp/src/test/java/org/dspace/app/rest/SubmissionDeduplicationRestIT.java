/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test suite for testing Deduplication operations
 * 
 * @author fcadili (francecso.cadili at 4science.it)
 *
 */
public class SubmissionDeduplicationRestIT extends AbstractControllerIntegrationTest {
    @Test
    /**
     * Test reject deduplication during workspace submission. Both verify and reject
     * operation are tested. The reference object of the test is an item.
     *
     * @throws Exception
     */
    public void workspaceItemsAndItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson itemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withPassword(password).build();
        EPerson workspaceItemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter2@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(itemSubmitter).build();
        Collection colWorkspace = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withSubmitterGroup(workspaceItemSubmitter).build();

        // 3. create an item with title "Sample submission" using the first submitter
        context.setCurrentUser(itemSubmitter);
        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workflow items with the second submitter
        context.setCurrentUser(workspaceItemSubmitter);
        String authToken = getAuthToken(workspaceItemSubmitter.getEmail(), password);

        // Test reject patch operation with a workspace item
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace)
                .withTitle("Sample submission").withIssueDate("2020-02-01")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();
        dedupService.commit();
        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());

        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision")
                        .doesNotExist());

        // try to reject
        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + item.getID() + "/submitterDecision", value));

        String patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision",
                        is("reject")));
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision",
                        is("reject")));

        String firstWitemUuid = witem.getItem().getID().toString();
        witem = null;
        detectDuplicate = null;
        value = null;
        patchBody = null;
        pdf = null;

        // 4b. create another workflow items with the second submitter
        context.turnOffAuthorisationSystem();

        // Test verify patch operation with another workspace item
        pdf = getClass().getResourceAsStream("simple-article.pdf");
        witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace).withTitle("Sample submission")
                .withIssueDate("2021-01-01").withFulltext("article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision")
                        .doesNotExist())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision")
                                .doesNotExist());

        // patch verify, the item with id item.getID()
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + item.getID() + "/submitterDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision")
                                .doesNotExist());
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision")
                                .doesNotExist());

        detectDuplicate = null;
        value = null;
        patchBody = null;

        // patch verify, the first workspace item with id firstWitemUuid
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test2");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + firstWitemUuid + "/submitterDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterNote",
                        is("test2")));
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterNote",
                        is("test2")));
    }

    @Test
    /**
     * Test reject deduplication during workspace submission. Both verify and reject
     * operation are tested. The reference object of the test is a workflow item.
     *
     * @throws Exception
     */
    public void workspaceItemsAndWorkflowItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson reviewer = EPersonBuilder.createEPerson(context).withEmail("reviewer@example.com")
                .withPassword(password).build();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colWorkflow = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withWorkflowGroup(1, eperson).build();
        Collection colWorkspace = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withSubmitterGroup(submitter).build();

        // 3. create a workflow item title "Sample submission"
        context.setCurrentUser(reviewer);

        XmlWorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, colWorkflow)
                .withTitle("Sample submission").withIssueDate("2017-10-17").withAuthor("Smith, Donald")
                .withAuthor("Doe, John").withSubject("ExtraEntry").build();

        // 4a. create workflow items with the second submitter
        context.setCurrentUser(submitter);
        String authToken = getAuthToken(submitter.getEmail(), password);

        // Test reject patch operation with a workspace item
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace)
                .withTitle("Sample submission").withIssueDate("2020-02-01")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();
        String workflowItemId = workflowItem.getItem().getID().toString();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision")
                                .doesNotExist());

        // try to reject
        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + workflowItemId + "/submitterDecision", value));

        String patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision",
                                is("reject")));
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision",
                                is("reject")));

        String firstWitemUuid = witem.getItem().getID().toString();
        witem = null;
        detectDuplicate = null;
        value = null;
        patchBody = null;
        pdf = null;

        // 4b. create another workflow items with the second submitter
        context.turnOffAuthorisationSystem();

        // Test verify patch operation with another workspace item
        pdf = getClass().getResourceAsStream("simple-article.pdf");
        witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace).withTitle("Sample submission")
                .withIssueDate("2021-01-01").withFulltext("article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision")
                                .doesNotExist())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision")
                                .doesNotExist());

        // patch verify, the workflow item with id workflowItemId
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + workflowItemId + "/submitterDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision")
                                .doesNotExist());
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision")
                                .doesNotExist());

        detectDuplicate = null;
        value = null;
        patchBody = null;

        // try to verify the fist workspace item
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test2");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + firstWitemUuid + "/submitterDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterNote",
                        is("test2")));
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].matchObject.id",
                        is(workflowItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workflowItemId + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].submitterNote",
                        is("test2")));
    }

    @Test
    /**
     * Test reject deduplication during workspace submission. A workspace item with
     * the same title of the one created before starting submission. The reference
     * object of the test are only workspace items.
     *
     * @throws Exception
     */
    public void workspaceItemsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson submitterOne = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withPassword(password).build();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter2@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colWorkspaceOne = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(submitterOne).build();
        Collection colWorkspace = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withSubmitterGroup(submitter).build();

        // 3. create an item with title "Sample submission" using the first submitter
        context.setCurrentUser(submitterOne);
        WorkspaceItem witemOne = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspaceOne)
                .withTitle("Sample submission").withIssueDate("2020-01-31").withAuthor("Cadili, Francesco")
                .withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workflow items with the submitter
        context.setCurrentUser(submitter);
        String authToken = getAuthToken(submitter.getEmail(), password);

        // Test reject patch operation with a workspace item
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace)
                .withTitle("Sample submission").withIssueDate("2020-02-01")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision").doesNotExist());

        // try to reject
        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(new AddOperation(
                "/sections/detect-duplicate/matches/" + witemOne.getItem().getID() + "/submitterDecision", value));

        String patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision", is("reject")));
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision", is("reject")));

        String secondWitemUuid = witem.getItem().getID().toString();
        witem = null;
        detectDuplicate = null;
        value = null;
        patchBody = null;
        pdf = null;

        // 4b. create another workflow items with the submitter
        context.turnOffAuthorisationSystem();

        // Test verify patch operation with another workspace item
        pdf = getClass().getResourceAsStream("simple-article.pdf");
        witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace).withTitle("Sample submission")
                .withIssueDate("2021-01-01").withFulltext("article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision").doesNotExist())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].matchObject.id",
                        is(secondWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterDecision")
                                .doesNotExist());

        // try to verify the workspace one
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test");
        detectDuplicate.add(new AddOperation(
                "/sections/detect-duplicate/matches/" + witemOne.getItem().getID() + "/submitterDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision", is("verify")))
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].matchObject.id",
                        is(secondWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterDecision")
                                .doesNotExist());
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision", is("verify")))
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].matchObject.id",
                        is(secondWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterDecision")
                                .doesNotExist());

        detectDuplicate = null;
        value = null;
        patchBody = null;

        // try to verify the second workspace item
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test");
        detectDuplicate.add(new AddOperation(
                "/sections/detect-duplicate/matches/" + secondWitemUuid + "/submitterDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision", is("verify")))
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].matchObject.id",
                        is(secondWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterNote",
                        is("test")));
        dedupService.commit();
        // check that changes persist
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID()
                        + "'].submitterDecision", is("verify")))
                .andExpect(jsonPath(
                        "$.sections['detect-duplicate'].matches['" + witemOne.getItem().getID() + "'].submitterNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].matchObject.id",
                        is(secondWitemUuid)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + secondWitemUuid + "'].submitterNote",
                        is("test")));
    }

    @Test
    /**
     * Test reject deduplication during workspace submission. Both verify and reject
     * operation are tested. The reference object of the test is an item.
     *
     * @throws Exception
     */
    public void workspaceItemCheckFailures() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson itemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withPassword(password).build();
        EPerson workspaceItemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter2@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(itemSubmitter).build();
        Collection colWorkspace = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withSubmitterGroup(workspaceItemSubmitter).build();

        // 3. create an item with title "Sample submission" using the first submitter
        context.setCurrentUser(itemSubmitter);
        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workflow items with the second submitter
        context.setCurrentUser(workspaceItemSubmitter);
        String authToken = getAuthToken(workspaceItemSubmitter.getEmail(), password);

        // Test reject patch operation with a workspace item
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace)
                .withTitle("Sample submission").withIssueDate("2020-02-01")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())));

        // generate some Unprocessable exceptions

        // generate a random UUID
        UUID id = UUID.randomUUID();
        int c = 0;
        while (id == item.getID() && c < 10) {
            id = UUID.randomUUID();
            c++;
        }

        // Ask for a patch with an UUID not in the list of duplicates
        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(new AddOperation("/sections/detect-duplicate/matches/" + id + "/submitterDecision", value));

        String patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with a number as UUID
        patchBody = null;
        detectDuplicate.clear();

        detectDuplicate
                .add(new AddOperation("/sections/detect-duplicate/matches/" + "1001" + "/submitterDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with an invalid operation
        value.clear();
        patchBody = null;
        detectDuplicate.clear();

        value.put("value", "invalid-op");
        value.put("note", null);
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + item.getID() + "/submitterDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with wrong type
        value.clear();
        patchBody = null;
        detectDuplicate.clear();

        value.put("value", "reject");
        value.put("note", null);
        value.put("type", "WORKFLOW");
        detectDuplicate.add(new AddOperation("/sections/detect-duplicate/matches/" + id + "/submitterDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with the wrong decision type
        value.clear();
        patchBody = null;
        detectDuplicate.clear();

        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(new AddOperation("/sections/detect-duplicate/matches/" + id + "/workflowDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    /**
     * Test reject deduplication during workflow submission. Both verify and reject
     * operation are tested. The reference object of the test is an item.
     *
     * @throws Exception
     */
    public void workflowItemsAndItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson itemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withNameInMetadata("submitter1", "").withPassword(password).build();
        EPerson reviewer = EPersonBuilder.createEPerson(context).withEmail("reviewer@example.com")
                .withNameInMetadata("reviewer", "").withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(itemSubmitter).build();
        Collection colWorkflow = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer).build();

        // 3. create an item with title "Sample submission" using the first submitter
        String submitterTocken = getAuthToken(itemSubmitter.getEmail(), password);
        context.setCurrentUser(itemSubmitter);
        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workflow item will all the required fields using reviewer
        String reviewerToken = getAuthToken(reviewer.getEmail(), password);
        context.setCurrentUser(reviewer);

        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, colWorkflow, reviewer)
                .withTitle("Sample submission").withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        /*
         * ^ BUG: Since DSpaceObject.metadata is set to FetchType.LAZY here a
         * "org.hibernate.LazyInitializationException: failed to lazily initialize a
         * collection of role: org.dspace.content.DSpaceObject.metadata, could not
         * initialize proxy - no Session" is generated.
         * 
         * Workaround: Set fetch type of DSpaceObject.metadata to EAGER.
         */
        pdf.close();

        // Test reject patch operation with a workspace item
        context.restoreAuthSystemState();

        // check security
        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision")
                        .doesNotExist());

        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + item.getID() + "/workflowDecision", value));

        String patchBody = getPatchContent(detectDuplicate);

        // check security
        getClient(submitterTocken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // execute the patch
        getClient(reviewerToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision",
                        is("reject")));
        dedupService.commit();
        // check that changes persist
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision",
                        is("reject")));

        String firstWitemUuid = witem.getItem().getID().toString();
        claimedTask = null;
        witem = null;
        detectDuplicate = null;
        value = null;
        patchBody = null;

        // 4b. create another workflow items with the second submitter
        context.turnOffAuthorisationSystem();

        // Test verify patch operation with another workspace item
        pdf = getClass().getResourceAsStream("simple-article.pdf");
        claimedTask = ClaimedTaskBuilder.createClaimedTask(context, colWorkflow, reviewer)
                .withTitle("Sample submission").withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        witem = claimedTask.getWorkflowItem();
        pdf.close();
        context.restoreAuthSystemState();

        // check security
        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision")
                        .doesNotExist())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision")
                        .doesNotExist());

        // patch verify, the item with id item.getID()
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + item.getID() + "/workflowDecision", value));

        patchBody = getPatchContent(detectDuplicate);

        // check security
        getClient(submitterTocken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // patch operation
        getClient(reviewerToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision")
                        .doesNotExist());
        dedupService.commit();
        // check that changes persist
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision")
                        .doesNotExist());

        detectDuplicate = null;
        value = null;
        patchBody = null;

        // patch verify, the first workspace item with id firstWitemUuid
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test2");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + firstWitemUuid + "/workflowDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(reviewerToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowNote",
                        is("test2")));
        dedupService.commit();
        // check that changes persist
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowNote",
                        is("test2")));
    }

    @Test
    /**
     * Test reject deduplication during workflow submission. Both verify and reject
     * operation are tested. The reference object of the test is an item.
     *
     * @throws Exception
     */
    public void workflowItemsAndWorkspaceItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withNameInMetadata("submitter1", "").withPassword(password).build();
        EPerson reviewer = EPersonBuilder.createEPerson(context).withEmail("reviewer@example.com")
                .withNameInMetadata("reviewer", "").withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(submitter).build();
        Collection colWorkflow = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer).build();

        // 3. create an item with title "Sample submission" using the first submitter
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        context.setCurrentUser(submitter);
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, colItem)
                .withTitle("Sample submission").withIssueDate("2020-01-31").withAuthor("Cadili, Francesco")
                .withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workspace items with the reviewer
        context.setCurrentUser(reviewer);
        String reviewerToken = getAuthToken(reviewer.getEmail(), password);

        // 3. a workflow item will all the required fields
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        ClaimedTask claimedTask = ClaimedTaskBuilder.createClaimedTask(context, colWorkflow, reviewer)
                .withTitle("Sample submission").withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        XmlWorkflowItem witem = claimedTask.getWorkflowItem();
        /*
         * ^ BUG: Since DSpaceObject.metadata is set to FetchType.LAZY here a
         * "org.hibernate.LazyInitializationException: failed to lazily initialize a
         * collection of role: org.dspace.content.DSpaceObject.metadata, could not
         * initialize proxy - no Session" is generated.
         * 
         * Workaround: set the metadata of EPerson objects.
         */
        pdf.close();

        // Test reject patch operation with a workspace item
        context.restoreAuthSystemState();

        // check security
        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        String workspaceItemId = workspaceItem.getItem().getID().toString();
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision")
                                .doesNotExist());

        // try to reject
        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + workspaceItemId + "/workflowDecision", value));

        String patchBody = getPatchContent(detectDuplicate);

        // check security
        getClient(submitterToken).perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON)).andExpect(status().isUnprocessableEntity());

        // make patch
        getClient(reviewerToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision",
                                is("reject")));
        dedupService.commit();
        // check that changes persist
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision",
                                is("reject")));

        String firstWitemUuid = witem.getItem().getID().toString();
        claimedTask = null;
        witem = null;
        detectDuplicate = null;
        value = null;
        patchBody = null;

        // 4b. create another workflow items with the second submitter
        context.turnOffAuthorisationSystem();

        // Test verify patch operation with another workspace item
        pdf = getClass().getResourceAsStream("simple-article.pdf");
        claimedTask = ClaimedTaskBuilder.createClaimedTask(context, colWorkflow, reviewer)
                .withTitle("Sample submission").withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        claimedTask.setStepID("editstep");
        claimedTask.setActionID("editaction");
        witem = claimedTask.getWorkflowItem();
        pdf.close();
        context.restoreAuthSystemState();

        // check security
        getClient().perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isUnauthorized());
        dedupService.commit();
        // check for duplicates
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision")
                                .doesNotExist())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision")
                        .doesNotExist());

        // patch verify, the item with id item.getID()
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + workspaceItemId + "/workflowDecision", value));

        patchBody = getPatchContent(detectDuplicate);

        // check security
        getClient(submitterToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        getClient(reviewerToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision")
                        .doesNotExist());
        dedupService.commit();
        // check that changes persist
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision")
                        .doesNotExist());

        detectDuplicate = null;
        value = null;
        patchBody = null;

        // patch verify, the first workspace item with id firstWitemUuid
        detectDuplicate = new ArrayList<Operation>();
        value = new HashMap<String, String>();
        value.put("value", "verify");
        value.put("note", "test2");
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + firstWitemUuid + "/workflowDecision", value));

        patchBody = getPatchContent(detectDuplicate);
        getClient(reviewerToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID())
                        .content(patchBody).contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId)))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowNote",
                        is("test2")));
        dedupService.commit();
        // check that changes persist
        getClient(reviewerToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].matchObject.id",
                        is(workspaceItemId.toString())))
                .andExpect(
                        jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowDecision",
                                is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + workspaceItemId + "'].workflowNote",
                        is("test")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].matchObject.id",
                        is(firstWitemUuid)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowDecision",
                        is("verify")))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + firstWitemUuid + "'].workflowNote",
                        is("test2")));
    }

    @Test
    /**
     * Test reject deduplication during workspace submission. Both verify and reject
     * operation are tested. The reference object of the test is an item.
     *
     * @throws Exception
     */
    public void workflowItemCheckFailures() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson itemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withNameInMetadata("submitter1", "").withPassword(password).build();
        EPerson reviewer = EPersonBuilder.createEPerson(context).withEmail("reviewer@example.com")
                .withNameInMetadata("reviewer", "").withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(itemSubmitter).build();
        Collection colWorkflow = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withWorkflowGroup(1, reviewer).build();

        // 3. create an item with title "Sample submission" using the first submitter
        context.setCurrentUser(itemSubmitter);
        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workspace items with the reviewer
        context.setCurrentUser(reviewer);
        String authToken = getAuthToken(reviewer.getEmail(), password);

        // 3. a workflow item will all the required fields
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, colWorkflow)
                .withTitle("Sample submission").withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        /*
         * ^ BUG: Since DSpaceObject.metadata is set to FetchType.LAZY here a
         * "org.hibernate.LazyInitializationException: failed to lazily initialize a
         * collection of role: org.dspace.content.DSpaceObject.metadata, could not
         * initialize proxy - no Session" is generated.
         * 
         * Workaround: Set fetch type of DSpaceObject.metadata to EAGER.
         */
        pdf.close();

        // Test reject patch operation with a workspace item
        context.restoreAuthSystemState();
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].workflowDecision")
                        .doesNotExist());

        // generate some Unprocessable exceptions

        // generate a random UUID
        UUID id = UUID.randomUUID();
        int c = 0;
        while (id == item.getID() && c < 10) {
            id = UUID.randomUUID();
            c++;
        }

        // Ask for a patch with an UUID not in the list of duplicates
        List<Operation> detectDuplicate = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(new AddOperation("/sections/detect-duplicate/matches/" + id + "/workflowDecision", value));

        String patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with a number as UUID
        patchBody = null;
        detectDuplicate.clear();

        detectDuplicate
                .add(new AddOperation("/sections/detect-duplicate/matches/" + "1001" + "/workflowDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with an invalid operation
        value.clear();
        patchBody = null;
        detectDuplicate.clear();

        value.put("value", "invalid-op");
        value.put("note", null);
        detectDuplicate.add(
                new AddOperation("/sections/detect-duplicate/matches/" + item.getID() + "/workflowDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with wrong type
        value.clear();
        patchBody = null;
        detectDuplicate.clear();

        value.put("value", "reject");
        value.put("note", null);
        value.put("type", "WORKSPACE");
        detectDuplicate.add(new AddOperation("/sections/detect-duplicate/matches/" + id + "/workflowDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Ask for a patch with the wrong decision type
        value.clear();
        patchBody = null;
        detectDuplicate.clear();

        value.put("value", "reject");
        value.put("note", null);
        detectDuplicate.add(new AddOperation("/sections/detect-duplicate/matches/" + id + "/submitterDecision", value));
        patchBody = getPatchContent(detectDuplicate);
        getClient(authToken)
                .perform(patch("/api/workflow/workflowitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    /**
     * Test reject deduplication during workspace submission. A patch that modifies
     * workspace title is performed and then duplicates are checked.
     *
     * @throws Exception
     */
    public void checkDedupIndexModification() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create two users to use as submitters
        EPerson itemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withPassword(password).build();
        EPerson workspaceItemSubmitter = EPersonBuilder.createEPerson(context).withEmail("submitter2@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(itemSubmitter).build();
        Collection colWorkspace = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withSubmitterGroup(workspaceItemSubmitter).build();

        // 3. create an item with title "Sample submission" using the first submitter
        context.setCurrentUser(itemSubmitter);
        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. create workflow items with the second submitter
        context.setCurrentUser(workspaceItemSubmitter);
        String authToken = getAuthToken(workspaceItemSubmitter.getEmail(), password);

        // Test reject patch operation with a workspace item
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace).withTitle("Test")
                .withIssueDate("2020-02-01").withSubject("Test")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();

        // try to modify the title
        List<Operation> addTitle = new ArrayList<Operation>();
        // create a list of values to use in add operation
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "Sample submission");
        values.add(value);
        addTitle.add(new AddOperation("/sections/traditionalpageone/dc.title", values));

        String patchBody = getPatchContent(addTitle);
        getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + witem.getID()).content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$", Matchers.is(WorkspaceItemMatcher
                        .matchItemWithTitleAndDateIssuedAndSubject(witem, "Sample submission", "2020-02-01", "Test"))));
        dedupService.commit();
        // check for duplicates
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].matchObject.id",
                        is(item.getID().toString())))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + item.getID() + "'].submitterDecision")
                        .doesNotExist());
    }
}
