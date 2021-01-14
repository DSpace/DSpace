/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.eperson.EPerson;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.junit.Ignore;
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

    IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                                   .getServiceByName(IndexingService.class.getName(),
                                                                     IndexingService.class);


    @Ignore
    @Test
    public void deleteWorkspaceItemSolrRecordAfterDeletionFromDbTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
        Collection col = CollectionBuilder.createCollection(context, community)
                                          .build();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col)
                                                          .withAbstract("headache")
                                                          .build();
        context.restoreAuthSystemState();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.addFilterQueries("search.resourceid:" + workspaceItem.getID());
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(1, indexableObjects.size());
        assertEquals(1, discoverResult.getTotalSearchResults());

        context.turnOffAuthorisationSystem();
        workspaceItemService.deleteAll(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        discoverResult = searchService.search(context, discoverQuery);
        indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(0, indexableObjects.size());
        assertEquals(0, discoverResult.getTotalSearchResults());
    }

    @Ignore
    @Test
    public void deleteWorkspaceItemSolrRecordAfterDeletionFromDbTestn() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withWorkflowGroup(1, admin)
                                                 .build();

        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                .withTitle("Test item")
                                                .withIssueDate("2019-03-06")
                                                .withSubject("ExtraEntry")
                                                .build();

        ItemBuilder.createItem(context, collection).build();



        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(collection);

        ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setParameter("submit_approve", "submit_approve");

        XmlWorkflowItem workflowItem = workflowService.startWithoutNotify(context, wsi);
        context.dispatchEvents();
        indexer.commit();

        assertSearchQuery(IndexablePoolTask.TYPE, 1);

        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        executeWorkflowAction(httpServletRequest, admin, workflow, workflowItem,
                "reviewstep", "claimaction");


        context.dispatchEvents();
        indexer.commit();

        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        assertSearchQuery(IndexablePoolTask.TYPE, 0);
        assertSearchQuery(IndexableClaimedTask.TYPE, 1);

        returnToPool(admin, workflowItem);
        context.dispatchEvents();
        indexer.commit();

        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        assertSearchQuery(IndexablePoolTask.TYPE, 1);
        assertSearchQuery(IndexableClaimedTask.TYPE, 0);

        workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, admin);
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

    private void executeWorkflowAction(HttpServletRequest httpServletRequest, EPerson user,
                                       Workflow workflow, XmlWorkflowItem workflowItem, String stepId, String actionId)
            throws Exception {
        final EPerson previousUser = context.getCurrentUser();
        context.setCurrentUser(user);
        workflowService.doState(context, user, httpServletRequest, workflowItem.getID(), workflow,
                                workflow.getStep(stepId).getActionConfig(actionId));
        context.setCurrentUser(previousUser);
    }

    private void returnToPool(EPerson user, XmlWorkflowItem workflowItem)
            throws Exception {
        final EPerson previousUser = context.getCurrentUser();
        context.setCurrentUser(user);
        ClaimedTask task = claimedTaskService
                .findByWorkflowIdAndEPerson(context, workflowItem, context.getCurrentUser());
        workflowService.deleteClaimedTask(context, workflowItem, task);
        workflowRequirementsService.removeClaimedUser(context, workflowItem, task.getOwner(), task.getStepID());
        context.setCurrentUser(previousUser);
    }
}
