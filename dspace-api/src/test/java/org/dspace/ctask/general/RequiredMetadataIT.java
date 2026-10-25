/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.Curator;
import org.junit.Test;

/**
 * Integration tests for the {@link RequiredMetadata} curation task.
 */
public class RequiredMetadataIT extends AbstractIntegrationTestWithDatabase {

    private static final String TASK_NAME = "requiredmetadata";

    /**
     * A failure on one item must not stop the task from checking the other items in a container.
     */
    @Test
    public void testCurateCollectionReportsAllIncompleteItems() throws Exception {
        // Other tests may have left the named plugin cache without this task
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .build();
        // Neither item has any metadata, so both lack required fields
        Item firstItem = ItemBuilder.createItem(context, collection)
                                    .build();
        Item secondItem = ItemBuilder.createItem(context, collection)
                                     .build();
        context.restoreAuthSystemState();

        StringBuilder report = new StringBuilder();
        Curator curator = new Curator();
        curator.setReporter(report);
        curator.addTask(context, TASK_NAME);
        curator.curate(context, collection);

        assertEquals("Curation should fail", Curator.CURATE_FAIL, curator.getStatus(TASK_NAME));
        assertThat("First item should be reported", report.toString(),
                   containsString("Item: " + firstItem.getHandle() + " missing required field"));
        assertThat("Second item should be reported", report.toString(),
                   containsString("Item: " + secondItem.getHandle() + " missing required field"));
    }
}
