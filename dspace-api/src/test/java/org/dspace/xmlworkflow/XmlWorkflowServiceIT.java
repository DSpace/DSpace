/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * IT for {@link XmlWorkflowServiceImpl}
 *
 * @author Maria Verdonck (Atmire) on 14/12/21
 */
public class XmlWorkflowServiceIT extends AbstractIntegrationTestWithDatabase {

    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
    protected IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                                             .getServiceByName(IndexingService.class.getName(),
                                                                 IndexingService.class);
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Cleans up the created workflow role groups after each test
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    @After
    public void cleanup() throws SQLException, AuthorizeException, IOException {
        Group reviewManagers = groupService.findByName(context, "ReviewManagers");
        if (reviewManagers != null) {
            groupService.delete(context, reviewManagers);
        }
    }

    /**
     * Test to verify that if a user submits an item into the workflow, then it gets rejected that the submitter gets
     * write access back on the item
     *
     * @throws Exception
     */
    @Test
    public void workflowUserRejectsItemTheySubmitted_ItemShouldBeEditable() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.org").build();
        context.setCurrentUser(submitter);
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community)
                                                      .withName("Collection WITH workflow")
                                                      .withWorkflowGroup(1, submitter)
                                                      .build();
        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(colWithWorkflow);
        ClaimedTask taskToReject = ClaimedTaskBuilder.createClaimedTask(context, colWithWorkflow, submitter)
                                                     .withTitle("Test workflow item to reject").build();
        context.restoreAuthSystemState();

        // Submitter person is both original submitter as well as reviewer, should have edit access of claimed task
        assertTrue(this.containsRPForUser(taskToReject.getWorkflowItem().getItem(), submitter, Constants.WRITE));

        // reject
        MockHttpServletRequest httpRejectRequest = new MockHttpServletRequest();
        httpRejectRequest.setParameter("submit_reject", "submit_reject");
        httpRejectRequest.setParameter("reason", "test");
        executeWorkflowAction(httpRejectRequest, workflow, taskToReject);

        // Submitter person is both original submitter as well as reviewer, should have edit access of reject, i.e.
        // sent back/to submission task
        assertTrue(this.containsRPForUser(taskToReject.getWorkflowItem().getItem(), submitter, Constants.WRITE));
    }

    /**
     * Test to verify that if a user submits an item into the workflow, a reviewmanager can select a single reviewer
     * eperson
     */
    @Test
    public void workflowUserSingleSelectedReviewer_ItemShouldBeEditable() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.org").build();
        context.setCurrentUser(submitter);
        EPerson reviewManager =
            EPersonBuilder.createEPerson(context).withEmail("reviewmanager-test@example.org").build();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community, "123456789/workflow-test-1")
            .withName("Collection WITH workflow")
            .withWorkflowGroup("reviewmanagers", reviewManager)
            .build();
        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(colWithWorkflow);
        ClaimedTask task = ClaimedTaskBuilder.createClaimedTask(context, colWithWorkflow, reviewManager)
            .withTitle("Test workflow item to reject").build();
        // Set reviewer group property and add reviewer to group
        SelectReviewerAction.resetGroup();
        configurationService.setProperty("action.selectrevieweraction.group", "Reviewers");
        Group reviewerGroup = GroupBuilder.createGroup(context).withName("Reviewers").build();
        EPerson reviewer = EPersonBuilder.createEPerson(context).withEmail("reviewer@example.org").build();
        groupService.addMember(context, reviewerGroup, reviewer);
        context.restoreAuthSystemState();

        // Review Manager should have access to workflow item
        assertTrue(this.containsRPForUser(task.getWorkflowItem().getItem(), reviewManager, Constants.WRITE));

        // select 1 reviewer
        MockHttpServletRequest httpSelectReviewerRequest = new MockHttpServletRequest();
        httpSelectReviewerRequest.setParameter("submit_select_reviewer", "true");
        httpSelectReviewerRequest.setParameter("eperson", reviewer.getID().toString());
        executeWorkflowAction(httpSelectReviewerRequest, workflow, task);

        // Reviewer should have access to workflow item
        assertTrue(this.containsRPForUser(task.getWorkflowItem().getItem(), reviewer, Constants.WRITE));
    }

    /**
     * Test to verify that if a user submits an item into the workflow, a reviewmanager can select a multiple reviewer
     * epersons
     */
    @Test
    public void workflowUserMultipleSelectedReviewer_ItemShouldBeEditable() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.org").build();
        context.setCurrentUser(submitter);
        EPerson reviewManager =
            EPersonBuilder.createEPerson(context).withEmail("reviewmanager-test@example.org").build();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community, "123456789/workflow-test-1")
            .withName("Collection WITH workflow")
            .withWorkflowGroup("reviewmanagers", reviewManager)
            .build();
        Workflow workflow = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory().getWorkflow(colWithWorkflow);
        ClaimedTask task = ClaimedTaskBuilder.createClaimedTask(context, colWithWorkflow, reviewManager)
            .withTitle("Test workflow item to reject").build();
        // Set reviewer group property and add reviewer to group
        SelectReviewerAction.resetGroup();
        configurationService.setProperty("action.selectrevieweraction.group", "Reviewers");
        Group reviewerGroup = GroupBuilder.createGroup(context).withName("Reviewers").build();
        EPerson reviewer1 = EPersonBuilder.createEPerson(context).withEmail("reviewer1@example.org").build();
        EPerson reviewer2 = EPersonBuilder.createEPerson(context).withEmail("reviewer2@example.org").build();
        groupService.addMember(context, reviewerGroup, reviewer1);
        groupService.addMember(context, reviewerGroup, reviewer2);
        context.restoreAuthSystemState();

        // Review Manager should have access to workflow item
        assertTrue(this.containsRPForUser(task.getWorkflowItem().getItem(), reviewManager, Constants.WRITE));

        // Select multiple reviewers
        MockHttpServletRequest httpSelectMultipleReviewers = new MockHttpServletRequest();
        httpSelectMultipleReviewers.setParameter("submit_select_reviewer", "true");
        httpSelectMultipleReviewers.setParameter("eperson", reviewer1.getID().toString(), reviewer2.getID().toString());
        executeWorkflowAction(httpSelectMultipleReviewers, workflow, task);

        // Reviewers should have access to workflow item
        assertTrue(this.containsRPForUser(task.getWorkflowItem().getItem(), reviewer1, Constants.WRITE));
        assertTrue(this.containsRPForUser(task.getWorkflowItem().getItem(), reviewer2, Constants.WRITE));
    }

    private boolean containsRPForUser(Item item, EPerson user, int action) throws SQLException {
        List<ResourcePolicy> rps = authorizeService.getPolicies(context, item);
        for (ResourcePolicy rp : rps) {
            if (rp.getEPerson().getID().equals(user.getID()) && rp.getAction() == action) {
                return true;
            }
        }
        return false;
    }

    private void executeWorkflowAction(HttpServletRequest httpServletRequest, Workflow workflow, ClaimedTask task)
        throws Exception {
        final EPerson previousUser = context.getCurrentUser();
        task = context.reloadEntity(task);
        context.setCurrentUser(task.getOwner());
        xmlWorkflowService
            .doState(context, task.getOwner(), httpServletRequest, task.getWorkflowItem().getID(), workflow,
                workflow.getStep(task.getStepID()).getActionConfig(task.getActionID()));
        context.commit();
        indexer.commit();
        context.setCurrentUser(previousUser);
    }
}
