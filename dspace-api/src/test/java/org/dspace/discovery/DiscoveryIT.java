/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
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
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableCollection;
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

    CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                                   .getServiceByName(IndexingService.class.getName(),
                                                                     IndexingService.class);

    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory.getInstance()
                                                                                      .getMetadataAuthorityService();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configurationService.setProperty("solr-database-resync.time-until-reindex", 1);
    }

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

    @Test
    public void verifySolrRecordsOfDeletedObjectsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2").build();
        CollectionBuilder.createCollection(context, child2)
                         .withName("Collection 3").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Public item 1")
                   .withIssueDate("2017-10-17")
                   .withAuthor("Smith, Donald")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 2")
                   .withIssueDate("2016-02-13")
                   .withAuthor("Smith, Maria")
                   .withSubject("TestingForMore")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 3")
                   .withIssueDate("2016-02-13")
                   .withAuthor("Doe, Jane")
                   .withSubject("AnotherTest")
                   .withSubject("ExtraEntry")
                   .build();

        context.setDispatcher("noindex");

        assertSearchQuery(IndexableCollection.TYPE, 3);
        assertSearchQuery(IndexableItem.TYPE, 3);
        collectionService.delete(context, col1);
        context.restoreAuthSystemState();
        assertSearchQuery(IndexableCollection.TYPE, 2);
        // Deleted item contained within totalFound due to predb status (SolrDatabaseResyncCli takes care of this)
        assertSearchQuery(IndexableItem.TYPE, 2, 3, 0, -1);
    }

    @Test
    public void verifySolrRecordsOfDeletedObjectsPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection 3").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Public item 1")
                   .withIssueDate("2010-10-17")
                   .withAuthor("Smith, Donald")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 2")
                   .withIssueDate("2011-08-13")
                   .withAuthor("Smith, Maria")
                   .withSubject("TestingForMore")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 3")
                   .withIssueDate("2012-02-19")
                   .withAuthor("Doe, Jane")
                   .withSubject("AnotherTest")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 4")
                   .withIssueDate("2013-05-16")
                   .withAuthor("Vova, Jane")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 5")
                    .withIssueDate("2015-04-13")
                    .withAuthor("Marco, Bruni")
                    .withSubject("ExtraEntry")
                    .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 6")
                   .withIssueDate("2016-01-21")
                   .withAuthor("Andriy, Beket")
                   .withSubject("ExtraEntry")
                   .build();

        context.setDispatcher("noindex");

        // check Collection type with start=0 and limit=default, we expect: indexableObjects=3, totalFound=3
        assertSearchQuery(IndexableCollection.TYPE, 3, 3, 0, -1);
        // check Item type with page=0 and limit=default, we expect: indexableObjects=6, totalFound=6
        assertSearchQuery(IndexableItem.TYPE, 6, 6, 0, -1);
        // delete col3 and all items that it contained
        collectionService.delete(context, col3);
        context.restoreAuthSystemState();

        // check Collection type with start=0 and limit=default, we expect: indexableObjects=2, totalFound=2
        assertSearchQuery(IndexableCollection.TYPE, 2, 2, 0, -1);
        // check Item type with start=0 and limit=2, we expect: indexableObjects=2, totalFound=6
        assertSearchQuery(IndexableItem.TYPE, 2, 6, 0, 2);

        // Run SolrDatabaseResyncCli, updating items with "preDB" status and removing stale items
        performSolrDatabaseResyncScript();

        // check Item type with start=2 and limit=4, we expect: indexableObjects=1, totalFound=3
        assertSearchQuery(IndexableItem.TYPE, 1, 3, 2, 4);
        // check Item type with start=0 and limit=default, we expect: indexableObjects=3, totalFound=3
        // totalFound now is 3 because stale objects deleted
        assertSearchQuery(IndexableItem.TYPE, 3, 3, 0, -1);
    }

    @Test
    public void disabledSolrToRemoveStaleObjectsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // disable removal of solr documents related to items not on database anymore (Stale)
        configurationService.setProperty("discovery.removestale.attempts", -1);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection 3").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Public item 1")
                   .withIssueDate("2010-10-17")
                   .withAuthor("Smith, Donald")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 2")
                   .withIssueDate("2011-08-13")
                   .withAuthor("Smith, Maria")
                   .withSubject("TestingForMore")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 3")
                   .withIssueDate("2012-02-19")
                   .withAuthor("Doe, Jane")
                   .withSubject("AnotherTest")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 4")
                   .withIssueDate("2013-05-16")
                   .withAuthor("Vova, Jane")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 5")
                    .withIssueDate("2015-04-13")
                    .withAuthor("Marco, Bruni")
                    .withSubject("ExtraEntry")
                    .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 6")
                   .withIssueDate("2016-01-21")
                   .withAuthor("Andriy, Beket")
                   .withSubject("ExtraEntry")
                   .build();

        context.setDispatcher("noindex");

        // check Collection type with start=0 and limit=default, we expect: indexableObjects=3, totalFound=3
        assertSearchQuery(IndexableCollection.TYPE, 3, 3, 0, -1);
        // check Item type with page=0 and limit=default, we expect: indexableObjects=6, totalFound=6
        assertSearchQuery(IndexableItem.TYPE, 6, 6, 0, -1);
        // delete col3 and all items that it contained
        collectionService.delete(context, col3);
        context.restoreAuthSystemState();

        // check Collection type with start=0 and limit=default,
        // we expect: indexableObjects=2, totalFound=should be 2 but we have 3 ->(1 stale object here)
        assertSearchQuery(IndexableCollection.TYPE, 2, 3, 0, -1);
        // check Item type with start=0 and limit=2, we expect: indexableObjects=2, totalFound=6
        assertSearchQuery(IndexableItem.TYPE, 2, 6, 0, 2);
        // check Item type with start=2 and limit=4,
        // we expect: indexableObjects=1, totalFound=should be 3 but we have 6 ->(3 stale objects here)
        assertSearchQuery(IndexableItem.TYPE, 1, 6, 2, 4);
        // check Item type with start=0 and limit=default,
        // we expect: indexableObjects=3, totalFound=should be 3 but we have 6 ->(3 stale objects here)
        assertSearchQuery(IndexableItem.TYPE, 3, 6, 0, -1);
    }

    @Test
    public void disabledRerunOfSolrQueryDueToStaleObjectsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // disable re-run of the solr query when stale documents are found
        configurationService.setProperty("discovery.removestale.attempts", 0);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community 2")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child2)
                                           .withName("Collection 3").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Public item 1")
                   .withIssueDate("2010-10-17")
                   .withAuthor("Smith, Donald")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 2")
                   .withIssueDate("2011-08-13")
                   .withAuthor("Smith, Maria")
                   .withSubject("TestingForMore")
                   .build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Public item 3")
                   .withIssueDate("2012-02-19")
                   .withAuthor("Doe, Jane")
                   .withSubject("AnotherTest")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 4")
                   .withIssueDate("2013-05-16")
                   .withAuthor("Vova, Jane")
                   .withSubject("ExtraEntry")
                   .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 5")
                    .withIssueDate("2015-04-13")
                    .withAuthor("Marco, Bruni")
                    .withSubject("ExtraEntry")
                    .build();

        ItemBuilder.createItem(context, col3)
                   .withTitle("Public item 6")
                   .withIssueDate("2016-01-21")
                   .withAuthor("Andriy, Beket")
                   .withSubject("ExtraEntry")
                   .build();

        context.setDispatcher("noindex");

        // check Collection type with start=0 and limit=default, we expect: indexableObjects=3, totalFound=3
        assertSearchQuery(IndexableCollection.TYPE, 3, 3, 0, -1);
        // check Item type with page=0 and limit=default, we expect: indexableObjects=6, totalFound=6
        assertSearchQuery(IndexableItem.TYPE, 6, 6, 0, -1);
        // delete col3 and all items that it contained
        collectionService.delete(context, col3);
        context.restoreAuthSystemState();

        // check Collection type with start=0 and limit=default,
        // we expect: indexableObjects=2, totalFound=should be 2 but we have 3 ->(1 stale object here)
        assertSearchQuery(IndexableCollection.TYPE, 2, 3, 0, -1);
        // as the previous query hit the stale object running a new query should lead to a clean situation
        assertSearchQuery(IndexableCollection.TYPE, 2, 2, 0, -1);

        // similar test over the items
        // check Item type with start=0 and limit=default,
        // we expect: indexableObjects=3, totalFound=6 (3 stale objects here)
        assertSearchQuery(IndexableItem.TYPE, 3, 6, 0, -1);

        // Run SolrDatabaseResyncCli, updating items with "preDB" status and removing stale items
        performSolrDatabaseResyncScript();

        // as SolrDatabaseResyncCli removed the stale objects, running a new query should lead to a clean situation
        assertSearchQuery(IndexableItem.TYPE, 3, 3, 0, -1);
    }

    @Test
    public void iteratorSearchServiceTest() throws SearchServiceException {
        String subject1 = "subject1";
        String subject2 = "subject2";
        int numberItemsSubject1 = 30;
        int numberItemsSubject2 = 2;
        Item[] itemsSubject1 = new Item[numberItemsSubject1];
        Item[] itemsSubject2 = new Item[numberItemsSubject2];
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        for (int i = 0; i < numberItemsSubject1; i++) {
            itemsSubject1[i] = ItemBuilder.createItem(context, collection)
                .withTitle("item subject 1 number" + i)
                .withSubject(subject1)
                .build();
        }

        for (int i = 0; i < numberItemsSubject2; i++) {
            itemsSubject2[i] = ItemBuilder.createItem(context, collection)
                .withTitle("item subject 2 number " + i)
                .withSubject(subject2)
                .build();
        }

        Collection collection2 = CollectionBuilder.createCollection(context, community).build();
        ItemBuilder.createItem(context, collection2)
            .withTitle("item collection2")
            .withSubject(subject1)
            .build();
        context.restoreAuthSystemState();


        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addFilterQueries("subject:" + subject1);

        Iterator<Item> itemIterator =
            searchService.iteratorSearch(context, new IndexableCollection(collection), discoverQuery);
        int counter = 0;
        List<Item> foundItems = new ArrayList<>();
        while (itemIterator.hasNext()) {
            foundItems.add(itemIterator.next());
            counter++;
        }
        for (Item item : itemsSubject1) {
            assertTrue(foundItems.contains(item));
        }
        assertEquals(numberItemsSubject1, counter);

        discoverQuery = new DiscoverQuery();
        discoverQuery.addFilterQueries("subject:" + subject2);

        itemIterator = searchService.iteratorSearch(context, null, discoverQuery);
        counter = 0;
        foundItems = new ArrayList<>();
        while (itemIterator.hasNext()) {
            foundItems.add(itemIterator.next());
            counter++;
        }
        assertEquals(numberItemsSubject2, counter);
        for (Item item : itemsSubject2) {
            assertTrue(foundItems.contains(item));
        }
    }

    private void assertSearchQuery(String resourceType, int size) throws SearchServiceException {
        assertSearchQuery(resourceType, size, size, 0, -1);
    }

    private void assertSearchQuery(String resourceType, int size, int totalFound, int start, int limit)
        throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        discoverQuery.addFilterQueries("search.resourcetype:" + resourceType);
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(size, indexableObjects.size());
        assertEquals(totalFound, discoverResult.getTotalSearchResults());
    }


    private void deposit(WorkspaceItem workspaceItem)
            throws SQLException, AuthorizeException, IOException, WorkflowException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        workspaceItem = context.reloadEntity(workspaceItem);
        XmlWorkflowItem unusedWorkflowItem = workflowService.startWithoutNotify(context, workspaceItem);
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

    public void performSolrDatabaseResyncScript() throws Exception {
        String[] args = new String[] {"solr-database-resync"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher
                .handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
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
