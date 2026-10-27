/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.LegacyPluginServiceImpl;
import org.dspace.ctask.testing.FailTask;
import org.dspace.ctask.testing.MarkerTask;
import org.dspace.eperson.EPerson;
import org.dspace.util.DSpaceConfigurationInitializer;
import org.dspace.util.DSpaceKernelInitializer;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test the attachment of curation tasks to workflows.
 *
 * @author mwood
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = { DSpaceKernelInitializer.class, DSpaceConfigurationInitializer.class },
        locations = { "classpath:spring/*.xml" }
)
public class WorkflowCurationIT
        extends AbstractIntegrationTestWithDatabase {
    @Inject
    private ItemService itemService;
    @Autowired
    private LegacyPluginServiceImpl legacyPluginService;
    @Autowired
    private XmlWorkflowService workflowService;
    @Autowired
    private XmlWorkflowFactory workflowFactory;
    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;
    @Autowired
    private WorkspaceItemService workspaceItemService;

    /**
     * Basic smoke test of a curation task attached to a workflow step.
     * See {@link MarkerTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void curationTest()
            throws Exception {
        context.turnOffAuthorisationSystem();
        legacyPluginService.clearNamedPluginClasses();
        //** GIVEN **

        // A submitter;
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .withLanguage("en")
                .build();

        // A containment hierarchy;
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-1";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .build();

        // A workflow configuration for the test Collection;
        // See test/dspaceFolder/config/spring/api/workflow.xml

        // A curation task attached to the workflow;
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.
        // This should include MarkerTask.

        // A workflow item;
        context.setCurrentUser(submitter);
        XmlWorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withTitle("Test of workflow curation")
                .withIssueDate("2021-05-14")
                .withSubject("Testing")
                .build();

        context.restoreAuthSystemState();

        //** THEN **

        assertThat("Item should have been curated", countMarks(wfi.getItem()) > 0);
    }

    /**
     * A curation task attached to a workflow step must run only when the item
     * enters the step, and not again when the step is claimed.
     * See {@link MarkerTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void curationDoesNotRunWhenClaiming()
            throws Exception {
        context.turnOffAuthorisationSystem();
        legacyPluginService.clearNamedPluginClasses();
        //** GIVEN **

        // A reviewer;
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .build();

        // A containment hierarchy, using the default workflow;
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-3";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .withWorkflowGroup("reviewer", reviewer)
                .build();

        // A curation task attached to 'reviewstep', the first step of the workflow;
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.

        // A workflow item, claimed by the reviewer in 'reviewstep';
        ClaimedTask task = ClaimedTaskBuilder.createClaimedTask(context, collection, reviewer)
                .withTitle("Test of workflow curation")
                .build();

        context.restoreAuthSystemState();

        //** THEN **

        assertEquals("Item should have been curated once", 1, countMarks(task.getWorkflowItem().getItem()));
    }

    /**
     * A curation task attached to a later workflow step must not run when the
     * item enters the workflow or when the first step is claimed, but once when
     * the item enters that later step.
     * See {@link MarkerTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void laterStepCurationRunsWhenEnteringStep()
            throws Exception {
        context.turnOffAuthorisationSystem();
        legacyPluginService.clearNamedPluginClasses();
        //** GIVEN **

        // A reviewer and an editor;
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .build();
        EPerson editor = EPersonBuilder.createEPerson(context)
                .withEmail("editor@example.com")
                .build();

        // A containment hierarchy, using the default workflow;
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-2";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .withWorkflowGroup("reviewer", reviewer)
                .withWorkflowGroup("editor", editor)
                .build();

        // A curation task attached to 'editstep', the second step of the workflow;
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.

        // A workflow item, claimed by the reviewer in 'reviewstep';
        ClaimedTask task = ClaimedTaskBuilder.createClaimedTask(context, collection, reviewer)
                .withTitle("Test of workflow curation")
                .build();
        XmlWorkflowItem wfi = task.getWorkflowItem();

        context.restoreAuthSystemState();

        assertEquals("reviewstep", wfi.getStepID());
        assertEquals("Item should not have been curated", 0, countMarks(wfi.getItem()));

        //** WHEN **

        // The reviewer approves the item;
        Workflow workflow = workflowFactory.getWorkflowByName(wfi.getWorkflowID());
        MockHttpServletRequest approveRequest = new MockHttpServletRequest();
        approveRequest.setParameter("submit_approve", "submit_approve");
        context.setCurrentUser(reviewer);
        workflowService.doState(context, reviewer, approveRequest, wfi.getID(), workflow,
                workflow.getStep(task.getStepID()).getActionConfig(task.getActionID()));

        //** THEN **

        assertEquals("editstep", wfi.getStepID());
        assertEquals("Item should have been curated once", 1, countMarks(wfi.getItem()));
    }

    /**
     * A failed curation task empowered to reject, attached to the first
     * workflow step, must send the item back to the submitter when the item
     * enters the workflow.
     * See {@link FailTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void firstStepCurationRejectsItem()
            throws Exception {
        context.turnOffAuthorisationSystem();
        legacyPluginService.clearNamedPluginClasses();
        //** GIVEN **

        // A submitter and a reviewer;
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .withLanguage("en")
                .build();
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .build();

        // A containment hierarchy, using the default workflow;
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-4";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .withWorkflowGroup("reviewer", reviewer)
                .build();

        // A failing curation task, empowered to reject, attached to 'reviewstep';
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.

        //** WHEN **

        // The item enters the workflow;
        context.setCurrentUser(submitter);
        XmlWorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withTitle("Test of workflow curation")
                .withIssueDate("2021-05-14")
                .withSubject("Testing")
                .build();

        context.restoreAuthSystemState();

        //** THEN **

        assertNull("Item should not be in workflow", xmlWorkflowItemService.find(context, wfi.getID()));
        assertNotNull("Item should be back in the workspace",
                workspaceItemService.findByItem(context, wfi.getItem()));
    }

    /**
     * A failed curation task empowered to reject, attached to a later workflow
     * step, must send the item back to the submitter when the item enters
     * that step.
     * See {@link FailTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void laterStepCurationRejectsItem()
            throws Exception {
        context.turnOffAuthorisationSystem();
        legacyPluginService.clearNamedPluginClasses();
        //** GIVEN **

        // A reviewer and an editor;
        EPerson reviewer = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer@example.com")
                .build();
        EPerson editor = EPersonBuilder.createEPerson(context)
                .withEmail("editor@example.com")
                .build();

        // A containment hierarchy, using the default workflow;
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-5";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .withWorkflowGroup("reviewer", reviewer)
                .withWorkflowGroup("editor", editor)
                .build();

        // A failing curation task, empowered to reject, attached to 'editstep';
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.

        // A workflow item, claimed by the reviewer in 'reviewstep';
        ClaimedTask task = ClaimedTaskBuilder.createClaimedTask(context, collection, reviewer)
                .withTitle("Test of workflow curation")
                .build();
        XmlWorkflowItem wfi = task.getWorkflowItem();

        context.restoreAuthSystemState();

        //** WHEN **

        // The reviewer approves the item;
        Workflow workflow = workflowFactory.getWorkflowByName(wfi.getWorkflowID());
        MockHttpServletRequest approveRequest = new MockHttpServletRequest();
        approveRequest.setParameter("submit_approve", "submit_approve");
        context.setCurrentUser(reviewer);
        workflowService.doState(context, reviewer, approveRequest, wfi.getID(), workflow,
                workflow.getStep(task.getStepID()).getActionConfig(task.getActionID()));

        //** THEN **

        assertNull("Item should not be in workflow", xmlWorkflowItemService.find(context, wfi.getID()));
        assertNotNull("Item should be back in the workspace",
                workspaceItemService.findByItem(context, wfi.getItem()));
    }

    /**
     * Count the Item's provenance records with MarkerTask's name.
     * @param item the item to search.
     * @return the number of times the item has been marked by MarkerTask.
     */
    private int countMarks(Item item) {
        List<MetadataValue> provenance = itemService.getMetadata(item,
                MarkerTask.SCHEMA, MarkerTask.ELEMENT, MarkerTask.QUALIFIER, MarkerTask.LANGUAGE);
        Pattern markerPattern = Pattern.compile(MarkerTask.class.getCanonicalName());
        int marks = 0;
        for (MetadataValue record : provenance) {
            if (markerPattern.matcher(record.getValue()).find()) {
                marks++;
            }
        }
        return marks;
    }
}
