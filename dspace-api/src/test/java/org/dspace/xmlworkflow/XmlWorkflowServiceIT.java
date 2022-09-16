/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.EPerson;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
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
