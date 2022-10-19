/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
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
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
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
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.deduplication.MockSolrDedupCore;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for testing Deduplication operations
 *
 * @author fcadili (francecso.cadili at 4science.it)
 * @author Kim Shepherd
 *
 */
public class SubmissionDeduplicationRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private MockSolrDedupCore dedupService;

    private Collection collection;

    private EPerson submitter;

    private EPerson editor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root community").build();

        submitter = EPersonBuilder.createEPerson(context)
                                  .withEmail("submitter.em@test.com")
                                  .withPassword(password)
                                  .build();

        editor = EPersonBuilder.createEPerson(context)
                               .withEmail("editor@example.com")
                               .withPassword(password).build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .withEntityType("Publication")
                                      .withSubmitterGroup(submitter)
                                      .withWorkflowGroup(2, editor).build();

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workflowItemService.deleteByCollection(context, collection);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();
    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }

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
     * The "restrict to items that the user can READ" check is temporarily disabled for this test
     * as it is explicitly trying to test matches between workspace / workflow items with non-administrators and
     * this optional behaviour needs to be tested.
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
        // temporarily disable auth check
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", false);
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

        // restore auth check
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", true);

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

        // Disable the auth read check temporarily
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", false);

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

        // 4b. create another workflow items with the submitter
        context.turnOffAuthorisationSystem();

        // Test verify patch operation with another workspace item
        pdf = getClass().getResourceAsStream("simple-article.pdf");
        witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace)
                .withTitle("Sample submission")
                .withIssueDate("2021-01-01")
                .withFulltext("article.pdf", "/local/path/simple-article.pdf", pdf)
                .grantLicense()
                .build();
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

    /**
     * Test duplicate detection in scenarios where the current user does not have authorization to read
     * or discover any matching items, and the configuration is set to check authorization first.
     *
     * @throws Exception
     */
    @Test
    public void unreadableItemsTest() throws Exception {

        context.turnOffAuthorisationSystem();

        // Make sure the authorization check is true for this test
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", true);

        // 1. create two users to use as submitters
        EPerson submitterOne = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withPassword(password).build();
        EPerson submitterTwo = EPersonBuilder.createEPerson(context).withEmail("submitter2@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colWorkspaceOne = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(submitterOne).build();
        Collection colWorkspaceTwo = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withSubmitterGroup(submitterTwo).build();

        // 3. Create a workspace item with first submitter
        context.setCurrentUser(submitterOne);
        WorkspaceItem witemOne = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspaceOne)
                .withTitle("Sample submission").withIssueDate("2020-01-31").withAuthor("Cadili, Francesco")
                .grantLicense().withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4. Create workspace item with second submitter - use the same title
        context.setCurrentUser(submitterTwo);
        String authToken = getAuthToken(submitterTwo.getEmail(), password);
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witemTwo = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspaceTwo)
                .withTitle("Sample submission").withIssueDate("2020-02-01").grantLicense()
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf).build();
        pdf.close();
        context.restoreAuthSystemState();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();

        // 5. Check that we do NOT get matches, even though in theory there should be one
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches").doesNotExist());

        // 6. Deliberately give submitterTwo READ access to witemOne and check again - this time our match should
        // be populated with a single match (witemOne)
        authorizeService.addPolicy(context, witemOne.getItem(), Constants.READ, submitterTwo);
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['"
                                + witemOne.getItem().getID() + "'].matchObject.id",
                        is(witemOne.getItem().getID().toString())));

        // 7. To prove the above in a different way, strip submitterTwo's READ policies from the witemOne object
        // and test the same get with the auth check disabled
        context.turnOffAuthorisationSystem();
        authorizeService.removeEPersonPolicies(context, witemOne.getItem(), submitterTwo);
        context.restoreAuthSystemState();
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", false);
        // now we should get one match
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)));
        // set check back to true
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", true);

        // 8. As submitterOne, perform the deposit of itemOne. It will inherit new policies
        //2. Three public items that are readable by Anonymous with different subjects
        context.turnOffAuthorisationSystem();
        Item publicItem1 = ItemBuilder.createItem(context, colWorkspaceOne)
                .withTitle("Sample submission")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();

        // 9. Expect ONE duplicate match now containing just the new publicItem1 (witemOne is private already)
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['"
                                + publicItem1.getID() + "'].matchObject.id",
                        is(publicItem1.getID().toString())));

        // 10. Remove ALL policies from the new archived item (ie. simulate admin only / restricted)
        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, publicItem1);
        context.restoreAuthSystemState();

        // 11. Expect NO duplicate matches (matches list is not returned if it's empty after a search)
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches").doesNotExist());

    }

    /**
     * Test reject deduplication during workspace submission. Both verify and reject
     * operation are tested. The reference object of the test is an item.
     *
     * @throws Exception
     */
    @Test
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
        detectDuplicate = new ArrayList<>();
        value = new HashMap<>();
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

        // temporarily disable authorization check
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", false);

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

        // restore authorization check
        configurationService.setProperty("deduplication.tool.duplicatechecker.authorize.check", true);

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
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample")
                .grantLicense().build();

        // 4a. create workflow items with the second submitter
        context.setCurrentUser(workspaceItemSubmitter);
        String authToken = getAuthToken(workspaceItemSubmitter.getEmail(), password);

        // Test reject patch operation with a workspace item
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspace).withTitle("Test")
                .withIssueDate("2020-02-01").withSubject("Test")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .grantLicense().build();
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

    @Test
    public void testWorkflowDuplicationWithSameTitleTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test publication", collection);
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", collection);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision").doesNotExist());
    }

    @Test
    public void testWorkflowDuplicationWithDifferentTitleTest() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test publication", collection);
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test NOT publication", collection);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches").doesNotHaveJsonPath());
    }

    @Test
    public void workflowDuplicationWithSameDoiButDifferentCommunityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collectionOfComA = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withEntityType("Publication")
                                                       .withSubmitterGroup(submitter)
                                                       .withName("Collection Of Community A").build();

        Collection collectionOfComB = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withEntityType("Publication")
                                                       .withSubmitterGroup(submitter)
                                                       .withWorkflowGroup("reviewer", editor)
                                                       .withName("Collection Of Community B").build();

        // Create a public archived item with a DOI
        Item item = ItemBuilder.createItem(context, collectionOfComA)
                               .withTitle("Test Item")
                               .withDoiIdentifier("10.1000/50182")
                               .build();

        // Submit workflow item as submitter user with the same DOI
        context.setCurrentUser(submitter);
        WorkflowItem workflowItem = WorkflowItemBuilder.createWorkflowItem(context, collectionOfComB)
                .withTitle("Test WorkflowItem").withSubmitter(submitter)
                .withDoiIdentifier("10.1000/50182").build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(editor.getEmail(), password)).perform(get("/api/workflow/workflowitems/"
                        + workflowItem.getID()))
                         .andExpect(status().isOk())
                         .andExpect(jsonPath("$.sections['detect-duplicate']", aMapWithSize(1)))
                         .andExpect(jsonPath("$.sections['detect-duplicate'].matches['"
                             + item.getID().toString() + "'].matchObject.id", is(item.getID().toString())));

    }

    /**
     *
     * Test fuzzy match signature works with a default config of levenshtein edit distance is a maximum of 1
     *
     * @throws Exception
     */
    @Test
    public void fuzzyMatchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // 1. create a user to use as submitter. Only one submitter and collection is needed here,
        // and will ensure READ permissions are consistent
        EPerson submitterOne = EPersonBuilder.createEPerson(context).withEmail("submitter1@example.com")
                .withPassword(password).build();

        // 2. A community-collection structure with one parent community with
        // sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colWorkspaceOne = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(submitterOne).build();

        // 3. Create a workspace item with first submitter
        context.setCurrentUser(submitterOne);
        String authToken = getAuthToken(submitterOne.getEmail(), password);
        WorkspaceItem witemOne = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspaceOne)
                .withTitle("Sample submission").withIssueDate("2020-01-31").withAuthor("Cadili, Francesco")
                .grantLicense().withAuthor("Perelli, Matteo").withSubject("Sample").build();

        // 4a. Create workspace item - use a title with a levenshtein distance of 1
        WorkspaceItem witemTwo = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspaceOne)
                .withTitle("Sample s_bmission").withIssueDate("2020-02-01").grantLicense().build();
        // 4b. Create workspace item - use a title with a levenshtein distance of 2
        WorkspaceItem witemThree = WorkspaceItemBuilder.createWorkspaceItem(context, colWorkspaceOne)
                .withTitle("S_mple s_bmission").withIssueDate("2020-02-01").grantLicense().build();
        context.restoreAuthSystemState();

        // security check
        getClient().perform(get("/api/submission/workspaceitems/" + witemTwo.getID()))
                .andExpect(status().isUnauthorized());
        dedupService.commit();

        // 5. Check that we get a single match, for witemTwo against witemOne. We should not get witemThree.
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witemOne.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
                .andExpect(jsonPath("$.sections['detect-duplicate'].matches['"
                                + witemTwo.getItem().getID() + "'].matchObject.id",
                        is(witemTwo.getItem().getID().toString())));

    }

    private Item createItem(String title, Collection collection) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .build();
    }

    private WorkflowItem createWorkflowItem(String title, Collection collection) {
        return WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle(title)
            .withSubmitter(submitter)
            .build();
    }

}