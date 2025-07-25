/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.LogicalFilterTest;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.curate.Curator;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class CopyFieldsCurationTaskTest extends AbstractUnitTest {

    protected Community community;
    protected Collection collection;
    protected Item item;
    protected Curator curator;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    protected ConfigurationService configurationService = kernelImpl.getConfigurationService();


    protected static final String editorName = "Doe, Jane";
    protected static final String authorName = "Doe, John";
    protected static final String P_TASK_DEF = "plugin.named.org.dspace.curate.CurationTask";
    protected static final String TASK_NAME = "testCopyFieldCT";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LogicalFilterTest.class);

    @Before
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();

            this.community = communityService.create(null, context);
            this.collection = collectionService.create(context, community);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            this.item = installItemService.installItem(context, workspaceItem);
            itemService.addMetadata(context, this.item,"dc", "contributor", "author", null,
                    this.authorName);
            itemService.addMetadata(context, this.item,"dc", "contributor", "editor", null,
                    this.editorName);
            itemService.update(context, this.item);

            configurationService.setProperty("curate." + this.TASK_NAME + ".targetField",
                    "dc.contributor.other");
            configurationService.setProperty("curate." + this.TASK_NAME + ".sourceFields", new String[]
                    {"dc.contributor.author", "dc.contributor.editor"});
        } catch (AuthorizeException | SQLException e) {
            log.error("Error encountered during init", e);
            fail("Error encountered during init: " + e.getMessage());
        }
    }

    // this registers our task. We could run this as part of #init(), but I prefer the curator to call the init method
    // of our task, after we set all its configuration. As we need to change the configuration for different tests,
    // we need to run this manually, when the ctask is configured.
    protected void registerTask() {
        // Must remove any cached named plugins before creating a new one
        pluginService.clearNamedPluginClasses();

        // Define a new task dynamically
        configurationService.setProperty(P_TASK_DEF, CopyFieldsCurationTask.class.getCanonicalName() + " = "
                + TASK_NAME);

        this.curator = new Curator();
        this.curator.addTask("testCopyFieldCT");
        assertTrue(this.curator.hasTask(TASK_NAME));
    }

    @After
    public void destroy() {
        try {
            // The init message should have turned the authorization system off
            // turn it off, if a test reinstated it.
            if (!context.ignoreAuthorization()) {
                context.turnOffAuthorisationSystem();
            }

            // delete the item we created
            itemService.delete(context, this.item);
            this.item = null;

            // delete the collection we created
            collectionService.delete(context, this.collection);
            this.collection = null;

            // delete the community we created
            communityService.delete(context, this.community);
            this.community = null;

            // remove our curation task
            if (this.curator.hasTask(CopyFieldsCurationTask.class.getCanonicalName() + " = " + TASK_NAME)) {
                this.curator.removeTask(CopyFieldsCurationTask.class.getCanonicalName() + " = " + TASK_NAME);
            }

            // clear all named plugins
            pluginService.clearNamedPluginClasses();
            // remove configuration properties we set
            configurationService.setProperty("curate." + this.TASK_NAME + ".targetField", null);
            configurationService.setProperty("curate." + this.TASK_NAME + ".sourceFields", null);
            configurationService.setProperty(P_TASK_DEF, null);
            // reload the original configuration
            configurationService.reloadConfig();

        } catch (Exception ex) {
            log.error("Error cleaning up test resources: " + ex.getMessage(), ex);
        } finally {
            context.restoreAuthSystemState();
        }
        super.destroy();
    }

    @Test
    public void testCopyFields() throws IOException {
        this.registerTask();
        assertTrue("Metadata field unexpectedly exists in freshly created item.",
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY)));
        // run the default configuration with the source and target fields defined above
        curator.curate(context, item);
        assertFalse("Metadatafield not created by copy field curation task.",
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY)));
        assertEquals("Metadata field not set as expected",
                itemService.getMetadataFirstValue(this.item, "dc", "contributor", "other", Item.ANY),
                this.authorName + this.editorName);
    }

    @Test
    public void testConcatenator() throws IOException {
        configurationService.setProperty("curate." + this.TASK_NAME + ".concatenator", "'\\, '");
        log.fatal("FOOOBAR: '" + this.configurationService.getPropertyValue(
                "curate." + this.TASK_NAME + ".concatenator").toString() + "'");
        this.registerTask();
        assertTrue("Metadata field unexpectedly exists in freshly created item.",
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY)));
        // run the default configuration with the source and target fields defined above
        curator.curate(context, item);
        assertFalse("Metadatafield not created by copy field curation task.",
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY)));
        assertEquals("Metadata field not set as expected:", this.authorName + ", " + this.editorName,
                itemService.getMetadataFirstValue(this.item, "dc", "contributor", "other", Item.ANY));

        // cleanup
        configurationService.setProperty("curate." + this.TASK_NAME + ".concatenator", null);
    }

    @Test
    public void testRemovalOfSourceFields() throws IOException {
        configurationService.setProperty("curate." + this.TASK_NAME + ".cleanSourceFields", Boolean.TRUE);
        this.registerTask();
        // run the default configuration with the source and target fields defined above
        curator.curate(context, item);
        assertTrue("Metadata field unexpectedly exists in freshly created item:"
                        + itemService.getMetadataFirstValue(this.item, "dc", "contributor", "author", Item.ANY),
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "author", Item.ANY)));
        assertTrue("Metadata field unexpectedly exists in freshly created item:"
                        + itemService.getMetadataFirstValue(this.item, "dc", "contributor", "editor", Item.ANY),
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "editor", Item.ANY)));

        // cleanup
        configurationService.setProperty("curate." + this.TASK_NAME + ".cleanSourceFields", null);
    }

    @Test
    public void testRemovalOfTargetField() throws IOException, SQLException, AuthorizeException {
        configurationService.setProperty("curate." + this.TASK_NAME + ".cleanTargetField", true);
        this.registerTask();

        itemService.addMetadata(context, this.item,"dc", "contributor", "other", null,
                this.editorName);
        itemService.update(context, item);

        // run the default configuration with the source and target fields defined above
        curator.curate(context, item);
        List<MetadataValue> otherContributors = itemService.getMetadata(this.item, "dc", "contributor",
                "other", Item.ANY);
        assertEquals("Target field not cleared by curation task.", 1, otherContributors.size());
        assertEquals("Metadata field not set as expected",
                itemService.getMetadataFirstValue(this.item, "dc", "contributor", "other", Item.ANY),
                this.authorName + this.editorName);

        // cleanup
        configurationService.setProperty("curate." + this.TASK_NAME + ".cleanTargetField", null);
    }

    @Test
    public void testSkippingOnMissingSourceField() throws SQLException, AuthorizeException, IOException {
        itemService.clearMetadata(context, item, "dc", "contributor", "editor", Item.ANY);
        itemService.update(context, this.item);

        List<MetadataValue> e = itemService.getMetadata(this.item, "dc", "contributor", "editor", Item.ANY);
        assertTrue("Editors were not cleared properly.",
                e == null || e.isEmpty());
        configurationService.setProperty("curate." + this.TASK_NAME + ".skipOnMissingFields", true);

        this.registerTask();

        curator.curate(context, item);

        List<MetadataValue> authors = itemService.getMetadata(this.item, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> editors = itemService.getMetadata(this.item, "dc", "contributor", "editor", Item.ANY);
        List<MetadataValue> others = itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY);

        assertTrue("Author changed while curation task should have skipped the item.",
                authors == null || !authors.get(0).equals(this.authorName));
        assertTrue("Editors were reinstated, while curation task should have skipped the item.",
                editors == null || editors.isEmpty());
        assertTrue("Other contributors were added while curation task should have skipped the item.",
                others == null || others.isEmpty());

        // cleanup
        configurationService.setProperty("curate." + this.TASK_NAME + ".skipOnMissingFields", null);
    }

    @Test
    public void testCurateDistributedMetadata() throws SQLException, AuthorizeException, IOException {
        communityService.addMetadata(context, community, "dc", "contributor", "author", null, authorName);
        communityService.addMetadata(context, community, "dc", "contributor", "editor", null, editorName);
        communityService.update(context, community);

        collectionService.addMetadata(context, collection, "dc", "contributor", "author", null, authorName);
        collectionService.addMetadata(context, collection, "dc", "contributor", "editor", null, editorName);
        collectionService.update(context, collection);

        this.registerTask();

        curator.curate(context, community);


        assertFalse("Metadatafield not created in community by copy field curation task.",
                CollectionUtils.isEmpty(communityService.getMetadata(this.community, "dc", "contributor",
                        "other", Item.ANY)));
        assertEquals("Metadata field not set as expected for community.",
                communityService.getMetadataFirstValue(this.community, "dc", "contributor", "other", Item.ANY),
                this.authorName + this.editorName);

        assertFalse("Metadatafield not created in collection by copy field curation task.",
                CollectionUtils.isEmpty(collectionService.getMetadata(this.collection, "dc", "contributor",
                        "other", Item.ANY)));
        assertEquals("Metadata field not set as expected for collection",
                collectionService.getMetadataFirstValue(this.collection, "dc", "contributor", "other", Item.ANY),
                this.authorName + this.editorName);

        assertFalse("Metadatafield not created by distributed copy field curation task.",
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY)));
        assertEquals("Metadata field not distributed into the item",
                itemService.getMetadataFirstValue(this.item, "dc", "contributor", "other", Item.ANY),
                this.authorName + this.editorName);
    }

    @Test
    public void testDistributedTypedCuration() throws SQLException, AuthorizeException, IOException {
        communityService.addMetadata(context, community, "dc", "contributor", "author", null, authorName);
        communityService.addMetadata(context, community, "dc", "contributor", "editor", null, editorName);
        communityService.update(context, community);

        assertTrue("Community should not have a field dc.contributor.other: " + communityService.getMetadataFirstValue(
                        this.community, "dc", "contributor", "other", Item.ANY),
                CollectionUtils.isEmpty(communityService.getMetadata(this.community, "dc", "contributor",
                        "other", Item.ANY)));

        collectionService.addMetadata(context, collection, "dc", "contributor", "author", null, authorName);
        collectionService.addMetadata(context, collection, "dc", "contributor", "editor", null, editorName);
        collectionService.update(context, collection);

        configurationService.setProperty("curate." + this.TASK_NAME + ".types", new String[] {"Collection"});
        this.registerTask();

        curator.curate(context, community);


        assertTrue("Curation Task did not skip the community: " + communityService.getMetadataFirstValue(
                this.community, "dc", "contributor", "other", Item.ANY),
                CollectionUtils.isEmpty(communityService.getMetadata(this.community, "dc", "contributor",
                        "other", Item.ANY)));

        assertFalse("Metadatafield not created in collection by copy field curation task.",
                CollectionUtils.isEmpty(collectionService.getMetadata(this.collection, "dc", "contributor",
                        "other", Item.ANY)));
        assertEquals("Metadata field not set as expected for collection",
                collectionService.getMetadataFirstValue(this.collection, "dc", "contributor", "other", Item.ANY),
                this.authorName + this.editorName);

        assertTrue("Curation Task did not skip the item.",
                CollectionUtils.isEmpty(itemService.getMetadata(this.item, "dc", "contributor", "other", Item.ANY)));

        // cleanup
        configurationService.setProperty("curate." + this.TASK_NAME + ".types", null);
    }

}
