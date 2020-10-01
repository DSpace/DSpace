/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Class to test interaction between EPerson deletion and tasks present in the workflow
 */
public class EPersonInWorkflowTest extends AbstractIntegrationTestWithDatabase {

    private final String REVIEW_STEP = "reviewstep";
    private final String CLAIM_ACTION = "claimaction";
    private final String REVIEW_ACTION = "reviewaction";
    private final String REVIEW_ROLE = "reviewer";
    private final String EDIT_STEP = "editstep";
    private final String EDIT_ACTION = "editaction";
    private final String FINAL_EDIT_ROLE = "finaleditor";
    private final String FINAL_EDIT_STEP = "finaleditstep";
    private final String FINAL_EDIT_ACTION = "finaleditaction";
    private final String EDIT_ROLE = "editor";
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkflowItemService workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance()
                                                                                       .getXmlWorkflowItemService();
    protected WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
                                                                               .getWorkspaceItemService();
    protected ClaimedTaskService claimedTaskService = XmlWorkflowServiceFactory.getInstance().getClaimedTaskService();
    protected PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();

    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
    protected CollectionRoleService collectionRoleService = XmlWorkflowServiceFactory.getInstance()
                                                                                     .getCollectionRoleService();


    private EPerson workflowUserA;
    private EPerson workflowUserB;
    private EPerson workflowUserC;
    private EPerson workflowUserD;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EPersonInWorkflowTest.class);

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses but no
     * execution order is guaranteed
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        workflowUserA = EPersonBuilder.createEPerson(context).withEmail("workflowUserA@example.org").build();
        workflowUserB = EPersonBuilder.createEPerson(context).withEmail("workflowUserB@example.org").build();
        workflowUserC = EPersonBuilder.createEPerson(context).withEmail("workflowUserC@example.org").build();
        workflowUserD = EPersonBuilder.createEPerson(context).withEmail("workflowUserD@example.org").build();

        context.restoreAuthSystemState();

    }


    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - claim it by user B
     * - delete user B
     * - verify the delete is refused
     * - remove user B from step 1
     * - verify that the removal is refused due to B being the last member in the workflow group and the group having
     * a claimed item
     * - approve it by user B and let it move to step 2
     * - remove user B from step 3
     * - approve it by user C
     * - verify that the item is archived without any actions apart from removing user B
     * - delete user B
     * - verify the delete succeeds
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup1() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, false);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);


        assertDeletionOfEperson(workflowUserB, false);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, FINAL_EDIT_ROLE, true);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, true);

        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user B
     * - verify the delete is refused
     * - remove user B from step 1
     * - verify that the removal is refused due to B being the last member in the workflow group and the group having a
     * pool task
     * - approve it by user B and let it move to step 2
     * - remove user B from step 3
     * - delete user B
     * - verify the delete succeeds
     * - Approve it by user C
     * - verify that the item is archived without any actions apart from the approving in step 2
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup2() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserB, false);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, false);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, true);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, "finaleditor", true);

        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }


    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user C
     * - verify the delete is refused
     * - remove user C from step 2
     * - delete user C
     * - verify the delete succeeds
     * - Approve it by user B
     * - verify that the item moved to step 3 without any actions apart from the approving in step 1
     * - Approve it by user B
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup3() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserC, false);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserC, collection, EDIT_ROLE, true);

        assertDeletionOfEperson(workflowUserC, true);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - approve it by user B, and let it move to step 2
     * - approve it by user C, and let it move to step 3
     * - claim it by user B
     * - remove user B from step 1
     * - delete user B
     * - verify the delete is refused
     * - remove user B from step 3, verify that the removal is refused due to user B having a claimed task and there
     * being no other members in step 3
     * - approve it by user B
     * - delete user B
     * - verify the delete suceeds
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup4() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);

        assertDeletionOfEperson(workflowUserB, false);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, true);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, FINAL_EDIT_ROLE, false);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, FINAL_EDIT_ROLE, true);
        assertDeletionOfEperson(workflowUserB, true);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Collection A - Step 1: user B
     * - Collection A - Step 2: user C
     * - Collection A - Step 3: user B
     *
     * - Collection B - Step 1: user B
     *
     * This test will perform the following checks:
     * - create a workspace item in Collection A, and let it move to step 1
     * - claim it by user B
     * - delete user B
     * - verify the delete is refused
     * - remove user B from Col A - step 3
     * - remove user B from Col B - step 1
     * - remove user B from Col A - step 1
     * - Verify that the removal from Col A - step 1 is refused because user B has a claimed task in that collection and
     * no other user is present
     * - approve it by user B, and let it move to step 2
     * - remove user B from Col A - step 1
     * - verify it succeeds
     * - delete user B
     * - verify it succeeds
     * - approve it by user C
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup5() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collectionA = CollectionBuilder.createCollection(context, parent)
                                                  .withWorkflowGroup(1, workflowUserB)
                                                  .withWorkflowGroup(2, workflowUserC)
                                                  .withWorkflowGroup(3, workflowUserB)
                                                  .build();

        Collection collectionB = CollectionBuilder.createCollection(context, parent)
                                                  .withWorkflowGroup(1, workflowUserB)
                                                  .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collectionA)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collectionA);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collectionA, FINAL_EDIT_ROLE, true);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collectionB, REVIEW_ROLE, true);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collectionA, REVIEW_ROLE, false);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collectionA, REVIEW_ROLE, true);
        assertDeletionOfEperson(workflowUserB, true);


        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * - create a workspace item, and let it move to step 1
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - claim it by user C, but don’t approve it
     * - delete user C
     * - verify the delete is refused
     * - remove user C from step 2
     * - delete user C
     * - verify the delete succeeds
     * - verify that the item moved to step 3 without any actions apart from deleting user C
     * - Approve it by user B
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup6() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserA, true);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after user has been removed from the workflow
     * group and the task has been passed, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user B
     * - verify the delete is refused
     * - remove user B from step 1
     * - verify the removal is refused
     * - remove user B from step 3
     * - verify the removal succeeds
     * - approve it by user B
     * - verify that the item moved to step 2
     * - remove user B from step 1
     * - delete user B
     * - verify the delete succeeds
     * - approve it by user C
     * - verify that the item is archived without any actions apart from removing user B
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenOnlyUserInGroup7() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserB, false);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, false);
        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, FINAL_EDIT_ROLE, true);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        assertRemovalOfEpersonFromWorkflowGroup(workflowUserB, collection, REVIEW_ROLE, true);
        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }


    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after another user has been added to the workflow
     * group, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - approve it by user B
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - delete user B
     * - verify the delete is refused
     * - add user D to workflow step 3
     * - delete user B
     * - verify the delete is refused
     * - add user D to workflow step 1
     * - delete user B
     * - verify the delete succeeds
     * - Approve it by user D
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserAfterReplacingUser1() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertDeletionOfEperson(workflowUserB, false);

        addUserToWorkflowGroup(workflowUserD, collection, FINAL_EDIT_ROLE);
        assertDeletionOfEperson(workflowUserB, false);
        addUserToWorkflowGroup(workflowUserD, collection, REVIEW_ROLE);

        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after another user has been added to the workflow
     * group, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user B
     * - verify the delete is refused
     * - add user D to workflow step 1
     * - add user D to workflow step 3
     * - delete user B
     * - verify the delete succeeds
     * - Approve it by user D
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - Approve it by user D
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserAfterReplacingUser2() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserB, false);
        addUserToWorkflowGroup(workflowUserD, collection, REVIEW_ROLE);
        addUserToWorkflowGroup(workflowUserD, collection, FINAL_EDIT_ROLE);
        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after another user has been added to the workflow
     * group, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user C
     * - verify the delete is refused
     * - add user D to workflow step 2
     * - delete user C
     * - verify the delete succeeds
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - Approve it by user D
     * - verify that the item moved to step 3
     * - Approve it by user B
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserAfterReplacingUser3() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserC, false);
        addUserToWorkflowGroup(workflowUserD, collection, EDIT_ROLE);
        assertDeletionOfEperson(workflowUserC, true);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after another user has been added to the workflow
     * group, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - claim it by user B, but don’t approve it
     * - delete user B
     * - verify the delete is refused
     * - add user D to workflow step 1
     * - add user D to workflow step 3
     * - delete user B
     * - verify the delete succeeds
     * - Verify user D can now claim and approve it
     * - verify that the item moved to step 2
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserAfterReplacingUser4() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);

        assertDeletionOfEperson(workflowUserB, false);
        addUserToWorkflowGroup(workflowUserD, collection, REVIEW_ROLE);
        addUserToWorkflowGroup(workflowUserD, collection, FINAL_EDIT_ROLE);
        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after another user has been added to the workflow
     * group, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - claim it by user C, but don’t approve it
     * - delete user C
     * - verify the delete is refused
     * - add user D to workflow step 2
     * - delete user C
     * - verify the delete succeeds
     * - Verify user D can now claim and approve it
     * - verify that the item moved to step 3
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserAfterReplacingUser5() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);

        assertDeletionOfEperson(workflowUserC, false);
        addUserToWorkflowGroup(workflowUserD, collection, EDIT_ROLE);
        assertDeletionOfEperson(workflowUserC, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson cannot be removed if they are the only member of a Workflow Group that has
     * tasks currently assigned to it. This test also verifies that after another user has been added to the workflow
     * group, the EPerson can be removed.
     *
     * This test has the following setup:
     * - Step 1: user B
     * - Step 2: user C
     * - Step 3: user B
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - verify that the item moved to step 3
     * - claim it by user B, but don’t approve it
     * - delete user B
     * - verify the delete is refused
     * - add user D to workflow step 1
     * - add user D to workflow step 3
     * - delete user B
     * - verify the delete succeeds
     * - Verify user D can now claim and approve it
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserAfterReplacingUser6() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB)
                                                 .withWorkflowGroup(2, workflowUserC)
                                                 .withWorkflowGroup(3, workflowUserB)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertDeletionOfEperson(workflowUserB, false);
        addUserToWorkflowGroup(workflowUserD, collection, REVIEW_ROLE);
        addUserToWorkflowGroup(workflowUserD, collection, FINAL_EDIT_ROLE);
        assertDeletionOfEperson(workflowUserB, true);


        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson can be removed if there is another user is present in the Workflow Group.
     *
     * This test has the following setup:
     * - Step 1: user B and D
     * - Step 2: user C and D
     * - Step 3: user B and D
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - approve it by user B
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - delete user B
     * - verify the delete succeeds
     * - Approve it by user D
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenMultipleUser1() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB, workflowUserD)
                                                 .withWorkflowGroup(2, workflowUserC, workflowUserD)
                                                 .withWorkflowGroup(3, workflowUserB, workflowUserD)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);

        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson can be removed if there is another user is present in the Workflow Group.
     * 
     * This test has the following setup:
     * - Step 1: user B and D
     * - Step 2: user C and D
     * - Step 3: user B and D
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user B
     * - verify the delete succeeds
     * - Approve it by user D
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - verify that the item moved to step 3
     * - Approve it by user D
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenMultipleUser2() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB, workflowUserD)
                                                 .withWorkflowGroup(2, workflowUserC, workflowUserD)
                                                 .withWorkflowGroup(3, workflowUserB, workflowUserD)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson can be removed if there is another user is present in the Workflow Group.
     * 
     * This test has the following setup:
     * - Step 1: user B and D
     * - Step 2: user C and D
     * - Step 3: user B and D
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - delete user C
     * - verify the delete succeeds
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - Approve it by user D
     * - verify that the item moved to step 3
     * - Approve it by user B
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenMultipleUser3() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB, workflowUserD)
                                                 .withWorkflowGroup(2, workflowUserC, workflowUserD)
                                                 .withWorkflowGroup(3, workflowUserB, workflowUserD)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        assertDeletionOfEperson(workflowUserC, true);

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson can be removed if there is another user is present in the Workflow Group.
     * 
     * This test has the following setup:
     * - Step 1: user B and D
     * - Step 2: user C and D
     * - Step 3: user B and D
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - claim it by user B, but don’t approve it
     * - delete user B
     * - verify the delete succeeds
     * - Verify user D can now claim and approve it
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - verify that the item moved to step 3
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenMultipleUser4() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB, workflowUserD)
                                                 .withWorkflowGroup(2, workflowUserC, workflowUserD)
                                                 .withWorkflowGroup(3, workflowUserB, workflowUserD)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);

        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson can be removed if there is another user is present in the Workflow Group.
     * 
     * This test has the following setup:
     * - Step 1: user B and D
     * - Step 2: user C and D
     * - Step 3: user B and D
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - claim it by user C, but don’t approve it
     * - delete user C
     * - verify the delete succeeds
     * - Verify user D can now claim and approve it
     * - verify that the item moved to step 3
     * - Approve it by user B
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenMultipleUser5() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB, workflowUserD)
                                                 .withWorkflowGroup(2, workflowUserC, workflowUserD)
                                                 .withWorkflowGroup(3, workflowUserB, workflowUserD)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);

        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        assertDeletionOfEperson(workflowUserC, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }

    /**
     * This test verifies that an EPerson can be removed if there is another user is present in the Workflow Group.
     *
     * This test has the following setup:
     * - Step 1: user B and D
     * - Step 2: user C and D
     * - Step 3: user B and D
     *
     * This test will perform the following checks:
     * - create a workspace item, and let it move to step 1
     * - Approve it by user B
     * - verify that the item moved to step 2
     * - Approve it by user C
     * - verify that the item moved to step 3
     * - claim it by user B, but don’t approve it
     * - delete user B
     * - verify the delete succeeds
     * - Verify user D can now claim and approve it
     * - verify that the item is archived
     *
     * @throws Exception
     */
    @Test
    public void testDeleteUserWhenMultipleUser6() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                                                 .withWorkflowGroup(1, workflowUserB, workflowUserD)
                                                 .withWorkflowGroup(2, workflowUserC, workflowUserD)
                                                 .withWorkflowGroup(3, workflowUserB, workflowUserD)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withSubmitter(workflowUserA)
                                                .withTitle("Test item full workflow")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        XmlWorkflowItem workflowItem = xmlWorkflowService.startWithoutNotify(context, wsi);
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, REVIEW_STEP, REVIEW_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserC, workflow, workflowItem, EDIT_STEP, EDIT_ACTION);


        executeWorkflowAction(httpServletRequest, workflowUserB, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        assertDeletionOfEperson(workflowUserB, true);

        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP, CLAIM_ACTION);
        executeWorkflowAction(httpServletRequest, workflowUserD, workflow, workflowItem, FINAL_EDIT_STEP,
                              FINAL_EDIT_ACTION);

        assertTrue(workflowItem.getItem().isArchived());

    }


    private void addUserToWorkflowGroup(EPerson ePerson, Collection collection, String roleName) throws SQLException {
        List<CollectionRole> roles = collectionRoleService.findByCollection(context, collection);
        for (CollectionRole role : roles) {
            if (StringUtils.equals(role.getRoleId(), roleName)) {
                Group group = role.getGroup();
                groupService.addMember(context, group, ePerson);
            }
        }

    }

    private void executeWorkflowAction(HttpServletRequest httpServletRequest, EPerson user,
                                       Workflow workflow, XmlWorkflowItem workflowItem, String stepId, String actionId)
            throws Exception {
        context.setCurrentUser(user);
        xmlWorkflowService.doState(context, user, httpServletRequest, workflowItem.getID(), workflow,
                                   workflow.getStep(stepId).getActionConfig(actionId));
        context.setCurrentUser(null);
    }

    private void assertRemovalOfEpersonFromWorkflowGroup(EPerson ePerson, Collection collection, String roleName,
                                                         boolean shouldSucceed) {
        boolean deleteSuccess = false;
        boolean deleteError = false;

        try {
            List<CollectionRole> roles = collectionRoleService.findByCollection(context, collection);
            for (CollectionRole role : roles) {
                if (StringUtils.equals(role.getRoleId(), roleName)) {
                    Group group = role.getGroup();
                    groupService.removeMember(context, group, ePerson);
                    deleteSuccess = true;
                }
            }
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException) {
                deleteSuccess = false;
                deleteError = true;
            } else {
                deleteSuccess = false;
                log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
                fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                             ": " + ex.getMessage());
            }
        }
        if (shouldSucceed) {
            assertTrue(deleteSuccess);
            assertFalse(deleteError);
        } else {
            assertTrue(deleteError);
            assertFalse(deleteSuccess);
        }
    }

    private void assertDeletionOfEperson(EPerson ePerson, boolean shouldSucceed) throws SQLException {
        boolean deleteSuccess;
        boolean deleteError = false;
        try {
            ePersonService.delete(context, ePerson);
            deleteSuccess = true;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException) {
                deleteSuccess = false;
                deleteError = true;
            } else {
                deleteSuccess = false;
                log.error("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() + ": ", ex);
                fail("Caught an Exception while deleting an EPerson. " + ex.getClass().getName() +
                             ": " + ex.getMessage());
            }
        }

        EPerson ePersonCheck = ePersonService.find(context, ePerson.getID());
        if (shouldSucceed) {
            assertTrue(deleteSuccess);
            assertFalse(deleteError);
            assertNull(ePersonCheck);
        } else {
            assertTrue(deleteError);
            assertFalse(deleteSuccess);
            assertNotNull(ePerson);
        }
    }
}
