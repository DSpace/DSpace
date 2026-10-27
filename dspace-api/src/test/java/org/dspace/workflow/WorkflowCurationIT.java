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
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.LegacyPluginServiceImpl;
import org.dspace.ctask.testing.MarkerTask;
import org.dspace.eperson.EPerson;
import org.dspace.util.DSpaceConfigurationInitializer;
import org.dspace.util.DSpaceKernelInitializer;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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

        assertThat("Item should have been curated", isMarked(wfi.getItem()));
    }

    /**
     * A curation task attached to a later workflow step must not run when the
     * item enters the workflow.
     * See {@link MarkerTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void laterStepCurationDoesNotRunAtWorkflowStart()
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
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-2";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .withWorkflowGroup("reviewer", reviewer)
                .build();

        // A curation task attached to 'editstep', the second step of the workflow;
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.

        // A workflow item, waiting in 'reviewstep';
        context.setCurrentUser(submitter);
        XmlWorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withTitle("Test of workflow curation")
                .withIssueDate("2021-05-14")
                .withSubject("Testing")
                .build();

        context.restoreAuthSystemState();

        //** THEN **

        assertEquals("reviewstep", wfi.getStepID());
        assertFalse("Item should not have been curated", isMarked(wfi.getItem()));
    }

    /**
     * Search the Item's provenance for MarkerTask's name.
     * @param item the item to search.
     * @return true if the item has been marked by MarkerTask.
     */
    private boolean isMarked(Item item) {
        List<MetadataValue> provenance = itemService.getMetadata(item,
                MarkerTask.SCHEMA, MarkerTask.ELEMENT, MarkerTask.QUALIFIER, MarkerTask.LANGUAGE);
        Pattern markerPattern = Pattern.compile(MarkerTask.class.getCanonicalName());
        for (MetadataValue record : provenance) {
            if (markerPattern.matcher(record.getValue()).find()) {
                return true;
            }
        }
        return false;
    }
}
