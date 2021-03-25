/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * This class will aim to test Discovery related use cases
 */
public class DiscoveryIT extends AbstractIntegrationTestWithDatabase {

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected SearchService searchService = SearchUtils.getSearchService();

    XmlWorkflowService workflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();

    WorkflowRequirementsService workflowRequirementsService = XmlWorkflowServiceFactory.getInstance().
            getWorkflowRequirementsService();

    ClaimedTaskService claimedTaskService = XmlWorkflowServiceFactory.getInstance().getClaimedTaskService();

    ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                                   .getServiceByName(IndexingService.class.getName(),
                                                                     IndexingService.class);

    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory.getInstance()
                                                                                      .getMetadataAuthorityService();

    @Test
    public void solrRecordsAfterDepositOrDeletionOfWorkspaceItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
        Collection col = CollectionBuilder.createCollection(context, community)
                                          .withName("Collection without workflow")
                                          .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community)
                .withName("Collection WITH workflow")
                .withWorkflowGroup(1, admin)
                .build();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                                                          .withTitle("No workflow")
                                                          .withAbstract("headache")
                                                          .build();
        WorkspaceItem anotherWorkspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                .withTitle("Another WS Item in No workflow collection")
                .withAbstract("headache")
                .build();
        WorkspaceItem workspaceItemInWfCollection = WorkspaceItemBuilder.createWorkspaceItem(context, colWithWorkflow)
                .withTitle("WS Item in workflow collection")
                .withAbstract("headache")
                .build();
        context.restoreAuthSystemState();

        // we start with 3 ws items
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 3);
        // simulate the deposit
        deposit(workspaceItem);
        // now we should have 1 archived item and 2 ws items, no wf items or tasks
        assertSearchQuery(IndexableWorkflowItem.TYPE, 0);
        assertSearchQuery(IndexablePoolTask.TYPE, 0);
        assertSearchQuery(IndexableClaimedTask.TYPE, 0);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 2);
        assertSearchQuery(IndexableItem.TYPE, 1);

        // simulate the deposit of the ws item in the workflow collection
        deposit(workspaceItemInWfCollection);
        // now we should have 1 wf, 1 pool task, 1 ws item and 1 item
        assertSearchQuery(IndexableWorkflowItem.TYPE, 1);
        assertSearchQuery(IndexablePoolTask.TYPE, 1);
        assertSearchQuery(IndexableClaimedTask.TYPE, 0);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 1);
        assertSearchQuery(IndexableItem.TYPE, 1);

        // simulate the delete of last workspace item
        deleteSubmission(anotherWorkspaceItem);

        assertSearchQuery(IndexableWorkflowItem.TYPE, 1);
        assertSearchQuery(IndexablePoolTask.TYPE, 1);
        assertSearchQuery(IndexableClaimedTask.TYPE, 0);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 0);
        assertSearchQuery(IndexableItem.TYPE, 1);
    }

    @Test
    public void solrRecordsAfterDealingWithWorkflowTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withWorkflowGroup(1, admin)
                                                 .build();
        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        ClaimedTask taskToApprove = ClaimedTaskBuilder.createClaimedTask(context, collection, admin)
                                                .withTitle("Test workflow item to approve")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();
        ClaimedTask taskToReject = ClaimedTaskBuilder.createClaimedTask(context, collection, admin)
                .withTitle("Test workflow item to reject")
                .withIssueDate("2019-03-06")
                .withSubject("ExtraEntry")
                .build();
        PoolTask taskToClaim = PoolTaskBuilder.createPoolTask(context, collection, admin)
                .withTitle("Test pool task to claim")
                .withIssueDate("2019-03-06")
                .withSubject("ExtraEntry")
                .build();
        ClaimedTask taskToUnclaim = ClaimedTaskBuilder.createClaimedTask(context, collection, admin)
                .withTitle("Test claimed task to unclaim")
                .withIssueDate("2019-03-06")
                .withSubject("ExtraEntry")
                .build();
        XmlWorkflowItem wfiToDelete = WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withTitle("Test workflow item to return")
                .withIssueDate("2019-03-06")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();
        // we start with 5 workflow items, 3 claimed tasks, 2 pool task
        assertSearchQuery(IndexableWorkflowItem.TYPE, 5);
        assertSearchQuery(IndexableClaimedTask.TYPE, 3);
        assertSearchQuery(IndexablePoolTask.TYPE, 2);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 0);
        assertSearchQuery(IndexableItem.TYPE, 0);

        // claim
        claim(workflow, taskToClaim, admin);
        assertSearchQuery(IndexableWorkflowItem.TYPE, 5);
        assertSearchQuery(IndexableClaimedTask.TYPE, 4);
        assertSearchQuery(IndexablePoolTask.TYPE, 1);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 0);
        assertSearchQuery(IndexableItem.TYPE, 0);

        // unclaim
        returnClaimedTask(taskToUnclaim);
        assertSearchQuery(IndexableWorkflowItem.TYPE, 5);
        assertSearchQuery(IndexableClaimedTask.TYPE, 3);
        assertSearchQuery(IndexablePoolTask.TYPE, 2);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 0);
        assertSearchQuery(IndexableItem.TYPE, 0);

        // reject
        MockHttpServletRequest httpRejectRequest = new MockHttpServletRequest();
        httpRejectRequest.setParameter("submit_reject", "submit_reject");
        httpRejectRequest.setParameter("reason", "test");
        executeWorkflowAction(httpRejectRequest, workflow, taskToReject);
        assertSearchQuery(IndexableWorkflowItem.TYPE, 4);
        assertSearchQuery(IndexableClaimedTask.TYPE, 2);
        assertSearchQuery(IndexablePoolTask.TYPE, 2);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 1);
        assertSearchQuery(IndexableItem.TYPE, 0);

        // approve
        MockHttpServletRequest httpApproveRequest = new MockHttpServletRequest();
        httpApproveRequest.setParameter("submit_approve", "submit_approve");
        executeWorkflowAction(httpApproveRequest, workflow, taskToApprove);
        assertSearchQuery(IndexableWorkflowItem.TYPE, 3);
        assertSearchQuery(IndexableClaimedTask.TYPE, 1);
        assertSearchQuery(IndexablePoolTask.TYPE, 2);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 1);
        assertSearchQuery(IndexableItem.TYPE, 1);

        // abort pool task
        // as we have already unclaimed this task it is a pool task now
        abort(taskToUnclaim.getWorkflowItem());
        assertSearchQuery(IndexableWorkflowItem.TYPE, 2);
        assertSearchQuery(IndexableClaimedTask.TYPE, 1);
        assertSearchQuery(IndexablePoolTask.TYPE, 1);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 2);
        assertSearchQuery(IndexableItem.TYPE, 1);

        // abort claimed task
        // as we have already claimed this task it is a claimed task now
        abort(taskToClaim.getWorkflowItem());
        assertSearchQuery(IndexableWorkflowItem.TYPE, 1);
        assertSearchQuery(IndexableClaimedTask.TYPE, 0);
        assertSearchQuery(IndexablePoolTask.TYPE, 1);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 3);
        assertSearchQuery(IndexableItem.TYPE, 1);

        // delete pool task / workflow item
        deleteWorkflowItem(wfiToDelete);
        assertSearchQuery(IndexableWorkflowItem.TYPE, 0);
        assertSearchQuery(IndexableClaimedTask.TYPE, 0);
        assertSearchQuery(IndexablePoolTask.TYPE, 0);
        assertSearchQuery(IndexableWorkspaceItem.TYPE, 3);
        assertSearchQuery(IndexableItem.TYPE, 1);
    }

    @Test
    public void solrRecordAfterDeleteTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
        Collection col = CollectionBuilder.createCollection(context, community)
                                          .withName("Collection")
                                          .build();

        Item item1 = ItemBuilder.createItem(context, col)
                               .withTitle("Publication 1")
                               .build();

        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Publication 2")
                                .build();

        context.restoreAuthSystemState();

        // we start with 2 items
        assertSearchQuery(IndexableItem.TYPE, 2);
        // simulate the delete of item2
        deleteItem(item2);
        // now we should have 1 item
        assertSearchQuery(IndexableItem.TYPE, 1);
        // simulate the delete of item1
        deleteItem(item1);
        // now we should have 0 item
        assertSearchQuery(IndexableItem.TYPE, 0);

    }

    @Test
    public void solrRecordFromMessyItemTest() throws Exception {
        configurationService.setProperty("authority.controlled.dc.subject", "true");
        metadataAuthorityService.clearCache();
        try {
            context.turnOffAuthorisationSystem();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 1").build();

            context.restoreAuthSystemState();

            assertSearchQuery(IndexableItem.TYPE, 0);

            context.turnOffAuthorisationSystem();

           ItemBuilder.createItem(context, col1)
                      .withTitle("Public item 1")
                      .withIssueDate("2021-01-21")
                      .withAuthor("Smith, Donald")
                      .withSubject("Test Value", "NOT-EXISTING", Choices.CF_ACCEPTED)
                      .build();

            context.restoreAuthSystemState();
            assertSearchQuery(IndexableItem.TYPE, 1);
        } finally {
            configurationService.setProperty("authority.controlled.dc.subject", "false");
            metadataAuthorityService.clearCache();
        }

    }

    private void assertSearchQuery(String resourceType, int size) throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.addFilterQueries("search.resourcetype:" + resourceType);
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(size, indexableObjects.size());
        assertEquals(size, discoverResult.getTotalSearchResults());
    }


    private void deposit(WorkspaceItem workspaceItem)
            throws SQLException, AuthorizeException, IOException, WorkflowException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        workspaceItem = context.reloadEntity(workspaceItem);
        XmlWorkflowItem workflowItem = workflowService.startWithoutNotify(context, workspaceItem);
        context.commit();
        indexer.commit();
        context.restoreAuthSystemState();
    }

    private void deleteItem(Item item) throws SQLException, AuthorizeException, IOException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        item = context.reloadEntity(item);
        itemService.delete(context, item);
        context.commit();
        indexer.commit();
        context.restoreAuthSystemState();
    }

    private void deleteSubmission(WorkspaceItem anotherWorkspaceItem)
            throws SQLException, AuthorizeException, IOException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        anotherWorkspaceItem = context.reloadEntity(anotherWorkspaceItem);
        workspaceItemService.deleteAll(context, anotherWorkspaceItem);
        context.commit();
        indexer.commit();
        context.restoreAuthSystemState();
    }

    private void deleteWorkflowItem(XmlWorkflowItem workflowItem)
            throws SQLException, AuthorizeException, IOException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        workflowItem = context.reloadEntity(workflowItem);
        workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, admin);
        context.commit();
        indexer.commit();
        context.restoreAuthSystemState();
    }

    private void returnClaimedTask(ClaimedTask taskToUnclaim) throws SQLException, IOException,
            WorkflowConfigurationException, AuthorizeException, SearchServiceException {
        final EPerson previousUser = context.getCurrentUser();
        taskToUnclaim = context.reloadEntity(taskToUnclaim);
        context.setCurrentUser(taskToUnclaim.getOwner());
        XmlWorkflowItem workflowItem = taskToUnclaim.getWorkflowItem();
        workflowService.deleteClaimedTask(context, workflowItem, taskToUnclaim);
        workflowRequirementsService.removeClaimedUser(context, workflowItem, taskToUnclaim.getOwner(),
                taskToUnclaim.getStepID());
        context.commit();
        indexer.commit();
        context.setCurrentUser(previousUser);
    }

    private void claim(Workflow workflow, PoolTask task, EPerson user)
            throws Exception {
        final EPerson previousUser = context.getCurrentUser();
        task = context.reloadEntity(task);
        context.setCurrentUser(user);
        Step step = workflow.getStep(task.getStepID());
        WorkflowActionConfig currentActionConfig = step.getActionConfig(task.getActionID());
        workflowService.doState(context, user, null, task.getWorkflowItem().getID(), workflow, currentActionConfig);
        context.commit();
        indexer.commit();
        context.setCurrentUser(previousUser);
    }

    private void executeWorkflowAction(HttpServletRequest httpServletRequest, Workflow workflow, ClaimedTask task)
            throws Exception {
        final EPerson previousUser = context.getCurrentUser();
        task = context.reloadEntity(task);
        context.setCurrentUser(task.getOwner());
        workflowService.doState(context, task.getOwner(), httpServletRequest, task.getWorkflowItem().getID(), workflow,
                                workflow.getStep(task.getStepID()).getActionConfig(task.getActionID()));
        context.commit();
        indexer.commit();
        context.setCurrentUser(previousUser);
    }

    private void abort(XmlWorkflowItem workflowItem)
            throws SQLException, AuthorizeException, IOException, SearchServiceException {
        final EPerson previousUser = context.getCurrentUser();
        workflowItem = context.reloadEntity(workflowItem);
        context.setCurrentUser(admin);
        workflowService.abort(context, workflowItem, admin);
        context.commit();
        indexer.commit();
        context.setCurrentUser(previousUser);
    }
}
